package social.ahtara.authentication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import social.ahtara.authentication.domain.AccountType


@Composable
fun AccountTypeBadge(type: AccountType, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val accent = colorResource(type.accentColor)
    Box(
        modifier = modifier.size(size).background(accent.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(type.icon),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(size / 2),
        )
    }
}