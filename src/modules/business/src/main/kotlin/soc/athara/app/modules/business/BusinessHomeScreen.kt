package soc.athara.app.modules.business

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import soc.athara.app.core.identityapi.AuthUser
import soc.athara.app.modules.business.home.CollapsingHeaderState
import soc.athara.app.modules.business.home.collapsingSection
import soc.athara.app.modules.business.home.components.BottomNavBar
import soc.athara.app.modules.business.home.components.TopBar
import soc.athara.app.modules.business.reader.BleCollector
import soc.athara.app.shared.designsystem.SyncNoticeHost


private const val ReaderButtonHideEnd = 0.5f






@Composable
internal fun BusinessHomeScreen(user: AuthUser?, onSignOut: () -> Unit, onLogin: () -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val headerState = remember { CollapsingHeaderState() }
    var bottomBarHeight by remember { mutableIntStateOf(0) }
    var scanning by rememberSaveable { mutableStateOf(false) }
    var showProfile by rememberSaveable { mutableStateOf(false) }

    val scrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val headerElevation by animateDpAsState(targetValue = if (scrolled) 4.dp else 0.dp, label = "headerElevation")

    SyncNoticeHost()

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .onSizeChanged { bottomBarHeight = it.height }
                    .graphicsLayer { translationY = bottomBarHeight * headerState.fraction },
            ) {
                BottomNavBar(
                    onReadClick = {
                        if (user == null) onLogin() else scanning = !scanning
                    },
                    readerVisibility = { 1f - (headerState.fraction / ReaderButtonHideEnd).coerceIn(0f, 1f) },
                )
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().nestedScroll(headerState.nestedScrollConnection)) {
            Surface(
                modifier = Modifier.fillMaxWidth().zIndex(1f),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = headerElevation,
            ) {
                Column(
                    modifier = Modifier.padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection),
                    ),
                ) {
                    Box(Modifier.collapsingSection(headerState)) {
                        TopBar(
                            onLogoClick = {
                                scope.launch {
                                    listState.animateScrollToItem(0)
                                    headerState.animateShow()
                                }
                            },
                            onProfileClick = { if (user == null) onLogin() else showProfile = true },
                            userInitial = user?.let { it.displayName.trim().firstOrNull()?.uppercase() ?: "?" },
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    start = 24.dp + innerPadding.calculateStartPadding(layoutDirection),
                    end = 24.dp + innerPadding.calculateEndPadding(layoutDirection),
                    top = 16.dp,
                    bottom = 24.dp + innerPadding.calculateBottomPadding(),
                ),
            ) {
                item {
                    Text(
                        text = if (user != null) stringResource(R.string.business_home_greeting, user.displayName)
                        else stringResource(R.string.business_home_greeting_guest),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                item {
                    Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.business_collect_payment), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.business_collect_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                if (user == null) {
                    item {
                        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(52.dp)) {
                            Text(stringResource(R.string.business_guest_cta))
                        }
                    }
                }
            }
        }
    }

    
    if (user != null) {
        val barHeight = with(LocalDensity.current) { bottomBarHeight.toDp() }
        Box(Modifier.fillMaxSize()) {
            val pillModifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = barHeight + 12.dp)
                .graphicsLayer { translationY = bottomBarHeight * headerState.fraction }
            BleCollector(scanning = scanning, onScanningChange = { scanning = it }, pillModifier = pillModifier)
        }
    }

    if (showProfile && user != null) {
        AlertDialog(
            onDismissRequest = { showProfile = false },
            title = { Text(user.displayName) },
            text = { Text(stringResource(R.string.business_profile_login, user.identifier)) },
            confirmButton = {
                TextButton(onClick = { showProfile = false; onSignOut() }) { Text(stringResource(R.string.business_action_sign_out)) }
            },
            dismissButton = {
                TextButton(onClick = { showProfile = false }) { Text(stringResource(R.string.business_profile_close)) }
            },
        )
    }
}
