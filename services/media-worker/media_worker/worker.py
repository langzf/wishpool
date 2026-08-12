from __future__ import annotations

import logging
import time
from dataclasses import dataclass

from .core_api import CoreApiClient
from .processor import MediaProcessingError, MediaProcessor

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class MediaWorker:
    core_api: CoreApiClient
    processor: MediaProcessor
    poll_interval_seconds: float

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
                self.core_api.fail(media_id, exc.code, str(exc), retryable=True)
                logger.warning("Media processing failed for %s: %s", media_id, exc)
            except Exception as exc:
                self.core_api.fail(media_id, "unexpected_error", str(exc), retryable=True)
                logger.exception("Unexpected media processing failure for %s", media_id)
        return len(items)

    def run_forever(self) -> None:
        while True:
            processed = self.run_once()
            if processed == 0:
                time.sleep(self.poll_interval_seconds)
