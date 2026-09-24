# 心愿卡图片复用/生成与碎片拆分方案设计

日期：2026-08-27  
项目：WishPool 星愿小屋  
范围：设计分析，不改动现有业务代码逻辑

## 1. 结论摘要

本设计建议把心愿图片能力拆成三层：先查家庭内可复用图片，再允许用户生成或上传，最后允许无图创建。图片复用与生成都应落到现有 `media_asset` 体系，`purpose='wish_image'`，由 `wish.image_media_id` 引用。现有 schema 缺少标题、标签、来源、语义键等检索字段，因此需要新增轻量元数据表，而不是把业务语义硬塞进 `media_asset`。

碎片视觉推荐采用“数值进度仍为权威 + 前端按整图裁切/遮罩渲染碎片格”的路线，第一期不为每块碎片创建独立业务实体。若需要更强静态资源复用，可由 `media-worker` 额外生成拼图预览衍生图，但不建议每块碎片独立 AI 生成。

## 2. 代码现状复核

### 2.1 心愿域

关键路径：

- `services/core-api/src/main/kotlin/com/wishpool/core/wishes/WishController.kt`
- `services/core-api/src/main/kotlin/com/wishpool/core/wishes/WishService.kt`
- `services/core-api/src/main/kotlin/com/wishpool/core/wishes/WishDtos.kt`
- `services/core-api/src/main/kotlin/com/wishpool/core/wishes/WishMappers.kt`

已确认现状：

| 项 | 现状 |
| --- | --- |
| 创建入口 | `POST /wishes` |
| 创建 DTO | `CreateWishRequest(familyId, childId, weekId, title, note?, imageMediaId?, requiredFragments, rewardMode)` |
| 图片处理 | `imageMediaId` 可空；传入时 `WishService.validateWishImage` 校验归属、`purpose='wish_image'`、状态为 `uploaded/ready`、content type 为 image |
| 自动复用/生成 | 无 |
| 进度模型 | `wish.required_fragments` / `wish.earned_fragments` 数值字段 |
| 初始状态 | 创建为 `draft`，当前家长端随后调用 `/wishes/{wishId}/activate` |

`WishService.createWish` 只负责创建心愿并发布 `wish.created` 事件，没有自动查询类似图片，也没有异步生成图片任务。

### 2.2 媒体域

关键路径：

- `services/core-api/src/main/kotlin/com/wishpool/core/media/MediaController.kt`
- `services/core-api/src/main/kotlin/com/wishpool/core/media/MediaService.kt`
- `services/core-api/src/main/kotlin/com/wishpool/core/media/MediaDtos.kt`
- `db/migrations/V001__initial_schema.sql`
- `db/migrations/V005__media_asset_related_resource.sql`
- `db/migrations/V007__media_wish_image_purpose.sql`
- `db/migrations/V009__media_processing_lease.sql`

已确认现状：

| 项 | 现状 |
| --- | --- |
| 上传流程 | `POST /media/upload-sessions` 创建 `upload_pending` 记录与 presigned PUT URL；客户端上传；`POST /media/{mediaId}/finalize` 标记 `uploaded` |
| 处理流程 | `media-worker` 通过 `/internal/media/processing/claim` 领取 `uploaded/processing/failed`，生成 derivatives 后回调 completed/failed |
| 存储 | S3 兼容对象存储，默认 bucket `wishpool-media` |
| 媒体用途 | 当前允许 `submission, feedback, wish_image, wish_redemption, memory_export` |
| related resource | `media_asset.related_type/related_id` 支持 `task_instance, submission, review, wish, wish_redemption, weekly_memory` |
| derivatives | 图片必需 `thumbnail, preview, ai_ready` |
| 缺口 | `media_asset` 没有 title、description、tags、source、prompt、similarity key、可见性/复用策略等字段 |

`media-worker/media_worker/processor.py` 已使用 Pillow 处理图片，具备继续生成拼图衍生图或遮罩预览图的基础能力。

### 2.3 AI Worker

关键路径：

- `services/ai-worker/ai_worker/app.py`
- `services/ai-worker/ai_worker/provider.py`
- `services/ai-worker/ai_worker/models.py`
- `services/ai-worker/ai_worker/config.py`

已确认现状：

| 项 | 现状 |
| --- | --- |
| 服务形态 | Python `BaseHTTPRequestHandler` HTTP 服务 |
| provider 抽象 | `AiProvider` + `DeterministicAiProvider` + `create_provider(name)` |
| 现有能力 | `precheck_submission`、`draft_feedback`、`generate_memory_narrative`、`summarize_privacy_request` |
| 图片生成 | 无 |
| 雏形 | `generate_memory_narrative` 返回 `cover_prompt`，说明已有“文本生成图片提示词”的产品方向 |

### 2.4 API 契约与前端

