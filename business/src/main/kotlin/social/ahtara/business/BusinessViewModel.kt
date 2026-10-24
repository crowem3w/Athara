package social.ahtara.business

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
import social.ahtara.business.data.BusinessDao
import social.ahtara.business.data.BusinessDatabase
import social.ahtara.business.data.CustomerEntity
import social.ahtara.business.data.EventEntity
import social.ahtara.business.data.ListingEntity
import social.ahtara.business.data.OrderEntity

internal data class BusinessUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val orders: List<OrderEntity> = emptyList(),
    val listings: List<ListingEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
) {
    val revenueCents: Long get() = orders.sumOf { it.amountCents }
}

internal class BusinessViewModel(
    private val dao: BusinessDao,
    private val ownerId: String,
) : ViewModel() {

    val state: StateFlow<BusinessUiState> =
        combine(dao.customers(ownerId), dao.orders(ownerId), dao.listings(ownerId), dao.events(ownerId)) { customers, orders, listings, events ->
            BusinessUiState(customers, orders, listings, events)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BusinessUiState())

    fun addCustomer(name: String) {
        viewModelScope.launch {
            dao.insertCustomer(CustomerEntity(ownerId = ownerId, name = name.trim()))
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
        }
    }

    fun deleteCustomer(id: Long) {
        viewModelScope.launch { dao.deleteCustomer(id, ownerId) }
    }

    fun deleteOrder(id: Long) {
        viewModelScope.launch { dao.deleteOrder(id, ownerId) }
    }

    



    fun saveListing(
        importedId: String?, name: String, category: String, latitude: Double, longitude: Double,
        description: String, hours: String, phone: String, website: String,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            val id = dao.insertListing(
                ListingEntity(
                    ownerId = ownerId, importedId = importedId?.trim()?.ifBlank { null }, name = name.trim(),
                    category = category, latitude = latitude, longitude = longitude,
                    description = description.trim().ifBlank { null }, hours = hours.trim().ifBlank { null },
                    phone = phone.trim().ifBlank { null }, website = normalizeWebsite(website),
                    createdAt = System.currentTimeMillis(),
                )
            )
            onResult(id != -1L)
        }
    }

    fun deleteListing(id: Long) {
        viewModelScope.launch { dao.deleteListing(id, ownerId) }
    }

    fun addEvent(
        title: String, description: String, startMillis: Long, endMillis: Long?,
        locationName: String, latitude: Double?, longitude: Double?, website: String,
    ) {
        viewModelScope.launch {
            dao.insertEvent(
                EventEntity(
                    ownerId = ownerId, title = title.trim(), description = description.trim().ifBlank { null },
                    startMillis = startMillis, endMillis = endMillis,
                    locationName = locationName.trim().ifBlank { null },
                    latitude = latitude, longitude = longitude, website = normalizeWebsite(website),
                )
            )
        }
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch { dao.deleteEvent(id, ownerId) }
    }

    companion object {
        fun factory(ownerId: String) = viewModelFactory {
            initializer {
                BusinessViewModel(BusinessDatabase.get(this[APPLICATION_KEY]!!).dao(), ownerId)
            }
        }
    }
}


internal fun parseAmountCents(input: String): Long? {
    val value = input.trim().replace(',', '.').toBigDecimalOrNull() ?: return null
    if (value.signum() <= 0 || value > BigDecimal("1000000000") || value.stripTrailingZeros().scale() > 2) return null
    return value.movePointRight(2).toLong()
}


internal fun normalizeWebsite(input: String): String? {
    val t = input.trim()
    if (t.isEmpty()) return null
    val withScheme = if ("://" in t) t else "https://$t"
    val lower = withScheme.lowercase()
    return if (lower.startsWith("https://") || lower.startsWith("http://")) withScheme else null
}


internal fun parseDateMillis(input: String): Long? = runCatching {
    java.time.LocalDate.parse(input.trim()).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
}.getOrNull()