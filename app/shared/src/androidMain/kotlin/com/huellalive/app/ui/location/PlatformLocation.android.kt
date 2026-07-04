package com.huellalive.app.ui.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@Composable
actual fun rememberCurrentLocationRequester(
    onLocation: (GeoPoint) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val currentOnLocation = rememberUpdatedState(onLocation)
    val currentOnError = rememberUpdatedState(onError)
    val locationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun fetchLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            currentOnError.value("Permiso de ubicacion requerido")
            return
        }

        locationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location == null) {
                    currentOnError.value("No se pudo obtener la ubicacion actual")
                } else {
                    currentOnLocation.value(GeoPoint(location.latitude, location.longitude))
                }
            }
            .addOnFailureListener {
                currentOnError.value(it.message ?: "No se pudo obtener la ubicacion")
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            fetchLocation()
        } else {
            currentOnError.value("Permiso de ubicacion denegado")
        }
    }

    return remember(context, permissionLauncher) {
        {
            val fineGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (fineGranted || coarseGranted) {
                fetchLocation()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
}
