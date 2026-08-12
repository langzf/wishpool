# Media Worker Module

## Purpose

`services/media-worker/` contains the Python media processing worker. It claims finalized media from Core API, downloads private originals from S3-compatible storage, creates derivative assets with Pillow and FFmpeg, uploads derivatives back to object storage, and records results through Core API internal endpoints.

## Important Files

| Path | Responsibility |
| --- | --- |
| `requirements.txt` | Python dependencies for S3 access, image processing, HTTP calls, and tests. |
| `media_worker/config.py` | Environment-backed runtime configuration for Core API, internal token, S3, polling, leases, and work directory. |
| `media_worker/core_api.py` | Internal Core API client for claim, complete, and failure calls. |
| `media_worker/storage.py` | S3-compatible download and upload wrapper. |
| `media_worker/processor.py` | Image, audio, and video derivative generation. |
| `media_worker/worker.py` | Claim/process/complete loop with retryable failure reporting. |
| `media_worker/__main__.py` | CLI entry point; supports continuous run or `--once`. |
| `tests/test_config.py` | Configuration unit coverage. |
| `tests/test_processor.py` | Derivative key and image processing unit coverage. |

## Runtime Behavior

- The worker calls `POST /internal/media/processing/claim` with `X-Internal-Token` to lease media items.
- For images it writes `thumbnail`, `preview`, and `ai_ready` JPEG derivatives with EXIF orientation normalized and metadata stripped.
- For audio it writes `transcoded` AAC/M4A, `waveform` JSON, and `ai_ready` WAV derivatives.
- For video it writes `thumbnail`, `keyframe`, `preview`, `transcoded`, and `ai_ready` MP4/image derivatives.
- Completed results are written with `POST /internal/media/{mediaId}/processing-completed`.
- Processing failures are reported with `POST /internal/media/{mediaId}/processing-failed`, allowing retry according to Core API leases.

## Configuration

| Variable | Default | Meaning |
| --- | --- | --- |
| `WISHPOOL_CORE_API_BASE_URL` | `http://localhost:8080` | Core API base URL. |
| `WISHPOOL_INTERNAL_TOKEN` | `wishpool-local-internal-token` | Shared internal API token. |
| `WISHPOOL_S3_ENDPOINT` / `S3_ENDPOINT` | `http://localhost:9000` | S3-compatible endpoint. |
| `WISHPOOL_S3_BUCKET` / `S3_BUCKET` | `wishpool-media` | Private media bucket. |
| `WISHPOOL_S3_ACCESS_KEY` / `S3_ACCESS_KEY` | `wishpool` | S3 access key. |
| `WISHPOOL_S3_SECRET_KEY` / `S3_SECRET_KEY` | `wishpool-local` | S3 secret key. |
| `WISHPOOL_MEDIA_POLL_INTERVAL_SECONDS` | `2` | Idle polling interval. |
| `WISHPOOL_MEDIA_CLAIM_LIMIT` | `5` | Max media items leased per claim. |
| `WISHPOOL_MEDIA_LEASE_SECONDS` | `300` | Lease duration for claimed media. |
| `WISHPOOL_MEDIA_WORK_DIR` | `/tmp/wishpool-media-worker` | Local scratch directory. |

## Verification

- Run `python3 -m pytest services/media-worker/tests`.
