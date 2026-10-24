package social.ahtara.websites.data

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


@Entity(tableName = "websites", indices = [Index("ownerId")])
internal data class WebsiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String,
    val name: String,
    val url: String,
    
    val category: String?,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long,
)

@Dao
internal interface WebsiteDao {
    @Query("SELECT * FROM websites WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun websites(ownerId: String): Flow<List<WebsiteEntity>>

    @Query("SELECT COUNT(*) FROM websites WHERE ownerId = :ownerId")
    suspend fun count(ownerId: String): Int

    @Insert
    suspend fun insert(website: WebsiteEntity): Long

    @Query("DELETE FROM websites WHERE id = :id AND ownerId = :ownerId")
    suspend fun delete(id: Long, ownerId: String)

    
    @Query("SELECT * FROM websites ORDER BY createdAt DESC")
    suspend fun all(): List<WebsiteEntity>
}

@Database(entities = [WebsiteEntity::class], version = 1, exportSchema = true)
internal abstract class WebsiteDatabase : RoomDatabase() {
    abstract fun dao(): WebsiteDao

    companion object {
        @Volatile private var instance: WebsiteDatabase? = null

        fun get(context: Context): WebsiteDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, WebsiteDatabase::class.java, "websites.db")
                .build().also { instance = it }
        }
    }
}