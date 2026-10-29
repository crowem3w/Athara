package social.ahtara.authentication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import social.ahtara.authentication.R
import social.ahtara.authentication.domain.SocialAccount
import social.ahtara.authentication.social.GoogleAuth
import social.ahtara.authentication.social.GoogleResult





internal data class SocialActions(
    val onAccount: (SocialAccount) -> Unit = {},
    val onApple: () -> Unit = {},
    
    val onError: (Int?) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onSubmit: () -> Unit = {},
) {
    companion object {
        val None = SocialActions()
    }
}







@Composable
internal fun SocialSignUpSection(
    isLoading: Boolean,
    actions: SocialActions,
) {
    val google = rememberGoogleSignIn(actions)
    val muted = colorResource(R.color.authentication_muted)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HorizontalDivider(Modifier.weight(1f), color = colorResource(R.color.authentication_field_border))
            Text(
                text = stringResource(R.string.authentication_or_continue_with),
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
            )
            HorizontalDivider(Modifier.weight(1f), color = colorResource(R.color.authentication_field_border))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SocialSignUpButton(
                label = R.string.authentication_social_google_short,
                icon = R.drawable.authentication_ic_google,
                enabled = !isLoading && !google.picking,
                onClick = google.start,
                modifier = Modifier.weight(1f),
            )
            SocialSignUpButton(
                label = R.string.authentication_social_apple_short,
                icon = R.drawable.authentication_ic_apple,
                enabled = !isLoading && !google.picking,
                onClick = actions.onApple,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = stringResource(R.string.authentication_social_demo_note),
            style = MaterialTheme.typography.bodySmall,
            color = muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SocialSignUpButton(
    label: Int,
    icon: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        modifier = modifier.height(ButtonHeight),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().height(ButtonHeight),
        ) {
            
            Icon(painterResource(icon), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (enabled) 1f else 0.5f),
            )
        }
    }
}


internal class GoogleSignIn(val picking: Boolean, val start: () -> Unit)





@Composable
internal fun rememberGoogleSignIn(actions: SocialActions): GoogleSignIn {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf(false) }
    return GoogleSignIn(
        picking = picking,
        start = {
            if (!picking) {
                picking = true
                scope.launch {
                    when (val result = GoogleAuth.signIn(context)) {
                        is GoogleResult.Success -> actions.onAccount(result.account)
                        GoogleResult.Cancelled -> actions.onError(null)
                        GoogleResult.NotConfigured -> actions.onError(R.string.authentication_error_google_not_configured)
                        GoogleResult.NoAccount -> actions.onError(R.string.authentication_error_google_no_account)
                        GoogleResult.Failed -> actions.onError(R.string.authentication_error_google_failed)
                    }
                    picking = false
                }
            }
        },
    )
}