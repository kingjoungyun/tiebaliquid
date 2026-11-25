package com.huanchengfly.tieba.post.ui.common.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassComponentSurface
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.GlasmioTheme

/**
 * 列表里的「一张玻璃卡」（T5 批次 2 抽出来的通用行容器）。
 *
 * 设置页 / 屏蔽列表 / 关注吧 / 粉丝列表这类**非帖子**的列表行，统一长这样：
 * 16dp 连续圆角 + `AppTokens.card` 材质 + 14/12dp 内边距 + 1dp `subtleStroke` 内缘
 * （浅色下卡与页面底都是近白，不描边边界太弱）。
 *
 * 和 `PostCard` 的分工：帖子用 `PostCard`，其余列表行用本组件 + 各自的行内容。
 */
@Composable
fun GlassListCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = ContinuousRoundedRectangle(16.dp)
    GlassComponentSurface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, GlasmioTheme.colors.subtleStroke, shape),
        shape = shape,
        tint = AppTokens.card,
        hostFill = AppTokens.groupedBackground,
        innerGlow = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            content = content,
        )
    }
}
