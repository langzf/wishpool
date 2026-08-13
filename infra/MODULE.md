# Infrastructure Module

## 模块定位

`infra/` 保存 WishPool 本地开发和部署相关配置。当前已包含本地 Docker Compose 运行时，用于启动 PostgreSQL、Redis、MinIO、Temporal、Core API、Realtime Gateway、Workflow Worker、AI Worker、Media Worker、Notification Service、Admin API、Parent Web 和 Admin Web。

## 文件说明

| 文件 | 用途 |
| --- | --- |
| `docker-compose/docker-compose.yml` | 本地依赖和应用服务编排，支持 PostgreSQL、Redis、MinIO、Temporal、Core API、Realtime Gateway、Workflow Worker、AI Worker、Media Worker、Notification Service、Admin API、Parent Web 和 Admin Web，并允许通过环境变量覆盖镜像与端口。 |
| `docker-compose/.env.example` | 本地服务连接环境变量示例，包含端口、Web 客户端、Realtime Gateway、Admin API、Notification Service、S3/MinIO 连接、Temporal 连接和镜像覆盖变量。 |
| `docker-compose/README.md` | Docker 本地部署手册，覆盖配置、启动、访问、健康检查、日志、备份恢复和常见问题。 |
| `docker-compose/temporal-dynamicconfig/development-sql.yaml` | Temporal 本地动态配置。 |
| `../scripts/start-local.sh` | 一键启动本地运行时、家长 Web、管理后台 Web，并导入家庭数据。 |
| `../scripts/seed-local-data.mjs` | 通过 Core API 导入或刷新本地家庭、孩子、心愿、周计划、任务和配对码。 |

## 维护规则

- 新增服务或端口变量时同步更新 `docker-compose/README.md`。
- 本地凭据只用于开发，不得复用到生产环境。
- 部署到正式环境时应在 `infra/terraform` 和 `infra/helm` 中独立建模。