实际 OpenAPI 路径为 `packages/api-contracts/openapi/wishpool.yaml`，不是任务描述中的 `openapi/wishpool.yaml`。`openapitools.json` 存在，Flutter 依赖生成的 `wishpool_api` 包，契约变更必须同步该 YAML 并重新生成客户端。

家长端：

- `apps/parent-web/app/wish/page.tsx` 有心愿创建表单与历史心愿卡库。
- `apps/parent-web/app/actions.ts` 的 `createWishAction` 只提交 `familyId, childId, weekId, title, note, requiredFragments, rewardMode`，没有上传心愿图或传 `imageMediaId`。
- 当前“心愿卡库”按历史 title 聚合，只复用标题、说明、碎片数，不复用媒体。

移动端：

- `apps/mobile/lib/src/features/wish_screen.dart` 使用本地渐变卡片和 `_FragmentGrid` 展示碎片。
- `apps/mobile/lib/src/data/wishpool_repository.dart` 从 `/children/{childId}/home-context` 映射 `earnedFragments/requiredFragments`，没有把 `imageMedia` 放入 `WishPoolSnapshot`。
- 移动端已有任务媒体上传流程：`/media/upload-sessions` → presigned PUT → `/media/{mediaId}/finalize` → `/submissions`，可复用为家长端/移动端上传心愿图的交互模型。

## 3. 设计目标与非目标

### 3.1 目标

- 心愿创建前给出可确认的图片复用建议，减少重复生成和重复上传。
- 接入云中立图片生成能力，provider 可切换，API key 不进入前端。
- 生成图片最终进入 `media_asset`，可被现有媒体处理、下载 URL、权限模型复用。
- 儿童端获得直观“碎片格”体验，不把进度退化成纯数字。
- 保持 `earnedFragments/requiredFragments` 为第一期权威进度模型，降低 reward 逻辑变更风险。

### 3.2 非目标

- 本设计阶段不修改业务代码、不新增 migration 文件、不修改 OpenAPI。
- 第一阶段不做跨家庭图片共享市场。
- 第一阶段不做人脸、儿童照片风格迁移或基于真实儿童形象生成，避免隐私和合规复杂度。
- 第一阶段不引入每块碎片独立业务生命周期。

## 4. 图片复用策略

### 4.1 “同类”定义

建议第一期采用可解释的规则评分，而不是直接上向量检索：

| 维度 | 规则 | 分值建议 |
| --- | --- | --- |
| 家庭/孩子范围 | 同一 `family_id` 且优先同一 `child_id`；同家庭其他孩子次之 | 必要条件，同孩子 +20 |
| 图片用途与状态 | `media_asset.purpose='wish_image'` 且 `status in ('uploaded','ready')` | 必要条件 |
| 标题归一化 | 去空格、大小写、全半角归一、常见量词/标点清洗后完全相等 | +50 |
| 关键词重合 | 从 wish title/note 提取名词关键词，Jaccard 相似度 | 0 到 +30 |
| 类目/标签 | 手工或模型生成标签重合，如 `science/toy/book/outdoor` | 0 到 +25 |
| 历史成功 | 被 redeemed 的同标题心愿，或 realizedCount 更高 | +10 |
| 时间窗 | 最近 180 天 +10；超过 2 年 -10 | -10 到 +10 |
| 安全过滤 | 被标记 `reuse_allowed=false`、删除、失败、非图片排除 | 必要条件 |

推荐阈值：

- `score >= 70`：作为默认推荐，可放在首位。
- `50 <= score < 70`：作为“可能相关”备选。
- `< 50`：不推荐。

### 4.2 现有 schema 能否支持

现有 schema 只能支持以下弱查询：

```sql
select *
from media_asset
where family_id = :family_id
  and child_id = :child_id
  and purpose = 'wish_image'
  and status in ('uploaded', 'ready')
order by created_at desc
limit 20;
```

这只能做“同家庭/同孩子最近图片”推荐，无法判断是否与“一台显微镜”同类。需要新增业务元数据。

### 4.3 建议新增数据结构

最小推荐新增表：`wish_image_asset_profile`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `media_asset_id` | uuid PK/FK | 指向 `media_asset(id)` |
| `family_id` | uuid | 冗余用于查询与 RLS/权限过滤 |
| `child_id` | uuid null | 与媒体一致 |
| `source_type` | text | `uploaded/generated/reused/imported` |
| `source_wish_id` | uuid null | 首次绑定或生成时的 wish |
| `title_snapshot` | text | 生成/上传时的心愿标题快照 |
| `note_snapshot` | text null | 心愿说明快照 |
| `normalized_title` | text | 归一化标题 |
| `keywords` | text[] | 关键词，如 `['显微镜','科学','观察']` |
| `category` | text null | 粗分类，如 `science/toy/book/trip/food/art/sport/custom` |
| `tags` | text[] | 展示或检索标签 |
| `prompt` | text null | 生成 prompt，不返回儿童端 |
| `provider` | text null | 生成供应商 |
| `model` | text null | 生成模型 |
| `reuse_allowed` | boolean | 默认 true |
| `created_at`/`updated_at` | timestamptz | 审计 |

