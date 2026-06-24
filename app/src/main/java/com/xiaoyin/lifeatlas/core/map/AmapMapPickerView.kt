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
fun AmapMapPickerView(
    selectedPoint: MapPickerPoint?,
    onPointSelected: (MapPickerPoint) -> Unit,
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
            view.configurePicker(
                selectedPoint = selectedPoint,
                onPointSelected = onPointSelected
            )
        }
    )
}

private fun MapView.configurePicker(
    selectedPoint: MapPickerPoint?,
    onPointSelected: (MapPickerPoint) -> Unit
) {
    val amap = map
    amap.setOnMapClickListener { latLng ->
        onPointSelected(
            MapPickerPoint(
                latitude = latLng.latitude,
                longitude = latLng.longitude
            )
        )
    }

    amap.clear()
    if (selectedPoint != null) {
        val position = LatLng(selectedPoint.latitude, selectedPoint.longitude)
        amap.addMarker(
            MarkerOptions()
                .position(position)
                .title("已选择的位置")
                .snippet("${selectedPoint.latitude}, ${selectedPoint.longitude}")
        )
        amap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14f))
    }
}
