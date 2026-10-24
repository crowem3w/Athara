package social.ahtara.app.reels.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import social.ahtara.app.reels.R
import social.ahtara.app.reels.Reel
import social.ahtara.app.ui.theme.Heart
import social.ahtara.app.ui.theme.Teal

@Composable
fun ReelOverlay(reel: Reel) {
    var liked by rememberSaveable(reel.id) { mutableStateOf(false) }
    var following by rememberSaveable(reel.id) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(280.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3000000)))),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            RailAction(
                painter = rememberVectorPainter(if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder),
                tint = if (liked) Heart else Color.White,
                label = formatCount(reel.likes + if (liked) 1 else 0),
                description = "Like",
                onClick = { liked = !liked },
            )
            RailAction(
                painter = painterResource(R.drawable.ic_reels_comment),
                label = formatCount(reel.comments),
                description = "Comments",
                onClick = {},
            )
            RailAction(
                painter = rememberVectorPainter(Icons.Outlined.Share),
                label = null,
                description = "Share",
                onClick = {},
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 88.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(reel.avatarColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = reel.creatorInitial,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "@${reel.creatorHandle}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = if (following) Color.White.copy(alpha = 0.25f) else Teal,
                            shape = RoundedCornerShape(50),
                        )
                        .clickable { following = !following }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = if (following) "Following" else "Follow",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = reel.caption,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = reel.audioLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RailAction(
    painter: Painter,
    label: String?,
    description: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Icon(
            painter = painter,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(32.dp),
        )
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000f).replace(".0M", "M")
    n >= 1_000 -> "%.1fK".format(n / 1_000f).replace(".0K", "K")
    else -> n.toString()
}