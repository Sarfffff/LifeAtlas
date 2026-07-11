package com.xiaoyin.lifeatlas.feature.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.data.repository.RepositoryProvider
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AnnualReviewUiState(
    val availableYears: List<Int> = emptyList(),
    val selectedYear: Int = java.time.Year.now().value,
    val records: List<MemoryRecord> = emptyList(),
    val photoCount: Int = 0,
    val placeCount: Int = 0,
    val favoriteMood: String? = null,
    val mostImportantRecord: MemoryRecord? = null,
    val coverPhoto: Photo? = null,
    val firstMemoryDate: Long? = null,
    val journeyDays: Long = 0
)

class AnnualReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepositoryProvider.memoryRepository(application)
    private val selectedYear = MutableStateFlow(java.time.Year.now().value)
    private val zoneId = ZoneId.systemDefault()

    val uiState: StateFlow<AnnualReviewUiState> = combine(
        repository.observeAllRecords(),
        repository.observeFirstPhotosByRecord(),
        repository.observeAllPhotos(),
        selectedYear
    ) { allRecords, firstPhotos, allPhotos, year ->
        val availableYears = allRecords
            .map { it.recordTime.year() }
            .distinct()
            .sortedDescending()
        val effectiveYear = year.takeIf { it in availableYears }
            ?: availableYears.firstOrNull()
            ?: year
        val records = allRecords.filter { it.recordTime.year() == effectiveYear }
        val recordIds = records.mapTo(mutableSetOf()) { it.id }
        val firstDate = records.minOfOrNull { it.recordTime }
        val lastDate = records.maxOfOrNull { it.recordTime }
        val important = records.maxWithOrNull(
            compareBy<MemoryRecord> { it.importance }.thenBy { it.recordTime }
        )
        AnnualReviewUiState(
            availableYears = availableYears,
            selectedYear = effectiveYear,
            records = records,
            photoCount = allPhotos.count { it.recordId in recordIds },
            placeCount = records.mapNotNull { it.locationName?.trim()?.takeIf(String::isNotEmpty) }.distinct().size,
            favoriteMood = records.mapNotNull { it.mood?.trim()?.takeIf(String::isNotEmpty) }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key,
            mostImportantRecord = important,
            coverPhoto = important?.let { firstPhotos[it.id] }
                ?: records.firstNotNullOfOrNull { firstPhotos[it.id] },
            firstMemoryDate = firstDate,
            journeyDays = if (firstDate != null && lastDate != null) {
                ChronoUnit.DAYS.between(
                    Instant.ofEpochMilli(firstDate).atZone(zoneId).toLocalDate(),
                    Instant.ofEpochMilli(lastDate).atZone(zoneId).toLocalDate()
                ) + 1
            } else {
                0
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnnualReviewUiState()
    )

    fun selectYear(year: Int) {
        selectedYear.value = year
    }

    private fun Long.year(): Int = Instant.ofEpochMilli(this).atZone(zoneId).year
}
