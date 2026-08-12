import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:wishpool_mobile/src/app.dart';

void main() {
  testWidgets('WishPool mobile shell renders task and parent tabs', (tester) async {
    await tester.pumpWidget(const WishPoolApp());

    expect(find.text('把今天的小星光收进口袋'), findsOneWidget);
    expect(find.byIcon(Icons.check_circle_outline), findsOneWidget);
    expect(find.text('家长'), findsOneWidget);
  });
}
