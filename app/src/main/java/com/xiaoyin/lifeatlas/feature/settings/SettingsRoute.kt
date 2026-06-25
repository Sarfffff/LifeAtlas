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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaoyin.lifeatlas.BuildConfig
import com.xiaoyin.lifeatlas.R
import com.xiaoyin.lifeatlas.core.datastore.UserProfileSettings
import com.xiaoyin.lifeatlas.core.map.MapSdkConfig
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessCream
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessLine
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMuted
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.data.export.BackupKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsRoute(
    onAccountClick: () -> Unit,
    onTagManagementClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showProfileEditor by remember { mutableStateOf(false) }
    var showDataPanel by remember { mutableStateOf(false) }

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

        TravelerProfileCard(
            profile = uiState.profile,
            onClick = { showProfileEditor = true }
        )

        SettingsSection(title = "数据与同步") {
            SettingsRow(
                icon = Icons.Outlined.CloudUpload,
                title = if (uiState.isExportingBackup) "正在打包备份" else "数据备份与恢复",
                subtitle = uiState.message?.text?.takeIf { it.contains("备份") } ?: "备份记录、标签、地点和照片缓存",
                onClick = { showDataPanel = true }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Sync,
                title = "多设备同步",
                subtitle = "通过备份包在手机之间迁移和恢复",
                onClick = { showDataPanel = true }
            )
        }

        SettingsSection(title = "地图与记录") {
            SettingsRow(
                icon = Icons.Outlined.LocationOn,
                title = "地图配置",
                subtitle = "${MapSdkConfig.provider.displayName} · Key ${MapSdkConfig.statusText}",
                onClick = onAccountClick
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
                subtitle = "邮箱验证、密码登录与登录状态管理",
                onClick = onAccountClick
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Lock,
                title = "隐私设置",
                subtitle = if (uiState.localFirstEnabled) "本地优先，不自动上传个人记录" else "本地优先已关闭",
                onClick = { viewModel.onLocalFirstChange(!uiState.localFirstEnabled) },
                trailing = {
                    Switch(
                        checked = uiState.localFirstEnabled,
                        onCheckedChange = viewModel::onLocalFirstChange
                    )
                }
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

    if (showProfileEditor) {
        ProfileEditorDialog(
            profile = uiState.profile,
            onDismiss = { showProfileEditor = false },
            onSave = { name, signature, avatarUri ->
                viewModel.updateProfile(name, signature, avatarUri)
                showProfileEditor = false
            }
        )
    }

    if (showDataPanel) {
        DataSyncDialog(
            uiState = uiState,
            onDismiss = { showDataPanel = false },
            onExportBackup = viewModel::prepareBackupExport,
            onExportJson = viewModel::prepareExport,
            onImport = {
                importLauncher.launch(arrayOf("application/json", "text/*", "application/zip", "application/octet-stream"))
            },
            onLocalFirstChange = viewModel::onLocalFirstChange
        )
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
                    preview.mediaFileCount?.let { Text("媒体缓存文件：$it 个") }
                    if (preview.backupKind == BackupKind.Zip) {
                        Text("导入会恢复记录数据，并把备份包里的照片缓存写回 App 私有目录。同 ID 的本地记录会被覆盖。")
                    } else {
                        Text("导入会按 JSON 文件恢复数据。同 ID 的本地记录会被覆盖，建议先导出当前数据。")
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
private fun TravelerProfileCard(profile: UserProfileSettings, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper.copy(alpha = 0.98f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(profile.avatarUri)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = WildernessTeal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = profile.signature,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WildernessMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = WildernessMuted, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun ProfileAvatar(avatarUri: String?) {
    val modifier = Modifier
        .size(74.dp)
        .clip(RoundedCornerShape(19.dp))

    if (avatarUri.isNullOrBlank()) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_mascot_text),
            contentDescription = "头像",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = avatarUri,
            contentDescription = "头像",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
private fun ProfileEditorDialog(
    profile: UserProfileSettings,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit
) {
    var name by remember(profile) { mutableStateOf(profile.displayName) }
    var signature by remember(profile) { mutableStateOf(profile.signature) }
    var avatarUri by remember(profile) { mutableStateOf(profile.avatarUri) }
    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) avatarUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑个人资料") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProfileAvatar(avatarUri)
                    OutlinedButton(onClick = { avatarLauncher.launch(arrayOf("image/*")) }) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("更换头像")
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = signature,
                    onValueChange = { signature = it },
                    label = { Text("签名") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, signature, avatarUri) }) {
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
private fun DataSyncDialog(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onExportBackup: () -> Unit,
    onExportJson: () -> Unit,
    onImport: () -> Unit,
    onLocalFirstChange: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("数据与同步") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("完整备份包会包含记录、标签、地点和可读取的照片缓存。换手机或重装 App 时，选择备份包即可恢复。")
                Text("多设备同步当前以备份包迁移实现；后续接入账号和云端后，会升级为自动同步。")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("本地优先", fontWeight = FontWeight.Black, color = WildernessTeal)
                    Switch(checked = uiState.localFirstEnabled, onCheckedChange = onLocalFirstChange)
                }
                Button(onClick = onExportBackup, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isExportingBackup) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isExportingBackup) "正在打包..." else "导出完整备份包")
                }
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isPreparingImport && !uiState.isImporting) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        when {
                            uiState.isPreparingImport -> "正在预览..."
                            uiState.isImporting -> "正在恢复..."
                            else -> "从备份包/JSON 恢复"
                        }
                    )
                }
                OutlinedButton(onClick = onExportJson, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isExporting) {
                    Icon(Icons.Outlined.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isExporting) "正在准备..." else "仅导出 JSON 数据")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
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
            Column { content() }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingText: String? = null,
    trailing: @Composable (() -> Unit)? = null
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
            Icon(imageVector = icon, contentDescription = null, tint = WildernessTeal, modifier = Modifier.size(27.dp))
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
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = WildernessMuted, modifier = Modifier.padding(start = 8.dp))
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = WildernessMuted, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 68.dp, end = 18.dp), color = WildernessLine.copy(alpha = 0.8f))
}

private fun Long.formatDateTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}
