package com.xiaoyin.lifeatlas.feature.city

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.data.repository.RepositoryProvider
import com.xiaoyin.lifeatlas.navigation.LifeAtlasDestination
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CityDetailUiState(
    val cityName: String = "",
    val records: List<MemoryRecord> = emptyList(),
    val firstPhotosByRecordId: Map<Long, Photo> = emptyMap()
)

class CityDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.memoryRepository(application.applicationContext)
    private val cityName = savedStateHandle
        .get<String>(LifeAtlasDestination.CityDetail.cityArg)
        ?.decodeRouteValue()
        .orEmpty()

    val uiState: StateFlow<CityDetailUiState> = combine(
        repository.observeLocatedRecords(),
        repository.observeFirstPhotosByRecord()
    ) { records, firstPhotos ->
        CityDetailUiState(
            cityName = cityName,
            records = records
                .filter { it.cityKey() == cityName }
                .sortedByDescending { it.recordTime },
            firstPhotosByRecordId = firstPhotos
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CityDetailUiState(cityName = cityName)
    )
}

private fun String.decodeRouteValue(): String {
    return URLDecoder.decode(this, StandardCharsets.UTF_8.name())
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
