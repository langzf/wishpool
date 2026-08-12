# Workflow Worker Module

`services/workflow-worker` contains the Kotlin worker process that publishes core-api outbox events into durable Temporal workflows. It is intentionally separate from the synchronous REST service so long-running work, retries, and compensation are observable and do not run inside user HTTP requests.

## Important Files

| File | Responsibility |
| --- | --- |
| `build.gradle.kts` | Kotlin/JVM worker build with Temporal SDK and Jackson dependencies. |
| `src/main/kotlin/com/wishpool/workflow/WorkflowWorkerApplication.kt` | Starts the Temporal worker and the outbox publisher loop. |
| `src/main/kotlin/com/wishpool/workflow/WorkerConfig.kt` | Reads worker runtime settings from environment variables. |
| `src/main/kotlin/com/wishpool/workflow/CoreApiClient.kt` | Calls core-api internal outbox and workflow activity endpoints with `X-Internal-Token`. |
| `src/main/kotlin/com/wishpool/workflow/OutboxPublisher.kt` | Claims outbox rows, routes event types to workflows, marks events published, and schedules retry after failures. |
| `src/main/kotlin/com/wishpool/workflow/Workflows.kt` | Defines `MaterializeWeeklyPlanWorkflow`, `RewardEvaluationWorkflow`, `GenerateMemoryWorkflow`, `PrivacyDeletionWorkflow`, `MediaProcessingWorkflow`, and `AiPrecheckWorkflow`. |
| `src/main/kotlin/com/wishpool/workflow/WorkflowActivitiesImpl.kt` | Activity implementations that call core-api internal endpoints. |

## Runtime Contract

- Input source is core-api `outbox_event`, claimed through `/internal/outbox/events/claim`.
- Successful workflow start marks an outbox row published through `/internal/outbox/events/{eventId}/published`.
- Failures call `/internal/outbox/events/{eventId}/retry` with a delay so events remain recoverable.
- `planning.weekly_plan_saved`, `submission.created`, `review.approved`, `task.skipped`, approved `review.revoked`, `wish.redeemed`, `privacy.deletion_requested`, and `media.uploaded` are routed.
- `submission.created` starts `AiPrecheckWorkflow`, whose activity calls Core API to write `ai_job` and `ai_precheck` records.
- `media.uploaded` starts `MediaProcessingWorkflow`, whose activity marks the media asset as `processing` before the Python media worker claims and writes derivatives through Core API.
- `wish.redeemed` starts `GenerateMemoryWorkflow`, whose activity asks Core API to generate/update the weekly memory and unlock the corresponding room item.
- `privacy.deletion_requested` starts `PrivacyDeletionWorkflow`, whose activity asks Core API to execute the deletion state machine.

## Environment

| Variable | Default | Purpose |
| --- | --- | --- |
| `WISHPOOL_CORE_API_BASE_URL` | `http://localhost:8080` | Core API internal HTTP base URL. |
| `WISHPOOL_INTERNAL_TOKEN` | `wishpool-local-internal-token` | Shared internal endpoint token. |
| `WISHPOOL_TEMPORAL_TARGET` | `localhost:7233` | Temporal frontend address. |
| `WISHPOOL_TEMPORAL_TASK_QUEUE` | `wishpool-workflows` | Worker task queue. |
| `WISHPOOL_OUTBOX_POLL_INTERVAL_MS` | `1000` | Poll interval after each claim cycle. |
| `WISHPOOL_OUTBOX_CLAIM_LIMIT` | `50` | Max outbox events claimed per cycle. |
| `WISHPOOL_OUTBOX_RETRY_DELAY_SECONDS` | `60` | Delay before retrying failed publication. |
