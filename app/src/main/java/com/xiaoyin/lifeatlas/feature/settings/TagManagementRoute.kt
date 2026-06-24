package com.xiaoyin.lifeatlas.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.xiaoyin.lifeatlas.core.model.Tag
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal

@Composable
fun TagManagementRoute(
    onBack: () -> Unit,
    viewModel: TagManagementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "标签背包",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = WildernessTeal
            )
            OutlinedButton(onClick = onBack) {
                Text("返回")
            }
        }
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = WildernessPaper)
        ) {
            Text(
                text = "这里用于整理记录里的标签。删除标签只会移除标签和记录的关联，不会删除任何记忆本身。",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
        }
        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (uiState.tags.isEmpty()) {
            EmptyTagCard()
        } else {
            uiState.tags.forEach { tag ->
                TagCard(
                    tag = tag,
                    onEdit = { viewModel.startEdit(tag) },
                    onDelete = { viewModel.requestDelete(tag) }
                )
            }
        }
    }

    uiState.editingTag?.let { tag ->
        TagEditDialog(
            tag = tag,
            name = uiState.editingName,
            color = uiState.editingColor,
            onNameChange = viewModel::onEditingNameChange,
            onColorChange = viewModel::onEditingColorChange,
            onDismiss = viewModel::cancelEdit,
            onSave = viewModel::saveEdit
        )
    }

    uiState.deletingTag?.let { tag ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("删除标签") },
            text = { Text("删除“${tag.name}”后，记录本身会保留，但这些记录将不再关联该标签。") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun TagCard(tag: Tag, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorDot(color = tag.color)
                AssistChip(
                    onClick = { },
                    label = { Text(tag.name) }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit) {
                    Text("编辑")
                }
                TextButton(onClick = onDelete) {
                    Text("删除")
                }
            }
        }
    }
}

@Composable
private fun EmptyTagCard() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Text(
            text = "暂无标签。新增或编辑记录时填写标签后，会出现在这里。",
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = WildernessTeal
        )
    }
}

@Composable
private fun TagEditDialog(
    tag: Tag,
    name: String,
    color: String?,
    onNameChange: (String) -> Unit,
    onColorChange: (String?) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑标签") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("标签名称") },
                    singleLine = true
                )
                Text(
                    text = "标签颜色",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagColorOptions.forEach { option ->
                        FilterChip(
                            selected = color == option.hex,
                            onClick = { onColorChange(option.hex) },
                            label = { Text(option.name) }
                        )
                    }
                }
                TextButton(onClick = { onColorChange(null) }) {
                    Text("清除颜色")
                }
                Text(
                    text = "原标签：${tag.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ColorDot(color: String?) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .background(color.toComposeColor(), RoundedCornerShape(9.dp))
    )
}

private fun String?.toComposeColor(): Color {
    return this?.let { value ->
        runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrNull()
    } ?: Color(0xFF8A8F98)
}

private data class TagColorOption(val name: String, val hex: String)

private val TagColorOptions = listOf(
    TagColorOption("山湖蓝", "#3B82F6"),
    TagColorOption("草地绿", "#22C55E"),
    TagColorOption("日落橙", "#F97316"),
    TagColorOption("花野紫", "#8B5CF6")
)
