import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:wishpool_mobile/src/app.dart';

void main() {
  testWidgets('WishPool mobile auth gate renders parent and child entries', (tester) async {
    SharedPreferences.setMockInitialValues({});

    await tester.pumpWidget(const WishPoolApp());

    expect(find.text('WishPool'), findsOneWidget);
    expect(find.text('家长手机号登录'), findsOneWidget);
    expect(find.text('家长'), findsOneWidget);
    expect(find.text('儿童'), findsOneWidget);
    expect(find.byIcon(Icons.sms_outlined), findsOneWidget);
  });
}
