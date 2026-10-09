from __future__ import annotations

from dataclasses import dataclass
from typing import Any

import requests

from .config import MediaWorkerConfig


@dataclass(frozen=True)
class CoreApiClient:
    config: MediaWorkerConfig

    def claim(self) -> list[dict[str, Any]]:
        response = self._post(
            "/internal/media/processing/claim",
            {"limit": self.config.claim_limit, "leaseSeconds": self.config.lease_seconds},
        )
        return list(response.get("items", []))

    def heartbeat(self, leases: list[dict[str, str]], lease_seconds: int) -> None:
        self._post("/internal/media/processing/heartbeat", {"leases": leases, "leaseSeconds": lease_seconds})

    def reap_expired(self) -> int:
        return int(self._post("/internal/media/processing/reap-expired", {}).get("count", 0))

    def complete(self, media_id: str, lease_token: str, derivatives: list[dict[str, Any]]) -> None:
        self._post(
            f"/internal/media/{media_id}/processing-completed",
            {"derivatives": derivatives, "leaseToken": lease_token},
        )

    def fail(
        self,
        media_id: str,
        lease_token: str,
        error_code: str,
        error_message: str,
        retryable: bool = True,
        delay_seconds: int | None = None,
    ) -> None:
        self._post(
            f"/internal/media/{media_id}/processing-failed",
            {
                "errorCode": error_code,
                "errorMessage": error_message,
                "retryable": retryable,
                "delaySeconds": delay_seconds if delay_seconds is not None else (60 if retryable else 0),
                "leaseToken": lease_token,
            },
        )

    def _post(self, path: str, body: dict[str, Any]) -> dict[str, Any]:
        url = f"{self.config.core_api_base_url}{path}"
        response = requests.post(
            url,
            json=body,
            headers={"X-Internal-Token": self.config.internal_token},
            timeout=30,
        )
        if response.status_code < 200 or response.status_code > 299:
            raise CoreApiError(response.status_code, response.text)
        if not response.text:
            return {}
        return response.json()


class CoreApiError(RuntimeError):
    def __init__(self, status_code: int, body: str) -> None:
        super().__init__(f"Core API request failed with HTTP {status_code}: {body}")
        self.status_code = status_code
        self.body = body
