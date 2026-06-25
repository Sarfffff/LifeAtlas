package com.xiaoyin.lifeatlas.feature.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaoyin.lifeatlas.R
import com.xiaoyin.lifeatlas.core.map.MapSdkConfig
import com.xiaoyin.lifeatlas.core.model.MemoryRecord
import com.xiaoyin.lifeatlas.core.model.Photo
import com.xiaoyin.lifeatlas.core.time.formatDate
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessCoral
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessCream
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMuted
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessSky
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower
import kotlinx.coroutines.delay

@Composable
fun MapRoute(
    onRecordClick: (Long) -> Unit,
    viewModel: MapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val records = uiState.locatedRecords
    var selectedIndex by remember(records) { mutableIntStateOf(0) }
    var userSelected by remember(records) { mutableStateOf(false) }

    LaunchedEffect(records, userSelected) {
        if (records.isEmpty() || userSelected) return@LaunchedEffect
        while (true) {
            delay(3_200)
            selectedIndex = (selectedIndex + 1) % records.size
        }
    }

    val selectedRecord = records.getOrNull(selectedIndex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WildernessCream)
    ) {
        LifeMapCanvas(
            records = records,
            selectedIndex = selectedIndex,
            onMarkerClick = { index ->
                selectedIndex = index
                userSelected = true
            },
            modifier = Modifier.fillMaxSize()
        )

        MapTopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(start = 18.dp, top = 18.dp, end = 18.dp)
        )

        MapStatusPills(
            count = records.size,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 104.dp, start = 18.dp, end = 18.dp)
        )

        MapLocateButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp, bottom = 54.dp)
        )

        if (selectedRecord == null) {
            EmptyMapMemoryCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
            )
        } else {
            MapMemorySheet(
                record = selectedRecord,
                photo = uiState.firstPhotosByRecordId[selectedRecord.id],
                onDetailClick = { onRecordClick(selectedRecord.id) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
            )
        }
    }
}

@Composable
private fun LifeMapCanvas(
    records: List<MemoryRecord>,
    selectedIndex: Int,
    onMarkerClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val animatedScale = remember { Animatable(1f) }

    LaunchedEffect(scale) {
        animatedScale.animateTo(scale, animationSpec = tween(160))
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 2.6f)
        offset = Offset(
            x = (offset.x + panChange.x).coerceIn(-260f, 260f),
            y = (offset.y + panChange.y).coerceIn(-360f, 240f)
        )
    }

    Box(modifier = modifier.clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedScale.value
                    scaleY = animatedScale.value
                    translationX = offset.x
                    translationY = offset.y
                }
                .transformable(transformableState)
        ) {
            Image(
                painter = painterResource(id = R.drawable.wilderness_map_panel),
                contentDescription = "人生地图",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            records.take(6).forEachIndexed { index, record ->
                LightMarker(
                    index = index,
                    title = record.locationName ?: record.title,
                    selected = index == selectedIndex,
                    onClick = { onMarkerClick(index) },
                    modifier = Modifier
                        .align(markerAlignment(index))
                        .offset(x = markerOffsetX(index), y = markerOffsetY(index))
                )
            }
        }

        MapZoomControls(
            scale = scale,
            onZoomIn = { scale = (scale + 0.24f).coerceAtMost(2.6f) },
            onZoomOut = {
                scale = (scale - 0.24f).coerceAtLeast(1f)
                if (scale == 1f) offset = Offset.Zero
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp, top = 70.dp)
        )
    }
}

@Composable
private fun MapTopBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "‹", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Light, color = WildernessTeal)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "人生地图",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = WildernessTeal
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Search, contentDescription = "搜索", tint = WildernessTeal, modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Layers, contentDescription = "图层", tint = WildernessTeal, modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun MapStatusPills(count: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MapPill(label = "坐标", value = "$count", modifier = Modifier.weight(0.9f))
        MapPill(label = "地图", value = "人生地图", modifier = Modifier.weight(1.2f))
        MapPill(label = "Key", value = MapSdkConfig.statusText, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MapPill(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(WildernessMeadow.copy(alpha = 0.82f))
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = WildernessTeal.copy(alpha = 0.65f))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = WildernessTeal)
    }
}