建议索引：

- `idx_wish_image_profile_family_child_created` on `(family_id, child_id, created_at desc)`
- `idx_wish_image_profile_normalized_title` on `(family_id, normalized_title)`
- `idx_wish_image_profile_keywords_gin` on `keywords` using GIN
- `idx_wish_image_profile_tags_gin` on `tags` using GIN
- 可选：启用 `pg_trgm` 后对 `normalized_title` 建 GIN trigram 索引，用于中文/混合标题模糊匹配。

说明：不建议直接在 `media_asset` 上加大量业务字段。`media_asset` 当前是通用媒体资产，submission、feedback、memory_export 也复用；图片复用语义只属于 wish image 场景。

### 4.4 复用查询 API 设计

新增端点：

```http
POST /wishes/image-candidates
```

请求：

```json
{
  "familyId": "uuid",
  "childId": "uuid",
  "title": "一台显微镜",
  "note": "想观察叶子和昆虫",
  "limit": 6
}
```

响应：

```json
{
  "query": {
    "normalizedTitle": "显微镜",
    "keywords": ["显微镜", "科学", "观察"],
    "category": "science"
  },
  "items": [
    {
      "media": { "...": "MediaAsset" },
      "score": 86,
      "reason": "同一孩子历史心愿标题相近，科学类标签匹配",
      "sourceWishId": "uuid",
      "sourceWishTitle": "儿童显微镜",
      "lastUsedAt": "2026-07-12T08:00:00Z"
    }
  ],
  "fallbackOptions": ["generate", "upload", "skip"]
}
```

为什么用 `POST`：请求体包含标题、说明、后续可能包含可配置偏好；没有副作用但查询条件较复杂，用 POST 可以避免 URL 长度和编码问题。

可选增强端点：

- `GET /wishes/{wishId}/image-candidates`：编辑心愿时基于已有 wish 查询。
- `POST /wishes/{wishId}/image`：绑定/替换心愿图片，避免创建时必须一次完成。

### 4.5 前端交互流程

家长端建议流程：

1. 家长输入标题和说明后，前端 debounce 调 `POST /wishes/image-candidates`。
2. 若有 `score >= 70`，展示“推荐使用这张图”，必须由家长确认后才把 `imageMediaId` 放入 create request。
3. 若只有弱候选，展示最多 3 张备选，不自动选中。
4. 无候选时展示两个主要动作：生成图片、上传图片；第三选项为“先不放图”。
5. 创建心愿后，如果图片生成还在排队，心愿先以无图/占位图展示，图片 ready 后通过 family event 或刷新展示。

移动端建议流程：

- 儿童端第一期只消费 `Wish.imageMedia.downloadUrl` 展示，不提供生成入口。
- 家长模式移动端后续可复用同样的候选查询和上传流程；上传链路直接参考现有 submission 上传。

### 4.6 兜底链路

推荐链路：

```text
输入 title/note
  -> 查询复用候选
  -> 家长确认复用：CreateWishRequest.imageMediaId = candidate.media.id
  -> 无合适候选：选择生成
      -> 创建 generation job
      -> 成功后生成 media_asset + profile
      -> 创建或更新 wish.image_media_id
  -> 生成失败/超时：提示上传或无图继续
  -> 选择上传：现有 upload-session/finalize，然后 create wish with imageMediaId
```

## 5. 图片生成接入方案

### 5.1 服务边界决策

推荐在 `ai-worker` 扩展图片生成 provider，但编排与状态仍由 `core-api` 持有。

理由：

- `ai-worker` 已有 provider 抽象和内部 HTTP token，适合承载模型供应商差异。
- `core-api` 拥有家庭权限、wish、media_asset、idempotency、事件发布，是任务状态和最终资产关系的正确边界。
- 生成出的文件仍进入 MinIO 和 `media_asset`，可继续由 `media-worker` 生成 thumbnail/preview/ai_ready。
- 新建单独 image-worker 会引入部署和运维复杂度，第一期价值不明显。

不建议让前端直接调供应商 API：API key 暴露风险高，也绕过家庭权限、审计、成本控制和内容安全。

### 5.2 Provider 接口

建议在 `ai_worker.models` 增加：

```python
@dataclass(frozen=True)
class WishImageGenerationRequest(JsonModel):
    request_id: str
    family_id: str
    child_age: int | None
    wish_title: str
    wish_note: str | None
    category: str | None
    style: Literal["warm_illustration", "storybook", "clean_product"] = "warm_illustration"
    aspect_ratio: Literal["1:1", "4:3"] = "1:1"
    negative_prompt: str | None = None

@dataclass(frozen=True)
class WishImageGenerationResponse(JsonModel):
    request_id: str
    provider: str
    model: str
    prompt: str
    content_type: str
    image_base64: str | None = None
    image_url: str | None = None
    latency_ms: int | None = None
    cost_units: float | None = None
```

