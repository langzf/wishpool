# WishPool Docker 部署手册

本文档说明如何使用 `infra/docker-compose/docker-compose.yml` 在本机部署 WishPool 的完整本地运行环境。当前编排面向个人/家庭自用部署，包含数据库、对象存储、工作流、后端服务、worker、家长 Web 和管理后台 Web。

## 1. 前置要求

- Docker Desktop 或 Docker Engine，需包含 Docker Compose v2。
- Node.js 22+ 与 npm 10+，用于安装 monorepo workspace 依赖和执行初始化脚本。
- 首次启动需要能拉取基础镜像：PostgreSQL、Redis、MinIO、Temporal、JDK、Python 和 Node。
- 推荐从仓库根目录执行命令。

## 2. 服务与端口

| 服务 | 容器名 | 地址 |
| --- | --- | --- |
| 家长 Web | `wishpool-parent-web` | `http://localhost:3000` |
| 管理后台 Web | `wishpool-admin-web` | `http://localhost:3001` |
| Core API | `wishpool-core-api` | `http://localhost:8080` |
| Realtime Gateway | `wishpool-realtime-gateway` | `http://localhost:8081` |
| Admin API | `wishpool-admin-api` | `http://localhost:8083` |
| Notification Service | `wishpool-notification-service` | `http://localhost:8084` |
| AI Worker | `wishpool-ai-worker` | `http://localhost:8100` |
| PostgreSQL | `wishpool-postgres` | `localhost:5432` |
| Redis | `wishpool-redis` | `localhost:6379` |
| MinIO S3 API | `wishpool-minio` | `http://localhost:9000` |
| MinIO Console | `wishpool-minio` | `http://localhost:9001` |
| Temporal | `wishpool-temporal` | `localhost:7233` |
| Temporal UI | `wishpool-temporal-ui` | `http://localhost:8088` |

## 3. 配置文件

复制环境变量示例：

```bash
cp infra/docker-compose/.env.example infra/docker-compose/.env
```

常用变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `PARENT_WEB_PORT` | `3000` | 家长 Web 对外端口。 |
| `ADMIN_WEB_PORT` | `3001` | 管理后台 Web 对外端口。 |
| `CORE_API_PORT` | `8080` | Core API 对外端口。 |
| `REALTIME_GATEWAY_PORT` | `8081` | 实时同步网关对外端口。 |
| `ADMIN_API_PORT` | `8083` | Admin API 对外端口。 |
| `NOTIFICATION_SERVICE_PORT` | `8084` | 通知服务对外端口。 |
| `AI_WORKER_PORT` | `8100` | AI Worker 对外端口。 |
| `POSTGRES_PORT` | `5432` | PostgreSQL 对外端口。 |
| `MINIO_API_PORT` | `9000` | MinIO S3 API 对外端口。 |
| `MINIO_CONSOLE_PORT` | `9001` | MinIO 控制台对外端口。 |
| `TEMPORAL_PORT` | `7233` | Temporal gRPC 端口。 |
| `TEMPORAL_UI_PORT` | `8088` | Temporal UI 对外端口。 |
| `WISHPOOL_TOKEN_SECRET` | 本地默认值 | Core API JWT 签名密钥，本地自用也建议改成自己的长随机值。 |
| `WISHPOOL_INTERNAL_TOKEN` | `wishpool-local-internal-token` | worker、Admin API 与 Core API 内部通信令牌。 |
| `WISHPOOL_ADMIN_TOKEN` | `wishpool-local-admin-token` | 管理后台登录令牌。 |
| `WISHPOOL_LOCAL_DEBUG_PHONE_CODE` | `true` | 本地手机号验证码调试开关，初始化脚本依赖该能力。 |

端口冲突时，只改对应端口即可，例如：

```bash
PARENT_WEB_PORT=3100 docker compose -f infra/docker-compose/docker-compose.yml up -d parent-web
```

## 4. 启动

推荐使用一键启动：

```bash
npm run local:start
```

该命令会：

- 安装 npm workspace 依赖。
- 启动 Docker Compose 中的全部服务和两个 Web 客户端。
- 等待 Core API 健康。
- 通过真实 Core API 导入或刷新一个本地家庭空间，包括家长账号、家庭、孩子、任务模板、本周心愿、周计划、今日任务和儿童配对码。
- 打印 Web 地址、管理后台令牌、家长访问令牌、孩子 id 和配对码等信息。

只启动容器：

```bash
docker compose -f infra/docker-compose/docker-compose.yml up -d
```

Core API 健康后导入或刷新本地家庭数据：

```bash
npm run local:seed
```

## 5. 登录与访问

家长 Web：

```text
http://localhost:3000
```

使用手机号 `18800000001` 登录。本地调试验证码会在登录页面显示，也会由 seed 脚本通过 Core API 自动读取。

管理后台 Web：

```text
http://localhost:3001
```

默认管理令牌：

```text
wishpool-local-admin-token
```

MinIO 控制台：

