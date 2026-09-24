from dataclasses import asdict, dataclass, field
from typing import Any, Literal


SubmissionMediaKind = Literal["image", "audio", "video", "text"]
RiskLevel = Literal["low", "medium", "high"]


class ValidationError(ValueError):
    pass


def _required_string(data: dict[str, Any], key: str) -> str:
    value = data.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ValidationError(f"{key} is required")
    return value


def _string_list(data: dict[str, Any], key: str) -> list[str]:
    value = data.get(key, [])
    if value is None:
        return []
    if not isinstance(value, list) or not all(isinstance(item, str) for item in value):
        raise ValidationError(f"{key} must be a string array")
    return value


def _optional_string(data: dict[str, Any], key: str) -> str | None:
    value = data.get(key)
    if value is None:
        return None
    if not isinstance(value, str):
        raise ValidationError(f"{key} must be a string")
    return value


@dataclass(frozen=True)
class JsonModel:
    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


@dataclass(frozen=True)
class MediaSignal(JsonModel):
    media_id: str
    kind: SubmissionMediaKind
    mime_type: str
    transcript: str | None = None
    visual_labels: list[str] = field(default_factory=list)
    duration_seconds: float | None = None

    @staticmethod
    def from_dict(data: dict[str, Any]) -> "MediaSignal":
        kind = _required_string(data, "kind")
        if kind not in {"image", "audio", "video", "text"}:
            raise ValidationError("kind is invalid")
        duration = data.get("duration_seconds")
        if duration is not None and (not isinstance(duration, int | float) or duration < 0):
            raise ValidationError("duration_seconds must be positive")
        return MediaSignal(
            media_id=_required_string(data, "media_id"),
            kind=kind,  # type: ignore[arg-type]
            mime_type=_required_string(data, "mime_type"),
            transcript=_optional_string(data, "transcript"),
            visual_labels=_string_list(data, "visual_labels"),
            duration_seconds=float(duration) if duration is not None else None,
        )


@dataclass(frozen=True)
class AiPrecheckRequest(JsonModel):
    submission_id: str
    task_title: str
    task_category: str
    child_age: int | None = None
    media: list[MediaSignal] = field(default_factory=list)
    child_note: str | None = None

    @staticmethod
    def from_dict(data: dict[str, Any]) -> "AiPrecheckRequest":
        age = data.get("child_age")
        if age is not None and (not isinstance(age, int) or age < 0 or age > 18):
            raise ValidationError("child_age must be between 0 and 18")
        media = data.get("media", [])
        if not isinstance(media, list):
            raise ValidationError("media must be an array")
        return AiPrecheckRequest(
            submission_id=_required_string(data, "submission_id"),
            task_title=_required_string(data, "task_title"),
            task_category=_required_string(data, "task_category"),
            child_age=age,
            media=[MediaSignal.from_dict(item) for item in media],
            child_note=_optional_string(data, "child_note"),
        )


@dataclass(frozen=True)
class AiPrecheckResponse(JsonModel):
    submission_id: str
    summary: str
    risk_level: RiskLevel
    confidence: float
    suggested_decision: Literal["approve", "needs_revision", "manual_review"]
    checklist: list[str]
    safety_notes: list[str]


@dataclass(frozen=True)
class FeedbackDraftRequest(JsonModel):
    child_name: str
    task_title: str
    decision: Literal["approved", "needs_revision"]
    evidence_summary: str
    parent_tone: Literal["warm", "encouraging", "concise"] = "warm"

    @staticmethod
    def from_dict(data: dict[str, Any]) -> "FeedbackDraftRequest":
        decision = _required_string(data, "decision")
        if decision not in {"approved", "needs_revision"}:
            raise ValidationError("decision is invalid")
        tone = data.get("parent_tone", "warm")
        if tone not in {"warm", "encouraging", "concise"}:
            raise ValidationError("parent_tone is invalid")
        return FeedbackDraftRequest(
            child_name=_required_string(data, "child_name"),
            task_title=_required_string(data, "task_title"),
            decision=decision,  # type: ignore[arg-type]
            evidence_summary=_required_string(data, "evidence_summary"),
            parent_tone=tone,  # type: ignore[arg-type]
        )


@dataclass(frozen=True)
class FeedbackDraftResponse(JsonModel):
    title: str
    message: str
    stickers: list[str]


