package com.xiaoyin.lifeatlas.feature.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecordDetailRoute(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: RecordDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val deleted by viewModel.deleted.collectAsState()

    LaunchedEffect(deleted) {
        if (deleted) {
            onDeleted()
        }
    }

    val record = uiState.record

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text("返回")
            }
            Button(onClick = viewModel::deleteRecord, enabled = record != null) {
                Text("删除")
            }
        }

        if (record == null) {
            Text(text = "记录不存在或已删除", style = MaterialTheme.typography.bodyLarge)
        } else {
            RecordDetailContent(record = record)
        }
    }
}

@Composable
private fun RecordDetailContent(record: MemoryRecord) {
    Text(
        text = record.title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = record.recordTime.formatDateTime(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
    )
    record.locationName?.let {
        Text(text = "地点：$it", style = MaterialTheme.typography.bodyMedium)
    }
    record.mood?.let {
        Text(text = "心情：$it", style = MaterialTheme.typography.bodyMedium)
    }
    Text(text = "重要程度：${record.importance}", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = record.content.ifBlank { "暂无正文" }, style = MaterialTheme.typography.bodyLarge)
}

private fun Long.formatDateTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}

