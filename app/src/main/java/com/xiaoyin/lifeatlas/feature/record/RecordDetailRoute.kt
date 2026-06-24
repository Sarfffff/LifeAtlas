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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessSky
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower
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
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) {
            onDeleted()
        }
    }

    val record = uiState.record

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
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
                OutlinedButton(
                    onClick = { record?.let { onEdit(it.id) } },
                    enabled = record != null && !uiState.isDeleting
                ) {
                    Text("编辑")
                }
                Button(
                    onClick = { showDeleteDialog = true },
                    enabled = record != null && !uiState.isDeleting
                ) {
                    Text(if (uiState.isDeleting) "删除中" else "删除")
                }
            }
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (record == null) {
            EmptyDetailCard()
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
                    enabled = !uiState.isDeleting,
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
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = record.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = WildernessTeal
            )
            Text(
                text = record.recordTime.formatDateTime(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
            record.locationName?.let {
                DetailLine(label = "地点", value = it)
            }
            if (record.latitude != null && record.longitude != null) {
                DetailLine(label = "坐标", value = "${record.latitude}, ${record.longitude}")
            }
            record.mood?.let {
                DetailLine(label = "心情", value = it)
            }
            DetailLine(label = "重要程度", value = "${record.importance}")
        }
    }
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
        Text(text = "照片记忆", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WildernessTeal)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(photos) { photo ->
                Card(
                    modifier = Modifier.width(220.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = WildernessPaper)
                ) {
                    DetailPhotoImage(photo = photo)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Text(
            text = record.content.ifBlank { "暂无正文" },
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "$label：", style = MaterialTheme.typography.bodyMedium, color = WildernessTeal, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f))
    }
}

@Composable
private fun EmptyDetailCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Text(
            text = "这段记忆不存在或已被删除。",
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = WildernessTeal
        )
    }
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
            .background(WildernessSky.copy(alpha = 0.35f)),
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
    } ?: WildernessWildflower
}

private fun Long.formatDateTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}