@dataclass(frozen=True)
class MemoryNarrativeRequest(JsonModel):
    child_name: str
    week_start_date: str
    approved_task_titles: list[str] = field(default_factory=list)
    parent_notes: list[str] = field(default_factory=list)
    wish_title: str | None = None

    @staticmethod
    def from_dict(data: dict[str, Any]) -> "MemoryNarrativeRequest":
        return MemoryNarrativeRequest(
            child_name=_required_string(data, "child_name"),
            week_start_date=_required_string(data, "week_start_date"),
            approved_task_titles=_string_list(data, "approved_task_titles"),
            parent_notes=_string_list(data, "parent_notes"),
            wish_title=_optional_string(data, "wish_title"),
        )


@dataclass(frozen=True)
class MemoryNarrativeResponse(JsonModel):
    title: str
    summary: str
    highlights: list[str]
    cover_prompt: str


@dataclass(frozen=True)
class PrivacySummaryRequest(JsonModel):
    request_id: str
    request_type: Literal["export", "delete"]
    family_id: str
    child_ids: list[str] = field(default_factory=list)
    requested_scopes: list[str] = field(default_factory=list)

    @staticmethod
    def from_dict(data: dict[str, Any]) -> "PrivacySummaryRequest":
        request_type = _required_string(data, "request_type")
        if request_type not in {"export", "delete"}:
            raise ValidationError("request_type is invalid")
        return PrivacySummaryRequest(
            request_id=_required_string(data, "request_id"),
            request_type=request_type,  # type: ignore[arg-type]
            family_id=_required_string(data, "family_id"),
            child_ids=_string_list(data, "child_ids"),
            requested_scopes=_string_list(data, "requested_scopes"),
        )


@dataclass(frozen=True)
class PrivacySummaryResponse(JsonModel):
    request_id: str
    operator_summary: str
    verification_steps: list[str]
    risk_notes: list[str]


@dataclass(frozen=True)
class ImageProviderConfig(JsonModel):
    code: str
    provider_type: Literal[
        "volcengine_ark",
        "aliyun_bailian",
        "siliconflow",
        "deterministic",
        "custom_openai_compatible",
    ]
    base_url: str
    api_key: str | None
    model_name: str
    extra_params: dict[str, Any] = field(default_factory=dict)

    @staticmethod
    def from_dict(data: dict[str, Any]) -> "ImageProviderConfig":
        provider_type = _required_string(data, "provider_type")
        if provider_type not in {"volcengine_ark", "aliyun_bailian", "siliconflow", "deterministic", "custom_openai_compatible"}:
            raise ValidationError("provider_type is invalid")
        extra_params = data.get("extra_params", {})
        if extra_params is None:
            extra_params = {}
        if not isinstance(extra_params, dict):
            raise ValidationError("extra_params must be an object")
        return ImageProviderConfig(
            code=_required_string(data, "code"),
            provider_type=provider_type,  # type: ignore[arg-type]
            base_url=_required_string(data, "base_url"),
            api_key=_optional_string(data, "api_key"),
            model_name=_required_string(data, "model_name"),
            extra_params=extra_params,
        )


@dataclass(frozen=True)
class WishImageGenerationRequest(JsonModel):
    request_id: str
    family_id: str
    child_age: int | None
    wish_title: str
    wish_note: str | None = None
    category: str | None = None
    style: Literal["warm_illustration", "storybook", "clean_product"] = "warm_illustration"
    aspect_ratio: Literal["1:1", "4:3"] = "1:1"
    negative_prompt: str | None = None
    provider_code: str | None = None
    provider_config: ImageProviderConfig | None = None

    @staticmethod
    def from_dict(data: dict[str, Any]) -> "WishImageGenerationRequest":
        age = data.get("child_age")
        if age is not None and (not isinstance(age, int) or age < 0 or age > 18):
            raise ValidationError("child_age must be between 0 and 18")
        style = data.get("style", "warm_illustration")
        if style not in {"warm_illustration", "storybook", "clean_product"}:
            raise ValidationError("style is invalid")
        aspect_ratio = data.get("aspect_ratio", "1:1")
        if aspect_ratio not in {"1:1", "4:3"}:
            raise ValidationError("aspect_ratio is invalid")
        provider_config = data.get("provider_config")
        if provider_config is not None and not isinstance(provider_config, dict):
            raise ValidationError("provider_config must be an object")
        return WishImageGenerationRequest(
            request_id=_required_string(data, "request_id"),
            family_id=_required_string(data, "family_id"),
            child_age=age,
            wish_title=_required_string(data, "wish_title"),
            wish_note=_optional_string(data, "wish_note"),
            category=_optional_string(data, "category"),
            style=style,  # type: ignore[arg-type]
            aspect_ratio=aspect_ratio,  # type: ignore[arg-type]
            negative_prompt=_optional_string(data, "negative_prompt"),
            provider_code=_optional_string(data, "provider_code"),
            provider_config=ImageProviderConfig.from_dict(provider_config) if provider_config is not None else None,
        )


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
