package com.huanchengfly.tieba.post.ui.common.theme.glass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glasmio 主题用的**静态 token 集合**（Stage 5 引入，Stage 6 起与运行时的 [AppTokens] 并存）：
 * `LiquidGlassTheme` 用它构造 `GlasmioTheme` 的 GlassColors / GlassDimensions。
 *
 * 与 [AppTokens] 的分工：
 * - [AppTokens]：**运行时**颜色（来自 XML 主题解析，随主题/accent 变化），页面读它；
 * - [GlassTokenSet]：**Glasmio 组件**的静态规范值（圆角、尺寸、深浅两套语义色），
 *   在 `LiquidGlassTheme` 里用，不随 accent 的 XML 值变化。
 */
@Immutable
data class GlassTokenSet(
    val isDark: Boolean,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val background: Color,
    val card: Color,
    val cardProminent: Color,
    val separator: Color,
    val stroke: Color,
    val accent: Color,
    val destructive: Color,
    val radiusCard: Dp = 26.dp,
    val radiusCapsule: Dp = 44.dp,
    val radiusAlert: Dp = 34.dp,
    val radiusSheet: Dp = 34.dp,
    val radiusCompact: Dp = 14.dp,
    val controlHeight: Dp = 44.dp,
    val iconButtonSize: Dp = 44.dp,
    val bottomBarHeight: Dp = 62.dp,
)

val LightGlassTokens = GlassTokenSet(
    isDark = false,
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF8A8A8E),
    textTertiary = Color(0xFFC7C7CC),
    background = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    cardProminent = Color(0xFFFFFFFF),
    separator = Color(0x4D3C3C43),
    stroke = Color(0x14000000),
    accent = Color(0xFF0088FF),
    destructive = Color(0xFFFF3B30),
)

val DarkGlassTokens = GlassTokenSet(
    isDark = true,
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF98989F),
    textTertiary = Color(0xFF5A5A5F),
    background = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    cardProminent = Color(0xFF2C2C2E),
    separator = Color(0x33FFFFFF),
    stroke = Color(0x1AFFFFFF),
    accent = Color(0xFF0091FF),
    destructive = Color(0xFFFF453A),
)

val LocalGlassTokens = staticCompositionLocalOf { LightGlassTokens }

fun glassTokenSet(isDark: Boolean, accent: Color = Color.Unspecified): GlassTokenSet {
    val base = if (isDark) DarkGlassTokens else LightGlassTokens
    return if (accent == Color.Unspecified) base else base.copy(accent = accent)
}
