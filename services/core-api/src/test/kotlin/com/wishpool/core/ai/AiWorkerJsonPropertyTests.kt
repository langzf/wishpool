package com.wishpool.core.ai

import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiWorkerJsonPropertyTests {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `wish image request serializes ai worker contract as snake case`() {
        val json = objectMapper.writeValueAsString(
            AiWorkerWishImageGenerationRequest(
                request_id = "job-1",
                family_id = "family-1",
                child_age = 8,
                wish_title = "Microscope",
                wish_note = "For science tasks",
                aspect_ratio = "1:1",
                negative_prompt = "dark",
                provider_code = "deterministic",
                provider_config = AiWorkerImageProviderConfig(
                    code = "deterministic",
                    provider_type = "local",
                    base_url = "http://localhost:8100",
                    api_key = null,
                    model_name = "deterministic-wish-image",
                    extra_params = mapOf("seed" to 7),
                ),
            )
        )

        assertTrue("\"request_id\"" in json)
        assertTrue("\"family_id\"" in json)
        assertTrue("\"child_age\"" in json)
        assertTrue("\"wish_title\"" in json)
        assertTrue("\"wish_note\"" in json)
        assertTrue("\"aspect_ratio\"" in json)
        assertTrue("\"negative_prompt\"" in json)
        assertTrue("\"provider_code\"" in json)
        assertTrue("\"provider_config\"" in json)
        assertTrue("\"provider_type\"" in json)
        assertTrue("\"base_url\"" in json)
        assertTrue("\"api_key\"" in json)
        assertTrue("\"model_name\"" in json)
        assertTrue("\"extra_params\"" in json)
        assertFalse("\"requestId\"" in json)
        assertFalse("\"providerConfig\"" in json)
    }

    @Test
    fun `wish image request contains required rest client payload fields`() {
        val json = objectMapper.writeValueAsString(
            AiWorkerWishImageGenerationRequest(
                request_id = "job-2",
                family_id = "family-2",
                wish_title = "A telescope",
            )
        )

        val payload = objectMapper.readTree(json)
        assertEquals("job-2", payload.get("request_id").stringValue())
        assertEquals("family-2", payload.get("family_id").stringValue())
        assertEquals("A telescope", payload.get("wish_title").stringValue())
        assertEquals("1:1", payload.get("aspect_ratio").stringValue())
    }

    @Test
    fun `wish image response deserializes ai worker snake case contract`() {
        val response = objectMapper.readValue<AiWorkerWishImageGenerationResponse>(
            """
            {
              "request_id": "job-1",
              "provider": "deterministic",
              "model": "deterministic-wish-image",
              "prompt": "Create a warm illustration",
              "content_type": "image/svg+xml",
              "image_base64": "PHN2Zz48L3N2Zz4=",
              "image_url": null,
              "latency_ms": 12,
              "cost_units": 0.0
            }
            """.trimIndent()
        )

        assertEquals("job-1", response.request_id)
        assertEquals("image/svg+xml", response.content_type)
        assertEquals("PHN2Zz48L3N2Zz4=", response.image_base64)
        assertEquals(12, response.latency_ms)
        assertEquals(0.0, response.cost_units)
    }

    @Test
    fun `precheck request serializes nested media signal fields as snake case`() {
        val json = objectMapper.writeValueAsString(
            AiWorkerPrecheckRequest(
                submission_id = "submission-1",
                task_title = "Reading",
                task_category = "language",
                child_age = 7,
                media = listOf(
                    AiWorkerMediaSignal(
                        media_id = "media-1",
                        kind = "audio",
                        mime_type = "audio/mpeg",
                        transcript = "read aloud",
                        visual_labels = listOf("book"),
                        duration_seconds = 3.5,
                    )
                ),
                child_note = "done",
            )
        )

        assertTrue("\"submission_id\"" in json)
        assertTrue("\"task_title\"" in json)
        assertTrue("\"task_category\"" in json)
        assertTrue("\"child_age\"" in json)
        assertTrue("\"media_id\"" in json)
        assertTrue("\"mime_type\"" in json)
        assertTrue("\"visual_labels\"" in json)
        assertTrue("\"duration_seconds\"" in json)
        assertTrue("\"child_note\"" in json)
        assertFalse("\"submissionId\"" in json)
        assertFalse("\"mediaId\"" in json)
        assertFalse("\"visualLabels\"" in json)
    }

    @Test
    fun `precheck response deserializes ai worker snake case contract`() {
        val response = objectMapper.readValue<AiWorkerPrecheckResponse>(
            """
            {
              "submission_id": "submission-1",
              "summary": "Looks safe",
              "risk_level": "low",
              "confidence": 0.92,
              "suggested_decision": "approve",
              "checklist": ["complete"],
              "safety_notes": ["none"]
            }
            """.trimIndent()
        )

        assertEquals("submission-1", response.submission_id)
        assertEquals("low", response.risk_level)
        assertEquals("approve", response.suggested_decision)
        assertEquals(listOf("none"), response.safety_notes)
    }
}
