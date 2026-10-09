import 'dart:convert';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum PendingUploadState { pending, uploading, failed }

const maxUploadAttempts = 8;

/// Backoff for the connectivity probe. A non-empty upload queue must be
/// checked frequently so a long upload backoff cannot hide network recovery.
class NetworkRecoveryBackoff {
  NetworkRecoveryBackoff({this.now = _systemNow});

  static DateTime _systemNow() => DateTime.now();
  final DateTime Function() now;
  Duration _delay = const Duration(seconds: 2);

  Duration get delay => _delay;

  void reset() => _delay = const Duration(seconds: 2);

  Duration delayForQueue({required bool hasPendingUploads}) {
    if (hasPendingUploads) return const Duration(seconds: 5);
    return _delay;
  }

  void recordFailure({required bool hasPendingUploads}) {
    if (hasPendingUploads) {
      _delay = const Duration(seconds: 5);
      return;
    }
    final nextSeconds = (_delay.inSeconds * 2).clamp(2, 60);
    _delay = Duration(seconds: nextSeconds);
  }
}

/// Adapter used by connectivity/foreground event sources.
class UploadRecoveryHandler {
  const UploadRecoveryHandler(this.queue, this.retry);
  final UploadQueue queue;
  final Future<void> Function() retry;

  Future<void> onNetworkRecovered() async {
    await queue.resetBackoff();
    await retry();
  }
}

class UploadQueue extends ChangeNotifier {
  UploadQueue({SharedPreferences? preferences}) : _preferences = preferences;

  static const _storageKey = 'wishpool.mobile.upload_queue.v1';
  final SharedPreferences? _preferences;
  final List<PendingUpload> _items = [];
  Future<void>? _loadFuture;
  String? _lastNotice;
  int _noticeVersion = 0;

  List<PendingUpload> get items => List.unmodifiable(_items);
  int get pendingCount =>
      _items.where((item) => item.state != PendingUploadState.uploading).length;
  int get uploadingCount =>
      _items.where((item) => item.state == PendingUploadState.uploading).length;
  int get failedCount =>
      _items.where((item) => item.state == PendingUploadState.failed).length;

  String? get lastNotice => _lastNotice;
  int get noticeVersion => _noticeVersion;

  void publishNotice(String notice) {
    _lastNotice = notice;
    _noticeVersion++;
    notifyListeners();
  }

  Future<void> load() => _loadFuture ??= _loadFromDisk();

  Future<void> enqueue(PendingUpload upload) async {
    await load();
    _items.removeWhere(
        (item) => item.clientMutationId == upload.clientMutationId);
    _items.add(upload);
    await _save();
    notifyListeners();
  }

  Future<void> update(PendingUpload upload) async {
    await load();
    final index = _items
        .indexWhere((item) => item.clientMutationId == upload.clientMutationId);
    if (index < 0) return;
    _items[index] = upload;
    await _save();
    notifyListeners();
  }

  Future<void> markCompleted(String clientMutationId) async {
    await load();
    _items.removeWhere((item) => item.clientMutationId == clientMutationId);
    await _save();
    notifyListeners();
  }

  /// Network recovery must not wait for an old offline backoff deadline.
  Future<void> resetBackoff() async {
    await load();
    var changed = false;
    for (var i = 0; i < _items.length; i++) {
      final item = _items[i];
      if (item.attempts == 0 &&
          item.nextAttemptAt == null &&
          item.lastError == null &&
          item.state == PendingUploadState.pending) {
        continue;
      }
      _items[i] = item.copyWith(
        state: PendingUploadState.pending,
        attempts: 0,
        clearNextAttempt: true,
        clearLastError: true,
      );
      changed = true;
    }
    if (changed) {
      await _save();
      notifyListeners();
    }
  }

  Future<void> retryManually(String clientMutationId) async {
    await load();
    final index =
        _items.indexWhere((item) => item.clientMutationId == clientMutationId);
    if (index < 0) return;
    _items[index] = _items[index].copyWith(
      state: PendingUploadState.pending,
      attempts: 0,
      clearNextAttempt: true,
      clearLastError: true,
    );
    await _save();
    notifyListeners();
  }

