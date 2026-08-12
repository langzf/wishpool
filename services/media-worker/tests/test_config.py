from media_worker.config import MediaWorkerConfig


def test_config_loads_defaults_and_overrides() -> None:
    config = MediaWorkerConfig.from_env(
        {
            "WISHPOOL_CORE_API_BASE_URL": "http://core-api:8080/",
            "WISHPOOL_INTERNAL_TOKEN": "internal",
            "WISHPOOL_S3_ENDPOINT": "http://minio:9000",
            "WISHPOOL_S3_BUCKET": "bucket",
            "WISHPOOL_MEDIA_POLL_INTERVAL_SECONDS": "0.1",
            "WISHPOOL_MEDIA_CLAIM_LIMIT": "100",
            "WISHPOOL_MEDIA_LEASE_SECONDS": "10",
        }
    )

    assert config.core_api_base_url == "http://core-api:8080"
    assert config.internal_token == "internal"
    assert config.s3_endpoint == "http://minio:9000"
    assert config.s3_bucket == "bucket"
    assert config.poll_interval_seconds == 0.25
    assert config.claim_limit == 50
    assert config.lease_seconds == 30
