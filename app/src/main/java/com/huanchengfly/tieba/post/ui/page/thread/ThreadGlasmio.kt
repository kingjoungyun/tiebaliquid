package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassBackdropHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import io.github.neoruaa.glasmio.core.GlasmioTheme

/**
 * 帖子详情页的 Glasmio 外壳（`ThreadPageUiStyle.GLASMIO`）。
 *
 * 这一层只做三件事，楼层内容一行都不动：
 * 1. 注入 [LiquidGlassTheme]（App 自己的明暗 / 主题色 / 鸿蒙字表）；
 * 2. 页级 backdrop host：页面内容进 `sampledContent`（被录进 LayerBackdrop），
 *    页面里的玻璃浮层（顶栏悬浮钮、菜单、底部操作栏）就能采到这一页的真实内容；
 * 3. 给 [topBar] 一个 **overlay 槽**：它画在录制层之上，因此可以安全地采样整页，
 *    也才能做到「内容从顶栏下穿过」。
 *
 * 和 `GlassPageHost` 的区别只有第 3 点——`GlassPageHost` 只接受页面内容，没有 overlay 槽。
 * 页面里的玻璃控件（普通内容层）仍然按 `LocalGlassSamplingSafe=false` 走
 * `GlassIconButton` 自带的纯色小 host，不会自己采样自己。
 *
 * 背景画在 `sampledContent` **里面**（而不是 host 的 modifier 上），否则 backdrop 里只有
 * 透明底，玻璃顶栏/菜单采样出来会发黑。
 */
@Composable
fun ThreadPageSkin(
    enabled: Boolean,
    topBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    if (!enabled || !isLiquidGlassSupported()) {
        content()
        return
    }
    LiquidGlassTheme(accent = ExtendedTheme.colors.primary) {
        LiquidGlassBackdropHost(
            modifier = Modifier.fillMaxSize(),
            sampledContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GlasmioTheme.colors.groupedBackground)
                ) {
                    content()
                }
            },
            overlayContent = { topBar() },
        )
    }
}
