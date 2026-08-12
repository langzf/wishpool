from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class MediaWorkerConfig:
    core_api_base_url: str
    internal_token: str
    s3_endpoint: str
    s3_bucket: str
    s3_access_key: str
    s3_secret_key: str
    s3_region: str
    poll_interval_seconds: float
    claim_limit: int
    lease_seconds: int
    work_dir: str

    @classmethod
    def from_env(cls, env: dict[str, str] | None = None) -> "MediaWorkerConfig":
        values = env if env is not None else os.environ
        return cls(
            core_api_base_url=values.get("WISHPOOL_CORE_API_BASE_URL", "http://localhost:8080").rstrip("/"),
            internal_token=values.get("WISHPOOL_INTERNAL_TOKEN", "wishpool-local-internal-token"),
            s3_endpoint=values.get("WISHPOOL_S3_ENDPOINT", values.get("S3_ENDPOINT", "http://localhost:9000")),
            s3_bucket=values.get("WISHPOOL_S3_BUCKET", values.get("S3_BUCKET", "wishpool-media")),
            s3_access_key=values.get("WISHPOOL_S3_ACCESS_KEY", values.get("S3_ACCESS_KEY", "wishpool")),
            s3_secret_key=values.get("WISHPOOL_S3_SECRET_KEY", values.get("S3_SECRET_KEY", "wishpool-local")),
            s3_region=values.get("WISHPOOL_S3_REGION", values.get("S3_REGION", "local")),
            poll_interval_seconds=max(float(values.get("WISHPOOL_MEDIA_POLL_INTERVAL_SECONDS", "2")), 0.25),
            claim_limit=min(max(int(values.get("WISHPOOL_MEDIA_CLAIM_LIMIT", "5")), 1), 50),
            lease_seconds=min(max(int(values.get("WISHPOOL_MEDIA_LEASE_SECONDS", "300")), 30), 3600),
            work_dir=values.get("WISHPOOL_MEDIA_WORK_DIR", "/tmp/wishpool-media-worker"),
        )
