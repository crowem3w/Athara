package social.ahtara.authentication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import social.ahtara.authentication.R
import social.ahtara.authentication.domain.AccountType





@Composable
fun AuthenticationTheme(accountType: AccountType? = null, content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = colorResource(accountType?.accentColor ?: R.color.authentication_primary),
        onPrimary = colorResource(R.color.authentication_on_primary),
        background = colorResource(R.color.authentication_background),
        onBackground = colorResource(R.color.authentication_on_background),
        surface = colorResource(R.color.authentication_background),
        onSurface = colorResource(R.color.authentication_on_background),
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}