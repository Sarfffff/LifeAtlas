package com.xiaoyin.lifeatlas.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyin.lifeatlas.BuildConfig
import com.xiaoyin.lifeatlas.R
import com.xiaoyin.lifeatlas.core.map.MapSdkConfig
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessCream
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessLine
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMuted
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower
import com.xiaoyin.lifeatlas.data.export.BackupKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsRoute(
    onTagManagementClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.writeExport(uri) else viewModel.onExportLaunchHandled()
    }
    val backupExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.writeBackupExport(uri) else viewModel.onBackupExportLaunchHandled()
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.prepareImport(uri)
    }

    LaunchedEffect(uiState.pendingExportJson) {
        if (uiState.pendingExportJson != null) exportLauncher.launch("lifeatlas_export.json")
    }

    LaunchedEffect(uiState.pendingBackupExport) {
        if (uiState.pendingBackupExport) backupExportLauncher.launch("lifeatlas_backup.zip")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WildernessCream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = WildernessTeal
        )

        TravelerProfileCard()

        SettingsSection(title = "数据与同步") {
            SettingsRow(
                icon = Icons.Outlined.CloudUpload,
                title = if (uiState.isExportingBackup) "数据备份中" else "数据备份与恢复",
                subtitle = uiState.message?.text?.takeIf { it.contains("备份") } ?: "上次备份：今天 08:30",
                onClick = viewModel::prepareBackupExport
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Sync,
                title = if (uiState.isPreparingImport || uiState.isImporting) "正在同步数据" else "多设备同步",
                subtitle = if (uiState.localFirstEnabled) "已同步到 2 台设备" else "本地优先已关闭",
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/*", "application/zip", "application/octet-stream"))
                }
            )
        }

        SettingsSection(title = "地图与记录") {
            SettingsRow(
                icon = Icons.Outlined.LocationOn,
                title = "地图配置",
                subtitle = "${MapSdkConfig.provider.displayName} · Key ${MapSdkConfig.statusText}",
                onClick = viewModel::prepareExport
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.EditNote,
                title = "记录偏好",
                subtitle = "标签、天气、心情等设置",
                onClick = onTagManagementClick
            )
        }

        SettingsSection(title = "账户与安全") {
            SettingsRow(
                icon = Icons.Outlined.PrivacyTip,
                title = "账号与安全",
                subtitle = "修改密码、绑定手机",
                onClick = {}
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Lock,
                title = "隐私设置",
                subtitle = "权限管理与隐私选项",
                onClick = {}
            )
        }

        SettingsSection(title = "关于") {
            SettingsRow(
                icon = Icons.Outlined.Info,
                title = "关于岁迹",
                subtitle = "版本 ${BuildConfig.VERSION_NAME}",
                trailingText = "版本 ${BuildConfig.VERSION_NAME}",
                onClick = {}
            )
        }

        uiState.message?.let { message ->
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = when (message.type) {
                    SettingsMessageType.Error -> Color(0xFFB4533A)
                    SettingsMessageType.Success -> WildernessTeal
                    SettingsMessageType.Info -> WildernessMuted
                },
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
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

@Composable
private fun TravelerProfileCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper.copy(alpha = 0.98f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_mascot_text),
                contentDescription = "旷野小旅人",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(19.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "旷野小旅人",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = WildernessTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lv.3",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF09A52))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "记录生活，探索世界",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WildernessMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = WildernessMuted,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = WildernessTeal.copy(alpha = 0.72f),
            modifier = Modifier
                .padding(start = 12.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(WildernessMeadow.copy(alpha = 0.72f))
                .padding(horizontal = 11.dp, vertical = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = WildernessPaper.copy(alpha = 0.98f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(WildernessPaper),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = WildernessTeal,
                modifier = Modifier.size(27.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = WildernessTeal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = WildernessMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = WildernessMuted,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = WildernessMuted,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp, end = 18.dp),
        color = WildernessLine.copy(alpha = 0.8f)
    )
}

private fun Long.formatDateTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}
