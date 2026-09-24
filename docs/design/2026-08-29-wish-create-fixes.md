# 创建心愿功能修复说明

日期：2026-08-29

## 背景

本轮只修复创建心愿的两个功能问题，不调整页面视觉风格：

1. 创建心愿时缺少“AI 生成图片”入口。
2. 目标碎片数为 10 时，使用 3x4 网格渲染出 12 个碎片 div，展示数量与目标数量不一致。

## 方案一：创建心愿增加 AI 生成图片入口

前端在 `WishCreateForm.tsx` 的心愿图片区新增“AI 生成”按钮，沿用现有按钮与状态区域样式。

交互规则：

- 心愿标题为空时按钮禁用，并通过按钮 title 提示“先填写心愿标题”。
- 点击后使用当前标题、备注、所选模型 providerCode 调用 parent-web server action。
- server action 代理调用 core-api 既有接口：
  - `POST /wishes/image-generations`
  - `GET /wishes/image-generations/{jobId}`
- 前端每 2 秒轮询一次，最多 30 次。
- 生成中禁用提交按钮。
- 生成成功后将返回的 `mediaAssetId` 自动写入 `selectedMediaId`，创建心愿时沿用隐藏字段 `imageMediaId` 提交。
- 上传、候选图、AI 生成三种图片选择互斥：选择其中一种会清理另两种本地状态。
- 失败时显示后端 `errorMessage`，没有错误详情时显示中文兜底文案。

预览 URL：

- core-api 原有 `WishImageGenerationJobResponse` 只有 `mediaAssetId`。
- 项目内 downloadUrl 由 `MediaService.toResponse` 生成，且没有公开 `GET /media/{id}`。
- 因此在既有生图 job 响应上新增可选字段 `media: MediaAssetResponse?`，不新增路由，保留原 `mediaAssetId` 兼容旧调用方。

## 方案二：碎片矩形切分算法

统一算法目标：`rows * cols == requiredFragments`。

算法：

1. 若外部传入 rows/cols，且二者为正整数并且乘积等于 target，则使用外部布局。
2. 否则从 `floor(sqrt(target))` 向下寻找最大因数作为 rows。
3. cols 使用 `target / rows`。
4. 这样得到的长方形尽量接近正方形，且格子数量一定等于 target。

示例：

- 6 -> 2x3
- 8 -> 2x4
- 9 -> 3x3
- 10 -> 2x5
- 12 -> 3x4
- 16 -> 4x4
- 20 -> 4x5
- 7 -> 1x7
- 11 -> 1x11

前端防御：

- `FragmentGrid` 和 Flutter `_FragmentBoard` 都会在外部 rows/cols 与 target 不匹配时忽略旧值并重算。
- 因为新算法保证 `cells == target`，已点亮数量直接使用 `current` 并做 `0..cells` 越界保护，不再按比例映射到更大的 cells。

后端处理：

- 创建心愿时由 core-api 统一派生 `fragment_grid_rows` 和 `fragment_grid_cols`，不再依赖调用方传入。
- 返回 `fragmentVisual` 时，如果历史数据中的 rows/cols 与 requiredFragments 不匹配，会自动忽略并使用新算法。
- `revealed` 直接按 earnedFragments 做 `0..total` 越界保护。

数据库约束：

- 旧迁移把 `fragment_grid_rows` 和 `fragment_grid_cols` 限制在 1..4。
- 需求示例中 `20 -> 4x5` 需要 `cols=5`，因此新增迁移 `V013__relax_wish_fragment_grid_constraints.sql`，将约束放宽为正整数。

## 改动清单

- `apps/parent-web/app/actions.ts`
  - 新增 `createWishImageGeneration`
  - 新增 `getWishImageGenerationJob`
- `apps/parent-web/app/wish/WishCreateForm.tsx`
  - 新增 AI 生成入口、轮询状态、成功预览和互斥清理
- `apps/parent-web/components/FragmentGrid.tsx`
  - 改为因数分解布局
  - rows/cols 与 target 不一致时重算
  - filled 改为直接按 current 映射
- `apps/mobile/lib/src/features/wish_screen.dart`
  - 同步碎片布局与 filled 计算
- `services/core-api/src/main/kotlin/com/wishpool/core/wishes/WishDtos.kt`
  - 生图 job 响应新增可选 `media`
- `services/core-api/src/main/kotlin/com/wishpool/core/wishes/WishImageGenerationService.kt`
  - 生图 job 响应补齐媒体 downloadUrl
- `services/core-api/src/main/kotlin/com/wishpool/core/wishes/WishService.kt`
  - 创建心愿统一派生 rows/cols
  - 返回碎片视觉信息时防御历史不匹配数据
- `db/migrations/V013__relax_wish_fragment_grid_constraints.sql`
  - 放宽碎片网格行列数数据库约束
- `packages/api-contracts/openapi/wishpool.yaml`
  - 同步生图 job 响应的可选 `media` 字段

## 验证建议

- parent-web 容器内执行：
  - `docker exec wishpool-parent-web sh -c "cd /workspace/apps/parent-web && npx tsc --noEmit"`
- core-api 重启后验证：
  - 新建 10 块心愿，确认后端返回 `fragmentVisual.total=10`、`rows=2`、`cols=5`。
  - 历史 10 块心愿即使存了 3x4，也应显示为 2x5。
  - 点击“AI 生成”，成功后应显示预览并提交 `imageMediaId`。
