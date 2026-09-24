from __future__ import annotations

import base64
import json
import time
from dataclasses import dataclass
from typing import Any
from urllib import error, request as urlrequest

from ai_worker.config import AiWorkerConfig
from ai_worker.models import (
    AiPrecheckRequest,
    AiPrecheckResponse,
    FeedbackDraftRequest,
    FeedbackDraftResponse,
    ImageProviderConfig,
    MemoryNarrativeRequest,
    MemoryNarrativeResponse,
    PrivacySummaryRequest,
    PrivacySummaryResponse,
    WishImageGenerationRequest,
    WishImageGenerationResponse,
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

    def generate_wish_image(self, request: WishImageGenerationRequest) -> WishImageGenerationResponse:
        provider = create_image_provider(request.provider_config)
        return provider.generate_wish_image(request)


class DeterministicAiProvider(AiProvider):
    def precheck_submission(self, request: AiPrecheckRequest) -> AiPrecheckResponse:
        media_kinds = ", ".join(sorted({item.kind for item in request.media})) or "none"
        evidence = request.child_note or "no child note"
        risky_terms = {"danger", "injury", "stranger", "privacy", "address", "phone", "危险", "受伤", "陌生人", "隐私"}
        joined_text = " ".join(
            [evidence, *[item.transcript or "" for item in request.media], *[" ".join(item.visual_labels) for item in request.media]]
        )
        has_risk = any(term in joined_text.lower() for term in risky_terms)

        return AiPrecheckResponse(
            submission_id=request.submission_id,
            summary=f"{request.task_title} submission includes {media_kinds}; evidence: {evidence[:80]}.",
            risk_level="medium" if has_risk else "low",
            confidence=0.72 if request.media else 0.54,
            suggested_decision="manual_review" if has_risk else "approve",
            checklist=[
                "Task title and submitted evidence have a basic match.",
                "Media count and types were recorded.",
                "Parent should confirm duration, clarity, and completion quality when needed.",
            ],
            safety_notes=["Potential safety or privacy terms were detected."] if has_risk else [],
        )

    def draft_feedback(self, request: FeedbackDraftRequest) -> FeedbackDraftResponse:
        if request.decision == "approved":
            message = f"{request.child_name}, you finished {request.task_title}. I can see your focus and persistence."
            title = "Today is lit up"
            stickers = ["spark", "heart", "star"]
        else:
            message = f"{request.child_name}, {request.task_title} can use a little more detail. {request.evidence_summary}"
            title = "再试一次会更好"
            stickers = ["retry", "hug", "seed"]

        if request.parent_tone == "concise":
            message = message.split(".")[0] + "."

        return FeedbackDraftResponse(title=title, message=message, stickers=stickers)

    def generate_memory_narrative(self, request: MemoryNarrativeRequest) -> MemoryNarrativeResponse:
        tasks = request.approved_task_titles[:5] or ["a completed family task"]
        highlights = [f"Completed {title}" for title in tasks[:3]]
        if request.wish_title:
            highlights.append(f"Moved closer to the wish: {request.wish_title}")

        return MemoryNarrativeResponse(
            title=f"{request.child_name} 的成长周",
            summary=f"Starting {request.week_start_date}, {request.child_name} completed {len(tasks)} meaningful task(s).",
            highlights=highlights,
            cover_prompt=f"warm family growth scrapbook cover, child achievements, task icons, wish theme {request.wish_title or 'starlight'}",
        )

    def summarize_privacy_request(self, request: PrivacySummaryRequest) -> PrivacySummaryResponse:
        scopes = ", ".join(request.requested_scopes) or "all family data"
        action = "export" if request.request_type == "export" else "delete"

        return PrivacySummaryResponse(
            request_id=request.request_id,
            operator_summary=f"Family {request.family_id} requested {action} for {scopes}; child profiles: {len(request.child_ids)}.",
            verification_steps=[
                "Confirm requester has parent or administrator permission.",
                "Freeze relevant family data writes during the operation window.",
                "Check data objects against database records.",
                "Record an audit event and notify the requester after completion.",
            ],
            risk_notes=["Delete requests are irreversible and require second confirmation."] if request.request_type == "delete" else [],
        )


@dataclass(frozen=True)
class ImageProvider:
    config: ImageProviderConfig
    timeout_seconds: float = 120.0

    def generate_wish_image(self, request: WishImageGenerationRequest) -> WishImageGenerationResponse:
        raise NotImplementedError

    def prompt_for(self, request: WishImageGenerationRequest) -> str:
        note = f"\nAdditional note: {request.wish_note}" if request.wish_note else ""
        category = f"\nCategory: {request.category}" if request.category else ""
        age = f"\nChild age: {request.child_age}" if request.child_age is not None else ""
        return (
            "Create a warm, clear illustration for a child family wish card. "
            "The main subject should be centered and easy to crop for a card cover and progress grid. "
            "Use bright but gentle colors, simple background, no text, no watermark, no brand logo, "
            "no realistic child face, no scary or unsafe scene.\n"
            f"Wish: {request.wish_title}{note}{category}{age}\n"
            f"Style: {request.style}. Aspect ratio: {request.aspect_ratio}."
        )

    def negative_prompt_for(self, request: WishImageGenerationRequest) -> str:
        return request.negative_prompt or "text, watermark, logo, photorealistic child face, scary, violent, unsafe, cluttered, blurry, low quality"

    def size_for(self, request: WishImageGenerationRequest) -> str:
        configured = self.config.extra_params.get("size")
        if isinstance(configured, str) and configured.strip():
            return configured.strip()
        if request.aspect_ratio == "4:3":
            return "1024x768"
        return "1024x1024"

    def post_json(self, url: str, payload: dict[str, Any], headers: dict[str, str]) -> dict[str, Any]:
        encoded = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        req = urlrequest.Request(url, data=encoded, method="POST")
        req.add_header("Content-Type", "application/json")
        for key, value in headers.items():
            req.add_header(key, value)
        try:
            with urlrequest.urlopen(req, timeout=self.timeout_seconds) as response:
                return json.loads(response.read().decode("utf-8"))
        except error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"image provider request failed with HTTP {exc.code}: {body[:300]}") from exc

    def get_json(self, url: str, headers: dict[str, str]) -> dict[str, Any]:
        req = urlrequest.Request(url, method="GET")
        for key, value in headers.items():
            req.add_header(key, value)
        try:
            with urlrequest.urlopen(req, timeout=self.timeout_seconds) as response:
                return json.loads(response.read().decode("utf-8"))
        except error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"image provider request failed with HTTP {exc.code}: {body[:300]}") from exc


