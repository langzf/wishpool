import json
import http.client
import threading
import unittest
from unittest.mock import patch

from http.server import ThreadingHTTPServer

from ai_worker.app import AiWorkerApp, create_handler
from ai_worker.models import ImageProviderConfig, WishImageGenerationRequest
from ai_worker.provider import VolcengineArkImageProvider, create_image_provider


class AiWorkerAppTests(unittest.TestCase):
    def setUp(self) -> None:
        self.app = AiWorkerApp()
        self.headers = {"authorization": "Bearer wishpool-local-internal-token"}

    def post(self, path: str, payload: dict, headers: dict[str, str] | None = None) -> tuple[int, dict]:
        body = json.dumps(payload).encode("utf-8")
        status_code, response = self.app.handle("POST", path, self.headers if headers is None else headers, body)
        return int(status_code), response

    def test_health_reports_provider(self) -> None:
        status_code, response = self.app.handle("GET", "/health", {}, b"")

        self.assertEqual(status_code, 200)
        self.assertEqual(response["provider"], "deterministic")

    def test_precheck_submission_requires_internal_token(self) -> None:
        status_code, _ = self.post(
            "/internal/ai/precheck-submission",
            {
                "submission_id": "submission-1",
                "task_title": "亲子阅读",
                "task_category": "growth",
            },
            headers={},
        )

        self.assertEqual(status_code, 401)

    def test_precheck_submission_returns_review_guidance(self) -> None:
        status_code, response = self.post(
            "/internal/ai/precheck-submission",
            {
                "submission_id": "submission-1",
                "task_title": "亲子阅读",
                "task_category": "growth",
                "child_age": 7,
                "media": [
                    {
                        "media_id": "media-1",
                        "kind": "image",
                        "mime_type": "image/jpeg",
                        "visual_labels": ["book", "family"],
                    }
                ],
                "child_note": "今天读完了故事。",
            },
        )

        self.assertEqual(status_code, 200)
        self.assertEqual(response["submission_id"], "submission-1")
        self.assertEqual(response["risk_level"], "low")
        self.assertEqual(response["suggested_decision"], "approve")

    def test_feedback_draft_for_revision(self) -> None:
        status_code, response = self.post(
            "/internal/ai/draft-feedback",
            {
                "child_name": "小满",
                "task_title": "钢琴练习",
                "decision": "needs_revision",
                "evidence_summary": "视频里只录到开始部分",
            },
        )

        self.assertEqual(status_code, 200)
        self.assertIn("再", response["title"])

    def test_memory_narrative_and_privacy_summary(self) -> None:
        memory_status, memory_response = self.post(
            "/internal/ai/generate-memory-narrative",
            {
                "child_name": "小满",
                "week_start_date": "2026-08-10",
                "approved_task_titles": ["阅读", "运动"],
                "wish_title": "城市积木",
            },
        )
        privacy_status, privacy_response = self.post(
            "/internal/ai/summarize-privacy-request",
            {
                "request_id": "privacy-1",
                "request_type": "delete",
                "family_id": "family-1",
                "child_ids": ["child-1"],
                "requested_scopes": ["media", "submissions"],
            },
        )

        self.assertEqual(memory_status, 200)
        self.assertIn("成长", memory_response["title"])
        self.assertEqual(privacy_status, 200)
        self.assertTrue(privacy_response["risk_notes"])

    def test_generate_wish_image_uses_request_provider_config(self) -> None:
        status_code, response = self.post(
            "/internal/ai/generate-wish-image",
            {
                "request_id": "wish-image-1",
                "family_id": "family-1",
                "child_age": 7,
                "wish_title": "A beginner microscope kit",
                "wish_note": "For observing leaves and small stones",
                "category": "science",
                "provider_code": "deterministic",
                "provider_config": {
                    "code": "deterministic",
                    "provider_type": "deterministic",
                    "base_url": "deterministic://local",
                    "api_key": None,
                    "model_name": "deterministic-wish-image",
                    "extra_params": {"timeout_seconds": 5},
                },
            },
        )

        self.assertEqual(status_code, 200)
        self.assertEqual(response["provider"], "deterministic")
        self.assertEqual(response["model"], "deterministic-wish-image")
        self.assertEqual(response["content_type"], "image/svg+xml")
        self.assertTrue(response["image_base64"])

    def test_http_handler_reads_chunked_json_body(self) -> None:
        server = ThreadingHTTPServer(("127.0.0.1", 0), create_handler(self.app))
        thread = threading.Thread(target=server.serve_forever)
        thread.start()
        try:
            connection = http.client.HTTPConnection(*server.server_address)
            connection.putrequest("POST", "/internal/ai/generate-wish-image")
            connection.putheader("Authorization", self.headers["authorization"])
            connection.putheader("Content-Type", "application/json")
            connection.putheader("Transfer-Encoding", "chunked")
            connection.endheaders()
            body = json.dumps(
                {
                    "request_id": "chunked-image-1",
                    "family_id": "family-1",
                    "wish_title": "A microscope",
                    "provider_code": "deterministic",
                    "provider_config": {
                        "code": "deterministic",
                        "provider_type": "deterministic",
                        "base_url": "deterministic://local",
                        "model_name": "deterministic-wish-image",
                        "extra_params": {},
                    },
                }
            ).encode("utf-8")
            split = len(body) // 2
            for chunk in (body[:split], body[split:]):
                connection.send(f"{len(chunk):X}\r\n".encode("ascii") + chunk + b"\r\n")
            connection.send(b"0\r\nX-Test: trailer\r\n\r\n")
            response = connection.getresponse()
            payload = json.loads(response.read())
            connection.close()
        finally:
            server.shutdown()
            thread.join()
            server.server_close()

        self.assertEqual(response.status, 200)
        self.assertEqual(payload["request_id"], "chunked-image-1")

    def test_http_handler_reads_content_length_json_body(self) -> None:
        server = ThreadingHTTPServer(("127.0.0.1", 0), create_handler(self.app))
        thread = threading.Thread(target=server.serve_forever)
        thread.start()
        try:
            body = json.dumps(
                {
                    "request_id": "fixed-length-image-1",
                    "family_id": "family-1",
                    "wish_title": "A telescope",
                    "provider_code": "deterministic",
                    "provider_config": {
                        "code": "deterministic",
                        "provider_type": "deterministic",
                        "base_url": "deterministic://local",
                        "model_name": "deterministic-wish-image",
                        "extra_params": {},
                    },
                }
            ).encode("utf-8")
            connection = http.client.HTTPConnection(*server.server_address)
            connection.request(
                "POST",
                "/internal/ai/generate-wish-image",
                body=body,
                headers={
                    "Authorization": self.headers["authorization"],
                    "Content-Type": "application/json",
                },
            )
            response = connection.getresponse()
            payload = json.loads(response.read())
            connection.close()
        finally:
            server.shutdown()
            thread.join()
            server.server_close()

        self.assertEqual(response.status, 200)
        self.assertEqual(payload["request_id"], "fixed-length-image-1")

    def test_create_image_provider_defaults_timeout_to_120_seconds(self) -> None:
        config = ImageProviderConfig(
            code="deterministic",
            provider_type="deterministic",
            base_url="deterministic://local",
            api_key=None,
            model_name="deterministic-wish-image",
            extra_params={},
        )

        provider = create_image_provider(config)

        self.assertEqual(provider.timeout_seconds, 120)

    def test_create_image_provider_uses_configured_timeout(self) -> None:
        config = ImageProviderConfig(
            code="deterministic",
            provider_type="deterministic",
            base_url="deterministic://local",
            api_key=None,
            model_name="deterministic-wish-image",
            extra_params={"timeout_seconds": 37},
        )

        provider = create_image_provider(config)

        self.assertEqual(provider.timeout_seconds, 37)

    def test_volcengine_ark_image_payload_includes_request_id(self) -> None:
        config = ImageProviderConfig(
            code="seedream-5-pro",
            provider_type="volcengine_ark",
            base_url="https://ark.example.com",
            api_key="test-api-key",
            model_name="doubao-seedream-5-0-pro-260628",
            extra_params={},
        )
        request = WishImageGenerationRequest(
            request_id="wish-image-ark-1",
            family_id="family-1",
            child_age=7,
            wish_title="A beginner microscope kit",
        )
        provider = VolcengineArkImageProvider(config)

        with patch.object(provider, "post_json", return_value={"data": [{"b64_json": "generated-image"}]}) as post_json:
            response = provider.generate_wish_image(request)

        post_json.assert_called_once()
        endpoint, payload, headers = post_json.call_args.args
        self.assertEqual(endpoint, "https://ark.example.com/images/generations")
        self.assertEqual(payload["request_id"], request.request_id)
        self.assertEqual(payload["model"], config.model_name)
        self.assertEqual(headers, {"Authorization": "Bearer test-api-key"})
        self.assertEqual(response.request_id, request.request_id)

    def test_volcengine_ark_image_endpoint_preserves_api_root_path(self) -> None:
        config = ImageProviderConfig(
            code="seedream-5-pro",
            provider_type="volcengine_ark",
            base_url="https://ark.example.com/api/v3",
            api_key="test-api-key",
            model_name="doubao-seedream-5-0-pro-260628",
            extra_params={},
        )
        request = WishImageGenerationRequest(
            request_id="wish-image-ark-api-root-1",
            family_id="family-1",
            child_age=7,
            wish_title="A beginner microscope kit",
        )
        provider = VolcengineArkImageProvider(config)

        with patch.object(provider, "post_json", return_value={"data": [{"b64_json": "generated-image"}]}) as post_json:
            provider.generate_wish_image(request)

        endpoint, _, _ = post_json.call_args.args
        self.assertEqual(endpoint, "https://ark.example.com/api/v3/images/generations")


if __name__ == "__main__":
    unittest.main()
