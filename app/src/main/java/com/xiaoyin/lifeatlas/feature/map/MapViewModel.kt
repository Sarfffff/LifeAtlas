package com.xiaoyin.lifeatlas.feature.map

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

data class MapUiState(
    val locatedRecords: List<MemoryRecord> = emptyList(),
    val firstPhotosByRecordId: Map<Long, Photo> = emptyMap(),
    val favoriteRecordIds: Set<Long> = emptySet(),
    val litCities: List<String> = emptyList()
)

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.memoryRepository(application)

    val uiState: StateFlow<MapUiState> = combine(
        repository.observeLocatedRecords(),
        repository.observeFirstPhotosByRecord(),
        repository.observeFavoriteRecordIds()
    ) { records, firstPhotos, favoriteRecordIds ->
        MapUiState(
            locatedRecords = records,
            firstPhotosByRecordId = firstPhotos,
            favoriteRecordIds = favoriteRecordIds,
            litCities = records.map { it.cityKey() }.distinct()
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MapUiState()
        )

    fun setFavorite(recordId: Long, favorite: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(recordId, favorite)
        }
    }
}

private fun MemoryRecord.cityKey(): String {
    val source = locationName?.trim().orEmpty()
    if (source.isBlank()) return "未知坐标"
    val cityMatch = Regex("""[\u4e00-\u9fa5A-Za-z0-9]+市""").find(source)?.value
    if (!cityMatch.isNullOrBlank()) return cityMatch
    return source
        .split(" ", "·", "-", "|", "，", ",")
        .firstOrNull { it.isNotBlank() }
        ?.take(8)
        ?: source.take(8)
}
