import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:wishpool_mobile/src/data/upload_queue.dart';
import 'package:wishpool_mobile/src/media/media_capture_utils.dart';
import 'package:wishpool_mobile/src/data/upload_retry_policy.dart';

void main() {
  test('backoff grows monotonically and caps at five minutes', () {
    final values =
        List<int>.generate(10, (i) => uploadBackoffForAttempt(i + 1).inSeconds);
    expect(
        values, orderedEquals(<int>[2, 4, 8, 16, 32, 64, 128, 256, 300, 300]));
  });

  test('queue persists across instances and deduplicates a stable mutation id',
      () async {
    SharedPreferences.setMockInitialValues({});
    final file = File(
        '${Directory.systemTemp.path}${Platform.pathSeparator}t24-queue.bin');
    await file.writeAsBytes(<int>[1, 2, 3], flush: true);
    final first = UploadQueue();
    final upload = PendingUpload(
      clientMutationId: 'mobile-media-task-abc',
      taskInstanceId: 'task-abc',
      file: file,
      contentType: 'image/jpeg',
    );

    await first.enqueue(upload);
    await first.enqueue(
        upload.copyWith(state: PendingUploadState.failed, attempts: 1));

    final afterRestart = UploadQueue()..load();
    await afterRestart.load();
    expect(afterRestart.items, hasLength(1));
    expect(afterRestart.items.single.clientMutationId, upload.clientMutationId);
    expect(afterRestart.items.single.state, PendingUploadState.failed);
    expect(afterRestart.items.single.attempts, 1);

    expect(stableClientMutationId('task-abc', file),
        stableClientMutationId('task-abc', file));
    await file.delete();
  });

  test('queue exposes a visible terminal notice after a conflict', () {
    final queue = UploadQueue();
    expect(queue.noticeVersion, 0);
    queue.publishNotice('这个任务已经打卡过啦');
    expect(queue.lastNotice, '这个任务已经打卡过啦');
    expect(queue.noticeVersion, 1);
  });

  test('network recovery clears attempts and next attempt deadline', () async {
    SharedPreferences.setMockInitialValues({});
    final file = File(
        '${Directory.systemTemp.path}${Platform.pathSeparator}t25a-queue.bin');
    await file.writeAsBytes(<int>[1], flush: true);
    final queue = UploadQueue();
    await queue.enqueue(PendingUpload(
      clientMutationId: 'recovery-item',
      taskInstanceId: 'task',
      file: file,
      contentType: 'image/jpeg',
      state: PendingUploadState.failed,
      attempts: 8,
      nextAttemptAt: DateTime.now().add(const Duration(minutes: 5)),
      lastError: 'offline',
    ));
    await queue.resetBackoff();
    final item = queue.items.single;
    expect(item.attempts, 0);
    expect(item.nextAttemptAt, isNull);
    expect(item.state, PendingUploadState.pending);
    await file.delete();
  });

  test('network recovery event clears long backoff and forces upload retry',
      () async {
    SharedPreferences.setMockInitialValues({});
    final file = File(
        '${Directory.systemTemp.path}${Platform.pathSeparator}t35-long-backoff.bin');
    await file.writeAsBytes(<int>[1], flush: true);
    final queue = UploadQueue();
    final before = DateTime.utc(2026, 10, 9, 3, 0);
    await queue.enqueue(PendingUpload(
        clientMutationId: 'long-backoff',
        taskInstanceId: 'task',
        file: file,
        contentType: 'image/jpeg',
        state: PendingUploadState.failed,
        attempts: 7,
        nextAttemptAt: before.add(const Duration(minutes: 5)),
        lastError: 'offline'));
    expect(queue.items.single.nextAttemptAt!.difference(before).inMinutes, 5);
    var retryCalls = 0;
    final recoveryEvents = Stream<int>.fromIterable(<int>[1]);
    await for (final _ in recoveryEvents) {
      await UploadRecoveryHandler(queue, () async => retryCalls++)
          .onNetworkRecovered();
    }
    final after = queue.items.single;
    expect(after.state, PendingUploadState.pending);
    expect(after.attempts, 0);
    expect(after.nextAttemptAt, isNull);
    expect(retryCalls, 1);
    await file.delete();
  });

  test('network recovery probe is bounded with pending uploads', () {
    final clock = DateTime.utc(2026, 10, 9, 3);
    final backoff = NetworkRecoveryBackoff(now: () => clock);
    for (var i = 0; i < 12; i++) {
      backoff.recordFailure(hasPendingUploads: true);
    }
    expect(backoff.delayForQueue(hasPendingUploads: true),
        const Duration(seconds: 5));
    expect(backoff.delayForQueue(hasPendingUploads: true),
        lessThanOrEqualTo(const Duration(seconds: 5)));
    backoff.reset();
    expect(backoff.delayForQueue(hasPendingUploads: false),
        const Duration(seconds: 2));
  });
}
