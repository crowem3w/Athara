package soc.athara.app.modules.business.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// First-pass UI models. Replace SampleData with real repositories (API / Room) when the
// backend endpoints for bookings, offerings and earnings exist.

enum class BookingStatus(val label: String) {
    Pending("Pending"),
    Confirmed("Confirmed"),
    InProgress("In progress"),
    Completed("Completed"),
    Cancelled("Cancelled"),
}

data class Booking(
    val ref: String,
    val customer: String,
    val offering: String,
    /** Days from today (0 = today). */
    val day: Int,
    val time: String,
    val guests: Int,
    val total: Long,
    val paid: Boolean,
    val note: String?,
    val status: BookingStatus,
)

enum class OfferingType(val label: String) { Product("Products"), Service("Services") }

data class Offering(
    val id: Int,
    val name: String,
    val type: OfferingType,
    val price: Long,
    val available: Boolean = true,
    val stock: Int? = null,
    val durationMin: Int? = null,
    val capacity: Int? = null,
) {
    val lowStock: Boolean get() = type == OfferingType.Product && stock != null && stock <= 5
}

enum class DashPeriod(val label: String) { Today("Today"), Week("7 days"), Month("30 days") }

data class PeriodStats(val revenue: Long, val bookings: Int, val orders: Int, val trend: List<Float>)

enum class ActivityKind { Booking, Payment, Order }

data class Activity(val kind: ActivityKind, val title: String, val detail: String)

fun peso(amount: Long): String = "\u20B1" + "%,d".format(amount)

fun dayLabel(day: Int): String = when (day) {
    0 -> "Today"
    1 -> "Tomorrow"
    else -> LocalDate.now().plusDays(day.toLong()).let {
        "${it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, ${it.dayOfMonth}"
    }
}

/** Everything the business tabs share. Hoisted so Home shortcuts and tabs stay in sync. */
class BusinessState(val supportsOrders: Boolean = true) {
    val bookings = mutableStateListOf<Booking>().apply { addAll(SampleData.bookings) }
    val offerings = mutableStateListOf<Offering>().apply { addAll(SampleData.offerings) }
    var isOpen by mutableStateOf(true)

    val newOrders: Int = if (supportsOrders) 2 else 0
    val pendingCount: Int get() = bookings.count { it.status == BookingStatus.Pending }
    val lowStock: List<Offering> get() = offerings.filter { it.lowStock }

    fun stats(period: DashPeriod): PeriodStats = SampleData.stats.getValue(period)

    fun setStatus(ref: String, status: BookingStatus) {
        val i = bookings.indexOfFirst { it.ref == ref }
        if (i >= 0) bookings[i] = bookings[i].copy(status = status)
    }

    fun save(offering: Offering) {
        if (offering.id == 0) {
            offerings.add(offering.copy(id = (offerings.maxOfOrNull { it.id } ?: 0) + 1))
        } else {
            val i = offerings.indexOfFirst { it.id == offering.id }
            if (i >= 0) offerings[i] = offering
        }
    }

    fun delete(id: Int) {
        offerings.removeAll { it.id == id }
    }
}

internal object SampleData {
    val bookings = listOf(
        Booking("BK-1042", "Maria Santos", "Island hopping tour", 0, "9:00 AM", 4, 6400, true, "Two kids, we need life vests.", BookingStatus.Pending),
        Booking("BK-1041", "Jon Reyes", "Sunset kayak", 0, "4:30 PM", 2, 2400, true, null, BookingStatus.Confirmed),
        Booking("BK-1038", "Dana Uy", "Cooking class", 0, "8:00 AM", 1, 900, true, null, BookingStatus.Completed),
        Booking("BK-1039", "Aiko Tanaka", "Deluxe room, 2 nights", 1, "2:00 PM", 2, 7800, false, "Late check-in around 9 PM.", BookingStatus.Pending),
        Booking("BK-1036", "Liam Cruz", "Scooter rental", 2, "10:00 AM", 1, 900, true, null, BookingStatus.Confirmed),
        Booking("BK-1033", "Ines Lopez", "Island hopping tour", 4, "9:00 AM", 6, 9600, true, null, BookingStatus.Confirmed),
        Booking("BK-1029", "Paolo Diaz", "Sunset kayak", 5, "4:30 PM", 2, 2400, false, null, BookingStatus.Cancelled),
    )

    val offerings = listOf(
        Offering(1, "Island hopping tour", OfferingType.Service, 1600, durationMin = 480, capacity = 12),
        Offering(2, "Sunset kayak", OfferingType.Service, 1200, durationMin = 90, capacity = 8),
        Offering(3, "Scooter rental", OfferingType.Service, 900, durationMin = 1440, capacity = 5),
        Offering(4, "Coconut jam jar", OfferingType.Product, 180, stock = 24),
        Offering(5, "Handwoven tote", OfferingType.Product, 650, stock = 3),
        Offering(6, "Dried mango pack", OfferingType.Product, 220, available = false, stock = 0),
    )

    val stats = mapOf(
        DashPeriod.Today to PeriodStats(8_400, 5, 3, listOf(1f, 2f, 2f, 4f, 3f, 5f, 6f)),
        DashPeriod.Week to PeriodStats(46_200, 31, 18, listOf(5f, 7f, 6f, 9f, 8f, 11f, 10f)),
        DashPeriod.Month to PeriodStats(184_900, 122, 74, listOf(20f, 26f, 24f, 31f, 29f, 38f, 41f)),
    )

    val activity = listOf(
        Activity(ActivityKind.Payment, "Payment received", "\u20B1850 from J. Reyes, 10 min ago"),
        Activity(ActivityKind.Booking, "New booking request", "Island hopping tour, 4 guests"),
        Activity(ActivityKind.Order, "Order picked up", "Coconut jam jar x2, yesterday"),
    )
}
