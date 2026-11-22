package com.huanchengfly.tieba.post.ui.common.theme.glass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * **App 自己的设计令牌**（Stage 5 · C）。
 *
 * 定位：我们定规范，Glasmio 施工。`LiquidGlassTheme` 从这里的 token 显式构造
 * `GlasmioTheme` 的 `colorScheme` / `colors` / `dimensions`，**不再依赖 Glasmio 的默认值**；
 * 页面要取色/取圆角时优先读 `LocalAppTokens.current`，而不是 `GlasmioTheme.colors`。
 *
 * 为什么还要保留 `ExtendedColors`：老页面（430 处）仍然读它，本轮不动，避免一次改太多。
 * 新页面 / 关键页面的新代码应该读这里。
 *
 * 深色规范（这次顺带验证「深色偏灰」）：
 * - 背景 **纯黑** `#000000`（不是 Glasmio 默认的 groupedBackground 灰）
 * - 卡片 `#1C1C1E`，卡片上浮层 `#2C2C2E`
 * - 正文白 `#FFFFFF`，次要 `#98989F`，三级 `#5A5A5F`
 */
@Immutable
data class AppTokens(
    val isDark: Boolean,
    // ── 文字 ──
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    // ── 表面 ──
    val background: Color,
    val card: Color,
    val cardProminent: Color,
    val separator: Color,
    val stroke: Color,
    // ── 语义色 ──
    val accent: Color,
    val destructive: Color,
    // ── 圆角 / 尺寸 ──
    val radiusCard: Dp = 26.dp,
    val radiusCapsule: Dp = 44.dp,
    val radiusAlert: Dp = 34.dp,
    val radiusSheet: Dp = 34.dp,
    val radiusCompact: Dp = 14.dp,
    val controlHeight: Dp = 44.dp,
    val iconButtonSize: Dp = 44.dp,
    val bottomBarHeight: Dp = 62.dp,
)

/** 浅色一套。 */
val LightAppTokens = AppTokens(
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

/** 深色一套：纯黑底 + #1C1C1E 卡片。 */
val DarkAppTokens = AppTokens(
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

/** 当前生效的一套（由 `LiquidGlassTheme` 提供）。 */
val LocalAppTokens = staticCompositionLocalOf { LightAppTokens }

/**
 * 按明暗取 token；[accent] 非 `Color.Unspecified` 时覆盖默认强调色（App 主题色优先）。
 */
fun appTokens(isDark: Boolean, accent: Color = Color.Unspecified): AppTokens {
    val base = if (isDark) DarkAppTokens else LightAppTokens
    return if (accent == Color.Unspecified) base else base.copy(accent = accent)
}
