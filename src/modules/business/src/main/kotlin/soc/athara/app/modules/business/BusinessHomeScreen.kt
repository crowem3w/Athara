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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import soc.athara.app.core.identityapi.AuthUser
import soc.athara.app.modules.business.home.BusinessTab
import soc.athara.app.modules.business.home.CollapsingHeaderState
import soc.athara.app.modules.business.home.collapsingSection
import soc.athara.app.modules.business.home.components.BottomNavBar
import soc.athara.app.modules.business.home.components.TopBar
import soc.athara.app.modules.business.model.BookingStatus
import soc.athara.app.modules.business.model.BusinessState
import soc.athara.app.modules.business.reader.BleCollector
import soc.athara.app.modules.business.tabs.AccountTab
import soc.athara.app.modules.business.tabs.BookingsTab
import soc.athara.app.modules.business.tabs.HomeActions
import soc.athara.app.modules.business.tabs.HomeTab
import soc.athara.app.modules.business.tabs.StoreTab
import soc.athara.app.shared.designsystem.SyncNoticeHost

private const val ReaderButtonHideEnd = 0.5f

/**
 * Authenticated business shell: top bar + four tabs + the existing centre "Collect payment" button.
 * Navigation between tabs is plain state; swap for Navigation Compose once screens need deep links.
 */
@Composable
internal fun BusinessHomeScreen(user: AuthUser?, onSignOut: () -> Unit, onLogin: () -> Unit) {
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val headerState = remember { CollapsingHeaderState() }
    val snackbar = remember { SnackbarHostState() }
    val state = remember { BusinessState() }

    var bottomBarHeight by remember { mutableIntStateOf(0) }
    var scanning by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(BusinessTab.Home) }

    // One-shot requests that Home shortcuts hand to other tabs.
    var bookingFilter by rememberSaveable { mutableStateOf<BookingStatus?>(null) }
    var openAvailability by rememberSaveable { mutableStateOf(false) }
    var openAddOffering by rememberSaveable { mutableStateOf(false) }

    val listStates = BusinessTab.entries.associateWith { rememberLazyListState() }
    val listState = listStates.getValue(tab)
    val scrolled by remember(tab) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val headerElevation by animateDpAsState(targetValue = if (scrolled) 4.dp else 0.dp, label = "headerElevation")

    fun go(target: BusinessTab) {
        tab = target
        scope.launch { headerState.animateShow() }
    }
    val message: (String) -> Unit = { text -> scope.launch { snackbar.showSnackbar(text) } }
    val name = user?.displayName.orEmpty()

    SyncNoticeHost()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .onSizeChanged { bottomBarHeight = it.height }
                    .graphicsLayer { translationY = bottomBarHeight * headerState.fraction },
            ) {
                BottomNavBar(
                    selected = tab,
                    onSelect = { bookingFilter = null; go(it) },
                    bookingsBadge = state.pendingCount,
                    onReadClick = { if (user == null) onLogin() else scanning = !scanning },
                    readerVisibility = { 1f - (headerState.fraction / ReaderButtonHideEnd).coerceIn(0f, 1f) },
                )
            }
        },
    ) { innerPadding ->
        val contentPadding = PaddingValues(
            start = 20.dp + innerPadding.calculateStartPadding(layoutDirection),
            end = 20.dp + innerPadding.calculateEndPadding(layoutDirection),
            top = 12.dp,
            bottom = 32.dp + innerPadding.calculateBottomPadding(),
        )

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
                            businessName = name,
                            isOpen = state.isOpen,
                            hasUnread = state.pendingCount > 0,
                            onNameClick = {
                                scope.launch {
                                    listState.animateScrollToItem(0)
                                    headerState.animateShow()
                                }
                            },
                            onStatusClick = { go(BusinessTab.Store) },
                            onNotificationsClick = { message("You have no other notifications") },
                        )
                    }
                }
            }

            if (user == null) {
                GuestContent(listState = listStates.getValue(BusinessTab.Home), padding = contentPadding, onLogin = onLogin)
            } else when (tab) {
                BusinessTab.Home -> HomeTab(
                    state = state,
                    name = name,
                    listState = listState,
                    padding = contentPadding,
                    actions = HomeActions(
                        onBookings = { filter -> bookingFilter = filter; go(BusinessTab.Bookings) },
                        onAddOffering = { openAddOffering = true; go(BusinessTab.Store) },
                        onAvailability = { openAvailability = true; go(BusinessTab.Bookings) },
                        onStore = { go(BusinessTab.Store) },
                        onEarnings = { go(BusinessTab.Account) },
                    ),
                )
                BusinessTab.Bookings -> BookingsTab(
                    state = state,
                    initialFilter = bookingFilter,
                    openAvailability = openAvailability,
                    onAvailabilityHandled = { openAvailability = false },
                    listState = listState,
                    padding = contentPadding,
                    onMessage = message,
                )
                BusinessTab.Store -> StoreTab(
                    state = state,
                    businessName = name,
                    launchAdd = openAddOffering,
                    onLaunchHandled = { openAddOffering = false },
                    listState = listState,
                    padding = contentPadding,
                    onMessage = message,
                )
                BusinessTab.Account -> AccountTab(
                    businessName = name,
                    identifier = user.identifier,
                    listState = listState,
                    padding = contentPadding,
                    onMessage = message,
                    onSignOut = onSignOut,
                )
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
}

@Composable
private fun GuestContent(
    listState: androidx.compose.foundation.lazy.LazyListState,
    padding: PaddingValues,
    onLogin: () -> Unit,
) {
    LazyColumn(state = listState, contentPadding = padding) {
        item {
            Text(
                stringResource(R.string.business_home_greeting_guest),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(52.dp)) {
                Text(stringResource(R.string.business_guest_cta))
            }
        }
    }
}
