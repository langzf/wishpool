# Core API Module

## Purpose

`services/core-api` is the Kotlin/Spring Boot service that owns WishPool core business facts: identity, family membership, child profiles, task planning, submissions, reviews, rewards, wishes, memories, room state, privacy requests, and event publication.

## Important Files

| File | Purpose |
| --- | --- |
| `build.gradle.kts` | Spring Boot Kotlin build, PostgreSQL/Flyway/security/web/test dependencies, and AWS SDK S3 client dependencies. |
| `src/main/kotlin/com/wishpool/core/CoreApiApplication.kt` | Service entry point. |
| `src/main/resources/application.yml` | Local datasource, Flyway, management endpoint, server, auth, trace, internal token, outbox lease, and S3-compatible object storage configuration. |
| `src/main/kotlin/com/wishpool/core/storage/S3StorageConfig.kt` | Builds `S3Client` and `S3Presigner` for MinIO/S3-compatible private media storage. |
| `src/main/kotlin/com/wishpool/core/config/SecurityConfig.kt` | Stateless HTTP security with public health endpoints. |
| `src/main/kotlin/com/wishpool/core/http/TraceIdFilter.kt` | Adds or preserves `X-Trace-Id` for every request and stores it on the request for error handling. |
| `src/main/kotlin/com/wishpool/core/http/ApiExceptionHandler.kt` | Converts domain, validation, unreadable request body, and unexpected errors into Problem Details responses. |
| `src/main/kotlin/com/wishpool/core/system/SystemController.kt` | Internal service version/status endpoint. |
| `src/main/kotlin/com/wishpool/core/security/TokenService.kt` | Creates and verifies signed bearer access tokens. |
| `src/main/kotlin/com/wishpool/core/security/BearerAuthenticationFilter.kt` | Resolves bearer tokens into the current WishPool user context. |
| `src/main/kotlin/com/wishpool/core/auth/AuthController.kt` | Exposes phone code, login, refresh, and `/me` routes. |
| `src/main/kotlin/com/wishpool/core/auth/AuthService.kt` | Owns phone verification, parent login, refresh tokens, device records, and current user context. |
| `src/main/kotlin/com/wishpool/core/family/FamilyController.kt` | Exposes family create/read/member/invite routes. |
| `src/main/kotlin/com/wishpool/core/family/FamilyService.kt` | Owns family creation, owner membership, member listing, parent invite creation, and audit records. |
| `src/main/kotlin/com/wishpool/core/family/FamilyPolicy.kt` | Centralizes family and child access checks for parent and child-device roles. |
| `src/main/kotlin/com/wishpool/core/child/ChildController.kt` | Exposes child list/create/update routes. |
| `src/main/kotlin/com/wishpool/core/child/ChildService.kt` | Owns child profile creation, update, lookup, and role-limited access. |
| `src/main/kotlin/com/wishpool/core/pairing/PairingController.kt` | Exposes child device pairing session creation and consumption. |
| `src/main/kotlin/com/wishpool/core/pairing/PairingService.kt` | Owns pairing code generation, one-time consumption, child-device user creation, membership, device registration, and token issue. |
| `src/main/kotlin/com/wishpool/core/home/HomeController.kt` | Exposes child and parent aggregate home contexts for clients. |
| `src/main/kotlin/com/wishpool/core/home/HomeService.kt` | Composes family, child, today tasks, wish, room, memory, review, task template, and notification data into client home responses. |
| `src/main/kotlin/com/wishpool/core/home/HomeDtos.kt` | Response DTOs for child home context, parent dashboard context, and child feedback cards. |
| `src/main/kotlin/com/wishpool/core/tasks/TaskPlanningController.kt` | Exposes task templates, weekly plans, today snapshot, skip, and postpone routes. |
| `src/main/kotlin/com/wishpool/core/tasks/TaskPlanningService.kt` | Owns task templates, idempotent weekly plan upsert, rule replacement, task materialization, today queries, skip, and postpone behavior. |
| `src/main/kotlin/com/wishpool/core/tasks/TaskDtos.kt` | Request and response DTOs for task template, weekly plan, task instance, today snapshot, skip, and postpone APIs. |
| `src/main/kotlin/com/wishpool/core/tasks/TaskMappers.kt` | JDBC row mappers for task planning responses. |
| `src/main/kotlin/com/wishpool/core/media/MediaController.kt` | Exposes signed upload session creation, media finalize routes, and internal media processing endpoints. |
| `src/main/kotlin/com/wishpool/core/media/MediaService.kt` | Owns media asset metadata, MinIO/S3 presigned PUT/GET URLs, object HEAD validation, purpose/related-resource checks, finalize state changes, processing leases, derivative writes, and media processing events. |
| `src/main/kotlin/com/wishpool/core/media/MediaDtos.kt` | Request and response DTOs for upload sessions, media assets, internal processing claims, derivative completion, and processing failures. |
| `src/main/kotlin/com/wishpool/core/media/MediaMappers.kt` | JDBC row mappers for media assets and media derivatives. |
| `src/main/kotlin/com/wishpool/core/submissions/SubmissionController.kt` | Exposes submission creation and submission detail routes. |
| `src/main/kotlin/com/wishpool/core/submissions/SubmissionService.kt` | Owns task submission idempotency, media attachment validation, attempt numbering, resubmission superseding, task status updates, and submission detail reads. |
| `src/main/kotlin/com/wishpool/core/reviews/ReviewController.kt` | Exposes pending review queue, review detail, review decision, and review revoke routes. |
| `src/main/kotlin/com/wishpool/core/reviews/ReviewService.kt` | Owns parent review decisions, feedback persistence, active-review uniqueness, task/submission state transitions, review events, revoke behavior, and review audit records. |
| `src/main/kotlin/com/wishpool/core/rewards/RewardService.kt` | Owns reward ledger writes, daily summary aggregation, wish fragment grants, reward adjustments, and wish unlock progress updates. |
| `src/main/kotlin/com/wishpool/core/wishes/WishController.kt` | Exposes wish creation, activation, detail, current wish, child wish list, and redemption routes. |
| `src/main/kotlin/com/wishpool/core/wishes/WishService.kt` | Owns wish lifecycle state changes, wish media validation, redemption media association, wish events, and idempotent wish commands. |
| `src/main/kotlin/com/wishpool/core/ai/AiPrecheckService.kt` | Owns submission AI precheck jobs, AI worker calls or local fallback, `ai_precheck` persistence, and precheck events. |
| `src/main/kotlin/com/wishpool/core/ai/AiWorkerClient.kt` | Calls the AI worker internal HTTP API when configured and provides deterministic local fallback otherwise. |
| `src/main/kotlin/com/wishpool/core/memories/MemoryController.kt` | Exposes memory timeline, memory detail, and memory export routes. |
| `src/main/kotlin/com/wishpool/core/memories/MemoryService.kt` | Owns weekly memory reads, idempotent export media request creation, workflow memory generation, memory items, and memory room unlocks. |
| `src/main/kotlin/com/wishpool/core/room/RoomController.kt` | Exposes room state and room item arrangement routes. |
| `src/main/kotlin/com/wishpool/core/room/RoomService.kt` | Owns room item reads, idempotent position updates, and room item arrangement events. |
| `src/main/kotlin/com/wishpool/core/privacy/PrivacyController.kt` | Exposes privacy export and family deletion request routes. |
| `src/main/kotlin/com/wishpool/core/privacy/PrivacyService.kt` | Owns privacy request validation, export placeholder media, family lock/delete workflow updates, audit, and privacy events. |
| `src/main/kotlin/com/wishpool/core/notifications/NotificationController.kt` | Exposes inbox, read-state, preference, push-token, and internal dispatch routes. |
| `src/main/kotlin/com/wishpool/core/notifications/NotificationService.kt` | Owns notification events, user preferences, push token registration, pending-claim dispatch state, and internal notification creation. |
| `src/main/kotlin/com/wishpool/core/notifications/NotificationEventProjector.kt` | Projects selected family events into notification inbox rows inside the same transaction. |
| `src/main/kotlin/com/wishpool/core/admin/AdminController.kt` | Exposes internal local administration routes protected by `X-Internal-Token`. |
| `src/main/kotlin/com/wishpool/core/admin/AdminService.kt` | Owns administration counters, family metadata queries, privacy queue reads, audit log reads, and governed media access grants. |
| `src/main/kotlin/com/wishpool/core/sync/SyncController.kt` | Exposes `/sync/pull` for family event cursor reads. |
| `src/main/kotlin/com/wishpool/core/sync/SyncService.kt` | Reads ordered family events, preserves seq continuity, and redacts events outside child-device access. |
| `src/main/kotlin/com/wishpool/core/outbox/OutboxController.kt` | Exposes internal outbox claim, published, and retry endpoints protected by `X-Internal-Token`. |
| `src/main/kotlin/com/wishpool/core/outbox/OutboxService.kt` | Claims unpublished outbox rows with leases, marks publication success, and schedules retry on failure. |
| `src/main/kotlin/com/wishpool/core/internal/InternalAuthService.kt` | Verifies shared internal endpoint token. |
| `src/main/kotlin/com/wishpool/core/workflow/WorkflowController.kt` | Exposes internal workflow activity endpoints for plan materialization, reward evaluation, AI precheck, memory generation, and privacy deletion. |
| `src/main/kotlin/com/wishpool/core/idempotency/IdempotencyService.kt` | Stores command idempotency keys and resolved resources for repeat-safe write endpoints. |
| `src/main/kotlin/com/wishpool/core/events/DomainEventPublisher.kt` | Writes ordered `family_event` rows and matching `outbox_event` rows inside the caller transaction. |

