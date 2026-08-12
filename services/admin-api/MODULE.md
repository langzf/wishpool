# Admin API Module

## Purpose

`services/admin-api` is the local administration API used by the WishPool admin web app. It keeps the admin surface separate from family-facing Core API routes while delegating facts and audit writes to Core API internal endpoints.

## Stack

- Kotlin/JVM
- Ktor Netty
- Java HTTP client
- Jackson Kotlin

## Key Files

| File | Responsibility |
| --- | --- |
| `src/main/kotlin/com/wishpool/admin/AdminApiApplication.kt` | Ktor server entry, health endpoint, and admin route definitions. |
| `src/main/kotlin/com/wishpool/admin/AdminConfig.kt` | Environment-driven runtime configuration, including Core API internal token and local admin token. |
| `src/main/kotlin/com/wishpool/admin/CoreApiClient.kt` | Internal-token Core API client for dashboard, family, privacy, audit, and media access routes. |
| `src/main/kotlin/com/wishpool/admin/Dtos.kt` | Admin API request and response DTOs. |
| `src/test/kotlin/com/wishpool/admin/AdminConfigTests.kt` | Local defaults and environment override tests. |

## Runtime Contract

- `GET /health` checks Core API reachability.
- Admin routes require `X-Admin-Token`.
- `GET /admin/dashboard` returns local system counters.
- `GET /admin/families` returns family metadata.
- `GET /admin/privacy-requests` returns privacy request work queue rows.
- `GET /admin/audit-logs` returns audit log rows.
- `POST /admin/media-access-grants` creates an audited short-lived media access grant through Core API.

Run:

```bash
./gradlew -p services/admin-api test
```
