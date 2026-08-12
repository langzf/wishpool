# WishPool Local Runtime

This directory starts the local dependencies used during development.

## Services

| Service | URL |
| --- | --- |
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |
| Core API | `http://localhost:8080` |
| Realtime Gateway | `http://localhost:8081` |
| Admin API | `http://localhost:8083` |
| Notification Service | `http://localhost:8084` |
| AI Worker | `http://localhost:8100` |
| MinIO S3 API | `http://localhost:9000` |
| MinIO Console | `http://localhost:9001` |
| Temporal | `localhost:7233` |
| Temporal UI | `http://localhost:8088` |

## Start

```bash
docker compose -f infra/docker-compose/docker-compose.yml up -d
```

If another local project already uses a default port, override only that port:

```bash
POSTGRES_PORT=55432 docker compose -f infra/docker-compose/docker-compose.yml up -d postgres
```

When Docker Hub is slow or unavailable, point services at a local mirror or cached image:

```bash
MINIO_IMAGE=registry.example.com/minio/minio:latest \
MINIO_MC_IMAGE=registry.example.com/minio/mc:latest \
TEMPORAL_IMAGE=registry.example.com/temporalio/auto-setup:1.26 \
TEMPORAL_UI_IMAGE=registry.example.com/temporalio/ui:2.38.0 \
docker compose -f infra/docker-compose/docker-compose.yml up -d minio minio-init temporal temporal-ui
```

Application services are included in this compose file. For a service-only workflow, the Realtime Gateway can still be started from the repository after Redis and Core API are available:

```bash
WISHPOOL_CORE_API_BASE_URL=http://localhost:8080 \
WISHPOOL_REDIS_URI=redis://localhost:6379 \
./services/realtime-gateway/gradlew -p services/realtime-gateway run
```

## Stop

```bash
docker compose -f infra/docker-compose/docker-compose.yml down
```

## Notes

- The MinIO bucket `wishpool-media` is created by `minio-init`.
- PostgreSQL credentials are for local development only.
- Flyway migrations live in `db/migrations`.
- Realtime clients connect to `/realtime` by WebSocket or `/realtime/sse` by SSE and keep their own `afterSeq` cursor.
