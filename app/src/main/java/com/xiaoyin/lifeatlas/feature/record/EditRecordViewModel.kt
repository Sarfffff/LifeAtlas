package com.xiaoyin.lifeatlas.feature.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.data.repository.RepositoryProvider
import com.xiaoyin.lifeatlas.navigation.LifeAtlasDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditRecordUiState(
    val recordId: Long = 0,
    val title: String = "",
    val content: String = "",
    val recordTime: Long = System.currentTimeMillis(),
    val locationName: String = "",
    val mood: String = "",
    val importance: Float = 3f,
    val createdAt: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false
) {
    val canSave: Boolean
        get() = title.isNotBlank() && !isSaving && !isLoading
}

class EditRecordViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.memoryRepository(application)
    private val recordId = checkNotNull(
        savedStateHandle.get<Long>(LifeAtlasDestination.EditRecord.recordIdArg)
    )

    private val _uiState = MutableStateFlow(EditRecordUiState(recordId = recordId))
    val uiState: StateFlow<EditRecordUiState> = _uiState

    init {
        viewModelScope.launch {
            repository.observeRecord(recordId).collect { record ->
                if (record == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "记录不存在或已删除"
                        )
                    }
                } else if (!_uiState.value.isSaving) {
                    _uiState.value = record.toUiState()
                }
            }
        }
    }

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
            repository.updateRecord(
                MemoryRecord(
                    id = state.recordId,
                    title = state.title.trim(),
                    content = state.content.trim(),
                    recordTime = state.recordTime,
                    latitude = null,
                    longitude = null,
                    locationName = state.locationName.trim().ifBlank { null },
                    mood = state.mood.trim().ifBlank { null },
                    importance = state.importance.toInt(),
                    createdAt = state.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    saved = true
                )
            }
        }
    }

    fun onSavedHandled() {
        _uiState.update { it.copy(saved = false) }
    }
}

private fun MemoryRecord.toUiState(): EditRecordUiState {
    return EditRecordUiState(
        recordId = id,
        title = title,
        content = content,
        recordTime = recordTime,
        locationName = locationName.orEmpty(),
        mood = mood.orEmpty(),
        importance = importance.toFloat(),
        createdAt = createdAt,
        isLoading = false
    )
}