  Future<void> _loadFromDisk() async {
    final preferences = _preferences ?? await SharedPreferences.getInstance();
    final raw = preferences.getString(_storageKey);
    if (raw == null || raw.isEmpty) return;
    try {
      final decoded = jsonDecode(raw);
      if (decoded is! List) return;
      _items
        ..clear()
        ..addAll(decoded.whereType<Map>().map(PendingUpload.fromJson));
    } catch (_) {
      _items.clear();
    }
  }

  Future<void> _save() async {
    final preferences = _preferences ?? await SharedPreferences.getInstance();
    await preferences.setString(
        _storageKey, jsonEncode(_items.map((item) => item.toJson()).toList()));
  }
}

class PendingUpload {
  const PendingUpload({
    required this.clientMutationId,
    required this.taskInstanceId,
    required this.file,
    required this.contentType,
    this.mediaAssetId,
    this.state = PendingUploadState.pending,
    this.attempts = 0,
    this.nextAttemptAt,
    this.lastError,
    this.uploadUrl,
    this.uploadUrlExpiresAt,
    this.sessionRequestedAt,
  });

  final String clientMutationId;
  final String taskInstanceId;
  final File file;
  final String contentType;
  final String? mediaAssetId;
  final PendingUploadState state;
  final int attempts;
  final DateTime? nextAttemptAt;
  final String? lastError;
  final String? uploadUrl;
  final DateTime? uploadUrlExpiresAt;
  final DateTime? sessionRequestedAt;

  PendingUpload copyWith({
    String? mediaAssetId,
    PendingUploadState? state,
    int? attempts,
    DateTime? nextAttemptAt,
    String? lastError,
    bool clearNextAttempt = false,
    bool clearLastError = false,
    String? uploadUrl,
    DateTime? uploadUrlExpiresAt,
    DateTime? sessionRequestedAt,
  }) =>
      PendingUpload(
        clientMutationId: clientMutationId,
        taskInstanceId: taskInstanceId,
        file: file,
        contentType: contentType,
        mediaAssetId: mediaAssetId ?? this.mediaAssetId,
        state: state ?? this.state,
        attempts: attempts ?? this.attempts,
        nextAttemptAt:
            clearNextAttempt ? null : (nextAttemptAt ?? this.nextAttemptAt),
        lastError: clearLastError ? null : (lastError ?? this.lastError),
        uploadUrl: uploadUrl ?? this.uploadUrl,
        uploadUrlExpiresAt: uploadUrlExpiresAt ?? this.uploadUrlExpiresAt,
        sessionRequestedAt: sessionRequestedAt ?? this.sessionRequestedAt,
      );

  Map<String, Object?> toJson() => <String, Object?>{
        'clientMutationId': clientMutationId,
        'taskInstanceId': taskInstanceId,
        'filePath': file.path,
        'contentType': contentType,
        'mediaAssetId': mediaAssetId,
        'state': state.name,
        'attempts': attempts,
        'nextAttemptAt': nextAttemptAt?.toIso8601String(),
        'lastError': lastError,
        'uploadUrl': uploadUrl,
        'uploadUrlExpiresAt': uploadUrlExpiresAt?.toIso8601String(),
        'sessionRequestedAt': sessionRequestedAt?.toIso8601String(),
      };

  factory PendingUpload.fromJson(Map json) {
    final stateName = json['state']?.toString();
    return PendingUpload(
      clientMutationId: json['clientMutationId'].toString(),
      taskInstanceId: json['taskInstanceId'].toString(),
      file: File(json['filePath'].toString()),
      contentType: json['contentType'].toString(),
      mediaAssetId: json['mediaAssetId']?.toString(),
      state: PendingUploadState.values.firstWhere(
        (value) => value.name == stateName,
        orElse: () => PendingUploadState.pending,
      ),
      attempts: (json['attempts'] as num?)?.toInt() ?? 0,
      nextAttemptAt: DateTime.tryParse(json['nextAttemptAt']?.toString() ?? ''),
      lastError: json['lastError']?.toString(),
      uploadUrl: json['uploadUrl']?.toString(),
      uploadUrlExpiresAt:
          DateTime.tryParse(json['uploadUrlExpiresAt']?.toString() ?? ''),
      sessionRequestedAt:
          DateTime.tryParse(json['sessionRequestedAt']?.toString() ?? ''),
    );
  }
}
