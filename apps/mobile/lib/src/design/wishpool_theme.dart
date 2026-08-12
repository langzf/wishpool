import 'package:flutter/material.dart';

class WishPoolColors {
  static const primary = Color(0xFF2563EB);
  static const onPrimary = Color(0xFFFFFFFF);
  static const secondary = Color(0xFF059669);
  static const accent = Color(0xFFD97706);
  static const background = Color(0xFFF8FAFC);
  static const foreground = Color(0xFF0F172A);
  static const muted = Color(0xFFF1F5FD);
  static const border = Color(0xFFE4ECFC);
  static const destructive = Color(0xFFDC2626);
  static const darkBackground = Color(0xFF0F172A);
  static const darkSurface = Color(0xFF111827);
}

class WishPoolSpacing {
  static const xxs = 4.0;
  static const xs = 8.0;
  static const sm = 12.0;
  static const md = 16.0;
  static const lg = 24.0;
  static const xl = 32.0;
}

class WishPoolTheme {
  static ThemeData light() {
    final colorScheme = ColorScheme.fromSeed(
      seedColor: WishPoolColors.primary,
      brightness: Brightness.light,
      primary: WishPoolColors.primary,
      secondary: WishPoolColors.secondary,
      surface: Colors.white,
      error: WishPoolColors.destructive,
    );

    return _base(colorScheme).copyWith(
      scaffoldBackgroundColor: WishPoolColors.background,
      cardColor: Colors.white,
    );
  }

  static ThemeData dark() {
    final colorScheme = ColorScheme.fromSeed(
      seedColor: WishPoolColors.primary,
      brightness: Brightness.dark,
      primary: const Color(0xFF93C5FD),
      secondary: const Color(0xFF6EE7B7),
      surface: WishPoolColors.darkSurface,
      error: const Color(0xFFFCA5A5),
    );

    return _base(colorScheme).copyWith(
      scaffoldBackgroundColor: WishPoolColors.darkBackground,
      cardColor: WishPoolColors.darkSurface,
    );
  }

  static ThemeData _base(ColorScheme colorScheme) {
    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      textTheme: const TextTheme(
        headlineLarge: TextStyle(fontSize: 32, fontWeight: FontWeight.w800, height: 1.15),
        headlineMedium: TextStyle(fontSize: 24, fontWeight: FontWeight.w800, height: 1.2),
        titleLarge: TextStyle(fontSize: 20, fontWeight: FontWeight.w800, height: 1.25),
        bodyLarge: TextStyle(fontSize: 16, height: 1.5),
        bodyMedium: TextStyle(fontSize: 14, height: 1.5),
        labelLarge: TextStyle(fontSize: 14, fontWeight: FontWeight.w800),
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
          side: BorderSide(color: colorScheme.outlineVariant),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size(44, 44),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(44, 44),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        height: 72,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        indicatorShape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      ),
    );
  }
}
