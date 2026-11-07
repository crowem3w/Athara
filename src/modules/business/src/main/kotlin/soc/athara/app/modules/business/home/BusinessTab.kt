package soc.athara.app.modules.business.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

/** The four destinations around the centre "Collect payment" button. */
internal enum class BusinessTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.Home),
    Bookings("Bookings", Icons.Outlined.CalendarMonth),
    Store("My Store", Icons.Outlined.Storefront),
    Account("Account", Icons.Outlined.AccountCircle),
}
