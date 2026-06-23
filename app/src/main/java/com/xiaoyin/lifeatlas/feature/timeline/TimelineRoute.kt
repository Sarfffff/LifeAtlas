package com.xiaoyin.lifeatlas.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xiaoyin.lifeatlas.core.ui.theme.AtlasMist

@Composable
fun TimelineRoute() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "时间轴", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            text = "按时间倒序回看人生节点。Room 接入后，这里会展示真实记录。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
        )

        Text(text = "2026 年 6 月", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        TimelinePreviewCard(
            title = "第一次拿到房本",
            meta = "武汉市洪山区 | 2026-06-15",
            body = "今天终于拿到了房本，算是人生阶段性节点。"
        )
        TimelinePreviewCard(
            title = "上海生活记录",
            meta = "上海市徐汇区 | 2026-06-08",
            body = "最近工作比较忙，但也慢慢稳定了。"
        )
    }
}

@Composable
private fun TimelinePreviewCard(title: String, meta: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = AtlasMist)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = meta, style = MaterialTheme.typography.bodySmall)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

