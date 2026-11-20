package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassSurface
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalLiquidGlassHosted
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.NavigationGlassMaterial
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioIconButton

/**
 * 设置二级页的「悬浮圆形返回钮」：44dp 圆、浮在内容上，替代原来的 M2 顶栏箭头。
 *
 * 这是「二级页范式」的一部分：设置页不再有白色 M2 顶栏（它会和 `groupedBackground` 的浅灰
 * 撞成一条白带），返回改成浮在分组列表左上角的玻璃圆钮，内容从状态栏下方直接起排。
 *
 * 两条实现：
 * 1. 在 [PrefsScreen] 的 backdrop host 里（`LocalLiquidGlassHosted = true`）→
 *    Glasmio 的 [GlasmioIconButton]（与 B 版样板页的返回钮、以及主界面顶栏的玻璃圆钮同一套材质）；
 * 2. 没有 host 时（例如主题选择页这种自绘页、或 API < 33 的降级路径）→
 *    走 App 自己的 [LiquidGlassSurface] 伪玻璃（它会自动降级，绝不会因为缺 host 抛异常）。
 */
@Composable
fun PrefsBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(id = R.string.button_back)
    val size = PrefsGlasmioDefaults.BackButtonSize
    if (LocalLiquidGlassHosted.current && isLiquidGlassSupported()) {
        GlasmioIconButton(
            onClick = onBack,
            modifier = modifier.size(size),
            size = size,
        ) {
            SfIcon(
                systemName = "chevron.left",
                contentDescription = description,
                size = PrefsGlasmioDefaults.BackButtonIconSize,
                mirrored = true,
                tint = GlasmioTheme.colors.content,
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            LiquidGlassSurface(
                modifier = Modifier.matchParentSize(),
                shape = CircleShape,
                material = NavigationGlassMaterial.RegularSmall,
                innerGlow = true,
            ) {}
            SfIcon(
                systemName = "chevron.left",
                contentDescription = description,
                size = PrefsGlasmioDefaults.BackButtonIconSize,
                mirrored = true,
                tint = glassColors().textPrimary,
            )
        }
    }
}

/**
 * 悬浮返回钮在页面里的固定落位：左上角、状态栏下方、与卡片的 16dp 页面留白对齐。
 *
 * 必须在 `Box` 里调用（用 `align`）；给 [PrefsScreen] 之外的设置页（主题选择、关于）
 * 复用，保证 8 个二级页的返回钮位置完全一致。
 */
@Composable
fun BoxScope.PrefsBackButtonOverlay(onBack: () -> Unit) {
    PrefsBackButton(
        onBack = onBack,
        modifier = Modifier
            .align(Alignment.TopStart)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                start = PrefsGlasmioDefaults.BackButtonStart,
                top = PrefsGlasmioDefaults.BackButtonTop,
            ),
    )
}
