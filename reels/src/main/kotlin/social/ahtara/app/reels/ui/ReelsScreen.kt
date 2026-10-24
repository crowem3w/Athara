package social.ahtara.app.reels.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import social.ahtara.app.reels.R
import social.ahtara.app.reels.Reel
import social.ahtara.app.reels.ReelSource

@Composable
fun ReelsScreen(reels: List<Reel>, onBack: () -> Unit) {
    if (reels.isEmpty()) {
        ReelsEmpty(onBack)
        return
    }
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { reels.size })

    
    val videoIndexByPage = remember(reels) {
        var next = 0
        reels.map { if (it.source is ReelSource.Video) next++ else null }
    }

    
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("Reels", "ExoPlayer error: ${error.errorCodeName}", error)
                }
            })
            setMediaItems(reels.mapNotNull { (it.source as? ReelSource.Video)?.let { v -> MediaItem.fromUri(v.url) } })
            prepare()
        }
    }

    val currentVideoIndex by rememberUpdatedState(videoIndexByPage.getOrNull(pagerState.settledPage))

    LaunchedEffect(pagerState.settledPage) {
        val videoIndex = videoIndexByPage[pagerState.settledPage]
        if (videoIndex == null) {
            player.pause()
        } else {
            player.seekToDefaultPosition(videoIndex)
            player.playWhenReady = true
        }
    }

    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> if (currentVideoIndex != null) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
        ) { page ->
            ReelPage(
                reel = reels[page],
                player = player,
                isCurrent = page == pagerState.settledPage,
            )
        }

        
        
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(start = 18.dp, top = 13.5.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_reels_back),
                contentDescription = "Back",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun ReelsEmpty(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Text(
            text = "No reels yet.",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.align(Alignment.Center),
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(start = 18.dp, top = 13.5.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_reels_back),
                contentDescription = "Back",
                tint = Color.White,
            )
        }
    }
}