@Composable
private fun LightMarker(
    index: Int,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = markerColor(index)
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 78.dp else 64.dp)
                .shadow(if (selected) 18.dp else 8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f))
                .border(if (selected) 5.dp else 4.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(if (selected) 48.dp else 40.dp)
            )
        }
        if (selected) {
            Text(
                text = "已点亮",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = WildernessTeal,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(WildernessPaper.copy(alpha = 0.88f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun MapZoomControls(
    scale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.76f))
            .border(1.dp, WildernessTeal.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "+",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = WildernessTeal,
            modifier = Modifier
                .clickable(onClick = onZoomIn)
                .padding(horizontal = 18.dp, vertical = 4.dp)
        )
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(1.dp)
                .background(WildernessTeal.copy(alpha = 0.16f))
        )
        Text(
            text = "-",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = if (scale > 1f) WildernessTeal else WildernessMuted,
            modifier = Modifier
                .clickable(onClick = onZoomOut)
                .padding(horizontal = 20.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MapLocateButton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(52.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(WildernessPaper),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.MyLocation, contentDescription = "定位", tint = WildernessTeal, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun MapMemorySheet(
    record: MemoryRecord,
    photo: Photo?,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper.copy(alpha = 0.98f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(48.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(WildernessMuted.copy(alpha = 0.36f))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Top) {
                MapRecordPhoto(photo = photo)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = WildernessTeal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = WildernessMuted, modifier = Modifier.size(16.dp))
                                Text(
                                    text = record.locationName ?: "已点亮坐标",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WildernessMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = WildernessTeal.copy(alpha = 0.75f))
                    }
                    Text(text = record.dateText(), style = MaterialTheme.typography.bodyMedium, color = WildernessMuted)
                    Text(
                        text = record.content.ifBlank { "这处坐标已经被你的记忆点亮。" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = WildernessTeal.copy(alpha = 0.74f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    MoodChip(record.mood ?: "开心")
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WildernessTeal)
                ) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("收藏", fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WildernessTeal)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("分享", fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = onDetailClick,
                    modifier = Modifier.weight(1.35f).height(48.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WildernessTeal, contentColor = WildernessPaper)
                ) {
                    Text("查看详情", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun MapRecordPhoto(photo: Photo?) {
    val model = photo?.thumbnailPath ?: photo?.originalUri
    if (model == null) {
        Image(
            painter = painterResource(id = R.drawable.wilderness_home_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 104.dp, height = 104.dp)
                .clip(RoundedCornerShape(18.dp))
        )
    } else {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 104.dp, height = 104.dp)
                .clip(RoundedCornerShape(18.dp))
        )
    }
}

@Composable
private fun MoodChip(mood: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(WildernessWildflower.copy(alpha = 0.24f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "☺", color = WildernessTeal, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = mood, color = WildernessTeal, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyMapMemoryCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper.copy(alpha = 0.98f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("还没有点亮坐标", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WildernessTeal)
            Text(
                "新增或编辑记录时填写地点，地图就会出现属于你的第一枚标记。",
                style = MaterialTheme.typography.bodyLarge,
                color = WildernessTeal.copy(alpha = 0.7f)
            )
        }
    }
}

private fun markerColor(index: Int): Color {
    return listOf(WildernessTeal, WildernessCoral, Color(0xFF8D69B8), WildernessWildflower, WildernessSky, WildernessMeadow)[index % 6]
}

private fun markerAlignment(index: Int): Alignment {
    return listOf(
        Alignment.TopCenter,
        Alignment.CenterEnd,
        Alignment.CenterStart,
        Alignment.BottomCenter,
        Alignment.Center,
        Alignment.TopStart
    )[index % 6]
}

private fun markerOffsetX(index: Int): Dp {
    return listOf((-10).dp, (-54).dp, 76.dp, 28.dp, (-20).dp, 88.dp)[index % 6]
}

private fun markerOffsetY(index: Int): Dp {
    return listOf(168.dp, (-82).dp, 28.dp, (-226).dp, (-8).dp, 238.dp)[index % 6]
}

private fun MemoryRecord.dateText(): String {
    return recordTime.formatDate().replace("-", ".")
}