class DeterministicImageProvider(ImageProvider):
    def generate_wish_image(self, request: WishImageGenerationRequest) -> WishImageGenerationResponse:
        started = time.monotonic()
        prompt = self.prompt_for(request)
        svg = (
            '<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">'
            '<rect width="1024" height="1024" fill="#dbeafe"/>'
            '<circle cx="760" cy="210" r="150" fill="#fde68a"/>'
            '<rect x="176" y="424" width="672" height="360" rx="48" fill="#ffffff" opacity="0.9"/>'
            '<path d="M284 620h456" stroke="#2563eb" stroke-width="34" stroke-linecap="round"/>'
            '<path d="M344 704h336" stroke="#059669" stroke-width="28" stroke-linecap="round"/>'
            '<circle cx="512" cy="394" r="92" fill="#fca5a5"/>'
            "</svg>"
        )
        return WishImageGenerationResponse(
            request_id=request.request_id,
            provider=self.config.code,
            model=self.config.model_name,
            prompt=prompt,
            content_type="image/svg+xml",
            image_base64=base64.b64encode(svg.encode("utf-8")).decode("ascii"),
            latency_ms=int((time.monotonic() - started) * 1000),
            cost_units=0.0,
        )


class CustomOpenAiCompatibleProvider(ImageProvider):
    def generate_wish_image(self, request: WishImageGenerationRequest) -> WishImageGenerationResponse:
        started = time.monotonic()
        prompt = self.prompt_for(request)
        payload = {
            "model": self.config.model_name,
            "prompt": prompt,
            "size": self.size_for(request),
            "n": 1,
            "response_format": self.config.extra_params.get("response_format", "b64_json"),
        }
        endpoint = self.config.base_url.rstrip("/") + "/images/generations"
        data = self.post_json(endpoint, payload, self.auth_headers())
        item = (data.get("data") or [{}])[0]
        return WishImageGenerationResponse(
            request_id=request.request_id,
            provider=self.config.code,
            model=self.config.model_name,
            prompt=prompt,
            content_type=str(item.get("mime_type") or "image/png"),
            image_base64=item.get("b64_json"),
            image_url=item.get("url"),
            latency_ms=int((time.monotonic() - started) * 1000),
            cost_units=float(self.config.extra_params.get("cost_units", 0) or 0),
        )

    def auth_headers(self) -> dict[str, str]:
        if not self.config.api_key:
            raise RuntimeError("image provider api_key is required")
        return {"Authorization": f"Bearer {self.config.api_key}"}


