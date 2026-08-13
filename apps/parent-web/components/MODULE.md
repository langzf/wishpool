# Module: apps/parent-web/components

## Purpose

`components/` 保存家长 Web 的可复用 React 组件，包括应用外壳、认证/设置面板、工作台和实时刷新器。

## Important Files

- `AuthPanel.tsx`: 手机号登录、验证码提交、家庭/儿童选择和家庭资料补全界面。
- `Dashboard.tsx`: 家长工作台 UI，展示真实聚合数据并触发审核、任务调整、计划保存、心愿创建、纪念册导出、小屋摆放、配对码和通知命令。
- `RealtimeRefresh.tsx`: 同源 SSE 客户端，维护浏览器端 cursor，并在家庭事件到达时刷新当前路由。
- `Shell.tsx`: 桌面侧栏和移动底部导航的应用外壳。

## Main APIs And Functions

- `ParentDashboard`: 渲染工作台指标、待审核、今日任务、心愿、计划、纪念册、小屋、通知和本地服务状态。
- `AuthPanel`: 根据 URL 参数展示验证码、错误、调试码和设置流程。
- `RealtimeRefresh`: 连接 `/api/realtime` 并按事件类型调用 `router.refresh()`。

## Data Flow And Integrations

组件接收 `lib/dashboard-data.ts` 提供的 `ParentDashboardData`。写操作通过 `app/actions.ts` 的 Server Actions 调用 Core API；实时刷新通过 `app/api/realtime/route.ts` 代理 Realtime Gateway。

## Tests

`npm run check --workspace @wishpool/parent-web` 会执行 `next build` 和结构检查。

## Maintenance Notes

- Keep this file updated when behavior, public APIs, routes, data models, or important files change.
