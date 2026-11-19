package com.huanchengfly.tieba.post.ui.common.theme.glass

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.glass.GlassSurface as GlasmioGlassSurface
import io.github.neoruaa.glasmio.glass.NavigationGlassMaterial

/**
 * 「真·液态玻璃」接入层（Glasmio）。
 *
 * 这一层只做三件事：
 * 1. 把 [GlasmioBackdropHost] 包一层，并在**玻璃层**里提供一个「host 存在」的标记；
 * 2. 提供 [LiquidGlassSurface]：有 host 就用真玻璃，没有就自动退回 P2 的伪玻璃；
 * 3. 把 App 自己的明暗（[ExtendedTheme.colors.isNightMode]）灌给 Glasmio。
 *
 * 为什么不 `try/catch` 试探：[io.github.neoruaa.glasmio.glass.LocalGlassBackdrop] 是
 * `staticCompositionLocalOf { error(...) }`，没有 host 时读它就是抛异常，异常发生在
 * 组合期间，catch 不住也没法优雅降级。所以用我们自己的 [LocalLiquidGlassHosted] 显式判断。
 */
val LocalLiquidGlassHosted = staticCompositionLocalOf { false }

/**
 * Glasmio 的材质渲染管线（RuntimeShader / RenderEffect）要求 API 33+；本工程 minSdk 也是 33，
 * 这里仍然显式判断一次，作为「降级路径」的第一个条件。
 */
@ReadOnlyComposable
fun isLiquidGlassSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * 整个「可被玻璃采样的页面」的容器。
 *
 * [sampledContent] 会被录进一个 graphics layer，玻璃从里面取像素；
 * [overlayContent] 画在录制层之上，玻璃控件必须放在这里，否则会自己采样自己（死循环）。
 */
@Composable
fun LiquidGlassBackdropHost(
    modifier: Modifier = Modifier,
    sampledContent: @Composable BoxScope.() -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit,
) {
    GlasmioBackdropHost(
        modifier = modifier,
        sampledContent = { sampledContent() },
        overlayContent = {
            CompositionLocalProvider(LocalLiquidGlassHosted provides true) {
                overlayContent()
            }
        },
    )
}

/**
 * 玻璃表面：真玻璃可用时用 Glasmio 的 [io.github.neoruaa.glasmio.glass.GlassSurface]，
 * 否则退回 P2 的伪玻璃（[GlassSurface]：半透明 + 1dp 描边 + 阴影）。
 *
 * 降级条件（任一成立就走伪玻璃）：
 * - [LocalLiquidGlassHosted] 为 false —— 当前这棵子树不在 [LiquidGlassBackdropHost] 里
 *   （例如宽屏走 NavigationRail 分支、或者开关关掉了玻璃层）。这一条也保证了「不会白屏、
 *   不会因为读不到 backdrop 而崩」。
 * - [isLiquidGlassSupported] 为 false —— API < 33，Glasmio 的 shader 起不来。
 *
 * 注意：降级是**整块表面**级的（形状/内容完全一致，只换 background 实现），所以布局不会跳。
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    material: NavigationGlassMaterial = NavigationGlassMaterial.RegularSmall,
    content: @Composable BoxScope.() -> Unit,
) {
    val hosted = LocalLiquidGlassHosted.current
    if (hosted && isLiquidGlassSupported()) {
        LiquidGlassTheme {
            GlasmioGlassSurface(
                modifier = modifier,
                shape = shape,
                material = material,
                content = content,
            )
        }
    } else {
        val colors = glassColors()
        // 降级路径：P2 的伪玻璃，刻意保留
        GlassSurface(
            modifier = modifier,
            shape = shape,
            fill = colors.glassFill,
            strokeColor = colors.glassStroke,
            content = content,
        )
    }
}

/**
 * Glasmio 的主题跟随：用 App 自己的 `isNightMode`（不是系统的 `isSystemInDarkTheme()`，
 * 这个 App 默认不跟随系统），Glasmio 据此解析 light/dark 材质配方与语义前景色。
 *
 * 只包玻璃这一小块：Glasmio 内部会装一个 M3 `MaterialTheme`，包太大有污染现有 M2 页面的风险；
 * 玻璃控件之外的内容保持原样。
 */
@Composable
fun LiquidGlassTheme(content: @Composable () -> Unit) {
    GlasmioTheme(
        isDark = ExtendedTheme.colors.isNightMode,
        content = content,
    )
}