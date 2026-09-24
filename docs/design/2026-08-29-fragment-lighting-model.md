# Wishpool 碎片化与逐个点亮三部分模型

日期：2026-08-29

## 阶段 0：当前实现分析

### 1. 当前碎片怎么切分

- 创建心愿时，`WishService.createWish` 调用 `deriveFragmentGrid(requiredFragments)` 计算 `rows/cols`，并写入 `wish.fragment_grid_rows`、`wish.fragment_grid_cols`。
- `deriveFragmentGrid` 从 1 遍历到平方根，保留最后一个因数对，所以会得到尽量接近正方形的布局：10 -> 2 x 5，质数 -> 1 x n。
- `fragment_visual_mode`、`fragment_grid_rows`、`fragment_grid_cols` 由迁移 `V010` 引入，`V013` 放宽了 rows/cols 的上限约束。
- web `FragmentGrid.fragmentLayout` 和 Flutter `_fragmentLayout` 都有历史数据防御：如果存储的 rows/cols 缺失或乘积不等于 target，就重新按因数分解派生。
- `irregular` 多边形不是持久化数据。web 用 `irregularPolygon(index)` 的 5 个固定形状轮换，Flutter 用 `_ShardClipper(index)` 的同一组比例点轮换。
- `puzzle_lines` 不是真实拼图片几何。web 通过 CSS `border-radius`、伪元素凸点和 nth-child 做视觉效果；Flutter 通过 `_PuzzleLinePainter` 在 tile 边缘画线。

### 2. 当前蒙层怎么做

- web 用 `.fragment-card-backdrop` 展示完整原图背景，`background-size: cover`。
- `.fragment-cells` 是覆盖在整图上的 CSS Grid。每个 `.fragment-cell` 默认是半透明深色、`backdrop-filter: blur(...)`、锁图标，形成“未点亮=磨砂/锁住”。
- 点亮时加 `.filled`，把 cell 背景设为 transparent，隐藏锁和纹理，等于在整图背景上挖开一片。
- `irregular` 用 `clip-path: polygon(...)` 裁剪每个覆盖 cell；`puzzle_lines` 主要靠 CSS 边角和伪元素线条；`grid_reveal` 是普通矩形。
- 每片“样子”目前完全由前端按 index 实时计算，没有持久化 mask，也没有后端返回每片 polygon。
- Flutter 也是同样思路：底层 `Image.network` 展示整图，上层 `GridView` 覆盖 tile；未点亮 tile 深色，点亮 tile 透明。

### 3. 当前点亮状态存了什么

- 数据库存储的是 `wish.earned_fragments` 计数。
- API 返回 `earnedFragments` 和 `fragmentVisual.revealed`，没有返回点亮集合。
- web 和 Flutter 都用 `index < current/filled` 判断是否点亮，含义是“前 N 个格子点亮”。
- 点亮顺序隐式固定为 DOM/GridView 的行优先 index 顺序，不能表达随机顺序、自定义顺序或撤销具体碎片。

### 4. 点亮动作

- 没有独立的“点亮碎片”业务接口。
- 任务提交审核通过后，`RewardService.evaluateWorkflowEvent("review.approved")` 最终调用 `refreshDailySummaryAndWish`。
- 当某天核心任务达到完成条件，`grantWishFragment` 写入一条 `reward_ledger`：`reward_type='wish_fragment'`、`amount=1`、`wish_id=<active wish>`。
- 如果后来审核撤销或当天不再满足条件，`adjustWishFragment` 写入 adjustment 负数。
- `refreshWishProgress` 把 `reward_ledger` 中 `wish_fragment/adjustment` 的金额汇总为 `wish.earned_fragments`，并在达到 required 时把状态改为 `unlocked`。
- 幂等主要依赖 `reward_ledger.idempotency_key` 唯一约束；并发下重复 grant 会被 `DuplicateKeyException` 吞掉。`refreshWishProgress` 是按账本重算的，计数层面可恢复一致。

### 5. 生成图片后的碎片化

- AI 生成图片走 `POST /wishes/image-generations` 创建 job，完成后产出 `media_asset`。
- 创建心愿时把生成图的 `mediaAssetId` 作为 `imageMediaId` 传给 `/wishes`。
- 后续碎片化与上传图完全同一套逻辑：心愿创建时只保存 rows/cols/mode，渲染端用完整图片背景 + 覆盖层。
- 生成图没有特殊的裁剪、mask、lit 数据。

### 6. 与三部分模型的差距

- 原始图片已有：`wish.image_media_id -> media_asset`。
- 裁剪/蒙层信息缺失：没有持久化每片形状、顺序、polygon 或 mask 版本。
- 当前点亮状态不足：只存计数，没有存点亮集合。
- 点亮顺序不可控：前端默认前 N 个，不能让后端或运营定义逐步点亮顺序。
- 跨端一致性弱：web/Flutter 各自实现同一组规则；一旦规则变化，历史心愿的外观会跟着变化。
- `irregular` 的视觉定义不稳定：没有把每片 polygon 固化到心愿记录里。

