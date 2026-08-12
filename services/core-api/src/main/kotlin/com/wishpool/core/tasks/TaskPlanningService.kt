package com.wishpool.core.tasks

import com.wishpool.core.child.ChildService
import com.wishpool.core.events.DomainEventPublisher
import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.ConflictError
import com.wishpool.core.shared.ForbiddenError
import com.wishpool.core.shared.NotFoundError
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class TaskPlanningService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
    private val childService: ChildService,
    private val eventPublisher: DomainEventPublisher,
) {
    fun listTaskTemplates(familyId: UUID): List<TaskTemplateResponse> {
        familyPolicy.requireParent(currentUser.require(), familyId)
        return jdbcClient.sql(
            """
            select id, family_id, title, category, submission_type, description, target_text, default_duration_sec
            from task_template
            where family_id = :family_id
              and archived_at is null
            order by category, title
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .query(::taskTemplateResponse)
            .list()
    }

    @Transactional
    fun createTaskTemplate(request: CreateTaskTemplateRequest): TaskTemplateResponse {
        val user = currentUser.require()
        familyPolicy.requireParent(user, request.familyId)
        validateCategory(request.category)
        validateSubmissionType(request.submissionType)
        return jdbcClient.sql(
            """
            insert into task_template (
              family_id, title, category, submission_type, description, target_text, default_duration_sec, created_by
            ) values (
              :family_id, :title, :category, :submission_type, :description, :target_text, :default_duration_sec, :created_by
            )
            returning id, family_id, title, category, submission_type, description, target_text, default_duration_sec
            """.trimIndent(),
        )
            .param("family_id", request.familyId)
            .param("title", request.title.trim())
            .param("category", request.category)
            .param("submission_type", request.submissionType)
            .param("description", request.description)
            .param("target_text", request.targetText)
            .param("default_duration_sec", request.defaultDurationSec)
            .param("created_by", user.userId)
            .query(::taskTemplateResponse)
            .single()
    }

    @Transactional
    fun saveWeeklyPlan(request: SaveWeeklyPlanRequest): WeeklyPlanResponse {
        val user = currentUser.require()
        familyPolicy.requireParent(user, request.familyId)
        val child = childService.findChild(request.childId) ?: throw NotFoundError("Child profile not found.")
        if (child.familyId != request.familyId) throw BadRequestError("Child profile does not belong to this family.")
        validatePlan(request)

        val planId = upsertWeeklyPlan(request, user.userId)
        val rules = replaceRules(planId, request.rules)
        eventPublisher.publishFamilyEvent(
            familyId = request.familyId,
            eventType = "planning.weekly_plan_saved",
            aggregateType = "weekly_plan",
            aggregateId = planId,
            payload = mapOf(
                "familyId" to request.familyId.toString(),
                "childId" to request.childId.toString(),
                "weekId" to request.weekId,
                "startDate" to request.startDate.toString(),
                "endDate" to request.endDate.toString(),
                "rewardMode" to request.rewardMode,
                "ruleCount" to rules.size,
            ),
        )
        return getWeeklyPlan(planId)
    }

    fun getWeeklyPlan(planId: UUID): WeeklyPlanResponse {
        val header = jdbcClient.sql(
            """
            select id, family_id, child_id, week_id, start_date, end_date, reward_mode, status
            from weekly_plan
            where id = :id
            """.trimIndent(),
        )
            .param("id", planId)
            .query(::weeklyPlanHeader)
            .optional()
            .orElseThrow { NotFoundError("Weekly plan not found.") }

        familyPolicy.requireParent(currentUser.require(), header.familyId)
        val rules = listRules(planId)
        return WeeklyPlanResponse(
            id = header.id,
            familyId = header.familyId,
            childId = header.childId,
            weekId = header.weekId,
            startDate = header.startDate,
            endDate = header.endDate,
            rewardMode = header.rewardMode,
            status = header.status,
            rules = rules,
        )
    }

    fun getToday(childId: UUID, date: LocalDate?): TodaySnapshotResponse {
        val user = currentUser.require()
        familyPolicy.requireCanAccessChild(user, childId)
        val child = childService.findChild(childId) ?: throw NotFoundError("Child profile not found.")
        val scheduledDate = date ?: LocalDate.now()
        val tasks = jdbcClient.sql(
            """
            select id, family_id, child_id, scheduled_date, title, category, submission_type,
                   description, target_text, is_core, require_review, status, latest_submission_id
            from task_instance
            where child_id = :child_id
              and scheduled_date = :scheduled_date
            order by sort_order, created_at
            """.trimIndent(),
        )
            .param("child_id", childId)
            .param("scheduled_date", scheduledDate)
            .query(::taskInstanceResponse)
            .list()

        return TodaySnapshotResponse(
            child = child,
            date = scheduledDate,
            tasks = tasks,
            dailySummary = dailySummary(child.familyId, childId, scheduledDate),
        )
    }

    @Transactional
    fun skipTask(taskId: UUID, request: SkipTaskRequest): TaskInstanceResponse {
        val task = findTask(taskId) ?: throw NotFoundError("Task not found.")
        val user = currentUser.require()
        familyPolicy.requireParent(user, task.familyId)
        if (task.status !in setOf("todo", "needs_revision", "adjusted_by_parent")) {
            throw ConflictError("Only open tasks can be skipped.")
        }

        val skipped = jdbcClient.sql(
            """
            update task_instance
            set status = 'skipped',
                skip_reason = :reason,
                version = version + 1,
                updated_at = now()
            where id = :id
            returning id, family_id, child_id, scheduled_date, title, category, submission_type,
                      description, target_text, is_core, require_review, status, latest_submission_id
            """.trimIndent(),
        )
            .param("id", taskId)
            .param("reason", request.reason)
            .query(::taskInstanceResponse)
            .single()
        eventPublisher.publishFamilyEvent(
            familyId = skipped.familyId,
            eventType = "task.skipped",
            aggregateType = "task_instance",
            aggregateId = skipped.id,
            payload = mapOf(
                "familyId" to skipped.familyId.toString(),
                "childId" to skipped.childId.toString(),
                "taskInstanceId" to skipped.id.toString(),
                "scheduledDate" to skipped.scheduledDate.toString(),
                "status" to skipped.status,
                "reason" to request.reason,
                "actorUserId" to user.userId.toString(),
            ),
        )
        return skipped
    }

    @Transactional
    fun postponeTask(taskId: UUID, request: PostponeTaskRequest): TaskInstanceResponse {
        val task = findTask(taskId) ?: throw NotFoundError("Task not found.")
        familyPolicy.requireParent(currentUser.require(), task.familyId)
        if (task.status !in setOf("todo", "needs_revision", "adjusted_by_parent")) {
            throw ConflictError("Only open tasks can be postponed.")
        }

        jdbcClient.sql(
            """
            update task_instance
            set status = 'adjusted_by_parent',
                skip_reason = coalesce(:reason, 'postponed'),
                version = version + 1,
                updated_at = now()
            where id = :id
            """.trimIndent(),
        )
            .param("id", taskId)
            .param("reason", request.reason)
            .update()

        val postponed = jdbcClient.sql(
            """
            insert into task_instance (
              family_id, child_id, weekly_plan_id, plan_rule_id, original_task_instance_id,
              scheduled_date, source, title, category, submission_type, description, target_text,
              is_core, require_review, status, sort_order
            ) values (
              :family_id, :child_id, :weekly_plan_id, :plan_rule_id, :original_task_instance_id,
              :scheduled_date, 'carry_over', :title, :category, :submission_type, :description, :target_text,
              :is_core, :require_review, 'todo', :sort_order
            )
            returning id, family_id, child_id, scheduled_date, title, category, submission_type,
                      description, target_text, is_core, require_review, status, latest_submission_id
            """.trimIndent(),
        )
            .param("family_id", task.familyId)
            .param("child_id", task.childId)
            .param("weekly_plan_id", task.weeklyPlanId)
            .param("plan_rule_id", task.planRuleId)
            .param("original_task_instance_id", task.id)
            .param("scheduled_date", request.newDate)
            .param("title", task.title)
            .param("category", task.category)
            .param("submission_type", task.submissionType)
            .param("description", task.description)
            .param("target_text", task.targetText)
            .param("is_core", task.isCore)
            .param("require_review", task.requireReview)
            .param("sort_order", task.sortOrder)
            .query(::taskInstanceResponse)
            .single()
        eventPublisher.publishFamilyEvent(
            familyId = task.familyId,
            eventType = "task.updated",
            aggregateType = "task_instance",
            aggregateId = task.id,
            payload = mapOf(
                "familyId" to task.familyId.toString(),
                "childId" to task.childId.toString(),
                "taskInstanceId" to task.id.toString(),
                "status" to "adjusted_by_parent",
                "scheduledDate" to task.scheduledDate.toString(),
                "reason" to (request.reason ?: "postponed"),
                "newTaskInstanceId" to postponed.id.toString(),
                "newScheduledDate" to postponed.scheduledDate.toString(),
            ),
        )
        eventPublisher.publishFamilyEvent(
            familyId = postponed.familyId,
            eventType = "task.created",
            aggregateType = "task_instance",
            aggregateId = postponed.id,
            payload = taskCreatedPayload(postponed, source = "carry_over"),
        )
        return postponed
    }

    private fun upsertWeeklyPlan(request: SaveWeeklyPlanRequest, userId: UUID): UUID {
        val existingId = jdbcClient.sql(
            """
            select id
            from weekly_plan
            where family_id = :family_id
              and child_id = :child_id
              and week_id = :week_id
            """.trimIndent(),
        )
            .param("family_id", request.familyId)
            .param("child_id", request.childId)
            .param("week_id", request.weekId)
            .query(UUID::class.java)
            .optional()
            .orElse(null)

        if (existingId != null) {
            jdbcClient.sql(
                """
                update weekly_plan
                set start_date = :start_date,
                    end_date = :end_date,
                    wish_id = :wish_id,
                    reward_mode = :reward_mode,
                    status = 'active',
                    version = version + 1,
                    updated_at = now()
                where id = :id
                """.trimIndent(),
            )
                .param("id", existingId)
                .param("start_date", request.startDate)
                .param("end_date", request.endDate)
                .param("wish_id", request.wishId)
                .param("reward_mode", request.rewardMode)
                .update()
            return existingId
        }

        return jdbcClient.sql(
            """
            insert into weekly_plan (
              family_id, child_id, week_id, start_date, end_date, wish_id, reward_mode, status, created_by
            ) values (
              :family_id, :child_id, :week_id, :start_date, :end_date, :wish_id, :reward_mode, 'active', :created_by
            )
            returning id
            """.trimIndent(),
        )
            .param("family_id", request.familyId)
            .param("child_id", request.childId)
            .param("week_id", request.weekId)
            .param("start_date", request.startDate)
            .param("end_date", request.endDate)
            .param("wish_id", request.wishId)
            .param("reward_mode", request.rewardMode)
            .param("created_by", userId)
            .query(UUID::class.java)
            .single()
    }

    private fun replaceRules(planId: UUID, rules: List<WeeklyPlanRuleInput>): List<WeeklyPlanRuleResponse> {
        jdbcClient.sql(
            """
            update weekly_plan_rule
            set superseded_at = now()
            where weekly_plan_id = :plan_id
              and superseded_at is null
            """.trimIndent(),
        )
            .param("plan_id", planId)
            .update()

        rules.forEachIndexed { index, rule ->
            jdbcClient.sql(
                """
                insert into weekly_plan_rule (
                  weekly_plan_id, task_template_id, title_snapshot, category, submission_type,
                  description_snapshot, target_text_snapshot, weekdays, is_core, require_review, sort_order
                ) values (
                  :weekly_plan_id, :task_template_id, :title, :category, :submission_type,
                  :description, :target_text, :weekdays, :is_core, :require_review, :sort_order
                )
                """.trimIndent(),
            )
                .param("weekly_plan_id", planId)
                .param("task_template_id", rule.taskTemplateId)
                .param("title", rule.title.trim())
                .param("category", rule.category)
                .param("submission_type", rule.submissionType)
                .param("description", rule.description)
                .param("target_text", rule.targetText)
                .param("weekdays", rule.weekdays.toTypedArray())
                .param("is_core", rule.isCore)
                .param("require_review", rule.requireReview)
                .param("sort_order", rule.sortOrder ?: index)
                .update()
        }

        return listRules(planId)
    }

    private fun listRules(planId: UUID): List<WeeklyPlanRuleResponse> =
        jdbcClient.sql(
            """
            select id, task_template_id, title_snapshot, category, submission_type,
                   description_snapshot, target_text_snapshot, weekdays, is_core, require_review, sort_order
            from weekly_plan_rule
            where weekly_plan_id = :plan_id
              and superseded_at is null
            order by sort_order, created_at
            """.trimIndent(),
        )
            .param("plan_id", planId)
            .query(::weeklyPlanRuleResponse)
            .list()

    @Transactional
    fun materializeWeeklyPlanFromWorkflow(planId: UUID, triggeredByEventId: UUID?): WeeklyPlanResponse {
        val header = findWeeklyPlanHeader(planId) ?: throw NotFoundError("Weekly plan not found.")
        val rules = listRules(planId)
        materializeTasks(
            planId = planId,
            request = SaveWeeklyPlanRequest(
                familyId = header.familyId,
                childId = header.childId,
                weekId = header.weekId,
                startDate = header.startDate,
                endDate = header.endDate,
                rewardMode = header.rewardMode,
                rules = emptyList(),
            ),
            rules = rules,
        )
        eventPublisher.publishFamilyEvent(
            familyId = header.familyId,
            eventType = "workflow.materialize_weekly_plan_completed",
            aggregateType = "weekly_plan",
            aggregateId = header.id,
            payload = mapOf(
                "familyId" to header.familyId.toString(),
                "childId" to header.childId.toString(),
                "weeklyPlanId" to header.id.toString(),
                "weekId" to header.weekId,
                "triggeredByEventId" to triggeredByEventId?.toString(),
            ),
        )
        return getWeeklyPlanForWorkflow(header, rules)
    }

    private fun materializeTasks(
        planId: UUID,
        request: SaveWeeklyPlanRequest,
        rules: List<WeeklyPlanRuleResponse>,
    ) {
        jdbcClient.sql(
            """
            delete from task_instance
            where weekly_plan_id = :plan_id
              and status = 'todo'
              and latest_submission_id is null
            """.trimIndent(),
        )
            .param("plan_id", planId)
            .update()

        val dates = generateSequence(request.startDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(request.endDate) }
            .toList()

        for (rule in rules) {
            for (date in dates) {
                val weekday = date.dayOfWeek.value
                if (weekday !in rule.weekdays) continue
                jdbcClient.sql(
                    """
                    with inserted as (
                      insert into task_instance (
                        family_id, child_id, weekly_plan_id, plan_rule_id, scheduled_date,
                        source, title, category, submission_type, description, target_text,
                        is_core, require_review, status, sort_order
                      ) values (
                        :family_id, :child_id, :weekly_plan_id, :plan_rule_id, :scheduled_date,
                        'weekly_rule', :title, :category, :submission_type, :description, :target_text,
                        :is_core, :require_review, 'todo', :sort_order
                      )
                      on conflict do nothing
                      returning id, family_id, child_id, scheduled_date, title, category, submission_type,
                                description, target_text, is_core, require_review, status, latest_submission_id
                    )
                    select id, family_id, child_id, scheduled_date, title, category, submission_type,
                           description, target_text, is_core, require_review, status, latest_submission_id
                    from inserted
                    """.trimIndent(),
                )
                    .param("family_id", request.familyId)
                    .param("child_id", request.childId)
                    .param("weekly_plan_id", planId)
                    .param("plan_rule_id", rule.id)
                    .param("scheduled_date", date)
                    .param("title", rule.title)
                    .param("category", rule.category)
                    .param("submission_type", rule.submissionType)
                    .param("description", rule.description)
                    .param("target_text", rule.targetText)
                    .param("is_core", rule.isCore)
                    .param("require_review", rule.requireReview)
                    .param("sort_order", rule.sortOrder)
                    .query(::taskInstanceResponse)
                    .optional()
                    .ifPresent { created ->
                        eventPublisher.publishFamilyEvent(
                            familyId = request.familyId,
                            eventType = "task.created",
                            aggregateType = "task_instance",
                            aggregateId = created.id,
                            payload = taskCreatedPayload(created, source = "weekly_rule"),
                        )
                    }
            }
        }
    }

    private fun findWeeklyPlanHeader(planId: UUID): WeeklyPlanHeader? =
        jdbcClient.sql(
            """
            select id, family_id, child_id, week_id, start_date, end_date, reward_mode, status
            from weekly_plan
            where id = :id
            """.trimIndent(),
        )
            .param("id", planId)
            .query(::weeklyPlanHeader)
            .optional()
            .orElse(null)

    private fun getWeeklyPlanForWorkflow(
        header: WeeklyPlanHeader,
        rules: List<WeeklyPlanRuleResponse>,
    ): WeeklyPlanResponse =
        WeeklyPlanResponse(
            id = header.id,
            familyId = header.familyId,
            childId = header.childId,
            weekId = header.weekId,
            startDate = header.startDate,
            endDate = header.endDate,
            rewardMode = header.rewardMode,
            status = header.status,
            rules = rules,
        )

    private fun taskCreatedPayload(task: TaskInstanceResponse, source: String): Map<String, Any?> =
        mapOf(
            "familyId" to task.familyId.toString(),
            "childId" to task.childId.toString(),
            "taskInstanceId" to task.id.toString(),
            "scheduledDate" to task.scheduledDate.toString(),
            "title" to task.title,
            "category" to task.category,
            "submissionType" to task.submissionType,
            "isCore" to task.isCore,
            "requireReview" to task.requireReview,
            "status" to task.status,
            "source" to source,
        )

    private fun dailySummary(familyId: UUID, childId: UUID, date: LocalDate): DailySummaryResponse {
        val stored = jdbcClient.sql(
            """
            select date, core_required, core_approved, core_skipped, fragment_status
            from daily_summary
            where family_id = :family_id
              and child_id = :child_id
              and date = :date
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("child_id", childId)
            .param("date", date)
            .query { rs, _ ->
                DailySummaryResponse(
                    date = rs.getDate("date").toLocalDate(),
                    coreRequired = rs.getInt("core_required"),
                    coreApproved = rs.getInt("core_approved"),
                    coreSkipped = rs.getInt("core_skipped"),
                    fragmentStatus = rs.getString("fragment_status"),
                )
            }
            .optional()
            .orElse(null)
        if (stored != null) return stored

        return jdbcClient.sql(
            """
            select
              count(*) filter (where is_core) as core_required,
              count(*) filter (where is_core and status = 'approved') as core_approved,
              count(*) filter (where is_core and status = 'skipped') as core_skipped
            from task_instance
            where family_id = :family_id
              and child_id = :child_id
              and scheduled_date = :date
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("child_id", childId)
            .param("date", date)
            .query { rs, _ ->
                DailySummaryResponse(
                    date = date,
                    coreRequired = rs.getInt("core_required"),
                    coreApproved = rs.getInt("core_approved"),
                    coreSkipped = rs.getInt("core_skipped"),
                    fragmentStatus = "not_earned",
                )
            }
            .single()
    }

    private fun findTask(taskId: UUID): TaskRecord? =
        jdbcClient.sql(
            """
            select id, family_id, child_id, weekly_plan_id, plan_rule_id, scheduled_date,
                   title, category, submission_type, description, target_text,
                   is_core, require_review, status, sort_order
            from task_instance
            where id = :id
            """.trimIndent(),
        )
            .param("id", taskId)
            .query { rs, _ ->
                TaskRecord(
                    id = rs.getObject("id", UUID::class.java),
                    familyId = rs.getObject("family_id", UUID::class.java),
                    childId = rs.getObject("child_id", UUID::class.java),
                    weeklyPlanId = rs.getObject("weekly_plan_id", UUID::class.java),
                    planRuleId = rs.getObject("plan_rule_id", UUID::class.java),
                    scheduledDate = rs.getDate("scheduled_date").toLocalDate(),
                    title = rs.getString("title"),
                    category = rs.getString("category"),
                    submissionType = rs.getString("submission_type"),
                    description = rs.getString("description"),
                    targetText = rs.getString("target_text"),
                    isCore = rs.getBoolean("is_core"),
                    requireReview = rs.getBoolean("require_review"),
                    status = rs.getString("status"),
                    sortOrder = rs.getInt("sort_order"),
                )
            }
            .optional()
            .orElse(null)

    private fun validatePlan(request: SaveWeeklyPlanRequest) {
        if (request.startDate.isAfter(request.endDate)) throw BadRequestError("startDate must be before or equal to endDate.")
        if (request.rules.isEmpty()) throw BadRequestError("Weekly plan must contain at least one rule.")
        if (request.rewardMode !in setOf("flexible", "strict")) throw BadRequestError("Unsupported rewardMode.")
        request.rules.forEach { rule ->
            validateCategory(rule.category)
            validateSubmissionType(rule.submissionType)
            if (rule.weekdays.isEmpty()) throw BadRequestError("Rule weekdays cannot be empty.")
            if (rule.weekdays.any { it !in 1..7 }) throw BadRequestError("Rule weekdays must be between 1 and 7.")
            if (rule.taskTemplateId != null && !templateBelongsToFamily(rule.taskTemplateId, request.familyId)) {
                throw ForbiddenError("Task template does not belong to this family.")
            }
        }
    }

    private fun templateBelongsToFamily(templateId: UUID, familyId: UUID): Boolean =
        jdbcClient.sql(
            """
            select count(*)
            from task_template
            where id = :id
              and family_id = :family_id
              and archived_at is null
            """.trimIndent(),
        )
            .param("id", templateId)
            .param("family_id", familyId)
            .query(Int::class.java)
            .single() == 1

    private fun validateCategory(category: String) {
        if (category !in setOf("study", "reading", "exercise", "habit", "custom")) {
            throw BadRequestError("Unsupported task category.")
        }
    }

    private fun validateSubmissionType(submissionType: String) {
        if (submissionType !in setOf("photo", "audio", "video", "manual")) {
            throw BadRequestError("Unsupported submission type.")
        }
    }
}

data class TaskRecord(
    val id: UUID,
    val familyId: UUID,
    val childId: UUID,
    val weeklyPlanId: UUID?,
    val planRuleId: UUID?,
    val scheduledDate: LocalDate,
    val title: String,
    val category: String,
    val submissionType: String,
    val description: String?,
    val targetText: String?,
    val isCore: Boolean,
    val requireReview: Boolean,
    val status: String,
    val sortOrder: Int,
)
