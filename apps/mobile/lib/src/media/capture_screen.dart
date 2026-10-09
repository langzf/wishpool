import 'dart:async';
import 'dart:io';

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:just_audio/just_audio.dart';
import 'package:record/record.dart';
import 'package:video_player/video_player.dart';

import '../data/wishpool_scope.dart';
import '../data/wishpool_repository.dart';
import '../shared/fixture_data.dart';
import 'media_capture_utils.dart';
import '../features/submission_waiting_screen.dart';
import '../shared/friendly_error.dart';

class MediaCaptureScreen extends StatefulWidget {
  const MediaCaptureScreen({
    super.key,
    required this.task,
    required this.scope,
    required this.onSubmitted,
  });
  final ChildTask task;
  final WishPoolScope scope;
  final VoidCallback onSubmitted;

  @override
  State<MediaCaptureScreen> createState() => _MediaCaptureScreenState();
}

class _MediaCaptureScreenState extends State<MediaCaptureScreen> {
  CameraController? _camera;
  File? _file;
  AudioPlayer? _player;
  VideoPlayerController? _video;
  final _recorder = AudioRecorder();
  Timer? _timer;
  int _seconds = 0;
  bool _busy = false;
  bool _recording = false;
  bool _videoRecording = false;
  bool _recordingTransition = false;
  bool _submitted = false;
  bool _canRetryUpload = false;
  String _status = '准备采集';
  int _lastNoticeVersion = 0;

  bool get _isPhoto => widget.task.submissionTypeCode == 'photo';
  bool get _isAudio => widget.task.submissionTypeCode == 'audio';

  @override
  void initState() {
    super.initState();
    widget.scope.uploadQueue.addListener(_handleQueueNotice);
    if (!_isAudio) _prepareCamera();
  }

  void _handleQueueNotice() {
    final queue = widget.scope.uploadQueue;
    if (!mounted || queue.noticeVersion == _lastNoticeVersion) return;
    _lastNoticeVersion = queue.noticeVersion;
    final notice = queue.lastNotice;
    if (notice == null) return;
    setState(() {
      _busy = false;
      _status = notice;
      _submitted = notice == '已提交，等家长看看';
      _canRetryUpload = notice == '网络不稳定，稍后自动重试';
    });
  }

  Future<void> _prepareCamera() async {
    try {
      final cameras = await availableCameras();
      if (cameras.isEmpty) throw StateError('没有可用摄像头');
      final controller = CameraController(cameras.first, ResolutionPreset.high,
          enableAudio: true);
      await controller.initialize();
      if (!mounted) return;
      setState(() => _camera = controller);
    } catch (error) {
      if (mounted) {
        setState(() => _status =
            '\u6444\u50cf\u5934\u4e0d\u53ef\u7528\uff0c${childFriendlyError(error)}');
      }
    }
  }

