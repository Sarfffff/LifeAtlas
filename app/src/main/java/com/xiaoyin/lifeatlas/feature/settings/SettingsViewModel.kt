package com.xiaoyin.lifeatlas.feature.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.data.export.ExportServiceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val isExporting: Boolean = false,
    val pendingExportJson: String? = null,
    val message: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val exportService = ExportServiceProvider.exportService(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

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
}
