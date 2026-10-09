import 'package:flutter/material.dart';
import 'package:just_audio/just_audio.dart';
import 'package:video_player/video_player.dart';

import '../design/wishpool_theme.dart';
import '../domain/wishpool_snapshot.dart';
import '../shared/friendly_error.dart';

class MemoryTimelineScreen extends StatelessWidget {
  const MemoryTimelineScreen({super.key, required this.snapshot, this.onFeature});

  final WishPoolSnapshot snapshot;
  final Future<void> Function(String memoryId)? onFeature;

  @override
  Widget build(BuildContext context) {
    final memories = snapshot.memoryTimeline;
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 120),
      children: [
        Text('成长回忆', style: Theme.of(context).textTheme.headlineLarge),
        const SizedBox(height: 6),
        const Text('每一周的努力，都会变成一张闪闪发光的回忆卡。'),
        const SizedBox(height: WishPoolSpacing.lg),
        if (memories.isEmpty)
          const _EmptyMemoryCard()
        else
          ...memories.map((memory) => _WeekCard(memory: memory, onFeature: onFeature)),
      ],
    );
  }
}

class _EmptyMemoryCard extends StatelessWidget {
  const _EmptyMemoryCard();

  @override
  Widget build(BuildContext context) {
    return const Card(
      child: Padding(
        padding: EdgeInsets.all(28),
        child: Column(
          children: [
            Icon(Icons.auto_stories_outlined,
                size: 52, color: WishPoolColors.primary),
            SizedBox(height: 12),
            Text('还没有生成回忆卡'),
            SizedBox(height: 4),
            Text('完成一周任务并经过家长确认后，这里会出现真实记录。'),
          ],
        ),
      ),
    );
  }
}

class _WeekCard extends StatelessWidget {
  const _WeekCard({required this.memory, this.onFeature});

  final MemoryTimelineItemData memory;
  final Future<void> Function(String memoryId)? onFeature;

  @override
  Widget build(BuildContext context) {
    final preview = memory.items.take(3).toList();
    return Card(
      margin: const EdgeInsets.only(bottom: WishPoolSpacing.md),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => Navigator.push<void>(
          context,
          MaterialPageRoute(builder: (_) => MemoryDetailScreen(memory: memory, onFeature: onFeature)),
        ),
        child: Padding(
          padding: const EdgeInsets.all(WishPoolSpacing.md),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: WishPoolColors.muted,
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: const Icon(Icons.auto_stories_rounded,
                        color: WishPoolColors.primary),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(memory.title,
                            style: Theme.of(context).textTheme.titleLarge),
                        const SizedBox(height: 4),
                        Text(
                            '第 ${memory.weekId} 周 · ${_status(memory.status)}'),
                      ],
                    ),
                  ),
                  const Icon(Icons.chevron_right_rounded),
                ],
              ),
              const SizedBox(height: 14),
              Text(memory.summary,
                  maxLines: 2, overflow: TextOverflow.ellipsis),
              if (preview.isNotEmpty) ...[
                const SizedBox(height: 14),
                SizedBox(
                  height: 78,
                  child: Row(
                    children: preview
                        .map((item) => Expanded(
                              child: Padding(
                                padding: const EdgeInsets.only(right: 8),
                                child: _MemoryPreview(item: item),
                              ),
                            ))
                        .toList(),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  static String _status(String status) => switch (status) {
        'generated' => '已生成',
        'exported' => '已导出',
        'generating' => '生成中',
        _ => '已记录',
      };
}

class _MemoryPreview extends StatelessWidget {
  const _MemoryPreview({required this.item});

  final MemoryMediaItemData item;

  @override
  Widget build(BuildContext context) {
    final isImage = item.kind.contains('image') || item.kind.contains('photo');
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: item.thumbnailUrl != null && isImage
          ? Image.network(item.thumbnailUrl!, fit: BoxFit.cover)
          : Container(
              color: WishPoolColors.muted,
              child: Center(child: Icon(_icon(item.kind), size: 30)),
            ),
    );
  }

  static IconData _icon(String kind) {
    if (kind.contains('audio') || kind.contains('voice')) {
      return Icons.mic_rounded;
    }
    if (kind.contains('video')) {
      return Icons.videocam_rounded;
    }
    if (kind.contains('image') || kind.contains('photo')) {
      return Icons.photo_rounded;
    }
    return Icons.check_circle_outline_rounded;
  }
}

class MemoryDetailScreen extends StatelessWidget {
  const MemoryDetailScreen({super.key, required this.memory, this.onFeature});

  final MemoryTimelineItemData memory;
  final Future<void> Function(String memoryId)? onFeature;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('本周回忆详情')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 32),
        children: [
          Text(memory.title, style: Theme.of(context).textTheme.headlineMedium),
          const SizedBox(height: 8),
          Text(memory.summary),
          const SizedBox(height: 18),
          if (memory.items.isEmpty)
            const Text('这一周暂时没有可展开的媒体记录。')
          else
            ...memory.items.map((item) => _MemoryItemCard(item: item)),
          const SizedBox(height: 14),
          OutlinedButton.icon(
            onPressed: onFeature == null
                ? null
                : () async {
                    try {
                      await onFeature!(memory.id);
                    } catch (_) {
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('精选入屋失败，请稍后重试')));
                      }
                    }
                  },
            icon: const Icon(Icons.home_work_outlined),
            label: const Text('精选入屋'),
          ),
        ],
      ),
    );
  }

  // ignore: unused_element
  void _showFeatureGap(BuildContext context) {
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
      content: Text('当前服务端还没有“精选入屋”接口，未修改小屋数据。'),
    ));
  }
}

