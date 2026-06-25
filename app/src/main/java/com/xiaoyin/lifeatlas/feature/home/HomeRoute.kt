package com.xiaoyin.lifeatlas.feature.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyin.lifeatlas.R
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.time.formatDate
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessSky
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower

@Composable
fun HomeRoute(
    onRecordClick: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
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
        HomeHero()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatTile("记录", uiState.recordCount.toString(), WildernessMeadow, Modifier.weight(1f))
            StatTile("照片", uiState.photoCount.toString(), WildernessSky, Modifier.weight(1f))
            StatTile("标签", uiState.tagCount.toString(), WildernessWildflower, Modifier.weight(1f))
        }

        InfoPanel(
            title = "旷野坐标",
            body = "已有 ${uiState.locatedRecordCount} 条记录写下坐标，地图会把它们连成你的来路。"
        )

        RecentRecordsPanel(
            records = uiState.recentRecords,
            onRecordClick = onRecordClick
        )
    }
}

@Composable
private fun HomeHero() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .clip(RoundedCornerShape(34.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.wilderness_home_hero),
                contentDescription = "旷野风景",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                WildernessPaper.copy(alpha = 0.92f),
                                WildernessPaper.copy(alpha = 0.58f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 26.dp, top = 24.dp, end = 130.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "岁迹",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = WildernessTeal
                )
                Text(
                    text = "人生是旷野，今天从哪里出发？",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = WildernessTeal
                )
                Text(
                    text = "把走过的地方、拍过的照片和写下的文字，连成一条可回看的生命小径。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = WildernessTeal.copy(alpha = 0.78f)
                )
            }
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_mascot_text),
                contentDescription = "岁迹吉祥物",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 22.dp, top = 44.dp)
                    .size(138.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 26.dp, bottom = 26.dp)
                    .fillMaxWidth(0.48f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(WildernessTeal.copy(alpha = 0.18f))
            )
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(92.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.82f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = WildernessTeal
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = WildernessTeal.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun InfoPanel(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WildernessTeal)
            Text(text = body, style = MaterialTheme.typography.bodyLarge, color = WildernessTeal.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun RecentRecordsPanel(records: List<MemoryRecord>, onRecordClick: (Long) -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "最近的风景", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WildernessTeal)
            if (records.isEmpty()) {
                Text(
                    text = "还没有留下第一段记忆。写下它，旷野就有了第一枚坐标。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = WildernessTeal.copy(alpha = 0.72f)
                )
            } else {
                records.forEach { record ->
                    RecentRecordRow(record = record, onClick = { onRecordClick(record.id) })
                }
            }
        }
    }
}

@Composable
private fun RecentRecordRow(record: MemoryRecord, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WildernessWildflower.copy(alpha = 0.22f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = record.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = WildernessTeal
        )
        Text(
            text = listOfNotNull(record.locationName, record.recordTime.formatDate()).joinToString(" | "),
            style = MaterialTheme.typography.bodyMedium,
            color = WildernessTeal.copy(alpha = 0.68f)
        )
    }
}
