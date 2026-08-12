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
