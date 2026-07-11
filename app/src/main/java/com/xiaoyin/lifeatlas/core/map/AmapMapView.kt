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
import kotlin.math.floor

@Composable
fun AmapMapView(
    markers: List<MapMarkerItem>,
    selectedMarkerId: Long? = null,
    onMarkerClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val renderState = remember { AmapRenderState() }
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
            view.updateMapContent(
                state = renderState,
                markers = markers,
                selectedMarkerId = selectedMarkerId,
                onMarkerClick = onMarkerClick
            )
        }
    )
}

private class AmapRenderState {
    var markers: List<MapMarkerItem> = emptyList()
    var selectedMarkerId: Long? = null
    var onMarkerClick: (Long) -> Unit = {}
    var markerSignature: Int = 0
    var listenerInstalled: Boolean = false
}

private data class ClusterPayload(
    val recordIds: List<Long>,
    val latitude: Double,
    val longitude: Double
)

private fun MapView.updateMapContent(
    state: AmapRenderState,
    markers: List<MapMarkerItem>,
    selectedMarkerId: Long?,
    onMarkerClick: (Long) -> Unit
) {
    val amap = map
    amap.mapType = AMap.MAP_TYPE_NORMAL
    amap.uiSettings.isZoomControlsEnabled = false
    amap.uiSettings.isCompassEnabled = false

    state.onMarkerClick = onMarkerClick
    val newSignature = markers.hashCode()
    val markersChanged = newSignature != state.markerSignature
    val selectionChanged = selectedMarkerId != state.selectedMarkerId
    state.markers = markers
    state.selectedMarkerId = selectedMarkerId
    state.markerSignature = newSignature

    if (!state.listenerInstalled) {
        amap.setOnMarkerClickListener { marker ->
            val payload = marker.`object` as? ClusterPayload ?: return@setOnMarkerClickListener false
            if (payload.recordIds.size == 1) {
                state.onMarkerClick(payload.recordIds.first())
            } else {
                amap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(payload.latitude, payload.longitude),
                        (amap.cameraPosition.zoom + 2.2f).coerceAtMost(18f)
                    )
                )
            }
            true
        }
        amap.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChange(position: com.amap.api.maps.model.CameraPosition?) = Unit

            override fun onCameraChangeFinish(position: com.amap.api.maps.model.CameraPosition?) {
                drawClusteredMarkers(amap, state)
            }
        })
        state.listenerInstalled = true
    }

    if (markersChanged) {
        fitMarkers(amap, markers)
    }
    if (markersChanged || selectionChanged) {
        drawClusteredMarkers(amap, state)
    }
}

private fun drawClusteredMarkers(amap: AMap, state: AmapRenderState) {
    amap.clear()
    val markers = state.markers
    val positions = markers.map { marker -> LatLng(marker.latitude, marker.longitude) }

    if (positions.size >= 2) {
        amap.addPolyline(
            PolylineOptions()
                .addAll(positions)
                .width(8f)
                .color(0xAA2F8F83.toInt())
        )
    }

    clusterMarkers(markers, amap.cameraPosition.zoom).forEach { cluster ->
        val selected = cluster.recordIds.size == 1 && cluster.recordIds.first() == state.selectedMarkerId
        val isCluster = cluster.recordIds.size > 1
        amap.addMarker(
            MarkerOptions()
                .position(LatLng(cluster.latitude, cluster.longitude))
                .title(if (isCluster) "${cluster.recordIds.size} 段记忆" else cluster.title)
                .snippet(if (isCluster) "点击放大查看" else cluster.snippet)
                .icon(
                    BitmapDescriptorFactory.defaultMarker(
                        when {
                            selected -> BitmapDescriptorFactory.HUE_ORANGE
                            isCluster -> BitmapDescriptorFactory.HUE_VIOLET
                            else -> BitmapDescriptorFactory.HUE_AZURE
                        }
                    )
                )
        )?.`object` = ClusterPayload(cluster.recordIds, cluster.latitude, cluster.longitude)
    }
}

private fun fitMarkers(amap: AMap, markers: List<MapMarkerItem>) {
    val positions = markers.map { LatLng(it.latitude, it.longitude) }
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

private data class MarkerCluster(
    val recordIds: List<Long>,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val snippet: String
)

private fun clusterMarkers(markers: List<MapMarkerItem>, zoom: Float): List<MarkerCluster> {
    if (markers.isEmpty()) return emptyList()
    val gridSize = when {
        zoom < 5f -> 8.0
        zoom < 7f -> 3.0
        zoom < 9f -> 1.2
        zoom < 11f -> 0.45
        zoom < 13f -> 0.16
        else -> 0.0
    }
    if (gridSize == 0.0) {
        return markers.map { marker ->
            MarkerCluster(listOf(marker.id), marker.latitude, marker.longitude, marker.title, marker.snippet)
        }
    }

    return markers.groupBy { marker ->
        floor(marker.latitude / gridSize).toLong() to floor(marker.longitude / gridSize).toLong()
    }.values.map { group ->
        MarkerCluster(
            recordIds = group.map { it.id },
            latitude = group.map { it.latitude }.average(),
            longitude = group.map { it.longitude }.average(),
            title = group.first().title,
            snippet = group.first().snippet
        )
    }
}
