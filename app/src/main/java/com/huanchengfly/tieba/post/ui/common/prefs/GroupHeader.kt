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
 * 字号走 [GlassTokens.Typography.footnote]（13sp），颜色 secondary；
 * 左缩进与卡片内容的文字起点对齐（16dp），因为卡片本身已经吃了页面 16dp 留白。
 */
@Composable
fun GroupHeader(
    title: String,
    color: Color = glassColors().textSecondary
) {
    Box(
        Modifier
            .padding(
                start = PrefsCardDefaults.ContentPadding,
                top = 4.dp,
            )
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            color = color,
            fontSize = GlassTokens.Typography.footnote.size,
            lineHeight = GlassTokens.Typography.footnote.lineHeight,
            fontWeight = GlassTokens.Typography.footnote.weight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
