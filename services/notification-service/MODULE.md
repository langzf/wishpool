# Notification Service Module

## Purpose

`services/notification-service` dispatches WishPool notification events from Core API. Core API owns notification facts and preferences; this service claims pending rows, applies preference/channel rules, and marks dispatch results through internal endpoints.

## Stack

- Kotlin/JVM
- Ktor Netty
- Java HTTP client
- Jackson Kotlin

## Key Files

| File | Responsibility |
| --- | --- |
| `src/main/kotlin/com/wishpool/notification/NotificationServiceApplication.kt` | Ktor server entry, health endpoint, manual dispatch route, and polling scheduler. |
| `src/main/kotlin/com/wishpool/notification/NotificationConfig.kt` | Environment-driven runtime configuration. |
| `src/main/kotlin/com/wishpool/notification/CoreApiClient.kt` | Internal-token Core API client for notification claim and dispatch-result routes. |
| `src/main/kotlin/com/wishpool/notification/NotificationDispatcher.kt` | Applies local channel/preference dispatch decisions. |
| `src/main/kotlin/com/wishpool/notification/Dtos.kt` | Notification dispatch DTOs. |
| `src/test/kotlin/com/wishpool/notification/NotificationConfigTests.kt` | Local defaults and environment override tests. |

## Runtime Contract

- `GET /health` returns dispatcher readiness.
- `POST /internal/notifications/dispatch-once` requires `X-Internal-Token`, then claims and dispatches one batch.
- `WISHPOOL_NOTIFICATION_DISPATCH_ENABLED=false` marks eligible inbox notifications as sent without calling external push providers, matching self-hosted local use.
- Hosted provider adapters can be added behind `NotificationDispatcher` without changing Core API notification facts.

Run:

```bash
./gradlew -p services/notification-service test
```
