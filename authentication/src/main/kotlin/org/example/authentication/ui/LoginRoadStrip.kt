package org.example.authentication.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

// Same road / roller look as OnboardingActivity (wave amplitude, roller size, scroll ratio).
// The wavelength is 144dp (onboarding: 140dp) so one full roll of the roller (4 sides = 144dp)
// lines up with the wave and the idle loop is seamless.
private val StripHeight = 66.dp
private val BaselineY = 46.dp          // resting line level inside the strip; the roller sits above it
private val WaveAmplitude = 8.dp
private val WaveLength = 144.dp
private val RollerSize = 36.dp
private const val ScrollRatio = 0.5f   // road scroll distance per distance the roller rolls
private val StripHalfWidth = 96.dp     // the strip is 2 x this wide, centered
private val EdgeFade = 44.dp           // soft fade at both ends of the strip
private val LineWidth = 3.dp

private val CruiseSpeed = 150.dp       // per second
private const val FadeInMillis = 300

/**
 * Wavy road with a rounded square rolling on it, centered above the "Welcome back" title.
 * The strip is in its resting place from the first frame and simply fades in; the square stays
 * at the center while the road keeps scrolling past it.
 */
@Composable
internal fun LoginRoadStrip(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val rollerColor = MaterialTheme.colorScheme.primary
    val lineColor = rollerColor.copy(alpha = 0.45f)

    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alpha.animateTo(1f, tween(FadeInMillis)) }

    // Read only in the draw block below, so frames skip recomposition.
    var distance by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val cruise = with(density) { CruiseSpeed.toPx() }
        // 2 x 144dp is a whole number of rolls and wavelengths, so wrapping by it is seamless.
        val period = with(density) { RollerSize.toPx() * 8f }
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = (now - last) / 1_000_000_000f
            last = now
            distance += cruise * dt
            if (distance > period * 1000f) distance %= period
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(StripHeight)
            .graphicsLayer { this.alpha = alpha.value },
    ) {
        val s = RollerSize.toPx()
        val centerX = size.width / 2f
        val baseY = BaselineY.toPx()
        val halfW = StripHalfWidth.toPx()
        val x0 = centerX - halfW
        val x1 = centerX + halfW
        val scroll = distance * ScrollRatio

        fun roadY(x: Float): Float {
            val wave = WaveAmplitude.toPx() * (1f - sin(2f * PI.toFloat() * (x + scroll) / WaveLength.toPx()))
            return baseY + wave
        }

        // Line: fades out at both ends.
        val path = Path().apply {
            var x = x0
            moveTo(x, roadY(x))
            while (x < x1) {
                x = minOf(x + 4f, x1)
                lineTo(x, roadY(x))
            }
        }
        val fade = EdgeFade.toPx() / (x1 - x0)
        val brush: Brush = Brush.horizontalGradient(
            0f to Color.Transparent,
            fade to lineColor,
            (1f - fade) to lineColor,
            1f to Color.Transparent,
            startX = x0,
            endX = x1,
        )
        drawPath(path, brush, style = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Round))

        // Roller: stays at the center and keeps rolling in place.
        drawRoller(cx = centerX, roadY = roadY(centerX), angle = distance / s * 90f, size = s, color = rollerColor)
    }
}

private fun DrawScope.drawRoller(cx: Float, roadY: Float, angle: Float, size: Float, color: Color) {
    // Height of a rolling square's center: dips to s/2 when flat, rises to s/sqrt(2) on a corner.
    val within = angle - floor(angle / 90f) * 90f
    val local = (within - 45f) * (PI.toFloat() / 180f)
    val centerHeight = size / sqrt(2f) * cos(local)
    val cy = roadY - centerHeight
    withTransform({ rotate(angle, Offset(cx, cy)) }) {
        drawRoundRect(
            color = color,
            topLeft = Offset(cx - size / 2f, cy - size / 2f),
            size = Size(size, size),
            cornerRadius = CornerRadius(size * 0.28f),
        )
        // Marker so the rotation is visible.
        drawCircle(Color.White, radius = size * 0.09f, center = Offset(cx + size * 0.2f, cy - size * 0.2f))
    }
}
