package org.example.modules.business.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.modules.business.R
import org.example.shared.designsystem.theme.Teal

/**
 * Text wordmark. The personal home draws a logo image (personal_logo / personal_logo_light) that is not in this
 * repository, so the business copy uses text; swap in the real logo drawable when it is available.
 */
@Composable
internal fun Wordmark(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, onClickLabel: String? = null) {
    val click = if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick) else Modifier
    Box(modifier = modifier.heightIn(min = 48.dp).then(click), contentAlignment = Alignment.CenterStart) {
        Text(
            text = stringResource(R.string.business_wordmark),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Teal,
        )
    }
}
