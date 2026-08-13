# Module: apps/mobile/lib/src/shared

## Purpose

`shared/` 保存移动端本地样例数据和轻量数据类。它用于未配置远程上下文时的演示体验，也作为移动 UI 的展示模型基线。

## Important Files

- `fixture_data.dart`: 定义 `ChildTask`、`WeeklyPlanRuleData`、`ReviewCardData`、`RoomItemData`、`NotificationItemData` 和 `NotificationPreferenceData`，并提供本地样例家庭、任务、心愿、周计划、小屋和通知数据。

## Main APIs And Functions

- `ChildTask`: 移动端今日任务展示与提交命令的输入模型。
- `WeeklyPlanRuleData`: 移动端家长页的周计划规则展示模型。
- `ReviewCardData`: 移动端家长审核卡片模型。
- `RoomItemData`: 移动端小屋摆放模型。
- `NotificationItemData` / `NotificationPreferenceData`: 通知中心和通知偏好的展示模型。

## Data Flow And Integrations

`WishPoolSnapshot` 在没有远程登录或儿童配对上下文时使用这里的样例数据。真实 API 数据会在 `data/wishpool_repository.dart` 中转换为同一组展示模型。

## Tests

`scripts/check-mobile.sh` 会验证移动端关键源码存在；有 Flutter SDK 时由 `apps/mobile/test/widget_test.dart` 覆盖 App 启动结构。

## Maintenance Notes

- Keep this file updated when behavior, public APIs, routes, data models, or important files change.
