package com.huanchengfly.tieba.post.ui.common.components.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalGlassSamplingSafe
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalLiquidGlassHosted
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassSurface
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost

/**
 * Glass 组件层的**采样环境**：让组件在没有页级 backdrop 的页面里也能拿到真玻璃。
 *
 * 三条路径（与 `GlassIconButton` 的 `GlassCircleHost` 同一套判断，只是换成通用形状）：
 * 1. 已经在玻璃 host 里（`LocalLiquidGlassHosted`，例如 `GlassPage` 的 overlay 层、
 *    弹窗的 `GlassDialogBackdropScope`）→ 直接采真实内容，画真玻璃；
 * 2. 页面没有 host（普通设置页、列表页）→ 就地起一个**只采样纯色**的小 host
 *    （`GlasmioBackdropHost(sampledColor = ...)`，Glasmio 给「滚动内容里的控件」留的
 *    `fillsBoundsUniformly` 路径，移动时复用已渲染结果，几乎零成本），
 *    并临时把 [LocalLiquidGlassHosted] 置真 → `LiquidGlassSurface` 走真玻璃分支；
 * 3. API < 33（shader 起不来）或宽屏等降级路径 → 不建 host，`LiquidGlassSurface`
 *    自己退回伪玻璃（半透明 + 1dp 描边 + 阴影），布局与内容完全不变。
 *
 * host 必须裁成组件自己的形状：采样层是铺满 host 的矩形，不裁的话圆角处会露出
 * 一个实色方块（`GlassIconButton` 踩过同样的坑）。
 *
 * @param fill 自带 host 时的采样底色（应当是组件所在页面的底色）
 * @param shape 组件形状（host 会按它裁剪）
 */
@Composable
fun GlassComponentHost(
    fill: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!isLiquidGlassSupported() || LocalLiquidGlassHosted.current) {
        content()
        return
    }
    GlasmioBackdropHost(
        sampledColor = fill,
        modifier = modifier.clip(shape),
    ) {
        CompositionLocalProvider(
            LocalLiquidGlassHosted provides true,
            LocalGlassSamplingSafe provides true,
        ) {
            content()
        }
    }
}

/**
 * 一个「能独立工作」的玻璃控件表面：组件层所有玻璃外壳都走这里。
 *
 * 相比直接用 [LiquidGlassSurface]，它多了 [GlassComponentHost] 那一层采样环境，
 * 所以在「没有活 backdrop 的页面」上依然是真玻璃而不是白盘。
 *
 * @param tint 玻璃材质的 tint；null 走 Glasmio 默认
 * @param hostFill 自带 host 的采样底色（页面底色）
 */
@Composable
fun GlassComponentSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    tint: Color? = null,
    hostFill: Color = AppTokens.groupedBackground,
    innerGlow: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!isLiquidGlassSupported() || LocalLiquidGlassHosted.current) {
        LiquidGlassSurface(
            modifier = modifier,
            shape = shape,
            navigationSurfaceColor = tint,
            innerGlow = innerGlow,
            content = content,
        )
        return
    }
    Box(modifier) {
        GlassComponentHost(
            fill = hostFill,
            shape = shape,
            modifier = Modifier.matchParentSize(),
        ) {
            LiquidGlassSurface(
                modifier = Modifier.matchParentSize(),
                shape = shape,
                navigationSurfaceColor = tint,
                innerGlow = innerGlow,
                content = content,
            )
        }
    }
}
