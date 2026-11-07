package soc.athara.app.modules.business.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import soc.athara.app.modules.business.R
import soc.athara.app.shared.designsystem.theme.Background

private val FabSize = 64.dp


private val FabRise = 28.dp
private val EdgeDrop = 12.dp
private val FabGap = 12.dp
private val DipDepth = FabSize - FabRise + FabGap
private val DipHalfWidth = 66.dp





@Composable
internal fun BottomNavBar(
    modifier: Modifier = Modifier,
    onReadClick: () -> Unit = {},
    readerIcon: Painter = painterResource(R.drawable.business_ic_scan),
    readerLabel: String = stringResource(R.string.business_collect_payment),
    
    readerVisibility: () -> Float = { 1f },
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
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {}
        }
        ReaderTab(
            onClick = onReadClick,
            visibility = readerVisibility,
            icon = readerIcon,
            contentDescription = readerLabel,
            modifier = Modifier.align(Alignment.TopCenter).size(FabSize),
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
    val curve = Path().apply {
        moveTo(left, 0f)
        dipLeft(left, cx, halfWidth, depth)
        dipRight(right, cx, halfWidth, depth)
    }
    val facePath = face.asAndroidPath()
    val curvePath = curve.asAndroidPath()

    
    
    val ambient = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(40, 0, 0, 0)
        maskFilter = android.graphics.BlurMaskFilter(10.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    
    val contact = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 2.dp.toPx()
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        color = android.graphics.Color.argb(26, 0, 0, 0)
        maskFilter = android.graphics.BlurMaskFilter(3.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    val lift = 1.dp.toPx() 

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
