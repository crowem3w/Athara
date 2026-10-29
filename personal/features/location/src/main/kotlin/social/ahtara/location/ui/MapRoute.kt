package social.ahtara.location.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.authentication.domain.ServiceArea
import social.ahtara.authentication.ui.LocationStepScreen
import social.ahtara.location.MapViewModel
import social.ahtara.location.R
import social.ahtara.location.UiEvent
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap






@Composable
fun MapRoute(
    initialPlaces: List<PlaceSelection>,
    preHome: Boolean = false,
    accountType: AccountType = AccountType.PERSONAL,
    onSkip: (() -> Unit)? = null,
    onBack: () -> Unit,
    onConfirm: (List<PlaceSelection>, String) -> Unit,
    viewModel: MapViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    @SuppressLint("MissingPermission")
    fun enableLocation() {
        val m = map ?: return
        val style = m.style ?: return
        val lc = m.locationComponent
        if (!lc.isLocationComponentActivated) {
            lc.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style)
                    .locationComponentOptions(
                        LocationComponentOptions.builder(context).pulseEnabled(true).build()
                    )
                    .useDefaultLocationEngine(true)
                    .build()
            )
        }
        lc.isLocationComponentEnabled = true
        lc.cameraMode = CameraMode.TRACKING
        lc.lastKnownLocation?.let {
            m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15.0), 1200)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) enableLocation() else viewModel.onLocationPermissionDenied()
    }

    fun onLocateClicked() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            enableLocation()
        } else {
            if (activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == true) {
                Toast.makeText(context, R.string.location_permission_rationale, Toast.LENGTH_LONG).show()
            }
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    
    // Business / Organization set exactly one location.
    LaunchedEffect(accountType) { viewModel.singleSelect = accountType == AccountType.BUSINESS }

    LaunchedEffect(initialPlaces) { if (initialPlaces.isNotEmpty()) viewModel.preselect(initialPlaces) }
    
    var centeredOnInitial by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(map, initialPlaces) {
        val m = map ?: return@LaunchedEffect
        if (initialPlaces.isEmpty()) return@LaunchedEffect
        if (!centeredOnInitial) {
            centeredOnInitial = true
            val points = initialPlaces.map { LatLng(it.latitude, it.longitude) }
            m.moveCamera(
                if (points.size == 1) CameraUpdateFactory.newLatLngZoom(points[0], 9.5)
                else CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(points).build(), 100)
            )
        }
    }

    
    LaunchedEffect(viewModel, map) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> Toast.makeText(
                    context,
                    context.getString(event.resId, *event.formatArgs.toTypedArray()),
                    if (event.long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
                is UiEvent.FlyTo -> map?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(event.target, event.zoom), 1200
                )
            }
        }
    }

    // No saved place yet: open on the whole island of Guimaras.
    LaunchedEffect(map) {
        val m = map ?: return@LaunchedEffect
        if (initialPlaces.isEmpty()) {
            m.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(ServiceArea.CENTER_LAT, ServiceArea.CENTER_LNG), 9.5,
                ),
            )
        }
    }

    LocationStepScreen(
        onBack = onBack,
        preHome = preHome,
        accountType = accountType,
        map = { bottomInset ->
            
            val bottomPx = with(LocalDensity.current) { bottomInset.roundToPx() }
            LaunchedEffect(map, bottomPx) { map?.setPadding(0, 0, 0, bottomPx) }

            MapContent(
                droppedMarkers = state.places.map { LatLng(it.latitude, it.longitude) },
                activeSearchMarker = null,
                onMapReady = { map = it },
                modifier = Modifier.fillMaxSize()
            )
            FloatingActionButton(
                onClick = ::onLocateClicked,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = bottomInset + 16.dp)
                    .size(FAB_SIZE),
                shape = CircleShape,
                containerColor = SurfaceWhite
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_my_location),
                    contentDescription = stringResource(R.string.cd_locate_me),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        footer = {
            LocationPanelFooter(
                canConfirm = state.places.isNotEmpty() &&
                    (accountType == AccountType.PERSONAL || state.address.isNotBlank()),
                onConfirm = { onConfirm(state.places, state.address.trim()) },
                onSkip = onSkip,
            )
        },
    ) {
        LocationPanelContent(
            query = state.query,
            results = state.results,
            places = state.places,
            categories = state.categories,
            onQueryChange = viewModel::onQueryChange,
            onSearchSubmit = viewModel::onSearchSubmit,
            onToggleCategory = viewModel::onToggleCategory,
            onResultClick = viewModel::onResultSelected,
            onRemovePlace = viewModel::onRemovePlace,
            onQuickPick = viewModel::onQuickPick,
            accountType = accountType,
            address = state.address,
            onAddressChange = viewModel::onAddressChange,
        )
    }
}

private val FAB_SIZE = 48.dp