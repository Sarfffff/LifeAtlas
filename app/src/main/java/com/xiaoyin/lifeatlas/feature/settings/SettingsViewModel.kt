package com.xiaoyin.lifeatlas.feature.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.datastore.AppSettingsRepository
import com.xiaoyin.lifeatlas.data.export.ExportServiceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val localFirstEnabled: Boolean = true,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val pendingExportJson: String? = null,
    val pendingImportUri: Uri? = null,
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

    fun prepareImport(uri: Uri) {
        _uiState.update {
            it.copy(
                pendingImportUri = uri,
                message = "确认导入后，同 ID 的本地记录会被备份内容覆盖"
            )
        }
    }

    fun confirmImport() {
        val uri = _uiState.value.pendingImportUri ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, pendingImportUri = null, message = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val jsonText = getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error("无法打开导入文件")
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
                pendingImportUri = null,
                message = null
            )
        }
    }
}
