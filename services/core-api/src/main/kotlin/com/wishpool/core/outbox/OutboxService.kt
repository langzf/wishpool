package com.wishpool.core.outbox

import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.NotFoundError
import com.wishpool.core.shared.ArchiveFailureError
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.slf4j.LoggerFactory
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import java.util.UUID
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.channels.FileChannel
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.GZIPOutputStream

@Service
class OutboxService(
    private val jdbcClient: JdbcClient,
    private val objectMapper: ObjectMapper,
    transactionManager: org.springframework.transaction.PlatformTransactionManager,
    @Value("\${wishpool.outbox.default-lease-seconds}") private val defaultLeaseSeconds: Long,
    @Value("\${wishpool.outbox.retention-days}") retentionDaysValue: String,
    @Value("\${wishpool.outbox.archive-dir}") private val archiveDir: String,
) {
    private val transactions = TransactionTemplate(transactionManager)
    private val retentionDays = retentionDaysValue.toLongOrNull()?.coerceIn(1, 3650) ?: run {
        logger.warn("WISHPOOL_OUTBOX_RETENTION_DAYS 非法，回落到 30 天: {}", retentionDaysValue)
        30
    }

    fun archive(dryRun: Boolean, batchSize: Int = 5000, maxBatches: Int = 20): OutboxArchiveResponse {
        if (batchSize !in 100..20000) throw BadRequestError("batchSize 必须在 100 到 20000 之间。")
        if (maxBatches !in 1..500) throw BadRequestError("maxBatches 必须在 1 到 500 之间。")
        val started = System.nanoTime()
        if (dryRun) {
            val count = transactions.execute {
                jdbcClient.sql("select count(*) from outbox_event where published_at is not null and published_at < now() - (:days * interval '1 day')")
                    .param("days", retentionDays).query(Long::class.java).single()
            } ?: 0L
            return result(count.toInt(), 0, 0, started, emptyList(), 0, count == 0L, true)
        }
        var candidateRows = 0; var archivedRows = 0; var deletedRows = 0; var batches = 0; var drained = false
        val paths = linkedSetOf<String>()
        while (batches < maxBatches) {
            val result = transactions.execute {
                jdbcClient.sql("set local statement_timeout = '60s'").update()
                jdbcClient.sql("set local lock_timeout = '60s'").update()
                jdbcClient.sql("select pg_advisory_xact_lock(hashtext('wishpool:outbox:archive'))").query { _, _ -> 1 }.list()
                val rows = selectArchiveRows(batchSize)
                if (rows.isEmpty()) rows to 0 else writeAndDeleteBatchV2(rows, paths)
            } ?: error("归档批事务未返回结果")
            candidateRows += result.first.size
            if (result.first.isEmpty()) { drained = true; break }
            batches++
            archivedRows += result.second; deletedRows += result.second
            if (dryRun) break
        }
        if (!dryRun && batches == maxBatches) drained = selectArchiveRows(1).isEmpty()
        return result(candidateRows, archivedRows, deletedRows, started, paths.toList(), batches, drained, dryRun)
    }

    private fun selectArchiveRows(limit: Int): List<ArchiveRow> = jdbcClient.sql(
            """select id, event_type, aggregate_type, aggregate_id, payload_json::text, available_at,
                      published_at, retry_count, created_at, leased_until, last_error
               from outbox_event
               where published_at is not null and published_at < now() - (:days * interval '1 day')
               order by published_at, id limit cast(:limit as integer) for update""").param("days", retentionDays).param("limit", limit).query { rs, _ ->
            ArchiveRow(rs.getObject("id", UUID::class.java), rs.getString("event_type"), rs.getString("aggregate_type"),
                rs.getObject("aggregate_id", UUID::class.java), rs.getString("payload_json"), rs.getObject("available_at", OffsetDateTime::class.java),
                rs.getObject("published_at", OffsetDateTime::class.java), rs.getInt("retry_count"), rs.getObject("created_at", OffsetDateTime::class.java),
                rs.getObject("leased_until", OffsetDateTime::class.java), rs.getString("last_error"))
        }.list()

    private fun writeAndDeleteBatchV2(rows: List<ArchiveRow>, paths: MutableSet<String>): Pair<List<ArchiveRow>, Int> {
        require(rows.isNotEmpty())
        val dir = Path.of(archiveDir)
        Files.createDirectories(dir)
        val stamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS'Z'").withZone(java.time.ZoneOffset.UTC).format(java.time.Instant.now())
        val finalPath = dir.resolve("outbox-$stamp-part-${partSequence.incrementAndGet()}.ndjson.gz")
        val tempPath = dir.resolve(".${finalPath.fileName}.tmp")
        try {
            var written = 0
            FileChannel.open(tempPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE).use { channel ->
                GZIPOutputStream(java.nio.channels.Channels.newOutputStream(channel)).bufferedWriter(StandardCharsets.UTF_8).use { out ->
                    rows.forEach { out.appendLine(toArchiveJson(it)); written++ }
                }
            }
            FileChannel.open(tempPath, StandardOpenOption.READ).use { it.force(true) }
            check(written == rows.size) { "archive row count $written != selected ${rows.size}" }
            check(countGzipLines(tempPath) == rows.size) { "archive verification failed for $tempPath" }
            Files.move(tempPath, finalPath, StandardCopyOption.ATOMIC_MOVE)
            FileChannel.open(dir, StandardOpenOption.READ).use { it.force(true) }
            paths.add(finalPath.toString())
            val deleted = jdbcClient.sql("""
                delete from outbox_event o
                using (select id from outbox_event
                       where published_at is not null
                         and published_at < now() - (:days * interval '1 day')
                       order by published_at, id
                       limit cast(:limit as integer)) t
                where o.id = t.id and o.published_at is not null
                returning o.id
            """).param("days", retentionDays).param("limit", rows.size).query { _, _ -> 1 }.list().size
            if (deleted != rows.size) error("archive row count ${rows.size} != deleted $deleted; transaction rolled back")
            return rows to deleted
        } catch (ex: Exception) {
            Files.deleteIfExists(tempPath)
            logger.error("归档批次失败", ex)
            throw if (ex is RuntimeException) ex else ArchiveFailureError("归档失败: ${ex.javaClass.simpleName}: ${ex.message}", ex)
        }
    }

    private fun countGzipLines(path: Path): Int = java.util.zip.GZIPInputStream(Files.newInputStream(path)).bufferedReader(StandardCharsets.UTF_8).use { it.lineSequence().count() }

    private fun result(c: Int, a: Int, d: Int, started: Long, paths: List<String>, batches: Int, drained: Boolean, dry: Boolean) = OutboxArchiveResponse(c, a, d, (System.nanoTime()-started)/1_000_000, paths.firstOrNull(), paths, batches, drained, dry)

    /* legacy implementation intentionally removed: batches are committed independently. */
    /*
        val grouped = rows.groupBy { it.createdAt.toLocalDate().withDayOfMonth(1).toString().substring(0, 7) }
        val paths = grouped.keys.map { Path.of(archiveDir).resolve("outbox-$it.ndjson.gz") }
        if (dryRun) return result(rows.size, 0, 0, started, paths, true)
        Files.createDirectories(Path.of(archiveDir))
        var archived = 0
        grouped.forEach { (month, monthRows) ->
            val target = Path.of(archiveDir).resolve("outbox-$month.ndjson.gz")
            if (Files.exists(target)) {
                val existing = countGzipLines(target)
                if (existing != monthRows.size) error("归档文件 $target 行数 $existing 与候选 ${monthRows.size} 不一致")
            } else {
                val temp = target.resolveSibling(".${target.fileName}.tmp-${UUID.randomUUID()}")
                GZIPOutputStream(Files.newOutputStream(temp)).bufferedWriter(StandardCharsets.UTF_8).use { out ->
                    monthRows.forEach { out.appendLine(toArchiveJson(it)) }
                }
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE)
            }
            archived += monthRows.size
        }
        if (deleted != archived) error("归档行数 $archived 与删除行数 $deleted 不一致，事务已回滚")
        return result(rows.size, archived, deleted, started, paths, false)
    }
    */
    private fun toArchiveJson(r: ArchiveRow): String {
        val n = objectMapper.createObjectNode()
        n.put("id", r.id.toString()); n.put("event_type", r.eventType); n.put("aggregate_type", r.aggregateType); n.put("aggregate_id", r.aggregateId.toString()); n.set("payload_json", objectMapper.readTree(r.payload)); n.put("available_at", r.availableAt.toString()); n.put("published_at", r.publishedAt.toString()); n.put("retry_count", r.retryCount); n.put("created_at", r.createdAt.toString()); r.leasedUntil?.let { n.put("leased_until", it.toString()) } ?: n.putNull("leased_until"); r.lastError?.let { n.put("last_error", it) } ?: n.putNull("last_error")
        return objectMapper.writeValueAsString(n)
    }

    private data class ArchiveRow(val id: UUID, val eventType: String, val aggregateType: String, val aggregateId: UUID, val payload: String, val availableAt: OffsetDateTime, val publishedAt: OffsetDateTime, val retryCount: Int, val createdAt: OffsetDateTime, val leasedUntil: OffsetDateTime?, val lastError: String?)
    @Transactional
    fun claim(request: ClaimOutboxEventsRequest): OutboxClaimResponse {
        val limit = request.limit.coerceIn(1, MAX_LIMIT)
        val leaseSeconds = (request.leaseSeconds ?: defaultLeaseSeconds).coerceIn(MIN_LEASE_SECONDS, MAX_LEASE_SECONDS)
        val events = jdbcClient.sql(
            """
            with claimable as (
              select id
              from outbox_event
              where published_at is null
                and available_at <= now()
                and (leased_until is null or leased_until <= now())
              order by available_at, created_at
              limit cast(:limit as integer)
              for update skip locked
            )
            update outbox_event e
            set leased_until = now() + (:lease_seconds * interval '1 second'),
                retry_count = retry_count + 1,
                last_error = null
            from claimable
            where e.id = claimable.id
            returning e.id, e.event_type, e.aggregate_type, e.aggregate_id, e.payload_json::text,
                      e.available_at, e.leased_until, e.retry_count, e.created_at
            """.trimIndent(),
        )
            .param("limit", limit)
            .param("lease_seconds", leaseSeconds)
            .query(::outboxEventResponse)
            .list()
        return OutboxClaimResponse(events)
    }

    @Transactional
    fun markPublished(eventId: UUID): OutboxAckResponse {
        val published = jdbcClient.sql(
            """
            update outbox_event
            set published_at = coalesce(published_at, now()),
                leased_until = null,
                last_error = null
            where id = :id
            returning id, published_at
            """.trimIndent(),
        )
            .param("id", eventId)
            .query { rs, _ ->
                OutboxAckResponse(
                    id = rs.getObject("id", UUID::class.java),
                    status = "published",
                    publishedAt = rs.getObject("published_at", OffsetDateTime::class.java),
                )
            }
            .optional()
            .orElseThrow { NotFoundError("Outbox event not found.") }
        return published
    }

    @Transactional
    fun scheduleRetry(eventId: UUID, request: RetryOutboxEventRequest): OutboxEventResponse {
        if (request.delaySeconds < 0) throw BadRequestError("delaySeconds must be greater than or equal to 0.")
        return jdbcClient.sql(
            """
            update outbox_event
            set available_at = now() + (:delay_seconds * interval '1 second'),
                leased_until = null,
                last_error = :reason
            where id = :id
              and published_at is null
            returning id, event_type, aggregate_type, aggregate_id, payload_json::text,
                      available_at, leased_until, retry_count, created_at
            """.trimIndent(),
        )
            .param("id", eventId)
            .param("delay_seconds", request.delaySeconds.coerceAtMost(MAX_RETRY_DELAY_SECONDS))
            .param("reason", request.reason?.trim())
            .query(::outboxEventResponse)
            .optional()
            .orElseThrow { NotFoundError("Unpublished outbox event not found.") }
    }

    private fun outboxEventResponse(rs: java.sql.ResultSet, rowNum: Int): OutboxEventResponse =
        OutboxEventResponse(
            id = rs.getObject("id", UUID::class.java),
            type = rs.getString("event_type"),
            aggregateType = rs.getString("aggregate_type"),
            aggregateId = rs.getObject("aggregate_id", UUID::class.java),
            payload = objectMapper.readTree(rs.getString("payload_json")),
            availableAt = rs.getObject("available_at", OffsetDateTime::class.java),
            leasedUntil = rs.getObject("leased_until", OffsetDateTime::class.java),
            retryCount = rs.getInt("retry_count"),
            createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
        )

    private companion object {
        val logger = LoggerFactory.getLogger(OutboxService::class.java)
        const val MAX_LIMIT = 500
        const val MIN_LEASE_SECONDS = 5L
        const val MAX_LEASE_SECONDS = 900L
        const val MAX_RETRY_DELAY_SECONDS = 86_400L
        val partSequence = AtomicLong(0)
    }
}
