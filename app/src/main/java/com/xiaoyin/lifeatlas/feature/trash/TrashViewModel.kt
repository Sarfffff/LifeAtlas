package com.xiaoyin.lifeatlas.feature.trash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrashUiState(
    val records: List<MemoryRecord> = emptyList(),
    val firstPhotosByRecordId: Map<Long, Photo> = emptyMap(),
    val message: String? = null,
    val isWorking: Boolean = false
)

class TrashViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.memoryRepository(application)
    private val operationState = MutableStateFlow(TrashUiState())

    val uiState: StateFlow<TrashUiState> = combine(
        repository.observeDeletedRecords(),
        repository.observeAllPhotosIncludingDeleted(),
        operationState
    ) { records, photos, operation ->
        operation.copy(
            records = records,
            firstPhotosByRecordId = photos.groupBy { it.recordId }.mapValues { it.value.first() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrashUiState())

    fun restore(recordId: Long) = runOperation("记忆已恢复") {
        repository.restoreRecord(recordId)
    }

    fun permanentlyDelete(recordId: Long) = runOperation("记忆已永久删除") {
        repository.permanentlyDeleteRecord(recordId)
    }

    fun emptyTrash() = runOperation("回收站已清空") {
        repository.emptyTrash()
    }

    fun clearMessage() {
        operationState.update { it.copy(message = null) }
    }

    private fun runOperation(successMessage: String, block: suspend () -> Unit) {
        if (operationState.value.isWorking) return
        viewModelScope.launch {
            operationState.update { it.copy(isWorking = true, message = null) }
            runCatching { block() }
                .onSuccess { operationState.update { it.copy(isWorking = false, message = successMessage) } }
                .onFailure { error ->
                    operationState.update {
                        it.copy(isWorking = false, message = error.message ?: "操作失败，请稍后重试")
                    }
                }
        }
    }
}
