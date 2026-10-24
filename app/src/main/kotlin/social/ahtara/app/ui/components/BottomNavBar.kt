package social.ahtara.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import social.ahtara.app.ui.theme.Background
import social.ahtara.app.ui.theme.NavInactive
import social.ahtara.app.ui.theme.Teal


private val FabSize = 64.dp






private val FabRise = 28.dp





private val EdgeDrop = 12.dp


private val FabGap = 12.dp


private val DipDepth = FabSize - FabRise + FabGap





private val DipHalfWidth = 66.dp

@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    onReelsClick: () -> Unit = {},
    onCreateClick: () -> Unit = {},
    onPlacesClick: () -> Unit = {},
    
    createVisibility: () -> Float = { 1f },
) {
    
    
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = FabRise - EdgeDrop)
                .dippedBarSurface(),
        ) {
            
            Spacer(Modifier.height(1.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeTab(selected = true, modifier = Modifier.weight(1f))
                ReelsTab(selected = false, modifier = Modifier.weight(1f), onClick = onReelsClick)
                
                Spacer(Modifier.width(FabSize + 8.dp))
                MessageTab(selected = false, modifier = Modifier.weight(1f))
                PlacesTab(selected = false, modifier = Modifier.weight(1f), onClick = onPlacesClick)
            }
        }
        CreateTab(
            onClick = onCreateClick,
            visibility = createVisibility,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(FabSize),
        )
    }
}








private fun Path.dipLeft(left: Float, cx: Float, halfWidth: Float, depth: Float) {
    val k = 0.55f * halfWidth
    cubicTo(left + k, 0f, cx - k, depth, cx, depth)
}

private fun Path.dipRight(right: Float, cx: Float, halfWidth: Float, depth: Float) {
    val k = 0.55f * halfWidth
    cubicTo(cx + k, depth, right - k, 0f, right, 0f)
}






private fun Modifier.dippedBarSurface(): Modifier = drawWithCache {
    
    val edgeDrop = EdgeDrop.toPx()
    val width = size.width
    val height = size.height - edgeDrop
    val cx = width / 2f
    val halfWidth = DipHalfWidth.toPx()
    val depth = DipDepth.toPx()
    val left = cx - halfWidth
    val right = cx + halfWidth

    
    val face = Path().apply {
        moveTo(0f, 0f)
        lineTo(left, 0f)
        dipLeft(left, cx, halfWidth, depth)
        dipRight(right, cx, halfWidth, depth)
        lineTo(width, 0f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    
    val bowl = Path().apply {
        moveTo(left, 0f)
        dipLeft(left, cx, halfWidth, depth)
        dipRight(right, cx, halfWidth, depth)
        close()
    }
    
    val curve = Path().apply {
        moveTo(left, 0f)
        dipLeft(left, cx, halfWidth, depth)
        dipRight(right, cx, halfWidth, depth)
    }

    onDrawBehind {
        translate(top = edgeDrop) {
            drawPath(face, color = Background)

            clipPath(bowl) {
                
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(cx, depth * 0.5f),
                        radius = halfWidth,
                    ),
                )

                
                
                val contactCenter = Offset(cx, depth - 3.dp.toPx())
                val contactRadius = FabSize.toPx() * 0.5f
                scale(scaleX = 1f, scaleY = 0.28f, pivot = contactCenter) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.20f), Color.Transparent),
                            center = contactCenter,
                            radius = contactRadius,
                        ),
                        radius = contactRadius,
                        center = contactCenter,
                    )
                }

                
                val bounceCenter = Offset(cx, depth - 1.5.dp.toPx())
                val bounceRadius = halfWidth * 0.5f
                scale(scaleX = 1f, scaleY = 0.3f, pivot = bounceCenter) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                            center = bounceCenter,
                            radius = bounceRadius,
                        ),
                        radius = bounceRadius,
                        center = bounceCenter,
                    )
                }

                
                translate(top = -2.5.dp.toPx()) {
                    drawPath(
                        path = curve,
                        color = Color.Black.copy(alpha = 0.16f),
                        style = Stroke(width = 0.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}


@Composable
internal fun BottomNavTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    icon: @Composable (tint: Color) -> Unit,
) {
    val tint = if (selected) Teal else NavInactive
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
    ) {
        icon(tint)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}