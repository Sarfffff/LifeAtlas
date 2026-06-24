package com.xiaoyin.lifeatlas.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xiaoyin.lifeatlas.core.map.AmapMapPickerView
import com.xiaoyin.lifeatlas.core.map.AmapReverseGeocoder
import com.xiaoyin.lifeatlas.core.map.MapPickerPoint
import com.xiaoyin.lifeatlas.core.map.MapSdkConfig
import com.xiaoyin.lifeatlas.core.ui.theme.AtlasMist
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
    val coroutineScope = rememberCoroutineScope()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "地图选点", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = AtlasMist)
        ) {
            if (MapSdkConfig.isAmapConfigured) {
                AmapMapPickerView(
                    selectedPoint = selectedPoint,
                    onPointSelected = { point -> selectedPoint = point },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                )
            } else {
                MapPickerUnavailableContent()
            }
        }
        Text(
            text = selectedPoint?.let { point ->
                point.address?.let { address ->
                    "已选择：$address\n${point.latitude}, ${point.longitude}"
                } ?: "已选择：${point.latitude}, ${point.longitude}"
            } ?: "点击地图选择记录位置。",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = {
                val point = selectedPoint ?: return@Button
                isResolvingAddress = true
                coroutineScope.launch {
                    val address = withContext(Dispatchers.IO) {
                        reverseGeocoder.reverseGeocode(point)
                    }
                    isResolvingAddress = false
                    onPointConfirmed(point.copy(address = address))
                }
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
                text = "地图 Key 未配置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "请在 local.properties 中配置 lifeatlas.amap.apiKey 后重新构建。",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
