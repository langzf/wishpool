import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:wishpool_mobile/src/media/media_capture_utils.dart';

void main() {
  test('媒体大小上限返回中文提示', () {
    final file = File(
        '${Directory.systemTemp.path}/t23-limit-${DateTime.now().microsecondsSinceEpoch}');
    file.writeAsBytesSync(List<int>.filled(maxMediaBytes + 1, 0));
    expect(mediaLimitMessage(file), contains('文件过大'));
    file.deleteSync();
  });

  test('同一任务和同一文件生成稳定幂等 key', () {
    final file = File('${Directory.systemTemp.path}/t23-key.txt')
      ..writeAsStringSync('x');
    expect(stableClientMutationId('task-1', file),
        stableClientMutationId('task-1', file));
    expect(stableClientMutationId('task-1', file),
        isNot(stableClientMutationId('task-2', file)));
    file.deleteSync();
  });
}
