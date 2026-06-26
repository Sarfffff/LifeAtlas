package com.xiaoyin.lifeatlas.feature.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.core.model.Tag
import com.xiaoyin.lifeatlas.data.repository.RepositoryProvider
import com.xiaoyin.lifeatlas.navigation.LifeAtlasDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordDetailUiState(
    val record: MemoryRecord? = null,
    val photos: List<Photo> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val isFavorite: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val deleted: Boolean = false
)

private data class DeleteOperationState(
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val deleted: Boolean = false
)

class RecordDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.memoryRepository(application)
    private val recordId = checkNotNull(
        savedStateHandle.get<Long>(LifeAtlasDestination.RecordDetail.recordIdArg)
    )

    private val deleteOperationState = MutableStateFlow(DeleteOperationState())

    val uiState: StateFlow<RecordDetailUiState> = combine(
        repository.observeRecord(recordId),
        repository.observePhotos(recordId),
        repository.observeTags(recordId),
        repository.observeFavoriteRecordIds(),
        deleteOperationState
    ) { record, photos, tags, favoriteRecordIds, deleteState ->
        RecordDetailUiState(
            record = record,
            photos = photos,
            tags = tags,
            isFavorite = recordId in favoriteRecordIds,
            isDeleting = deleteState.isDeleting,
            errorMessage = deleteState.errorMessage,
            deleted = deleteState.deleted
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecordDetailUiState()
        )

    fun deleteRecord() {
        if (deleteOperationState.value.isDeleting) return

        viewModelScope.launch {
            deleteOperationState.update { it.copy(isDeleting = true, errorMessage = null) }
            runCatching {
                repository.deleteRecord(recordId)
            }.onSuccess {
                deleteOperationState.update {
                    it.copy(
                        isDeleting = false,
                        deleted = true
                    )
                }
            }.onFailure { error ->
                deleteOperationState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = error.toDeleteErrorMessage()
                    )
                }
            }
        }
    }

    fun setFavorite(favorite: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(recordId, favorite)
        }
    }
}

private fun Throwable.toDeleteErrorMessage(): String {
    return message?.takeIf { it.isNotBlank() }?.let { "删除失败：$it" }
        ?: "删除失败，请稍后重试。记录仍保留在当前页面。"
}
