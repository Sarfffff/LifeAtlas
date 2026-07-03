package com.xiaoyin.lifeatlas.feature.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.auth.AuthRepository
import com.xiaoyin.lifeatlas.core.datastore.AppSettingsRepository
import com.xiaoyin.lifeatlas.core.datastore.CloudSyncSettings
import com.xiaoyin.lifeatlas.core.datastore.RecordPreferenceSettings
import com.xiaoyin.lifeatlas.core.datastore.UserProfileSettings
import com.xiaoyin.lifeatlas.data.export.BackupKind
import com.xiaoyin.lifeatlas.data.export.ExportServiceProvider
import com.xiaoyin.lifeatlas.data.export.LifeAtlasImportPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.util.zip.ZipException

data class SettingsUiState(
    val localFirstEnabled: Boolean = true,
    val profile: UserProfileSettings = UserProfileSettings(),
    val recordPreferences: RecordPreferenceSettings = RecordPreferenceSettings(),
    val cloudSyncSettings: CloudSyncSettings = CloudSyncSettings(),
    val firebaseConfigured: Boolean = false,
    val backendConfigured: Boolean = false,
    val authModeLabel: String = "本地账号",
    val isExporting: Boolean = false,
    val isExportingBackup: Boolean = false,
    val isImporting: Boolean = false,
    val isPreparingImport: Boolean = false,
    val isPreparingCloudSync: Boolean = false,
    val pendingExportJson: String? = null,
    val pendingBackupExport: Boolean = false,
    val pendingBackupZipUri: Uri? = null,
    val pendingImportJson: String? = null,
    val importPreview: LifeAtlasImportPreview? = null,
    val message: SettingsMessage? = null
)

data class SettingsMessage(
    val text: String,
    val type: SettingsMessageType
)

