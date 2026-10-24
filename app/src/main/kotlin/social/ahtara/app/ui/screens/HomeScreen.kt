package social.ahtara.app.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.app.ui.components.BottomNavBar
import social.ahtara.app.ui.components.SearchBar
import social.ahtara.app.ui.components.TopBar
import social.ahtara.app.ui.components.FilterTabs
import social.ahtara.socialfeed.feed.rememberSocialFeedState
import social.ahtara.travel.model.PlaceCategory
import social.ahtara.socialfeed.ui.socialFeedItems


private const val CreateButtonHideEnd = 0.5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    
    places: List<PlaceSelection> = emptyList(),
    
    categories: Set<PlaceCategory> = emptySet(),
    onToggleCategory: (PlaceCategory) -> Unit = {},
    onClearCategories: () -> Unit = {},
    userInitial: String = "M",
    onPlacesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onReelsClick: () -> Unit = {},
    onCreateClick: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current

    
    val headerState = remember { CollapsingHeaderState() }
    var bottomBarHeight by remember { mutableIntStateOf(0) }

    
    val feed = rememberSocialFeedState()

    
    var isRefreshing by remember { mutableStateOf(false) }

    val loadPosts: suspend (Boolean) -> Unit = { force -> feed.load(places, force) }

    val refresh: () -> Unit = {
        if (!isRefreshing) {
            scope.launch {
                isRefreshing = true
                try {
                    loadPosts(true) 
                } finally {
                    isRefreshing = false
                }
            }
        }
    }

    
    LaunchedEffect(places) {
        isRefreshing = true
        try {
            loadPosts(false)
        } finally {
            isRefreshing = false
        }
    }

    
    
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) headerState.animateShow()
    }

    
    
    val onLogoClick: () -> Unit = {
        scope.launch { listState.animateScrollToItem(0) }
        refresh()
    }

    
    val feedScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val headerElevation by animateDpAsState(
        targetValue = if (feedScrolled) 4.dp else 0.dp,
        label = "headerElevation",
    )

    
    
    val thresholdPx = with(LocalDensity.current) { PullToRefreshDefaults.PositionalThreshold.toPx() }

    Scaffold(
        bottomBar = {
            
            Box(
                modifier = Modifier
                    .onSizeChanged { bottomBarHeight = it.height }
                    .graphicsLayer { translationY = bottomBarHeight * headerState.fraction },
            ) {
                BottomNavBar(
                    onReelsClick = onReelsClick,
                    onCreateClick = onCreateClick,
                    onPlacesClick = onPlacesClick,
                    
                    createVisibility = {
                        1f - (headerState.fraction / CreateButtonHideEnd).coerceIn(0f, 1f)
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(headerState.nestedScrollConnection),
        ) {
            
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f),
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
                    
                    Box(Modifier.collapsingSection(headerState, HeaderSection.TopBar)) {
                        TopBar(
                            onLogoClick = onLogoClick,
                            onProfileClick = onProfileClick,
                            userInitial = userInitial,
                        )
                    }
                    Column(Modifier.collapsingSection(headerState, HeaderSection.Search)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SearchBar()
                    }
                    Column(Modifier.collapsingSection(headerState, HeaderSection.Filters)) {
                        Spacer(modifier = Modifier.height(16.dp))
                        FilterTabs(
                            selected = categories,
                            onToggle = onToggleCategory,
                            onClear = onClearCategories,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = refresh,
                state = pullState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = isRefreshing,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                },
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = pullState.distanceFraction * thresholdPx },
                    state = listState,
                    contentPadding = PaddingValues(
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection),
                        bottom = innerPadding.calculateBottomPadding(),
                    ),
                ) {
                    socialFeedItems(
                        state = feed,
                        isLoading = isRefreshing,
                        hasPlaces = places.isNotEmpty(),
                        categories = categories,
                    )
                }
            }
        }
    }
}