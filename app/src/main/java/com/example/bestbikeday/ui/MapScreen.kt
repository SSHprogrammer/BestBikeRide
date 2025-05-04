package com.example.bestbikeday.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.bestbikeday.data.Location

@Composable
fun MapScreen(
    selectedLocation: Location?,
    onLocationSelected: (Location) -> Unit,
    modifier: Modifier = Modifier
) {
    var mapProperties by remember {
        mutableStateOf(
            MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = true
            )
        )
    }
    
    var cameraPositionState by remember {
        mutableStateOf(
            CameraPositionState(
                position = CameraPosition.fromLatLngZoom(
                    LatLng(
                        selectedLocation?.lat ?: 48.8566,
                        selectedLocation?.lon ?: 2.3522
                    ),
                    10f
                )
            )
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true
            )
        ) {
            selectedLocation?.let { location ->
                Marker(
                    state = MarkerState(
                        position = LatLng(location.lat, location.lon)
                    ),
                    title = location.name,
                    snippet = location.displayName
                )
            }
        }

        // Theme Toggle Button
        IconButton(
            onClick = {
                mapProperties = mapProperties.copy(
                    mapType = when (mapProperties.mapType) {
                        MapType.NORMAL -> MapType.SATELLITE
                        MapType.SATELLITE -> MapType.HYBRID
                        MapType.HYBRID -> MapType.TERRAIN
                        MapType.TERRAIN -> MapType.NORMAL
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = when (mapProperties.mapType) {
                    MapType.NORMAL -> Icons.Default.Map
                    MapType.SATELLITE -> Icons.Default.Satellite
                    MapType.HYBRID -> Icons.Default.Layers
                    MapType.TERRAIN -> Icons.Default.Terrain
                },
                contentDescription = "Toggle Map Type"
            )
        }
    }
} 