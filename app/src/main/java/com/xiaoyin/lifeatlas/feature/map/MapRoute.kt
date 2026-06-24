package com.xiaoyin.lifeatlas.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessSky
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower

@Composable
fun MapRoute(
    onRecordClick: (Long) -> Unit,
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val markerItems = uiState.locatedRecords.toMapMarkerItems()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "人生地图", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = WildernessTeal)
        Text(
            text = "每一枚坐标，都是你在旷野里停留过的证据。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = WildernessPaper)
        ) {
            if (MapSdkConfig.isAmapConfigured) {
                AmapMapView(
                    markers = markerItems,
                    onMarkerClick = onRecordClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
            } else {
                MapUnavailableContent()
            }
        }
        MapSummary(count = uiState.locatedRecords.size)
        if (uiState.locatedRecords.isEmpty()) {
            MapEmptyCard()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.locatedRecords) { record ->
                    LocatedRecordCard(record = record)
                }
            }
        }
    }
}

@Composable
private fun MapSummary(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MapStatChip(label = "坐标", value = "$count")
        MapStatChip(label = "地图", value = MapSdkConfig.provider.displayName)
    }
}

@Composable
private fun MapStatChip(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessMeadow.copy(alpha = 0.58f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = WildernessTeal.copy(alpha = 0.68f))
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = WildernessTeal)
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
                text = "地图还在等风起",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WildernessTeal
            )
            Text(
                text = "配置高德 Key 后，你的坐标会在这里展开成一片旷野。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun MapEmptyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessSky.copy(alpha = 0.34f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "还没有点亮坐标", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WildernessTeal)
            Text(
                text = "新增或编辑记录时填写经纬度，地图就会出现属于你的第一枚 marker。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
            )
        }
    }
}

@Composable
private fun LocatedRecordCard(record: MemoryRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WildernessTeal
            )
            record.locationName?.let { locationName ->
                Text(text = locationName, style = MaterialTheme.typography.bodyMedium)
            }
            AssistChip(
                onClick = { },
                label = {
                    Text("${record.latitude}, ${record.longitude}")
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(WildernessWildflower, RoundedCornerShape(4.dp))
                    )
                }
            )
        }
    }
}