## 阶段 1：目标架构

### 三部分数据模型

1. 原始图片
   - 继续使用 `wish.image_media_id` 关联 `media_asset`。
   - `WishResponse.imageMedia.downloadUrl` 给 web/mobile 展示完整图。

2. 裁剪/蒙层信息
   - 新增 `wish.fragment_mask_json jsonb`。
   - 创建心愿时生成并持久化 mask，历史数据由迁移按现有 rows/cols/mode 派生。
   - `grid_reveal` 和 `puzzle_lines` 可由 rows/cols/mode + cell index 表达；`irregular` 固化每片 polygon 点位，避免跨端漂移。

示例：

```json
{
  "version": 1,
  "mode": "irregular",
  "rows": 2,
  "cols": 5,
  "total": 10,
  "revealOrder": [0, 1, 2, 3, 4, 5, 6, 7, 8, 9],
  "cells": [
    {
      "index": 0,
      "row": 0,
      "col": 0,
      "polygon": [
        { "x": 0.02, "y": 0.08 },
        { "x": 0.88, "y": 0.0 },
        { "x": 1.0, "y": 0.72 },
        { "x": 0.18, "y": 1.0 }
      ]
    }
  ]
}
```

3. 当前点亮状态
   - 新增 `wish.fragment_lit_json jsonb`。
   - 结构为 `{ "version": 1, "litIndexes": [0, 1, 2] }`。
   - `earned_fragments` 暂时保留为兼容计数字段和状态汇总字段；读接口返回 `earnedFragments`，同时返回 `fragmentVisual.litIndexes`。
   - 历史心愿没有 mask/lit 时，服务端和前端都按 rows/cols/mode + earnedFragments 派生。

### 存储与接口

- 使用 `wish` 表 JSONB 列，不拆关联表。原因：mask/lit 是心愿聚合内的小体积结构，读心愿时必需，独立表会增加读路径复杂度。
- 新迁移 `V014__wish_fragment_mask_lighting.sql`：
  - `fragment_mask_json jsonb`
  - `fragment_lit_json jsonb not null default '{"version":1,"litIndexes":[]}'`
  - 为现有心愿回填 mask/lit。
- 创建心愿：
  - 先按 `requiredFragments` 计算 rows/cols。
  - 用 rows/cols/mode 生成完整 mask。
  - 初始 lit 为空集合。
- 点亮更新：
  - 仍由 `RewardService` 根据任务和账本驱动。
  - `refreshWishProgress` 汇总出 earned 数量后，根据持久化 mask 的 `revealOrder` 取前 earned 个 index 写入 `fragment_lit_json`。
  - 撤销时同样重算集合，天然幂等。
  - 并发仍以 reward ledger 唯一键和按账本重算为主；`fragment_lit_json` 是 `earned_fragments` 的确定性投影。
- 读接口：
  - `Wish.fragmentVisual` 增加 `mask` 和 `litIndexes`。
  - 保留 `mode/rows/cols/revealed/total`，现有客户端不会被破坏。

### 渲染端消费

- web `FragmentGrid`：
  - 优先使用 `mask.cells` 作为 cell 列表和 polygon。
  - 使用 `litIndexes` Set 判断点亮，而不是 `index < current`。
  - 无 mask/lit 时保持旧逻辑兜底。
- Flutter `_FragmentBoard`：
  - `WishPoolSnapshot` 增加 mask/lit 字段。
  - `_FragmentTile` 优先使用 mask cell polygon。
  - 无 mask/lit 时保持旧逻辑。

### 逐步点亮顺序

- 顺序存于 `mask.revealOrder`。
- 当前实现先使用稳定行优先顺序，后续可替换为随机或策划顺序，只要创建时固化即可。
- 前端不再推断“前 N 个”，而是消费 `litIndexes`。

### 实施计划

1. 数据库迁移：新增 JSONB 列，回填历史 mask/lit。
2. 后端 DTO/mapper/service：生成、读取、返回 mask/lit；奖励汇总同步 lit。
3. OpenAPI：补充 `WishFragmentMask`、`WishFragmentCell`、`WishFragmentPoint` 和 `litIndexes`。
4. web：数据映射与 `FragmentGrid` 改为 mask/lit 驱动。
5. mobile：snapshot/repository/wish_screen 改为 mask/lit 驱动。
6. 验证：parent-web tsc、core-api compileKotlin offline、Flutter analyze 可用则执行。

### 风险与取舍

- `fragment_lit_json` 由账本投影得到，不作为独立业务事实源；好处是撤销和重算简单，代价是暂不能表达“手动点亮任意指定片”。
- 历史数据回填在迁移中使用行优先 revealOrder，确保和旧 UI 行为一致。
- Flutter 的 puzzle 几何仍由 painter 表达，mask 主要固化 cell 和 irregular polygon；后续如果要精确拼图凸凹形状，可扩展 cell 的 edge metadata。
