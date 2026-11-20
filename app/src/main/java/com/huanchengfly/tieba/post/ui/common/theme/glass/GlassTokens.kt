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
import io.github.neoruaa.glasmio.glass.NavigationGlassMaterial
import kotlin.math.pow

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

        /** 胶囊距屏幕底 21dp（FlClash `navigation_dock.dart:26` `_edgeMargin`）。 */
        val dockMarginBottom: Dp = 21.dp

        /** 顶栏上的悬浮玻璃按钮：FlClash 右上角「🖊」是 44dp 圆形，图标 20dp。 */
        val topBarActionSize: Dp = 44.dp
        val topBarActionIconSize: Dp = 20.dp

        /** 相邻玻璃按钮之间的一半间距（按钮自身左右各让这么多，合起来 8dp）。 */
        val topBarActionGap: Dp = 4.dp
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

    /** FlClash `LiquidGlassStyle.regular` 的浅色 tint（白 54%）。 */
    val lightNavigationTint: Color = Color(0x8AFDFDFD)

    /** 深色 tint 的 tone 抬升量；数值来自 FlClash `_darkGlassTintOverCard`。 */
    const val darkNavigationToneLift: Float = 7f

    /** 深色 tint 的 alpha；数值来自 FlClash `_darkGlassTint(...).withValues(alpha: 0.62)`。 */
    const val darkNavigationAlpha: Float = 0.62f

    /** 顶栏薄玻璃的 tint 透明度上限（PLAN 第 10 节第 10 条：滚动后必须够可读）。 */
    const val topBarGlassAlpha: Float = 0.62f

    /**
     * 顶栏 44dp 玻璃圆钮的 tint alpha 乘数。
     *
     * 圆钮尺寸小、下面压着内容，tint 用满会糊成一块实心圆盘；0.8 让 FlClash regular 那组
     * tint（浅白 58% / 深 container+7 tone 62%）留一点透，同时深色下仍能看出壳。
     */
    const val topBarActionTintMultiplier: Float = 0.8f

    /**
     * dock 用的玻璃材质。
     *
     * 先用 FlClash 同档的 `RegularSmall`（有 5dp 霜化，胶囊才像玻璃而不是一块半透明色板）；
     * 它的 fill 本身已经有厚度，所以真正的透明度靠 [dockTintMultiplier] 调。
     */
    val dockMaterial: NavigationGlassMaterial = NavigationGlassMaterial.RegularSmall

    /**
     * dock 的 tint alpha 乘数。
     *
     * 浅色不传 1f：FlClash 的 `#8AFDFDFD` 是它自己管线的**最终**不透明度，Glasmio 会在其之上
     * 再叠材质 fill，照搬会叠到 ~0.75（参考图里能看见胶囊后面的卡片，我们却是一块板子）。
     * 深色本来就只有一层 17% 的亮度 fill，乘数可以接近 1 才不至于糊在纯黑里。
     */
    fun dockTintMultiplier(dark: Boolean): Float = if (dark) 1.3f else 0.35f

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
 * 导航玻璃（dock / 顶栏薄玻璃）的 tint，数值取自 FlClash `LiquidGlassStyle.regular`：
 * - 浅色 `#8AFDFDFD`（白 ≈ 54%）；
 * - 深色取 container 色（App 的 `card`）抬高 7 级 tone，alpha 0.62。
 *
 * 深色这一档是「胶囊能看见」的关键：纯 container 在近黑背景上和背景糊成一片，
 * 抬 7 级 tone 后才有可辨的壳，再叠 1px 上缘高光（见 `LiquidGlassSurface(innerGlow=)`）。
 */
@Composable
fun glassNavigationTint(): Color {
    val colors = ExtendedTheme.colors
    return if (colors.isNightMode) {
        colors.card.shiftTone(7f).copy(alpha = 0.62f)
    } else {
        GlassTokens.lightNavigationTint
    }
}

/**
 * 按 L*（tone）平移颜色，保留色相/彩度。
 *
 * FlClash 用 material 的 HCT `shiftTone`；工程里没有 HCT 依赖，这里在 sRGB→线性→L* 上做
 * 等价的一维平移：先把线性 RGB 按新亮度整体缩放，再编码回 sRGB。
 */
fun Color.shiftTone(delta: Float): Color {
    fun toLinear(c: Float): Float =
        if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

    fun toSrgb(c: Float): Float =
        if (c <= 0.0031308f) c * 12.92f else 1.055f * c.pow(1f / 2.4f) - 0.055f

    val lr = toLinear(red)
    val lg = toLinear(green)
    val lb = toLinear(blue)
    val y = 0.2126f * lr + 0.7152f * lg + 0.0722f * lb
    val lStar = if (y <= 0.008856f) 903.3f * y else 116f * y.pow(1f / 3f) - 16f
    val target = (lStar + delta).coerceIn(0f, 100f)
    val targetY = if (target <= 8f) target / 903.3f else ((target + 16f) / 116f).pow(3f)
    val scale = if (y <= 0.0001f) 0f else targetY / y
    return Color(
        red = toSrgb((lr * scale).coerceIn(0f, 1f)),
        green = toSrgb((lg * scale).coerceIn(0f, 1f)),
        blue = toSrgb((lb * scale).coerceIn(0f, 1f)),
        alpha = alpha,
    )
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
