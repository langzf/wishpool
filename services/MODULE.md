# Services Module

## Purpose

`services/` contains backend services and workers. Core business state lives in the Kotlin/Spring Boot Core API, durable long-running work is started from a separate Kotlin workflow worker, realtime client synchronization is served by a Kotlin/Ktor gateway, media derivatives are produced by a Python worker, AI capabilities are exposed through an internal Python worker, notifications are dispatched by a Kotlin service, and local administration is exposed by a dedicated Kotlin API.

## Submodules

| Path | Responsibility |
| --- | --- |
| `core-api/` | WishPool core business REST API, Flyway integration, health checks, shared HTTP behavior, family events, internal workflow activity endpoints, and outbox claim/ack APIs. |
| `workflow-worker/` | Kotlin/Temporal worker that claims core-api outbox events, starts durable workflows, and acknowledges or retries publication. |
| `realtime-gateway/` | Kotlin/Ktor WebSocket and SSE gateway that authenticates through Core API, pulls authorized family events by cursor, and records active connections in Redis. |
| `media-worker/` | Python worker that claims finalized media through Core API, creates image/audio/video derivatives with Pillow and FFmpeg, uploads them to S3-compatible storage, and records completion or retryable failure. |
| `ai-worker/` | Python internal HTTP worker that provides deterministic local AI precheck, feedback drafting, memory narrative, and privacy summary capabilities with a provider boundary for model integration. |
| `notification-service/` | Kotlin/Ktor dispatcher that claims Core API notification events, applies preferences and channel rules, and records dispatch results. |
| `admin-api/` | Kotlin/Ktor local administration API that fronts Core API internal administration routes for dashboard, privacy, audit, and governed media access. |

## Maintenance Notes

Each service must keep its own `MODULE.md` current when routes, jobs, persistence, integrations, or public behavior change.
