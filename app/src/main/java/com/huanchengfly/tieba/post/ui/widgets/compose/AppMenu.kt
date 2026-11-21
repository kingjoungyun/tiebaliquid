package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.menuBackground
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalGlassBackdropAvailable
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import io.github.neoruaa.glasmio.glass.LocalBlurBackdrop
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.ui.GlasmioMenu

/** Glasmio 菜单的默认宽度（M2 `DropdownMenu` 的内容宽度一般也在 200–280dp 之间）。 */
private val AppMenuWidth: Dp = 240.dp

private const val AppMenuMeasureSlot = "app-menu-content"

/**
 * 全 App 统一的**玻璃菜单**：外观 / 动画来自 Glasmio 的 `GlasmioMenu`，
 * 对外 API 尽量贴近 `DropdownMenu`，所以老的
 * `DropdownMenu { DropdownMenuItem { ... } }` 基本只改一个函数名。
 *
 * 三条实现路径：
 * 1. 有 backdrop（Activity 级 `GlassRootHost` 或页级 host）+ API 33+ → `GlasmioMenu`；
 *    采样源默认取 `LocalBlurBackdrop`（host 提供；跨窗口坐标由 Glasmio 内部的
 *    `CrossWindowBackdrop` 映射，所以菜单能透出背后真实内容并糊掉细节）。
 * 2. 没有 host / API < 33 → 退回 M2 `DropdownMenu`（与改造前完全一致，绝不崩）。
 *
 * 位置：调用方把它放在**触发器所在的 Box 里**（和以前 `DropdownMenu` 一样），
 * 它把自己在窗口里的位置作为 `externalAnchorBounds` 交给 Glasmio，
 * 菜单贴着这块区域展开（靠边时自动翻到上方），与 M2 行为一致。modifier 只在 M2 回落路径生效。
 *
 * 动效：Glasmio 的「非锚点形变」（缩放 + 模糊 + 阴影弹簧）。
 * 真正的「从触发器液体形变过来」是 `GlasmioMenu` 的 source transfer，要求触发器本身是
 * Glasmio 玻璃控件并作为 `anchor` 槽传入；通用菜单不做这个强约束（TODO：锚点式菜单专用入口）。
 *
 * 高度：GlasmioMenu 的玻璃壳要固定高度，这里用 [SubcomposeLayout] 先量内容高度。
 * 内容会被组合两次（一次量高、一次在 Popup 里真正绘制）；菜单项都是无副作用的行，代价可忽略。
 */
@Composable
fun AppMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    menuWidth: Dp = AppMenuWidth,
    heightOverride: Dp? = null,
    backdrop: Backdrop? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // 没有 backdrop 或 API < 33：保持 M2 行为（和改造前一样用 menuBackground），绝不因为缺 host 抛异常。
    if (!isLiquidGlassSupported() || !LocalGlassBackdropAvailable.current) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier.background(ExtendedTheme.colors.menuBackground),
            content = content,
        )
        return
    }

    val glassBackdrop = backdrop ?: LocalBlurBackdrop.current
    val menuHeight = rememberMenuContentHeight(menuWidth = menuWidth, content = content)
    var anchorBounds by remember { mutableStateOf<Rect?>(null) }

    // 注意 1：玻璃路径**不**把 caller 的 modifier（历史上只用来刷 menuBackground）套到锚点盒子上，
    // 否则会在触发器的位置画出一块矩形。玻璃材质自己就是背景。
    // 注意 2：这个盒子是 0 尺寸的，`boundsInWindow()` 对 0 尺寸节点会直接返回 Rect.Zero，
    // 所以必须用 positionInWindow() + size 自己拼 Rect（否则菜单会被钳到屏幕左上角）。
    Box(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) {
                val position = coordinates.positionInWindow()
                anchorBounds = Rect(
                    left = position.x,
                    top = position.y,
                    right = position.x + coordinates.size.width,
                    bottom = position.y + coordinates.size.height,
                )
            }
        }
    ) {
        GlasmioMenu(
            expanded = expanded,
            onExpandedChange = { opened -> if (!opened) onDismissRequest() },
            // itemCount 只在「没给 heightOverride」时用于估算；这里总是给实测高度。
            itemCount = 1,
            heightOverride = heightOverride ?: menuHeight.takeIf { it > 0.dp },
            menuWidth = menuWidth,
            backdrop = glassBackdrop,
            // 通用菜单没有 Glasmio 玻璃触发器，不做 source transfer 形变。
            morphFromAnchor = false,
            // 触发器是我们布局里的普通内容，别让菜单淡入淡出把它带走。
            keepAnchorVisible = true,
            externalAnchorBounds = anchorBounds,
            anchor = {},
        ) { _, _ ->
            content()
        }
    }
}

/**
 * 量一段菜单内容的自然高度（宽度锁定为 [menuWidth]）。
 *
 * 用 [SubcomposeLayout] 而不是普通布局：内容只用来量高，必须在调用方布局里占 0 尺寸，
 * 否则会把触发器/浮层布局撑开。写回 state 只在数值变化时发生，至多触发一次额外的 layout pass。
 */
@Composable
private fun rememberMenuContentHeight(
    menuWidth: Dp,
    content: @Composable ColumnScope.() -> Unit,
): Dp {
    val heightPx = remember { mutableStateOf(0) }
    SubcomposeLayout { constraints ->
        val widthPx = menuWidth.roundToPx()
            .coerceAtMost(constraints.maxWidth.takeIf { it != Constraints.Infinity } ?: Int.MAX_VALUE)
            .coerceAtLeast(0)
        val placeable = subcompose(AppMenuMeasureSlot) {
            Column(content = content)
        }.firstOrNull()?.measure(
            Constraints(
                minWidth = widthPx,
                maxWidth = widthPx,
                minHeight = 0,
                maxHeight = Constraints.Infinity,
            )
        )
        if (placeable != null && placeable.height != heightPx.value) {
            heightPx.value = placeable.height
        }
        layout(0, 0) {}
    }
    val density = LocalDensity.current
    return with(density) { heightPx.value.toDp() }
}
