# Mobile App Module

## Purpose

`apps/mobile` is the Flutter client for both child and parent family usage. The app is organized around the complete household loop: child daily tasks, multimedia check-in entry, wish progress, room arrangement, and parent review/plan actions.

## Stack

- Flutter
- Dart
- Material 3
- Generated `wishpool_api` Dart client from OpenAPI

## Key Files

| File | Responsibility |
| --- | --- |
| `lib/main.dart` | App entry. |
| `lib/src/app.dart` | Theme and root app shell. |
| `lib/src/design/wishpool_theme.dart` | Flutter theme mapped from shared WishPool tokens. |
| `lib/src/features/mobile_home.dart` | Bottom navigation shell. |
| `lib/src/features/child_dashboard.dart` | Child task dashboard. |
| `lib/src/features/wish_screen.dart` | Wish card progress view. |
| `lib/src/features/room_screen.dart` | Room arrangement preview. |
| `lib/src/features/parent_dashboard.dart` | Parent review and plan view. |
| `lib/src/domain/wishpool_snapshot.dart` | Mobile home snapshot model with local fallback data. |
| `lib/src/infrastructure/runtime_config.dart` | Runtime Core API and realtime endpoint configuration. |
| `lib/src/infrastructure/wishpool_api_client.dart` | Lightweight HTTP JSON client for Core API probes and future repository calls. |
| `lib/src/data/wishpool_repository.dart` | Repository boundary that loads remote data when enabled and falls back locally. |
| `lib/src/data/sync_coordinator.dart` | WebSocket sync coordinator boundary for family event cursor connections. |
| `lib/src/data/upload_queue.dart` | Offline upload queue model for pending child submissions. |
| `lib/src/data/wishpool_scope.dart` | Dependency scope for runtime config, API client, repository, sync, and upload queue. |
| `test/widget_test.dart` | Flutter widget smoke test. |

## Runtime Notes

- Screens load through `WishPoolRepository`; local sample data remains the fallback when remote API loading is disabled or unavailable.
- `pubspec.yaml` references the generated Dart API client path under `packages/api-contracts/generated/dart`; the repository boundary is ready for generated client calls.
- `SyncCoordinator` and `UploadQueue` establish the realtime and offline media submission boundaries required by the complete mobile experience.

## Verification

Run `flutter test` when Flutter SDK is installed. In this workspace, root validation checks the source structure because the local machine does not expose a Flutter SDK.
