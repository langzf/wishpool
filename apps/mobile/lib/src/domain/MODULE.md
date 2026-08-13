# Module: apps/mobile/lib/src/domain

## Purpose

`domain/` 保存移动端跨页面共享的领域快照模型。这里不直接访问网络、存储或平台能力，只定义 UI 所需的稳定数据形状。

## Important Files

- `wishpool_snapshot.dart`: 定义 `WishPoolSnapshot`，聚合儿童姓名、心愿进度、碎片数、今日任务、审核卡片、周计划规则、小屋、回忆和通知数据。

## Main APIs And Functions

- `WishPoolSnapshot`: 移动端首页、心愿、通知、小屋和家长页共享的只读快照。
- `fixtureSnapshot`: 未登录或远程加载失败时使用的本地样例快照。

## Data Flow And Integrations

`WishPoolRepository.loadHomeSnapshot()` 从 Core API 聚合接口读取真实数据并映射到 `WishPoolSnapshot`。各 feature 页面只读取该快照，写命令通过 `WishPoolScope` 回到 repository。

## Tests

`scripts/check-mobile.sh` 验证领域模型文件存在。有 Flutter SDK 时，移动端 widget 测试会经过 `WishPoolScope` 和 `AuthGate` 加载快照。

## Maintenance Notes

- Keep this file updated when behavior, public APIs, routes, data models, or important files change.
