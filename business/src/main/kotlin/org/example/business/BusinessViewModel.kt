package org.example.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.math.BigDecimal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.example.business.data.BusinessDao
import org.example.business.data.BusinessDatabase
import org.example.business.data.CustomerEntity
import org.example.business.data.OrderEntity
import org.example.core.sync.SyncCoordinator
import org.example.business.data.deleteCustomer
import org.example.business.data.deleteOrder

internal data class BusinessUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val orders: List<OrderEntity> = emptyList(),
) {
    val revenueCents: Long get() = orders.sumOf { it.amountCents }
}

internal class BusinessViewModel(
    private val dao: BusinessDao,
    private val ownerId: String,
    /** Called after every local change so the sync engine pushes it as soon as the device is online. */
    private val onChanged: () -> Unit,
) : ViewModel() {

    val state: StateFlow<BusinessUiState> =
        combine(dao.customers(ownerId), dao.orders(ownerId)) { customers, orders ->
            BusinessUiState(customers, orders)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BusinessUiState())

    fun addCustomer(name: String) {
        viewModelScope.launch {
            dao.insertCustomer(CustomerEntity(ownerId = ownerId, name = name.trim()))
            onChanged()
        }
    }

    fun addOrder(title: String, customerName: String, amountCents: Long) {
        viewModelScope.launch {
            dao.insertOrder(
                OrderEntity(
                    ownerId = ownerId,
                    title = title.trim(),
                    customerName = customerName.trim().ifBlank { null },
                    amountCents = amountCents,
                    createdAt = System.currentTimeMillis(),
                )
            )
            onChanged()
        }
    }

    fun deleteCustomer(id: Long) {
        viewModelScope.launch {
            dao.deleteCustomer(id, ownerId)
            onChanged()
        }
    }

    fun deleteOrder(id: Long) {
        viewModelScope.launch {
            dao.deleteOrder(id, ownerId)
            onChanged()
        }
    }

    companion object {
        fun factory(ownerId: String) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                BusinessViewModel(BusinessDatabase.get(app).dao(), ownerId) { SyncCoordinator.requestSync(app) }
            }
        }
    }
}

/** "12.50" -> 1250. Null when the text is not a positive amount with at most 2 decimals. */
internal fun parseAmountCents(input: String): Long? {
    val value = input.trim().replace(',', '.').toBigDecimalOrNull() ?: return null
    if (value.signum() <= 0 || value > BigDecimal("1000000000") || value.stripTrailingZeros().scale() > 2) return null
    return value.movePointRight(2).toLong()
}
