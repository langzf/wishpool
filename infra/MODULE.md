# Infrastructure Module

## 模块定位

`infra/` 保存 WishPool 本地开发和部署相关配置。当前已包含本地 Docker Compose 运行时，用于启动 PostgreSQL、Redis、MinIO、Temporal、Core API、Realtime Gateway、Workflow Worker、AI Worker、Media Worker、Notification Service 和 Admin API。

## 文件说明

| 文件 | 用途 |
| --- | --- |
| `docker-compose/docker-compose.yml` | 本地依赖和应用服务编排，支持 PostgreSQL、Redis、MinIO、Temporal、Core API、Realtime Gateway、Workflow Worker、AI Worker、Media Worker、Notification Service 和 Admin API，并允许通过环境变量覆盖镜像与端口。 |
| `docker-compose/.env.example` | 本地服务连接环境变量示例，包含端口、Realtime Gateway、Admin API、Notification Service、S3/MinIO 连接、Temporal 连接和镜像覆盖变量。 |
| `docker-compose/README.md` | 本地运行说明。 |
| `docker-compose/temporal-dynamicconfig/development-sql.yaml` | Temporal 本地动态配置。 |

## 维护规则

- 新增服务或端口变量时同步更新 `docker-compose/README.md`。
- 本地凭据只用于开发，不得复用到生产环境。
- 部署到正式环境时应在 `infra/terraform` 和 `infra/helm` 中独立建模。
