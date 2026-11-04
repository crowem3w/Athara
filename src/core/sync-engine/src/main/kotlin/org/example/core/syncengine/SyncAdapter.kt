package org.example.core.syncengine

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import org.example.core.network.MutationDto
import org.example.core.network.MutationResultDto
import org.example.core.network.RecordDto

object SyncState {
    const val SYNCED = 0
    const val PENDING_UPSERT = 1
    const val PENDING_DELETE = 2
}

object SyncStatus {
    const val APPLIED = "applied"
    const val CONFLICT = "conflict"
    const val REJECTED = "rejected"
    const val GONE = "gone"
}

/** One synced table. A feature module registers one adapter per resource with [SyncCoordinator]. */
interface SyncAdapter {
    /** Path segment of /v1/sync/{resource}. */
    val resource: String

    /** Local changes not yet acknowledged by the server, oldest first. */
    suspend fun pendingMutations(ownerId: String): List<MutationDto>

    /** Applies the server's answer to one pushed change. Returns true if a full re-pull is needed. */
    suspend fun applyPushResult(ownerId: String, result: MutationResultDto): Boolean

    /** Applies records pulled from the server. The server's version wins. */
    suspend fun applyRemote(ownerId: String, records: List<RecordDto>)
}

/** Columns every synced Room entity carries. */
interface SyncRow {
    /** Local primary key. */
    val id: Long
    /** Client-generated UUID: idempotency key for pushes, and the link when the push response is lost. */
    val clientId: String
    /** Null until the server has acknowledged the row. */
    val serverId: String?
    /** Last server version seen (0 = never synced). */
    val version: Long
    /** One of [SyncState]. */
    val syncState: Int
}

/**
 * Generic outbox logic over a Room table, so each entity only describes its columns.
 *
 * Server-authoritative rules:
 *  - whatever record the server returns (applied OR conflict) overwrites the local row;
 *  - a pulled record never overwrites a row with a pending local change; that change is pushed first
 *    and the server's answer then decides (this protects edits made while a sync is running);
 *  - a rejected change is dropped and the next pull restores the server's state.
 */
abstract class TableSync<E : SyncRow>(final override val resource: String) : SyncAdapter {
    protected abstract suspend fun pending(ownerId: String): List<E>
    protected abstract suspend fun byServerId(ownerId: String, serverId: String): E?
    protected abstract suspend fun byClientId(ownerId: String, clientId: String): E?
    protected abstract suspend fun insert(row: E)
    protected abstract suspend fun update(row: E)
    protected abstract suspend fun delete(id: Long)
    protected abstract fun toData(row: E): JsonObject

    /** Builds the row for [record]: copy [existing] (or create a new row for [ownerId]) with the server's fields and SYNCED state. */
    protected abstract fun fromRemote(ownerId: String, existing: E?, record: RecordDto): E

    /** [row] unchanged but with syncState = SYNCED. */
    protected abstract fun markSynced(row: E): E

    final override suspend fun pendingMutations(ownerId: String): List<MutationDto> =
        pending(ownerId).map { row ->
            val delete = row.syncState == SyncState.PENDING_DELETE
            MutationDto(
                clientId = row.clientId,
                serverId = row.serverId,
                baseVersion = row.version,
                op = if (delete) "delete" else "upsert",
                data = if (delete) null else toData(row),
            )
        }

    final override suspend fun applyPushResult(ownerId: String, result: MutationResultDto): Boolean {
        val local = byClientId(ownerId, result.clientId) ?: return false
        val record = result.record
        return when {
            record != null -> { applyRecord(ownerId, local, record); false }
            local.syncState == SyncState.PENDING_DELETE &&
                (result.status == SyncStatus.APPLIED || result.status == SyncStatus.GONE) -> { delete(local.id); false }
            result.status == SyncStatus.GONE -> { delete(local.id); false }
            result.status == SyncStatus.REJECTED -> {
                if (local.serverId == null) delete(local.id) else update(markSynced(local))
                local.serverId != null // restore the server's state with a full pull
            }
            else -> false // "applied" without a record: leave it pending, the engine retries
        }
    }

    final override suspend fun applyRemote(ownerId: String, records: List<RecordDto>) {
        for (record in records) {
            val existing = byServerId(ownerId, record.serverId) ?: record.clientId?.let { byClientId(ownerId, it) }
            if (existing != null && existing.syncState != SyncState.SYNCED) continue
            applyRecord(ownerId, existing, record)
        }
    }

    private suspend fun applyRecord(ownerId: String, existing: E?, record: RecordDto) {
        when {
            record.deleted -> existing?.let { delete(it.id) }
            existing == null -> insert(fromRemote(ownerId, null, record))
            else -> update(fromRemote(ownerId, existing, record))
        }
    }
}

fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull
