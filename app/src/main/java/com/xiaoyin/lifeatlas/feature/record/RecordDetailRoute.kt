package com.xiaoyin.lifeatlas.feature.record

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.core.model.Tag
import coil.compose.SubcomposeAsyncImage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecordDetailRoute(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    viewModel: RecordDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val deleted by viewModel.deleted.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { record?.let { onEdit(it.id) } }, enabled = record != null) {
                    Text("编辑")
                }
                Button(onClick = { showDeleteDialog = true }, enabled = record != null) {
                    Text("删除")
                }
            }
        }

        if (record == null) {
            Text(text = "记录不存在或已删除", style = MaterialTheme.typography.bodyLarge)
        } else {
            RecordDetailContent(record = record, photos = uiState.photos, tags = uiState.tags)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除记录") },
            text = { Text("这条记录会从本地数据库中删除，关联照片记录也会一起删除。此操作暂时无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteRecord()
                    }
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun RecordDetailContent(record: MemoryRecord, photos: List<Photo>, tags: List<Tag>) {
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
    if (record.latitude != null && record.longitude != null) {
        Text(
            text = "坐标：${record.latitude}, ${record.longitude}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
    record.mood?.let {
        Text(text = "心情：$it", style = MaterialTheme.typography.bodyMedium)
    }
    Text(text = "重要程度：${record.importance}", style = MaterialTheme.typography.bodyMedium)
    if (tags.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tags) { tag ->
                AssistChip(
                    onClick = { },
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
    if (photos.isNotEmpty()) {
        Text(text = "照片", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(photos) { photo ->
                Card(
                    modifier = Modifier.width(220.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    DetailPhotoImage(photo = photo)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = record.content.ifBlank { "暂无正文" }, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun DetailPhotoImage(photo: Photo) {
    SubcomposeAsyncImage(
        model = photo.displayUri,
        contentDescription = "记录照片",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .height(160.dp)
            .fillMaxWidth(),
        loading = {
            PhotoPlaceholder(text = "正在加载照片")
        },
        error = {
            PhotoPlaceholder(text = "照片无法显示\n可编辑记录后重新选择")
        }
    )
}

@Composable
private fun PhotoPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

private fun Long.formatDateTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}
