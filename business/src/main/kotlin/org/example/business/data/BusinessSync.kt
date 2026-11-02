package org.example.business.data

import android.content.Context
import java.util.UUID
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.example.core.net.RecordDto
import org.example.core.sync.SyncCoordinator
import org.example.core.sync.SyncState
import org.example.core.sync.TableSync
import org.example.core.sync.long
import org.example.core.sync.str

internal class CustomerSync(private val dao: BusinessDao) : TableSync<CustomerEntity>("customers") {
    override suspend fun pending(ownerId: String) = dao.pendingCustomers(ownerId)
    override suspend fun byServerId(ownerId: String, serverId: String) = dao.customerByServerId(ownerId, serverId)
    override suspend fun byClientId(ownerId: String, clientId: String) = dao.customerByClientId(ownerId, clientId)
    override suspend fun insert(row: CustomerEntity) { dao.insertCustomer(row) }
    override suspend fun update(row: CustomerEntity) = dao.updateCustomer(row)
    override suspend fun delete(id: Long) = dao.deleteCustomerRow(id)

    override fun toData(row: CustomerEntity): JsonObject = buildJsonObject { put("name", row.name) }

    override fun fromRemote(ownerId: String, existing: CustomerEntity?, record: RecordDto): CustomerEntity =
        (existing ?: CustomerEntity(ownerId = ownerId, name = "")).copy(
            name = record.data.str("name").orEmpty(),
            clientId = record.clientId ?: existing?.clientId ?: UUID.randomUUID().toString(),
            serverId = record.serverId,
            version = record.version,
            syncState = SyncState.SYNCED,
        )

    override fun markSynced(row: CustomerEntity) = row.copy(syncState = SyncState.SYNCED)
}

internal class OrderSync(private val dao: BusinessDao) : TableSync<OrderEntity>("orders") {
    override suspend fun pending(ownerId: String) = dao.pendingOrders(ownerId)
    override suspend fun byServerId(ownerId: String, serverId: String) = dao.orderByServerId(ownerId, serverId)
    override suspend fun byClientId(ownerId: String, clientId: String) = dao.orderByClientId(ownerId, clientId)
    override suspend fun insert(row: OrderEntity) { dao.insertOrder(row) }
    override suspend fun update(row: OrderEntity) = dao.updateOrder(row)
    override suspend fun delete(id: Long) = dao.deleteOrderRow(id)

    override fun toData(row: OrderEntity): JsonObject = buildJsonObject {
        put("title", row.title)
        put("customerName", row.customerName)
        put("amountCents", row.amountCents)
        put("createdAt", row.createdAt)
    }

    override fun fromRemote(ownerId: String, existing: OrderEntity?, record: RecordDto): OrderEntity =
        (existing ?: OrderEntity(ownerId = ownerId, title = "", customerName = null, amountCents = 0, createdAt = 0)).copy(
            title = record.data.str("title").orEmpty(),
            customerName = record.data.str("customerName"),
            amountCents = record.data.long("amountCents") ?: 0,
            createdAt = record.data.long("createdAt") ?: 0,
            clientId = record.clientId ?: existing?.clientId ?: UUID.randomUUID().toString(),
            serverId = record.serverId,
            version = record.version,
            syncState = SyncState.SYNCED,
        )

    override fun markSynced(row: OrderEntity) = row.copy(syncState = SyncState.SYNCED)
}

/** Registers this module's tables with the sync engine. Call once from Application.onCreate. */
object BusinessSync {
    fun install(context: Context) {
        val dao = BusinessDatabase.get(context).dao()
        SyncCoordinator.register(CustomerSync(dao))
        SyncCoordinator.register(OrderSync(dao))
    }
}
