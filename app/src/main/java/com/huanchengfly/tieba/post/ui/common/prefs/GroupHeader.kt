package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors

/**
 * 分组标题：渲染在卡片**上方**（FlClash / iOS 的系统分组列表风格）。
 *
 * 规格：17sp / w600 / 次级文字色，单行省略；左缩进 16dp 与卡片内容的文字起点对齐
 * （卡片本身已经吃了页面 16dp 留白）。
 *
 * 上间距（首组 16dp、其余组 24dp）不在这里做，而是由 [PrefsCard] 的 `headerTopSpacing`
 * 统一处理 —— 这样「组间距 8dp」和「标题上间距」不会叠成两份。
 */
@Composable
fun GroupHeader(
    title: String,
    color: Color = glassColors().textSecondary
) {
    Box(
        Modifier
            .padding(start = PrefsCardDefaults.ContentPadding)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            color = color,
            fontSize = GlassTokens.Typography.body.size,
            lineHeight = GlassTokens.Typography.body.lineHeight,
            fontWeight = GlassTokens.Typography.headline.weight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
