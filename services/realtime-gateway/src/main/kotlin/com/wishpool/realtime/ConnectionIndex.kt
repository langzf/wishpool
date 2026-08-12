package com.wishpool.realtime

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.util.UUID

interface ConnectionIndex : AutoCloseable {
    suspend fun register(context: RealtimeConnectionContext)
    suspend fun refresh(context: RealtimeConnectionContext)
    suspend fun unregister(context: RealtimeConnectionContext)

    override fun close() = Unit
}

class NoopConnectionIndex : ConnectionIndex {
    override suspend fun register(context: RealtimeConnectionContext) = Unit
    override suspend fun refresh(context: RealtimeConnectionContext) = Unit
    override suspend fun unregister(context: RealtimeConnectionContext) = Unit
}

class LettuceConnectionIndex(
    redisUri: String,
    private val ttlSeconds: Long,
) : ConnectionIndex {
    private val client: RedisClient = RedisClient.create(redisUri)
    private val connection: StatefulRedisConnection<String, String> = client.connect()
    private val commands = connection.sync()

    override suspend fun register(context: RealtimeConnectionContext) {
        withContext(Dispatchers.IO) {
            commands.sadd(familyConnectionsKey(context.request.familyId), context.connectionId)
            commands.hset(
                connectionKey(context.connectionId),
                mapOf(
                    "connectionId" to context.connectionId,
                    "familyId" to context.request.familyId.toString(),
                    "userId" to context.user.id.toString(),
                    "transport" to context.request.transport,
                    "connectedAt" to OffsetDateTime.now().toString(),
                    "latestSeq" to context.latestSeq.toString(),
                ),
            )
            expire(context)
        }
    }

    override suspend fun refresh(context: RealtimeConnectionContext) {
        withContext(Dispatchers.IO) {
            commands.hset(connectionKey(context.connectionId), "latestSeq", context.latestSeq.toString())
            expire(context)
        }
    }

    override suspend fun unregister(context: RealtimeConnectionContext) {
        withContext(Dispatchers.IO) {
            commands.srem(familyConnectionsKey(context.request.familyId), context.connectionId)
            commands.del(connectionKey(context.connectionId))
        }
    }

    private fun expire(context: RealtimeConnectionContext) {
        commands.expire(connectionKey(context.connectionId), ttlSeconds)
        commands.expire(familyConnectionsKey(context.request.familyId), ttlSeconds)
    }

    override fun close() {
        connection.close()
        client.shutdown()
    }
}

private fun familyConnectionsKey(familyId: UUID): String =
    "wishpool:realtime:family:$familyId:connections"

private fun connectionKey(connectionId: String): String =
    "wishpool:realtime:connection:$connectionId"
