package org.example.modules.business.data

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







@Entity(
    tableName = "customers",
    indices = [Index("ownerId"), Index(value = ["clientId"], unique = true), Index(value = ["ownerId", "serverId"], unique = true)],
)
internal data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
    val ownerId: String,
    val name: String,
    override val clientId: String = UUID.randomUUID().toString(),
    override val serverId: String? = null,
    override val version: Long = 0,
    override val syncState: Int = SyncState.PENDING_UPSERT,
) : SyncRow

@Entity(
    tableName = "orders",
    indices = [Index("ownerId"), Index(value = ["clientId"], unique = true), Index(value = ["ownerId", "serverId"], unique = true)],
)
internal data class OrderEntity(
    @PrimaryKey(autoGenerate = true) override val id: Long = 0,
    val ownerId: String,
    val title: String,
    val customerName: String?,
    
    val amountCents: Long,
    val createdAt: Long,
    override val clientId: String = UUID.randomUUID().toString(),
    override val serverId: String? = null,
    override val version: Long = 0,
    override val syncState: Int = SyncState.PENDING_UPSERT,
) : SyncRow

@Dao
internal interface BusinessDao {
    
    @Query("SELECT * FROM customers WHERE ownerId = :ownerId AND syncState != 2 ORDER BY id DESC")
    fun customers(ownerId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM orders WHERE ownerId = :ownerId AND syncState != 2 ORDER BY createdAt DESC")
    fun orders(ownerId: String): Flow<List<OrderEntity>>

    @Insert
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    
    @Query("DELETE FROM customers WHERE id = :id AND ownerId = :ownerId AND serverId IS NULL")
    suspend fun deleteLocalOnlyCustomer(id: Long, ownerId: String)

    
    @Query("UPDATE customers SET syncState = 2 WHERE id = :id AND ownerId = :ownerId AND serverId IS NOT NULL")
    suspend fun tombstoneCustomer(id: Long, ownerId: String)

    @Query("DELETE FROM orders WHERE id = :id AND ownerId = :ownerId AND serverId IS NULL")
    suspend fun deleteLocalOnlyOrder(id: Long, ownerId: String)

    @Query("UPDATE orders SET syncState = 2 WHERE id = :id AND ownerId = :ownerId AND serverId IS NOT NULL")
    suspend fun tombstoneOrder(id: Long, ownerId: String)

    
    @Query("SELECT * FROM customers WHERE ownerId = :ownerId AND syncState != 0 ORDER BY id")
    suspend fun pendingCustomers(ownerId: String): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE ownerId = :ownerId AND serverId = :serverId LIMIT 1")
    suspend fun customerByServerId(ownerId: String, serverId: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE ownerId = :ownerId AND clientId = :clientId LIMIT 1")
    suspend fun customerByClientId(ownerId: String, clientId: String): CustomerEntity?

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerRow(id: Long)

    @Query("SELECT * FROM orders WHERE ownerId = :ownerId AND syncState != 0 ORDER BY id")
    suspend fun pendingOrders(ownerId: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE ownerId = :ownerId AND serverId = :serverId LIMIT 1")
    suspend fun orderByServerId(ownerId: String, serverId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE ownerId = :ownerId AND clientId = :clientId LIMIT 1")
    suspend fun orderByClientId(ownerId: String, clientId: String): OrderEntity?

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrderRow(id: Long)
}


internal suspend fun BusinessDao.deleteCustomer(id: Long, ownerId: String) {
    deleteLocalOnlyCustomer(id, ownerId)
    tombstoneCustomer(id, ownerId)
}

internal suspend fun BusinessDao.deleteOrder(id: Long, ownerId: String) {
    deleteLocalOnlyOrder(id, ownerId)
    tombstoneOrder(id, ownerId)
}

@Database(entities = [CustomerEntity::class, OrderEntity::class], version = 3, exportSchema = true)
internal abstract class BusinessDatabase : RoomDatabase() {
    abstract fun dao(): BusinessDao

    companion object {
        @Volatile private var instance: BusinessDatabase? = null

        fun get(context: Context): BusinessDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, BusinessDatabase::class.java, "business.db")
                
                
                .fallbackToDestructiveMigrationFrom(true, 1, 2)
                .build().also { instance = it }
        }
    }
}