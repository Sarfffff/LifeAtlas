package com.xiaoyin.lifeatlas.feature.favorites

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaoyin.lifeatlas.R
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessCream
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMuted
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun FavoriteCenterRoute(
    onBack: () -> Unit,
    onRecordClick: (Long) -> Unit,
    viewModel: FavoriteCenterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WildernessCream)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FavoriteHeader(count = uiState.records.size, onBack = onBack)

        if (uiState.records.isEmpty()) {
            EmptyFavoritesCard()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.records, key = { it.id }) { record ->
                    FavoriteRecordCard(
                        record = record,
                        photo = uiState.firstPhotosByRecordId[record.id],
                        onClick = { onRecordClick(record.id) },
                        onRemoveClick = { viewModel.removeFavorite(record.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun FavoriteHeader(count: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "返回",
                tint = WildernessTeal,
                modifier = Modifier.size(30.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "收藏中心",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = WildernessTeal
            )
            Text(
                text = "集中回看 $count 段想反复抵达的记忆",
                style = MaterialTheme.typography.bodyLarge,
                color = WildernessMuted
            )
        }
    }
}

@Composable
private fun EmptyFavoritesCard() {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Bookmark,
                contentDescription = null,
                tint = WildernessWildflower,
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = "还没有收藏的记忆",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = WildernessTeal
            )
            Text(
                text = "在地图页打开某一段记忆，点收藏后会出现在这里。",
                style = MaterialTheme.typography.bodyLarge,
                color = WildernessMuted
            )
        }
    }
}

@Composable
private fun FavoriteRecordCard(
    record: MemoryRecord,
    photo: Photo?,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                FavoritePhoto(photo = photo)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = WildernessTeal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = WildernessTeal.copy(alpha = 0.58f),
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = record.locationName ?: "未填写地点",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WildernessMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = record.recordTime.formatDate(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WildernessMuted
                    )
                    MoodChip(record.mood ?: "平静")
                }
            }
            Text(
                text = record.content.toFavoriteSummary(),
                style = MaterialTheme.typography.bodyLarge,
                color = WildernessTeal.copy(alpha = 0.74f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已收藏",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = WildernessTeal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(WildernessWildflower.copy(alpha = 0.34f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                OutlinedButton(onClick = onRemoveClick) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = WildernessTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("取消收藏", color = WildernessTeal, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun FavoritePhoto(photo: Photo?) {
    val modifier = Modifier
        .size(width = 112.dp, height = 128.dp)
        .clip(RoundedCornerShape(18.dp))

    if (photo == null) {
        Image(
            painter = painterResource(id = R.drawable.wilderness_home_hero),
            contentDescription = "默认照片",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = photo.displayUri,
            contentDescription = "记录照片",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
private fun MoodChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(WildernessWildflower.copy(alpha = 0.34f))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.SentimentSatisfied,
                contentDescription = null,
                tint = WildernessWildflower,
                modifier = Modifier.size(15.dp)
            )
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = WildernessTeal)
        }
    }
}

private fun Long.formatDate(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
}

private fun String.toFavoriteSummary(maxLength: Int = 58): String {
    val normalized = trim().replace(Regex("\\s+"), " ")
    if (normalized.isBlank()) return "暂无正文"
    return if (normalized.length <= maxLength) normalized else "${normalized.take(maxLength)}..."
}
