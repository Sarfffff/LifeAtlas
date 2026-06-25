package com.xiaoyin.lifeatlas.feature.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoriteCenterUiState(
    val records: List<MemoryRecord> = emptyList(),
    val firstPhotosByRecordId: Map<Long, Photo> = emptyMap()
)

class FavoriteCenterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.memoryRepository(application.applicationContext)

    val uiState: StateFlow<FavoriteCenterUiState> = combine(
        repository.observeFavoriteRecords(),
        repository.observeFirstPhotosByRecord()
    ) { records, firstPhotos ->
        FavoriteCenterUiState(
            records = records,
            firstPhotosByRecordId = firstPhotos
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FavoriteCenterUiState()
    )

    fun removeFavorite(recordId: Long) {
        viewModelScope.launch {
            repository.setFavorite(recordId, false)
        }
    }
}
