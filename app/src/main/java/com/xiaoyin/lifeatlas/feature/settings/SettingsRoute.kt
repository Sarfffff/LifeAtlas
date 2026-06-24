package com.xiaoyin.lifeatlas.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyin.lifeatlas.R
import com.xiaoyin.lifeatlas.BuildConfig
import com.xiaoyin.lifeatlas.data.export.BackupKind
import com.xiaoyin.lifeatlas.core.map.MapSdkConfig
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessSky
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower
import com.xiaoyin.lifeatlas.feature.settings.SettingsMessageType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsRoute(
    onTagManagementClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
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
    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.writeBackupExport(uri)
        } else {
            viewModel.onBackupExportLaunchHandled()
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

    LaunchedEffect(uiState.pendingBackupExport) {
        if (uiState.pendingBackupExport) {
            backupExportLauncher.launch("lifeatlas_backup.zip")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "设置", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = WildernessTeal)
        Text(
            text = "把数据、地图和账号入口收进背包，继续轻装上路。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
        )
        TravelerProfileCard()
        SettingGroup(title = "账号与本地", tint = WildernessMeadow) {
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
                title = "账号与安全",
                body = "邮箱验证登录、密码登录和云同步将在后续版本接入。"
            )
        }
        SettingGroup(title = "数据背包", tint = WildernessWildflower) {
            SettingCard(
                title = "它是做什么的？",
                body = "数据背包用于把你的记录、标签、地点和照片缓存打包带走。换手机、重装 App 或备份重要记忆时使用。"
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
                title = "完整备份包",
                body = "导出 JSON 数据和已生成的照片缩略图缓存为 zip 文件。",
                trailing = {
                    Button(
                        onClick = viewModel::prepareBackupExport,
                        enabled = !uiState.isExportingBackup
                    ) {
                        Text(if (uiState.isExportingBackup) "打包中" else "导出")
                    }
                }
            )
            SettingCard(
                title = "数据导入",
                body = "预览 JSON 或完整备份包，确认后再恢复到本机。",
                trailing = {
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "application/zip", "application/octet-stream")) },
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
        }
        SettingGroup(title = "地图与记录", tint = WildernessSky) {
            SettingCard(
                title = "标签管理",
                body = "整理标签颜色、名称和记录关联。",
                trailing = {
                    OutlinedButton(onClick = onTagManagementClick) {
                        Text("查看")
                    }
                }
            )
            SettingCard(
                title = "地图配置",
                body = "供应商：${MapSdkConfig.provider.displayName}\nKey 状态：${if (MapSdkConfig.isAmapConfigured) "已配置" else "未配置"}"
            )
        }
        SettingGroup(title = "关于岁迹", tint = WildernessMeadow) {
            SettingCard(
                title = "版本信息",
                body = "版本：${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）\n包名：${BuildConfig.APPLICATION_ID}\n模式：${if (uiState.localFirstEnabled) "本地优先" else "本地优先已关闭"}"
            )
            SettingCard(title = "人生地图", body = "岁迹 | 我的人生地图\n当前阶段：V1.0 正式体验版，等待体验反馈")
        }
        uiState.message?.let { message ->
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = when (message.type) {
                    SettingsMessageType.Success -> MaterialTheme.colorScheme.primary
                    SettingsMessageType.Info -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    SettingsMessageType.Error -> MaterialTheme.colorScheme.error
                }
            )
        }
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
                    preview.mediaFileCount?.let { mediaFileCount ->
                        Text("媒体缓存文件：$mediaFileCount 个")
                    }
                    if (preview.backupKind == BackupKind.Zip) {
                        Text("导入会恢复结构化数据，并把备份包内媒体缓存写回 App 私有目录。同 ID 的本地记录会被覆盖。")
                    } else {
                        Text("导入会按备份文件恢复数据。同 ID 的本地记录会被覆盖，建议先导出当前数据。")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmImport
                ) {
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
private fun TravelerProfileCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_mascot_text),
                contentDescription = "旷野小旅人",
                modifier = Modifier
                    .size(72.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "旷野小旅人", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WildernessTeal)
                Text(
                    text = "记录生活，探索世界",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
            Text(
                text = "Lv.1",
                style = MaterialTheme.typography.labelLarge,
                color = WildernessTeal,
                modifier = Modifier
                    .background(WildernessWildflower.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun SettingGroup(title: String, tint: androidx.compose.ui.graphics.Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .background(tint, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(text = title, style = MaterialTheme.typography.labelLarge, color = WildernessTeal, fontWeight = FontWeight.Bold)
                }
            }
            content()
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    body: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = WildernessTeal)
                Text(text = body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
            }
            trailing?.invoke()
        }
    }
}
