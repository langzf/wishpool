package com.wishpool.core.home

import com.wishpool.core.child.ChildService
import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.family.FamilyService
import com.wishpool.core.media.MediaAssetRecord
import com.wishpool.core.media.MediaService
import com.wishpool.core.media.mediaAssetRecord
import com.wishpool.core.memories.MemoryService
import com.wishpool.core.notifications.NotificationService
import com.wishpool.core.reviews.ReviewService
import com.wishpool.core.room.RoomService
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import com.wishpool.core.shared.NotFoundError
import com.wishpool.core.tasks.TaskPlanningService
import com.wishpool.core.wishes.WishService
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class HomeService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
    private val familyService: FamilyService,
    private val childService: ChildService,
    private val taskPlanningService: TaskPlanningService,
    private val reviewService: ReviewService,
    private val wishService: WishService,
    private val roomService: RoomService,
    private val memoryService: MemoryService,
    private val notificationService: NotificationService,
    private val mediaService: MediaService,
) {
    fun getChildHomeContext(childId: UUID, date: LocalDate?): ChildHomeContextResponse {
        val user = currentUser.require()
        familyPolicy.requireCanAccessChild(user, childId)
        val child = childService.findChild(childId) ?: throw NotFoundError("Child profile not found.")
        val today = taskPlanningService.getToday(childId, date)
        val wish = currentWishOrNull(childId)
        val room = roomService.getRoomState(childId)
        val memories = memoryService.listMemories(childId, cursor = null, limit = 1).items
        return ChildHomeContextResponse(
            child = child,
            today = today.copy(currentWish = wish),
            currentWish = wish,
            room = room,
            latestMemory = memories.firstOrNull(),
            latestFeedback = latestFeedback(childId),
            unreadNotifications = unreadNotificationCount(child.familyId, user.userId),
        )
    }

    fun getParentDashboardContext(familyId: UUID, childId: UUID?, date: LocalDate?): ParentDashboardContextResponse {
        val user = currentUser.require()
        familyPolicy.requireParent(user, familyId)
        val family = familyService.getFamily(familyId)
        val children = childService.listChildren(familyId)
        val selectedChild = selectChild(children, childId)
        val today = selectedChild?.let { taskPlanningService.getToday(it.id, date) }
        val wish = selectedChild?.let { currentWishOrNull(it.id) }
        val weeklyPlan = selectedChild?.let { currentWeeklyPlanOrNull(familyId, it.id, date ?: LocalDate.now()) }
        val wishHistory = selectedChild?.let { wishService.listChildWishHistory(it.id, limit = 24) }.orEmpty()
        val memories = selectedChild?.let { memoryService.listMemories(it.id, cursor = null, limit = 8).items }.orEmpty()
        return ParentDashboardContextResponse(
            family = family,
            children = children,
            selectedChild = selectedChild,
            today = today?.copy(currentWish = wish),
            currentWish = wish,
            weeklyPlan = weeklyPlan,
            pendingReviews = reviewService.listPendingReviews(familyId),
            taskTemplates = taskPlanningService.listTaskTemplates(familyId),
            wishHistory = wishHistory,
            memories = memories,
            room = selectedChild?.let { roomService.getRoomState(it.id) },
            notificationInbox = notificationService.listInbox(familyId, status = null, limit = 20),
        )
    }

    private fun selectChild(children: List<com.wishpool.core.child.ChildProfileResponse>, childId: UUID?): com.wishpool.core.child.ChildProfileResponse? {
        if (children.isEmpty()) return null
        if (childId == null) return children.first()
        return children.firstOrNull { it.id == childId } ?: throw BadRequestError("childId does not belong to this family.")
    }

    private fun currentWishOrNull(childId: UUID): com.wishpool.core.wishes.WishResponse? =
        try {
            wishService.getCurrentWish(childId)
        } catch (ex: NotFoundError) {
            null
        }

    private fun currentWeeklyPlanOrNull(familyId: UUID, childId: UUID, date: LocalDate): com.wishpool.core.tasks.WeeklyPlanResponse? {
        val planId = jdbcClient.sql(
            """
            select id
            from weekly_plan
            where family_id = :family_id
              and child_id = :child_id
              and status = 'active'
              and start_date <= :date
              and end_date >= :date
            order by updated_at desc
            limit 1
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("child_id", childId)
            .param("date", date)
            .query(UUID::class.java)
            .optional()
            .orElse(null)
        return planId?.let { taskPlanningService.getWeeklyPlan(it) }
    }

    private fun latestFeedback(childId: UUID): ChildFeedbackCardResponse? =
        jdbcClient.sql(
            """
            select
              r.id,
              ti.title,
              r.decision,
              f.emoji,
              f.text,
              f.audio_media_id,
              r.created_at,
              ma.id as media_id,
              ma.family_id as media_family_id,
              ma.child_id as media_child_id,
              ma.purpose as media_purpose,
              ma.storage_key as media_storage_key,
              ma.content_type as media_content_type,
              ma.size_bytes as media_size_bytes,
              ma.status as media_status,
              ma.related_type as media_related_type,
              ma.related_id as media_related_id
            from review r
            join task_instance ti on ti.id = r.task_instance_id
            left join feedback f on f.review_id = r.id
            left join media_asset ma on ma.id = f.audio_media_id
            where r.child_id = :child_id
              and r.revoked_at is null
            order by r.created_at desc
            limit 1
            """.trimIndent(),
        )
            .param("child_id", childId)
            .query { rs, _ ->
                ChildFeedbackCardResponse(
                    reviewId = rs.getObject("id", UUID::class.java),
                    taskTitle = rs.getString("title"),
                    decision = rs.getString("decision"),
                    emoji = rs.getString("emoji"),
                    text = rs.getString("text"),
                    audioMedia = rs.getObject("media_id", UUID::class.java)?.let {
                        mediaService.toResponse(
                            MediaAssetRecord(
                                id = it,
                                familyId = rs.getObject("media_family_id", UUID::class.java),
                                childId = rs.getObject("media_child_id", UUID::class.java),
                                purpose = rs.getString("media_purpose"),
                                storageKey = rs.getString("media_storage_key"),
                                contentType = rs.getString("media_content_type"),
                                sizeBytes = rs.getLong("media_size_bytes").takeUnless { rs.wasNull() },
                                status = rs.getString("media_status"),
                                relatedType = rs.getString("media_related_type"),
                                relatedId = rs.getObject("media_related_id", UUID::class.java),
                            ),
                        )
                    },
                    createdAt = rs.getObject("created_at", OffsetDateTime::class.java),
                )
            }
            .optional()
            .orElse(null)

    private fun unreadNotificationCount(familyId: UUID, userId: UUID): Int =
        jdbcClient.sql(
            """
            select count(*)
            from notification_event
            where family_id = :family_id
              and recipient_user_id = :user_id
              and status in ('pending', 'sent', 'failed')
            """.trimIndent(),
        )
            .param("family_id", familyId)
            .param("user_id", userId)
            .query(Int::class.java)
            .single()
}
