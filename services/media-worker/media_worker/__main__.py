from __future__ import annotations

import argparse
import logging

from .config import MediaWorkerConfig
from .core_api import CoreApiClient
from .processor import MediaProcessor
from .storage import ObjectStorage
from .worker import MediaWorker


def main() -> None:
    parser = argparse.ArgumentParser(description="WishPool media processing worker")
    parser.add_argument("--once", action="store_true", help="claim and process one batch, then exit")
    args = parser.parse_args()

    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
    config = MediaWorkerConfig.from_env()
    storage = ObjectStorage(config)
    worker = MediaWorker(
        core_api=CoreApiClient(config),
        processor=MediaProcessor(config, storage),
        poll_interval_seconds=config.poll_interval_seconds,
        max_attempts=config.max_attempts,
        retry_backoff_seconds=config.retry_backoff_seconds,
        core_api_retry_backoff_seconds=config.core_api_retry_backoff_seconds,
        core_api_retry_max_backoff_seconds=config.core_api_retry_max_backoff_seconds,
    )
    if args.once:
        worker.run_once()
    else:
        worker.run_forever()


if __name__ == "__main__":
    main()
