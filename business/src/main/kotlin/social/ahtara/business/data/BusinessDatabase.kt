package social.ahtara.business.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Entity
import kotlinx.coroutines.flow.Flow


@Entity(tableName = "customers", indices = [Index("ownerId")])
internal data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String,
    val name: String,
)

@Entity(tableName = "orders", indices = [Index("ownerId")])
internal data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String,
    val title: String,
    val customerName: String?,
    
    val amountCents: Long,
    val createdAt: Long,
)






@Entity(
    tableName = "listings",
    indices = [Index("ownerId"), Index(value = ["importedId"], unique = true)],
)
internal data class ListingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String,
    val importedId: String?,
    val name: String,
    
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val description: String?,
    val hours: String?,
    val phone: String?,
    val website: String?,
    val createdAt: Long,
)


@Entity(tableName = "events", indices = [Index("ownerId")])
internal data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String,
    val title: String,
    val description: String?,
    val startMillis: Long,
    val endMillis: Long?,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val website: String?,
)

@Dao
internal interface BusinessDao {
    @Query("SELECT * FROM customers WHERE ownerId = :ownerId ORDER BY id DESC")
    fun customers(ownerId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM orders WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun orders(ownerId: String): Flow<List<OrderEntity>>

    @Insert
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Query("DELETE FROM customers WHERE id = :id AND ownerId = :ownerId")
    suspend fun deleteCustomer(id: Long, ownerId: String)

    @Query("DELETE FROM orders WHERE id = :id AND ownerId = :ownerId")
    suspend fun deleteOrder(id: Long, ownerId: String)

    @Query("SELECT * FROM listings WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun listings(ownerId: String): Flow<List<ListingEntity>>

    @Query("SELECT * FROM events WHERE ownerId = :ownerId ORDER BY startMillis DESC")
    fun events(ownerId: String): Flow<List<EventEntity>>

    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertListing(listing: ListingEntity): Long

    @Insert
    suspend fun insertEvent(event: EventEntity): Long

    @Query("DELETE FROM listings WHERE id = :id AND ownerId = :ownerId")
    suspend fun deleteListing(id: Long, ownerId: String)

    @Query("DELETE FROM events WHERE id = :id AND ownerId = :ownerId")
    suspend fun deleteEvent(id: Long, ownerId: String)

    
    @Query("SELECT * FROM listings ORDER BY createdAt DESC")
    suspend fun allListings(): List<ListingEntity>

    
    @Query("SELECT * FROM events WHERE COALESCE(endMillis, startMillis + 86400000) >= :now ORDER BY startMillis ASC")
    suspend fun upcomingEvents(now: Long): List<EventEntity>
}

@Database(
    entities = [CustomerEntity::class, OrderEntity::class, ListingEntity::class, EventEntity::class],
    version = 3,
    exportSchema = true,
)
internal abstract class BusinessDatabase : RoomDatabase() {
    abstract fun dao(): BusinessDao

    companion object {
        @Volatile private var instance: BusinessDatabase? = null

        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE customers_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ownerId TEXT NOT NULL, name TEXT NOT NULL)")
                db.execSQL("INSERT INTO customers_new (id, ownerId, name) SELECT id, ownerId, name FROM customers")
                db.execSQL("DROP TABLE customers")
                db.execSQL("ALTER TABLE customers_new RENAME TO customers")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_ownerId ON customers (ownerId)")
            }
        }

        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS listings (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "ownerId TEXT NOT NULL, importedId TEXT, name TEXT NOT NULL, category TEXT NOT NULL, " +
                        "latitude REAL NOT NULL, longitude REAL NOT NULL, description TEXT, hours TEXT, " +
                        "phone TEXT, website TEXT, createdAt INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_listings_ownerId ON listings (ownerId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_listings_importedId ON listings (importedId)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "ownerId TEXT NOT NULL, title TEXT NOT NULL, description TEXT, startMillis INTEGER NOT NULL, " +
                        "endMillis INTEGER, locationName TEXT, latitude REAL, longitude REAL, website TEXT)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_ownerId ON events (ownerId)")
            }
        }

        fun get(context: Context): BusinessDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, BusinessDatabase::class.java, "business.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
        }
    }
}