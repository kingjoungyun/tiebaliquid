package com.huanchengfly.tieba.post.ui.common.theme.glass

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import io.github.neoruaa.glasmio.core.LocalGlassColors as GlasmioLocalGlassColors
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.GlassSurface as GlasmioGlassSurface
import io.github.neoruaa.glasmio.glass.NavigationGlassMaterial
import io.github.neoruaa.glasmio.ui.GlasmioIconButton

/**
 * 全 App 统一的「悬浮玻璃圆钮」。
 *
 * 之前有三套实现，观感/降级行为各不相同：
 * 1. `PrefsBackButton`（设置/我的）→ Glasmio `GlasmioIconButton`，无 host 时退伪玻璃；
 * 2. `MainTopBarGlassActionItem`（主界面顶栏）→ 手搓 `LiquidGlassSurface` + 导航 tint；
 * 3. 吧页 `ForumCircleButton` → 实色 `elevatedBackground` + 1dp 描边（那时没有 host，做不了玻璃）。
 *
 * 现在收敛成这一个组件，按「采样环境」自动选路：
 *
 * | 所在位置 | 判定 | 实现 |
 * |---|---|---|
 * | host 的 overlay 层（dock、顶栏） | [LocalGlassSamplingSafe] = true | Glasmio 控件 + 外层 backdrop（**采到真实内容**） |
 * | 普通页面内容里（吧页、历史页…） | 有 host 但采样不安全 / 干脆没 host | 组件**自带一个纯色小 host**（尺寸 = 按钮），再用 Glasmio 控件采样它 |
 * | API < 33 | [isLiquidGlassSupported] = false | [LiquidGlassSurface] 伪玻璃（P2 老实现） |
 *
 * 为什么要「自带 host」而不是「无 host 就退伪玻璃」：
 * - `LocalGlassBackdrop` 的默认实现是 `error(...)`，直接读会抛异常；
 * - 页面内容本身就在外层录制层里，读外层 backdrop 会让控件「自己采样自己」（Glasmio 明令禁止的
 *   feedback loop）；
 * - 自带一个纯色小 host 既避免递归，又保住真 Glasmio 材质（纯色底的玻璃就是 iOS 悬浮钮的观感），
 *   而且 `fillsBoundsUniformly` 的纯色源会让 Glasmio 复用已渲染结果，几乎零成本。
 * 所以本 App 里「没 host 的页面」上，悬浮钮依然是真玻璃，而不是掉回伪玻璃。
 *
 * 用法（一行）：
 * ```
 * GlassIconButton(onClick = onBack, contentDescription = "返回") { tint ->
 *     SfIcon("chevron.left", null, tint = tint, mirrored = true)
 * }
 * ```
 *
 * @param icon 图标槽：参数是按材质解析好的前景色（浅玻璃→深色，深玻璃→浅色）。传了 [tint] 就用 tint。
 * @param tint 强制图标颜色；null = 跟随材质
 * @param navigationSurfaceColor 玻璃材质 tint；默认用 App 的导航玻璃 token
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = GlassTokens.Spacing.topBarActionSize,
    tint: Color? = null,
    navigationSurfaceColor: Color? = glassNavigationTint(),
    icon: @Composable (Color) -> Unit,
) {
    GlassCircleHost(
        modifier = modifier,
        size = size,
        fill = navigationSurfaceColor ?: glassNavigationTint(),
    ) { materialTint ->
        GlasmioIconButton(
            onClick = onClick,
            enabled = enabled,
            size = size,
            navigationSurfaceColor = navigationSurfaceColor,
        ) {
            icon(tint ?: materialTint)
        }
    }
}

/** [GlassIconButton] 的 ImageVector 便捷重载（绝大多数调用点一行搞定）。 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = GlassTokens.Spacing.topBarActionSize,
    iconSize: Dp = GlassTokens.Spacing.topBarActionIconSize,
    tint: Color? = null,
    navigationSurfaceColor: Color? = glassNavigationTint(),
) {
    GlassIconButton(
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
        enabled = enabled,
        size = size,
        tint = tint,
        navigationSurfaceColor = navigationSurfaceColor,
    ) { color ->
        Image(
            imageVector = icon,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(color),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * 只要玻璃壳、不要点击的圆面（菜单 ⋯ 触发器、纯展示）。
 *
 * 菜单触发器必须由外层 `ClickMenu` 的 `combinedClickable` 处理点击，
 * 用 [GlassIconButton] 会自带 clickable 把点击吃掉，所以单独留一个无手势版本。
 */
@Composable
fun GlassCircleSurface(
    modifier: Modifier = Modifier,
    size: Dp = GlassTokens.Spacing.topBarActionSize,
    navigationSurfaceColor: Color? = glassNavigationTint(),
    content: @Composable BoxScope.(Color) -> Unit,
) {
    GlassCircleHost(
        modifier = modifier,
        size = size,
        fill = navigationSurfaceColor ?: glassNavigationTint(),
    ) { materialTint ->
        LiquidGlassTheme(accent = ExtendedTheme.colors.primary) {
            GlasmioGlassSurface(
                modifier = Modifier.size(size),
                shape = CircleShape,
                material = NavigationGlassMaterial.RegularSmall,
                navigationSurfaceColor = navigationSurfaceColor,
                emphasis = GlassEmphasis.Regular,
            ) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    content(materialTint)
                }
            }
        }
    }
}

/**
 * 圆钮的「采样环境」适配层：决定用外层 backdrop 还是自带一个纯色小 host，
 * 并在真玻璃路径下保证 `LocalGlassBackdrop` 可读、材质明暗正确。
 */
@Composable
private fun GlassCircleHost(
    modifier: Modifier,
    size: Dp,
    fill: Color,
    content: @Composable (materialTint: Color) -> Unit,
) {
    if (!isLiquidGlassSupported()) {
        // API < 33：伪玻璃（LiquidGlassSurface 自己会降级），前景色用 App 色板。
        val colors = glassColors()
        val tint = if (colors.isDark) Color.White else Color.Black
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            LiquidGlassSurface(
                modifier = Modifier.matchParentSize(),
                shape = CircleShape,
                material = NavigationGlassMaterial.RegularSmall,
                navigationSurfaceColor = fill,
                innerGlow = true,
            ) {}
            content(tint)
        }
        return
    }

    val ambientSafe = LocalGlassSamplingSafe.current
    if (ambientSafe) {
        LiquidGlassTheme(accent = ExtendedTheme.colors.primary) {
            Box(modifier.size(size), contentAlignment = Alignment.Center) {
                content(GlasmioLocalGlassColors.current.content)
            }
        }
    } else {
        // 自带纯色 host：尺寸 = 按钮、uniformFill（Glasmio 复用已渲染结果），
        // 既不递归、也拿得到真材质。
        GlasmioBackdropHost(
            sampledColor = fill,
            modifier = modifier.size(size),
        ) {
            CompositionLocalProvider(
                LocalLiquidGlassHosted provides true,
                LocalGlassSamplingSafe provides true,
            ) {
                LiquidGlassTheme(accent = ExtendedTheme.colors.primary) {
                    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                        content(GlasmioLocalGlassColors.current.content)
                    }
                }
            }
        }
    }
}
