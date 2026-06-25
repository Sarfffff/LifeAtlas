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
import com.xiaoyin.lifeatlas.core.datastore.RecordPreferenceSettings
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
    var showSyncPanel by remember { mutableStateOf(false) }
    var showMapPanel by remember { mutableStateOf(false) }
    var showPreferencePanel by remember { mutableStateOf(false) }
    var showAccountPanel by remember { mutableStateOf(false) }
    var showPrivacyPanel by remember { mutableStateOf(false) }

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
                subtitle = "查看当前同步方式和后续云同步计划",
                onClick = { showSyncPanel = true }
            )
        }

        SettingsSection(title = "地图与记录") {
            SettingsRow(
                icon = Icons.Outlined.LocationOn,
                title = "地图配置",
                subtitle = "${MapSdkConfig.provider.displayName} · Key ${MapSdkConfig.statusText}",
                onClick = { showMapPanel = true }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.EditNote,
                title = "记录偏好",
                subtitle = "默认记录习惯、标签和照片说明",
                onClick = { showPreferencePanel = true }
            )
        }

        SettingsSection(title = "账户与安全") {
            SettingsRow(
                icon = Icons.Outlined.PrivacyTip,
                title = "账号与安全",
                subtitle = "邮箱验证、密码登录与登录状态管理",
                onClick = { showAccountPanel = true }
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.PrivacyTip,
                title = "隐私与权限说明",
                subtitle = "查看照片、定位、网络和备份用途",
                onClick = { showPrivacyPanel = true }
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
                title = "使用引导",
                subtitle = "重新查看记录、地图和备份说明",
                onClick = viewModel::showOnboardingAgain
            )
            SettingsDivider()
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

    if (showSyncPanel) {
        SimpleSettingsDialog(
            title = "多设备同步",
            body = "当前版本采用“备份包迁移”：在旧手机导出完整备份包，在新手机选择恢复即可带走记录、标签、地点和照片缓存。后续接入云端数据库后，会升级为账号自动同步。",
            confirmText = "导出/恢复备份",
            onConfirm = {
                showSyncPanel = false
                showDataPanel = true
            },
            onDismiss = { showSyncPanel = false }
        )
    }

    if (showMapPanel) {
        SimpleSettingsDialog(
            title = "地图配置",
            body = "当前地图服务：${MapSdkConfig.provider.displayName}\nKey 状态：${MapSdkConfig.statusText}\n\n地图页会点亮带经纬度的记录。新增或编辑记录时使用“地图选点”，保存后该事件会成为地图上的一枚坐标。",
            confirmText = "知道了",
            onConfirm = { showMapPanel = false },
            onDismiss = { showMapPanel = false }
        )
    }

    if (showPreferencePanel) {
        RecordPreferenceDialog(
            localFirstEnabled = uiState.localFirstEnabled,
            preferences = uiState.recordPreferences,
            onLocalFirstChange = viewModel::onLocalFirstChange,
            onSavePreferences = viewModel::updateRecordPreferences,
            onManageTags = {
                showPreferencePanel = false
                onTagManagementClick()
            },
            onDismiss = { showPreferencePanel = false }
        )
    }

    if (showAccountPanel) {
        SimpleSettingsDialog(
            title = "账号与安全",
            body = "这里用于管理邮箱登录、邮箱验证、忘记密码和退出登录。点击下方按钮会进入账号管理页；如果已经登录，会显示账号状态，不会要求重新注册。",
            confirmText = "管理账号",
            onConfirm = {
                showAccountPanel = false
                onAccountClick()
            },
            onDismiss = { showAccountPanel = false }
        )
    }

    if (showPrivacyPanel) {
        PrivacyPermissionDialog(onDismiss = { showPrivacyPanel = false })
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
private fun RecordPreferenceDialog(
    localFirstEnabled: Boolean,
    preferences: RecordPreferenceSettings,
    onLocalFirstChange: (Boolean) -> Unit,
    onSavePreferences: (String, String, String) -> Unit,
    onManageTags: () -> Unit,
    onDismiss: () -> Unit
) {
    var defaultMood by remember(preferences) { mutableStateOf(preferences.defaultMood) }
    var defaultTags by remember(preferences) { mutableStateOf(preferences.defaultTags) }
    var photoSaveStrategy by remember(preferences) { mutableStateOf(preferences.photoSaveStrategy) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录偏好") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("这里集中管理写记录时常用的习惯设置。保存后，新增记录会自动带入默认心情和默认标签。")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("本地优先", fontWeight = FontWeight.Black, color = WildernessTeal)
                        Text("默认不自动上传个人记录", style = MaterialTheme.typography.bodyMedium, color = WildernessMuted)
                    }
                    Switch(checked = localFirstEnabled, onCheckedChange = onLocalFirstChange)
                }
                OutlinedTextField(
                    value = defaultMood,
                    onValueChange = { defaultMood = it },
                    label = { Text("默认心情") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = defaultTags,
                    onValueChange = { defaultTags = it },
                    label = { Text("默认标签") },
                    placeholder = { Text("用逗号分隔，例如：日常，家人") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = photoSaveStrategy,
                    onValueChange = { photoSaveStrategy = it },
                    label = { Text("照片保存策略") },
                    placeholder = { Text("缓存缩略图，保留原图引用") },
                    minLines = 2
                )
                Text("当前照片策略会作为说明保存；实际照片仍优先生成缩略图并缓存到 App 私有目录。")
                OutlinedButton(onClick = onManageTags, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("管理标签")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSavePreferences(defaultMood, defaultTags, photoSaveStrategy)
                    onDismiss()
                }
            ) {
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
private fun PrivacyPermissionDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("隐私与权限说明") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionItem(
                    title = "照片与文件",
                    body = "用于导入记录照片、生成缩略图、导出和恢复备份包。照片优先缓存在 App 私有目录。"
                )
                PermissionItem(
                    title = "位置与地图",
                    body = "用于地图选点、当前位置选择和点亮城市。只有你保存记录时，坐标才会进入本地数据库。"
                )
                PermissionItem(
                    title = "网络",
                    body = "用于 Firebase 邮箱登录、发送验证邮件、地图 SDK 加载地图服务，以及后续云同步。"
                )
                PermissionItem(
                    title = "账号",
                    body = "邮箱登录用于识别你的账号。当前记录仍以本地保存为主，后续云同步会再明确确认。"
                )
                PermissionItem(
                    title = "本地优先",
                    body = "开启本地优先时，岁迹不会自动上传个人记录。重要数据建议定期导出完整备份包。"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
}

@Composable
private fun PermissionItem(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = WildernessTeal)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = WildernessMuted)
    }
}

@Composable
private fun SimpleSettingsDialog(
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = WildernessMuted
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
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
