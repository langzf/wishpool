import 'package:flutter/material.dart';

import 'design/wishpool_theme.dart';
import 'features/auth_gate.dart';

class WishPoolApp extends StatelessWidget {
  const WishPoolApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'WishPool',
      debugShowCheckedModeBanner: false,
      theme: WishPoolTheme.light(),
      darkTheme: WishPoolTheme.dark(),
      home: const AuthGate(),
    );
  }
}
