package social.ahtara.app.location.ui

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import social.ahtara.app.location.R
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import social.ahtara.authentication.domain.ServiceArea
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val MARKERS_SOURCE_ID = "dropped-markers-source"
private const val MARKERS_LAYER_ID = "dropped-markers-layer"
private const val PIN_ICON_ID = "pin-icon"
private const val ACTIVE_SEARCH_SOURCE_ID = "active-search-source"
private const val ACTIVE_SEARCH_LAYER_ID = "active-search-layer"
private const val SEARCH_PIN_ICON_ID = "search-pin-icon"






@Composable
fun MapContent(
    droppedMarkers: List<LatLng>,
    activeSearchMarker: LatLng?,
    onMapReady: (MapLibreMap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { MapView(context) }

    
    var savedCamera by rememberSaveable { mutableStateOf<CameraPosition?>(null) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var markerSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    var searchSource by remember { mutableStateOf<GeoJsonSource?>(null) }

    val currentOnMapReady by rememberUpdatedState(onMapReady)

    DisposableEffect(mapView, lifecycle) {
        mapView.onCreate(Bundle())
        mapView.getMapAsync { m ->
            map = m
            m.cameraPosition = savedCamera
                ?: CameraPosition.Builder()
                    .target(LatLng(ServiceArea.CENTER_LAT, ServiceArea.CENTER_LNG)).zoom(9.5).build()
            // Guimaras only: keep the map on the island and stop zooming out to the world view.
            m.setLatLngBoundsForCameraTarget(
                LatLngBounds.Builder()
                    .include(LatLng(ServiceArea.SOUTH, ServiceArea.WEST))
                    .include(LatLng(ServiceArea.NORTH, ServiceArea.EAST))
                    .build()
            )
            m.setMinZoomPreference(8.5)
            m.addOnCameraIdleListener { savedCamera = m.cameraPosition }
            m.setStyle(STYLE_URL) { style ->
                markerSource = style.addPinLayer(
                    context, MARKERS_SOURCE_ID, MARKERS_LAYER_ID, PIN_ICON_ID, R.drawable.ic_marker_pin
                )
                searchSource = style.addPinLayer(
                    context, ACTIVE_SEARCH_SOURCE_ID, ACTIVE_SEARCH_LAYER_ID,
                    SEARCH_PIN_ICON_ID, R.drawable.ic_search_pin
                )
            }
            currentOnMapReady(m)
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()

        val callbacks = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() = mapView.onLowMemory()
            override fun onTrimMemory(level: Int) = Unit
        }
        context.registerComponentCallbacks(callbacks)

        onDispose {
            lifecycle.removeObserver(observer)
            context.unregisterComponentCallbacks(callbacks)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(markerSource, droppedMarkers) {
        markerSource?.setGeoJson(pointsGeoJson(droppedMarkers))
    }
    LaunchedEffect(searchSource, activeSearchMarker) {
        searchSource?.setGeoJson(pointsGeoJson(listOfNotNull(activeSearchMarker)))
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun Style.addPinLayer(
    context: android.content.Context,
    sourceId: String,
    layerId: String,
    iconId: String,
    @DrawableRes drawable: Int
): GeoJsonSource {
    addImage(iconId, bitmapFromDrawable(context, drawable))
    val source = GeoJsonSource(sourceId, pointsGeoJson(emptyList()))
    addSource(source)
    addLayer(
        SymbolLayer(layerId, sourceId).withProperties(
            PropertyFactory.iconImage(iconId),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true)
        )
    )
    return source
}

private fun bitmapFromDrawable(context: android.content.Context, @DrawableRes res: Int): Bitmap {
    val drawable = ContextCompat.getDrawable(context, res)!!
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth.coerceAtLeast(1),
        drawable.intrinsicHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}


private fun pointsGeoJson(points: List<LatLng>): String {
    val features = JSONArray()
    for (p in points) {
        features.put(
            JSONObject()
                .put("type", "Feature")
                .put(
                    "geometry",
                    JSONObject().put("type", "Point")
                        .put("coordinates", JSONArray().put(p.longitude).put(p.latitude))
                )
                .put("properties", JSONObject())
        )
    }
    return JSONObject().put("type", "FeatureCollection").put("features", features).toString()
}