package soc.athara.app.shared.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import soc.athara.app.core.identityapi.AccountType





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