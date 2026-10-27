package social.ahtara.app.location

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.app.location.search.PlaceCategory
import social.ahtara.app.location.search.SearchPipeline
import social.ahtara.app.location.search.SearchResult
import org.maplibre.android.geometry.LatLng
import kotlin.coroutines.cancellation.CancellationException

data class MapUiState(
    val query: String = "",
    
    val categories: Set<PlaceCategory> = PlaceCategory.entries.toSet(),
    val results: List<SearchResult> = emptyList(),
    
    val places: List<PlaceSelection> = emptyList()
)


sealed interface UiEvent {
    data class ShowMessage(
        @StringRes val resId: Int,
        val formatArgs: List<Any> = emptyList(),
        val long: Boolean = false
    ) : UiEvent

    data class FlyTo(val target: LatLng, val zoom: Double) : UiEvent
}

class MapViewModel : ViewModel() {

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var searchJob: Job? = null
    private var initialApplied = false

    
    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        scheduleSearch(debounceMs = SEARCH_DEBOUNCE_MS)
    }

    
    fun onSearchSubmit() {
        scheduleSearch(debounceMs = 0)
    }

    
    fun onToggleCategory(category: PlaceCategory) {
        val current = _state.value.categories
        val next = if (category in current) current - category else current + category
        if (next.isEmpty()) return
        _state.update { it.copy(categories = next) }
        scheduleSearch(debounceMs = 0)
    }

    
    fun preselect(places: List<PlaceSelection>) {
        if (initialApplied) return
        initialApplied = true
        _state.update { it.copy(places = places.filter { p -> social.ahtara.authentication.domain.ServiceArea.contains(p) }) }
    }

    
    fun onResultSelected(result: SearchResult) {
        searchJob?.cancel()
        val place = PlaceSelection(result.fullName, result.latLng.latitude, result.latLng.longitude)
        _state.update {
            it.copy(query = "", results = emptyList(), places = it.places.addedOnce(place))
        }
        _events.trySend(UiEvent.FlyTo(result.latLng, zoomFor(result.category)))
    }

    
    fun onQuickPick(place: PlaceSelection) {
        _state.update { it.copy(places = it.places.addedOnce(place)) }
        val zoom = if (place == social.ahtara.authentication.domain.ServiceArea.default) 9.5 else 12.0
        _events.trySend(UiEvent.FlyTo(LatLng(place.latitude, place.longitude), zoom))
    }

    fun onRemovePlace(place: PlaceSelection) {
        _state.update { it.copy(places = it.places - place) }
    }

    fun onLocationPermissionDenied() {
        _events.trySend(UiEvent.ShowMessage(R.string.location_permission_denied, long = true))
    }

    private fun scheduleSearch(debounceMs: Long) {
        searchJob?.cancel()
        val query = _state.value.query.trim()
        val categories = _state.value.categories

        if (query.isEmpty()) {
            _state.update { it.copy(results = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            try {
                val results = withContext(Dispatchers.IO) { SearchPipeline.search(query, categories) }
                _state.update { it.copy(results = results) }
                if (results.isEmpty()) {
                    _events.trySend(UiEvent.ShowMessage(R.string.search_no_results, listOf(query)))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.trySend(UiEvent.ShowMessage(R.string.search_error, long = true))
            }
        }
    }

    private fun zoomFor(category: PlaceCategory): Double = when (category) {
        PlaceCategory.COUNTRY -> 9.5
        PlaceCategory.PROVINCE -> 9.5
        PlaceCategory.CITY -> 12.0
    }

    private companion object {
        
        const val SEARCH_DEBOUNCE_MS = 600L
    }
}


private fun PlaceSelection.isSameSpot(other: PlaceSelection): Boolean =
    kotlin.math.abs(latitude - other.latitude) < 0.0001 && kotlin.math.abs(longitude - other.longitude) < 0.0001

private fun List<PlaceSelection>.addedOnce(place: PlaceSelection): List<PlaceSelection> =
    if (any { it.isSameSpot(place) }) this else this + place