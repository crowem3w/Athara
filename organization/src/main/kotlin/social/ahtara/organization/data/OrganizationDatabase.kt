package social.ahtara.organization.data

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
import kotlinx.coroutines.flow.Flow


@Entity(tableName = "members", indices = [Index("ownerId")])
internal data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String,
    val name: String,
    val role: String?,
)

@Entity(tableName = "teams", indices = [Index("ownerId")])
internal data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String,
    val name: String,
)

@Entity(tableName = "events", indices = [Index("ownerId")])
internal data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String,
    val title: String,
    
    val date: String,
)

@Dao
internal interface OrganizationDao {
    @Query("SELECT * FROM members WHERE ownerId = :ownerId ORDER BY name COLLATE NOCASE")
    fun members(ownerId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM teams WHERE ownerId = :ownerId ORDER BY name COLLATE NOCASE")
    fun teams(ownerId: String): Flow<List<TeamEntity>>

    @Query("SELECT * FROM events WHERE ownerId = :ownerId ORDER BY date ASC")
    fun events(ownerId: String): Flow<List<EventEntity>>

    @Insert suspend fun insertMember(member: MemberEntity): Long
    @Insert suspend fun insertTeam(team: TeamEntity): Long
    @Insert suspend fun insertEvent(event: EventEntity): Long

    @Query("DELETE FROM members WHERE id = :id AND ownerId = :ownerId")
    suspend fun deleteMember(id: Long, ownerId: String)

    @Query("DELETE FROM teams WHERE id = :id AND ownerId = :ownerId")
    suspend fun deleteTeam(id: Long, ownerId: String)

    @Query("DELETE FROM events WHERE id = :id AND ownerId = :ownerId")
    suspend fun deleteEvent(id: Long, ownerId: String)
}

@Database(
    entities = [MemberEntity::class, TeamEntity::class, EventEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class OrganizationDatabase : RoomDatabase() {
    abstract fun dao(): OrganizationDao

    companion object {
        @Volatile private var instance: OrganizationDatabase? = null

        fun get(context: Context): OrganizationDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, OrganizationDatabase::class.java, "organization.db")
                .build().also { instance = it }
        }
    }
}