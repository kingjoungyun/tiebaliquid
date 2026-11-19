package com.huanchengfly.tieba.post.ui.common.theme.glass

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

/**
 * 「液态玻璃」设计令牌（P1）。
 *
 * 数值取自 FlClash 的 `prism_ui` 设计系统，但这里是纯 Compose 实现，
 * 不依赖 Flutter / 不引入任何新依赖。
 *
 * 本文件只放**数值与颜色**，不参与现有 `ExtendedTheme`（M2 色板）的构建，
 * 以免污染既有主题；取色统一走 [glassColors]。
 */
object GlassTokens {

    /** 圆角阶梯（dp）。 */
    object Radii {
        val none: Dp = 0.dp
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 16.dp
        val lg: Dp = 20.dp
        val xl: Dp = 24.dp
        val xxl: Dp = 28.dp
        val alert: Dp = 34.dp
        val full: Dp = 1000.dp
    }

    /** 间距 / 尺寸阶梯（dp）。 */
    object Spacing {
        val pagePadding: Dp = 16.dp
        val cardGap: Dp = 8.dp
        val rowMinHeight: Dp = 56.dp
        val barHeight: Dp = 72.dp
        val dockHeight: Dp = 62.dp
        val dockInsetH: Dp = 4.dp
        val dockMarginH: Dp = 16.dp
        val dockMarginBottom: Dp = 12.dp
    }

    /**
     * 字号阶梯：字号 / 行高 / 字重。
     * 只描述数值，渲染时用 [GlassTextStyle.toTextStyle] 转成 `TextStyle`。
     */
    object Typography {
        val display = GlassTextStyle(40.sp, 44.sp, FontWeight(700))
        val largeTitle = GlassTextStyle(34.sp, 41.sp, FontWeight(700))
        val pageTitle = GlassTextStyle(24.sp, 30.sp, FontWeight(600))
        val title2 = GlassTextStyle(22.sp, 28.sp, FontWeight(600))
        val title3 = GlassTextStyle(20.sp, 25.sp, FontWeight(600))
        val headline = GlassTextStyle(17.sp, 22.sp, FontWeight(600))
        val body = GlassTextStyle(17.sp, 22.sp, FontWeight(400))
        val subheadline = GlassTextStyle(15.sp, 20.sp, FontWeight(400))
        val footnote = GlassTextStyle(13.sp, 18.sp, FontWeight(400))
        val caption1 = GlassTextStyle(12.sp, 16.sp, FontWeight(400))
        val caption2 = GlassTextStyle(11.sp, 13.sp, FontWeight(400))
    }

    /** 动效令牌。 */
    object Motion {
        val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
        val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1.0f)
        const val pressDurationMs: Int = 200
        const val commonDurationMs: Int = 300
    }

    /** 玻璃材质本身的一些数值（伪玻璃阶段用）。 */
    object Glass {
        val strokeWidth: Dp = 1.dp
        val shadowElevation: Dp = 16.dp
        val shadowOffsetY: Dp = 4.dp
        val shadowAlpha: Float = 0.08f
        val selectedPillAlpha: Float = 0.12f
    }

    /** 浅色令牌。 */
    val lightColors: GlassColors = GlassColors(
        isDark = false,
        background = Color(0xFFF2F2F7),
        surface = Color(0xFFFFFFFF),
        surfaceSecondary = Color(0xFFEDEDF0),
        primary = Color(0xFF0A84FF),
        textPrimary = Color(0xFF000000),
        textSecondary = Color(0xFF8E8E93),
        glassFill = Color.White.copy(alpha = 0.72f),
        glassStroke = Color.White.copy(alpha = 0.65f),
        gradientTop = Color(0xFFF7F7FA),
        gradientBottom = Color(0xFFEDEEF3),
        shadow = Color.Black.copy(alpha = Glass.shadowAlpha),
    )

    /** 深色令牌。 */
    val darkColors: GlassColors = GlassColors(
        isDark = true,
        background = Color(0xFF000000),
        surface = Color(0xFF1C1C1E),
        surfaceSecondary = Color(0xFF2C2C2E),
        primary = Color(0xFF0A84FF),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF98989F),
        glassFill = Color(0xFF1C1C1E).copy(alpha = 0.72f),
        glassStroke = Color.White.copy(alpha = 0.12f),
        gradientTop = Color(0xFF0A0A0C),
        gradientBottom = Color(0xFF17171A),
        shadow = Color.Black.copy(alpha = Glass.shadowAlpha),
    )

    /**
     * 非 Composable 取色入口（给非 Compose 上下文 / 预览 / 单测用）。
     *
     * Compose 内请优先用 [glassColors]，它会跟随系统明暗。
     */
    fun colors(dark: Boolean = false): GlassColors = if (dark) darkColors else lightColors
}

/**
 * 一套玻璃色。纯数据，无 `CompositionLocal`，避免影响既有 `ExtendedTheme`。
 */
@Immutable
data class GlassColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceSecondary: Color,
    val primary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val glassFill: Color,
    val glassStroke: Color,
    val gradientTop: Color,
    val gradientBottom: Color,
    val shadow: Color,
) {
    /** 选中态胶囊底色。 */
    val selectedPill: Color
        get() = primary.copy(alpha = GlassTokens.Glass.selectedPillAlpha)
}

/**
 * 跟随 **App 自己的主题明暗** 取色（即 [ExtendedTheme] 的 `isNightMode`）。
 *
 * 为什么不是 `isSystemInDarkTheme()`：实测这个 App 的明暗由它自己的主题设置决定
 * （默认固定浅色，`cmd uimode night yes` 时页面文字仍是浅色主题的黑字），
 * 玻璃层若跟着系统变深会出现「黑字压深底」看不清。用 App 主题的明暗才和页面文字一致，
 * 也等价于改造前 `Surface(color = ExtendedTheme.colors.background)` 的行为。
 *
 * 注意：这里只借用 `isNightMode` 这一个布尔值，**颜色全部来自 glass 令牌**，
 * 不读 M2 色板，因此不会污染既有主题。
 */
@Composable
fun glassColors(): GlassColors =
    if (ExtendedTheme.colors.isNightMode) GlassTokens.darkColors else GlassTokens.lightColors

/** 显式指定明暗的取色入口（预览 / 需要强制明暗的地方用）。 */
fun glassColors(dark: Boolean): GlassColors =
    if (dark) GlassTokens.darkColors else GlassTokens.lightColors

/** 字号 / 行高 / 字重三元组。 */
@Immutable
data class GlassTextStyle(
    val size: TextUnit,
    val lineHeight: TextUnit,
    val weight: FontWeight,
)
