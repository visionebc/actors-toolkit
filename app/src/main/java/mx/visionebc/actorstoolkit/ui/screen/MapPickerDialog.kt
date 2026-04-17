package mx.visionebc.actorstoolkit.ui.screen

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerDialog(
    initialLat: Double?,
    initialLng: Double?,
    onDismiss: () -> Unit,
    onConfirm: (lat: Double, lng: Double) -> Unit
) {
    val startLat = initialLat ?: 19.4326
    val startLng = initialLng ?: -99.1332
    val startZoom = if (initialLat != null) 15 else 5

    var pickedLat by remember { mutableStateOf(initialLat) }
    var pickedLng by remember { mutableStateOf(initialLng) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
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
                                Text("Tap the map to drop a pin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                if (la != null && lo != null) onConfirm(la, lo)
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

                Box(Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            @SuppressLint("SetJavaScriptEnabled")
                            val wv = WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = WebViewClient()
                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onPick(lat: Double, lng: Double) {
                                        pickedLat = lat
                                        pickedLng = lng
                                    }
                                }, "AndroidBridge")
                                loadDataWithBaseURL(
                                    "https://openstreetmap.org/",
                                    buildLeafletHtml(startLat, startLng, startZoom, initialLat, initialLng),
                                    "text/html", "utf-8", null
                                )
                            }
                            wv
                        }
                    )
                }
            }
        }
    }
}

private fun buildLeafletHtml(centerLat: Double, centerLng: Double, zoom: Int, pinLat: Double?, pinLng: Double?): String {
    val initialMarker = if (pinLat != null && pinLng != null) {
        "var marker = L.marker([$pinLat, $pinLng], {draggable:true}).addTo(map);" +
            "marker.on('dragend', function(e){ var p = marker.getLatLng(); AndroidBridge.onPick(p.lat, p.lng); });"
    } else {
        "var marker = null;"
    }
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset='utf-8'>
<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>
<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css' />
<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>
<style>html,body,#map{height:100%;margin:0;padding:0;}</style>
</head>
<body>
<div id='map'></div>
<script>
var map = L.map('map').setView([$centerLat, $centerLng], $zoom);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; OpenStreetMap'
}).addTo(map);
$initialMarker
map.on('click', function(e){
    if (marker) { marker.setLatLng(e.latlng); }
    else {
        marker = L.marker(e.latlng, {draggable:true}).addTo(map);
        marker.on('dragend', function(ev){ var p = marker.getLatLng(); AndroidBridge.onPick(p.lat, p.lng); });
    }
    AndroidBridge.onPick(e.latlng.lat, e.latlng.lng);
});
</script>
</body>
</html>
""".trimIndent()
}
