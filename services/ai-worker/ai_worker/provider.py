from ai_worker.models import (
    AiPrecheckRequest,
    AiPrecheckResponse,
    FeedbackDraftRequest,
    FeedbackDraftResponse,
    MemoryNarrativeRequest,
    MemoryNarrativeResponse,
    PrivacySummaryRequest,
    PrivacySummaryResponse,
)


class AiProvider:
    def precheck_submission(self, request: AiPrecheckRequest) -> AiPrecheckResponse:
        raise NotImplementedError

    def draft_feedback(self, request: FeedbackDraftRequest) -> FeedbackDraftResponse:
        raise NotImplementedError

    def generate_memory_narrative(self, request: MemoryNarrativeRequest) -> MemoryNarrativeResponse:
        raise NotImplementedError

    def summarize_privacy_request(self, request: PrivacySummaryRequest) -> PrivacySummaryResponse:
        raise NotImplementedError


class DeterministicAiProvider(AiProvider):
    def precheck_submission(self, request: AiPrecheckRequest) -> AiPrecheckResponse:
        media_kinds = ", ".join(sorted({item.kind for item in request.media})) or "无媒体"
        evidence = request.child_note or "未填写儿童备注"
        risky_terms = {"危险", "受伤", "陌生人", "隐私", "地址", "电话"}
        joined_text = " ".join(
            [evidence, *[item.transcript or "" for item in request.media], *[" ".join(item.visual_labels) for item in request.media]]
        )
        has_risk = any(term in joined_text for term in risky_terms)

        return AiPrecheckResponse(
            submission_id=request.submission_id,
            summary=f"{request.task_title} 提交包含 {media_kinds}，备注线索：{evidence[:80]}。",
            risk_level="medium" if has_risk else "low",
            confidence=0.72 if request.media else 0.54,
            suggested_decision="manual_review" if has_risk else "approve",
            checklist=[
                "任务标题与提交内容已完成基础匹配",
                "媒体数量和类型已记录",
                "需要家长确认时长、清晰度和真实完成情况",
            ],
            safety_notes=["发现潜在风险词，请家长仔细查看"] if has_risk else [],
        )

    def draft_feedback(self, request: FeedbackDraftRequest) -> FeedbackDraftResponse:
        if request.decision == "approved":
            message = f"{request.child_name}，你完成了「{request.task_title}」，我看到了你的认真和坚持。继续把这份节奏保持下去。"
            title = "今天的星光已点亮"
            stickers = ["spark", "heart", "star"]
        else:
            message = f"{request.child_name}，「{request.task_title}」还可以再补充一点点。{request.evidence_summary}，我们一起把它做得更完整。"
            title = "再试一次会更好"
            stickers = ["retry", "hug", "seed"]

        if request.parent_tone == "concise":
            message = message.split("。")[0] + "。"

        return FeedbackDraftResponse(title=title, message=message, stickers=stickers)

    def generate_memory_narrative(self, request: MemoryNarrativeRequest) -> MemoryNarrativeResponse:
        tasks = request.approved_task_titles[:5] or ["一次认真完成的家庭任务"]
        highlights = [f"完成「{title}」" for title in tasks[:3]]
        if request.wish_title:
            highlights.append(f"向心愿「{request.wish_title}」又靠近了一步")

        return MemoryNarrativeResponse(
            title=f"{request.child_name} 的成长闪光周",
            summary=f"从 {request.week_start_date} 开始的这一周，{request.child_name} 完成了 {len(tasks)} 项值得记录的任务。"
            "这些小行动串起来，变成了能被家人看见的成长证据。",
            highlights=highlights,
            cover_prompt=f"warm family growth scrapbook cover, child achievements, task icons, wish theme {request.wish_title or 'starlight'}",
        )

    def summarize_privacy_request(self, request: PrivacySummaryRequest) -> PrivacySummaryResponse:
        scopes = ", ".join(request.requested_scopes) or "全部家庭数据"
        action = "导出" if request.request_type == "export" else "删除"

        return PrivacySummaryResponse(
            request_id=request.request_id,
            operator_summary=f"家庭 {request.family_id} 请求{action}范围：{scopes}，涉及儿童档案 {len(request.child_ids)} 个。",
            verification_steps=[
                "确认请求人具备家长或管理员权限",
                "锁定相关家庭数据写入窗口",
                "执行数据对象与数据库记录一致性检查",
                "完成后记录审计事件并通知请求人",
            ],
            risk_notes=["删除请求不可逆，需要二次确认"] if request.request_type == "delete" else [],
        )


def create_provider(name: str) -> AiProvider:
    normalized = name.strip().lower()
    if normalized == "deterministic":
        return DeterministicAiProvider()
    raise ValueError(f"Unsupported AI provider: {name}")
