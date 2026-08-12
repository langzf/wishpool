# AI Worker Module

## Purpose

`services/ai-worker` provides WishPool's AI capability boundary. It exposes internal HTTP endpoints for submission precheck, parent feedback drafting, weekly memory narrative generation, and privacy request summaries. The default provider is deterministic so local deployment and automated tests remain stable without an external model.

## Stack

- Python
- Standard library HTTP server
- `dataclasses`
- `unittest`

## Key Files

| File | Responsibility |
| --- | --- |
| `ai_worker/app.py` | Standard-library HTTP app, internal bearer auth, health endpoint, JSON parsing, and AI routes. |
| `ai_worker/models.py` | Dataclass request/response models and validation helpers for AI capabilities. |
| `ai_worker/provider.py` | Provider abstraction and deterministic local provider. |
| `ai_worker/config.py` | Environment-driven configuration. |
| `ai_worker/__main__.py` | Threaded local server entry. |
| `tests/test_ai_worker.py` | Route and provider behavior tests. |

## Runtime Contract

- `GET /health` returns worker readiness.
- Internal routes require `Authorization: Bearer <WISHPOOL_AI_INTERNAL_TOKEN>`.
- `POST /internal/ai/precheck-submission` returns a structured recommendation for parent review.
- `POST /internal/ai/draft-feedback` returns a short parent-facing feedback draft.
- `POST /internal/ai/generate-memory-narrative` returns a weekly memory title, summary, and highlights.
- `POST /internal/ai/summarize-privacy-request` returns a human-readable privacy request summary.
- The default provider is deterministic and local. A hosted model adapter can replace it behind the same provider interface without changing Core API or workflow contracts.

## Runtime

Run one local server:

```bash
PYTHONPATH=services/ai-worker python3 -m ai_worker
```

Default port is `8100`.

## Verification

Run `npm run test:ai-worker`.
