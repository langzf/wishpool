package com.wishpool.core.rewards

import com.wishpool.core.events.DomainEventPublisher
import com.wishpool.core.shared.BadRequestError
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class RewardService(
    private val jdbcClient: JdbcClient,
    private val eventPublisher: DomainEventPublisher,
) {
    private val ZERO_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    fun evaluateAfterTaskApproved(taskId: UUID, reviewId: UUID, createdBy: UUID) {
        val task = taskRewardContext(taskId) ?: return
        grantStarLight(task, reviewId, createdBy)
        refreshDailySummaryAndWish(task.familyId, task.childId, task.scheduledDate, createdBy)
    }

    fun evaluateAfterTaskSkipped(taskId: UUID, createdBy: UUID) {
        val task = taskRewardContext(taskId) ?: return
        refreshDailySummaryAndWish(task.familyId, task.childId, task.scheduledDate, createdBy)
    }

    fun adjustAfterApprovedReviewRevoked(taskId: UUID, reviewId: UUID, createdBy: UUID) {
        val task = taskRewardContext(taskId) ?: return
        adjustStarLight(task, reviewId, createdBy)
        refreshDailySummaryAndWish(task.familyId, task.childId, task.scheduledDate, createdBy)
    }

    @Transactional
    fun evaluateWorkflowEvent(eventType: String, taskId: UUID, reviewId: UUID?, actorUserId: UUID) {
        val effectiveActorUserId = effectiveActorUserId(eventType, taskId, reviewId, actorUserId)
        when (eventType) {
            "review.approved" -> evaluateAfterTaskApproved(
                taskId = taskId,
                reviewId = reviewId ?: throw BadRequestError("reviewId is required for review.approved."),
                createdBy = effectiveActorUserId,
            )
            "task.skipped" -> evaluateAfterTaskSkipped(taskId = taskId, createdBy = effectiveActorUserId)
            "review.revoked" -> adjustAfterApprovedReviewRevoked(
                taskId = taskId,
                reviewId = reviewId ?: throw BadRequestError("reviewId is required for review.revoked."),
                createdBy = effectiveActorUserId,
            )
            else -> throw BadRequestError("Unsupported reward workflow event type.")
        }
    }

    private fun effectiveActorUserId(
        eventType: String,
        taskId: UUID,
        reviewId: UUID?,
        actorUserId: UUID,
    ): UUID {
        if (actorUserId != ZERO_UUID) return actorUserId
        if (eventType in setOf("review.approved", "review.revoked") && reviewId != null) {
            return jdbcClient.sql("select reviewed_by from review where id = :id")
                .param("id", reviewId)
                .query(UUID::class.java)
                .optional()
                .orElseThrow { BadRequestError("reviewId does not reference an existing review.") }
        }
        return jdbcClient.sql(
            """
            select f.owner_user_id
            from task_instance ti
            join family f on f.id = ti.family_id
            where ti.id = :task_id
            """.trimIndent(),
        )
            .param("task_id", taskId)
            .query(UUID::class.java)
            .optional()
            .orElseThrow { BadRequestError("taskInstanceId does not reference an existing task.") }
    }

    private fun grantStarLight(task: TaskRewardContext, reviewId: UUID, createdBy: UUID) {
        val rewardId = insertReward(
            familyId = task.familyId,
            childId = task.childId,
            weekId = task.weekId,
            date = task.scheduledDate,
            rewardType = "star_light",
            reason = "task_approved",
            amount = 1,
            wishId = null,
            taskInstanceId = task.id,
            reviewId = reviewId,
            sourceRewardId = null,
            idempotencyKey = "star_light:task_approved:${task.id}:$reviewId",
            createdBy = createdBy,
        ) ?: return

        eventPublisher.publishFamilyEvent(
            familyId = task.familyId,
            eventType = "reward.star_light_granted",
            aggregateType = "reward_ledger",
            aggregateId = rewardId,
            payloadJson = """
            {
              "rewardId": "$rewardId",
              "familyId": "${task.familyId}",
              "childId": "${task.childId}",
              "taskInstanceId": "${task.id}",
              "reviewId": "$reviewId",
              "amount": 1,
              "reason": "task_approved"
            }
            """.trimIndent(),
        )
    }

    private fun adjustStarLight(task: TaskRewardContext, reviewId: UUID, createdBy: UUID) {
        val sourceRewardId = jdbcClient.sql(
            """
            select id
            from reward_ledger
            where reward_type = 'star_light'
              and reason = 'task_approved'
              and task_instance_id = :task_id
              and review_id = :review_id
              and amount > 0
            order by created_at
            limit 1
            """.trimIndent(),
        )
            .param("task_id", task.id)
            .param("review_id", reviewId)
            .query(UUID::class.java)
            .optional()
            .orElse(null) ?: return

        val rewardId = insertReward(
            familyId = task.familyId,
            childId = task.childId,
            weekId = task.weekId,
            date = task.scheduledDate,
            rewardType = "adjustment",
            reason = "review_revoked",
            amount = -1,
            wishId = null,
            taskInstanceId = task.id,
            reviewId = reviewId,
            sourceRewardId = sourceRewardId,
            idempotencyKey = "adjustment:review_revoked:$reviewId",
            createdBy = createdBy,
        ) ?: return

        eventPublisher.publishFamilyEvent(
            familyId = task.familyId,
            eventType = "reward.adjusted",
            aggregateType = "reward_ledger",
            aggregateId = rewardId,
            payloadJson = """
            {
              "rewardId": "$rewardId",
              "sourceRewardId": "$sourceRewardId",
              "familyId": "${task.familyId}",
              "childId": "${task.childId}",
              "taskInstanceId": "${task.id}",
              "reviewId": "$reviewId",
              "amount": -1,
              "reason": "review_revoked"
            }
            """.trimIndent(),
        )
    }

    private fun refreshDailySummaryAndWish(familyId: UUID, childId: UUID, date: LocalDate, createdBy: UUID) {
        val aggregate = aggregateDay(familyId, childId, date)
        val fragmentStatus = fragmentStatus(aggregate)
        val existingFragmentRewardId = currentFragmentRewardId(familyId, childId, date)
        val activeWish = activeWish(familyId, childId, aggregate.weekId)
        val shouldGrantFragment = fragmentStatus == "earned" && existingFragmentRewardId == null && activeWish != null

        val fragmentRewardId = if (shouldGrantFragment) {
            grantWishFragment(aggregate, activeWish, createdBy)
        } else {
            existingFragmentRewardId
        }
        val effectiveFragmentRewardId = if (fragmentStatus == "earned") {
            fragmentRewardId
        } else {
            existingFragmentRewardId?.let { adjustWishFragment(aggregate, it, createdBy) }
            null
        }

        upsertDailySummary(aggregate, fragmentStatus, effectiveFragmentRewardId)
        activeWish?.let { refreshWishProgress(it.id) }
        if (activeWish == null && existingFragmentRewardId != null) {
            wishIdForReward(existingFragmentRewardId)?.let(::refreshWishProgress)
        }
    }

    private fun fragmentStatus(aggregate: DailyTaskAggregate): String =
        when {
            aggregate.coreRequired == 0 -> "not_eligible"
            aggregate.coreApproved + aggregate.coreSkipped >= aggregate.coreRequired -> "earned"
            else -> "not_earned"
        }

    private fun grantWishFragment(aggregate: DailyTaskAggregate, wish: ActiveWishRecord, createdBy: UUID): UUID? {
        val rewardId = insertReward(
            familyId = aggregate.familyId,
            childId = aggregate.childId,
            weekId = aggregate.weekId,
            date = aggregate.date,
            rewardType = "wish_fragment",
            reason = "daily_core_completed",
            amount = 1,
            wishId = wish.id,
            taskInstanceId = null,
            reviewId = null,
            sourceRewardId = null,
            idempotencyKey = "wish_fragment:${aggregate.familyId}:${aggregate.childId}:${aggregate.weekId}:${aggregate.date}",
            createdBy = createdBy,
        ) ?: return null

        eventPublisher.publishFamilyEvent(
            familyId = aggregate.familyId,
            eventType = "reward.wish_fragment_granted",
            aggregateType = "reward_ledger",
            aggregateId = rewardId,
            payloadJson = """
            {
              "rewardId": "$rewardId",
              "familyId": "${aggregate.familyId}",
              "childId": "${aggregate.childId}",
              "wishId": "${wish.id}",
              "date": "${aggregate.date}",
              "weekId": "${aggregate.weekId}",
              "amount": 1
            }
            """.trimIndent(),
        )
        return rewardId
    }

    private fun adjustWishFragment(aggregate: DailyTaskAggregate, sourceRewardId: UUID, createdBy: UUID): UUID? {
        val source = jdbcClient.sql(
            """
            select id, wish_id
            from reward_ledger
            where id = :id
              and reward_type = 'wish_fragment'
              and amount > 0
            """.trimIndent(),
        )
            .param("id", sourceRewardId)
            .query { rs, _ ->
                SourceFragmentReward(
                    id = rs.getObject("id", UUID::class.java),
                    wishId = rs.getObject("wish_id", UUID::class.java),
                )
            }
            .optional()
            .orElse(null) ?: return null

        val rewardId = insertReward(
            familyId = aggregate.familyId,
            childId = aggregate.childId,
            weekId = aggregate.weekId,
            date = aggregate.date,
            rewardType = "adjustment",
            reason = "daily_core_no_longer_completed",
            amount = -1,
            wishId = source.wishId,
            taskInstanceId = null,
            reviewId = null,
            sourceRewardId = source.id,
            idempotencyKey = "adjustment:daily_core_no_longer_completed:${source.id}",
            createdBy = createdBy,
        ) ?: return null

        eventPublisher.publishFamilyEvent(
            familyId = aggregate.familyId,
            eventType = "reward.adjusted",
            aggregateType = "reward_ledger",
            aggregateId = rewardId,
            payloadJson = """
            {
              "rewardId": "$rewardId",
              "sourceRewardId": "${source.id}",
              "familyId": "${aggregate.familyId}",
              "childId": "${aggregate.childId}",
              "wishId": "${source.wishId}",
              "date": "${aggregate.date}",
              "amount": -1,
              "reason": "daily_core_no_longer_completed"
            }
            """.trimIndent(),
        )
        return rewardId
    }

    private fun refreshWishProgress(wishId: UUID) {
        val beforeStatus = jdbcClient.sql("select status from wish where id = :id")
            .param("id", wishId)
            .query(String::class.java)
            .single()

        val wish = jdbcClient.sql(
            """
            with progress as (
              select greatest(0, coalesce(sum(amount), 0)::int) as earned
              from reward_ledger
              where wish_id = :wish_id
                and reward_type in ('wish_fragment', 'adjustment')
            ),
            lit_state as (
              select jsonb_build_object(
                'version', 1,
                'litIndexes', coalesce(
                  (
                    select jsonb_agg(value::int order by ordinality)
                    from jsonb_array_elements_text(coalesce(w.fragment_mask_json -> 'revealOrder', '[]'::jsonb))
                      with ordinality as ordered(value, ordinality)
                    where ordinality <= least((select earned from progress), w.required_fragments)
                  ),
                  (
                    select coalesce(jsonb_agg(index_value order by index_value), '[]'::jsonb)
                    from generate_series(0, least((select earned from progress), w.required_fragments) - 1) as index_value
                  )
                )
              ) as fragment_lit_json
              from wish w
              where w.id = :wish_id
            )
            update wish
            set earned_fragments = (select earned from progress),
                fragment_lit_json = (select fragment_lit_json from lit_state),
                status = case
                  when status in ('active', 'unlocked') and (select earned from progress) >= required_fragments then 'unlocked'
                  when status = 'unlocked' and (select earned from progress) < required_fragments then 'active'
                  else status
                end,
                unlocked_at = case
                  when unlocked_at is null and status = 'active' and (select earned from progress) >= required_fragments then now()
                  else unlocked_at
                end,
                updated_at = now()
            where id = :wish_id
            returning id, family_id, child_id, earned_fragments, required_fragments, status
            """.trimIndent(),
        )
            .param("wish_id", wishId)
            .query { rs, _ ->
                WishProgress(
                    id = rs.getObject("id", UUID::class.java),
                    familyId = rs.getObject("family_id", UUID::class.java),
                    childId = rs.getObject("child_id", UUID::class.java),
                    earnedFragments = rs.getInt("earned_fragments"),
                    requiredFragments = rs.getInt("required_fragments"),
                    status = rs.getString("status"),
                )
            }
            .single()

        if (beforeStatus == "active" && wish.status == "unlocked" && wish.earnedFragments >= wish.requiredFragments) {
            eventPublisher.publishFamilyEvent(
                familyId = wish.familyId,
                eventType = "wish.unlocked",
                aggregateType = "wish",
                aggregateId = wish.id,
                payloadJson = """
                {
                  "wishId": "${wish.id}",
                  "familyId": "${wish.familyId}",
                  "childId": "${wish.childId}",
                  "earnedFragments": ${wish.earnedFragments},
                  "requiredFragments": ${wish.requiredFragments}
                }
                """.trimIndent(),
            )
        }
    }

    private fun upsertDailySummary(aggregate: DailyTaskAggregate, fragmentStatus: String, fragmentRewardId: UUID?) {
        jdbcClient.sql(
            """
            insert into daily_summary (
              family_id, child_id, date, week_id, core_required, core_approved, core_skipped,
              total_tasks, approved_tasks, fragment_status, fragment_reward_id, updated_at
            ) values (
              :family_id, :child_id, :date, :week_id, :core_required, :core_approved, :core_skipped,
              :total_tasks, :approved_tasks, :fragment_status, :fragment_reward_id, now()
            )
            on conflict (family_id, child_id, date)
            do update set
              week_id = excluded.week_id,
              core_required = excluded.core_required,
              core_approved = excluded.core_approved,
              core_skipped = excluded.core_skipped,
              total_tasks = excluded.total_tasks,
              approved_tasks = excluded.approved_tasks,
              fragment_status = excluded.fragment_status,
              fragment_reward_id = excluded.fragment_reward_id,
              updated_at = now()
            """.trimIndent(),
        )
            .param("family_id", aggregate.familyId)
            .param("child_id", aggregate.childId)
            .param("date", aggregate.date)
            .param("week_id", aggregate.weekId)
            .param("core_required", aggregate.coreRequired)
            .param("core_approved", aggregate.coreApproved)
            .param("core_skipped", aggregate.coreSkipped)
            .param("total_tasks", aggregate.totalTasks)
            .param("approved_tasks", aggregate.approvedTasks)
            .param("fragment_status", fragmentStatus)
            .param("fragment_reward_id", fragmentRewardId)
            .update()
    }

    private fun aggregateDay(familyId: UUID, childId: UUID, date: LocalDate): DailyTaskAggregate =
        jdbcClient.sql(
            """
            select
              coalesce(max(wp.week_id), to_char(cast(:date as date), 'IYYY-"W"IW')) as week_id,
              count(*) filter (where ti.is_core) as core_required,
              count(*) filter (where ti.is_core and ti.status = 'approved') as core_approved,
              count(*) filter (where ti.is_core and ti.status = 'skipped') as core_skipped,
              count(*) as total_tasks,
              count(*) filter (where ti.status = 'approved') as approved_tasks
            from task_instance ti
            left join weekly_plan wp on wp.id = ti.weekly_plan_id
            where ti.family_id = :family_id
              and ti.child_id = :child_id
              and ti.scheduled_date = :date
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("child_id", childId)
            .param("date", date)
            .query { rs, _ ->
                DailyTaskAggregate(
                    familyId = familyId,
                    childId = childId,
                    date = date,
                    weekId = rs.getString("week_id"),
                    coreRequired = rs.getInt("core_required"),
                    coreApproved = rs.getInt("core_approved"),
                    coreSkipped = rs.getInt("core_skipped"),
                    totalTasks = rs.getInt("total_tasks"),
                    approvedTasks = rs.getInt("approved_tasks"),
                )
            }
            .single()

    private fun taskRewardContext(taskId: UUID): TaskRewardContext? =
        jdbcClient.sql(
            """
            select ti.id, ti.family_id, ti.child_id, ti.scheduled_date, coalesce(wp.week_id, to_char(ti.scheduled_date, 'IYYY-"W"IW')) as week_id
            from task_instance ti
            left join weekly_plan wp on wp.id = ti.weekly_plan_id
            where ti.id = :id
            """.trimIndent(),
        )
            .param("id", taskId)
            .query { rs, _ ->
                TaskRewardContext(
                    id = rs.getObject("id", UUID::class.java),
                    familyId = rs.getObject("family_id", UUID::class.java),
                    childId = rs.getObject("child_id", UUID::class.java),
                    scheduledDate = rs.getDate("scheduled_date").toLocalDate(),
                    weekId = rs.getString("week_id"),
                )
            }
            .optional()
            .orElse(null)

    private fun wishIdForReward(rewardId: UUID): UUID? =
        jdbcClient.sql("select wish_id from reward_ledger where id = :id")
            .param("id", rewardId)
            .query(UUID::class.java)
            .optional()
            .orElse(null)

    private fun activeWish(familyId: UUID, childId: UUID, weekId: String): ActiveWishRecord? =
        jdbcClient.sql(
            """
            select id, family_id, child_id, week_id
            from wish
            where family_id = :family_id
              and child_id = :child_id
              and week_id = :week_id
              and status in ('active', 'unlocked')
            order by case status when 'active' then 0 else 1 end
            limit 1
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("child_id", childId)
            .param("week_id", weekId)
            .query { rs, _ ->
                ActiveWishRecord(
                    id = rs.getObject("id", UUID::class.java),
                    familyId = rs.getObject("family_id", UUID::class.java),
                    childId = rs.getObject("child_id", UUID::class.java),
                    weekId = rs.getString("week_id"),
                )
            }
            .optional()
            .orElse(null)

    private fun currentFragmentRewardId(familyId: UUID, childId: UUID, date: LocalDate): UUID? =
        jdbcClient.sql(
            """
            select fragment_reward_id
            from daily_summary
            where family_id = :family_id
              and child_id = :child_id
              and date = :date
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("child_id", childId)
            .param("date", date)
            .query(UUID::class.java)
            .optional()
            .orElse(null)

    private fun insertReward(
        familyId: UUID,
        childId: UUID,
        weekId: String?,
        date: LocalDate?,
        rewardType: String,
        reason: String,
        amount: Int,
        wishId: UUID?,
        taskInstanceId: UUID?,
        reviewId: UUID?,
        sourceRewardId: UUID?,
        idempotencyKey: String,
        createdBy: UUID,
    ): UUID? =
        try {
            jdbcClient.sql(
                """
                insert into reward_ledger (
                  family_id, child_id, week_id, date, reward_type, reason, amount,
                  wish_id, task_instance_id, review_id, source_reward_id, idempotency_key, created_by
                ) values (
                  :family_id, :child_id, :week_id, :date, :reward_type, :reason, :amount,
                  :wish_id, :task_instance_id, :review_id, :source_reward_id, :idempotency_key, :created_by
                )
                returning id
                """.trimIndent(),
            )
                .param("family_id", familyId)
                .param("child_id", childId)
                .param("week_id", weekId)
                .param("date", date)
                .param("reward_type", rewardType)
                .param("reason", reason)
                .param("amount", amount)
                .param("wish_id", wishId)
                .param("task_instance_id", taskInstanceId)
                .param("review_id", reviewId)
                .param("source_reward_id", sourceRewardId)
                .param("idempotency_key", idempotencyKey)
                .param("created_by", createdBy)
                .query(UUID::class.java)
                .single()
        } catch (ex: DuplicateKeyException) {
            null
        }
}

data class TaskRewardContext(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val scheduledDate: LocalDate,
    val weekId: String,
)

data class DailyTaskAggregate(
    val familyId: UUID,
    val childId: UUID,
    val date: LocalDate,
    val weekId: String,
    val coreRequired: Int,
    val coreApproved: Int,
    val coreSkipped: Int,
    val totalTasks: Int,
    val approvedTasks: Int,
)

data class ActiveWishRecord(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val weekId: String,
)

data class WishProgress(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val earnedFragments: Int,
    val requiredFragments: Int,
    val status: String,
)

data class SourceFragmentReward(
    val id: UUID,
    val wishId: UUID,
)
