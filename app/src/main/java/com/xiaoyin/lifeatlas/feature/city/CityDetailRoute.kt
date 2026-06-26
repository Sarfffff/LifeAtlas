package com.xiaoyin.lifeatlas.feature.city

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMuted
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CityDetailRoute(
    onBack: () -> Unit,
    onRecordClick: (Long) -> Unit,
    viewModel: CityDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WildernessCream)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CityHeader(
            cityName = uiState.cityName,
            count = uiState.records.size,
            onBack = onBack
        )

        if (uiState.records.isEmpty()) {
            EmptyCityCard(uiState.cityName)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(uiState.records, key = { it.id }) { record ->
                    CityRecordCard(
                        record = record,
                        photo = uiState.firstPhotosByRecordId[record.id],
                        onShareClick = {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "岁迹城市记忆：${record.title}")
                                        putExtra(Intent.EXTRA_TEXT, record.toShareText(uiState.cityName))
                                    },
                                    "分享这段城市记忆"
                                )
                            )
                        },
                        onClick = { onRecordClick(record.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun CityHeader(cityName: String, count: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = WildernessTeal, modifier = Modifier.size(30.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = cityName.ifBlank { "城市详情" },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = WildernessTeal
            )
            Text(
                text = "这里点亮了 $count 段记忆",
                style = MaterialTheme.typography.bodyLarge,
                color = WildernessMuted
            )
        }
    }
}

@Composable
private fun EmptyCityCard(cityName: String) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = WildernessWildflower, modifier = Modifier.size(36.dp))
            Text(
                text = "${cityName.ifBlank { "这里" }}还没有记录",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = WildernessTeal
            )
            Text(
                text = "新增记录时选择这个城市或地点，保存后会在这里汇总。",
                style = MaterialTheme.typography.bodyLarge,
                color = WildernessMuted
            )
        }
    }
}

@Composable
private fun CityRecordCard(
    record: MemoryRecord,
    photo: Photo?,
    onShareClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            CityPhoto(photo = photo)
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
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = WildernessTeal.copy(alpha = 0.58f), modifier = Modifier.size(17.dp))
                    Text(
                        text = record.locationName ?: "未填写地点",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WildernessMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(15.dp))
                        .background(WildernessMeadow.copy(alpha = 0.58f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Timeline, contentDescription = null, tint = WildernessTeal, modifier = Modifier.size(15.dp))
                    Text(record.recordTime.formatDate(), style = MaterialTheme.typography.labelMedium, color = WildernessTeal, fontWeight = FontWeight.Black)
                }
                Text(
                    text = record.content.toCitySummary(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WildernessTeal.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onShareClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = "分享",
                            tint = WildernessTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CityPhoto(photo: Photo?) {
    val modifier = Modifier
        .size(width = 104.dp, height = 122.dp)
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

private fun Long.formatDate(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
}

private fun String.toCitySummary(maxLength: Int = 46): String {
    val normalized = trim().replace(Regex("\\s+"), " ")
    if (normalized.isBlank()) return "暂无正文"
    return if (normalized.length <= maxLength) normalized else "${normalized.take(maxLength)}..."
}

private fun MemoryRecord.toShareText(cityName: String): String {
    val city = cityName.ifBlank { locationName.orEmpty() }.ifBlank { "未填写城市" }
    val place = locationName?.takeIf { it.isNotBlank() } ?: "未填写地点"
    val coordinate = if (latitude != null && longitude != null) {
        "\n坐标：$latitude, $longitude"
    } else {
        ""
    }
    return "我在岁迹点亮了 $city 的一段记忆：$title\n地点：$place\n时间：${recordTime.formatDate()}$coordinate\n\n${content.ifBlank { "这段记忆还没有正文。" }}"
}
