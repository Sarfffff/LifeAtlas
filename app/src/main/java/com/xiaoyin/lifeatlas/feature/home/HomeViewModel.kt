package com.xiaoyin.lifeatlas.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val recordCount: Int = 0,
    val photoCount: Int = 0,
    val tagCount: Int = 0,
    val locatedRecordCount: Int = 0,
    val latestRecord: MemoryRecord? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.memoryRepository(application)

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeAllRecords(),
        repository.observePhotoCount(),
        repository.observeAllTags()
    ) { records, photoCount, tags ->
        HomeUiState(
            recordCount = records.size,
            photoCount = photoCount,
            tagCount = tags.size,
            locatedRecordCount = records.count { it.latitude != null && it.longitude != null },
            latestRecord = records.firstOrNull()
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
        }
    }
}
