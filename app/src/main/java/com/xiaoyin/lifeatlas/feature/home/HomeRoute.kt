package com.xiaoyin.lifeatlas.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaoyin.lifeatlas.R
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.core.time.formatDate
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessCoral
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessSky
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower
import com.xiaoyin.lifeatlas.feature.record.ImportanceStars
import kotlinx.coroutines.delay

@Composable
fun HomeRoute(
    onAddClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onRecordClick: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val carouselPhotos = uiState.recentRecords
        .mapNotNull { record -> uiState.firstPhotosByRecordId[record.id] }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HomeHeader()
        PhotoCarousel(photos = carouselPhotos)
        StatsRow(
            recordCount = uiState.recordCount,
            locationCount = uiState.locatedRecordCount,
            photoCount = uiState.photoCount,
            tagCount = uiState.tagCount
        )
        NewMemoryButton(onClick = onAddClick)
        RecentMemories(
            records = uiState.recentRecords,
            firstPhotosByRecordId = uiState.firstPhotosByRecordId,
            onRecordClick = onRecordClick,
            onViewAllClick = onViewAllClick,
            onDeleteRecord = viewModel::deleteRecord
        )
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "岁迹",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = WildernessTeal
            )
            Text(
                text = "人生是旷野，今天从哪里出发？",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = WildernessTeal.copy(alpha = 0.78f)
            )
        }
        Spacer(modifier = Modifier.size(30.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoCarousel(photos: List<Photo>) {
    val pageCount = if (photos.isEmpty()) 1 else photos.size
    val pagerState = rememberPagerState(pageCount = { pageCount })

    LaunchedEffect(pageCount) {
        if (pageCount <= 1) return@LaunchedEffect
        while (true) {
            delay(3200)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % pageCount)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(186.dp)
            .clip(RoundedCornerShape(22.dp))
    ) { page ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (photos.isEmpty()) {
                Image(
                    painter = painterResource(id = R.drawable.wilderness_home_hero),
                    contentDescription = "旷野照片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = photos[page].displayUri,
                    contentDescription = "记忆照片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun StatsRow(recordCount: Int, locationCount: Int, photoCount: Int, tagCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(Icons.Outlined.Article, "记录", recordCount.toString(), WildernessMeadow, Modifier.weight(1f))
        StatCard(Icons.Outlined.Place, "地点", locationCount.toString(), WildernessMeadow, Modifier.weight(1f))
        StatCard(Icons.Outlined.PhotoCamera, "照片", photoCount.toString(), WildernessSky, Modifier.weight(1f))
        StatCard(Icons.Outlined.LocalOffer, "标签", tagCount.toString(), WildernessWildflower, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
                Text(text = label, style = MaterialTheme.typography.labelLarge, color = WildernessTeal.copy(alpha = 0.7f))
            }
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = WildernessTeal)
            Text(text = if (label == "照片") "张" else if (label == "地点") "个" else if (label == "标签") "个" else "条", style = MaterialTheme.typography.labelMedium, color = WildernessTeal.copy(alpha = 0.68f))
        }
    }
}

@Composable
private fun NewMemoryButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(WildernessTeal)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, tint = WildernessPaper, modifier = Modifier.size(24.dp))
            Text(
                text = "写一段新记忆",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = WildernessPaper
            )
        }
    }
}

@Composable
private fun RecentMemories(
    records: List<MemoryRecord>,
    firstPhotosByRecordId: Map<Long, Photo>,
    onRecordClick: (Long) -> Unit,
    onViewAllClick: () -> Unit,
    onDeleteRecord: (Long) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<MemoryRecord?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "最近记忆", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = WildernessTeal)
            Text(
                text = "查看全部 >",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = WildernessTeal.copy(alpha = 0.56f),
                modifier = Modifier.clickable(onClick = onViewAllClick)
            )
        }
        if (records.isEmpty()) {
            EmptyRecentCard()
        } else {
            records.take(2).forEach { record ->
                RecentMemoryCard(
                    record = record,
                    photo = firstPhotosByRecordId[record.id],
                    onClick = { onRecordClick(record.id) },
                    onLongPress = { pendingDelete = record }
                )
            }
        }
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除最近记忆") },
            text = { Text("确定把「${record.title}」移入回收站吗？之后仍可在设置中恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDeleteRecord(record.id)
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentMemoryCard(record: MemoryRecord, photo: Photo?, onClick: () -> Unit, onLongPress: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (photo == null) {
                Image(
                    painter = painterResource(id = R.drawable.wilderness_home_hero),
                    contentDescription = "默认风景",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 112.dp, height = 100.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                AsyncImage(
                    model = photo.displayUri,
                    contentDescription = "记录照片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 112.dp, height = 100.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = WildernessTeal,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null, tint = WildernessTeal.copy(alpha = 0.54f), modifier = Modifier.size(20.dp))
                }
                Text(
                    text = record.locationName ?: "未填写地点",
                    style = MaterialTheme.typography.bodySmall,
                    color = WildernessTeal.copy(alpha = 0.62f)
                )
                Text(
                    text = record.recordTime.formatDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = WildernessTeal.copy(alpha = 0.58f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoodChip(record.mood ?: "开心")
                    ImportanceStars(value = record.importance.toFloat(), starSize = 15.dp)
                }
            }
        }
    }
}

@Composable
private fun MoodChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(WildernessWildflower.copy(alpha = 0.38f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Outlined.SentimentSatisfied, contentDescription = null, tint = WildernessWildflower, modifier = Modifier.size(14.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = WildernessTeal)
        }
    }
}

@Composable
private fun EmptyRecentCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper)
    ) {
        Text(
            text = "还没有留下第一段记忆。写下它，旷野就有了第一枚坐标。",
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = WildernessTeal.copy(alpha = 0.72f)
        )
    }
}
