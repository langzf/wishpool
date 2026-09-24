from __future__ import annotations

import logging
import time
from dataclasses import dataclass

import requests

from .core_api import CoreApiClient
from .processor import MediaProcessingError, MediaProcessor

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class MediaWorker:
    core_api: CoreApiClient
    processor: MediaProcessor
    poll_interval_seconds: float
    max_attempts: int
    retry_backoff_seconds: int
    core_api_retry_backoff_seconds: float = 5
    core_api_retry_max_backoff_seconds: float = 60

    @staticmethod
    def _attempt_count(item: dict) -> int | None:
        value = item.get("processing", {}).get("attemptCount")
        return int(value) if value is not None else None

    def _failure_policy(self, item: dict, error_code: str) -> tuple[str, bool, int]:
        attempt_count = self._attempt_count(item)
        # Older Core API instances did not expose the internal attempt count.
        # Invalid media is deterministically unrecoverable, so fail it closed
        # during the compatibility window instead of allowing another poison loop.
        exhausted = (
            attempt_count is None and error_code == "invalid_media_input"
        ) or (attempt_count is not None and attempt_count >= self.max_attempts)
        if exhausted:
            permanent_code = "invalid_media_input_permanent" if error_code == "invalid_media_input" else "processing_retry_exhausted"
            return permanent_code, False, 0
        return error_code, True, self.retry_backoff_seconds

    def run_once(self) -> int:
        items = self.core_api.claim()
        for item in items:
            media = item["media"]
            media_id = media["id"]
            try:
                derivatives = self.processor.process(item)
                self.core_api.complete(media_id, derivatives)
                logger.info("Processed media asset %s with %s derivatives", media_id, len(derivatives))
            except MediaProcessingError as exc:
                code, retryable, delay = self._failure_policy(item, exc.code)
                self.core_api.fail(media_id, code, str(exc), retryable=retryable, delay_seconds=delay)
                logger.warning("Media processing failed for %s: %s", media_id, exc)
            except Exception as exc:
                code, retryable, delay = self._failure_policy(item, "unexpected_error")
                self.core_api.fail(media_id, code, str(exc), retryable=retryable, delay_seconds=delay)
                logger.exception("Unexpected media processing failure for %s", media_id)
        return len(items)

    def run_forever(self) -> None:
        core_api_failure_count = 0
        while True:
            try:
                processed = self.run_once()
                if core_api_failure_count:
                    logger.info("MEDIA_WORKER_CORE_API_RECOVERED failures=%s", core_api_failure_count)
                    core_api_failure_count = 0
            except (requests.ConnectionError, requests.Timeout) as exc:
                core_api_failure_count += 1
                delay = min(
                    self.core_api_retry_backoff_seconds * (2 ** (core_api_failure_count - 1)),
                    self.core_api_retry_max_backoff_seconds,
                )
                logger.warning(
                    "MEDIA_WORKER_CORE_API_RETRY attempt=%s delay_seconds=%.2f error=%s",
                    core_api_failure_count,
                    delay,
                    type(exc).__name__,
                )
                time.sleep(delay)
                continue
            if processed == 0:
                time.sleep(self.poll_interval_seconds)
