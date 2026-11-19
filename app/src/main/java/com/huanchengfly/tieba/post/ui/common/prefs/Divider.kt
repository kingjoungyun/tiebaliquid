package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 组内分割线（FlClash settings 组）。
 *
 * 颜色为浅色 `#00000014` / 深色 `#FFFFFF1F`，左侧缩进 16dp（右侧不缩进），
 * 也就是「从文字起点开始、一直画到卡片右边缘」。
 */
@Composable
fun Divider(
    modifier: Modifier = Modifier,
    color: Color = PrefsCardDefaults.divider(),
    thickness: Dp = PrefsCardDefaults.DividerThickness,
    indent: Dp = PrefsCardDefaults.DividerIndent
) {
    Box(
        modifier
            .then(Modifier.padding(start = indent))
            .fillMaxWidth()
            .height(thickness)
            .background(color = color)
    )
}
