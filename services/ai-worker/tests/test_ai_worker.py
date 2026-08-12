import json
import unittest

from ai_worker.app import AiWorkerApp


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


if __name__ == "__main__":
    unittest.main()
