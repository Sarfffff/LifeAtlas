package com.xiaoyin.lifeatlas.feature.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessMuted
import com.xiaoyin.lifeatlas.core.ui.theme.WildernessWildflower
import kotlin.math.roundToInt

@Composable
fun ImportanceStars(
    value: Float,
    modifier: Modifier = Modifier,
    starSize: Dp = 26.dp,
    editableStarSize: Dp = 28.dp,
    editableButtonSize: Dp = 34.dp,
    onValueChange: ((Float) -> Unit)? = null
) {
    val selectedCount = value.roundToInt().coerceIn(1, 5)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..5).forEach { star ->
            val tint = if (star <= selectedCount) {
                WildernessWildflower
            } else {
                WildernessMuted.copy(alpha = 0.28f)
            }
            if (onValueChange == null) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(starSize)
                )
            } else {
                IconButton(onClick = { onValueChange(star.toFloat()) }, modifier = Modifier.size(editableButtonSize)) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "$star 星",
                        tint = tint,
                        modifier = Modifier.size(editableStarSize)
                    )
                }
            }
        }
    }
}
