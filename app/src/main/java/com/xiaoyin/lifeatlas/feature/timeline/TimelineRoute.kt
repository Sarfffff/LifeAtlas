package com.xiaoyin.lifeatlas.feature.timeline

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.time.formatDate
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TimelineRoute(
    onRecordClick: (Long) -> Unit,
    viewModel: TimelineViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val records = uiState.records

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        PageHeader()
        TimelineSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            onClear = viewModel::clearSearchQuery
        )
        TimelineTagFilter(
            tags = uiState.tags,
            selectedTagId = uiState.selectedTagId,
            onSelectTag = viewModel::selectTag
        )
        TimelineSearchResultHint(query = uiState.searchQuery, resultCount = records.size)

        if (records.isEmpty()) {
            EmptyTrail(text = uiState.emptyStateText())
        } else {
            val groupedRecords = records.groupBy { it.recordTime.formatMonth() }
            groupedRecords.forEach { (month, monthRecords) ->
                TimelineMonthSection(
                    month = month,
                    records = monthRecords,
                    onRecordClick = onRecordClick
                )
            }
        }
    }
}

@Composable
private fun PageHeader() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "时间轴", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = WildernessTeal)
            Text(
                text = "沿着记忆的小径，回看每一段走过的风景。",
                style = MaterialTheme.typography.bodyLarge,
                color = WildernessTeal.copy(alpha = 0.68f)
            )
        }
    }
}

@Composable
private fun TimelineSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(22.dp),
            placeholder = { Text("寻找记忆") },
            singleLine = true
        )
        if (query.isNotBlank()) {
            TextButton(onClick = onClear) {
                Text("清空")
            }
        }
    }
}

@Composable
private fun TimelineTagFilter(
    tags: List<com.xiaoyin.lifeatlas.core.model.Tag>,
    selectedTagId: Long?,
    onSelectTag: (Long?) -> Unit
) {
    if (tags.isEmpty()) {
        Text(
            text = "暂无标签。新增或编辑记录时填写标签后，可在这里筛选。",
            style = MaterialTheme.typography.bodyMedium,
            color = WildernessTeal.copy(alpha = 0.62f)
        )
        return
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selectedTagId == null,
                onClick = { onSelectTag(null) },
                label = { Text("全部") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WildernessMeadow,
                    selectedLabelColor = WildernessTeal
                )
            )
        }
        items(tags) { tag ->
            FilterChip(
                selected = selectedTagId == tag.id,
                onClick = { onSelectTag(tag.id) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WildernessWildflower.copy(alpha = 0.75f),
                    selectedLabelColor = WildernessTeal
                ),
                label = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TagColorDot(color = tag.color)
                        Text(tag.name)
                    }
                }
            )
        }
    }
}

@Composable
private fun TimelineSearchResultHint(query: String, resultCount: Int) {
    if (query.isBlank()) return

    Text(
        text = "找到 $resultCount 条匹配记录",
        style = MaterialTheme.typography.bodySmall,
        color = WildernessTeal.copy(alpha = 0.62f)
    )
}

@Composable
private fun TimelineMonthSection(
    month: String,
    records: List<MemoryRecord>,
    onRecordClick: (Long) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(WildernessWildflower, RoundedCornerShape(13.dp))
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((records.size * 142).dp)
                    .background(WildernessTeal.copy(alpha = 0.18f), RoundedCornerShape(2.dp))
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = month,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = WildernessTeal
            )
            records.forEach { record ->
                TimelineRecordCard(
                    record = record,
                    onClick = { onRecordClick(record.id) }
                )
            }
        }
    }
}

@Composable
private fun TimelineRecordCard(record: MemoryRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = WildernessTeal
            )
            Text(
                text = listOfNotNull(record.locationName, record.recordTime.formatDate()).joinToString(" | "),
                style = MaterialTheme.typography.bodyMedium,
                color = WildernessTeal.copy(alpha = 0.72f)
            )
            Text(
                text = record.content.toTimelineSummary(),
                style = MaterialTheme.typography.bodyLarge,
                color = WildernessTeal.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun EmptyTrail(text: String) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "这里还很安静", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WildernessTeal)
            Text(text = text, style = MaterialTheme.typography.bodyLarge, color = WildernessTeal.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun TagColorDot(color: String?) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color.toComposeColor(), RoundedCornerShape(5.dp))
    )
}

private fun String?.toComposeColor(): Color {
    return this?.let { value ->
        runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrNull()
    } ?: Color(0xFF8A8F98)
}

private fun String.toTimelineSummary(maxLength: Int = 62): String {
    val normalized = trim().replace(Regex("\\s+"), " ")
    if (normalized.isBlank()) return "暂无正文"
    return if (normalized.length <= maxLength) normalized else "${normalized.take(maxLength)}..."
}

private fun Long.formatMonth(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy 年 M 月"))
}

private fun TimelineUiState.emptyStateText(): String {
    if (searchQuery.isNotBlank() && selectedTagId != null) return "当前标签下没有匹配的记录"
    if (searchQuery.isNotBlank()) return "没有匹配的记录"
    if (selectedTagId != null) return "当前标签下暂无记录"
    return "还没有走到这里"
}
