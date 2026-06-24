package com.xiaoyin.lifeatlas.feature.record

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.xiaoyin.lifeatlas.core.time.formatDate
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessSky
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordRoute(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onMapPickerClick: (Double?, Double?) -> Unit,
    pickedLatitude: Double? = null,
    pickedLongitude: Double? = null,
    pickedAddress: String? = null,
    onMapPickerResultHandled: () -> Unit = {},
    viewModel: EditRecordViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.recordTime)
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        viewModel.onPhotosSelected(uris.map { it.toString() })
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            viewModel.onSavedHandled()
            onSaved(uiState.recordId)
        }
    }

    LaunchedEffect(pickedLatitude, pickedLongitude, pickedAddress) {
        if (pickedLatitude != null && pickedLongitude != null) {
            viewModel.onMapPointSelected(pickedLatitude, pickedLongitude, pickedAddress)
            onMapPickerResultHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "整理记忆",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = WildernessTeal
            )
            OutlinedButton(onClick = onBack) {
                Text("返回")
            }
        }
        Text(
            text = "补上新的照片、地点和心情，让这段旅程更完整。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
        )

        if (uiState.isLoading) {
            EmptyEditCard(text = "正在载入这段记忆")
        } else {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标题") },
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.content,
                onValueChange = viewModel::onContentChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("正文") },
                minLines = 4
            )
            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("发生日期：${uiState.recordTime.formatDate()}")
            }
            OutlinedTextField(
                value = uiState.locationName,
                onValueChange = viewModel::onLocationNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("地点") },
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.latitudeText,
                    onValueChange = viewModel::onLatitudeChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("纬度") },
                    placeholder = { Text("30.5") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.longitudeText,
                    onValueChange = viewModel::onLongitudeChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("经度") },
                    placeholder = { Text("114.3") },
                    singleLine = true
                )
            }
            OutlinedButton(
                onClick = {
                    onMapPickerClick(
                        uiState.latitudeText.toValidLatitudeOrNull(),
                        uiState.longitudeText.toValidLongitudeOrNull()
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("地图选点 / 使用定位")
            }
            OutlinedTextField(
                value = uiState.mood,
                onValueChange = viewModel::onMoodChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("心情") },
                singleLine = true
            )
            Text(text = "重要程度：${uiState.importance.toInt()}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = uiState.importance,
                onValueChange = viewModel::onImportanceChange,
                valueRange = 1f..5f,
                steps = 3
            )
            OutlinedTextField(
                value = uiState.tagsText,
                onValueChange = viewModel::onTagsTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标签") },
                placeholder = { Text("用逗号分隔，例如：旅行，家人") },
                singleLine = true
            )
            Button(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("管理照片：${uiState.photoUris.size} 张")
            }
            if (uiState.photoUris.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.photoUris.forEach { uri ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = WildernessPaper)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "记录照片",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(96.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(WildernessSky.copy(alpha = 0.35f))
                                )
                                TextButton(onClick = { viewModel.removePhoto(uri) }) {
                                    Text("移除")
                                }
                            }
                        }
                    }
                }
            } else {
                EmptyEditCard(text = "这条记录还没有照片，可以先保存文字，之后再补。")
            }
            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            uiState.infoMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(
                onClick = viewModel::saveRecord,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        uiState.isSaving -> "保存中"
                        uiState.title.isBlank() -> "填写标题后保存"
                        else -> "确认保存修改"
                    }
                )
            }
            Text(
                text = "如果按钮不可点击，请先填写标题。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(viewModel::onRecordTimeChange)
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun EmptyEditCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
        )
    }
}

private fun String.toValidLatitudeOrNull(): Double? {
    return trim().toDoubleOrNull()?.takeIf { it in -90.0..90.0 }
}

private fun String.toValidLongitudeOrNull(): Double? {
    return trim().toDoubleOrNull()?.takeIf { it in -180.0..180.0 }
}
