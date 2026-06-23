package com.xiaoyin.lifeatlas.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.xiaoyin.lifeatlas.core.ui.theme.AtlasMist

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = viewModel()) {
    var localFirst by remember { mutableStateOf(true) }
    val uiState by viewModel.uiState.collectAsState()
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.writeExport(uri)
        } else {
            viewModel.onExportLaunchHandled()
        }
    }

    LaunchedEffect(uiState.pendingExportJson) {
        if (uiState.pendingExportJson != null) {
            exportLauncher.launch("lifeatlas_export.json")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "设置", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        SettingCard(
            title = "本地优先",
            body = "第一版不强制登录，不自动上传个人记录。",
            trailing = {
                Switch(checked = localFirst, onCheckedChange = { localFirst = it })
            }
        )
        SettingCard(
            title = "数据导出",
            body = "导出记录、照片 URI、标签和关联关系为 JSON 文件。",
            trailing = {
                Button(
                    onClick = viewModel::prepareExport,
                    enabled = !uiState.isExporting
                ) {
                    Text(if (uiState.isExporting) "准备中" else "导出")
                }
            }
        )
        uiState.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        SettingCard(title = "关于岁迹", body = "岁迹 | 我的人生地图")
    }
}

@Composable
private fun SettingCard(
    title: String,
    body: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AtlasMist)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
            }
            trailing?.invoke()
        }
    }
}
