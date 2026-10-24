package social.ahtara.app.ui.screens

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt


enum class HeaderSection { Filters, Search, TopBar }








@Stable
class CollapsingHeaderState {
    var offset by mutableFloatStateOf(0f)
        private set

    private var filtersHeight by mutableIntStateOf(0)
    private var searchHeight by mutableIntStateOf(0)
    private var topBarHeight by mutableIntStateOf(0)

    val maxOffset: Float get() = (filtersHeight + searchHeight + topBarHeight).toFloat()

    
    val fraction: Float get() = if (maxOffset > 0f) (offset / maxOffset).coerceIn(0f, 1f) else 0f

    internal fun report(section: HeaderSection, fullHeight: Int) {
        when (section) {
            HeaderSection.Filters -> if (filtersHeight != fullHeight) filtersHeight = fullHeight
            HeaderSection.Search -> if (searchHeight != fullHeight) searchHeight = fullHeight
            HeaderSection.TopBar -> if (topBarHeight != fullHeight) topBarHeight = fullHeight
        }
    }

    internal fun hiddenFor(section: HeaderSection): Float = when (section) {
        HeaderSection.Filters -> offset.coerceIn(0f, filtersHeight.toFloat())
        HeaderSection.Search -> (offset - filtersHeight).coerceIn(0f, searchHeight.toFloat())
        HeaderSection.TopBar ->
            (offset - filtersHeight - searchHeight).coerceIn(0f, topBarHeight.toFloat())
    }

    
    internal fun scrollBy(dy: Float): Float {
        val old = offset
        offset = (old - dy).coerceIn(0f, maxOffset)
        return old - offset
    }

    
    suspend fun animateShow() {
        if (offset > 0f) animate(offset, 0f) { value, _ -> offset = value }
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            Offset(0f, scrollBy(available.y))
    }
}





fun Modifier.collapsingSection(state: CollapsingHeaderState, section: HeaderSection): Modifier =
    this
        .clipToBounds()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(
                constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity),
            )
            state.report(section, placeable.height)
            val hidden = state.hiddenFor(section).roundToInt().coerceIn(0, placeable.height)
            val hiddenFraction =
                if (placeable.height > 0) hidden.toFloat() / placeable.height else 0f
            layout(placeable.width, placeable.height - hidden) {
                placeable.placeRelativeWithLayer(0, -hidden) { alpha = 1f - hiddenFraction }
            }
        }