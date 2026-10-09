import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:wishpool_mobile/src/shared/friendly_error.dart';

void main() {
  testWidgets('failure hint renders child-friendly text', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Text('采集失败，${childFriendlyError(Exception('Socket 500'))}'),
        ),
      ),
    );

    final rendered = tester.widget<Text>(find.byType(Text)).data!;
    expect(rendered, contains(RegExp(r'[\u4e00-\u9fff]')));
    expect(
      rendered,
      isNot(contains(RegExp(r'Exception|WishPool|Dio|Socket|\b(409|500|401|403)\b'))),
    );
  });
}