`AiProvider` 增加：

```python
def generate_wish_image(self, request: WishImageGenerationRequest) -> WishImageGenerationResponse:
    raise NotImplementedError
```

`app.py` 增加内部路由：

```text
POST /internal/ai/generate-wish-image
Authorization: Bearer {WISHPOOL_AI_INTERNAL_TOKEN}
```

实现约束：

- provider 返回 base64 或临时 URL，`core-api`/生成编排组件下载后上传 MinIO。
- provider 不能直接写数据库。
- deterministic provider 返回可测试占位图或固定小图，用于本地和 CI。

### 5.3 Prompt 构造

Prompt 应由后端统一生成并审计保存，前端只提供 title/note/style。建议模板：

```text
为儿童家庭心愿卡生成一张温暖、清晰、适合 6-12 岁儿童的插画封面。
主题：{wish_title}
补充说明：{wish_note}
分类：{category}
画面要求：主体明确，占据画面中心；背景简洁；色彩明亮但不过度刺激；不要出现文字、水印、品牌 logo、真实儿童面孔、危险场景。
输出：{aspect_ratio}，适合裁切为心愿卡封面和碎片格。
```

负向 prompt：

```text
text, watermark, logo, photorealistic child face, scary, violent, unsafe, cluttered, blurry, low quality
```

如果 wish title 是具体商品名，建议把品牌名弱化为通用对象，如“显微镜玩具/儿童科学工具”，避免商标和购买导向风险。

### 5.4 任务编排

建议新增 core-api 持久任务表 `wish_image_generation_job`，由 core-api 提供 claim 模式，`ai-worker` 或轻量 orchestration worker 轮询执行。

表字段建议：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | uuid PK | generation request id |
| `family_id` | uuid | 权限与成本归属 |
| `child_id` | uuid | 目标孩子 |
| `wish_id` | uuid null | 可在创建 wish 前生成，也可创建后补图 |
| `title_snapshot` | text | 请求标题 |
| `note_snapshot` | text null | 请求说明 |
| `style` | text | 风格 |
| `status` | text | `queued/running/succeeded/failed_retryable/failed_final/cancelled` |
| `media_asset_id` | uuid null | 成功后生成的媒体 |
| `provider`/`model` | text null | 实际模型 |
| `prompt` | text null | 审计 |
| `attempt_count` | int | 重试次数 |
| `available_at`/`leased_until` | timestamptz | claim 租约 |
| `error_code`/`error_message` | text null | 失败诊断 |
| `created_by` | uuid | 家长 |
| `created_at`/`updated_at` | timestamptz | 审计 |

内部端点：

- `POST /internal/wish-image-generation/claim`
- `POST /internal/wish-image-generation/{jobId}/started`
- `POST /internal/wish-image-generation/{jobId}/completed`
- `POST /internal/wish-image-generation/{jobId}/failed`

外部端点：

- `POST /wishes/image-generations`：创建生成任务
- `GET /wishes/image-generations/{jobId}`：查询状态
- `POST /wishes/{wishId}/image`：将生成成功或上传完成的 media 绑定到 wish

为什么不直接复用 `ai_job`：现有 `ai_job` 的 `job_type` 枚举绑定 submission/content safety/memory，且 `submission_id` 是核心关联；图片生成需要 `media_asset_id`、prompt、lease 与产物落库，不宜强塞到当前结构。

Temporal 选择：

- 如果生成图片绑定“创建心愿”主流程，建议后续由 `workflow-worker` 通过 outbox event 启动 durable workflow。
- 第一阶段更建议采用 `media-worker` 类似 claim 模式，代码路径短、符合现有 V009 lease 模型，且图片生成只需要有限重试和状态查询。

### 5.5 生成后落库与 MinIO

推荐步骤：

1. core-api 创建 generation job，状态 `queued`。
2. worker 领取 job，调用 `ai-worker /internal/ai/generate-wish-image`。
3. worker 获得 image bytes 后上传到 MinIO，key 建议：

```text
families/{familyId}/children/{childId}/wish_image/generated/{mediaId}.png
```

4. core-api 在事务内插入 `media_asset`：
   - `purpose='wish_image'`
   - `status='uploaded'`
   - `related_type='wish'`，如果 `wish_id` 已存在则填 `related_id`
   - `created_by` 使用发起家长
5. 插入 `wish_image_asset_profile`。
6. 如果 `wish_id` 已存在，更新 `wish.image_media_id`。
7. 发布 `media.uploaded` / `wish.image_generation_succeeded` family event。
8. 让现有 `media-worker` 异步生成 derivatives，最终状态变 `ready`。

注意：当前 `MediaService.createUploadSession` 只适合客户端上传。服务端生成图片可以新增内部 media create/finalize 能力，或让 generation worker 通过 internal endpoint 创建服务端媒体记录，避免绕过审计。

