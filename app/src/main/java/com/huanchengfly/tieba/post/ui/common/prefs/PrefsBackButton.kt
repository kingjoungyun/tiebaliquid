package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassIconButton
import io.github.neoruaa.glasmio.icons.SfIcon

/**
 * 设置二级页的「悬浮圆形返回钮」：44dp 圆、浮在内容上，替代原来的 M2 顶栏箭头。
 *
 * 这是「二级页范式」的一部分：设置页不再有白色 M2 顶栏（它会和 `groupedBackground` 的浅灰
 * 撞成一条白带），返回改成浮在分组列表左上角的玻璃圆钮，内容从状态栏下方直接起排。
 *
 * 实现已经收敛到全 App 统一的 [GlassIconButton]（它自己处理「在 host 里 / 不在 host 里 /
 * API < 33」三种采样环境），这里只负责图标 + 无障碍描述。
 */
@Composable
fun PrefsBackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(id = R.string.button_back)
    GlassIconButton(
        onClick = onBack,
        contentDescription = description,
        modifier = modifier,
        size = PrefsGlasmioDefaults.BackButtonSize,
    ) { tint ->
        SfIcon(
            systemName = "chevron.left",
            contentDescription = description,
            size = PrefsGlasmioDefaults.BackButtonIconSize,
            mirrored = true,
            tint = tint,
        )
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
