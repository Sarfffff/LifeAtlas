package com.xiaoyin.lifeatlas.feature.timeline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.core.model.Tag
import com.xiaoyin.lifeatlas.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimelineUiState(
    val records: List<MemoryRecord> = emptyList(),
    val firstPhotosByRecordId: Map<Long, Photo> = emptyMap(),
    val tags: List<Tag> = emptyList(),
    val selectedTagId: Long? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.memoryRepository(application)
    private val selectedTagId = MutableStateFlow<Long?>(null)
    private val recordsFlow = selectedTagId.flatMapLatest { tagId ->
        if (tagId == null) {
            repository.observeAllRecords()
        } else {
            repository.observeRecordsByTag(tagId)
        }
    }

    val uiState: StateFlow<TimelineUiState> = combine(
        recordsFlow,
        repository.observeFirstPhotosByRecord(),
        repository.observeAllTags(),
        selectedTagId
    ) { records, firstPhotos, tags, activeTagId ->
        TimelineUiState(
            records = records,
            firstPhotosByRecordId = firstPhotos,
            tags = tags,
            selectedTagId = activeTagId
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimelineUiState()
        )

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
        }
    }

    fun selectTag(tagId: Long?) {
        selectedTagId.update { tagId }
    }
}
