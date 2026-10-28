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
import social.ahtara.core.ui.theme.SurfaceMuted
import social.ahtara.core.ui.theme.Teal
import social.ahtara.core.ui.theme.TextPrimary
import social.ahtara.travel.model.PlaceCategory






@Composable
fun FilterTabs(
    selected: Set<PlaceCategory>,
    onToggle: (PlaceCategory) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        CategoryChip(label = "All", selected = selected.isEmpty(), onClick = onClear)
        PlaceCategory.entries.forEach { category ->
            CategoryChip(
                label = category.label,
                selected = category in selected,
                onClick = { onToggle(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (selected) Teal else SurfaceMuted
    val contentColor = if (selected) Color.White else TextPrimary
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = contentColor,
        maxLines = 1,
        modifier = Modifier
            .padding(end = 10.dp)
            .clip(CircleShape)
            .background(color = backgroundColor, shape = CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}