package com.xiaoyin.lifeatlas.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.core.time.formatDate
import com.xiaoyin.lifeatlas.core.ui.theme.AtlasMist
import coil.compose.AsyncImage
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "时间轴", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            text = "按时间倒序回看人生节点。当前内容来自本地 Room 数据库。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
        )
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
            Text(
                text = uiState.emptyStateText(),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            val groupedRecords = records.groupBy { it.recordTime.formatMonth() }
            groupedRecords.forEach { (month, monthRecords) ->
                Text(
                    text = month,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
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
            label = { Text("搜索记录") },
            placeholder = { Text("标题、正文、地点、心情") },
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
    if (tags.isEmpty()) return

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selectedTagId == null,
                onClick = { onSelectTag(null) },
                label = { Text("全部") }
            )
        }
        items(tags) { tag ->
            FilterChip(
                selected = selectedTagId == tag.id,
                onClick = { onSelectTag(tag.id) },
                label = { Text(tag.name) }
            )
        }
    }
}

@Composable
private fun TimelinePreviewCard(record: MemoryRecord, firstPhoto: Photo?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AtlasMist)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            firstPhoto?.let { photo ->
                AsyncImage(
                    model = photo.originalUri,
                    contentDescription = "记录首图",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }
            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
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
    return "暂无记录"
}
