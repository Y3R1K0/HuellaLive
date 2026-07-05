package com.huellalive.app.ui.map

import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.ui.location.GeoPoint
import com.huellalive.app.ui.location.rememberCurrentLocationRequester
import com.huellalive.app.ui.theme.DustyRose
import com.huellalive.app.ui.theme.Surface as SurfaceColor
import com.huellalive.app.ui.theme.TextPrimary
import com.huellalive.app.ui.theme.TextSecondary
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val OSM_RASTER_STYLE = """
{
  "version": 8,
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "© OpenStreetMap contributors"
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
actual fun ShelterMap(
    shelters: List<ShelterProfileDto>,
    modifier: Modifier,
    onShelterClick: (ShelterProfileDto) -> Unit
) {
    val context = LocalContext.current
    val currentOnShelterClick = rememberUpdatedState(onShelterClick)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var selectedShelter by remember { mutableStateOf<ShelterProfileDto?>(null) }
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var locationMessage by remember { mutableStateOf<String?>(null) }

    val requestLocation = rememberCurrentLocationRequester(
        onLocation = { point ->
            userLocation = point
            locationMessage = null
            map?.cameraPosition = CameraPosition.Builder()
                .target(LatLng(point.latitude, point.longitude))
                .zoom(13.5)
                .build()
        },
        onError = { locationMessage = it }
    )

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
                        readyMap.setStyle(Style.Builder().fromJson(OSM_RASTER_STYLE)) {
                            styleReady = true
                        }
                    }
                }
            }
        )

        LaunchedEffect(map, styleReady, shelters, userLocation) {
            val readyMap = map ?: return@LaunchedEffect
            if (!styleReady) return@LaunchedEffect

            readyMap.clear()
            val markerShelterIds = mutableMapOf<Long, String>()
            val locatedShelters = shelters.filter { it.latitude != null && it.longitude != null }

            locatedShelters.forEach { shelter ->
                val marker = readyMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(shelter.latitude!!, shelter.longitude!!))
                        .title(shelter.user.name)
                        .snippet(shelter.location ?: "Albergue HuellaLive")
                )
                markerShelterIds[marker.id] = shelter.id
            }

            userLocation?.let { point ->
                readyMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(point.latitude, point.longitude))
                        .title("Tu ubicacion")
                )
            }

            readyMap.setOnMarkerClickListener { marker ->
                val shelterId = markerShelterIds[marker.id]
                selectedShelter = shelters.firstOrNull { it.id == shelterId }
                selectedShelter == null
            }

            if (userLocation == null && locatedShelters.isNotEmpty()) {
                val first = locatedShelters.first()
                readyMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(first.latitude!!, first.longitude!!))
                    .zoom(if (locatedShelters.size == 1) 13.5 else 10.5)
                    .build()
            }
        }

        IconButton(
            onClick = requestLocation,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .background(SurfaceColor, CircleShape)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Centrar en mi ubicacion", tint = DustyRose)
        }

        val locatedCount = shelters.count { it.latitude != null && it.longitude != null }
        if (locatedCount == 0) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            ) {
                Text(
                    if (shelters.isEmpty()) {
                        "No hay albergues registrados."
                    } else {
                        "Hay ${shelters.size} albergue(s) registrado(s), pero aun no agregan su ubicacion."
                    },
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        locationMessage?.let { message ->
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
            ) {
                Text(message, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            }
        }

        selectedShelter?.let { shelter ->
            ShelterMapCard(
                shelter = shelter,
                userLocation = userLocation,
                onProfile = { currentOnShelterClick.value(shelter) },
                onDirections = { openDirections(context, shelter) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        Text(
            "© OpenStreetMap",
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ShelterMapCard(
    shelter: ShelterProfileDto,
    userLocation: GeoPoint?,
    onProfile: () -> Unit,
    onDirections: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distance = if (userLocation != null && shelter.latitude != null && shelter.longitude != null) {
        distanceKm(userLocation.latitude, userLocation.longitude, shelter.latitude, shelter.longitude)
    } else null

    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth().padding(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = DustyRose.copy(alpha = 0.18f), shape = CircleShape) {
                Icon(Icons.Default.Pets, null, tint = DustyRose, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(shelter.user.name, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(shelter.location ?: "Sin direccion", color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                }
                distance?.let {
                    Text("${formatDistance(it)} de distancia", color = DustyRose, fontSize = 12.sp)
                }
            }
            IconButton(onClick = onDirections) {
                Icon(Icons.Default.Directions, contentDescription = "Como llegar", tint = DustyRose)
            }
            IconButton(onClick = onProfile) {
                Icon(Icons.Default.Visibility, contentDescription = "Ver albergue", tint = TextPrimary)
            }
        }
    }
}

private fun openDirections(context: android.content.Context, shelter: ShelterProfileDto) {
    val latitude = shelter.latitude ?: return
    val longitude = shelter.longitude ?: return
    val label = Uri.encode(shelter.user.name)
    val uri = Uri.parse("geo:0,0?q=$latitude,$longitude($label)")
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(Intent.createChooser(intent, "Como llegar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLng / 2) * sin(dLng / 2)
    return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
}

private fun formatDistance(distanceKm: Double): String =
    if (distanceKm < 1) "${(distanceKm * 1000).toInt()} m" else "${"%.1f".format(distanceKm)} km"
