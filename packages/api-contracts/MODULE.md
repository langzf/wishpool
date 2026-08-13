# API Contracts Module

## Purpose

`packages/api-contracts/` 保存 WishPool 的跨端 API 契约。后端实现、移动端 SDK、Web SDK 和契约测试都应以这里的 OpenAPI 文件为准。

## Important Files

- `openapi/wishpool.yaml`：OpenAPI 3.1 REST API 契约，覆盖认证、手机号验证码、家庭、儿童、配对、首页聚合、任务、媒体、提交、审核、心愿、纪念册、小屋、同步、隐私请求、管理治理和内部 worker API。
- `openapi-generator/*.yaml`：Kotlin server interface、Dart client、TypeScript client 的生成配置。

## Main APIs And Functions

- 当前模块不包含运行时代码。
- `wishpool.yaml` 是生成 Kotlin 服务端接口、Dart 客户端、TypeScript 客户端和 API 文档的源文件。
- `Home` tag 定义儿童首页和家长工作台聚合上下文，供移动端和 Web 首页稳定读取。
- `Internal` tag 下的 outbox 和 workflow activity 路由使用 `X-Internal-Token`，供 `services/workflow-worker` 消费，不面向普通客户端。
- `npm run lint:openapi` 使用 Redocly 校验契约。
- `npm run generate:openapi` 使用 OpenAPI Generator 生成多端代码到 `packages/api-contracts/generated/`。

## Data Flow And Integrations

- `services/core-api` 实现契约中的 REST API。
- `apps/mobile`、`apps/parent-web`、`apps/admin-web` 使用契约生成客户端类型和请求代码。
- 契约中的枚举、请求体、响应体需要与 `docs/DETAILED_DESIGN.md` 和 `db/migrations` 保持一致。

## Tests

- OpenAPI lint 已接入根目录 `npm run check`。
- 生成客户端配置已固化，生成产物默认不提交。

## Maintenance Notes

- Keep this file updated when behavior, public APIs, routes, data models, or important files change.
