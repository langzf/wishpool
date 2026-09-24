# Wish Image Generation Fix

Date: 2026-08-29

## Problem

Creating a wish image generation job inserted a `wish_image_generation_job` row with `status = 'queued'`, but no process consumed that job. The parent web polling flow therefore never observed a terminal status.

## Approach

Use the same synchronous pattern as `AiPrecheckService`: the core API creates the job, immediately claims it, calls `AiWorkerClient.generateWishImage`, stores the generated image as a media asset, then updates the job to a terminal state before returning the response.

This keeps the current parent web polling contract intact. A slow provider can still be observed by the existing polling path if the request returns before the UI sees the final state in a later follow-up request, while deterministic/local generation completes in the initial response.

## State Machine

The implemented path uses existing database statuses:

- `queued`: job inserted and waiting to be claimed.
- `running`: core API claimed the job with `where status = 'queued'`, incremented `attempt_count`, and set a short lease.
- `succeeded`: ai-worker returned base64 image content, the image was uploaded to object storage, a `media_asset` row was created, and `media_asset_id` was attached to the job.
- `failed_final`: ai-worker failed, returned no image content, returned invalid base64, or generated media persistence failed.

`failed_retryable` and `cancelled` remain part of the schema/contract but are not introduced by this synchronous create flow.

## Media Persistence

Generated image bytes are stored through `MediaService.createGeneratedWishImage`:

- purpose: `wish_image`
- status: `uploaded`
- related resource: `wish` when the generation request is tied to an existing wish
- object storage key: the same family/child/purpose layout as upload sessions
- response URL: existing `MediaService.toResponse` uses the public S3 presigner for browser-accessible `downloadUrl`

SVG deterministic output is supported with the `image/svg+xml` extension mapping.

## Changed Files

- `services/core-api/src/main/kotlin/com/wishpool/core/wishes/WishImageGenerationService.kt`
- `services/core-api/src/main/kotlin/com/wishpool/core/media/MediaService.kt`
- `docs/design/2026-08-29-wish-image-generation-fix.md`

## Verification

Expected local verification:

```bash
./gradlew -p services/core-api compileKotlin --offline
```

Hermes should restart services and run the end-to-end runtime check:

1. Deterministic provider: create job, observe `succeeded`, verify SVG media `downloadUrl` renders in parent web.
2. Volcengine Ark: valid key succeeds with real image; invalid/missing key produces `failed_final` with a visible `errorMessage`.