class _MemoryItemCard extends StatelessWidget {
  const _MemoryItemCard({required this.item});

  final MemoryMediaItemData item;

  @override
  Widget build(BuildContext context) {
    final kind = item.kind.toLowerCase();
    final isAudio = kind == 'audio';
    final isVideo = kind == 'video';
    final isImage = kind == 'photo';
    final isApprovedTask = kind == 'approved_task';
    final imageUrl = item.url ?? item.thumbnailUrl;
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              Icon(isAudio
                  ? Icons.mic_rounded
                  : isVideo
                      ? Icons.videocam_rounded
                      : isImage
                          ? Icons.photo_rounded
                          : Icons.check_circle_outline_rounded),
              const SizedBox(width: 8),
              Expanded(
                  child: Text(item.title,
                      style: Theme.of(context).textTheme.titleMedium)),
              if (item.scheduledDate != null) Text(item.scheduledDate!),
            ]),
            if (item.note != null) ...[
              const SizedBox(height: 8),
              Text(item.note!),
            ],
            if (isImage && imageUrl != null) ...[
              const SizedBox(height: 10),
              ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: Image.network(imageUrl,
                    height: 170, width: double.infinity, fit: BoxFit.cover),
              ),
            ] else if (isAudio && item.url != null)
              AudioPlayerWidget(url: item.url!, durationSec: item.durationSec),
            if (isVideo && item.url != null) ...[
              _VideoPlayer(url: item.url!),
            ],
            if (!isApprovedTask && !isImage && !isAudio && !isVideo) ...[
              const Padding(
                padding: EdgeInsets.only(top: 10),
                child: Text('这条记录没有可播放媒体。'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

typedef AudioPlayerFactory = AudioPlayerController Function();
abstract class AudioPlayerController {
  Stream<PlayerState> get playerStateStream;
  Stream<Duration> get positionStream;
  Duration? get duration;
  bool get playing;
  ProcessingState get processingState;
  Future<void> load(String url);
  Future<void> play();
  Future<void> pause();
  Future<void> seek(Duration position);
  Future<void> dispose();
}
class JustAudioPlayerController implements AudioPlayerController {
  JustAudioPlayerController() : _player = AudioPlayer();
  final AudioPlayer _player;
  @override Stream<PlayerState> get playerStateStream => _player.playerStateStream;
  @override Stream<Duration> get positionStream => _player.positionStream;
  @override Duration? get duration => _player.duration;
  @override bool get playing => _player.playing;
  @override ProcessingState get processingState => _player.processingState;
  @override Future<void> load(String url) async { await _player.setUrl(url); }
  @override Future<void> play() => _player.play();
  @override Future<void> pause() => _player.pause();
  @override Future<void> seek(Duration position) => _player.seek(position);
  @override Future<void> dispose() => _player.dispose();
}
AudioPlayerFactory audioPlayerFactory = JustAudioPlayerController.new;

class AudioPlayerWidget extends StatefulWidget {
  const AudioPlayerWidget({super.key, required this.url, this.durationSec, this.playerFactory});
  final String url;
  final int? durationSec;
  final AudioPlayerFactory? playerFactory;

  @override
  State<AudioPlayerWidget> createState() => _AudioPlayerState();
}

class _AudioPlayerState extends State<AudioPlayerWidget> {
  late final AudioPlayerController _player = (widget.playerFactory ?? audioPlayerFactory)();
  bool _loading = false;
  String? _error;

  @override
  void initState() { super.initState(); _loadMetadata(); }

  Future<void> _loadMetadata() async {
    if (mounted) setState(() { _loading = true; _error = null; });
    try { await _player.load(widget.url); if (mounted) setState(() {}); }
    catch (error) { if (mounted) setState(() => _error = childFriendlyError(error)); }
    finally { if (mounted) setState(() => _loading = false); }
  }

  @override
  void dispose() {
    _player.dispose();
    super.dispose();
  }

  Future<void> _toggle() async {
    setState(() => _loading = true);
    try {
      if (_error != null || _player.processingState == ProcessingState.idle) await _loadMetadata();
      if (_error != null) return;
      if (_player.playing) {
        await _player.pause();
      } else {
        await _player.play();
      }
    } catch (error) {
      if (mounted) setState(() => _error = childFriendlyError(error));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(children: [
      if (_error != null) Row(children: [Expanded(child: Text(_error!)), TextButton(onPressed: _loadMetadata, child: const Text('重试'))]),
      StreamBuilder<PlayerState>(
      stream: _player.playerStateStream,
      builder: (context, snapshot) => Row(
        children: [
          IconButton(
            onPressed: _loading ? null : _toggle,
            icon: Icon(snapshot.data?.playing == true
                ? Icons.pause_circle_filled
                : Icons.play_circle_fill),
          ),
          const Text('播放语音'),
        ],
      ),
      ),
      StreamBuilder<Duration>(
        stream: _player.positionStream,
        builder: (context, snapshot) {
          final position = snapshot.data ?? Duration.zero;
          final duration = _player.duration ?? (widget.durationSec == null ? null : Duration(seconds: widget.durationSec!));
          final max = duration?.inMilliseconds.toDouble() ?? 1.0;
          return Column(children: [
            Slider(
              value: position.inMilliseconds.clamp(0, max.toInt()).toDouble(),
              max: max,
              onChanged: duration == null
                  ? null
                  : (value) => _player.seek(Duration(milliseconds: value.toInt())),
            ),
            Align(
              alignment: Alignment.centerRight,
              child: Text('${formatAudioDuration(duration == null ? null : position)} / ${formatAudioDuration(duration)}'),
            ),
          ]);
        },
      ),
    ]);
  }

}

String formatAudioDuration(Duration? value) {
  if (value == null) return '--:--';
  if (value.inHours > 0) return '${value.inHours.toString().padLeft(2, '0')}:${(value.inMinutes % 60).toString().padLeft(2, '0')}:${(value.inSeconds % 60).toString().padLeft(2, '0')}';
  return '${value.inMinutes.toString().padLeft(2, '0')}:${(value.inSeconds % 60).toString().padLeft(2, '0')}';
}

class _VideoPlayer extends StatefulWidget {
  const _VideoPlayer({required this.url});
  final String url;

  @override
  State<_VideoPlayer> createState() => _VideoPlayerState();
}

class _VideoPlayerState extends State<_VideoPlayer> {
  late final VideoPlayerController _controller =
      VideoPlayerController.networkUrl(Uri.parse(widget.url));
  String? _error;

  @override
  void initState() {
    super.initState();
    _controller.initialize().then((_) {
      if (mounted) setState(() {});
    }).catchError((_) {
      if (mounted) setState(() => _error = '这条记录暂时播不了，稍后再试');
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_error != null) {
      return Padding(padding: const EdgeInsets.all(12), child: Text(_error!));
    }
    if (!_controller.value.isInitialized) {
      return const Padding(padding: EdgeInsets.all(12), child: Text('视频加载中…'));
    }
    return Column(
      children: [
        AspectRatio(
            aspectRatio: _controller.value.aspectRatio,
            child: VideoPlayer(_controller)),
        TextButton.icon(
          onPressed: () async {
            try {
              if (_controller.value.isPlaying) {
                await _controller.pause();
              } else {
                await _controller.play();
              }
              if (mounted) setState(() {});
            } catch (_) {
              if (mounted) setState(() => _error = '这条记录暂时播不了，稍后再试');
            }
          },
          icon: Icon(
              _controller.value.isPlaying ? Icons.pause : Icons.play_arrow),
          label: Text(_controller.value.isPlaying ? '暂停视频' : '播放视频'),
        ),
      ],
    );
  }
}
