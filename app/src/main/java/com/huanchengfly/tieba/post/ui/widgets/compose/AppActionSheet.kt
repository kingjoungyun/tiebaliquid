package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalGlassBackdropAvailable
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.ui.GlasmioActionSheetContent
import io.github.neoruaa.glasmio.ui.GlasmioBottomSheet
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import kotlinx.coroutines.delay
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

/** Action sheet 里的一项。`destructive = true` 用红色（iOS 的破坏性操作）。 */
@Immutable
data class ActionSheetItem(
    val label: String,
    val systemName: String? = null,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * 全 App 统一的 **iOS 风格动作面板**（底部弹出 + 拖拽关闭 + 玻璃材质）。
 *
 * 实现：`GlasmioBottomSheet(fitContent = true)` + `GlasmioActionSheetContent` + `GlasmioListRow`——
 * 与 Glasmio 自己的 sample 里「Overlays → Action Sheet」同一套组合（`IosSheets.kt` 的
 * `GlasmioBottomSheet` 负责 scrim / 拖拽 / 弹簧 / insets，`GlasmioActionSheetContent` 负责面板排版）。
 *
 * 与 [AppMenu]（下拉菜单）并存：菜单适合锚在某个控件上的短列表，动作面板适合从底部升起的
 * 确认/破坏性操作。
 *
 * 背景采样：
 * - 页面有 host（`GlassPageHost`）→ 面板是独立窗口，采样 `LocalBlurBackdrop` 拿到真实内容（玻璃）；
 * - 页面没有 host → 这里**自带一个纯色 host**，面板照样是玻璃（纯色底的玻璃），不会因为
 *   `LocalGlassBackdrop` 的默认 `error(...)` 抛异常；
 * - API < 33 → 退回 M2 `AlertDialog`（极简，只为不崩）。
 *
 * @param items 面板里的操作项（顺序即显示顺序）；最后一行固定是「取消」
 * @param title 面板标题（必填，iOS 面板一定有标题）
 * @param message 标题下的一行说明
 */
@Composable
fun AppActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    items: List<ActionSheetItem>,
    message: String? = null,
    cancelLabel: String = stringResource(R.string.button_cancel),
) {
    // 保留 400ms 让「收起」动画跑完再拆掉（GlasmioBottomSheet 自己处理 visible 的进出动画）。
    var keepAlive by remember { mutableStateOf(visible) }
    LaunchedEffect(visible) {
        if (visible) {
            keepAlive = true
        } else {
            delay(400)
            keepAlive = false
        }
    }
    if (!keepAlive) return

    if (!isLiquidGlassSupported()) {
        if (visible) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(text = title) },
                text = {
                    Column {
                        items.forEach { item ->
                            TextButton(onClick = {
                                onDismiss()
                                item.onClick()
                            }) { Text(text = item.label) }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text(text = cancelLabel) }
                },
            )
        }
        return
    }

    val sheet: @Composable () -> Unit = {
        LiquidGlassTheme(accent = AppTokens.primary) {
            val colors = GlasmioTheme.colors
            GlasmioBottomSheet(
                visible = visible,
                onDismissRequest = onDismiss,
                fitContent = true,
            ) {
                GlasmioActionSheetContent(title = title, message = message) {
                    items.forEachIndexed { index, item ->
                        GlasmioListRow(
                            title = item.label,
                            systemName = item.systemName,
                            showTopSeparator = index > 0,
                            titleColor = if (item.destructive) colors.destructive else Color.Unspecified,
                            onClick = {
                                onDismiss()
                                item.onClick()
                            },
                        )
                    }
                    GlasmioListRow(
                        title = cancelLabel,
                        showTopSeparator = true,
                        titleColor = colors.accent,
                        onClick = onDismiss,
                    )
                }
            }
        }
    }

    if (LocalGlassBackdropAvailable.current) {
        sheet()
    } else {
        // 没有页面 host：自带一个纯色 host（面板是独立窗口，采样纯色底即可，零额外开销）。
        GlasmioBackdropHost(
            sampledColor = glassColors().background,
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        ) {
            sheet()
        }
    }
}
