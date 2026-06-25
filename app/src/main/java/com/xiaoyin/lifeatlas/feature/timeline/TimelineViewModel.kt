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

private data class TimelineFilterState(
    val query: String,
    val category: String,
    val showFilters: Boolean
)

data class TimelineUiState(
    val records: List<MemoryRecord> = emptyList(),
    val firstPhotosByRecordId: Map<Long, Photo> = emptyMap(),
    val tags: List<Tag> = emptyList(),
    val selectedTagId: Long? = null,
    val searchQuery: String = "",
    val selectedCategory: String = "全部",
    val showCategoryFilters: Boolean = true
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.memoryRepository(application)
    private val selectedTagId = MutableStateFlow<Long?>(null)
    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow("全部")
    private val showCategoryFilters = MutableStateFlow(true)
    private val filterState = combine(
        searchQuery,
        selectedCategory,
        showCategoryFilters
    ) { query, category, filtersVisible ->
        TimelineFilterState(query, category, filtersVisible)
    }
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
        selectedTagId,
        filterState
    ) { records, firstPhotos, tags, activeTagId, filters ->
        TimelineUiState(
            records = records.filterByCategory(filters.category).filterByQuery(filters.query),
            firstPhotosByRecordId = firstPhotos,
            tags = tags,
            selectedTagId = activeTagId,
            searchQuery = filters.query,
            selectedCategory = filters.category,
            showCategoryFilters = filters.showFilters
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

    fun selectCategory(category: String) {
        selectedCategory.update { category }
    }

    fun toggleCategoryFilters() {
        showCategoryFilters.update { !it }
    }

    fun onSearchQueryChange(value: String) {
        searchQuery.update { value }
    }

    fun clearSearchQuery() {
        searchQuery.update { "" }
    }
}

private fun List<MemoryRecord>.filterByCategory(category: String): List<MemoryRecord> {
    if (category == "全部") return this
    return filter { record ->
        listOf(
            record.title,
            record.content,
            record.locationName.orEmpty(),
            record.mood.orEmpty()
        ).any { field -> field.contains(category, ignoreCase = true) }
    }
}

private fun List<MemoryRecord>.filterByQuery(query: String): List<MemoryRecord> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this

    return filter { record ->
        listOf(
            record.title,
            record.content,
            record.locationName.orEmpty(),
            record.mood.orEmpty()
        ).any { field -> field.contains(normalizedQuery, ignoreCase = true) }
    }
}
