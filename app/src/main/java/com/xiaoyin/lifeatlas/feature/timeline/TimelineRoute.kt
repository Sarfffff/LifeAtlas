package com.xiaoyin.lifeatlas.feature.timeline

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaoyin.lifeatlas.R
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessCoral
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessSky
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower
import com.xiaoyin.lifeatlas.feature.record.ImportanceStars
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class TimelineMonthGroup(
    val monthNumber: String,
    val year: String,
    val records: List<MemoryRecord>
)

@Composable
fun TimelineRoute(
    onRecordClick: (Long) -> Unit,
    viewModel: TimelineViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val groups = uiState.records.groupBy { it.recordTime.monthKey() }
        .map { (key, monthRecords) ->
            val parts = key.split("-")
            TimelineMonthGroup(
                year = parts.getOrNull(0).orEmpty(),
                monthNumber = parts.getOrNull(1)?.trimStart('0').orEmpty(),
                records = monthRecords
            )
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TimelineHeader(onFilterClick = viewModel::toggleCategoryFilters)
        TimelineSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            onClear = viewModel::clearSearchQuery
        )
        if (uiState.showCategoryFilters) {
            TimelineCategoryChips(
                selectedCategory = uiState.selectedCategory,
                favoriteOnly = uiState.favoriteOnly,
                onCategoryClick = viewModel::selectCategory,
                onFavoriteClick = viewModel::toggleFavoriteOnly
            )
        }
        TimelineSearchResultHint(uiState = uiState)

        if (uiState.records.isEmpty()) {
            EmptyTrail(text = uiState.emptyStateText())
        } else {
            groups.forEachIndexed { index, group ->
                TimelineMonthSection(
                    group = group,
                    isLast = index == groups.lastIndex,
                    firstPhotosByRecordId = uiState.firstPhotosByRecordId,
                    favoriteRecordIds = uiState.favoriteRecordIds,
                    onFavoriteClick = viewModel::setFavorite,
                    onRecordClick = onRecordClick
                )
            }
        }
    }
}

@Composable
private fun TimelineHeader(onFilterClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "时间轴",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = WildernessTeal
        )
        IconButton(onClick = onFilterClick) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = "筛选",
                tint = WildernessTeal,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun TimelineSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = WildernessTeal.copy(alpha = 0.55f))
            },
            placeholder = { Text("搜索记忆、地点或标签") },
            singleLine = true
        )
        if (query.isNotBlank()) {
            TextButton(onClick = onClear) {
                Text("清空")
            }
        }
    }
}

@Composable
private fun TimelineCategoryChips(
    selectedCategory: String,
    favoriteOnly: Boolean,
    onCategoryClick: (String) -> Unit,
    onFavoriteClick: () -> Unit
) {
    val chips = listOf(
        "全部" to WildernessMeadow,
        "旅行" to WildernessSky.copy(alpha = 0.72f),
        "日常" to WildernessWildflower.copy(alpha = 0.72f),
        "工作" to Color(0xFFD9C8EF),
        "生活" to WildernessCoral.copy(alpha = 0.35f)
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(chips.size + 1) { index ->
            if (index == chips.size) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (favoriteOnly) WildernessTeal else WildernessPaper)
                        .clickable(onClick = onFavoriteClick)
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Bookmark,
                            contentDescription = null,
                            tint = if (favoriteOnly) WildernessPaper else WildernessTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "收藏",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = if (favoriteOnly) WildernessPaper else WildernessTeal
                        )
                    }
                }
                return@items
            }
            val (label, color) = chips[index]
            val selected = label == selectedCategory
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) WildernessTeal else color)
                    .clickable { onCategoryClick(label) }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if (selected) WildernessPaper else WildernessTeal
                )
            }
        }
    }
}

@Composable
private fun TimelineSearchResultHint(uiState: TimelineUiState) {
    if (uiState.searchQuery.isBlank() && uiState.selectedCategory == "全部" && !uiState.favoriteOnly) return

    Text(
        text = if (uiState.favoriteOnly) {
            "收藏夹中找到 ${uiState.records.size} 条记录"
        } else {
            "找到 ${uiState.records.size} 条匹配记录"
        },
        style = MaterialTheme.typography.bodySmall,
        color = WildernessTeal.copy(alpha = 0.62f)
    )
}

