# Module: apps/parent-web/app

## Purpose

`app/` 是家长 Web 的 Next.js App Router 层，负责路由、页面装配、全局样式、Server Actions 和实时 SSE 代理。

## Important Files

- `actions.ts`: 封装手机号验证码、登录、家庭设置、儿童选择、退出、审核、任务调整、计划保存、配对码、心愿、纪念册、小屋和通知命令。
- `api/realtime/route.ts`: 使用 httpOnly cookie 中的访问令牌代理 Realtime Gateway SSE，避免浏览器直接持有后端 token。
- `layout.tsx`: 根布局和样式加载。
- `page.tsx`: 根据 cookie session 和家庭上下文展示登录、设置或工作台。
- `styles.css`: 家长端设计 token、布局、表单、工作台、空状态、通知和响应式样式。

## Main APIs And Functions

- Server Actions: `requestParentPhoneCodeAction`、`loginParentAction`、`completeParentFamilySetupAction`、`approveReviewAction`、`requestRevisionAction`、`skipTaskAction`、`postponeTaskAction`、`saveWeeklyPlanAction`、`createWishAction`、`exportMemoryAction`、`arrangeRoomItemAction`、`markNotificationsReadAction` 和 `updateNotificationPreferenceAction`。
- `GET /api/realtime`: 家长 Web 的同源 SSE 入口。

## Data Flow And Integrations

页面读取 `lib/session.ts` 的 cookie session。Server Actions 使用 Core API 读写业务数据并在成功后 `revalidatePath("/")`。SSE 代理读取会话 token 后连接 Realtime Gateway `/realtime/sse`。

## Tests

`npm run check --workspace @wishpool/parent-web` 会执行 Next 生产构建和结构检查。

## Maintenance Notes

- Keep this file updated when behavior, public APIs, routes, data models, or important files change.
