package com.xiaoyin.lifeatlas.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.core.time.formatDate
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessSky
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower
import coil.compose.SubcomposeAsyncImage
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageHeader(title = "时间轴", subtitle = "沿着记忆的小径，回看每一段走过的风景。")
        TimelineSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            onClear = viewModel::clearSearchQuery
        )
        TimelineSearchResultHint(
            query = uiState.searchQuery,
            resultCount = records.size
        )
        TimelineTagFilter(
            tags = uiState.tags,
            selectedTagId = uiState.selectedTagId,
            onSelectTag = viewModel::selectTag
        )

        if (records.isEmpty()) {
            EmptyTrail(text = uiState.emptyStateText())
        } else {
            val groupedRecords = records.groupBy { it.recordTime.formatMonth() }
            groupedRecords.forEach { (month, monthRecords) ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TrailMarker()
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = month,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = WildernessTeal
                        )
                        monthRecords.forEach { record ->
                            TimelinePreviewCard(
                                record = record,
                                firstPhoto = uiState.firstPhotosByRecordId[record.id],
                                onClick = { onRecordClick(record.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = WildernessTeal)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        }
    }
}

@Composable
private fun TimelineSearchResultHint(query: String, resultCount: Int) {
    if (query.isBlank()) return

    Text(
        text = "找到 $resultCount 条匹配记录",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
    )
}

@Composable
private fun TimelineSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            label = { Text("寻找记忆") },
            placeholder = { Text("标题、地点、心情") },
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TagColorDot(color = tag.color)
                        Text(tag.name)
                    }
                }
            )
        }
    }
}

@Composable
private fun TrailMarker() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(WildernessSunColor(), RoundedCornerShape(9.dp))
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(120.dp)
                .background(WildernessTeal.copy(alpha = 0.18f), RoundedCornerShape(1.dp))
        )
    }
}

@Composable
private fun WildernessSunColor(): Color = WildernessWildflower

@Composable
private fun EmptyTrail(text: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "这里还很安静", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WildernessTeal)
            Text(text = text.replace("暂无记录", "还没有走到这里"), style = MaterialTheme.typography.bodyMedium)
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

@Composable
private fun TimelinePreviewCard(record: MemoryRecord, firstPhoto: Photo?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            firstPhoto?.let { photo ->
                TimelinePhotoImage(photo = photo)
            }
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WildernessTeal
            )
            Text(
                text = listOfNotNull(record.locationName, record.recordTime.formatDate()).joinToString(" | "),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = record.content.toTimelineSummary(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun TimelinePhotoImage(photo: Photo) {
    SubcomposeAsyncImage(
        model = photo.displayUri,
        contentDescription = "记录首图",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(WildernessSky.copy(alpha = 0.34f), RoundedCornerShape(16.dp)),
        loading = {
            TimelinePhotoPlaceholder(text = "正在加载照片")
        },
        error = {
            TimelinePhotoPlaceholder(text = "照片无法显示")
        }
    )
}

@Composable
private fun TimelinePhotoPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun String.toTimelineSummary(maxLength: Int = 80): String {
    val normalized = trim().replace(Regex("\\s+"), " ")
    if (normalized.isBlank()) return "暂无正文"
    return if (normalized.length <= maxLength) {
        normalized
    } else {
        "${normalized.take(maxLength)}..."
    }
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
