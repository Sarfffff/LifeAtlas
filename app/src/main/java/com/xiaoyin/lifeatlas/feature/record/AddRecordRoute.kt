package com.xiaoyin.lifeatlas.feature.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AddRecordRoute(
    onRecordSaved: () -> Unit,
    viewModel: AddRecordViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.savedRecordId) {
        if (uiState.savedRecordId != null) {
            viewModel.onSavedHandled()
            onRecordSaved()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "新增记录", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
        OutlinedTextField(
            value = uiState.locationName,
            onValueChange = viewModel::onLocationNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("地点") },
            singleLine = true
        )
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
            Text(if (uiState.isSaving) "保存中" else "保存")
        }
    }
}
