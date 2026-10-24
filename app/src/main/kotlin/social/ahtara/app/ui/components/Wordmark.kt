package social.ahtara.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import social.ahtara.app.R

private const val PRESSED_SCALE = 0.95f













@Composable
fun Wordmark(
    modifier: Modifier = Modifier,
    height: Dp = 30.dp,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "logoPressScale",
    )

    val onLightBackground = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val logoRes = if (onLightBackground) R.drawable.logo else R.drawable.logo_light

    
    
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClickLabel = onClickLabel,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(clickModifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        Image(
            painter = painterResource(id = logoRes),
            contentDescription = "Ahtara",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(height)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}