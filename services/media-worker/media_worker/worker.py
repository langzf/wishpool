from __future__ import annotations

import logging
import time
import threading
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
        if not items:
            self.core_api.reap_expired()
        for item in items:
            media = item["media"]
            media_id = media["id"]
            lease_token = item["leaseToken"]
            stop_heartbeat = threading.Event()
            def heartbeat() -> None:
                interval = max(self.core_api.config.lease_seconds / 3, 10)
                while not stop_heartbeat.wait(interval):
                    try:
                        self.core_api.heartbeat([{"mediaId": media_id, "leaseToken": lease_token}], self.core_api.config.lease_seconds)
                    except Exception:
                        logger.warning("媒体处理心跳续租失败 id=%s", media_id, exc_info=True)
            heartbeat_thread = threading.Thread(target=heartbeat, daemon=True)
            heartbeat_thread.start()
            started_at = time.perf_counter()
            try:
                derivatives = self.processor.process(item)
                stop_heartbeat.set()
                self.core_api.complete(media_id, lease_token, derivatives)
                elapsed_ms = (time.perf_counter() - started_at) * 1000
                logger.info(
                    "MEDIA_PROCESSED asset=%s elapsed_ms=%.1f derivative_kinds=%s derivative_count=%s",
                    media_id,
                    elapsed_ms,
                    ",".join(item["kind"] for item in derivatives),
                    len(derivatives),
                )
            except MediaProcessingError as exc:
                code, retryable, delay = self._failure_policy(item, exc.code)
                stop_heartbeat.set()
                self.core_api.fail(media_id, lease_token, code, str(exc), retryable=retryable, delay_seconds=delay)
                logger.warning("Media processing failed for %s: %s", media_id, exc)
            except Exception as exc:
                code, retryable, delay = self._failure_policy(item, "unexpected_error")
                stop_heartbeat.set()
                self.core_api.fail(media_id, lease_token, code, str(exc), retryable=retryable, delay_seconds=delay)
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
