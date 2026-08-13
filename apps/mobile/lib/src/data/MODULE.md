# Module: apps/mobile/lib/src/data

## Purpose

`data/` 负责移动端应用服务层：认证、业务数据读取、写命令、上传队列和实时同步。UI 不直接调用 HTTP 客户端，而是通过 `WishPoolScope` 使用这里的能力。

## Important Files

- `auth_repository.dart`: 封装手机号验证码、登录、家庭创建、儿童创建和儿童配对。
- `sync_coordinator.dart`: 维护家庭 WebSocket 连接、事件游标、本地游标持久化和重连。
- `upload_queue.dart`: 保存待上传媒体任务的内存队列，用于失败提示和后续恢复能力。
- `wishpool_repository.dart`: 读取儿童首页、家长工作台、通知偏好，并执行提交、审核、跳过、延后、小屋摆放和通知命令。
- `wishpool_scope.dart`: Flutter `InheritedWidget`，向页面暴露 repository、API client、同步器和上传队列。

## Main APIs And Functions

- `WishPoolRepository.loadHomeSnapshot()`: 将 `/children/{childId}/home-context`、`/families/{familyId}/parent-dashboard`、通知和偏好转换为 `WishPoolSnapshot`。
- `submitManualTask()` / `submitMediaTask()`: 创建提交；媒体任务会先创建上传会话、直传对象、finalize，再关联 submission。
- `approveReview()` / `requestRevision()`: 提交家长审核决定。
- `skipTask()` / `postponeTask()`: 移动端家长页调整当天任务。
- `arrangeRoomItem()`: 保存小屋元素位置。
- `markNotificationsRead()` / `updateNotificationPreference()`: 管理站内通知和偏好。
- `SyncCoordinator.start()`: 建立实时事件流并在断线后按 cursor 恢复。

## Data Flow And Integrations

认证会话由 `auth_session_store.dart` 存入 `shared_preferences`。业务读取走 Core API 聚合接口；媒体上传使用 Core API 返回的对象存储签名 URL；实时同步连接 Realtime Gateway，并由页面按事件类型触发刷新。

## Tests

`scripts/check-mobile.sh` 覆盖关键类和文件存在性。有 Flutter SDK 时，`apps/mobile/test/widget_test.dart` 会验证 App 的认证入口结构。

## Maintenance Notes

- Keep this file updated when behavior, public APIs, routes, data models, or important files change.
