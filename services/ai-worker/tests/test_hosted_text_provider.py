import json
from unittest.mock import patch

from ai_worker.models import AiPrecheckRequest, MediaSignal, TextProviderConfig
from ai_worker.provider import HostedTextProvider


def test_hosted_provider_posts_structured_chat_request_and_parses_response():
    provider = HostedTextProvider(TextProviderConfig(
        code="stub-openai", provider_type="openai_compatible", base_url="http://stub",
        api_key="test-only", model_name="stub-model", capability="text_vision"))
    req = AiPrecheckRequest("s", "Read", "reading", media=[MediaSignal("m", "image", "image/png", image_url="http://signed/image.png")])
    body = {"choices": [{"message": {"content": json.dumps({
        "summary": "STUB SUMMARY", "risk_level": "low", "confidence": 0.99,
        "suggested_decision": "approve", "checklist": ["stub"], "safety_notes": []
    })}}]}
    class Response:
        def __enter__(self): return self
        def __exit__(self, *args): pass
        def read(self): return json.dumps(body).encode()
    with patch("ai_worker.provider.urlrequest.urlopen", return_value=Response()) as call:
        result = provider.precheck_submission(req)
    payload = json.loads(call.call_args.args[0].data)
    assert payload["model"] == "stub-model"
    assert payload["messages"][-1]["content"][1]["type"] == "image_url"
    assert result.provider_status == "used"
    assert result.summary == "STUB SUMMARY"
