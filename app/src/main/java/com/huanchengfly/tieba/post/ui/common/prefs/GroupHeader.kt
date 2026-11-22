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
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceGroupTitle
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalGlassTokens

/**
 * 分组标题。
 *
 * - `PrefsUiStyle.GLASSMIO`：直接交给 `GlasmioPreferenceGroupTitle`（= `GlasmioSectionTitle`：
 *   39dp 高、17sp w590、次级色、底部 9dp、左右 16dp），与卡片同一条基线。
 * - `PrefsUiStyle.LEGACY`：FlClash / iOS 的系统分组列表风格（17sp / w600 / 次级文字色，
 *   单行省略；左缩进 16dp 与卡片内容的文字起点对齐）。
 *
 * 上间距不在这里做：Glasmio 由 `LazyColumn` 的组间距负责，A 版由 [PrefsCard] 的
 * `headerTopSpacing` 统一处理 —— 这样「组间距」和「标题上间距」不会叠成两份。
 */
@Composable
fun GroupHeader(
    title: String,
    color: Color = LocalGlassTokens.current.textSecondary
) {
    if (LocalPrefsUiStyle.current == PrefsUiStyle.GLASSMIO) {
        GlasmioPreferenceGroupTitle(title)
        return
    }
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