enum class SettingsMessageType {
    Success,
    Info,
    Error
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val exportService = ExportServiceProvider.exportService(application)
    private val settingsRepository = AppSettingsRepository(application)
    private val authRepository = AuthRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.localFirstEnabled,
                settingsRepository.userProfile,
                settingsRepository.recordPreferences,
                settingsRepository.cloudSyncSettings
            ) { localFirstEnabled, profile, recordPreferences, cloudSyncSettings ->
                SettingsUiState(
                    localFirstEnabled = localFirstEnabled,
                    profile = profile,
                    recordPreferences = recordPreferences,
                    cloudSyncSettings = cloudSyncSettings,
                    firebaseConfigured = authRepository.isFirebaseActive(),
                    backendConfigured = authRepository.isBackendConfigured(),
                    authModeLabel = authRepository.authModeLabel()
                )
            }.collect { settingsState ->
                _uiState.update {
                    it.copy(
                        localFirstEnabled = settingsState.localFirstEnabled,
                        profile = settingsState.profile,
                        recordPreferences = settingsState.recordPreferences,
                        cloudSyncSettings = settingsState.cloudSyncSettings,
                        firebaseConfigured = settingsState.firebaseConfigured,
                        backendConfigured = settingsState.backendConfigured,
                        authModeLabel = settingsState.authModeLabel
                    )
                }
            }
        }
    }

    fun onLocalFirstChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLocalFirstEnabled(enabled)
        }
    }

    fun updateProfile(displayName: String, signature: String, avatarUri: String?) {
        viewModelScope.launch {
            settingsRepository.updateProfile(displayName, signature, avatarUri)
            _uiState.update { it.copy(message = SettingsMessage("资料已更新", SettingsMessageType.Success)) }
        }
    }

    fun showOnboardingAgain() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(false)
        }
    }

    fun updateRecordPreferences(defaultMood: String, defaultTags: String, photoSaveStrategy: String) {
        viewModelScope.launch {
            settingsRepository.updateRecordPreferences(defaultMood, defaultTags, photoSaveStrategy)
            _uiState.update { it.copy(message = SettingsMessage("记录偏好已保存", SettingsMessageType.Success)) }
        }
    }

    fun onCloudSyncEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCloudSyncEnabled(enabled)
            _uiState.update {
                it.copy(
                    message = SettingsMessage(
                        if (enabled) "云同步已标记为启用。当前仍保持本地优先，正式上传前会再次确认。" else "云同步已关闭，数据继续只保留在本机和备份包中。",
                        SettingsMessageType.Info
                    )
                )
            }
        }
    }

    fun prepareCloudSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingCloudSync = true, message = null) }
            runCatching {
                require(authRepository.isBackendConfigured()) { "请先登录国内后端账号，再进行云端备份" }
                val backupJson = withContext(Dispatchers.IO) { exportService.exportJson() }
                authRepository.uploadCloudBackup(backupJson)
            }.onSuccess { result ->
                settingsRepository.markCloudSyncPrepared()
                _uiState.update {
                    it.copy(
                        isPreparingCloudSync = false,
                        message = SettingsMessage(
                            "云端轻量备份完成：已保存记录、标签、地点和照片引用，约 ${result.size} 字节。照片原文件请继续使用完整备份包保存。",
                            SettingsMessageType.Success
                        )
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isPreparingCloudSync = false,
                        message = error.toSettingsMessage("云端轻量备份失败")
                    )
                }
            }
        }
    }
    fun prepareExport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, message = null) }
            runCatching {
                withContext(Dispatchers.IO) { exportService.exportJson() }
            }.onSuccess { json ->
                _uiState.update { it.copy(isExporting = false, pendingExportJson = json) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isExporting = false, message = error.toSettingsMessage("准备 JSON 导出失败"))
                }
            }
        }
    }

    fun writeExport(uri: Uri) {
        val json = _uiState.value.pendingExportJson ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("无法打开导出文件")
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        pendingExportJson = null,
                        message = SettingsMessage("JSON 数据导出完成", SettingsMessageType.Success)
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        pendingExportJson = null,
                        message = error.toSettingsMessage("导出失败")
                    )
                }
            }
        }
    }

    fun onExportLaunchHandled() {
        _uiState.update { it.copy(pendingExportJson = null) }
    }

    fun prepareBackupExport() {
        _uiState.update { it.copy(pendingBackupExport = true, message = null) }
    }

    fun writeBackupExport(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingBackup = true, pendingBackupExport = false, message = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                        exportService.exportBackupZip(output)
                    } ?: error("无法打开备份包文件")
                }
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isExportingBackup = false,
                        message = SettingsMessage(
                            text = "备份包导出完成：${result.recordCount} 条记录，${result.photoCount} 张照片引用，${result.mediaFileCount} 个媒体缓存文件",
                            type = SettingsMessageType.Success
                        )
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isExportingBackup = false, message = error.toSettingsMessage("备份包导出失败"))
                }
            }
        }
    }

    fun onBackupExportLaunchHandled() {
        _uiState.update { it.copy(pendingBackupExport = false) }
    }

    fun prepareImport(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPreparingImport = true,
                    pendingImportJson = null,
                    pendingBackupZipUri = null,
                    importPreview = null,
                    message = null
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    when (uri.detectBackupInputKind()) {
                        BackupInputKind.Zip -> ImportPreparation(
                            jsonText = null,
                            backupZipUri = uri,
                            preview = previewZipFromUri(uri)
                        )
                        BackupInputKind.Json -> {
                            val jsonText = readTextFromUri(uri)
                            ImportPreparation(
                                jsonText = jsonText,
                                backupZipUri = null,
                                preview = exportService.previewJson(jsonText)
                            )
                        }
                    }
                }
            }.onSuccess { preparation ->
                _uiState.update {
                    it.copy(
                        isPreparingImport = false,
                        pendingImportJson = preparation.jsonText,
                        pendingBackupZipUri = preparation.backupZipUri,
                        importPreview = preparation.preview,
                        message = SettingsMessage("请确认备份摘要后再导入", SettingsMessageType.Info)
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isPreparingImport = false, message = error.toSettingsMessage("无法预览导入文件"))
                }
            }
        }
    }

    fun confirmImport() {
        val state = _uiState.value
        if (state.importPreview?.backupKind == BackupKind.Zip) {
            val uri = state.pendingBackupZipUri ?: return
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isImporting = true,
                        pendingImportJson = null,
                        pendingBackupZipUri = null,
                        importPreview = null,
                        message = null
                    )
                }
                runCatching {
                    withContext(Dispatchers.IO) {
                        getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                            exportService.importBackupZip(input)
                        } ?: error("无法打开备份包文件")
                    }
                }.onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            message = SettingsMessage(
                                text = "备份包导入完成：${result.recordCount} 条记录，${result.photoCount} 张照片引用，${result.tagCount} 个标签，恢复 ${result.restoredMediaFileCount} 个媒体缓存文件",
                                type = SettingsMessageType.Success
                            )
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(isImporting = false, message = error.toSettingsMessage("备份包导入失败"))
                    }
                }
            }
            return
        }

        val jsonText = state.pendingImportJson ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    pendingImportJson = null,
                    pendingBackupZipUri = null,
                    importPreview = null,
                    message = null
                )
            }
            runCatching {
                withContext(Dispatchers.IO) { exportService.importJson(jsonText) }
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        message = SettingsMessage(
                            text = "导入完成：${result.recordCount} 条记录，${result.photoCount} 张照片引用，${result.tagCount} 个标签",
                            type = SettingsMessageType.Success
                        )
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isImporting = false, message = error.toSettingsMessage("导入失败")) }
            }
        }
    }

    fun cancelImport() {
        _uiState.update {
            it.copy(
                pendingImportJson = null,
                pendingBackupZipUri = null,
                importPreview = null,
                message = null
            )
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        return getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("无法打开导入文件")
    }

    private fun previewZipFromUri(uri: Uri): LifeAtlasImportPreview {
        return getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            exportService.previewBackupZip(input)
        } ?: error("无法打开备份包文件")
    }

    private fun Uri.detectBackupInputKind(): BackupInputKind {
        val mimeType = getApplication<Application>().contentResolver.getType(this).orEmpty().lowercase()
        val name = lastPathSegment.orEmpty().lowercase()
        return if (mimeType.contains("zip") || name.endsWith(".zip")) BackupInputKind.Zip else BackupInputKind.Json
    }
}

private enum class BackupInputKind {
    Json,
    Zip
}

private data class ImportPreparation(
    val jsonText: String?,
    val backupZipUri: Uri?,
    val preview: LifeAtlasImportPreview
)

private fun Throwable.toSettingsMessage(fallback: String): SettingsMessage {
    val detail = when (this) {
        is SerializationException -> "文件内容不是有效的岁迹备份格式"
        is ZipException -> "备份包已损坏或不是有效的 zip 文件"
        is IOException -> "文件读写失败，请检查文件是否仍可访问"
        is IllegalArgumentException -> message ?: fallback
        else -> message ?: fallback
    }
    return SettingsMessage("$fallback：$detail", SettingsMessageType.Error)
}
