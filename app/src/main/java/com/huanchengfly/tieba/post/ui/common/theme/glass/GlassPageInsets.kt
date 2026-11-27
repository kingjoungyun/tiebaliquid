package com.huanchengfly.tieba.post.ui.common.theme.glass

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 页面顶部 insets 的**唯一来源**（B 线 · insets 收口）。
 *
 * 背景：以前每个页面自己写
 * `WindowInsets.statusBars.asPaddingValues().calculateTopPadding()` 再加一个魔法数，
 * 于是「状态栏 + 顶栏高」这件事散落在十几个文件里，改一处顶栏高度要全仓搜。
 *
 * 约定（新增页面请遵守）：
 * 1. **页面不要再直接读 `WindowInsets.statusBars`**；需要状态栏高度就调 [statusBarTop]，
 *    需要给「透明悬浮顶栏」让位就调 [contentTopPadding]；
 * 2. 用 [GlassPage] 的页面**什么都不用算**：骨架自己会把 content padding 算好传进来；
 * 3. 自绘顶栏的页面（吧页 / 子帖 / 帖子页等）用 [contentTopPadding] 作为
 *    `LazyColumn(contentPadding = PaddingValues(top = …))` 的 top，列表就能滚到顶栏下方。
 */
object GlassPageInsets {

    /** 标准玻璃顶栏（[GlassTokens.Spacing.barHeight]，72dp）的高度。 */
    val topBarHeight: Dp = GlassTokens.Spacing.barHeight

    /** 状态栏高度（overlay 顶栏从这一条开始画）。 */
    @Composable
    fun statusBarTop(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    /** 内容需要让出的顶部高度 = 状态栏 + 标准顶栏。 */
    @Composable
    fun contentTopPadding(): Dp = statusBarTop() + topBarHeight

    /** 内容需要让出的顶部高度，顶栏不是标准高度时用这个（例如吧页的 48dp 顶栏）。 */
    @Composable
    fun contentTopPadding(barHeight: Dp): Dp = statusBarTop() + barHeight
}

/** 兼容写法：`0.dp` 之类的常量仍需 `dp` 扩展，放这里避免各处 import。 */
private val DpUnused: Dp = 0.dp
