package com.xiaoyin.lifeatlas.feature.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.core.time.formatDate
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessCream
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMuted
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessSky
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal

@Composable
fun TrashRoute(onBack: () -> Unit, viewModel: TrashViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var pendingPermanentDelete by remember { mutableStateOf<MemoryRecord?>(null) }
    var showEmptyConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(WildernessCream).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = WildernessTeal) }
            Column(modifier = Modifier.weight(1f)) {
                Text("回收站", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = WildernessTeal)
                Text("删除的记忆会留在这里，直到你永久清除。", color = WildernessMuted)
            }
            if (state.records.isNotEmpty()) {
                TextButton(onClick = { showEmptyConfirm = true }, enabled = !state.isWorking) { Text("清空") }
            }
        }

        state.message?.let {
            Text(it, color = WildernessTeal, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
        }

        if (state.records.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WildernessPaper)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("回收站是空的", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WildernessTeal)
                    Text("从首页、时间轴或详情页删除的记忆会先来到这里。", color = WildernessMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.records, key = { it.id }) { record ->
                    TrashRecordCard(
                        record = record,
                        photo = state.firstPhotosByRecordId[record.id],
                        enabled = !state.isWorking,
                        onRestore = { viewModel.restore(record.id) },
                        onPermanentDelete = { pendingPermanentDelete = record }
                    )
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }

    pendingPermanentDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingPermanentDelete = null },
            title = { Text("永久删除") },
            text = { Text("确定永久删除「${record.title}」吗？关联照片缓存也会被清理，此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    pendingPermanentDelete = null
                    viewModel.permanentlyDelete(record.id)
                }) { Text("永久删除") }
            },
            dismissButton = { TextButton(onClick = { pendingPermanentDelete = null }) { Text("取消") } }
        )
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text("清空回收站") },
            text = { Text("回收站中的 ${state.records.size} 条记忆及其照片缓存会被永久删除。") },
            confirmButton = {
                TextButton(onClick = {
                    showEmptyConfirm = false
                    viewModel.emptyTrash()
                }) { Text("确认清空") }
            },
            dismissButton = { TextButton(onClick = { showEmptyConfirm = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun TrashRecordCard(
    record: MemoryRecord,
    photo: Photo?,
    enabled: Boolean,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = WildernessPaper)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (photo != null) {
                    AsyncImage(
                        model = photo.displayUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(78.dp)
                    )
                } else {
                    Spacer(Modifier.size(78.dp).background(WildernessSky.copy(alpha = 0.35f), RoundedCornerShape(16.dp)))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(record.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = WildernessTeal, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(record.locationName ?: "未填写地点", color = WildernessMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("记录于 ${record.recordTime.formatDate()}", color = WildernessMuted)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onRestore, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Restore, contentDescription = null)
                    Text("恢复")
                }
                Button(onClick = onPermanentDelete, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.DeleteForever, contentDescription = null)
                    Text("永久删除")
                }
            }
        }
    }
}
