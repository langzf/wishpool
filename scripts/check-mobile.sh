#!/usr/bin/env sh
set -eu

required_files="
apps/mobile/pubspec.yaml
apps/mobile/analysis_options.yaml
apps/mobile/lib/main.dart
apps/mobile/lib/src/app.dart
apps/mobile/lib/src/design/wishpool_theme.dart
apps/mobile/lib/src/features/mobile_home.dart
apps/mobile/lib/src/features/auth_gate.dart
apps/mobile/lib/src/features/child_dashboard.dart
apps/mobile/lib/src/features/notification_screen.dart
apps/mobile/lib/src/features/wish_screen.dart
apps/mobile/lib/src/features/room_screen.dart
apps/mobile/lib/src/features/parent_dashboard.dart
apps/mobile/lib/src/data/wishpool_repository.dart
apps/mobile/lib/src/data/auth_repository.dart
apps/mobile/lib/src/data/sync_coordinator.dart
apps/mobile/lib/src/data/upload_queue.dart
apps/mobile/lib/src/data/wishpool_scope.dart
apps/mobile/lib/src/infrastructure/runtime_config.dart
apps/mobile/lib/src/infrastructure/wishpool_api_client.dart
apps/mobile/lib/src/infrastructure/auth_session_store.dart
apps/mobile/lib/src/auth/session.dart
apps/mobile/lib/src/domain/wishpool_snapshot.dart
apps/mobile/test/widget_test.dart
"

for file in $required_files; do
  test -f "$file"
done

if command -v flutter >/dev/null 2>&1; then
  (cd apps/mobile && flutter test)
else
  rg -n "class WishPoolApp|class AuthGate|class MobileHomeScreen|class NotificationScreen|NavigationDestination|wishpool_api|WishPoolRepository|AuthRepository|AuthSessionStore|SyncCoordinator|UploadQueue" apps/mobile >/dev/null
  echo "validated Flutter mobile source structure; Flutter SDK not available"
fi
