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
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions

@Composable
fun AmapMapView(
    markers: List<MapMarkerItem>,
    selectedMarkerId: Long? = null,
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
                selectedMarkerId = selectedMarkerId,
                onMarkerClick = onMarkerClick
            )
        }
    )
}

private fun MapView.renderMarkers(
    markers: List<MapMarkerItem>,
    selectedMarkerId: Long?,
    onMarkerClick: (Long) -> Unit
) {
    val amap = map
    amap.clear()
    amap.mapType = AMap.MAP_TYPE_NORMAL
    amap.uiSettings.isZoomControlsEnabled = false
    amap.uiSettings.isCompassEnabled = false
    amap.setOnMarkerClickListener { marker ->
        val recordId = marker.`object` as? Long
        if (recordId != null) {
            onMarkerClick(recordId)
            true
        } else {
            false
        }
    }

    val positions = markers.map { marker -> LatLng(marker.latitude, marker.longitude) }

    if (positions.size >= 2) {
        amap.addPolyline(
            PolylineOptions()
                .addAll(positions)
                .width(8f)
                .color(0xAA2F8F83.toInt())
        )
    }

    markers.forEach { marker ->
        val selected = marker.id == selectedMarkerId
        amap.addMarker(
            MarkerOptions()
                .position(LatLng(marker.latitude, marker.longitude))
                .title(marker.title)
                .snippet(marker.snippet)
                .icon(
                    BitmapDescriptorFactory.defaultMarker(
                        if (selected) BitmapDescriptorFactory.HUE_ORANGE else BitmapDescriptorFactory.HUE_AZURE
                    )
                )
        )?.`object` = marker.id
    }

    when {
        positions.size >= 2 -> {
            val boundsBuilder = LatLngBounds.builder()
            positions.forEach(boundsBuilder::include)
            amap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
        }
        positions.size == 1 -> {
            amap.moveCamera(CameraUpdateFactory.newLatLngZoom(positions.first(), 10.5f))
        }
    }
}
