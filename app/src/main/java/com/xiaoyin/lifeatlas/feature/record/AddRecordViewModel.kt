package com.xiaoyin.lifeatlas.feature.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    val photoUris: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedRecordId: Long? = null
) {
    val canSave: Boolean
        get() = title.isNotBlank() && !isSaving
}

class AddRecordViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.memoryRepository(application)

    private val _uiState = MutableStateFlow(AddRecordUiState())
    val uiState: StateFlow<AddRecordUiState> = _uiState

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, errorMessage = null) }
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
        _uiState.update { it.copy(latitudeText = value, errorMessage = null) }
    }

    fun onLongitudeChange(value: String) {
        _uiState.update { it.copy(longitudeText = value, errorMessage = null) }
    }

    fun onMapPointSelected(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(
                latitudeText = latitude.toString(),
                longitudeText = longitude.toString(),
                errorMessage = null
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
            current.copy(photoUris = (current.photoUris + uris).distinct())
        }
    }

    fun removePhoto(uri: String) {
        _uiState.update { current ->
            current.copy(photoUris = current.photoUris.filterNot { it == uri })
        }
    }

    fun saveRecord() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请先填写标题") }
            return
        }
        val coordinate = state.parseCoordinateOrNull()
        if (coordinate == null && (state.latitudeText.isNotBlank() || state.longitudeText.isNotBlank())) {
            _uiState.update { it.copy(errorMessage = "请填写有效经纬度，纬度范围 -90 到 90，经度范围 -180 到 180") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val now = System.currentTimeMillis()
            val recordId = repository.addRecord(
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

            _uiState.update {
                it.copy(
                    isSaving = false,
                    savedRecordId = recordId
                )
            }
        }
    }

    fun onSavedHandled() {
        _uiState.update { it.copy(savedRecordId = null) }
    }
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
