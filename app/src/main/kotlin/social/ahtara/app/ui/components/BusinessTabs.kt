package social.ahtara.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import social.ahtara.app.ui.theme.SurfaceMuted
import social.ahtara.app.ui.theme.Teal
import social.ahtara.app.ui.theme.TextPrimary

/**
 * PLACEHOLDER tabs for the Business home, taken from the Business account description
 * ("customers, orders and revenue"). Replace with the real list.
 */
enum class BusinessTab(val label: String) {
    OVERVIEW("Overview"),
    CUSTOMERS("Customers"),
    ORDERS("Orders"),
    REVENUE("Revenue"),
}

@Composable
fun BusinessTabs(
    selected: BusinessTab,
    onSelect: (BusinessTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        BusinessTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) Color.White else TextPrimary,
                maxLines = 1,
                modifier = Modifier
                    .padding(end = 10.dp)
                    .clip(CircleShape)
                    .background(color = if (isSelected) Teal else SurfaceMuted, shape = CircleShape)
                    .clickable(role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}
