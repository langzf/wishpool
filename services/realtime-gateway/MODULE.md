# Realtime Gateway Module

## Purpose

`services/realtime-gateway/` contains the Kotlin/Ktor service that maintains WishPool realtime synchronization connections. It accepts WebSocket and SSE clients, authenticates them through the Core API, pulls authorized family events by cursor, emits ordered realtime envelopes, and records active connection state in Redis.

## Important Files

| Path | Responsibility |
| --- | --- |
| `build.gradle.kts` | Ktor, Lettuce, Jackson, Kotlin, and test dependencies for the standalone gateway. |
| `src/main/kotlin/com/wishpool/realtime/RealtimeGatewayApplication.kt` | Application entry point, Ktor WebSocket installation, health/version routes, `/realtime`, and `/realtime/sse`. |
| `src/main/kotlin/com/wishpool/realtime/RealtimeConfig.kt` | Environment-backed service configuration for port, Core API URL, Redis URI, polling, heartbeat, and sync limits. |
| `src/main/kotlin/com/wishpool/realtime/CoreApiClient.kt` | JDK HTTP client that validates Bearer tokens with `/me` and pulls events from `/sync/pull`. |
| `src/main/kotlin/com/wishpool/realtime/RealtimeSessionManager.kt` | Connection authentication, WebSocket/SSE loops, heartbeat, client `ping`/`sync.pull` handling, and event emission. |
| `src/main/kotlin/com/wishpool/realtime/ConnectionIndex.kt` | Redis connection index for family connection sets and per-connection metadata, plus no-op implementation for tests or isolated runs. |
| `src/main/kotlin/com/wishpool/realtime/Dtos.kt` | Shared realtime, Core API, and sync DTOs. |
| `src/test/kotlin/com/wishpool/realtime/RealtimeConfigTests.kt` | Unit coverage for local defaults and environment override behavior. |
| `src/test/kotlin/com/wishpool/realtime/RealtimeJsonTests.kt` | Unit coverage for `/sync/pull`, `/me`, and realtime envelope JSON shapes. |

## Runtime Behavior

- Clients connect to `GET /realtime?familyId={familyId}&afterSeq={seq}` by WebSocket or `GET /realtime/sse?familyId={familyId}&afterSeq={seq}` by SSE.
- Bearer tokens may be passed by `Authorization: Bearer ...`; SSE and browser WebSocket clients may also use `accessToken` query parameter.
- The gateway calls Core API `/me` to verify that the authenticated member is active in the requested family.
- Event reads go through Core API `/sync/pull`, so parent and child-device visibility, including `sync.redacted` placeholders, stays centralized in `core-api`.
- Redis keys use `wishpool:realtime:family:{familyId}:connections` and `wishpool:realtime:connection:{connectionId}` with a renewable TTL.
- WebSocket clients receive `realtime.connected`, `family.event`, `realtime.heartbeat`, `realtime.pong`, and `realtime.error` envelopes.
- SSE clients receive named events using the same JSON envelopes in `data`.

## Configuration

| Variable | Default | Meaning |
| --- | --- | --- |
| `WISHPOOL_REALTIME_PORT` / `PORT` | `8081` | Gateway HTTP/WebSocket port. |
| `WISHPOOL_CORE_API_BASE_URL` | `http://localhost:8080` | Base URL for Core API authentication and sync calls. |
| `WISHPOOL_REDIS_URI` / `REDIS_URL` | `redis://localhost:6379` | Redis connection URI for active connection indexes. |
| `WISHPOOL_REDIS_ENABLED` | `true` | Enables the Redis connection index. |
| `WISHPOOL_REALTIME_POLL_INTERVAL_MS` | `1000` | Family event pull cadence per active connection. |
| `WISHPOOL_REALTIME_HEARTBEAT_INTERVAL_SECONDS` | `15` | Heartbeat cadence when no events are available. |
| `WISHPOOL_REALTIME_SYNC_LIMIT` | `500` | Max events requested from Core API per pull, clamped to `1..1000`. |
| `WISHPOOL_REALTIME_CONNECTION_TTL_SECONDS` | `90` | Redis TTL for connection metadata and family connection sets. |

## Verification

- Run `./services/realtime-gateway/gradlew -p services/realtime-gateway test`.
- W9 smoke validation was run against local PostgreSQL and Redis on alternate ports: phone login -> create family/child/weekly plan -> internal materialization -> child-device pairing -> parent SSE and child-device SSE both received `realtime.connected`, `planning.weekly_plan_saved`, three `task.created`, `workflow.materialize_weekly_plan_completed`, and `family.child_device_paired`; Redis contained active family and connection keys while the SSE streams were open.
