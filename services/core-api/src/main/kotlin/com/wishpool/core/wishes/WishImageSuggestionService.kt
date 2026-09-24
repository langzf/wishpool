package com.wishpool.core.wishes

import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.media.MediaAssetRecord
import com.wishpool.core.media.MediaService
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

@Service
class WishImageSuggestionService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
    private val mediaService: MediaService,
) {
    fun findCandidates(request: WishImageCandidateRequest): WishImageCandidateResponse {
        if (request.title.isBlank()) throw BadRequestError("title cannot be blank.")
        val user = currentUser.require()
        familyPolicy.requireParent(user, request.familyId)
        familyPolicy.requireCanAccessChild(user, request.childId).also {
            if (it.familyId != request.familyId) throw BadRequestError("Child profile does not belong to this family.")
        }

        val query = analyze(request.title, request.note)
        val rows = jdbcClient.sql(
            """
            select
              p.media_asset_id,
              p.family_id,
              p.child_id,
              p.source_wish_id,
              p.title_snapshot,
              p.normalized_title,
              p.keywords,
              p.category,
              p.tags,
              p.created_at as profile_created_at,
              ma.purpose,
              ma.storage_key,
              ma.content_type,
              ma.size_bytes,
              ma.status,
              ma.related_type,
              ma.related_id
            from wish_image_asset_profile p
            join media_asset ma on ma.id = p.media_asset_id
            where p.family_id = :family_id
              and p.reuse_allowed = true
              and ma.purpose = 'wish_image'
              and ma.status in ('uploaded', 'ready')
              and lower(split_part(ma.content_type, ';', 1)) like 'image/%'
            order by p.created_at desc
            limit 80
            """.trimIndent(),
        )
            .param("family_id", request.familyId)
            .query(::profileRow)
            .list()

        val items = rows.asSequence()
            .map { row -> scoredCandidate(row, query, request.childId) }
            .filter { it.score >= 50 }
            .sortedWith(compareByDescending<WishImageCandidate> { it.score }.thenByDescending { it.lastUsedAt })
            .take(request.limit.coerceIn(1, 12))
            .toList()

        return WishImageCandidateResponse(query = query, items = items)
    }

    fun upsertProfileForWish(wish: WishRecord, media: MediaAssetRecord, sourceType: String = "uploaded") {
        if (media.purpose != "wish_image") return
        val query = analyze(wish.title, wish.note)
        jdbcClient.sql(
            """
            insert into wish_image_asset_profile (
              media_asset_id, family_id, child_id, source_type, source_wish_id,
              title_snapshot, note_snapshot, normalized_title, keywords, category, tags, similarity_key
            ) values (
              :media_asset_id, :family_id, :child_id, :source_type, :source_wish_id,
              :title_snapshot, :note_snapshot, :normalized_title, :keywords, :category, :tags, :similarity_key
            )
            on conflict (media_asset_id) do update set
              source_type = excluded.source_type,
              source_wish_id = coalesce(wish_image_asset_profile.source_wish_id, excluded.source_wish_id),
              title_snapshot = excluded.title_snapshot,
              note_snapshot = excluded.note_snapshot,
              normalized_title = excluded.normalized_title,
              keywords = excluded.keywords,
              category = excluded.category,
              tags = excluded.tags,
              similarity_key = excluded.similarity_key,
              updated_at = now()
            """.trimIndent(),
        )
            .param("media_asset_id", media.id)
            .param("family_id", wish.familyId)
            .param("child_id", media.childId)
            .param("source_type", sourceType)
            .param("source_wish_id", wish.id)
            .param("title_snapshot", wish.title)
            .param("note_snapshot", wish.note)
            .param("normalized_title", query.normalizedTitle)
            .param("keywords", query.keywords.toTypedArray())
            .param("category", query.category)
            .param("tags", query.category?.let { arrayOf(it) } ?: emptyArray<String>())
            .param("similarity_key", (listOf(query.normalizedTitle) + query.keywords).distinct().joinToString("|"))
            .update()
    }

    private fun scoredCandidate(row: WishImageProfileRow, query: WishImageCandidateQuery, childId: UUID): WishImageCandidate {
        var score = 0
        val reasons = mutableListOf<String>()
        if (row.childId == childId) {
            score += 20
            reasons += "same child"
        } else {
            reasons += "same family"
        }
        if (row.normalizedTitle == query.normalizedTitle) {
            score += 50
            reasons += "same normalized title"
        }

        val keywordScore = overlapScore(query.keywords.toSet(), row.keywords.toSet(), 30)
        if (keywordScore > 0) {
            score += keywordScore
            reasons += "keyword overlap"
        }
        val rowCategory = row.category ?: inferCategory(row.keywords + listOf(row.normalizedTitle))
        val tagScore = overlapScore(listOfNotNull(query.category).toSet(), (row.tags + listOfNotNull(rowCategory)).toSet(), 25)
        if (tagScore > 0) {
            score += tagScore
            reasons += "category match"
        }

        val ageDays = ChronoUnit.DAYS.between(row.createdAt, OffsetDateTime.now()).coerceAtLeast(0)
        when {
            ageDays <= 180 -> {
                score += 10
                reasons += "recent"
            }
            ageDays > 730 -> score -= 10
        }

        return WishImageCandidate(
            media = mediaService.toResponse(row.media),
            score = score.coerceIn(0, 100),
            reason = reasons.distinct().joinToString(", "),
            sourceWishId = row.sourceWishId,
            sourceWishTitle = row.titleSnapshot,
            lastUsedAt = row.createdAt,
        )
    }

    private fun overlapScore(left: Set<String>, right: Set<String>, max: Int): Int {
        if (left.isEmpty() || right.isEmpty()) return 0
        val intersection = left.intersect(right).size
        val union = left.union(right).size
        return ((intersection.toDouble() / union.toDouble()) * max).roundToInt()
    }

    private fun analyze(title: String, note: String?): WishImageCandidateQuery {
        return analyzeText(title, note)
    }

    internal companion object {
        fun analyzeText(title: String, note: String?): WishImageCandidateQuery {
            val normalized = normalize(title)
            val words = (tokenize(title) + tokenize(note.orEmpty())).distinct().take(12)
            val category = inferCategory(words + listOf(normalized))
            return WishImageCandidateQuery(
                normalizedTitle = normalized,
                keywords = words,
                category = category,
            )
        }

        private fun normalize(value: String): String =
            value.lowercase(Locale.ROOT)
                .replace(Regex("[\\p{Punct}\\p{P}\\s]+"), "")
                .trim()

        private fun tokenize(value: String): List<String> =
            value.lowercase(Locale.ROOT)
                .split(Regex("[\\p{Punct}\\p{P}\\s]+"))
                .flatMap(::tokenizeSegment)
                .filter { it.length >= 2 }

        private fun tokenizeSegment(segment: String): List<String> {
            val trimmed = segment.trim()
            if (trimmed.isBlank()) return emptyList()
            if (!trimmed.any(::isCjk)) return listOf(trimmed)

            val tokens = linkedSetOf<String>()
            Regex("[a-z0-9]+").findAll(trimmed)
                .map { it.value }
                .filter { it.length >= 2 }
                .forEach(tokens::add)

            contiguousCjkRuns(trimmed).forEach { cjkText ->
                chineseKeywordTerms
                    .filter { cjkText.contains(it) }
                    .forEach(tokens::add)
                cjkText
                    .windowed(size = 2, step = 1, partialWindows = false)
                    .forEach(tokens::add)
                if (tokens.isEmpty() && cjkText.length >= 2) tokens += cjkText
            }
            return tokens.toList()
        }

        private fun contiguousCjkRuns(value: String): List<String> {
            val runs = mutableListOf<String>()
            val current = StringBuilder()
            value.forEach { char ->
                if (isCjk(char)) {
                    current.append(char)
                } else if (current.isNotEmpty()) {
                    runs += current.toString()
                    current.clear()
                }
            }
            if (current.isNotEmpty()) runs += current.toString()
            return runs
        }

        fun inferCategory(tokens: List<String>): String? {
            val text = tokens.joinToString(" ")
            return categoryKeywords.firstOrNull { (_, keywords) -> keywords.any(text::contains) }?.first
        }

        private fun isCjk(char: Char): Boolean =
            Character.UnicodeBlock.of(char) in cjkBlocks

        private val cjkBlocks = setOf(
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
            Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
        )

        private val categoryKeywords = listOf(
            "book" to listOf("book", "read", "story", "library", "书", "故事", "阅读", "图书", "绘本"),
            "science" to listOf("science", "microscope", "telescope", "experiment", "robot", "科学", "显微镜", "望远镜", "实验", "机器人"),
            "trip" to listOf("park", "trip", "camp", "museum", "zoo", "公园", "旅行", "露营", "博物馆", "动物园"),
            "sport" to listOf("ball", "bike", "swim", "sport", "skate", "球", "自行车", "游泳", "运动", "滑板"),
            "art" to listOf("paint", "draw", "music", "craft", "lego", "blocks", "画画", "绘画", "音乐", "手工", "乐高", "积木"),
            "toy" to listOf("toy", "game", "doll", "puzzle", "玩具", "游戏", "娃娃", "拼图"),
            "food" to listOf("cake", "picnic", "restaurant", "food", "蛋糕", "野餐", "餐厅", "食物", "美食"),
        )

        private val chineseKeywordTerms = categoryKeywords
            .flatMap { (_, keywords) -> keywords }
            .filter { keyword -> keyword.any { Character.UnicodeBlock.of(it) in cjkBlocks } }
            .distinct()
    }
}

