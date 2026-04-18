package mx.visionebc.actorstoolkit.ui.screen

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

private fun ensureOsmConfigured(ctx: Context) {
    val cfg = Configuration.getInstance()
    cfg.userAgentValue = ctx.packageName
    cfg.osmdroidBasePath = ctx.getExternalFilesDir(null) ?: ctx.filesDir
    cfg.osmdroidTileCache = ctx.getExternalFilesDir("osmdroid-tiles") ?: ctx.filesDir
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerDialog(
    initialLat: Double?,
    initialLng: Double?,
    initialAddress: String = "",
    onDismiss: () -> Unit,
    onConfirm: (lat: Double, lng: Double, address: String) -> Unit
) {
    val startLat = initialLat ?: 19.4326
    val startLng = initialLng ?: -99.1332
    val startZoom = if (initialLat != null) 15.0 else 5.0

    var pickedLat by remember { mutableStateOf(initialLat) }
    var pickedLng by remember { mutableStateOf(initialLng) }
    var address by remember { mutableStateOf(initialAddress) }
    var searching by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }

    LaunchedEffect(Unit) { ensureOsmConfigured(context) }

    fun placeOrMoveMarker(lat: Double, lng: Double, recenter: Boolean) {
        val mv = mapView ?: return
        val point = GeoPoint(lat, lng)
        val existing = marker
        if (existing == null) {
            val m = Marker(mv).apply {
                position = point
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                isDraggable = true
                setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                    override fun onMarkerDrag(m: Marker) {}
                    override fun onMarkerDragStart(m: Marker) {}
                    override fun onMarkerDragEnd(m: Marker) {
                        pickedLat = m.position.latitude
                        pickedLng = m.position.longitude
                        reverseGeocodeAsync(scope, context, m.position.latitude, m.position.longitude) {
                            address = it
                        }
                    }
                })
            }
            mv.overlays.add(m)
            marker = m
        } else {
            existing.position = point
        }
        if (recenter) mv.controller.animateTo(point)
        mv.invalidate()
    }

    fun runForwardGeocode() {
        if (address.isBlank()) return
        searching = true
        scope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(address, 1)
                val hit = results?.firstOrNull()
                if (hit != null) {
                    withContext(Dispatchers.Main) {
                        pickedLat = hit.latitude
                        pickedLng = hit.longitude
                        hit.getAddressLine(0)?.let { if (it.isNotBlank()) address = it }
                        placeOrMoveMarker(hit.latitude, hit.longitude, recenter = true)
                        mapView?.controller?.setZoom(16.0)
                    }
                }
            } catch (_: Exception) {}
            withContext(Dispatchers.Main) { searching = false }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Pick Location", style = MaterialTheme.typography.titleMedium)
                            if (pickedLat != null && pickedLng != null) {
                                Text(
                                    String.format("%.5f, %.5f", pickedLat, pickedLng),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text("Type an address or tap the map", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cancel") }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val la = pickedLat; val lo = pickedLng
                                if (la != null && lo != null) onConfirm(la, lo, address.trim())
                            },
                            enabled = pickedLat != null && pickedLng != null
                        ) {
                            Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Use")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    placeholder = { Text("Type an address and search") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    trailingIcon = {
                        IconButton(onClick = { keyboard?.hide(); runForwardGeocode() }, enabled = address.isNotBlank() && !searching) {
                            if (searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Search, "Search")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); runForwardGeocode() }),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                        factory = { ctx ->
                            ensureOsmConfigured(ctx)
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                setUseDataConnection(true)
                                controller.setZoom(startZoom)
                                controller.setCenter(GeoPoint(startLat, startLng))

                                val tapReceiver = object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        pickedLat = p.latitude
                                        pickedLng = p.longitude
                                        placeOrMoveMarker(p.latitude, p.longitude, recenter = false)
                                        reverseGeocodeAsync(scope, ctx, p.latitude, p.longitude) {
                                            address = it
                                        }
                                        return true
                                    }
                                    override fun longPressHelper(p: GeoPoint): Boolean = false
                                }
                                overlays.add(0, MapEventsOverlay(tapReceiver))

                                mapView = this

                                if (initialLat != null && initialLng != null) {
                                    val m = Marker(this).apply {
                                        position = GeoPoint(initialLat, initialLng)
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        isDraggable = true
                                        setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                            override fun onMarkerDrag(m: Marker) {}
                                            override fun onMarkerDragStart(m: Marker) {}
                                            override fun onMarkerDragEnd(m: Marker) {
                                                pickedLat = m.position.latitude
                                                pickedLng = m.position.longitude
                                                reverseGeocodeAsync(scope, ctx, m.position.latitude, m.position.longitude) {
                                                    address = it
                                                }
                                            }
                                        })
                                    }
                                    overlays.add(m)
                                    marker = m
                                    invalidate()
                                }
                            }
                        },
                        onRelease = { mv ->
                            mv.onDetach()
                        }
                    )
                }
            }
        }
    }
}

private fun reverseGeocodeAsync(
    scope: kotlinx.coroutines.CoroutineScope,
    ctx: Context,
    lat: Double,
    lng: Double,
    onResult: (String) -> Unit
) {
    scope.launch(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(ctx, Locale.getDefault())
            @Suppress("DEPRECATION")
            val results = geocoder.getFromLocation(lat, lng, 1)
            val formatted = results?.firstOrNull()?.getAddressLine(0)
            if (!formatted.isNullOrBlank()) {
                withContext(Dispatchers.Main) { onResult(formatted) }
            }
        } catch (_: Exception) {}
    }
}
