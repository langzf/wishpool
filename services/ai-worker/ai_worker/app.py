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
    WishImageGenerationRequest,
)
from ai_worker.provider import AiProvider, create_image_provider, create_provider


RequestFactory = Callable[[dict[str, Any]], Any]
ProviderMethod = Callable[[Any], Any]
MAX_REQUEST_BODY_BYTES = 10 * 1024 * 1024


class RequestBodyError(ValueError):
    def __init__(self, status_code: HTTPStatus, detail: str):
        super().__init__(detail)
        self.status_code = status_code


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
            "/internal/ai/generate-wish-image": (
                WishImageGenerationRequest.from_dict,
                lambda req: create_image_provider(req.provider_config, self.config).generate_wish_image(req),
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

        def _read_body(self) -> bytes:
            transfer_encoding = self.headers.get("transfer-encoding", "")
            if transfer_encoding and transfer_encoding.lower().split(",")[-1].strip() == "chunked":
                return self._read_chunked_body()

            content_length = self.headers.get("content-length")
            if content_length is None:
                return b""
            try:
                length = int(content_length)
            except ValueError as exc:
                raise RequestBodyError(HTTPStatus.BAD_REQUEST, "Invalid Content-Length") from exc
            if length < 0:
                raise RequestBodyError(HTTPStatus.BAD_REQUEST, "Invalid Content-Length")
            if length > MAX_REQUEST_BODY_BYTES:
                raise RequestBodyError(HTTPStatus.REQUEST_ENTITY_TOO_LARGE, "Request body too large")
            return self.rfile.read(length)

        def _read_chunked_body(self) -> bytes:
            body = bytearray()
            while True:
                size_line = self.rfile.readline()
                if not size_line:
                    raise RequestBodyError(HTTPStatus.BAD_REQUEST, "Malformed chunked body")
                try:
                    size_text = size_line.strip().split(b";", 1)[0]
                    chunk_size = int(size_text, 16)
                except (ValueError, UnicodeDecodeError) as exc:
                    raise RequestBodyError(HTTPStatus.BAD_REQUEST, "Malformed chunk size") from exc
                if chunk_size < 0:
                    raise RequestBodyError(HTTPStatus.BAD_REQUEST, "Malformed chunk size")
                if len(body) + chunk_size > MAX_REQUEST_BODY_BYTES:
                    raise RequestBodyError(HTTPStatus.REQUEST_ENTITY_TOO_LARGE, "Request body too large")
                if chunk_size == 0:
                    while True:
                        trailer = self.rfile.readline()
                        if not trailer or trailer in (b"\r\n", b"\n"):
                            return bytes(body)
                        if b":" not in trailer:
                            raise RequestBodyError(HTTPStatus.BAD_REQUEST, "Malformed chunk trailer")
                chunk = self.rfile.read(chunk_size)
                if len(chunk) != chunk_size or self.rfile.read(2) != b"\r\n":
                    raise RequestBodyError(HTTPStatus.BAD_REQUEST, "Malformed chunked body")
                body.extend(chunk)

        def _handle(self) -> None:
            headers = {key.lower(): value for key, value in self.headers.items()}
            try:
                body = self._read_body()
                status_code, payload = app.handle(self.command, self.path.split("?", 1)[0], headers, body)
            except RequestBodyError as exc:
                status_code, payload = exc.status_code, {"detail": str(exc)}
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
