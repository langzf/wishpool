package com.wishpool.core.family

import com.wishpool.core.security.CurrentUser
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.util.UUID

@Service
class DeviceService(private val jdbc: JdbcClient, private val currentUser: CurrentUser, private val policy: FamilyPolicy) {
    fun list(familyId: UUID): List<FamilyDeviceResponse> {
        policy.requireMember(currentUser.require(), familyId)
        return jdbc.sql("""
            select id, platform, device_name, child_id, push_provider, push_token,
                   last_seen_at, created_at, revoked_at from device where family_id=:family_id order by created_at desc
        """).param("family_id", familyId).query(::deviceResponse).list()
    }

    @Transactional
    fun revoke(familyId: UUID, deviceId: UUID, request: RevokeDeviceRequest): FamilyDeviceResponse {
        val user = currentUser.require(); policy.requireParent(user, familyId)
        val existing = jdbc.sql("select id, platform, device_name, child_id, push_provider, push_token, last_seen_at, created_at, revoked_at from device where id=:id and family_id=:family_id")
            .param("id", deviceId).param("family_id", familyId).query(::deviceResponse).optional().orElseThrow { com.wishpool.core.shared.ForbiddenError("Device does not belong to this family.") }
        if (existing.revokedAt == null) {
            jdbc.sql("update device set revoked_at=now(), revoked_by_user_id=:user_id, updated_at=now() where id=:id and revoked_at is null")
                .param("user_id", user.userId).param("id", deviceId).update()
            jdbc.sql("update auth_session set status='revoked', revoked_at=now() where device_id=:id and status='active'")
                .param("id", deviceId).update()
            jdbc.sql("insert into audit_log (family_id, actor_user_id, actor_role, action, resource_type, resource_id, metadata_json) values (:family_id,:user_id,'parent','device.revoked','device',:device_id,cast(:metadata as jsonb))")
                .param("family_id", familyId).param("user_id", user.userId).param("device_id", deviceId)
                .param("metadata", "{\"reason\":\"${(request.reason ?: "").replace("\\", "\\\\").replace("\"", "\\\"")}\"}").update()
        }
        return jdbc.sql("select id, platform, device_name, child_id, push_provider, push_token, last_seen_at, created_at, revoked_at from device where id=:id")
            .param("id", deviceId).query(::deviceResponse).single()
    }
}

private fun deviceResponse(rs: ResultSet, row: Int) = FamilyDeviceResponse(
    id=rs.getObject("id", UUID::class.java), platform=rs.getString("platform"), deviceName=rs.getString("device_name"), childId=rs.getObject("child_id", UUID::class.java),
    pushProvider=rs.getString("push_provider"), pushTokenRegistered=rs.getString("push_token") != null, lastSeenAt=rs.getObject("last_seen_at", java.time.OffsetDateTime::class.java),
    createdAt=rs.getObject("created_at", java.time.OffsetDateTime::class.java), revokedAt=rs.getObject("revoked_at", java.time.OffsetDateTime::class.java))
