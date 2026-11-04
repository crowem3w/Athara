package org.example.modules.organization.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import org.example.core.syncengine.SyncRow
import org.example.core.syncengine.SyncState

/**
 * Every row belongs to one signed-in user ([ownerId] = AuthUser.id, i.e. the server's user id), so
 * accounts never see each other's data. The last four columns are sync bookkeeping ([SyncRow]):
 * new local rows start as PENDING_UPSERT, deleting a synced row only tombstones it (PENDING_DELETE)
 * until the server confirms.
 */
@Entity(
    tableName = "members",
    indices = [Index("ownerId"), Index(value = ["clientId"], unique = true), Index(value = ["ownerId", "serverId"], unique = true)],
)
internal data class MemberEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
    val ownerId: String,
    val name: String,
    val role: String?,
    override val clientId: String = UUID.randomUUID().toString(),
    override val serverId: String? = null,
    override val version: Long = 0,
    override val syncState: Int = SyncState.PENDING_UPSERT,
) : SyncRow

@Entity(
    tableName = "teams",
    indices = [Index("ownerId"), Index(value = ["clientId"], unique = true), Index(value = ["ownerId", "serverId"], unique = true)],
)
internal data class TeamEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
    val ownerId: String,
    val name: String,
    override val clientId: String = UUID.randomUUID().toString(),
    override val serverId: String? = null,
    override val version: Long = 0,
    override val syncState: Int = SyncState.PENDING_UPSERT,
) : SyncRow

@Entity(
    tableName = "events",
    indices = [Index("ownerId"), Index(value = ["clientId"], unique = true), Index(value = ["ownerId", "serverId"], unique = true)],
)
internal data class EventEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
    val ownerId: String,
    val title: String,
    /** ISO date (yyyy-MM-dd); sorts and compares correctly as text. */
    val date: String,
    override val clientId: String = UUID.randomUUID().toString(),
    override val serverId: String? = null,
    override val version: Long = 0,
    override val syncState: Int = SyncState.PENDING_UPSERT,
) : SyncRow

