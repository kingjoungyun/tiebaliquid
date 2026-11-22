package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.utils.ThemeUtil

val ExtendedColors.pullRefreshIndicator: Color
    get() = if (ThemeUtil.isTranslucentTheme(theme)) {
        windowBackground
    } else {
        indicator
    }

val ExtendedColors.loadMoreIndicator: Color
    get() = if (ThemeUtil.isTranslucentTheme(theme)) {
        windowBackground
    } else {
        indicator
    }

val ExtendedColors.threadBottomBar: Color
    get() = if (ThemeUtil.isTranslucentTheme(theme)) {
        windowBackground
    } else {
        bottomBar
    }

val ExtendedColors.menuBackground: Color
    get() = if (ThemeUtil.isTranslucentTheme(theme)) {
        windowBackground
    } else {
        card
    }

val ExtendedColors.invertChipBackground: Color
    get() = if (ThemeUtil.isNightMode(theme)) primary.copy(alpha = 0.3f) else primary

val ExtendedColors.invertChipContent: Color
    get() = if (ThemeUtil.isNightMode(theme)) primary else onPrimary
// ── AppTokens 上的同名扩展（Stage 6：页面统一读 AppTokens）──────────────────────────
// 语义与上面的 ExtendedColors 版本完全一致，只是 receiver 换成运行时 token 对象。

val AppTokens.pullRefreshIndicator: Color
    get() = if (ThemeUtil.isTranslucentTheme(theme)) windowBackground else indicator

val AppTokens.loadMoreIndicator: Color
    get() = if (ThemeUtil.isTranslucentTheme(theme)) windowBackground else indicator

val AppTokens.threadBottomBar: Color
    get() = if (ThemeUtil.isTranslucentTheme(theme)) windowBackground else bottomBar

val AppTokens.menuBackground: Color
    get() = if (ThemeUtil.isTranslucentTheme(theme)) windowBackground else card

val AppTokens.invertChipBackground: Color
    get() = if (ThemeUtil.isNightMode(theme)) primary.copy(alpha = 0.3f) else primary

val AppTokens.invertChipContent: Color
    get() = if (ThemeUtil.isNightMode(theme)) primary else onPrimary