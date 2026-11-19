package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors

/**
 * 设置页的「大标题收进顶栏」。
 *
 * 大标题（[PrefsLargeTitle]）放在列表第一项里随内容滚走；顶栏标题（[PrefsCollapsedTitle]）
 * 的透明度直接由滚动偏移驱动，不做动画曲线 —— 手指一动顶栏就开始接字。
 */
private val CollapseDistance = 32.dp

/** 顶栏标题的淡入进度 0..1（列表滚过 [CollapseDistance] 后完全显示）。 */
@Composable
fun rememberPrefsTitleCollapseProgress(listState: LazyListState): Float {
    val collapseDistancePx = with(LocalDensity.current) { CollapseDistance.toPx() }
    val progress by remember(collapseDistancePx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
        }
    }
    return progress
}

/**
 * 列表顶部的大标题：FlClash 的 `pageTitle`，24sp / w600，左对齐。
 *
 * 左右留白由 [PrefsScreen] 的 `contentPadding`（16dp）给，这里只补上下的小间距，
 * 保证标题与卡片左边缘对齐。
 */
@Composable
fun PrefsLargeTitle(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
    ) {
        Text(
            text = text,
            style = GlassTokens.Typography.pageTitle.toTextStyle(glassColors().textPrimary),
            maxLines = 1,
        )
    }
}

/** 顶栏里的小标题：17sp / w600，[progress] 为 0 时完全不可见。 */
@Composable
fun PrefsCollapsedTitle(text: String, progress: Float) {
    Text(
        text = text,
        style = GlassTokens.Typography.headline.toTextStyle(glassColors().textPrimary),
        modifier = Modifier.alpha(progress),
        maxLines = 1,
    )
}
