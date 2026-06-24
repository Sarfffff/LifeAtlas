package com.xiaoyin.lifeatlas.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import com.xiaoyin.lifeatlas.R
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.time.formatDate
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessSky
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessSunset
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        WildernessHero()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(title = "记录", value = uiState.recordCount.toString(), color = WildernessMeadow, modifier = Modifier.weight(1f))
            StatCard(title = "照片", value = uiState.photoCount.toString(), color = WildernessSky, modifier = Modifier.weight(1f))
            StatCard(title = "标签", value = uiState.tagCount.toString(), color = WildernessWildflower, modifier = Modifier.weight(1f))
        }

        SectionCard(
            title = "旷野坐标",
            body = "已有 ${uiState.locatedRecordCount} 条记录写下坐标，地图会把它们连成你的来路。"
        )

        RecentRecordsCard(
            records = uiState.recentRecords,
            onRecordClick = onRecordClick
        )
    }
}

@Composable
private fun WildernessHero() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            WildernessSky.copy(alpha = 0.72f),
                            WildernessMeadow.copy(alpha = 0.74f),
                            WildernessWildflower.copy(alpha = 0.42f)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "岁迹",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = WildernessTeal
                )
                Text(
                    text = "人生是旷野，今天从哪里出发？",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = WildernessTeal
                )
                Text(
                    text = "把走过的地方、拍过的照片、写下的文字，变成一条可回看的生命小径。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WildernessTeal.copy(alpha = 0.78f)
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 168.dp)
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(WildernessTeal.copy(alpha = 0.14f))
            )
            Box(
                modifier = Modifier
                    .padding(start = 210.dp, top = 124.dp)
                    .height(46.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 60.dp, topEnd = 60.dp))
                    .background(WildernessTeal.copy(alpha = 0.24f))
            )
            Image(
                painter = painterResource(id = R.drawable.mascot_companion),
                contentDescription = "岁迹小旅人",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .height(130.dp)
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WildernessTeal)
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = WildernessTeal.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun SectionCard(title: String, body: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WildernessTeal)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f))
        }
    }
}

@Composable
private fun RecentRecordsCard(records: List<MemoryRecord>, onRecordClick: (Long) -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "最近的风景", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WildernessTeal)
            if (records.isEmpty()) {
                Text(
                    text = "还没有留下第一段记忆。写下它，旷野就有了第一枚坐标。",
                    style = MaterialTheme.typography.bodyMedium
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
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = record.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = listOfNotNull(record.locationName, record.recordTime.formatDate()).joinToString(" | "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
        )
    }
}