@Dao
internal interface OrganizationDao {
    // ---- what the UI reads and writes (tombstoned rows are hidden) ----
    @Query("SELECT * FROM members WHERE ownerId = :ownerId AND syncState != 2 ORDER BY name COLLATE NOCASE")
    fun members(ownerId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM teams WHERE ownerId = :ownerId AND syncState != 2 ORDER BY name COLLATE NOCASE")
    fun teams(ownerId: String): Flow<List<TeamEntity>>

    @Query("SELECT * FROM events WHERE ownerId = :ownerId AND syncState != 2 ORDER BY date ASC")
    fun events(ownerId: String): Flow<List<EventEntity>>

    @Insert suspend fun insertMember(member: MemberEntity): Long
    @Insert suspend fun insertTeam(team: TeamEntity): Long
    @Insert suspend fun insertEvent(event: EventEntity): Long

    /** Never reached the server: remove outright. */
    @Query("DELETE FROM members WHERE id = :id AND ownerId = :ownerId AND serverId IS NULL")
    suspend fun deleteLocalOnlyMember(id: Long, ownerId: String)

    /** Known to the server: keep a tombstone until the delete is pushed. */
    @Query("UPDATE members SET syncState = 2 WHERE id = :id AND ownerId = :ownerId AND serverId IS NOT NULL")
    suspend fun tombstoneMember(id: Long, ownerId: String)

    @Query("DELETE FROM teams WHERE id = :id AND ownerId = :ownerId AND serverId IS NULL")
    suspend fun deleteLocalOnlyTeam(id: Long, ownerId: String)

    @Query("UPDATE teams SET syncState = 2 WHERE id = :id AND ownerId = :ownerId AND serverId IS NOT NULL")
    suspend fun tombstoneTeam(id: Long, ownerId: String)

    @Query("DELETE FROM events WHERE id = :id AND ownerId = :ownerId AND serverId IS NULL")
    suspend fun deleteLocalOnlyEvent(id: Long, ownerId: String)

    @Query("UPDATE events SET syncState = 2 WHERE id = :id AND ownerId = :ownerId AND serverId IS NOT NULL")
    suspend fun tombstoneEvent(id: Long, ownerId: String)

    // ---- what the sync engine uses ----
    @Query("SELECT * FROM members WHERE ownerId = :ownerId AND syncState != 0 ORDER BY id")
    suspend fun pendingMembers(ownerId: String): List<MemberEntity>

    @Query("SELECT * FROM members WHERE ownerId = :ownerId AND serverId = :serverId LIMIT 1")
    suspend fun memberByServerId(ownerId: String, serverId: String): MemberEntity?

    @Query("SELECT * FROM members WHERE ownerId = :ownerId AND clientId = :clientId LIMIT 1")
    suspend fun memberByClientId(ownerId: String, clientId: String): MemberEntity?

    @Update suspend fun updateMember(member: MemberEntity)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteMemberRow(id: Long)

    @Query("SELECT * FROM teams WHERE ownerId = :ownerId AND syncState != 0 ORDER BY id")
    suspend fun pendingTeams(ownerId: String): List<TeamEntity>

    @Query("SELECT * FROM teams WHERE ownerId = :ownerId AND serverId = :serverId LIMIT 1")
    suspend fun teamByServerId(ownerId: String, serverId: String): TeamEntity?

    @Query("SELECT * FROM teams WHERE ownerId = :ownerId AND clientId = :clientId LIMIT 1")
    suspend fun teamByClientId(ownerId: String, clientId: String): TeamEntity?

    @Update suspend fun updateTeam(team: TeamEntity)

    @Query("DELETE FROM teams WHERE id = :id")
    suspend fun deleteTeamRow(id: Long)

    @Query("SELECT * FROM events WHERE ownerId = :ownerId AND syncState != 0 ORDER BY id")
    suspend fun pendingEvents(ownerId: String): List<EventEntity>

    @Query("SELECT * FROM events WHERE ownerId = :ownerId AND serverId = :serverId LIMIT 1")
    suspend fun eventByServerId(ownerId: String, serverId: String): EventEntity?

    @Query("SELECT * FROM events WHERE ownerId = :ownerId AND clientId = :clientId LIMIT 1")
    suspend fun eventByClientId(ownerId: String, clientId: String): EventEntity?

    @Update suspend fun updateEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventRow(id: Long)
}

/** Same call sites as before the migration: delete a synced row = tombstone, an unsynced one = remove. */
internal suspend fun OrganizationDao.deleteMember(id: Long, ownerId: String) {
    deleteLocalOnlyMember(id, ownerId)
    tombstoneMember(id, ownerId)
}

internal suspend fun OrganizationDao.deleteTeam(id: Long, ownerId: String) {
    deleteLocalOnlyTeam(id, ownerId)
    tombstoneTeam(id, ownerId)
}

internal suspend fun OrganizationDao.deleteEvent(id: Long, ownerId: String) {
    deleteLocalOnlyEvent(id, ownerId)
    tombstoneEvent(id, ownerId)
}

@Database(
    entities = [MemberEntity::class, TeamEntity::class, EventEntity::class],
    version = 2,
    exportSchema = true,
)
internal abstract class OrganizationDatabase : RoomDatabase() {
    abstract fun dao(): OrganizationDao

    companion object {
        @Volatile private var instance: OrganizationDatabase? = null

        fun get(context: Context): OrganizationDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, OrganizationDatabase::class.java, "organization.db")
                // v1 was the offline-only schema (ownerId = old local user ids, which no longer exist).
                // Pre-production: those rows are dropped on purpose. From v2 on, write real Migrations.
                .fallbackToDestructiveMigrationFrom(true, 1)
                .build().also { instance = it }
        }
    }
}
