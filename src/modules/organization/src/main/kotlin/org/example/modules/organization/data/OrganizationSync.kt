package org.example.modules.organization.data

import android.content.Context
import java.util.UUID
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.example.core.network.RecordDto
import org.example.core.syncengine.SyncCoordinator
import org.example.core.syncengine.SyncInstaller
import org.example.core.syncengine.SyncState
import org.example.core.syncengine.TableSync
import org.example.core.syncengine.str

internal class MemberSync(private val dao: OrganizationDao) : TableSync<MemberEntity>("members") {
    override suspend fun pending(ownerId: String) = dao.pendingMembers(ownerId)
    override suspend fun byServerId(ownerId: String, serverId: String) = dao.memberByServerId(ownerId, serverId)
    override suspend fun byClientId(ownerId: String, clientId: String) = dao.memberByClientId(ownerId, clientId)
    override suspend fun insert(row: MemberEntity) { dao.insertMember(row) }
    override suspend fun update(row: MemberEntity) = dao.updateMember(row)
    override suspend fun delete(id: Long) = dao.deleteMemberRow(id)

    override fun toData(row: MemberEntity): JsonObject = buildJsonObject {
        put("name", row.name)
        put("role", row.role)
    }

    override fun fromRemote(ownerId: String, existing: MemberEntity?, record: RecordDto): MemberEntity =
        (existing ?: MemberEntity(ownerId = ownerId, name = "", role = null)).copy(
            name = record.data.str("name").orEmpty(),
            role = record.data.str("role"),
            clientId = record.clientId ?: existing?.clientId ?: UUID.randomUUID().toString(),
            serverId = record.serverId,
            version = record.version,
            syncState = SyncState.SYNCED,
        )

    override fun markSynced(row: MemberEntity) = row.copy(syncState = SyncState.SYNCED)
}

internal class TeamSync(private val dao: OrganizationDao) : TableSync<TeamEntity>("teams") {
    override suspend fun pending(ownerId: String) = dao.pendingTeams(ownerId)
    override suspend fun byServerId(ownerId: String, serverId: String) = dao.teamByServerId(ownerId, serverId)
    override suspend fun byClientId(ownerId: String, clientId: String) = dao.teamByClientId(ownerId, clientId)
    override suspend fun insert(row: TeamEntity) { dao.insertTeam(row) }
    override suspend fun update(row: TeamEntity) = dao.updateTeam(row)
    override suspend fun delete(id: Long) = dao.deleteTeamRow(id)

    override fun toData(row: TeamEntity): JsonObject = buildJsonObject { put("name", row.name) }

    override fun fromRemote(ownerId: String, existing: TeamEntity?, record: RecordDto): TeamEntity =
        (existing ?: TeamEntity(ownerId = ownerId, name = "")).copy(
            name = record.data.str("name").orEmpty(),
            clientId = record.clientId ?: existing?.clientId ?: UUID.randomUUID().toString(),
            serverId = record.serverId,
            version = record.version,
            syncState = SyncState.SYNCED,
        )

    override fun markSynced(row: TeamEntity) = row.copy(syncState = SyncState.SYNCED)
}

internal class EventSync(private val dao: OrganizationDao) : TableSync<EventEntity>("events") {
    override suspend fun pending(ownerId: String) = dao.pendingEvents(ownerId)
    override suspend fun byServerId(ownerId: String, serverId: String) = dao.eventByServerId(ownerId, serverId)
    override suspend fun byClientId(ownerId: String, clientId: String) = dao.eventByClientId(ownerId, clientId)
    override suspend fun insert(row: EventEntity) { dao.insertEvent(row) }
    override suspend fun update(row: EventEntity) = dao.updateEvent(row)
    override suspend fun delete(id: Long) = dao.deleteEventRow(id)

    override fun toData(row: EventEntity): JsonObject = buildJsonObject {
        put("title", row.title)
        put("date", row.date)
    }

    override fun fromRemote(ownerId: String, existing: EventEntity?, record: RecordDto): EventEntity =
        (existing ?: EventEntity(ownerId = ownerId, title = "", date = "")).copy(
            title = record.data.str("title").orEmpty(),
            date = record.data.str("date").orEmpty(),
            clientId = record.clientId ?: existing?.clientId ?: UUID.randomUUID().toString(),
            serverId = record.serverId,
            version = record.version,
            syncState = SyncState.SYNCED,
        )

    override fun markSynced(row: EventEntity) = row.copy(syncState = SyncState.SYNCED)
}

/** Registers this module's tables with the sync engine (listed in App.kt). */
object OrganizationSync : SyncInstaller {
    override fun install(context: Context) {
        val dao = OrganizationDatabase.get(context).dao()
        SyncCoordinator.register(MemberSync(dao))
        SyncCoordinator.register(TeamSync(dao))
        SyncCoordinator.register(EventSync(dao))
    }
}
