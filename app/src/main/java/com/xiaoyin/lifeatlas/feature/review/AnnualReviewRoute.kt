package com.xiaoyin.lifeatlas.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Place
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaoyin.lifeatlas.core.time.formatDate
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessCream
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMuted
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessSky
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower

@Composable
fun AnnualReviewRoute(
    onBack: () -> Unit,
    onRecordClick: (Long) -> Unit,
    viewModel: AnnualReviewViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WildernessCream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(WildernessPaper)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = WildernessTeal)
            }
            Column {
                Text("年度回顾", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = WildernessTeal)
                Text("把这一年的脚印，收进一页风景。", color = WildernessMuted)
            }
        }

        if (state.availableYears.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.availableYears.forEach { year ->
                    val selected = year == state.selectedYear
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selected) WildernessTeal else WildernessPaper)
                            .clickable { viewModel.selectYear(year) }
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    ) {
                        Text("${year}年", color = if (selected) WildernessPaper else WildernessTeal, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (state.records.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = WildernessPaper), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("这一年还没有记忆", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WildernessTeal)
                    Text("下一段出发，会成为年度回顾里的第一束光。", color = WildernessMuted)
                }
            }
            return@Column
        }

        Card(colors = CardDefaults.cardColors(containerColor = WildernessPaper), shape = RoundedCornerShape(26.dp)) {
            Column {
                val coverPhoto = state.coverPhoto
                if (coverPhoto != null) {
                    AsyncImage(
                        model = coverPhoto.displayUri,
                        contentDescription = "年度照片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(210.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp).background(WildernessSky.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${state.selectedYear} · 人生是旷野", color = WildernessTeal, fontWeight = FontWeight.Black)
                    }
                }
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("${state.selectedYear}，你留下了 ${state.records.size} 段记忆", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WildernessTeal)
                    Text(
                        state.firstMemoryDate?.let { "这一年的故事，从 ${it.formatDate()} 开始。" }.orEmpty(),
                        color = WildernessMuted
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReviewStat(Icons.Outlined.CalendarMonth, "记忆", state.records.size.toString(), WildernessMeadow, Modifier.weight(1f))
            ReviewStat(Icons.Outlined.Image, "照片", state.photoCount.toString(), WildernessSky, Modifier.weight(1f))
            ReviewStat(Icons.Outlined.Place, "地点", state.placeCount.toString(), WildernessWildflower, Modifier.weight(1f))
        }

        Card(colors = CardDefaults.cardColors(containerColor = WildernessMeadow.copy(alpha = 0.58f)), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("这一年的小结", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = WildernessTeal)
                Text("记录跨度 ${state.journeyDays} 天", color = WildernessTeal)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Mood, contentDescription = null, tint = WildernessTeal)
                    Text("最常出现的心情：${state.favoriteMood ?: "未填写"}", color = WildernessTeal)
                }
            }
        }

        state.mostImportantRecord?.let { record ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onRecordClick(record.id) },
                colors = CardDefaults.cardColors(containerColor = WildernessPaper),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("年度重要记忆", color = WildernessMuted, fontWeight = FontWeight.Bold)
                    Text(record.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WildernessTeal, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${record.locationName ?: "未填写地点"} · ${record.recordTime.formatDate()}", color = WildernessMuted)
                    Text(record.content.ifBlank { "这段记忆还没有正文。" }, maxLines = 3, overflow = TextOverflow.Ellipsis, color = WildernessTeal.copy(alpha = 0.78f))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ReviewStat(icon: ImageVector, label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.7f)), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = WildernessTeal, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = WildernessTeal)
            Text(label, style = MaterialTheme.typography.labelMedium, color = WildernessTeal.copy(alpha = 0.72f))
        }
    }
}