### 5.6 供应商选型

以下价格和能力为 2026-08-27 设计期核对，供应商报价会变化，上线前必须复核。

| 供应商 | 国内网络/API 可用性 | 能力 | 成本量级 | 延迟估计 | 备注 |
| --- | --- | --- | --- | --- | --- |
| 阿里云百炼/通义万相 | 国内可用，企业接入成熟 | 文生图、图生图、编辑；官方文档建议纯文生图优先使用 wan2.6-t2i | 官方价格页显示部分图像输出约 0.10 元/张，具体模型按价格页确认 | 通常 5-20 秒，取决于排队和分辨率 | 推荐第一候选；API key 待产品提供 |
| 火山引擎即梦 AI | 国内可用，字节生态 | 即梦 4.0 支持文生图、图像编辑、多图组合 | 需按火山引擎具体模型计费页/商务报价确认 | 通常 5-20 秒 | 画面质量强；API key 待产品提供 |
| 硅基流动 SiliconFlow | 国内访问相对友好，OpenAI-compatible 风格 | 多开源图像模型，如 FLUX、Z-Image 等 | 官方 pricing 显示低价模型可到约 $0.005/张，FLUX 约 $0.03-$0.06/张 | 通常 3-15 秒 | 低成本候选；需验证内容安全和服务稳定性 |
| Stability AI | 国际服务，国内网络不确定 | Stable Image Core/Ultra 等 | 官方 credit 模式，1 credit=$0.01；不同服务按 credits 扣费 | 国内链路可能较不稳定 | 不作为国内生产首选，可作为海外/备用 provider |
| 自建 SD/FLUX | 完全可控 | 可私有化、可调模型 | 有 GPU 固定成本，低量不划算 | 取决于 GPU | 目前不建议，除非图片量很大或有合规私有化要求 |

参考链接：

- 阿里云模型价格：https://help.aliyun.com/zh/model-studio/model-pricing
- 阿里云通义万相 API：https://help.aliyun.com/zh/model-studio/wan-image-generation-api-reference
- 阿里云 Qwen-Image API：https://help.aliyun.com/zh/model-studio/qwen-image-api
- 火山引擎即梦图片生成 4.0：https://www.volcengine.com/docs/85621/1817045
- 火山引擎即梦产品页：https://www.volcengine.com/product/jimeng
- SiliconFlow pricing：https://www.siliconflow.com/pricing
- Stability AI pricing：https://platform.stability.ai/pricing

免费/低价档判断：

- MVP 阶段如果每个家庭每周 1-3 张心愿图，按 0.10 元/张或 $0.005-$0.06/张估算，直接 API 成本可控。
- 免费额度适合开发和小流量试点，不适合作为稳定生产预算依据。
- 需要产品提供 API key、预算上限、单家庭/月生成次数上限。

### 5.7 失败降级、幂等与重试

失败降级：

- 生成 20-30 秒内未完成：前端显示“生成中”，允许先创建/激活无图心愿。
- 失败可重试：保留 job，提供“重新生成”。
- 失败最终态：引导上传或继续无图。
- 供应商内容安全拒绝：展示通用失败原因，不回显敏感 prompt 细节。

幂等：

- `POST /wishes/image-generations` 支持 `Idempotency-Key`。
- 同一 `familyId + childId + normalizedTitle + style` 在短时间窗口内可复用进行中的 job，避免重复扣费。
- `completed` 回调以 jobId 幂等更新；若 media 已存在，直接返回成功。

重试：

- 网络/5xx/超时：指数退避，最多 3 次。
- 4xx、内容策略拒绝、余额不足：`failed_final`。
- 生成结果下载失败：可重试。

## 6. 碎片拆分设计

### 6.1 方案对比

| 方案 | 描述 | 儿童端视觉体验 | 成本 | 复杂度 | 与现有进度衔接 | 风险 |
| --- | --- | --- | --- | --- | --- | --- |
| A 前端 CSS 网格/遮罩切割整图 | 不生成碎片文件；前端用同一张图按格子裁切，未获得部分加暗/模糊/锁定 | 中等；能看到拼图逐步点亮，但边缘不是真实碎片 | 0 | 低 | 极好，直接用 `earned/required` | 不同端需统一渲染规则 |
| B media-worker 用 Pillow 切 N 张碎片图 | 将心愿图切成 N 张小图，存 MinIO 或 derivatives | 中高；真实图片块，可缓存 | 低，仅 CPU/存储 | 中 | 好，但需要建立块序号和 derivative metadata | requiredFragments 可变，N 太多会造成对象膨胀 |
| C AI 生成带拼图分隔线底图 | 生成整图时要求带 3x3/4x3 拼图区分线，前端按进度显示块 | 中高；美术一致，但不一定精确 | 一张图生成成本 | 中 | 好，仍按数值渲染 | AI 可能画错格线、文字、水印；对 requiredFragments 不灵活 |
| D 每块碎片独立 AI 生成 | 每个碎片单独生成小图或奖励图 | 最高但不稳定 | 高，N 倍生成成本 | 高 | 差，需要每块生命周期 | 成本、延迟、风格一致性、审核风险都高 |

