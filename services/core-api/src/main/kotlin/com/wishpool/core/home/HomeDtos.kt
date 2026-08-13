package com.wishpool.core.home

import com.wishpool.core.child.ChildProfileResponse
import com.wishpool.core.family.FamilyResponse
import com.wishpool.core.media.MediaAssetResponse
import com.wishpool.core.memories.WeeklyMemoryResponse
import com.wishpool.core.notifications.NotificationListResponse
import com.wishpool.core.reviews.PendingReviewCardResponse
import com.wishpool.core.room.RoomStateResponse
import com.wishpool.core.tasks.TaskTemplateResponse
import com.wishpool.core.tasks.TodaySnapshotResponse
import com.wishpool.core.tasks.WeeklyPlanResponse
import com.wishpool.core.wishes.WishResponse
import java.time.OffsetDateTime
import java.util.UUID

data class ChildHomeContextResponse(
    val child: ChildProfileResponse,
    val today: TodaySnapshotResponse,
    val currentWish: WishResponse?,
    val room: RoomStateResponse,
    val latestMemory: WeeklyMemoryResponse?,
    val latestFeedback: ChildFeedbackCardResponse?,
    val unreadNotifications: Int,
)

data class ParentDashboardContextResponse(
    val family: FamilyResponse,
    val children: List<ChildProfileResponse>,
    val selectedChild: ChildProfileResponse?,
    val today: TodaySnapshotResponse?,
    val currentWish: WishResponse?,
    val weeklyPlan: WeeklyPlanResponse?,
    val pendingReviews: List<PendingReviewCardResponse>,
    val taskTemplates: List<TaskTemplateResponse>,
    val memories: List<WeeklyMemoryResponse>,
    val room: RoomStateResponse?,
    val notificationInbox: NotificationListResponse,
)

data class ChildFeedbackCardResponse(
    val reviewId: UUID,
    val taskTitle: String,
    val decision: String,
    val emoji: String?,
    val text: String?,
    val audioMedia: MediaAssetResponse?,
    val createdAt: OffsetDateTime,
)
