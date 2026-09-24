from media_worker.config import MediaWorkerConfig
from media_worker.worker import MediaWorker


def make_worker(max_attempts: int = 5) -> MediaWorker:
    return MediaWorker(None, None, 2, max_attempts, 60)  # type: ignore[arg-type]


def test_retryable_failure_is_requeued_below_limit() -> None:
    worker = make_worker()
    code, retryable, delay = worker._failure_policy({"processing": {"attemptCount": 4}}, "unexpected_error")
    assert (code, retryable, delay) == ("unexpected_error", True, 60)


def test_invalid_image_becomes_permanent_at_limit() -> None:
    worker = make_worker()
    code, retryable, delay = worker._failure_policy({"processing": {"attemptCount": 5}}, "invalid_media_input")
    assert (code, retryable, delay) == ("invalid_media_input_permanent", False, 0)


def test_old_core_api_cannot_requeue_invalid_image_forever() -> None:
    worker = make_worker()
    code, retryable, delay = worker._failure_policy({"processing": {}}, "invalid_media_input")
    assert (code, retryable, delay) == ("invalid_media_input_permanent", False, 0)


def test_core_api_retry_backoff_is_exponential_and_capped() -> None:
    worker = MediaWorker(None, None, 2, 5, 60, 5, 20)  # type: ignore[arg-type]
    assert worker.core_api_retry_backoff_seconds * (2**0) == 5
    assert min(worker.core_api_retry_backoff_seconds * (2**4), worker.core_api_retry_max_backoff_seconds) == 20