### 6.2 推荐方案

推荐第一期采用 A+少量 B 扩展：

1. 权威数据仍保持 `wish.required_fragments` / `wish.earned_fragments`。
2. 前端用整张 `imageMedia.preview/downloadUrl` 渲染固定网格，每个格子的 background-position 对应整图区域。
3. 未获得格子显示低亮度、磨砂、星形锁或半透明遮罩；已获得格子显示原图并加轻微亮边。
4. requiredFragments 大于 12 时，视觉格固定压缩到 12 或 16 格，显示映射进度；实际数值仍显示 `earned/required`。
5. 可选第二阶段让 `media-worker` 为 `wish_image` 生成额外 derivative：`fragment_preview`，metadata 包含 `gridRows/gridCols/cellCount`，用于统一端上布局，不存每块图。

不推荐 D。儿童端的主要反馈来自“逐格揭开目标图”和任务完成动效，不需要 N 次 AI 生成。D 会把一个心愿从 1 次生成变成 9/12/16 次生成，成本和失败面都线性放大，且小图风格不一致会破坏拼图整体感。

### 6.3 数据结构建议

第一期不新增独立 `wish_fragment` 实体。

保留：

- `wish.required_fragments`
- `wish.earned_fragments`
- `reward_ledger.reward_type='wish_fragment'`
- `reward_ledger.wish_id`

新增可选展示配置字段：

| 位置 | 字段 | 说明 |
| --- | --- | --- |
| `wish` | `fragment_visual_mode` text default `grid_reveal` | 渲染模式：`grid_reveal/star_grid/puzzle_lines` |
| `wish` | `fragment_grid_rows` int null | 固定行数，默认由 requiredFragments 推导 |
| `wish` | `fragment_grid_cols` int null | 固定列数，默认由 requiredFragments 推导 |

也可以不改 `wish` 表，把展示配置放到 `wish_image_asset_profile.fragment_layout_json`。若碎片布局只和图片有关，放 profile 更合理；若同一图片在不同 wish 上可使用不同 requiredFragments，则放 wish 更合理。

建议第一期不落布局字段，由前端使用确定性规则：

| requiredFragments | 视觉格数 |
| --- | --- |
| 1-4 | 2x2 |
| 5-6 | 3x2 |
| 7-9 | 3x3 |
| 10-12 | 4x3 |
| 13-16 | 4x4 |
| >16 | 4x4，按比例点亮 |

如果后续要“每块获得时间、来源任务、动画回放”，再新增 `wish_fragment_event` 或查询 `reward_ledger` 即可，不必现在引入 `wish_fragment` 主表。

### 6.4 碎片生成时机

推荐：

- 心愿创建/绑定图片后，不预生成每块碎片文件。
- 前端实时按 `earnedFragments/requiredFragments` 渲染。
- `media-worker` 只负责标准图片 derivatives；若增加 `fragment_preview`，在图片 ready 处理时生成一次。

原因：

- requiredFragments 可能调整或复用同一图片到不同心愿，预生成 N 张碎片会造成失配。
- 前端裁切对网络更友好，只加载一张图。
- 移动端离线/弱网可继续使用占位图和数值点亮。

### 6.5 API 与前端展示建议

API 响应建议扩展 `Wish`：

```json
{
  "imageMedia": { "...": "MediaAsset" },
  "imageGeneration": {
    "jobId": "uuid",
    "status": "queued|running|succeeded|failed_retryable|failed_final"
  },
  "fragmentVisual": {
    "mode": "grid_reveal",
    "rows": 3,
    "cols": 4,
    "revealed": 5,
    "total": 12
  }
}
```

也可以不返回 `fragmentVisual`，由客户端计算。但考虑 Flutter 和 Next.js 要保持一致，建议由 core-api 返回计算结果或提供共享规则文档。

家长端：

- 创建表单加入图片候选条、生成按钮、上传按钮。
- 心愿卡库展示历史封面，点击复用时带入 `imageMediaId`。
- 历史心愿详情中显示当前封面和兑换照片，避免把“心愿图”和“兑现留存图”混淆。

学生端：

- `_WishHeroCard` 顶部区域改为图片揭示网格。
- 没有图片时使用当前渐变/图标占位，但仍按格子点亮。
- 每获得一个碎片后，当前格子播放点亮动画；事件来自已有 family sync 刷新即可。
- 不显示生成中细节，只显示稳定的心愿卡状态。

## 7. DB Migration 清单

以下仅为后续实现清单，本次设计不创建 migration 文件。当前最新为 `V009__media_processing_lease.sql`，后续从 `V010` 开始。

