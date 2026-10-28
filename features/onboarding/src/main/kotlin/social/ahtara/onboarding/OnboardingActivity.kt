package social.ahtara.onboarding

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
import social.ahtara.onboarding.ui.theme.OnboardingTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt


private val PanelHeight = 300.dp
private val PanelCornerRadius = 28.dp


private val WaveAmplitude = 8.dp
private val WaveLength = 140.dp
private val RollerSize = 36.dp
private const val TripMillis = 1500         
private const val RoadScrollRatio = 0.5f    


private const val WindUpSides = 2           
private const val WindUpMillis = 450
private const val LaunchMillis = 900




private val OnboardingLift = 56.dp
private const val LiftDelayMillis = 300
private const val LiftMillis = 600

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val lightBars = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = lightBars)
        
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
        
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}





private class Road(
    private val cornerRadius: Float,
    private val amplitude: Float,
    private val wavelength: Float,
    private val scroll: Float,
    
    private val headroom: Float = 0f,
    
    private val lift: Float = 0f,
) {
    
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


private fun Density.trackLength(width: Float): Float {
    val side = RollerSize.toPx()
    return max(1f, floor((width - 2f * side) / side)) * side
}


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






private fun DrawScope.drawRoller(distance: Float, lift: Float, color: Color) {
    val s = RollerSize.toPx()
    val road = roadAt(distance, size.width, lift)

    val cx = s + distance
    val angle = distance / s * 90f
    
    
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
        
        drawCircle(Color.White, radius = s * 0.09f, center = Offset(cx + s * 0.2f, cy - s * 0.2f))
    }
}

@Composable
internal fun WelcomeScreen(onGetStarted: () -> Unit) {
    
    
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

    
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var panelWidth by remember { mutableFloatStateOf(0f) }
    var exiting by remember { mutableStateOf(false) }
    val exitDistance = remember { Animatable(0f) }
    val exitLift = remember { Animatable(0f) } 

    
    val distance: () -> Float = {
        if (exiting) exitDistance.value
        else idle.value * density.run { trackLength(panelWidth) }
    }

    val startExit: () -> Unit = start@{
        if (exiting || panelWidth <= 0f) return@start
        val width = panelWidth
        val side = with(density) { RollerSize.toPx() }
        val from = idle.value * density.run { trackLength(width) } 
        
        val windUp = max(from - WindUpSides * side, -side / 2f)
        scope.launch {
            exitDistance.snapTo(from)
            exiting = true
            exitDistance.animateTo(windUp, tween(WindUpMillis, easing = FastOutSlowInEasing))
            
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
        
        
        Image(
            painter = painterResource(R.drawable.onboarding_background),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = PanelHeight),
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.Fit,
        )

        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { panelWidth = it.width.toFloat() },
        ) {
            
            
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
                    .padding(top = OnboardingLift) 
                    .navigationBarsPadding()
                    .height(PanelHeight)
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 24.dp + WaveAmplitude * 2, 
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