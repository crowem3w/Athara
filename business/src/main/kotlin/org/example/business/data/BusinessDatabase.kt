package org.example.business.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Entity
import kotlinx.coroutines.flow.Flow

/** Every row belongs to one signed-in user ([ownerId] = AuthUser.id), so accounts never see each other's data. */
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
    /** Money is stored as integer minor units (cents) to avoid floating-point errors. */
    val amountCents: Long,
    val createdAt: Long,
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
}

@Database(entities = [CustomerEntity::class, OrderEntity::class], version = 2, exportSchema = true)
internal abstract class BusinessDatabase : RoomDatabase() {
    abstract fun dao(): BusinessDao

    companion object {
        @Volatile private var instance: BusinessDatabase? = null

        /** v2: customers no longer store an email address. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE customers_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ownerId TEXT NOT NULL, name TEXT NOT NULL)")
                db.execSQL("INSERT INTO customers_new (id, ownerId, name) SELECT id, ownerId, name FROM customers")
                db.execSQL("DROP TABLE customers")
                db.execSQL("ALTER TABLE customers_new RENAME TO customers")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_ownerId ON customers (ownerId)")
            }
        }

        fun get(context: Context): BusinessDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, BusinessDatabase::class.java, "business.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }
    }
}
