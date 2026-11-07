package soc.athara.app.core.syncengine

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import soc.athara.app.core.network.MutationDto
import soc.athara.app.core.network.MutationResultDto
import soc.athara.app.core.network.RecordDto

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


interface SyncAdapter {
    
    val resource: String

    
    suspend fun pendingMutations(ownerId: String): List<MutationDto>

    
    suspend fun applyPushResult(ownerId: String, result: MutationResultDto): Boolean

    
    suspend fun applyRemote(ownerId: String, records: List<RecordDto>)

    
    suspend fun requestDelete(ownerId: String, clientId: String): Boolean
}


interface SyncRow {
    
    val id: Long
    
    val clientId: String
    
    val serverId: String?
    
    val version: Long
    
    val syncState: Int
}










abstract class TableSync<E : SyncRow>(final override val resource: String) : SyncAdapter {
    protected abstract suspend fun pending(ownerId: String): List<E>
    protected abstract suspend fun byServerId(ownerId: String, serverId: String): E?
    protected abstract suspend fun byClientId(ownerId: String, clientId: String): E?
    protected abstract suspend fun insert(row: E)
    protected abstract suspend fun update(row: E)
    protected abstract suspend fun delete(id: Long)
    protected abstract fun toData(row: E): JsonObject

    
    protected abstract fun fromRemote(ownerId: String, existing: E?, record: RecordDto): E

    
    protected abstract fun markSynced(row: E): E

    protected abstract fun markPendingDelete(row: E): E

    
    protected abstract fun label(row: E): String

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
            record != null -> {
                val wasPendingDelete = local.syncState == SyncState.PENDING_DELETE
                applyRecord(ownerId, local, record)
                
                if (wasPendingDelete && result.status == SyncStatus.CONFLICT && !record.deleted) {
                    SyncNotices.deleteConflict(label(local), resource, local.clientId)
                }
                false
            }
            local.syncState == SyncState.PENDING_DELETE &&
                (result.status == SyncStatus.APPLIED || result.status == SyncStatus.GONE) -> { delete(local.id); false }
            result.status == SyncStatus.GONE -> { delete(local.id); false }
            result.status == SyncStatus.REJECTED -> {
                val existing = local.serverId != null
                if (existing) update(markSynced(local)) else delete(local.id)
                SyncNotices.rejected(label(local), reverted = existing)
                existing
            }
            else -> false 
        }
    }

    final override suspend fun applyRemote(ownerId: String, records: List<RecordDto>) {
        for (record in records) {
            val existing = byServerId(ownerId, record.serverId) ?: record.clientId?.let { byClientId(ownerId, it) }
            if (existing != null && existing.syncState != SyncState.SYNCED) continue
            applyRecord(ownerId, existing, record)
        }
    }

    final override suspend fun requestDelete(ownerId: String, clientId: String): Boolean {
        val row = byClientId(ownerId, clientId) ?: return false
        if (row.syncState != SyncState.SYNCED) return false
        update(markPendingDelete(row))
        return true
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