| Migration | 内容 |
| --- | --- |
| `V010__wish_image_asset_profile.sql` | 新增 `wish_image_asset_profile`；索引 family/child/time、normalized_title、keywords GIN、tags GIN；可选 `create extension if not exists pg_trgm` |
| `V011__wish_image_generation_job.sql` | 新增 `wish_image_generation_job`；状态枚举 check；lease 字段；`idx_wish_image_generation_claim` |
| `V012__wish_image_generation_media_links.sql` | 若 V011 未含完整 FK，则补 `media_asset_id`、`wish_id` FK；增加唯一约束避免同 job 多 media |
| `V013__wish_fragment_visual_config.sql` | 可选，新增 wish 视觉配置字段；若第一期前端规则足够，此 migration 延后 |

不建议修改 `media_asset_purpose_check`，因为 `wish_image` 已在 V007 加入。

## 8. OpenAPI 契约变更清单

实际文件：`packages/api-contracts/openapi/wishpool.yaml`

新增路径：

| 路径 | 方法 | 说明 |
| --- | --- | --- |
| `/wishes/image-candidates` | POST | 基于 title/note 查询可复用图片 |
| `/wishes/image-generations` | POST | 创建心愿图生成任务 |
| `/wishes/image-generations/{jobId}` | GET | 查询生成任务 |
| `/wishes/{wishId}/image` | POST/PUT | 绑定或替换心愿图片 |
| `/internal/wish-image-generation/claim` | POST | worker 领取生成任务 |
| `/internal/wish-image-generation/{jobId}/completed` | POST | worker 完成 |
| `/internal/wish-image-generation/{jobId}/failed` | POST | worker 失败 |

新增/修改 schemas：

- `WishImageCandidateRequest`
- `WishImageCandidateResponse`
- `WishImageCandidate`
- `CreateWishImageGenerationRequest`
- `WishImageGenerationJob`
- `BindWishImageRequest`
- `WishImageProfile`
- `WishFragmentVisual`
- `Wish.imageGeneration?`
- `Wish.fragmentVisual?`
- `MediaAsset` 可选增加 `derivatives?` 或保持现状，通过已有 media endpoints 获取。

同步点：

- 修改 YAML 后运行项目既有 OpenAPI 生成流程，更新 TypeScript/Kotlin/Dart 客户端。
- 移动端 `wishpool_api` 生成包需要同步，否则新增字段和端点不可用。

## 9. 分模块落地计划

| 步骤 | 模块 | 内容 | 依赖 | 工作量 |
| --- | --- | --- | --- | --- |
| 1 | core-api/db | 增加 `wish_image_asset_profile` 与 repository/service 查询逻辑 | 无 | M |
| 2 | core-api/openapi | 增加 `/wishes/image-candidates` 与 DTO，更新 YAML | 步骤 1 | S |
| 3 | parent-web | 心愿创建表单接入候选查询、确认复用、无图兜底 | 步骤 2 | M |
| 4 | parent-web/mobile | 支持上传 `wish_image`，复用现有 upload-session/finalize 流程 | 步骤 2 | M |
| 5 | core-api/db | 增加 `wish_image_generation_job`、外部创建/查询、内部 claim/completed/failed | 步骤 1 | L |
| 6 | ai-worker | 增加 `generate_wish_image` provider 接口、deterministic provider、一个真实供应商 adapter | 步骤 5；产品提供 API key | L |
| 7 | generation worker | 可放 ai-worker 进程内轮询，或单独轻量 worker；负责调用 provider、上传 MinIO、回写 core-api | 步骤 5/6 | L |
| 8 | media-worker | 可选增加 `wish_image` 的 `fragment_preview` derivative；第一期可不做 | 步骤 4 | S/M |
| 9 | mobile | `WishPoolSnapshot` 增加 image URL；`wish_screen.dart` 改为图片揭示网格 | 步骤 2/4 | M |
| 10 | admin-web | 展示生成队列、失败原因、供应商配置健康度 | 步骤 5/6 | M |
| 11 | workflow-worker | 若要 durable orchestration，新增 outbox 路由和 Temporal workflow | 步骤 5 后可选 | M/L |

推荐交付顺序：

1. 先做复用和上传，不接真实 AI，立刻提升用户体验。
2. 再做生成 job 和 deterministic provider，把端到端状态打通。
3. 最后接真实供应商，并在 admin-web 加预算和失败可观测性。

## 10. 决策记录

