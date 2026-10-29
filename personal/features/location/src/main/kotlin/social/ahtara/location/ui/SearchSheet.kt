package social.ahtara.location.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.authentication.domain.ServiceArea
import social.ahtara.authentication.ui.LocationSearchField
import social.ahtara.authentication.ui.StepPrimaryButton
import social.ahtara.location.R
import social.ahtara.location.search.PlaceCategory
import social.ahtara.location.search.SearchResult






@Composable
fun ColumnScope.LocationPanelContent(
    query: String,
    categories: Set<PlaceCategory>,
    results: List<SearchResult>,
    places: List<PlaceSelection>,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onToggleCategory: (PlaceCategory) -> Unit,
    onResultClick: (SearchResult) -> Unit,
    onRemovePlace: (PlaceSelection) -> Unit,
    onQuickPick: (PlaceSelection) -> Unit = {},
    accountType: social.ahtara.authentication.domain.AccountType = social.ahtara.authentication.domain.AccountType.PERSONAL,
    address: String = "",
    onAddressChange: (String) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current

    LocationSearchField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(R.string.search_hint),
        onSearch = {
            focusManager.clearFocus()
            onSearchSubmit()
        },
    )

    CategoryChips(selected = categories, onToggle = onToggleCategory)

    // Guimaras and its five municipalities, one tap each.
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        // Organizations pick municipalities; Personal / Business can also pick the whole province.
        val picks = if (accountType == social.ahtara.authentication.domain.AccountType.ORGANIZATION)
            ServiceArea.municipalities else ServiceArea.quickPicks
        picks.forEach { pick ->
            FilterChip(
                selected = places.contains(pick),
                onClick = { onQuickPick(pick) },
                label = { Text(pick.name.substringBefore(",")) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }

    if (accountType != social.ahtara.authentication.domain.AccountType.PERSONAL) {
        social.ahtara.authentication.ui.LocationAddressField(
            value = address,
            onValueChange = onAddressChange,
            placeholder = stringResource(R.string.address_hint),
        )
    }

    if (results.isNotEmpty()) {
        Column {
            results.forEach { result ->
                ResultRow(result) {
                    focusManager.clearFocus()
                    onResultClick(result)
                }
            }
        }
    }

    if (places.isEmpty()) {
        if (results.isEmpty()) {
            Text(
                text = stringResource(
                    when (accountType) {
                        social.ahtara.authentication.domain.AccountType.ORGANIZATION -> R.string.pick_hint_municipalities
                        social.ahtara.authentication.domain.AccountType.BUSINESS -> R.string.pick_hint_single
                        else -> R.string.pick_hint
                    },
                ),
                color = Subtle,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    } else {
        Text(
            text = stringResource(R.string.shortlist_title, places.size),
            color = Subtle,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Column {
            places.forEach { place -> ShortlistRow(place, onRemove = { onRemovePlace(place) }) }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChips(selected: Set<PlaceCategory>, onToggle: (PlaceCategory) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PlaceCategory.entries.sortedBy { it.chipOrder() }.forEach { category ->
            FilterChip(
                selected = category in selected,
                onClick = { onToggle(category) },
                label = { Text(stringResource(category.chipLabel())) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

private fun PlaceCategory.chipOrder(): Int = when (this) {
    PlaceCategory.COUNTRY -> 0
    PlaceCategory.CITY -> 1
    PlaceCategory.PROVINCE -> 2
}

private fun PlaceCategory.chipLabel(): Int = when (this) {
    PlaceCategory.COUNTRY -> R.string.category_countries
    PlaceCategory.CITY -> R.string.category_cities
    PlaceCategory.PROVINCE -> R.string.category_provinces
}


@Composable
fun ColumnScope.LocationPanelFooter(
    canConfirm: Boolean,
    onConfirm: () -> Unit,
    onSkip: (() -> Unit)? = null,
) {
    StepPrimaryButton(
        text = stringResource(R.string.confirm_location),
        onClick = onConfirm,
        enabled = canConfirm,
    )
    if (onSkip != null) {
        androidx.compose.material3.TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.skip_location)) }
    }
}

@Composable
private fun ShortlistRow(place: PlaceSelection, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PinBadge(tint = MaterialTheme.colorScheme.primary)
        Text(
            text = place.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        
        IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.cd_remove_place, place.name),
                tint = Subtle,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PinBadge(tint: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier.size(36.dp).background(IconBackground, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_result_pin),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ResultRow(result: SearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PinBadge(tint = result.category.tint())
        Column(
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = result.label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!result.subtitle.isNullOrBlank()) {
                Text(
                    text = result.subtitle,
                    color = Subtle,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}