package social.ahtara.authentication.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import social.ahtara.authentication.R
import social.ahtara.authentication.data.ImageDecoding

private val HeaderAvatarSize = 112.dp
private val HeaderAvatarInset = 16.dp
private val HeaderAvatarRing = 5.dp
private val HeaderGray = Color(0xFFD8DBE0)
private val HeaderPlaceholder = Color(0xFFE4E6EB)






@Composable
fun ProfileHeader(coverPath: String?, photoPath: String?, modifier: Modifier = Modifier) {
    val cover = rememberFileImage(coverPath, 1000)
    val photo = rememberFileImage(photoPath, 400)
    val panel = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxWidth().padding(bottom = HeaderAvatarSize / 2)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
                .clip(RoundedCornerShape(20.dp))
                .background(HeaderGray),
        ) {
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = stringResource(R.string.authentication_setup_cover),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = HeaderAvatarInset, y = HeaderAvatarSize / 2)
                .size(HeaderAvatarSize),
            shape = CircleShape,
            color = HeaderPlaceholder,
            border = BorderStroke(HeaderAvatarRing, panel),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (photo != null) {
                    Image(
                        bitmap = photo,
                        contentDescription = stringResource(R.string.authentication_setup_photo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.authentication_ic_personal),
                        contentDescription = stringResource(R.string.authentication_setup_photo),
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberFileImage(path: String?, maxSide: Int): ImageBitmap? {
    val resolver = LocalContext.current.contentResolver
    val image by produceState<ImageBitmap?>(initialValue = null, path, maxSide) {
        value = if (path == null) null else withContext(Dispatchers.IO) {
            runCatching { ImageDecoding.decode(resolver, Uri.fromFile(File(path)), maxSide)?.asImageBitmap() }.getOrNull()
        }
    }
    return image
}