  @override
  void dispose() {
    widget.scope.uploadQueue.removeListener(_handleQueueNotice);
    _timer?.cancel();
    _camera?.dispose();
    _player?.dispose();
    _video?.dispose();
    _recorder.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('${widget.task.submissionType}采集')),
      body: ListView(padding: const EdgeInsets.all(20), children: [
        Text(widget.task.title,
            style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 12),
        Text(_status),
        const SizedBox(height: 20),
        if (_file == null) _captureArea() else _previewArea(),
        const SizedBox(height: 20),
        if (_file == null)
          FilledButton.icon(
              onPressed:
                  (_busy && !_recording && !_videoRecording) ? null : _capture,
              icon: Icon(_isAudio
                  ? Icons.mic
                  : (_isPhoto ? Icons.camera_alt : Icons.videocam)),
              label: Text(_recording
                  ? '停止录音'
                  : (_videoRecording
                      ? '停止录制'
                      : (_isPhoto ? '拍照' : (_isAudio ? '录音' : '录视频'))))),
        if (_file != null) ...[
          OutlinedButton(
              onPressed: _busy ? null : _retake,
              child: Text(_isAudio ? '重录' : '重拍/重录')),
          FilledButton.icon(
              onPressed: _busy ? null : _upload,
              icon: const Icon(Icons.cloud_upload),
              label: Text(_busy ? '上传中…' : '上传并提交')),
          if (_submitted)
            OutlinedButton(
              onPressed: () {
                widget.onSubmitted();
                Navigator.pop(context);
              },
              child: const Text('返回首页'),
            ),
          if (_canRetryUpload)
            OutlinedButton(
              onPressed: _busy ? null : _retryUpload,
              child: const Text('立即重试'),
            ),
        ],
      ]),
    );
  }

  Widget _captureArea() {
    if (_isAudio) {
      return const Card(
          child: Padding(
              padding: EdgeInsets.all(32),
              child: Icon(Icons.mic_none, size: 96)));
    }
    if (_camera == null || !_camera!.value.isInitialized) {
      return const Center(child: CircularProgressIndicator());
    }
    return AspectRatio(
        aspectRatio: _camera!.value.aspectRatio,
        child: CameraPreview(_camera!));
  }

  Widget _previewArea() {
    if (_isPhoto) return Image.file(_file!, fit: BoxFit.contain, height: 280);
    if (_isAudio) {
      return Column(children: [
        const Icon(Icons.audiotrack, size: 80),
        Text('录音时长：${_seconds}s'),
        ElevatedButton(onPressed: _playAudio, child: const Text('试听'))
      ]);
    }
    if (_video?.value.isInitialized != true) {
      return const Center(child: CircularProgressIndicator());
    }
    return Column(children: [
      AspectRatio(
          aspectRatio: _video!.value.aspectRatio, child: VideoPlayer(_video!)),
      ElevatedButton(
          onPressed: () => setState(
              () => _video!.value.isPlaying ? _video!.pause() : _video!.play()),
          child: const Text('播放/暂停'))
    ]);
  }

  Future<void> _capture() async {
    if (_isAudio) return _toggleRecording();
    final camera = _camera;
    if (camera == null) return;
    if (_videoRecording) {
      await _stopVideoRecording(camera);
      return;
    }
    try {
      setState(() => _busy = true);
      if (!_isPhoto) {
        await camera.startVideoRecording();
        if (mounted) {
          setState(() {
            _busy = false;
            _videoRecording = true;
            _status = '正在录视频，再次点击停止';
          });
        }
        return;
      }
      final result = await camera.takePicture();
      final file = File(result.path);
      if (_isPhoto) {
        final compressed = await compressPhoto(file);
        final originalBytes = file.lengthSync();
        final selectedBytes = compressed.lengthSync();
        final status = compressed.path == file.path
            ? '\u539f\u59cb $selectedBytes \u5b57\u8282'
            : '\u7167\u7247\u5df2\u538b\u7f29\uff1a$originalBytes \u2192 $selectedBytes \u5b57\u8282';
        setState(() => _status = status);
        /*
            '照片已压缩：${file.lengthSync()} → ${compressed.lengthSync()} 字节');
        */
        await _setFile(compressed);
      } else {
        await _setFile(file);
      }
    } catch (error) {
      if (mounted) {
        setState(() => _status =
            '\u91c7\u96c6\u5931\u8d25\uff0c${childFriendlyError(error)}');
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _stopVideoRecording(CameraController camera) async {
    if (_recordingTransition) return;
    _recordingTransition = true;
    try {
      if (mounted) setState(() => _busy = true);
      final result = await camera.stopVideoRecording();
      if (mounted) {
        setState(() {
          _videoRecording = false;
          _status = '视频录制完成，可预览';
        });
      }
      await _setFile(File(result.path));
    } catch (error) {
      if (mounted) {
        setState(() => _status =
            '\u505c\u6b62\u5f55\u5236\u5931\u8d25\uff0c${childFriendlyError(error)}');
      }
    } finally {
      _recordingTransition = false;
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _toggleRecording() async {
    if (_recordingTransition) return;
    _recordingTransition = true;
    if (_recording) {
      try {
        final path = await _recorder.stop();
        _timer?.cancel();
        if (path != null) await _setFile(File(path));
        if (mounted) {
          setState(() {
            _recording = false;
            _status = '录音完成，可试听';
          });
        }
      } finally {
        _recordingTransition = false;
      }
      return;
    }
    try {
      if (!await _recorder.hasPermission()) {
        if (mounted) setState(() => _status = '请允许麦克风权限');
        return;
      }
      final path =
          '${Directory.systemTemp.path}${Platform.pathSeparator}wishpool-${DateTime.now().millisecondsSinceEpoch}.m4a';
      if (mounted) {
        setState(() {
          _recording = true;
          _status = '正在录音，再次点击停止';
        });
      }
      await _recorder.start(const RecordConfig(encoder: AudioEncoder.aacLc),
          path: path);
      _seconds = 0;
      _timer = Timer.periodic(const Duration(seconds: 1), (_) {
        if (mounted && _recording) setState(() => _seconds++);
      });
    } catch (error) {
      if (mounted) {
        setState(() {
          _recording = false;
          _status =
              '\u5f55\u97f3\u5931\u8d25\uff0c${childFriendlyError(error)}';
        });
      }
    } finally {
      _recordingTransition = false;
    }
  }

  Future<void> _setFile(File file) async {
    final message = mediaLimitMessage(file);
    if (message != null) {
      setState(() => _status = message);
      return;
    }
    _file = file;
    if (!_isPhoto && !_isAudio) {
      _video = VideoPlayerController.file(file);
      await _video!.initialize();
    }
    if (mounted) setState(() {});
  }

  Future<void> _playAudio() async {
    _player ??= AudioPlayer();
    await _player!.setFilePath(_file!.path);
    await _player!.play();
  }

  void _retake() {
    _video?.dispose();
    setState(() {
      _file = null;
      _video = null;
      _status = '准备采集';
    });
  }

  Future<void> _upload() async {
    final file = _file;
    if (file == null) return;
    setState(() {
      _busy = true;
      _status = '上传中…';
    });
    try {
      final outcome = await widget.scope.submitMediaTask(
        task: widget.task,
        file: file,
        contentType:
            mediaContentType(file.path, widget.task.submissionTypeCode),
      );
      if (mounted) {
        setState(() => _status = switch (outcome) {
              MediaSubmitOutcome.completed => '已提交，等家长看看',
              MediaSubmitOutcome.alreadySubmitted => '这个任务已经打卡过啦',
              MediaSubmitOutcome.queued => '当前离线，已保存，联网后自动上传',
              MediaSubmitOutcome.inProgress => '正在上传，请稍候',
              _ => '当前离线，已保存，联网后自动上传',
            });
        widget.onSubmitted();
        if (outcome != MediaSubmitOutcome.failed &&
            outcome != MediaSubmitOutcome.inProgress) {
          await Navigator.pushReplacement<void, void>(
            context,
            MaterialPageRoute(
              builder: (_) => SubmissionWaitingScreen(
                task: widget.task,
                file: file,
                scope: widget.scope,
              ),
            ),
          );
        }
      }
    } catch (_) {
      if (mounted) setState(() => _status = '上传失败，已保存，可稍后重试');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _retryUpload() async {
    if (!mounted) return;
    setState(() {
      _busy = true;
      _canRetryUpload = false;
      _status = '正在重试…';
    });
    await widget.scope.resetUploadBackoff();
    await widget.scope.retryPendingUploads(force: true);
    if (mounted) setState(() => _busy = false);
  }
}
