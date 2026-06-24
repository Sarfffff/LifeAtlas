package com.xiaoyin.lifeatlas.feature.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.datastore.AppSettingsRepository
import com.xiaoyin.lifeatlas.data.export.ExportServiceProvider
import com.xiaoyin.lifeatlas.data.export.LifeAtlasImportPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val localFirstEnabled: Boolean = true,
    val isExporting: Boolean = false,
    val isExportingBackup: Boolean = false,
    val isImporting: Boolean = false,
    val isPreparingImport: Boolean = false,
    val pendingExportJson: String? = null,
    val pendingBackupExport: Boolean = false,
    val pendingImportJson: String? = null,
    val importPreview: LifeAtlasImportPreview? = null,
    val message: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val exportService = ExportServiceProvider.exportService(application)
    private val settingsRepository = AppSettingsRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepository.localFirstEnabled.collect { enabled ->
                _uiState.update { it.copy(localFirstEnabled = enabled) }
            }
        }
    }

    fun onLocalFirstChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLocalFirstEnabled(enabled)
        }
    }

    fun prepareExport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, message = null) }
            val json = exportService.exportJson()
            _uiState.update {
                it.copy(
                    isExporting = false,
                    pendingExportJson = json
                )
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
                        message = "导出完成"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        pendingExportJson = null,
                        message = error.message ?: "导出失败"
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
                        message = "备份包导出完成：${result.recordCount} 条记录，${result.photoCount} 张照片引用，${result.mediaFileCount} 个媒体缓存文件"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isExportingBackup = false,
                        message = error.message ?: "备份包导出失败"
                    )
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
                    importPreview = null,
                    message = null
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    val jsonText = readTextFromUri(uri)
                    jsonText to exportService.previewJson(jsonText)
                }
            }.onSuccess { (jsonText, preview) ->
                _uiState.update {
                    it.copy(
                        isPreparingImport = false,
                        pendingImportJson = jsonText,
                        importPreview = preview,
                        message = "请确认备份摘要后再导入"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isPreparingImport = false,
                        message = error.message ?: "无法预览导入文件"
                    )
                }
            }
        }
    }

    fun confirmImport() {
        val jsonText = _uiState.value.pendingImportJson ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    pendingImportJson = null,
                    importPreview = null,
                    message = null
                )
            }
            runCatching {
                withContext(Dispatchers.IO) {
                    exportService.importJson(jsonText)
                }
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        message = "导入完成：${result.recordCount} 条记录，${result.photoCount} 张照片引用，${result.tagCount} 个标签"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        message = error.message ?: "导入失败"
                    )
                }
            }
        }
    }

    fun cancelImport() {
        _uiState.update {
            it.copy(
                pendingImportJson = null,
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
}
