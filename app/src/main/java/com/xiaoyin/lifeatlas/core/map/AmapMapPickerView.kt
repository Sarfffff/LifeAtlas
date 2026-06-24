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
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng

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
    amap.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
        override fun onCameraChange(position: CameraPosition) = Unit

        override fun onCameraChangeFinish(position: CameraPosition) {
            val center = position.target ?: return
            onPointSelected(
                MapPickerPoint(
                    latitude = center.latitude,
                    longitude = center.longitude
                )
            )
        }
    })
    amap.setOnMapClickListener { latLng ->
        amap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        onPointSelected(
            MapPickerPoint(
                latitude = latLng.latitude,
                longitude = latLng.longitude
            )
        )
    }

    if (selectedPoint != null) {
        val position = LatLng(selectedPoint.latitude, selectedPoint.longitude)
        amap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14f))
    }
}
