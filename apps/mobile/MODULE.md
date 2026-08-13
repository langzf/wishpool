# Mobile App Module

## Purpose

`apps/mobile` is the Flutter client for both child and parent family usage. The app is organized around the complete household loop: child daily tasks, multimedia check-in entry, wish progress, room arrangement, and parent review/plan actions.

## Stack

- Flutter
- Dart
- Material 3
- `file_picker`, `mime`, and `shared_preferences`
- Generated `wishpool_api` Dart client from OpenAPI

## Key Files

| File | Responsibility |
| --- | --- |
| `lib/main.dart` | App entry. |
| `lib/src/app.dart` | Theme and root app shell. |
| `lib/src/auth/session.dart` | Persistable session model with parent and child-device role context. |
| `lib/src/design/wishpool_theme.dart` | Flutter theme mapped from shared WishPool tokens. |
| `lib/src/features/auth_gate.dart` | Session restore, phone login, child pairing, and first family setup flow. |
| `lib/src/features/mobile_home.dart` | Bottom navigation shell, realtime refresh wiring, and child check-in sheet. |
| `lib/src/features/child_dashboard.dart` | Child task dashboard with dynamic task summary and empty state. |
| `lib/src/features/notification_screen.dart` | Inbox notifications and parent notification preference controls. |
| `lib/src/features/wish_screen.dart` | Wish card progress view with real fragment counts. |
| `lib/src/features/room_screen.dart` | Room state preview with percentage-based arrangement save action. |
| `lib/src/features/parent_dashboard.dart` | Parent review, daily task adjustment, and weekly plan view. |
| `lib/src/domain/wishpool_snapshot.dart` | Mobile home snapshot model with wish fragments, plan rules, notifications, and local fallback data. |
| `lib/src/infrastructure/auth_session_store.dart` | SharedPreferences session persistence. |
| `lib/src/infrastructure/runtime_config.dart` | Runtime Core API and realtime endpoint configuration. |
| `lib/src/infrastructure/wishpool_api_client.dart` | Lightweight HTTP JSON client with bearer auth, raw JSON, JSON POST, and presigned file upload support. |
| `lib/src/data/auth_repository.dart` | Phone-code login, child-device pairing, token refresh, `/me` enrichment, and family bootstrap commands. |
| `lib/src/data/wishpool_repository.dart` | Repository boundary for child/parent home contexts, review commands, submission upload flow, task skip/postpone, room arrangement, inbox notifications, and preferences. |
| `lib/src/data/sync_coordinator.dart` | WebSocket sync coordinator with reconnect, cursor persistence, and family event stream. |
| `lib/src/data/upload_queue.dart` | Offline upload queue model for pending child submissions. |
| `lib/src/data/wishpool_scope.dart` | Dependency scope for runtime config, API client, repository, sync, and upload queue. |
| `test/widget_test.dart` | Flutter widget smoke test. |

## Runtime Notes

- `AuthGate` restores saved sessions, refreshes tokens, supports parent phone login and child-device pairing, and creates a first family/child when the parent has no household context.
- Screens load through `WishPoolRepository`; local sample data remains the fallback when remote API loading is disabled or unavailable, while valid empty arrays from Core API render as empty product states.
- Child-device sessions only load child home context and hide parent review navigation; parent sessions also load the parent dashboard context.
- Task submission uses `file_picker` and `mime` for media selection, creates `/media/upload-sessions`, uploads to the presigned URL, finalizes media, then posts `/submissions`.
- Parent review actions call `/reviews`; daily task adjustments call `/tasks/{taskId}/skip` and `/tasks/{taskId}/postpone`; room arrangement calls `/room/items/{itemId}/arrange`.
- `pubspec.yaml` references the generated Dart API client path under `packages/api-contracts/generated/dart`; the repository boundary is ready for generated client calls.
- `SyncCoordinator` keeps the family cursor in local storage and refreshes the mobile home when relevant family events arrive.
- `UploadQueue` establishes the offline media submission boundary required by the complete mobile experience.

## Verification

Run `flutter test` when Flutter SDK is installed. In this workspace, root validation checks the source structure because the local machine does not expose a Flutter SDK.
