package social.ahtara.authentication.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import social.ahtara.authentication.R

private val HeaderAvatarSize = 112.dp
private val HeaderAvatarInset = 16.dp
private val HeaderAvatarRing = 5.dp
private val HeaderGray = Color(0xFFD8DBE0)
private val HeaderPlaceholder = Color(0xFFE4E6EB)






@Composable
fun ProfileHeader(coverUrl: String?, photoUrl: String?, modifier: Modifier = Modifier) {
    val panel = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxWidth().padding(bottom = HeaderAvatarSize / 2)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
                .clip(RoundedCornerShape(20.dp))
                .background(HeaderGray),
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
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
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
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
