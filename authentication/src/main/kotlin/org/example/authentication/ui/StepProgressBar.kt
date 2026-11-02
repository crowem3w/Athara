package org.example.authentication.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import org.example.authentication.R
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Total number of bars in the sign-up flow. */
internal const val SIGN_UP_TOTAL_STEPS = 4

/** Bars filled on the account type screen. */
internal const val STEP_ACCOUNT_TYPE = 2

/** Bars filled on the registration forms. */
internal const val STEP_REGISTRATION = 3

/** Bars filled on the account setup step that follows registration. */
internal const val STEP_SETUP = 4

private val BarWidth = 28.dp
private val BarHeight = 6.dp
private val BarGap = 8.dp
private val RollerSize = 20.dp
private val HopHeight = 12.dp
private const val TravelHopScale = 1.6f  // the hop to the next bar is higher than the in-place hops
private val BarBottomInset = 3.dp        // room under the bar for the landing jolt

/** Height of the strip: room above the bar for the roller's highest hop, plus the inset below. */
private val StripHeight = 52.dp

/** Distance from the top of the strip to the vertical center of the bars. */
internal val StepProgressBarCenterY = StripHeight - BarBottomInset - BarHeight / 2

private const val HopSeconds = 0.6f      // one in-place hop = one quarter turn
private const val TravelSeconds = 0.75f  // hop from the previous bar to the current one
private const val TravelTurns = 180f     // half a turn while travelling (two quarter turns)

// Jolt of the current bar each time the square lands: a quickly fading up/down wobble.
private val JoltAmplitude = 2.dp
private const val JoltFrequency = 18f    // wobbles per second
private const val JoltDecaySeconds = 0.07f

/**
 * Segmented progress bar: the first [landedStep] of [totalSteps] rounded bars are teal, the rest
 * light gray. A small rounded square sits on the current bar, rolling a quarter turn with every
 * hop and never stopping.
 *
 * When [currentStep] changes, the square hops to the new bar (rolling forward when moving ahead,
 * backward when going back). The bar changes color only once the square has landed on it.
 */
@Composable
internal fun StepProgressBar(
    currentStep: Int,
    modifier: Modifier = Modifier,
    totalSteps: Int = SIGN_UP_TOTAL_STEPS,
) {
    val active = colorResource(R.color.authentication_primary)
    val inactive = colorResource(R.color.authentication_progress_inactive)

    // Read only inside the draw block, so frames do not recompose.
    var elapsed by remember { mutableFloatStateOf(0f) }
    var landedStep by remember { mutableIntStateOf(currentStep) } // bar the square rests on / left from
    var targetStep by remember { mutableIntStateOf(currentStep) } // bar it is hopping to (== landed when resting)
    var travelStart by remember { mutableFloatStateOf(0f) }
    var landTime by remember { mutableFloatStateOf(0f) }          // when the square last touched a bar
    var baseAngle by remember { mutableFloatStateOf(0f) }         // rotation accumulated by finished hops
    val target by rememberUpdatedState(currentStep)

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            elapsed += (now - last) / 1_000_000_000f
            last = now

            val travelling = targetStep != landedStep
            if (travelling && elapsed >= travelStart + TravelSeconds) {
                // Landed: the bar fills now, and the square carries its rotation over.
                baseAngle += TravelTurns * (if (targetStep > landedStep) 1f else -1f)
                landedStep = targetStep
                landTime = travelStart + TravelSeconds
            }
            // Resting and a new step was asked for: hop there. A change asked for mid-hop waits
            // for the landing, then hops on from there.
            if (targetStep == landedStep && target != landedStep) {
                targetStep = target
                travelStart = elapsed
            }
        }
    }

    val stripWidth = BarWidth * totalSteps + BarGap * (totalSteps - 1)
    Canvas(modifier = modifier.width(stripWidth).height(StripHeight)) {
        val gap = BarGap.toPx()
        val barH = BarHeight.toPx()
        val barW = BarWidth.toPx()
        val barTop = size.height - BarBottomInset.toPx() - barH
        val radius = CornerRadius(barH / 2f)

        fun barCenter(step: Int) = (step - 1) * (barW + gap) + barW / 2f

        val s = RollerSize.toPx()
        val hop = HopHeight.toPx()
        val cx: Float
        val lift: Float
        val angle: Float
        val landedFor: Float // seconds since the square last touched the bar
        if (targetStep != landedStep) {
            val t = ((elapsed - travelStart) / TravelSeconds).coerceIn(0f, 1f)
            val dir = if (targetStep > landedStep) 1f else -1f
            cx = barCenter(landedStep) + (barCenter(targetStep) - barCenter(landedStep)) * t
            lift = 4f * t * (1f - t) * hop * TravelHopScale
            angle = baseAngle + dir * TravelTurns * t
            landedFor = 1000f // airborne: no jolt
        } else {
            val e = (elapsed - landTime).coerceAtLeast(0f)
            val count = (e / HopSeconds).toInt()
            val t = e / HopSeconds - count
            cx = barCenter(landedStep)
            lift = 4f * t * (1f - t) * hop
            angle = baseAngle + 90f * (count + t)
            landedFor = t * HopSeconds
        }
        val jolt = JoltAmplitude.toPx() * exp(-landedFor / JoltDecaySeconds) *
            sin(2f * PI.toFloat() * JoltFrequency * landedFor)
        // The square rides the bar only while it is still close to it.
        val ride = (1f - lift / (hop * 0.3f)).coerceIn(0f, 1f)
        val cy = barTop - s / 2f - lift + jolt * ride

        for (i in 0 until totalSteps) {
            val shake = if (i == landedStep - 1) jolt else 0f
            drawRoundRect(
                color = if (i < landedStep) active else inactive,
                topLeft = Offset(i * (barW + gap), barTop + shake),
                size = Size(barW, barH),
                cornerRadius = radius,
            )
        }

        withTransform({ rotate(angle, Offset(cx, cy)) }) {
            drawRoundRect(
                color = active,
                topLeft = Offset(cx - s / 2f, cy - s / 2f),
                size = Size(s, s),
                cornerRadius = CornerRadius(s * 0.28f),
            )
            // Marker so the rotation is visible.
            drawCircle(Color.White, radius = s * 0.09f, center = Offset(cx + s * 0.2f, cy - s * 0.2f))
        }
    }
}
