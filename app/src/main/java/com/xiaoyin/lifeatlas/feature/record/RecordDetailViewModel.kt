package com.xiaoyin.lifeatlas.feature.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.data.repository.RepositoryProvider
import com.xiaoyin.lifeatlas.navigation.LifeAtlasDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecordDetailUiState(
    val record: MemoryRecord? = null,
    val isDeleting: Boolean = false,
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

    val uiState: StateFlow<RecordDetailUiState> = repository.observeRecord(recordId)
        .map { record -> RecordDetailUiState(record = record) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecordDetailUiState()
        )

    private val _deleteState = kotlinx.coroutines.flow.MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleteState

    fun deleteRecord() {
        viewModelScope.launch {
            repository.deleteRecord(recordId)
            _deleteState.value = true
        }
    }
}
