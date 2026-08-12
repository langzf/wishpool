from __future__ import annotations

from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from typing import Any, Callable

from ai_worker.config import AiWorkerConfig, load_config
from ai_worker.models import (
    AiPrecheckRequest,
    FeedbackDraftRequest,
    MemoryNarrativeRequest,
    PrivacySummaryRequest,
    ValidationError,
)
from ai_worker.provider import AiProvider, create_provider


RequestFactory = Callable[[dict[str, Any]], Any]
ProviderMethod = Callable[[Any], Any]


class AiWorkerApp:
    def __init__(self, config: AiWorkerConfig | None = None, provider: AiProvider | None = None):
        self.config = config or load_config()
        self.provider = provider or create_provider(self.config.provider)

    def handle(self, method: str, path: str, headers: dict[str, str], body: bytes) -> tuple[int, dict[str, Any]]:
        if method == "GET" and path == "/health":
            return HTTPStatus.OK, {"status": "ok", "provider": self.config.provider}

        routes: dict[str, tuple[RequestFactory, ProviderMethod]] = {
            "/internal/ai/precheck-submission": (AiPrecheckRequest.from_dict, self.provider.precheck_submission),
            "/internal/ai/draft-feedback": (FeedbackDraftRequest.from_dict, self.provider.draft_feedback),
            "/internal/ai/generate-memory-narrative": (
                MemoryNarrativeRequest.from_dict,
                self.provider.generate_memory_narrative,
            ),
            "/internal/ai/summarize-privacy-request": (
                PrivacySummaryRequest.from_dict,
                self.provider.summarize_privacy_request,
            ),
        }

        if method != "POST" or path not in routes:
            return HTTPStatus.NOT_FOUND, {"detail": "Not found"}

        if headers.get("authorization") != f"Bearer {self.config.internal_token}":
            return HTTPStatus.UNAUTHORIZED, {"detail": "Invalid internal token"}

        try:
            payload = json.loads(body.decode("utf-8") or "{}")
            if not isinstance(payload, dict):
                raise ValidationError("body must be a JSON object")
            factory, provider_method = routes[path]
            request = factory(payload)
            response = provider_method(request)
            return HTTPStatus.OK, response.to_dict()
        except json.JSONDecodeError:
            return HTTPStatus.BAD_REQUEST, {"detail": "Malformed JSON"}
        except ValidationError as exc:
            return HTTPStatus.BAD_REQUEST, {"detail": str(exc)}


def create_handler(app: AiWorkerApp) -> type[BaseHTTPRequestHandler]:
    class AiWorkerHandler(BaseHTTPRequestHandler):
        server_version = "WishPoolAiWorker/0.1"

        def do_GET(self) -> None:
            self._handle()

        def do_POST(self) -> None:
            self._handle()

        def log_message(self, format: str, *args: Any) -> None:
            return

        def _handle(self) -> None:
            length = int(self.headers.get("content-length", "0"))
            body = self.rfile.read(length) if length else b""
            headers = {key.lower(): value for key, value in self.headers.items()}
            status_code, payload = app.handle(self.command, self.path.split("?", 1)[0], headers, body)
            encoded = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            self.send_response(status_code)
            self.send_header("content-type", "application/json; charset=utf-8")
            self.send_header("content-length", str(len(encoded)))
            self.end_headers()
            self.wfile.write(encoded)

    return AiWorkerHandler


def run_server(config: AiWorkerConfig | None = None) -> None:
    app = AiWorkerApp(config)
    server = ThreadingHTTPServer((app.config.host, app.config.port), create_handler(app))
    try:
        server.serve_forever()
    finally:
        server.server_close()
