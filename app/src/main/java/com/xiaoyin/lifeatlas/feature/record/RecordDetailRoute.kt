package com.xiaoyin.lifeatlas.feature.record

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val context = LocalContext.current
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
            RecordDetailContent(
                record = record,
                photos = uiState.photos,
                tags = uiState.tags,
                isFavorite = uiState.isFavorite,
                onFavoriteClick = { viewModel.setFavorite(!uiState.isFavorite) },
                onShareClick = {
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "岁迹记忆：${record.title}")
                                putExtra(Intent.EXTRA_TEXT, record.toShareText())
                            },
                            "分享这段记忆"
                        )
                    )
                },
                onEditPhotos = { onEdit(record.id) }
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除记录") },
            text = { Text("这条记录会移入回收站，暂时不会清理照片。你可以在设置的回收站中恢复或永久删除。") },
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
private fun RecordDetailContent(
    record: MemoryRecord,
    photos: List<Photo>,
    tags: List<Tag>,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onEditPhotos: () -> Unit
) {
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
                color = WildernessTeal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("重要程度", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                ImportanceStars(value = record.importance.toFloat())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onFavoriteClick, modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.Icon(
                        if (isFavorite) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isFavorite) "已收藏" else "收藏")
                }
                OutlinedButton(onClick = onShareClick, modifier = Modifier.weight(1f)) {
                    androidx.compose.material3.Icon(
                        Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("分享")
                }
            }
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
    DetailPhotoSection(photos = photos, onEditPhotos = onEditPhotos)
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

private fun MemoryRecord.toShareText(): String {
    val place = locationName?.takeIf { it.isNotBlank() } ?: "未填写地点"
    val coordinate = if (latitude != null && longitude != null) {
        "\n坐标：$latitude, $longitude"
    } else {
        ""
    }
    return "我在岁迹记录了一段记忆：$title\n地点：$place\n日期：${recordTime.formatDateTime()}$coordinate\n\n${content.ifBlank { "这段记忆还没有正文。" }}"
}

@Composable
private fun DetailPhotoSection(photos: List<Photo>, onEditPhotos: () -> Unit) {
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "照片记忆",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = WildernessTeal
                    )
                    Text(
                        text = if (photos.isEmpty()) "还没有照片，可以为这段记忆补一张风景。" else "已保存 ${photos.size} 张照片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                    )
                }
                OutlinedButton(onClick = onEditPhotos) {
                    Text(if (photos.isEmpty()) "添加照片" else "管理照片")
                }
            }

            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .background(WildernessSky.copy(alpha = 0.26f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "添加照片后，这里会展示这段记忆的画面。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WildernessTeal.copy(alpha = 0.72f)
                    )
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(photos.size) { index ->
                        val photo = photos[index]
                        Card(
                            modifier = Modifier
                                .width(228.dp)
                                .clickable { selectedPhotoIndex = index },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        ) {
                            DetailPhotoImage(photo = photo)
                        }
                    }
                }
            }
        }
    }

    selectedPhotoIndex?.let { index ->
        PhotoViewerDialog(
            photos = photos,
            initialIndex = index,
            onDismiss = { selectedPhotoIndex = null }
        )
    }
}

@Composable
private fun PhotoViewerDialog(
    photos: List<Photo>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, photos.lastIndex),
        pageCount = { photos.size }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WildernessTeal)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) { page ->
                SubcomposeAsyncImage(
                    model = photos[page].compressedPath ?: photos[page].originalUri,
                    contentDescription = "照片 ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    loading = { PhotoPlaceholder("正在加载照片") },
                    error = { PhotoPlaceholder("照片无法显示") }
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
                    .background(WildernessPaper.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
            ) {
                androidx.compose.material3.Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = WildernessTeal)
            }

            Text(
                text = "${pagerState.currentPage + 1} / ${photos.size}",
                color = WildernessTeal,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp)
                    .background(WildernessPaper.copy(alpha = 0.92f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
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
