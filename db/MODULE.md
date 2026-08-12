# Database Module

## 模块定位

`db/` 保存 WishPool PostgreSQL 数据库迁移和种子数据。数据库是任务、提交、审核、奖励、心愿、纪念册、媒体元数据、同步事件、通知、审计和隐私请求的事实源。

## 文件说明

| 文件 | 用途 |
| --- | --- |
| `migrations/V001__initial_schema.sql` | Flyway 初始迁移，创建身份、家庭、任务、提交、媒体、AI、审核、奖励、心愿、纪念册、小屋、事件、通知、审计和隐私请求表。 |
| `migrations/V002__auth_phone_codes.sql` | 手机号登录验证码表，支持本地 mock 短信 provider、验证码消费、过期和尝试次数控制。 |
| `migrations/V003__task_postpone_origin.sql` | 为延后任务增加 `original_task_instance_id`，保留原任务与新日期任务的追踪关系。 |
| `migrations/V004__weekly_plan_rule_superseded.sql` | 为周计划规则增加 `superseded_at`，二次保存计划时保留旧规则给历史任务引用。 |
| `migrations/V005__media_asset_related_resource.sql` | 为媒体资产增加 `related_type` 和 `related_id`，记录上传会话预期关联的任务、提交、审核、心愿或回忆资源。 |
| `migrations/V006__idempotency_records.sql` | 新增通用 `idempotency_record` 表，记录写命令幂等键与已创建资源的映射。 |
| `migrations/V007__media_wish_image_purpose.sql` | 扩展媒体用途枚举，允许心愿卡图片使用 `wish_image` 目的。 |
| `migrations/V008__outbox_publish_lease.sql` | 为 `outbox_event` 增加 `leased_until` 和 `last_error`，支持 outbox publisher 领取、失败释放和可观测重试。 |
| `migrations/V009__media_processing_lease.sql` | 为 `media_asset` 增加处理租约、可重试时间、尝试次数和错误字段，支持 media-worker 领取与失败重试。 |
| `flyway.conf.example` | 本地 PostgreSQL Flyway 命令配置示例。 |

## 维护规则

- 所有 schema 变更通过新的 Flyway migration 追加，不修改已应用迁移。
- 表、字段、状态枚举和索引需要与 `docs/DETAILED_DESIGN.md` 保持一致。
- 涉及奖励、审核、心愿和隐私删除的变更必须保留事务一致性和审计能力。
- 使用 `npm run check:db` 校验迁移文件命名和基本结构。
