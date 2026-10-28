package social.ahtara.authentication.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import social.ahtara.authentication.R


@Composable
internal fun AuthLogo(
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
) {
    Image(
        painter = painterResource(R.drawable.authentication_logo),
        contentDescription = stringResource(R.string.authentication_logo_description),
        contentScale = ContentScale.Fit,
        modifier = modifier.width(width),
    )
}