import 'dart:async';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:wishpool_mobile/src/infrastructure/wishpool_api_client.dart';
import 'package:wishpool_mobile/src/shared/friendly_error.dart';

void main() {
  test('all child-facing exception mappings are Chinese and sanitized', () {
    for (final error in <Object>[
      const WishPoolApiException(409, 'conflict'),
      const WishPoolApiException(401, 'unauthorized'),
      const WishPoolApiException(403, 'forbidden'),
      const WishPoolApiException(408, 'timeout'),
      const WishPoolApiException(429, 'rate limited'),
      const WishPoolApiException(500, 'server'),
      TimeoutException('timeout'),
      const SocketException('offline'),
      Exception('raw'),
      StateError('unknown'),
    ]) {
      final message = childFriendlyError(error);
      expect(message,
          isNot(contains(RegExp(r'Exception|409|500|WishPool|Dio|Socket'))));
      expect(message, contains(RegExp(r'[\u4e00-\u9fff]')));
    }
    expect(
        childFriendlyError(const WishPoolApiException(409, 'x')), '这个任务已经打卡过啦');
  });
}
