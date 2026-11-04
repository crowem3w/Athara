package org.example.app.onboarding

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.app.onboarding.ui.theme.OnboardingTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/** Roughly the height of a typical phone keyboard. */
private val PanelHeight = 300.dp
private val PanelCornerRadius = 28.dp

// Road / roller animation tuning
private val WaveAmplitude = 8.dp
private val WaveLength = 140.dp
private val RollerSize = 36.dp
private const val TripMillis = 1500         // one crossing of the panel (left -> right or back)
private const val RoadScrollRatio = 0.5f    // road scroll distance per distance the roller travels

// "Get Started" exit animation: wind up to the left, then accelerate off the right edge
private const val WindUpSides = 2           // wind-up distance, in roller side lengths
private const val WindUpMillis = 450
private const val LaunchMillis = 900

// Hand-off to the sign-in screen: while the roller leaves, the right end of the line rises so it
// meets the line that AuthenticationActivity continues from. Keep OnboardingLift in sync with
// the matching constants in the :authentication module's LoginRoadStrip.kt.
private val OnboardingLift = 56.dp
private const val LiftDelayMillis = 300
private const val LiftMillis = 600

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light system bar icons-on-light style, regardless of the system dark mode.
        val lightBars = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = lightBars)
        // Result stays RESULT_CANCELED unless the user taps Get Started.
        setContent {
            OnboardingTheme {
                WelcomeScreen(onGetStarted = ::completeOnboarding)
            }
        }
    }

    private fun completeOnboarding() {
        Onboarding.markCompleted(this)
        setResult(Activity.RESULT_OK)
        finish()
        // No slide/fade: the sign-in screen continues the line from where it stopped.
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}

/**
 * The panel's top edge: rounded corners plus a bumpy "road" wave that scrolls
 * backwards ([scroll]) while the roller travels forward.
 */
private class Road(
    private val cornerRadius: Float,
    private val amplitude: Float,
    private val wavelength: Float,
    private val scroll: Float,
    /** Extra room above the panel's resting top edge so the line can rise without leaving the bounds. */
    private val headroom: Float = 0f,
    /** How far the right end of the line has risen (px); 0 at the left end, full at the right edge. */
    private val lift: Float = 0f,
) {
    /** Y of the surface at [x], measured down from the top of the panel's bounds. */
    fun yAt(x: Float, width: Float): Float {
        val xc = x.coerceIn(0f, width)
        val r = cornerRadius
        val dx = when {
            xc < r -> r - xc
            xc > width - r -> xc - (width - r)
            else -> 0f
        }
        val base = r - sqrt(max(0f, r * r - dx * dx))
        val wave = amplitude * (1f - sin(2f * PI.toFloat() * (xc + scroll) / wavelength))
        val t = if (width > 0f) xc / width else 0f
        return headroom + base + wave - lift * t * t
    }
}

