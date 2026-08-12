# Packages Module

## 模块定位

`packages/` 保存跨端共享的契约、生成类型、设计令牌和样例数据。API 契约供后端、移动端和 Web 端生成客户端与类型；设计令牌和样例数据让客户端在接入真实接口前保持一致体验与数据语义。

## 文件说明

| 文件 | 用途 |
| --- | --- |
| `api-contracts/openapi/wishpool.yaml` | WishPool REST API OpenAPI 3.1 契约，覆盖认证、家庭、儿童、任务、媒体、提交、审核、心愿、纪念册、小屋、同步和隐私请求。 |
| `api-contracts/openapi-generator/` | 多端 OpenAPI 生成配置。 |
| `design-tokens/` | 跨端颜色、字体、间距、圆角、阴影和动效令牌。 |
| `app-fixtures/` | 家庭、儿童、今日任务、审核、心愿、纪念册、小屋和隐私请求样例数据。 |

## 维护规则

- API 变更先改 OpenAPI，再生成客户端和服务端接口。
- OpenAPI schema 中的枚举和字段需要与 `docs/DETAILED_DESIGN.md`、数据库 migration 保持一致。
- 后续新增 AsyncAPI、JSON Schema 或 Protobuf 时放在 `api-contracts/` 下。
- 客户端视觉变化优先调整 `design-tokens/`，再同步到具体端。
- 样例数据需要保持家庭闭环一致，不能只覆盖单个孤立页面。
