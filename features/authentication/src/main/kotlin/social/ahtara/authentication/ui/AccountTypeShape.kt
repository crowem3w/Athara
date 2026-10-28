package social.ahtara.authentication.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import social.ahtara.authentication.domain.AccountType







@Composable
internal fun AccountTypeShape(
    type: AccountType,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        
        val boxW = size.width * 0.55f
        val boxH = size.height
        val left = size.width * 0.60f

        val path = Path().apply { outline(type, boxW, boxH) }

        translate(left = left, top = 0f) {
            drawPath(path, color.copy(alpha = 0.14f))
            
            scale(scale = 0.62f, pivot = Offset(boxW * 0.55f, boxH * 0.5f)) {
                drawPath(path, color.copy(alpha = 0.22f))
            }
        }
    }
}


private fun Path.outline(type: AccountType, w: Float, h: Float) {
    fun m(x: Float, y: Float) = moveTo(x * w, y * h)
    fun c(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
        cubicTo(x1 * w, y1 * h, x2 * w, y2 * h, x3 * w, y3 * h)

    when (type) {
        
        AccountType.PERSONAL -> {
            m(0.35f, 0.05f)
            c(0.75f, -0.05f, 1.00f, 0.35f, 0.95f, 0.60f)
            c(0.90f, 0.90f, 0.55f, 1.05f, 0.30f, 0.90f)
            c(0.05f, 0.75f, 0.00f, 0.50f, 0.10f, 0.30f)
            c(0.15f, 0.15f, 0.25f, 0.10f, 0.35f, 0.05f)
        }
        
        AccountType.BUSINESS -> {
            m(0.50f, 0.00f)
            c(0.90f, 0.05f, 1.10f, 0.30f, 1.00f, 0.70f)
            c(0.95f, 0.95f, 0.70f, 1.10f, 0.40f, 1.00f)
            c(0.15f, 0.92f, 0.25f, 0.75f, 0.35f, 0.60f)
            c(0.45f, 0.45f, 0.10f, 0.30f, 0.25f, 0.12f)
            c(0.32f, 0.04f, 0.40f, 0.00f, 0.50f, 0.00f)
        }
        
        AccountType.ORGANIZATION -> {
            m(0.20f, 0.20f)
            c(0.30f, -0.05f, 0.65f, 0.00f, 0.70f, 0.20f)
            c(0.95f, 0.10f, 1.10f, 0.45f, 0.90f, 0.60f)
            c(1.05f, 0.85f, 0.80f, 1.10f, 0.60f, 0.95f)
            c(0.45f, 1.10f, 0.10f, 1.00f, 0.20f, 0.75f)
            c(-0.05f, 0.60f, 0.00f, 0.30f, 0.20f, 0.20f)
        }
    }
    close()
}