## Data Flow And Integrations

- Flyway reads SQL migrations from `../../db/migrations` by default.
- The local datasource defaults to `jdbc:postgresql://localhost:5432/wishpool`.
- Health checks are exposed through Spring Boot Actuator.
- Implemented W3 routes follow the OpenAPI contract: `/auth/phone-codes`, `/auth/login`, `/auth/refresh`, `/me`, `/families`, `/families/{familyId}`, `/families/{familyId}/members`, `/families/{familyId}/invites`, `/families/{familyId}/children`, `/children/{childId}`, `/families/{familyId}/pairing-sessions`, and `/pairing/consume`.
- Implemented client aggregation routes follow the OpenAPI contract: `/children/{childId}/home-context` and `/families/{familyId}/parent-dashboard`.
- Implemented W4 routes follow the OpenAPI contract: `/task-templates`, `/plans`, `/plans/{planId}`, `/children/{childId}/today`, `/tasks/{taskId}/skip`, and `/tasks/{taskId}/postpone`.
- Implemented W5 routes follow the OpenAPI contract: `/media/upload-sessions`, `/media/{mediaId}/finalize`, `/submissions`, and `/submissions/{submissionId}`.
- Implemented W6 routes follow the OpenAPI contract: `/reviews/pending`, `/reviews/{submissionId}/detail`, `/reviews`, and `/reviews/{reviewId}/revoke`.
- Implemented W7 routes follow the OpenAPI contract: `/wishes`, `/wishes/{wishId}/activate`, `/wishes/{wishId}`, `/children/{childId}/wishes/current`, `/children/{childId}/wishes`, and `/wishes/{wishId}/redeem`.
- Implemented W8 routes follow the OpenAPI contract: `/sync/pull`, `/internal/outbox/events/claim`, `/internal/outbox/events/{eventId}/published`, `/internal/outbox/events/{eventId}/retry`, `/internal/workflows/materialize-weekly-plan`, `/internal/workflows/evaluate-reward`, `/internal/workflows/run-ai-precheck`, `/internal/workflows/generate-memory`, and `/internal/workflows/privacy-deletion`.
- Implemented W10 internal routes: `/internal/media/processing/claim`, `/internal/media/{mediaId}/processing-source`, `/internal/media/{mediaId}/processing-started`, `/internal/media/{mediaId}/processing-completed`, and `/internal/media/{mediaId}/processing-failed`.
- Implemented memory, room, and privacy routes follow the OpenAPI contract: `/memories`, `/memories/{memoryId}`, `/memories/{memoryId}/export`, `/room/state`, `/room/items/{itemId}/arrange`, `/privacy/export`, and `/privacy/delete`.
- Implemented notification routes follow the OpenAPI contract: `/notifications`, `/notifications/read`, `/notification-preferences`, `/devices/push-token`, `/internal/notifications`, `/internal/notifications/claim`, and `/internal/notifications/{notificationId}/dispatch-result`.
- Implemented internal administration routes follow the OpenAPI contract: `/internal/admin/dashboard`, `/internal/admin/families`, `/internal/admin/privacy-requests`, `/internal/admin/audit-logs`, and `/internal/admin/media-access-grants`.
- Phone login uses local debug verification codes by default for self-hosted development; production SMS can replace the provider without changing the login contract.
- Parent access and child-device access are enforced through `FamilyPolicy`.
- Weekly plan save soft-replaces editable rules with `superseded_at` and emits `planning.weekly_plan_saved`; materialization runs through the internal workflow activity so user HTTP requests do not own long-running work.
- The materialization activity creates `todo` task instances for matching weekdays while retaining submitted or otherwise touched historical instances, and emits `task.created` plus `workflow.materialize_weekly_plan_completed`.
- Postpone marks the original task as `adjusted_by_parent` and creates a new `carry_over` task linked by `original_task_instance_id`.
- Media upload creates a `media_asset` row scoped by family, child, purpose, and `relatedResource`, returns a short-lived presigned PUT URL, and finalize confirms the object exists with matching size/content type before marking it `uploaded` and emitting `media.uploaded`.
- Media processing uses claim leases on `media_asset`, writes `media_derivative` rows idempotently by `(media_asset_id, kind)`, sets successful assets to `ready`, records retryable failures, and emits `media.processing_started`, `media.processing_completed`, or `media.processing_failed`.
- Submission creation requires a registered device, validates that attached media belongs to the same task, advances the task to `pending_review`, supersedes the previous editable submission on resubmit, and emits `submission.created` to `family_event` plus `outbox_event`.
- AI precheck is triggered from `submission.created` outbox processing, records `ai_job` and `ai_precheck`, calls the AI worker when configured, falls back locally otherwise, and emits `submission.ai_prechecked`.
- Review creation is parent-only, locks submission and task rows, allows one active review per submission, moves approved submissions/tasks to `approved`, moves revision requests to submission `rejected` and task `needs_revision`, persists text/emoji/audio feedback, and emits `review.approved` or `review.revision_requested` plus `feedback.created` when feedback is present.
- Review revoke is parent-only, marks the review revoked with a reason, reopens approved tasks for review when applicable, hides revoked review data from submission detail, writes audit, and emits `review.revoked`.
- Reward evaluation runs through the internal workflow activity: approved tasks grant idempotent star light, a day with all core tasks `approved` or `skipped` grants one idempotent wish fragment, `daily_summary` is maintained from task facts, and review revoke writes negative adjustment rows instead of deleting rewards.
- `/sync/pull` returns monotonically ordered family events. Parent members receive full payloads; child-device members receive their own `childId` events and redacted placeholders for inaccessible seq values so cursor continuity is preserved without leaking data.
- Internal outbox publication uses `available_at` plus `leased_until`; workers claim rows with `for update skip locked`, acknowledge successful publication, or release the lease with retry metadata.
- Wish lifecycle is parent-controlled for creation, activation, and redemption; child devices can read their current wish and wish history through child-scoped policy checks.
- Memory generation runs from `wish.redeemed`, upserts `weekly_memory`, rewrites memory items from approved tasks, unlocks a room shelf item, and emits `memory.generated` plus `room.item_unlocked`.
- Room arrangement persists `position_json` and emits `room.item_arranged`.
- Privacy export creates a privacy request plus export media placeholder and emits `privacy.export_requested`; privacy deletion locks the family, marks records and media deleted through the internal workflow activity, writes audit, and emits `privacy.deletion_completed`.
- Notification projection creates inbox rows from task, submission, AI precheck, review, reward, wish, and memory family events; the dispatcher claims pending rows and records sent, failed, or suppressed outcomes.
- Administration routes expose metadata and audited short-lived media access without making the admin web call family-facing APIs directly.
- Parent dashboard aggregation includes selected child, today tasks, active wish, active weekly plan, pending reviews, task templates, memories, room state, and notification inbox.
- Child home aggregation includes today tasks, current wish, room state, latest memory, latest feedback, and unread notification count under child access policy.
- JSON request body read failures return HTTP 400 Problem Details instead of leaking as unexpected server errors.

