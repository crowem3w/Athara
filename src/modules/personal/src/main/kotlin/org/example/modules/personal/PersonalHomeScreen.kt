package org.example.modules.personal

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.example.modules.personal.features.travel.model.PlaceCategory
import org.example.modules.personal.features.travel.model.ServiceArea
import org.example.core.identityapi.AuthUser
import org.example.modules.personal.home.HomeScreen
import org.example.shared.designsystem.theme.AhtaraTheme

private val CategorySaver = listSaver<Set<PlaceCategory>, String>(
    save = { selected -> selected.map { it.name } },
    restore = { names -> names.mapNotNull { PlaceCategory.fromName(it) }.toSet() },
)

/**
 * Entry point of the Personal home. Owns the screen-level state (category filter, profile sheet);
 * layout and feed behaviour live in [HomeScreen].
 */
@Composable
internal fun PersonalHomeScreen(user: AuthUser, onSignOut: () -> Unit) {
    var categories by rememberSaveable(stateSaver = CategorySaver) { mutableStateOf(emptySet<PlaceCategory>()) }
    var showProfile by rememberSaveable { mutableStateOf(false) }

    AhtaraTheme {
        HomeScreen(
            // Default service area until place picking exists; use emptyList() to see the "add a place" state.
            places = listOf(ServiceArea.default),
            categories = categories,
            onToggleCategory = { category ->
                categories = if (category in categories) categories - category else categories + category
            },
            onClearCategories = { categories = emptySet() },
            userInitial = user.displayName.trim().firstOrNull()?.uppercase() ?: "?",
            onProfileClick = { showProfile = true },
        )

        if (showProfile) {
            AlertDialog(
                onDismissRequest = { showProfile = false },
                title = { Text(user.displayName) },
                text = { Text(stringResource(R.string.personal_profile_login, user.identifier)) },
                confirmButton = {
                    TextButton(onClick = { showProfile = false; onSignOut() }) {
                        Text(stringResource(R.string.personal_action_sign_out))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showProfile = false }) {
                        Text(stringResource(R.string.personal_profile_close))
                    }
                },
            )
        }
    }
}
