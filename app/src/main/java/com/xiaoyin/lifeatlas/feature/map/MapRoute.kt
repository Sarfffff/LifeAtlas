package com.xiaoyin.lifeatlas.feature.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyin.lifeatlas.R
import com.xiaoyin.lifeatlas.core.map.MapSdkConfig
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessCoral
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower

@Composable
fun MapRoute(
    onRecordClick: (Long) -> Unit,
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "人生地图",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = WildernessTeal
        )
        Text(
            text = "每一枚坐标，都是你在旷野里停留过的证据。",
            style = MaterialTheme.typography.bodyLarge,
            color = WildernessTeal.copy(alpha = 0.68f)
        )
        IllustratedMapPanel(pinCount = uiState.locatedRecords.size)
        MapSummary(count = uiState.locatedRecords.size)

        if (uiState.locatedRecords.isEmpty()) {
            MapEmptyCard()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                uiState.locatedRecords.forEach { record ->
                    LocatedRecordCard(record = record, onClick = { onRecordClick(record.id) })
                }
            }
        }
    }
}

@Composable
private fun IllustratedMapPanel(pinCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .clip(RoundedCornerShape(30.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.wilderness_map_panel),
                contentDescription = "人生地图插画",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            val visiblePins = pinCount.coerceIn(1, 4)
            if (visiblePins >= 1) MapPin(WildernessTeal, Modifier.align(Alignment.TopCenter).offset(y = 30.dp))
            if (visiblePins >= 2) MapPin(WildernessCoral, Modifier.align(Alignment.CenterEnd).offset(x = (-52).dp, y = (-16).dp))
            if (visiblePins >= 3) MapPin(Color(0xFF8D69B8), Modifier.align(Alignment.Center).offset(x = (-54).dp, y = 34.dp))
            if (visiblePins >= 4) MapPin(WildernessWildflower, Modifier.align(Alignment.BottomCenter).offset(y = (-42).dp))
        }
    }
}

@Composable
private fun MapPin(color: Color, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.Place,
        contentDescription = null,
        tint = color,
        modifier = modifier.size(48.dp)
    )
}

@Composable
private fun MapSummary(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MapStatChip(label = "坐标", value = "$count", modifier = Modifier.weight(0.8f))
        MapStatChip(label = "地图", value = MapSdkConfig.provider.displayName, modifier = Modifier.weight(1.3f))
        MapStatChip(label = "Key", value = MapSdkConfig.statusText, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MapStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessMeadow.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "$label  ", style = MaterialTheme.typography.bodyLarge, color = WildernessTeal.copy(alpha = 0.62f))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = WildernessTeal)
        }
    }
}

@Composable
private fun MapEmptyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "还没有点亮坐标", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WildernessTeal)
            Text(
                text = "新增或编辑记录时填写经纬度，地图就会出现属于你的第一枚 marker。",
                style = MaterialTheme.typography.bodyLarge,
                color = WildernessTeal.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun LocatedRecordCard(record: MemoryRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = WildernessTeal
            )
            record.locationName?.let { locationName ->
                Text(text = locationName, style = MaterialTheme.typography.bodyLarge, color = WildernessTeal.copy(alpha = 0.78f))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(WildernessWildflower.copy(alpha = 0.18f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(WildernessWildflower, RoundedCornerShape(5.dp))
                    )
                    Text(
                        text = "${record.latitude}, ${record.longitude}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = WildernessTeal
                    )
                }
            }
        }
    }
}