## Tests

- `CoreApiApplicationTests` verifies Spring context startup with the `test` profile.
- `TraceIdFilterTests` verifies health requests preserve the `X-Trace-Id` response header.
- `ApiExceptionHandlerTests` verifies malformed JSON request bodies return HTTP 400 Problem Details with a trace id.
- `src/test/resources/application-test.yml` uses H2 and disables Flyway for fast service-level tests. PostgreSQL migration checks are handled by `db/` tooling and local runtime validation.
- W3 smoke validation was run against local PostgreSQL on an alternate port: phone code -> login -> create family -> create child -> create pairing session -> consume pairing code -> child-device `/me`.
- W4 smoke validation was run against local PostgreSQL on an alternate port: task template -> weekly plan -> task materialization -> today snapshot -> skip -> postpone.
- W5 smoke validation was run against local PostgreSQL and MinIO on alternate ports: child-device signed upload -> direct PUT to MinIO -> finalize media -> create submission -> idempotent retry -> parent reads submission detail -> `submission.created` event and outbox rows verified.
- W6 smoke validation was run against local PostgreSQL and MinIO on alternate ports: pending review list -> approve with feedback -> idempotent retry -> review detail includes feedback -> approval events/outbox/audit verified; revision request -> task `needs_revision` -> child resubmission attempt number increments; approved review revoke -> task/submission reopen for review -> revoked review hidden from submission detail.
- W7 smoke validation was run against local PostgreSQL and MinIO on alternate ports: create and activate wish -> approve daily core task -> star light and wish fragment ledger rows -> daily summary earned -> wish unlock -> redeem wish with photo; revoke approved review -> adjustment ledger rows -> daily summary not earned -> wish progress returns below unlock.
- W8 smoke validation was run against local PostgreSQL, Redis, and MinIO on alternate ports: phone login -> family/child/plan -> internal materialize activity -> `/sync/pull` returned `planning.weekly_plan_saved`, `task.created`, `workflow.materialize_weekly_plan_completed`, `task.skipped` in seq order -> reward activity completed for skipped task -> internal outbox claim and published ack succeeded.
- W10 smoke validation was run against local PostgreSQL and MinIO on alternate ports: create image submission media -> direct PUT to MinIO -> finalize media -> media-worker claim -> generated `thumbnail`, `preview`, and `ai_ready` derivatives -> media asset became `ready` -> `media.uploaded`, `media.processing_started`, and `media.processing_completed` events verified.

Run:

```bash
./gradlew -p services/core-api test
```

## Maintenance Notes

Keep this file updated when controllers, modules, persistence behavior, security rules, jobs, or integrations change.
