package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsCardDefaults
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalLiquidGlassHosted
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import com.huanchengfly.tieba.post.ui.widgets.compose.Switch
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.ui.GlasmioSwitch
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

/**
 * 设置页的开关（Glasmio 优先）。
 *
 * 为什么不能直接 `GlasmioSwitch(...)`：它默认从 `LocalGlassBackdrop` 取采样源，而这个
 * CompositionLocal 的默认实现是 `error(...)`（没有 host 直接抛异常）。设置页是独立目的地，
 * 不在 `MainPage` 的玻璃 host 里，所以这里给它就地搭一个**只采样单一底色**的 host：
 * Glasmio 的文档里这条路径就是给「滚动内容里的控件」用的（`fillsBoundsUniformly`，
 * 移动时能复用已渲染结果，不会每帧重录）。
 *
 * 玻璃只用于这个控件本身（iOS 26/27 的开关就是玻璃），卡片与列表仍然是纯色。
 *
 * 尺寸 63x28dp 是 Glasmio 0.1.3 里 `GlasmioSwitch` 的固定尺寸；host 必须与它同尺寸，
 * 否则「只采样底色」的那层会按 Row 剩余宽度铺开。
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PrefsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = glassColors()
    // accent 的唯一来源是 App 主题色。glassColors() 已经把 primary 换成它，这里再显式兜底一次，
    // 避免预览/未初始化主题时拿到 Color.Unspecified 导致开关轨道透明。
    val accent = AppTokens.primary.let { if (it == Color.Unspecified) colors.primary else it }
    if (isLiquidGlassSupported()) {
        val switch: @Composable () -> Unit = {
            GlasmioSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                // 不再用 Glasmio 默认的 iOS 系统绿：跟随 App 主题色。
                onTintColor = accent,
            )
        }
        if (LocalLiquidGlassHosted.current) {
            // 已经在玻璃 host 里（例如将来的底部导航容器内）：直接采样真实页面
            switch()
        } else {
            GlasmioBackdropHost(
                sampledColor = PrefsCardDefaults.surface(),
                modifier = modifier.size(SwitchWidth, SwitchHeight),
            ) {
                switch()
            }
        }
    } else {
        // 兜底：API < 33 时 Glasmio 的 shader 起不来，退回 App 自己的开关（本工程 minSdk=33，
        // 这条分支目前只用于极端情况，保持行为一致而不是崩掉）
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            enabled = enabled,
            modifier = modifier,
        )
    }
}

private val SwitchWidth = 63.dp
private val SwitchHeight = 28.dp
