package social.ahtara.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import social.ahtara.core.ui.theme.Teal
import social.ahtara.core.ui.theme.TextSecondary
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.travel.model.PlaceCategory


internal fun PlaceSelection.feedKey(): String = "$name|$latitude|$longitude"


private fun PlaceSelection.shortName(): String = name.substringBefore(',').trim().ifBlank { name }







@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlacesScreen(
    places: List<PlaceSelection>,
    activeKeys: Set<String>,
    categories: Set<PlaceCategory>,
    onTogglePlace: (PlaceSelection) -> Unit,
    onRemovePlace: (PlaceSelection) -> Unit,
    onAddPlace: () -> Unit,
    onToggleCategory: (PlaceCategory) -> Unit,
    onClearCategories: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Places", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            ) {
                
                Text("Locations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Tick the places your feed should show. With none ticked, all of them are shown.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                if (places.isEmpty()) {
                    Text("No saved places yet.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
                }
                places.forEachIndexed { index, place ->
                    val checked = activeKeys.isEmpty() || place.feedKey() in activeKeys
                    Row(
                        Modifier.fillMaxWidth().clickable { onTogglePlace(place) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { onTogglePlace(place) })
                        Column(Modifier.weight(1f)) {
                            Text(place.shortName(), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (index == 0) Text("Main location", style = MaterialTheme.typography.bodySmall, color = Teal)
                        }
                        IconButton(onClick = { onRemovePlace(place) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove ${place.shortName()}")
                        }
                    }
                }
                OutlinedButton(onClick = onAddPlace, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Add location")
                }

                Spacer(Modifier.height(28.dp))

                
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (categories.isNotEmpty()) TextButton(onClick = onClearCategories) { Text("Show all") }
                }
                Text(
                    "Pick one or more. With none picked, every category is shown.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PlaceCategory.entries.forEach { category ->
                        FilterChip(
                            selected = category in categories,
                            onClick = { onToggleCategory(category) },
                            label = { Text(category.label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Teal, selectedLabelColor = androidx.compose.ui.graphics.Color.White),
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}