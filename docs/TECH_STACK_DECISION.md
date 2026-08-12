# WishPool 技术决策

版本：v1.0  
日期：2026-08-12

## 1. 总结

WishPool 的架构不是 Firebase 小后端，也不是单一语言包打天下。推荐技术组合是：

```text
移动/平板：Flutter + Dart
家长 Web / 运营后台：Next.js + TypeScript
核心业务后端：Kotlin + Spring Boot
实时网关：Kotlin + Ktor/Spring WebFlux
AI/模型/媒体处理：Python + FastAPI + workers
数据库：PostgreSQL
工作流：Temporal
对象存储：S3-compatible private buckets
缓存与连接状态：Redis
事件：PostgreSQL Outbox + Event Bus
部署：Docker + Kubernetes + Terraform + Helm
```

当前开发环境按自用本地部署设计：

- 本地依赖使用 Docker Compose。
- 登录主路径使用手机号验证码，微信登录作为 provider adapter 预留。
- 推送通道属于部署配置，不阻塞核心业务开发。
- API 契约先用 OpenAPI 固化，数据库变更通过 Flyway migration 管理。

## 2. 各端选择

| 端 | 架构 | 语言 | 脚手架 |
| --- | --- | --- | --- |
| 儿童平板端 | Flutter feature modules + offline sync | Dart | `flutter create apps/mobile` |
| 儿童手机端 | 与平板同 App，响应式布局 | Dart | 同上 |
| 家长手机端 | 与儿童端同 App，角色路由分流 | Dart | 同上 |
| 家长 Web | Next.js App Router | TypeScript | `create-next-app apps/parent-web` |
| 运营后台 | Next.js Admin | TypeScript | `create-next-app apps/admin-web` |

移动端坚持 Flutter 的原因：

- 儿童小屋、心愿卡、拼图和成长动画需要稳定一致的渲染体验。
- 平板和手机形态都重要，一个 Flutter App 可以共享大部分领域逻辑。
- 设备能力可以通过 Flutter plugin 和 platform channel 接入。

Web 端不用 Flutter Web 的原因：

- 家长 Web 和后台更像工作台，重表单、表格、检索、编辑和权限管理。
- React/Next.js 生态对后台和内容编辑器更成熟。

## 3. 服务端选择

| 服务 | 架构 | 语言 | 脚手架 |
| --- | --- | --- | --- |
| Core API | 模块化领域服务 | Kotlin | Spring Boot + Gradle Kotlin DSL |
| Realtime Gateway | WebSocket/SSE gateway | Kotlin | Ktor 或 Spring WebFlux |
| Workflow Worker | Durable workflow workers | Kotlin | Temporal SDK |
| Notification Service | Push/站内通知 | Kotlin | Spring Boot |
| AI Gateway | 模型路由与 schema 校验 | Python | FastAPI + Pydantic |
| AI Worker | OCR/ASR/Vision/LLM 执行 | Python | uv/Poetry worker |
| Media Worker | 转码、缩略图、波形、抽帧 | Python | FFmpeg + object storage SDK |

核心业务后端用 Kotlin，而不是 TypeScript：

- WishPool 的难点是状态机、事务、审计、权限和奖励账本。
- Kotlin/Spring Boot 对长期领域建模、事务控制、测试和工程治理更稳。
- TypeScript 留给 Web、工具层和生成 SDK 更合适。

AI/媒体用 Python：

- OCR、ASR、Vision、LLM、FFmpeg、OpenCV、Pydantic 生态都在 Python 最强。
- 模型服务需要快速实验和替换，不应该绑死在核心业务进程里。

## 4. 数据与基础设施

| 能力 | 选择 | 原因 |
| --- | --- | --- |
| 事实库 | PostgreSQL | 强关系、事务、约束、聚合、审计 |
| 本地缓存 | Drift/SQLite | 移动端离线和断网恢复 |
| 对象存储 | S3-compatible | 私有媒体、签名 URL、生命周期 |
| 缓存 | Redis | 会话、限流、连接索引 |
| 工作流 | Temporal | 长流程、重试、补偿、可观测 |
| 事件 | Outbox + Event Bus | 保证状态写入和事件发布一致 |
| 部署 | Kubernetes | 多服务、worker、实时连接、AI 资源隔离 |
| IaC | Terraform | 环境可复现 |
| 可观测 | OpenTelemetry | 跨端、后端、AI 链路追踪 |

## 5. Firebase 的位置

Firebase 不作为 WishPool 核心架构。可以作为候选补充：

- Crashlytics：移动崩溃监控候选。
- Remote Config：移动端实验配置候选。
- FCM：海外 Android 推送候选。

但以下能力不交给 Firebase 作为事实源：

- 任务。
- 提交。
- 审核。
- 奖励。
- 心愿。
- 纪念册。
- 儿童媒体权限。
- AI 预审结果。

原因是项目需要自有数据模型、关系约束、事务、审计、隐私删除和模型处理治理。

## 6. 判断

最适合 WishPool 的不是“单栈最省事”，而是职责清楚的多栈：

- Dart/Flutter 负责最重要的家庭移动体验。
- TypeScript/Next.js 负责 Web 工作台。
- Kotlin/Spring Boot 负责可信业务事实。
- Python/FastAPI 负责 AI 和媒体智能。
- PostgreSQL/Temporal/Object Storage 负责长期一致性、长任务和媒体资产。
