package com.xiaoyin.lifeatlas.feature.record

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyin.lifeatlas.core.time.formatDate
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordRoute(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "编辑记录", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        if (uiState.isLoading) {
            Text(text = "正在加载记录", style = MaterialTheme.typography.bodyLarge)
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
                Text("管理照片（${uiState.photoUris.size}）")
            }
            if (uiState.photoUris.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.photoUris.forEach { uri ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "记录照片",
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.removePhoto(uri) }) {
                                Text("移除")
                            }
                        }
                    }
                }
            }
            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = viewModel::saveRecord,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaving) "保存中" else "保存修改")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("取消")
            }
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
