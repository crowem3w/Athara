package social.ahtara.authentication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import social.ahtara.authentication.R
import social.ahtara.authentication.domain.Country
import social.ahtara.authentication.domain.SocialProvider





@Composable
internal fun ColumnScope.SocialCompleteContent(
    state: AuthUiState,
    onMobile: (String) -> Unit,
    onCountry: (Country) -> Unit,
    actions: SocialActions,
    showTitle: Boolean = true,
) {
    val account = requireNotNull(state.socialAccount)

    if (showTitle) {
        Text(
            text = stringResource(R.string.authentication_social_complete_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
    }
    Text(
        text = stringResource(R.string.authentication_social_complete_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = colorResource(R.color.authentication_muted),
    )

    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            ) {
                Text(
                    text = account.displayName.ifBlank { account.email.orEmpty() }.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName.ifBlank { account.email.orEmpty() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = account.email ?: if (account.provider == SocialProvider.APPLE) {
                        stringResource(R.string.authentication_social_apple_account)
                    } else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorResource(R.color.authentication_muted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    IdentifierInput(
        state = state,
        onMobile = onMobile,
        onCountry = onCountry,
        imeAction = ImeAction.Done,
    )

    state.formError?.let {
        Text(
            text = state.formErrorType
                ?.let { type -> stringResource(it, stringResource(type.title)) }
                ?: stringResource(it),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    Button(
        onClick = actions.onSubmit,
        enabled = !state.isLoading,
        modifier = Modifier.fillMaxWidth().height(ButtonHeight),
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(stringResource(R.string.authentication_social_complete_action))
        }
    }

    TextButton(
        onClick = actions.onCancel,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    ) { Text(stringResource(R.string.authentication_social_complete_other)) }
}