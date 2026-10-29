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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
    onTransportClick: () -> Unit = {},
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
                TransportTab(selected = false, modifier = Modifier.weight(1f), onClick = onTransportClick)
                
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

    // The bar: a flat surface with a rounded dip cut out for the Create button.
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
    val curve = Path().apply {
        moveTo(left, 0f)
        dipLeft(left, cx, halfWidth, depth)
        dipRight(right, cx, halfWidth, depth)
    }
    val facePath = face.asAndroidPath()
    val curvePath = curve.asAndroidPath()

    // Soft ambient shadow: a blurred copy of the bar itself, so it fades out smoothly from the
    // top edge and follows the curve of the dip. No flat fill behind the Create button.
    val ambient = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(40, 0, 0, 0)
        maskFilter = android.graphics.BlurMaskFilter(10.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    // Tighter, fainter shadow hugging the curve, so the dip reads as a clean edge (no hard line).
    val contact = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 2.dp.toPx()
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        color = android.graphics.Color.argb(26, 0, 0, 0)
        maskFilter = android.graphics.BlurMaskFilter(3.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    val lift = 1.dp.toPx() // light from below-front: shadow sits slightly above the edge

    onDrawBehind {
        translate(top = edgeDrop) {
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                native.save()
                native.translate(0f, -lift)
                native.drawPath(facePath, ambient)
                native.drawPath(curvePath, contact)
                native.restore()
            }
            drawPath(face, color = Background)
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