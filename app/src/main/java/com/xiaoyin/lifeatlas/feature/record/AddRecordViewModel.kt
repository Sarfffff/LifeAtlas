package com.xiaoyin.lifeatlas.feature.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.datastore.AppSettingsRepository
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddRecordUiState(
    val title: String = "",
    val content: String = "",
    val recordTime: Long = System.currentTimeMillis(),
    val locationName: String = "",
    val latitudeText: String = "",
    val longitudeText: String = "",
    val mood: String = "",
    val importance: Float = 3f,
    val tagsText: String = "",
    val photoSaveStrategy: String = "缓存缩略图，保留原图引用",
    val photoUris: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val savedRecordId: Long? = null
) {
    val canSave: Boolean
        get() = title.isNotBlank() && !isSaving
}

class AddRecordViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.memoryRepository(application)
    private val settingsRepository = AppSettingsRepository(application)

    private val _uiState = MutableStateFlow(AddRecordUiState())
    val uiState: StateFlow<AddRecordUiState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepository.recordPreferences.collect { preferences ->
                _uiState.update { current ->
                    current.copy(
                        mood = current.mood.ifBlank { preferences.defaultMood },
                        tagsText = current.tagsText.ifBlank { preferences.defaultTags },
                        photoSaveStrategy = preferences.photoSaveStrategy
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, errorMessage = null, infoMessage = null) }
    }

    fun onContentChange(value: String) {
        _uiState.update { it.copy(content = value) }
    }

    fun onRecordTimeChange(value: Long) {
        _uiState.update { it.copy(recordTime = value) }
    }

    fun onLocationNameChange(value: String) {
        _uiState.update { it.copy(locationName = value) }
    }

    fun onLatitudeChange(value: String) {
        _uiState.update { it.copy(latitudeText = value, errorMessage = null, infoMessage = null) }
    }

    fun onLongitudeChange(value: String) {
        _uiState.update { it.copy(longitudeText = value, errorMessage = null, infoMessage = null) }
    }

    fun onMapPointSelected(latitude: Double, longitude: Double, address: String?) {
        _uiState.update {
            it.copy(
                latitudeText = latitude.toString(),
                longitudeText = longitude.toString(),
                locationName = address ?: it.locationName,
                errorMessage = null,
                infoMessage = "已从地图回填位置"
            )
        }
    }

    fun onMoodChange(value: String) {
        _uiState.update { it.copy(mood = value) }
    }

    fun onImportanceChange(value: Float) {
        _uiState.update { it.copy(importance = value) }
    }

    fun onTagsTextChange(value: String) {
        _uiState.update { it.copy(tagsText = value) }
    }

    fun onPhotosSelected(uris: List<String>) {
        _uiState.update { current ->
            if (uris.isEmpty()) {
                current.copy(infoMessage = "未选择新的照片", errorMessage = null)
            } else {
                val merged = (current.photoUris + uris).distinct()
                val addedCount = merged.size - current.photoUris.size
                current.copy(
                    photoUris = merged,
                    infoMessage = if (addedCount > 0) "已添加 $addedCount 张照片" else "选择的照片已在记录中",
                    errorMessage = null
                )
            }
        }
    }

    fun removePhoto(uri: String) {
        _uiState.update { current ->
            current.copy(
                photoUris = current.photoUris.filterNot { it == uri },
                infoMessage = "已移除 1 张照片",
                errorMessage = null
            )
        }
    }

    fun saveRecord() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请先填写标题", infoMessage = null) }
            return
        }
        val coordinate = state.parseCoordinateOrNull()
        if (coordinate == null && (state.latitudeText.isNotBlank() || state.longitudeText.isNotBlank())) {
            _uiState.update {
                it.copy(
                    errorMessage = "坐标格式不正确。纬度范围 -90 到 90，经度范围 -180 到 180；如果暂时不确定，可以清空坐标后保存。",
                    infoMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, infoMessage = null) }

            runCatching {
                val now = System.currentTimeMillis()
                repository.addRecord(
                    MemoryRecord(
                        id = 0,
                        title = state.title.trim(),
                        content = state.content.trim(),
                        recordTime = state.recordTime,
                        latitude = coordinate?.latitude,
                        longitude = coordinate?.longitude,
                        locationName = state.locationName.trim().ifBlank { null },
                        mood = state.mood.trim().ifBlank { null },
                        importance = state.importance.toInt(),
                        createdAt = now,
                        updatedAt = now
                    ),
                    photoUris = state.photoUris,
                    tagNames = state.tagsText.toTagNames()
                )
            }.onSuccess { recordId ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        savedRecordId = recordId
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.toSaveErrorMessage(),
                        infoMessage = null
                    )
                }
            }
        }
    }

    fun onSavedHandled() {
        _uiState.update { it.copy(savedRecordId = null) }
    }
}

private fun Throwable.toSaveErrorMessage(): String {
    return message?.takeIf { it.isNotBlank() }?.let { "保存失败：$it" }
        ?: "保存失败，请稍后重试。已填写内容仍保留在当前页面。"
}

private data class AddRecordCoordinate(val latitude: Double, val longitude: Double)

private fun AddRecordUiState.parseCoordinateOrNull(): AddRecordCoordinate? {
    val latitude = latitudeText.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
    val longitude = longitudeText.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
    if (latitude == null && longitude == null) return null
    if (latitude == null || longitude == null) return null
    if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    return AddRecordCoordinate(latitude = latitude, longitude = longitude)
}

private fun String.toTagNames(): List<String> {
    return split(",", "，")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}