```text
http://localhost:9001
```

默认账号：

```text
wishpool
```

默认密码：

```text
wishpool-local
```

## 6. 健康检查

查看容器状态：

```bash
docker compose -f infra/docker-compose/docker-compose.yml ps
```

检查 Core API：

```bash
curl http://localhost:8080/actuator/health
```

检查 Realtime Gateway：

```bash
curl http://localhost:8081/health
```

检查 Admin API：

```bash
curl http://localhost:8083/health
```

检查 Notification Service：

```bash
curl http://localhost:8084/health
```

检查 AI Worker：

```bash
curl http://localhost:8100/health
```

验证 Compose 配置：

```bash
npm run check:infra
```

验证本地运行脚本：

```bash
npm run check:local-runtime
```

## 7. 日志与重启

查看全部日志：

```bash
docker compose -f infra/docker-compose/docker-compose.yml logs -f
```

查看单个服务日志：

```bash
docker compose -f infra/docker-compose/docker-compose.yml logs -f core-api
```

重启单个服务：

```bash
docker compose -f infra/docker-compose/docker-compose.yml restart core-api
```

重新创建单个服务：

```bash
docker compose -f infra/docker-compose/docker-compose.yml up -d --force-recreate parent-web
```

停止服务并保留数据卷：

```bash
docker compose -f infra/docker-compose/docker-compose.yml down
```

## 8. 数据卷

当前 Compose 使用两个持久化数据卷：

| 数据卷 | 内容 |
| --- | --- |
| `wishpool-local_postgres_data` | PostgreSQL 业务库与 Temporal 使用的数据。 |
| `wishpool-local_minio_data` | MinIO 对象存储数据。 |

停止服务不会删除数据卷。删除数据卷会清空本地业务数据和媒体对象。

## 9. 备份与恢复

备份 PostgreSQL：

```bash
docker compose -f infra/docker-compose/docker-compose.yml exec -T postgres \
  pg_dump -U wishpool -d wishpool > wishpool-postgres.sql
```

恢复 PostgreSQL：

```bash
docker compose -f infra/docker-compose/docker-compose.yml exec -T postgres \
  psql -U wishpool -d wishpool < wishpool-postgres.sql
```

备份 MinIO 数据卷：

```bash
docker run --rm \
  -v wishpool-local_minio_data:/data \
  -v "$PWD":/backup \
  alpine tar czf /backup/wishpool-minio-data.tgz -C /data .
```

恢复 MinIO 数据卷：

```bash
docker run --rm \
  -v wishpool-local_minio_data:/data \
  -v "$PWD":/backup \
  alpine sh -c "cd /data && tar xzf /backup/wishpool-minio-data.tgz"
```

## 10. 镜像源

网络受限时，可在 `.env` 中覆盖镜像：

```bash
MINIO_IMAGE=registry.example.com/minio/minio:latest
MINIO_MC_IMAGE=registry.example.com/minio/mc:latest
TEMPORAL_IMAGE=registry.example.com/temporalio/auto-setup:1.26
TEMPORAL_UI_IMAGE=registry.example.com/temporalio/ui:2.38.0
```

然后重新启动：

```bash
docker compose -f infra/docker-compose/docker-compose.yml up -d
```

## 11. 常见问题

端口被占用：

```bash
lsof -i :3000
```

修改 `infra/docker-compose/.env` 中对应端口后重新启动。

Web 容器启动失败并提示找不到依赖：

```bash
npm install
docker compose -f infra/docker-compose/docker-compose.yml restart parent-web admin-web
```

初始化脚本提示 Core API 未健康：

```bash
docker compose -f infra/docker-compose/docker-compose.yml logs -f core-api
```

重点检查 PostgreSQL、Flyway、MinIO 初始化和 `WISHPOOL_INTERNAL_TOKEN`。

seed 脚本提示需要本地调试验证码：

```bash
WISHPOOL_LOCAL_DEBUG_PHONE_CODE=true docker compose -f infra/docker-compose/docker-compose.yml up -d core-api
```

家长 Web 收不到实时刷新：

```bash
curl http://localhost:8081/health
docker compose -f infra/docker-compose/docker-compose.yml logs -f realtime-gateway
```

确认 Realtime Gateway、Redis 和 Core API 都在运行。

## 12. 服务说明

- `core-api` 使用 Spring Boot，启动时执行 Flyway 迁移，业务事实写入 PostgreSQL。
- `workflow-worker` 读取 Core API outbox 并启动 Temporal 工作流。
- `realtime-gateway` 提供 WebSocket 与 SSE，客户端通过 cursor 补拉家庭事件。
- `ai-worker` 当前提供本地可运行的 AI 能力接口。
- `media-worker` 处理 finalized media，并将衍生资源写回 Core API。
- `notification-service` 默认只处理站内通知状态，不调用外部系统推送。
- `parent-web` 和 `admin-web` 使用 Node 容器运行 Next.js 开发服务，挂载当前仓库源码。
