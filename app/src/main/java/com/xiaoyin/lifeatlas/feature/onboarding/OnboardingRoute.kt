package com.xiaoyin.lifeatlas.feature.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.EditLocationAlt
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessCream
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMeadow
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMuted
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessPaper
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessSky
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessTeal
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val body: String,
    val icon: ImageVector,
    val color: Color
)

private val onboardingPages = listOf(
    OnboardingPage(
        title = "写下一段新记忆",
        body = "用文字、照片、标签和心情记录重要时刻。每一条记录都会进入时间轴，之后可以搜索、筛选和回看。",
        icon = Icons.Outlined.EditLocationAlt,
        color = WildernessMeadow
    ),
    OnboardingPage(
        title = "点亮人生地图",
        body = "新增记录时使用地图选点，保存后这个地点会被点亮。地图页可以按城市查看，也可以收藏想反复回看的坐标。",
        icon = Icons.Outlined.Map,
        color = WildernessSky
    ),
    OnboardingPage(
        title = "本地优先，记得备份",
        body = "第一版以本地保存为主。重要记忆建议定期导出完整备份包，换手机或重装 App 时可以恢复。",
        icon = Icons.Outlined.Backup,
        color = WildernessWildflower
    )
)

@Composable
fun OnboardingRoute(
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WildernessCream)
            .padding(horizontal = 22.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "欢迎来到岁迹",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = WildernessTeal
            )
            OutlinedButton(onClick = onSkip, shape = RoundedCornerShape(18.dp)) {
                Text("跳过")
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { pageIndex ->
            OnboardingPageCard(
                page = onboardingPages[pageIndex],
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 34.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onboardingPages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(width = if (index == pagerState.currentPage) 24.dp else 8.dp, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (index == pagerState.currentPage) WildernessTeal else WildernessMeadow)
                    )
                }
            }

            Button(
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WildernessTeal, contentColor = WildernessPaper)
            ) {
                Text(if (isLastPage) "开始记录" else "下一步", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun OnboardingPageCard(page: OnboardingPage, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = WildernessPaper),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(page.color.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(page.icon, contentDescription = null, tint = WildernessTeal, modifier = Modifier.size(58.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = WildernessTeal,
                textAlign = TextAlign.Center
            )
            Text(
                text = page.body,
                style = MaterialTheme.typography.bodyLarge,
                color = WildernessMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
