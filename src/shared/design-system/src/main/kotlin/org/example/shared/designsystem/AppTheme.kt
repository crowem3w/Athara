package org.example.shared.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import org.example.core.identityapi.AccountType

/**
 * The app's Material theme. Pass [accountType] to use that type's accent color
 * (Personal teal, Business blue, Organization purple).
 */
@Composable
fun AppTheme(accountType: AccountType? = null, content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = colorResource(accountType?.accentColor ?: R.color.designsystem_primary),
        onPrimary = colorResource(R.color.designsystem_on_primary),
        background = colorResource(R.color.designsystem_background),
        onBackground = colorResource(R.color.designsystem_on_background),
        surface = colorResource(R.color.designsystem_background),
        onSurface = colorResource(R.color.designsystem_on_background),
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}