private data class WishImageProfileRow(
    val media: MediaAssetRecord,
    val childId: UUID?,
    val sourceWishId: UUID?,
    val titleSnapshot: String,
    val normalizedTitle: String,
    val keywords: List<String>,
    val category: String?,
    val tags: List<String>,
    val createdAt: OffsetDateTime,
)

private fun profileRow(rs: ResultSet, rowNum: Int): WishImageProfileRow =
    WishImageProfileRow(
        media = MediaAssetRecord(
            id = rs.getObject("media_asset_id", UUID::class.java),
            familyId = rs.getObject("family_id", UUID::class.java),
            childId = rs.getObject("child_id", UUID::class.java),
            purpose = rs.getString("purpose"),
            storageKey = rs.getString("storage_key"),
            contentType = rs.getString("content_type"),
            sizeBytes = rs.getLong("size_bytes").takeUnless { rs.wasNull() },
            status = rs.getString("status"),
            relatedType = rs.getString("related_type"),
            relatedId = rs.getObject("related_id", UUID::class.java),
        ),
        childId = rs.getObject("child_id", UUID::class.java),
        sourceWishId = rs.getObject("source_wish_id", UUID::class.java),
        titleSnapshot = rs.getString("title_snapshot"),
        normalizedTitle = rs.getString("normalized_title"),
        keywords = (rs.getArray("keywords")?.array as? Array<*>)?.filterIsInstance<String>().orEmpty(),
        category = rs.getString("category"),
        tags = (rs.getArray("tags")?.array as? Array<*>)?.filterIsInstance<String>().orEmpty(),
        createdAt = rs.getObject("profile_created_at", OffsetDateTime::class.java),
    )
