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
    val locationName: String = "",
    val mood: String = "",
    val importance: Float = 3f,
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

    fun onLocationNameChange(value: String) {
        _uiState.update { it.copy(locationName = value) }
    }

    fun onMoodChange(value: String) {
        _uiState.update { it.copy(mood = value) }
    }

    fun onImportanceChange(value: Float) {
        _uiState.update { it.copy(importance = value) }
    }

    fun saveRecord() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请先填写标题") }
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
                    recordTime = now,
                    latitude = null,
                    longitude = null,
                    locationName = state.locationName.trim().ifBlank { null },
                    mood = state.mood.trim().ifBlank { null },
                    importance = state.importance.toInt(),
                    createdAt = now,
                    updatedAt = now
                )
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

