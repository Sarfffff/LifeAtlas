package com.xiaoyin.lifeatlas.feature.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xiaoyin.lifeatlas.core.location.CurrentLocationProvider
import com.xiaoyin.lifeatlas.core.map.AmapMapPickerView
import com.xiaoyin.lifeatlas.core.map.AmapReverseGeocoder
import com.xiaoyin.lifeatlas.core.map.MapPickerPoint
import com.xiaoyin.lifeatlas.core.map.MapSdkConfig
import com.xiaoyin.lifeatlas.core.map.ReverseGeocodeResult
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MapPickerRoute(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onBack: () -> Unit,
    onPointConfirmed: (MapPickerPoint) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationProvider = remember(context) { CurrentLocationProvider(context) }
    val reverseGeocoder = remember { AmapReverseGeocoder() }
    var selectedPoint by remember(initialLatitude, initialLongitude) {
        mutableStateOf(
            if (initialLatitude != null && initialLongitude != null) {
                MapPickerPoint(
                    latitude = initialLatitude,
                    longitude = initialLongitude
                )
            } else {
                null
            }
        )
    }
    var isResolvingAddress by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isLocating = true
            locationProvider.requestCurrentLocation { location ->
                isLocating = false
                if (location == null) {
                    message = "无法获取当前位置，请确认定位服务已开启。"
                } else {
                    val point = MapPickerPoint(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    selectedPoint = point
                    message = "已定位到当前位置，正在识别地点名称。"
                    resolveAddress(
                        point = point,
                        reverseGeocoder = reverseGeocoder,
                        coroutineScope = coroutineScope,
                        onResolvingChange = { isResolvingAddress = it },
                        onMessage = { message = it },
                        onResolved = { resolvedPoint -> selectedPoint = resolvedPoint }
                    )
                }
            }
        } else {
            message = "未授予定位权限。你仍然可以手动点击地图选点。"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "地图选点", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = WildernessTeal)
        Text(
            text = if (MapSdkConfig.isAmapConfigured) {
                "拖动地图，或者使用当前位置，把这一段记忆钉在世界上。"
            } else {
                "当前没有配置高德 Key，真实地图不可用；定位权限仍可获取当前位置，但不能替代地图服务 Key。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WildernessPaper)
        ) {
            if (MapSdkConfig.isAmapConfigured) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                ) {
                    AmapMapPickerView(
                        selectedPoint = selectedPoint,
                        onPointSelected = { point -> selectedPoint = point },
                        modifier = Modifier.fillMaxSize()
                    )
                    CenterMapPin(
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Text(
                        text = "拖动地图，定位针所在处即为记录位置",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp)
                            .background(WildernessPaper.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = WildernessTeal
                    )
                }
            } else {
                MapPickerUnavailableContent()
            }
        }
        Text(
            text = selectedPoint?.let { point ->
                point.address?.let { address ->
                    "已选择：$address\n${point.latitude}, ${point.longitude}"
                } ?: "已选择：${point.latitude}, ${point.longitude}"
            } ?: if (MapSdkConfig.isAmapConfigured) "拖动地图，让中心定位针停在记录位置。" else "可点击“使用当前位置”申请定位权限。也可以返回表单手动填写经纬度。",
            style = MaterialTheme.typography.bodyMedium
        )
        message?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }
        OutlinedButton(
            onClick = {
                if (locationProvider.hasLocationPermission()) {
                    isLocating = true
                    locationProvider.requestCurrentLocation { location ->
                        isLocating = false
                        if (location == null) {
                            message = "无法获取当前位置，请确认定位服务已开启。"
                        } else {
                            val point = MapPickerPoint(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                            selectedPoint = point
                            message = "已定位到当前位置，正在识别地点名称。"
                            resolveAddress(
                                point = point,
                                reverseGeocoder = reverseGeocoder,
                                coroutineScope = coroutineScope,
                                onResolvingChange = { isResolvingAddress = it },
                                onMessage = { message = it },
                                onResolved = { resolvedPoint -> selectedPoint = resolvedPoint }
                            )
                        }
                    }
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            enabled = !isLocating && !isResolvingAddress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLocating) "正在定位" else "使用当前位置")
        }
        Button(
            onClick = {
                val point = selectedPoint ?: return@Button
                resolveAddress(
                    point = point,
                    reverseGeocoder = reverseGeocoder,
                    coroutineScope = coroutineScope,
                    onResolvingChange = { isResolvingAddress = it },
                    onMessage = { message = it },
                    onResolved = onPointConfirmed
                )
            },
            enabled = selectedPoint != null && !isResolvingAddress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isResolvingAddress) "正在识别地点" else "使用此位置")
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("取消")
        }
    }
}

@Composable
private fun CenterMapPin(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = "中心定位针",
            tint = WildernessTeal,
            modifier = Modifier
                .size(54.dp)
                .shadow(8.dp, RoundedCornerShape(28.dp))
        )
        Box(
            modifier = Modifier
                .padding(top = 42.dp)
                .size(10.dp)
                .background(WildernessMeadow, RoundedCornerShape(5.dp))
        )
    }
}

private fun resolveAddress(
    point: MapPickerPoint,
    reverseGeocoder: AmapReverseGeocoder,
    coroutineScope: CoroutineScope,
    onResolvingChange: (Boolean) -> Unit,
    onMessage: (String) -> Unit,
    onResolved: (MapPickerPoint) -> Unit
) {
    onResolvingChange(true)
    coroutineScope.launch {
        val result = withContext(Dispatchers.IO) {
            reverseGeocoder.reverseGeocode(point)
        }
        onResolvingChange(false)
        when (result) {
            is ReverseGeocodeResult.Success -> {
                onMessage("已识别地点名称。")
                onResolved(point.copy(address = result.address))
            }
            ReverseGeocodeResult.AddressNotFound -> {
                onMessage("已保留经纬度，但没有识别到明确地点名称。")
                onResolved(point)
            }
            ReverseGeocodeResult.MissingKey -> {
                onMessage("地图 Key 未配置，已保留经纬度；地点名称可之后手动补充。")
                onResolved(point)
            }
            ReverseGeocodeResult.NetworkUnavailable -> {
                onMessage("网络不可用，已保留经纬度，稍后可手动补充地点名称。")
                onResolved(point)
            }
            ReverseGeocodeResult.ServiceUnavailable -> {
                onMessage("地点识别服务暂时不可用，已保留经纬度。")
                onResolved(point)
            }
        }
    }
}

@Composable
private fun MapPickerUnavailableContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "真实地图需要高德 Key",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WildernessTeal
            )
            Text(
                text = "定位权限只能获取当前位置，不能替代地图服务 Key。你可以先用当前位置或手动经纬度保存记录，之后配置 Key 再使用拖拽地图选点。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
            MapSdkConfig.setupSteps.forEachIndexed { index, step ->
                Text(
                    text = "${index + 1}. $step",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                )
            }
        }
    }
}
