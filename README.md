# WishPool

WishPool is a self-hosted family growth companion app. The product loop is:

```text
weekly wish -> daily tasks -> child media check-ins -> AI-assisted precheck
-> parent review -> rewards and wish fragments -> wish redemption
-> weekly memories -> room display
```

## Repository Layout

| Path | Purpose |
| --- | --- |
| `apps/` | Mobile app, parent web, and admin web clients. |
| `services/` | Backend services and workers. |
| `packages/` | Shared contracts, generated clients, design tokens, and sample data. |
| `db/` | Flyway SQL migrations and database tooling. |
| `infra/` | Local runtime and deployment infrastructure. |
| `docs/` | Architecture, detailed design, visual direction, and implementation plan. |
| `scripts/` | Repository-level validation scripts. |

## Local Dependencies

Docker deployment details live in `infra/docker-compose/README.md`.

Start the local runtime and import a usable family workspace:

```bash
npm run local:start
```

This starts Docker Compose services and both Web clients, waits for Core API, creates or reuses a local parent account, family, child, weekly wish, weekly plan, today's tasks, and a child pairing code.

Start only the Docker Compose services:

```bash
docker compose -f infra/docker-compose/docker-compose.yml up -d
```

Import or refresh local seed data after Core API is healthy:

```bash
npm run local:seed
```

## Common Commands

Install JavaScript tooling:

```bash
npm install
```

Run all repository checks:

```bash
npm run check
```

Validate OpenAPI and Flyway migration naming without starting the app:

```bash
npm run lint:openapi
npm run check:db
```

Run the Core API tests:

```bash
./services/core-api/gradlew -p services/core-api test
```

Run the Workflow Worker tests:

```bash
./services/workflow-worker/gradlew -p services/workflow-worker test
```

Run the Realtime Gateway tests:

```bash
./services/realtime-gateway/gradlew -p services/realtime-gateway test
```

Run the Media Worker tests:

```bash
npm run test:media-worker
```

Run the AI Worker tests:

```bash
npm run test:ai-worker
```

Run the Notification Service and Admin API tests:

```bash
npm run test:notification-service
npm run test:admin-api
```

Run client and shared package checks:

```bash
npm run check:shared
npm run check:clients
```

Start the Core API:

```bash
./services/core-api/gradlew -p services/core-api bootRun
```

Start the Workflow Worker after Core API and Temporal are available:

```bash
./services/workflow-worker/gradlew -p services/workflow-worker run
```

Run one Media Worker batch after Core API and MinIO are available:

```bash
python3 -m pip install --target /tmp/wishpool-media-worker-deps -r services/media-worker/requirements.txt
PYTHONPATH=services/media-worker:/tmp/wishpool-media-worker-deps \
python3 -m media_worker --once
```

Start the AI Worker:

```bash
PYTHONPATH=services/ai-worker python3 -m ai_worker
```

Start the Notification Service after Core API is available:

```bash
WISHPOOL_CORE_API_BASE_URL=http://localhost:8080 \
./services/notification-service/gradlew -p services/notification-service run
```

Start the Admin API after Core API is available:

```bash
WISHPOOL_CORE_API_BASE_URL=http://localhost:8080 \
./services/admin-api/gradlew -p services/admin-api run
```

Start the Realtime Gateway after Core API and Redis are available:

```bash
WISHPOOL_CORE_API_BASE_URL=http://localhost:8080 \
WISHPOOL_REDIS_URI=redis://localhost:6379 \
./services/realtime-gateway/gradlew -p services/realtime-gateway run
```

Start the Web clients outside Docker Compose:

```bash
npm run dev --workspace @wishpool/parent-web
npm run dev --workspace @wishpool/admin-web
```

## Documentation

Start from `PROJECT_CODE_STRUCTURE.md`, then read:

- `docs/ARCHITECTURE_DESIGN.md`
- `docs/DETAILED_DESIGN.md`
- `docs/TECH_STACK_DECISION.md`
- `docs/IMPLEMENTATION_PLAN.md`
