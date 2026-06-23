package com.xiaoyin.lifeatlas.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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

        if (records.isEmpty()) {
            Text(
                text = "暂无记录",
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
            Text(text = record.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun Long.formatMonth(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy 年 M 月"))
}