| 决策 | 结论 | 理由 |
| --- | --- | --- |
| 图片复用是否自动绑定 | 不自动绑定，必须用户确认 | 家长创建心愿是强意图操作，自动复用可能选错图；确认成本低 |
| “同类”是否使用向量检索 | 第一期不用，先规则评分 | 当前数据量小，规则可解释；后续可在 profile 上加 embedding |
| 图片生成放哪里 | 扩展 ai-worker provider，core-api 持有任务状态 | 延续现有 provider 风格，避免 key 暴露和状态分散 |
| 生成任务编排 | 第一期 claim 模式，后续可 Temporal | media-worker 已验证 claim/lease；生成任务复杂度可控 |
| 碎片是否建独立实体 | 第一期不建 | 当前 reward 已用数值和 ledger；实体会扩大业务复杂度 |
| 碎片是否 AI 逐块生成 | 不推荐 | 成本 N 倍、失败面 N 倍、整体图一致性差 |
| 是否生成拼图底图 | 可选，不作为主方案 | AI 格线不稳定；前端确定性网格更可控 |

## 11. 风险与待确认

| 风险/问题 | 影响 | 建议 |
| --- | --- | --- |
| 供应商价格和 SLA 变化 | 成本和体验不稳定 | 上线前按 2026-08-27 之后的最新价格复核，配置预算上限 |
| 图片生成内容安全 | 儿童产品风险高 | provider 前后都做安全策略，禁止真实儿童面孔、危险内容、品牌 logo |
| 复用错图 | 家长困惑或误创建 | 只推荐不自动绑定；显示来源和理由 |
| `media_asset` 语义不足 | 复用质量差 | 新增 profile 表 |
| 移动端 generated client 滞后 | 编译或运行缺字段 | OpenAPI 变更后必须更新 Dart client |
| 图片 derivatives 未 ready | 前端短时间无 preview | 可使用原图 downloadUrl 或占位；media-worker 完成后刷新 |
| requiredFragments 大于视觉格 | 儿童感觉少了碎片 | 明确“视觉格映射进度”，显示数值；必要时上限 16 格 |

## 12. 原型验证建议

本次不创建原型文件。后续若要低风险验证，建议路径：

- `docs/design/prototypes/wish-fragment-grid.html`

原型只用静态 HTML/CSS/JS，输入 `imageUrl/current/required`，验证 2x2、3x3、4x3、4x4 在移动端宽度下的视觉效果。不要改 `apps/` 或 `services/`。

## 13. 复核清单

本设计实际阅读/检索过的关键文件：

- `services/core-api/src/main/kotlin/com/wishpool/core/wishes/WishController.kt`
- `services/core-api/src/main/kotlin/com/wishpool/core/wishes/WishService.kt`
- `services/core-api/src/main/kotlin/com/wishpool/core/wishes/WishDtos.kt`
- `services/core-api/src/main/kotlin/com/wishpool/core/wishes/WishMappers.kt`
- `services/core-api/src/main/kotlin/com/wishpool/core/media/MediaController.kt`
- `services/core-api/src/main/kotlin/com/wishpool/core/media/MediaService.kt`
- `services/core-api/src/main/kotlin/com/wishpool/core/media/MediaDtos.kt`
- `services/core-api/src/main/kotlin/com/wishpool/core/media/MediaMappers.kt`
- `services/ai-worker/ai_worker/app.py`
- `services/ai-worker/ai_worker/provider.py`
- `services/ai-worker/ai_worker/models.py`
- `services/ai-worker/ai_worker/config.py`
- `services/media-worker/media_worker/processor.py`
- `services/media-worker/media_worker/core_api.py`
- `services/media-worker/media_worker/storage.py`
- `services/media-worker/media_worker/worker.py`
- `services/media-worker/media_worker/config.py`
- `services/media-worker/media_worker/__main__.py`
- `db/migrations/V001__initial_schema.sql`
- `db/migrations/V005__media_asset_related_resource.sql`
- `db/migrations/V007__media_wish_image_purpose.sql`
- `db/migrations/V009__media_processing_lease.sql`
- `packages/api-contracts/openapi/wishpool.yaml`
- `openapitools.json`
- `apps/parent-web/app/actions.ts`
- `apps/parent-web/app/wish/page.tsx`
- `apps/parent-web/lib/dashboard-data.ts`
- `apps/mobile/lib/src/features/wish_screen.dart`
- `apps/mobile/lib/src/data/wishpool_repository.dart`
- `apps/mobile/lib/src/domain/wishpool_snapshot.dart`
- `apps/mobile/MODULE.md`
- `apps/parent-web/app/MODULE.md`
- `services/MODULE.md`
- `packages/api-contracts/MODULE.md`

外部资料复核链接：

- 阿里云模型价格：https://help.aliyun.com/zh/model-studio/model-pricing
- 阿里云通义万相 API：https://help.aliyun.com/zh/model-studio/wan-image-generation-api-reference
- 阿里云 Qwen-Image API：https://help.aliyun.com/zh/model-studio/qwen-image-api
- 火山引擎即梦图片生成 4.0：https://www.volcengine.com/docs/85621/1817045
- 火山引擎即梦产品页：https://www.volcengine.com/product/jimeng
- SiliconFlow pricing：https://www.siliconflow.com/pricing
- Stability AI pricing：https://platform.stability.ai/pricing