class VolcengineArkImageProvider(CustomOpenAiCompatibleProvider):
    def generate_wish_image(self, request: WishImageGenerationRequest) -> WishImageGenerationResponse:
        started = time.monotonic()
        prompt = self.prompt_for(request)
        payload = {
            "request_id": request.request_id,
            "model": self.config.model_name,
            "prompt": prompt,
            "size": self.config.extra_params.get("size", self.size_for(request)),
            "watermark": bool(self.config.extra_params.get("watermark", False)),
            "response_format": self.config.extra_params.get("response_format", "b64_json"),
        }
        endpoint = self.config.base_url.rstrip("/") + "/images/generations"
        data = self.post_json(endpoint, payload, self.auth_headers())
        item = (data.get("data") or [{}])[0]
        return WishImageGenerationResponse(
            request_id=request.request_id,
            provider=self.config.code,
            model=self.config.model_name,
            prompt=prompt,
            content_type=str(item.get("mime_type") or "image/png"),
            image_base64=item.get("b64_json"),
            image_url=item.get("url"),
            latency_ms=int((time.monotonic() - started) * 1000),
            cost_units=float(self.config.extra_params.get("cost_units", 0) or 0),
        )


class SiliconFlowImageProvider(CustomOpenAiCompatibleProvider):
    pass


class AliyunBailianImageProvider(ImageProvider):
    def generate_wish_image(self, request: WishImageGenerationRequest) -> WishImageGenerationResponse:
        started = time.monotonic()
        if not self.config.api_key:
            raise RuntimeError("image provider api_key is required")
        prompt = self.prompt_for(request)
        endpoint = self.config.base_url.rstrip("/") or "https://dashscope.aliyuncs.com"
        create_payload = {
            "model": self.config.model_name,
            "input": {"prompt": prompt, "negative_prompt": self.negative_prompt_for(request)},
            "parameters": {
                "size": self.config.extra_params.get("size", self.size_for(request)),
                **{key: value for key, value in self.config.extra_params.items() if key not in {"size", "cost_units", "poll_interval_seconds", "max_poll_seconds"}},
            },
        }
        task = self.post_json(
            endpoint + "/api/v1/services/aigc/text2image/image-synthesis",
            create_payload,
            {"Authorization": f"Bearer {self.config.api_key}", "X-DashScope-Async": "enable"},
        )
        task_id = (((task.get("output") or {}).get("task_id")) or task.get("task_id"))
        if not task_id:
            raise RuntimeError("aliyun bailian response did not include task_id")
        poll_interval = float(self.config.extra_params.get("poll_interval_seconds", 2) or 2)
        max_poll_seconds = float(self.config.extra_params.get("max_poll_seconds", 25) or 25)
        deadline = time.monotonic() + max_poll_seconds
        image_url = None
        while time.monotonic() < deadline:
            time.sleep(poll_interval)
            result = self.get_json(
                endpoint + f"/api/v1/tasks/{task_id}",
                {"Authorization": f"Bearer {self.config.api_key}"},
            )
            output = result.get("output") or {}
            status = output.get("task_status")
            if status == "SUCCEEDED":
                results = output.get("results") or []
                image_url = (results[0] or {}).get("url") if results else None
                break
            if status in {"FAILED", "CANCELED"}:
                raise RuntimeError(f"aliyun bailian task failed: {status}")
        if not image_url:
            raise TimeoutError("aliyun bailian task did not finish before timeout")
        return WishImageGenerationResponse(
            request_id=request.request_id,
            provider=self.config.code,
            model=self.config.model_name,
            prompt=prompt,
            content_type="image/png",
            image_url=image_url,
            latency_ms=int((time.monotonic() - started) * 1000),
            cost_units=float(self.config.extra_params.get("cost_units", 0) or 0),
        )


def create_provider(name: str) -> AiProvider:
    normalized = name.strip().lower()
    if normalized == "deterministic":
        return DeterministicAiProvider()
    raise ValueError(f"Unsupported AI provider: {name}")


def create_image_provider(config: ImageProviderConfig | None, worker_config: AiWorkerConfig | None = None) -> ImageProvider:
    provider_config = config or env_image_provider_config(worker_config or AiWorkerConfig())
    provider_type = provider_config.provider_type
    timeout = float(provider_config.extra_params.get("timeout_seconds", 120) or 120)
    if provider_type == "deterministic":
        return DeterministicImageProvider(provider_config, timeout_seconds=timeout)
    if provider_type == "volcengine_ark":
        return VolcengineArkImageProvider(provider_config, timeout_seconds=timeout)
    if provider_type == "aliyun_bailian":
        return AliyunBailianImageProvider(provider_config, timeout_seconds=timeout)
    if provider_type == "siliconflow":
        return SiliconFlowImageProvider(provider_config, timeout_seconds=timeout)
    if provider_type == "custom_openai_compatible":
        return CustomOpenAiCompatibleProvider(provider_config, timeout_seconds=timeout)
    raise ValueError(f"Unsupported image provider type: {provider_type}")


def env_image_provider_config(config: AiWorkerConfig) -> ImageProviderConfig:
    return ImageProviderConfig(
        code=config.image_provider_code,
        provider_type=config.image_provider_type,  # type: ignore[arg-type]
        base_url=config.image_base_url,
        api_key=config.image_api_key,
        model_name=config.image_model_name,
        extra_params={"timeout_seconds": config.request_timeout_seconds},
    )
