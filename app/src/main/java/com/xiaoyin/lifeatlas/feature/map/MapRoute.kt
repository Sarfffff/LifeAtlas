package com.xiaoyin.lifeatlas.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyin.lifeatlas.core.map.AmapMapView
import com.xiaoyin.lifeatlas.core.map.MapSdkConfig
import com.xiaoyin.lifeatlas.core.map.MapMarkerItem
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.ui.theme.AtlasMist

@Composable
fun MapRoute(viewModel: MapViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val markerItems = uiState.locatedRecords.toMapMarkerItems()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "地图", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = AtlasMist)
        ) {
            if (MapSdkConfig.isAmapConfigured) {
                AmapMapView(
                    markers = markerItems,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
            } else {
                MapUnavailableContent()
            }
        }
        Text(
            text = "当前地图页会把带坐标记录渲染为 marker，下方列表保留为辅助信息。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
        )
        if (uiState.locatedRecords.isEmpty()) {
            Text(
                text = "暂无带坐标的记录。新增或编辑记录时填写经纬度后，会出现在这里。",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.locatedRecords) { record ->
                    LocatedRecordCard(record = record)
                }
            }
        }
    }
}

private fun List<MemoryRecord>.toMapMarkerItems(): List<MapMarkerItem> {
    return mapNotNull { record ->
        val latitude = record.latitude
        val longitude = record.longitude
        if (latitude == null || longitude == null) {
            null
        } else {
            MapMarkerItem(
                id = record.id,
                latitude = latitude,
                longitude = longitude,
                title = record.title,
                snippet = record.locationName ?: "$latitude, $longitude"
            )
        }
    }
}

@Composable
private fun MapUnavailableContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
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

@Composable
private fun LocatedRecordCard(record: MemoryRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            record.locationName?.let { locationName ->
                Text(text = locationName, style = MaterialTheme.typography.bodyMedium)
            }
            AssistChip(
                onClick = { },
                label = {
                    Text("${record.latitude}, ${record.longitude}")
                }
            )
        }
    }
}
