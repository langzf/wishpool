package com.wishpool.core.wishes

import org.junit.jupiter.api.Test
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WishImageSuggestionServiceTests {
    @Test
    fun `chinese similar title produces keyword and category overlap`() {
        val profile = analyze("显微镜", "带图测试")
        val query = analyze("儿童显微镜", null)

        assertTrue("显微镜" in profile.keywords)
        assertTrue("显微" in profile.keywords)
        assertTrue("显微镜" in query.keywords)
        assertTrue("显微" in query.keywords)
        assertEquals("science", profile.category)
        assertEquals("science", query.category)

        val score = 20 +
            overlapScore(query.keywords.toSet(), profile.keywords.toSet(), 30) +
            overlapScore(setOfNotNull(query.category), setOfNotNull(profile.category), 25) +
            10
        assertTrue(score >= 50)

        val legacyProfileKeywords = setOf("显微镜", "带图测试")
        val inferredLegacyCategory = WishImageSuggestionService.inferCategory(legacyProfileKeywords.toList() + "显微镜")
        val legacyScore = 20 +
            overlapScore(query.keywords.toSet(), legacyProfileKeywords, 30) +
            overlapScore(setOfNotNull(query.category), setOfNotNull(inferredLegacyCategory), 25) +
            10
        assertTrue(legacyScore >= 50)
    }

    @Test
    fun `english title tokenizes and matches existing keywords`() {
        val profile = analyze("Microscope", null)
        val query = analyze("Kids microscope science kit", null)

        assertTrue("microscope" in profile.keywords)
        assertTrue("microscope" in query.keywords)
        assertEquals("science", query.category)
        assertTrue(profile.keywords.toSet().intersect(query.keywords.toSet()).isNotEmpty())
    }

    @Test
    fun `exact title keeps normalized title match`() {
        val profile = analyze("显微镜", null)
        val query = analyze("显微镜", null)

        assertEquals("显微镜", profile.normalizedTitle)
        assertEquals(profile.normalizedTitle, query.normalizedTitle)
    }

    @Test
    fun `unrelated title remains below candidate threshold`() {
        val profile = analyze("显微镜", null)
        val query = analyze("儿童跑鞋", null)

        val score = 20 +
            overlapScore(query.keywords.toSet(), profile.keywords.toSet(), 30) +
            overlapScore(setOfNotNull(query.category), setOfNotNull(profile.category), 25) +
            10

        assertTrue(profile.keywords.toSet().intersect(query.keywords.toSet()).isEmpty())
        assertTrue(score < 50)
    }

    private fun analyze(title: String, note: String?): WishImageCandidateQuery =
        WishImageSuggestionService.analyzeText(title, note)

    private fun overlapScore(left: Set<String>, right: Set<String>, max: Int): Int {
        if (left.isEmpty() || right.isEmpty()) return 0
        val intersection = left.intersect(right).size
        val union = left.union(right).size
        return ((intersection.toDouble() / union.toDouble()) * max).roundToInt()
    }
}
