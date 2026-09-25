package com.wishpool.core.rewards

import com.wishpool.core.family.FamilyPolicy
import com.wishpool.core.security.CurrentUser
import com.wishpool.core.shared.BadRequestError
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class RewardSummaryService(
    private val jdbcClient: JdbcClient,
    private val currentUser: CurrentUser,
    private val familyPolicy: FamilyPolicy,
) {
    fun getSummary(childId: UUID, weekId: String?, fromDate: LocalDate?, toDate: LocalDate?): RewardSummaryResponse {
        familyPolicy.requireCanAccessChild(currentUser.require(), childId)
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw BadRequestError("fromDate must be on or before toDate.")
        }
        return jdbcClient.sql(
            """
            select
              coalesce(sum(amount) filter (where reward_type = 'star_light'), 0)::int as star_light,
              coalesce(sum(amount) filter (where reward_type = 'wish_fragment'), 0)::int as wish_fragment,
              coalesce(sum(amount) filter (where reward_type = 'adjustment'), 0)::int as adjustment,
              coalesce(sum(amount), 0)::int as total
            from reward_ledger
            where child_id = :child_id
              and (cast(:week_id as text) is null or week_id = cast(:week_id as text))
              and (cast(:from_date as date) is null or date >= cast(:from_date as date))
              and (cast(:to_date as date) is null or date <= cast(:to_date as date))
            """.trimIndent(),
        )
            .param("child_id", childId)
            .param("week_id", weekId)
            .param("from_date", fromDate)
            .param("to_date", toDate)
            .query { rs, _ ->
                RewardSummaryResponse(
                    childId = childId, weekId = weekId, fromDate = fromDate, toDate = toDate,
                    starLight = rs.getInt("star_light"), wishFragment = rs.getInt("wish_fragment"),
                    adjustment = rs.getInt("adjustment"), total = rs.getInt("total"),
                )
            }
            .single()
    }
}
