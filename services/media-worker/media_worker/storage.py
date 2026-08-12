from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import boto3

from .config import MediaWorkerConfig


@dataclass(frozen=True)
class ObjectStorage:
    config: MediaWorkerConfig

    def __post_init__(self) -> None:
        object.__setattr__(
            self,
            "_client",
            boto3.client(
                "s3",
                endpoint_url=self.config.s3_endpoint,
                aws_access_key_id=self.config.s3_access_key,
                aws_secret_access_key=self.config.s3_secret_key,
                region_name=self.config.s3_region,
            ),
        )

    def download(self, key: str, target: Path) -> None:
        target.parent.mkdir(parents=True, exist_ok=True)
        self._client.download_file(self.config.s3_bucket, key, str(target))

    def upload(self, key: str, source: Path, content_type: str) -> int:
        self._client.upload_file(
            str(source),
            self.config.s3_bucket,
            key,
            ExtraArgs={"ContentType": content_type},
        )
        return source.stat().st_size