private class RoadTopShape(private val road: Road) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path().apply {
            moveTo(0f, size.height)
            var x = 0f
            while (true) {
                lineTo(x, road.yAt(x, size.width))
                if (x >= size.width) break
                x = min(x + 4f, size.width)
            }
            lineTo(size.width, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

/** Distance the roller travels between its two end stops: a whole number of sides. */
private fun Density.trackLength(width: Float): Float {
    val side = RollerSize.toPx()
    return max(1f, floor((width - 2f * side) / side)) * side
}

/** Road state for roller [distance] from the left stop (px, may be < 0 or > track); the road scrolls opposite to the roller. */
private fun Density.roadAt(distance: Float, width: Float, lift: Float = 0f): Road {
    return Road(
        cornerRadius = PanelCornerRadius.toPx(),
        amplitude = WaveAmplitude.toPx(),
        wavelength = WaveLength.toPx(),
        scroll = distance * RoadScrollRatio,
        headroom = OnboardingLift.toPx(),
        lift = lift,
    )
}

/**
 * Rounded square rolling along the road between a left and a right stop.
 * It rolls one side length per 90 degrees (and un-rolls on the way back),
 * so it rests flat at both ends.
 */
private fun DrawScope.drawRoller(distance: Float, lift: Float, color: Color) {
    val s = RollerSize.toPx()
    val road = roadAt(distance, size.width, lift)

    val cx = s + distance
    val angle = distance / s * 90f
    // Height of a rolling square's center: dips to s/2 when flat, rises to s/sqrt(2) on a corner.
    // floor-based modulo so negative angles (wind-up to the left) work too.
    val within = angle - floor(angle / 90f) * 90f
    val local = (within - 45f) * (PI.toFloat() / 180f)
    val centerHeight = s / sqrt(2f) * cos(local)
    val cy = road.yAt(cx, size.width) - centerHeight

    withTransform({ rotate(angle, Offset(cx, cy)) }) {
        drawRoundRect(
            color = color,
            topLeft = Offset(cx - s / 2f, cy - s / 2f),
            size = Size(s, s),
            cornerRadius = CornerRadius(s * 0.28f),
        )
        // Marker so the rotation is visible.
        drawCircle(Color.White, radius = s * 0.09f, center = Offset(cx + s * 0.2f, cy - s * 0.2f))
    }
}

@Composable
internal fun WelcomeScreen(onGetStarted: () -> Unit) {
    // Idle: loops while waiting for the tap: 0 -> 1 -> 0 -> ... (back and forth).
    // The State is only read in draw/layer blocks below, so frames skip recomposition.
    val idle = rememberInfiniteTransition(label = "roller").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(TripMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rollerProgress",
    )
    val rollerColor = MaterialTheme.colorScheme.primary

    // Exit: after "Get Started" the roller is driven by [exitDistance] (px) instead.
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var panelWidth by remember { mutableFloatStateOf(0f) }
    var exiting by remember { mutableStateOf(false) }
    val exitDistance = remember { Animatable(0f) }
    val exitLift = remember { Animatable(0f) } // right end of the line rising toward the sign-in screen

    // Roller distance from the left stop, read only in draw/layer blocks.
    val distance: () -> Float = {
        if (exiting) exitDistance.value
        else idle.value * density.run { trackLength(panelWidth) }
    }

    val startExit: () -> Unit = start@{
        if (exiting || panelWidth <= 0f) return@start
        val width = panelWidth
        val side = with(density) { RollerSize.toPx() }
        val from = idle.value * density.run { trackLength(width) } // freeze where it is
        // Wind up two sides to the left (never past the left edge), then launch.
        val windUp = max(from - WindUpSides * side, -side / 2f)
        scope.launch {
            exitDistance.snapTo(from)
            exiting = true
            exitDistance.animateTo(windUp, tween(WindUpMillis, easing = FastOutSlowInEasing))
            // Accelerate until fully off the right edge, while the line's right end rises.
            launch {
                delay(LiftDelayMillis.toLong())
                exitLift.animateTo(
                    with(density) { OnboardingLift.toPx() },
                    tween(LiftMillis, easing = FastOutSlowInEasing),
                )
            }
            exitDistance.animateTo(width, tween(LaunchMillis, easing = EaseInCubic))
            onGetStarted()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Illustration: scaled to fit the area above the panel (never cropped
        // or covered) and pinned to the top.
        Image(
            painter = painterResource(R.drawable.onboarding_background),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = PanelHeight),
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.Fit,
        )

        // Panel container: extends under the navigation bar; content area is PanelHeight.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { panelWidth = it.width.toFloat() },
        ) {
            // White panel with the animated road-shaped top edge (and shadow).
            // Shape is read in the layer block, so animation frames skip recomposition.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        shape = RoadTopShape(roadAt(distance(), size.width, exitLift.value))
                        clip = true
                        shadowElevation = 12.dp.toPx()
                    }
                    .background(Color.White),
            )

            Column(
                modifier = Modifier
                    .padding(top = OnboardingLift) // headroom for the rising line; content stays put
                    .navigationBarsPadding()
                    .height(PanelHeight)
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 24.dp + WaveAmplitude * 2, // clear the deepest bump
                        bottom = 24.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.onboarding_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.onboarding_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                }
                Button(onClick = startExit, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onboarding_get_started))
                }
            }

            // Roller drawn on top of everything, riding the panel's top edge.
            Canvas(Modifier.matchParentSize()) {
                drawRoller(distance(), exitLift.value, rollerColor)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun WelcomeScreenPreview() {
    OnboardingTheme { WelcomeScreen(onGetStarted = {}) }
}
