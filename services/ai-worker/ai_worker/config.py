from dataclasses import dataclass
import os


@dataclass(frozen=True)
class AiWorkerConfig:
    provider: str = "deterministic"
    core_api_base_url: str = "http://localhost:8080"
    internal_token: str = "wishpool-local-internal-token"
    request_timeout_seconds: float = 120.0
    max_prompt_chars: int = 6000
    image_provider_code: str = "deterministic"
    image_provider_type: str = "deterministic"
    image_base_url: str = "deterministic://local"
    image_api_key: str | None = None
    image_model_name: str = "deterministic-wish-image"
    host: str = "0.0.0.0"
    port: int = 8100


def _float_env(name: str, default: float, minimum: float, maximum: float) -> float:
    raw = os.getenv(name)
    if raw is None:
        return default
    value = float(raw)
    if value < minimum or value > maximum:
        raise ValueError(f"{name} must be between {minimum} and {maximum}")
    return value


def _int_env(name: str, default: int, minimum: int, maximum: int) -> int:
    raw = os.getenv(name)
    if raw is None:
        return default
    value = int(raw)
    if value < minimum or value > maximum:
        raise ValueError(f"{name} must be between {minimum} and {maximum}")
    return value


def load_config() -> AiWorkerConfig:
    return AiWorkerConfig(
        provider=os.getenv("WISHPOOL_AI_PROVIDER", "deterministic"),
        core_api_base_url=os.getenv("WISHPOOL_AI_CORE_API_BASE_URL", "http://localhost:8080"),
        internal_token=os.getenv("WISHPOOL_AI_INTERNAL_TOKEN", "wishpool-local-internal-token"),
        request_timeout_seconds=_float_env("WISHPOOL_AI_REQUEST_TIMEOUT_SECONDS", 120.0, 1.0, 120.0),
        max_prompt_chars=_int_env("WISHPOOL_AI_MAX_PROMPT_CHARS", 6000, 1000, 32000),
        image_provider_code=os.getenv("WISHPOOL_AI_IMAGE_PROVIDER_CODE", "deterministic"),
        image_provider_type=os.getenv("WISHPOOL_AI_IMAGE_PROVIDER_TYPE", "deterministic"),
        image_base_url=os.getenv("WISHPOOL_AI_IMAGE_BASE_URL", "deterministic://local"),
        image_api_key=os.getenv("WISHPOOL_AI_IMAGE_API_KEY"),
        image_model_name=os.getenv("WISHPOOL_AI_IMAGE_MODEL_NAME", "deterministic-wish-image"),
        host=os.getenv("WISHPOOL_AI_HOST", "0.0.0.0"),
        port=_int_env("WISHPOOL_AI_PORT", 8100, 1, 65535),
    )
