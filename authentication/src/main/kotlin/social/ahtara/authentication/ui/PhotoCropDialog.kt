package social.ahtara.authentication.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import social.ahtara.authentication.R
import social.ahtara.authentication.data.ImageDecoding

private const val MaxZoom = 6f






@Composable
internal fun PhotoCropDialog(
    uri: Uri,
    aspect: Float,
    circle: Boolean,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching { ImageDecoding.decode(context.contentResolver, uri, 2048) }.getOrNull()
        }
    }
    var saving by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
                val density = LocalDensity.current
                val containerW = with(density) { maxWidth.toPx() }
                val containerH = with(density) { maxHeight.toPx() }
                val margin = with(density) { 24.dp.toPx() }
                val corner = with(density) { 20.dp.toPx() }
                val stroke = with(density) { 2.dp.toPx() }

                
                var winW = containerW - 2 * margin
                var winH = winW / aspect
                if (winH > containerH * 0.6f) {
                    winH = containerH * 0.6f
                    winW = winH * aspect
                }
                val bmp = bitmap
                val minScale = if (bmp != null) max(winW / bmp.width, winH / bmp.height) else 1f
                var zoom by remember(bmp) { mutableFloatStateOf(1f) }
                var offset by remember(bmp) { mutableStateOf(Offset.Zero) }

                Canvas(
                    Modifier.fillMaxSize().pointerInput(bmp, winW, winH) {
                        if (bmp == null) return@pointerInput
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            val newZoom = (zoom * gestureZoom).coerceIn(1f, MaxZoom)
                            val s = minScale * newZoom
                            val maxX = max(0f, (bmp.width * s - winW) / 2f)
                            val maxY = max(0f, (bmp.height * s - winH) / 2f)
                            zoom = newZoom
                            offset = Offset(
                                (offset.x + pan.x).coerceIn(-maxX, maxX),
                                (offset.y + pan.y).coerceIn(-maxY, maxY),
                            )
                        }
                    },
                ) {
                    val c = center
                    val s = minScale * zoom
                    if (bmp != null) {
                        withTransform({
                            translate(c.x + offset.x, c.y + offset.y)
                            scale(s, s, pivot = Offset.Zero)
                        }) {
                            drawImage(bmp.asImageBitmap(), topLeft = Offset(-bmp.width / 2f, -bmp.height / 2f))
                        }
                    }
                    val window = Rect(c.x - winW / 2f, c.y - winH / 2f, c.x + winW / 2f, c.y + winH / 2f)
                    val dim = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(Offset.Zero, size))
                        if (circle) addOval(window) else addRoundRect(RoundRect(window, CornerRadius(corner)))
                    }
                    drawPath(dim, Color.Black.copy(alpha = 0.65f))
                    if (circle) {
                        drawCircle(Color.White, radius = winW / 2f, center = c, style = Stroke(stroke))
                    } else {
                        drawRoundRect(Color.White, window.topLeft, window.size, CornerRadius(corner), Stroke(stroke))
                    }
                }

                if (bmp == null) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)

                Row(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val white = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    TextButton(onClick = onCancel, colors = white) {
                        Text(stringResource(R.string.authentication_setup_crop_cancel))
                    }
                    TextButton(
                        enabled = bmp != null && !saving,
                        colors = white,
                        onClick = {
                            val source = bmp ?: return@TextButton
                            saving = true
                            val s = minScale * zoom
                            val sw = winW / s
                            val sh = winH / s
                            val sx = source.width / 2f + (-winW / 2f - offset.x) / s
                            val sy = source.height / 2f + (-winH / 2f - offset.y) / s
                            scope.launch {
                                val out = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val x = sx.toInt().coerceIn(0, source.width - 1)
                                        val y = sy.toInt().coerceIn(0, source.height - 1)
                                        val w = min(sw.toInt().coerceAtLeast(1), source.width - x)
                                        val h = min(sh.toInt().coerceAtLeast(1), source.height - y)
                                        val cropped = Bitmap.createBitmap(source, x, y, w, h)
                                        val dir = File(context.cacheDir, "crop").apply { mkdirs() }
                                        val file = File.createTempFile("crop_", ".jpg", dir)
                                        file.outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                                        Uri.fromFile(file).toString()
                                    }.getOrNull()
                                }
                                saving = false
                                if (out != null) onDone(out) else onCancel()
                            }
                        },
                    ) { Text(stringResource(R.string.authentication_setup_crop_done)) }
                }
                Text(
                    text = stringResource(R.string.authentication_setup_crop_hint),
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                )
            }
        }
    }
}