package com.xiaoyin.lifeatlas.core.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions

@Composable
fun AmapMapView(
    markers: List<MapMarkerItem>,
    onMarkerClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            view.renderMarkers(
                markers = markers,
                onMarkerClick = onMarkerClick
            )
        }
    )
}

private fun MapView.renderMarkers(
    markers: List<MapMarkerItem>,
    onMarkerClick: (Long) -> Unit
) {
    val amap = map
    amap.clear()
    amap.setOnMarkerClickListener { marker ->
        val recordId = marker.`object` as? Long
        if (recordId != null) {
            onMarkerClick(recordId)
            true
        } else {
            false
        }
    }

    val firstMarkerPosition = markers.firstOrNull()?.let { marker ->
        LatLng(marker.latitude, marker.longitude)
    }

    markers.forEach { marker ->
        amap.addMarker(
            MarkerOptions()
                .position(LatLng(marker.latitude, marker.longitude))
                .title(marker.title)
                .snippet(marker.snippet)
        )?.`object` = marker.id
    }

    if (firstMarkerPosition != null) {
        amap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstMarkerPosition, 12f))
    }
}
