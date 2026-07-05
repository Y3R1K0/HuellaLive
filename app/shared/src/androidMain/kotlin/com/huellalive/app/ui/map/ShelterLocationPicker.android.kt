package com.huellalive.app.ui.map

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.huellalive.app.ui.location.GeoPoint
import com.huellalive.app.ui.theme.DustyRose
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val LOCATION_PICKER_STYLE = """
{
  "version": 8,
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "OpenStreetMap contributors"
    }
  },
  "layers": [
    {
      "id": "osm",
      "type": "raster",
      "source": "osm"
    }
  ]
}
"""

@Composable
actual fun ShelterLocationPicker(
    selectedPoint: GeoPoint?,
    modifier: Modifier,
    onPointChanged: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val currentOnPointChanged = rememberUpdatedState(onPointChanged)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_MOVE,
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL,
                    MotionEvent.ACTION_POINTER_UP -> {
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false
            }
        }
    }

    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(modifier = modifier.background(Color(0xFF111414))) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    getMapAsync { readyMap ->
                        map = readyMap
                        readyMap.uiSettings.apply {
                            isCompassEnabled = true
                            isAttributionEnabled = true
                            isLogoEnabled = false
                        }
                        readyMap.addOnMapClickListener { point ->
                            currentOnPointChanged.value(GeoPoint(point.latitude, point.longitude))
                            true
                        }
                        readyMap.setStyle(Style.Builder().fromJson(LOCATION_PICKER_STYLE)) {
                            styleReady = true
                        }
                    }
                }
            }
        )

        LaunchedEffect(map, styleReady, selectedPoint) {
            val readyMap = map ?: return@LaunchedEffect
            val point = selectedPoint ?: return@LaunchedEffect
            if (!styleReady) return@LaunchedEffect
            readyMap.clear()
            readyMap.addMarker(
                MarkerOptions()
                    .position(LatLng(point.latitude, point.longitude))
                    .title("Ubicacion del albergue")
            )
            readyMap.cameraPosition = CameraPosition.Builder()
                .target(LatLng(point.latitude, point.longitude))
                .zoom(16.0)
                .build()
        }

        if (selectedPoint == null) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = DustyRose,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