@Composable
private fun TimelineMonthSection(
    group: TimelineMonthGroup,
    isLast: Boolean,
    firstPhotosByRecordId: Map<Long, Photo>,
    favoriteRecordIds: Set<Long>,
    onFavoriteClick: (Long, Boolean) -> Unit,
    onRecordClick: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MonthRail(
            month = group.monthNumber,
            year = group.year,
            isLast = isLast,
            height = (group.records.size * 172).coerceAtLeast(172).dp
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            group.records.forEach { record ->
                TimelineRecordCard(
                    record = record,
                    photo = firstPhotosByRecordId[record.id],
                    isFavorite = record.id in favoriteRecordIds,
                    onFavoriteClick = {
                        onFavoriteClick(record.id, record.id !in favoriteRecordIds)
                    },
                    onClick = { onRecordClick(record.id) }
                )
            }
        }
    }
}

@Composable
private fun MonthRail(month: String, year: String, isLast: Boolean, height: Dp) {
    Row(modifier = Modifier.width(64.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${month}月", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WildernessTeal)
            Text(text = year, style = MaterialTheme.typography.bodySmall, color = WildernessTeal.copy(alpha = 0.58f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(18.dp)
                    .background(WildernessWildflower, RoundedCornerShape(9.dp))
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .height(height)
                        .width(2.dp)
                        .background(WildernessMeadow.copy(alpha = 0.85f), RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

@Composable
private fun TimelineRecordCard(
    record: MemoryRecord,
    photo: Photo?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimelinePhoto(photo = photo)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = WildernessTeal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isFavorite) "取消收藏" else "收藏",
                            tint = if (isFavorite) WildernessWildflower else WildernessTeal.copy(alpha = 0.54f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                if (isFavorite) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Bookmark, contentDescription = null, tint = WildernessWildflower, modifier = Modifier.size(14.dp))
                        Text(
                            text = "已收藏",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = WildernessTeal.copy(alpha = 0.72f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.LocationOn, contentDescription = null, tint = WildernessTeal.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                    Text(
                        text = record.locationName ?: "未填写地点",
                        style = MaterialTheme.typography.bodySmall,
                        color = WildernessTeal.copy(alpha = 0.66f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = record.content.toTimelineSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = WildernessTeal.copy(alpha = 0.68f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoodChip(record.mood ?: "平静")
                    ImportanceStars(value = record.importance.toFloat(), starSize = 14.dp)
                }
            }
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = WildernessTeal.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .offset(x = 2.dp)
                    .size(24.dp)
            )
        }
    }
}

@Composable
private fun TimelinePhoto(photo: Photo?) {
    if (photo == null) {
        Image(
            painter = painterResource(id = R.drawable.wilderness_home_hero),
            contentDescription = "默认照片",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 100.dp, height = 130.dp)
                .clip(RoundedCornerShape(14.dp))
        )
    } else {
        AsyncImage(
            model = photo.displayUri,
            contentDescription = "记录照片",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 100.dp, height = 130.dp)
                .clip(RoundedCornerShape(14.dp))
        )
    }
}

@Composable
private fun MoodChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(WildernessWildflower.copy(alpha = 0.38f))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Outlined.SentimentSatisfied, contentDescription = null, tint = WildernessWildflower, modifier = Modifier.size(14.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = WildernessTeal)
        }
    }
}

@Composable
private fun EmptyTrail(text: String) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = WildernessPaper)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "这里还很安静", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WildernessTeal)
            Text(text = text, style = MaterialTheme.typography.bodyLarge, color = WildernessTeal.copy(alpha = 0.7f))
        }
    }
}

private fun String.toTimelineSummary(maxLength: Int = 34): String {
    val normalized = trim().replace(Regex("\\s+"), " ")
    if (normalized.isBlank()) return "暂无正文"
    return if (normalized.length <= maxLength) normalized else "${normalized.take(maxLength)}..."
}

private fun Long.monthKey(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM"))
}

private fun TimelineUiState.emptyStateText(): String {
    if (favoriteOnly) return "收藏夹里还没有记忆。去地图页收藏一段想反复回看的坐标。"
    if (searchQuery.isNotBlank() && selectedTagId != null) return "当前标签下没有匹配的记录"
    if (searchQuery.isNotBlank()) return "没有匹配的记录"
    if (selectedTagId != null) return "当前标签下暂无记录"
    if (selectedCategory != "全部") return "当前分类下暂无记录"
    return "还没有走到这里"
}
