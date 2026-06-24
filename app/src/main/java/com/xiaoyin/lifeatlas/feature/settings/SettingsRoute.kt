package com.xiaoyin.lifeatlas.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyin.lifeatlas.core.ui.theme.AtlasMist
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.writeExport(uri)
        } else {
            viewModel.onExportLaunchHandled()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.prepareImport(uri)
        }
    }

    LaunchedEffect(uiState.pendingExportJson) {
        if (uiState.pendingExportJson != null) {
            exportLauncher.launch("lifeatlas_export.json")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "设置", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        SettingCard(
            title = "本地优先",
            body = "第一版不强制登录，不自动上传个人记录。",
            trailing = {
                Switch(
                    checked = uiState.localFirstEnabled,
                    onCheckedChange = viewModel::onLocalFirstChange
                )
            }
        )
        SettingCard(
            title = "数据导出",
            body = "导出记录、照片 URI、标签和关联关系为 JSON 文件。",
            trailing = {
                Button(
                    onClick = viewModel::prepareExport,
                    enabled = !uiState.isExporting
                ) {
                    Text(if (uiState.isExporting) "准备中" else "导出")
                }
            }
        )
        SettingCard(
            title = "数据导入",
            body = "从岁迹 JSON 备份恢复记录、照片 URI、标签和关联关系。",
            trailing = {
                Button(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                    enabled = !uiState.isImporting && !uiState.isPreparingImport
                ) {
                    Text(
                        when {
                            uiState.isPreparingImport -> "预览中"
                            uiState.isImporting -> "导入中"
                            else -> "导入"
                        }
                    )
                }
            }
        )
        SettingCard(
            title = "标签管理",
            body = "后续支持标签重命名、删除和颜色设置。当前可在新增或编辑记录时填写标签。"
        )
        uiState.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        SettingCard(title = "关于岁迹", body = "岁迹 | 我的人生地图")
    }

    uiState.importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text("确认导入备份") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("备份时间：${preview.exportedAt.formatDateTime()}")
                    Text("记录：${preview.recordCount} 条")
                    Text("照片引用：${preview.photoCount} 张")
                    Text("标签：${preview.tagCount} 个")
                    Text("标签关联：${preview.recordTagCount} 条")
                    Text("导入会按备份文件恢复数据。同 ID 的本地记录会被覆盖，建议先导出当前数据。")
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmImport) {
                    Text("确认导入")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelImport) {
                    Text("取消")
                }
            }
        )
    }
}

private fun Long.formatDateTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}

@Composable
private fun SettingCard(
    title: String,
    body: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AtlasMist)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
            }
            trailing?.invoke()
        }
    }
}
