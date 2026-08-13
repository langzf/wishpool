# Module: apps/mobile/lib/src/features

## Purpose

`features/` 保存移动端主要页面和交互流程。页面只处理展示、表单、手势和反馈，业务读写通过 `WishPoolScope` 进入数据层。

## Important Files

- `auth_gate.dart`: 登录、家庭资料补全、儿童配对和会话恢复入口。
- `mobile_home.dart`: 角色化底部导航、首页快照加载、实时事件监听、任务提交弹层和媒体选择。
- `child_dashboard.dart`: 儿童今日任务、动态完成统计和空状态。
- `wish_screen.dart`: 心愿卡、碎片进度和兑现规则说明。
- `notification_screen.dart`: 站内通知列表、已读命令和家长通知偏好开关。
- `room_screen.dart`: 儿童小屋、回忆提示和小屋摆放命令。
- `parent_dashboard.dart`: 家长移动页的审核、今日任务跳过/延后、周计划展示和退出登录。

## Main APIs And Functions

- `AuthGate`: 根据本地 session 决定展示认证流、资料补全、儿童配对或主 App。
- `MobileHomeScreen`: 装配所有主页面并对相关实时事件刷新快照。
- `ChildDashboardScreen`: 展示未完成任务并触发打卡弹层。
- `ParentDashboardScreen`: 调用审核和任务调整命令。
- `NotificationScreen`: 调用通知已读和偏好更新命令。

## Data Flow And Integrations

页面从 `WishPoolSnapshot` 渲染数据。任务提交会使用文件选择器读取本地文件，然后经 repository 创建上传会话和 submission。实时事件来自 `SyncCoordinator`，命中任务、审核、奖励、心愿、纪念册、小屋或通知事件时刷新页面。

## Tests

`scripts/check-mobile.sh` 验证 feature 页面文件和关键类存在。有 Flutter SDK 时，widget 测试会覆盖 App 启动和认证入口。

## Maintenance Notes

- Keep this file updated when behavior, public APIs, routes, data models, or important files change.
