import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:wishpool_mobile/src/data/upload_queue.dart';
import 'package:wishpool_mobile/src/media/media_capture_utils.dart';

void main() {
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
}
