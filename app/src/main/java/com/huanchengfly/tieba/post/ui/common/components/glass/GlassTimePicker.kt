package com.huanchengfly.tieba.post.ui.common.components.glass

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogScope
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideContentColor
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ItemStyles
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.TimePicker
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState

/** 时间轮的高度（与原 `TimePickerDialog` 一致）。 */
private val GlassTimePickerHeight = 150.dp

/**
 * 玻璃时间选择弹窗（T2）。
 *
 * **不重写滚轮算法**：滚动、吸附、`PickerTime` 解析全部复用
 * `ui/widgets/compose/picker/TimePicker.kt`（原 `TimePickerDialog` 用的就是它），
 * 这一层只换外壳：
 * - 外壳：组件层的 [GlassDialogCard]（`AnyPopDialog` + 跨窗口 backdrop + 玻璃卡片），
 *   取代原来 M2 共享 `Dialog` 的白框；
 * - 标题：`GlassDialogTitle`（17sp / w600），调用方仍传 composable，写法不变；
 * - 底部：`取消` / `确定` 两个玻璃按钮并排（原来是一上一下两个 M2 `TextButton`）。
 *
 * 参数与 `ui/widgets/compose/Dialogs.kt` 的 `TimePickerDialog` 一一对应，
 * 所以试点页只需要换函数名（回滚时换回来即可）。
 */
@Composable
fun GlassTimePickerDialog(
    currentTime: String,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable (DialogScope.() -> Unit))? = null,
    dialogState: DialogState = rememberDialogState(),
    onValueChange: ((String) -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    is24TimeFormat: Boolean = true,
    confirmText: String = stringResource(id = R.string.button_sure_default),
    cancelText: String = stringResource(id = R.string.button_cancel),
    content: @Composable (DialogScope.() -> Unit) = {},
) {
    var timeVal by remember { mutableStateOf(currentTime) }
    // 每次重新打开时回到外部传入的当前值（与原 TimePickerPref 里的 reset 等价）
    LaunchedEffect(dialogState.show, currentTime) {
        if (dialogState.show) timeVal = currentTime
    }
    val dialogScope = remember(dialogState) {
        DialogScope(onDismiss = { dialogState.show = false })
    }

    GlassDialogCard(
        dialogState = dialogState,
        modifier = modifier,
        onDismiss = onCancel,
    ) {
        if (title != null) {
            GlassDialogTitle {
                ProvideContentColor(color = AppTokens.text) {
                    with(dialogScope) { title() }
                }
            }
        }

        ProvideTextStyle(value = MaterialTheme.typography.body2) {
            ProvideContentColor(color = AppTokens.textSecondary) {
                with(dialogScope) { content() }
            }
        }

        TimePicker(
            currentTime = timeVal,
            onTimeChanged = {
                timeVal = it
                onValueChange?.invoke(it)
            },
            is24TimeFormat = is24TimeFormat,
            modifier = Modifier
                .fillMaxWidth()
                .height(GlassTimePickerHeight),
            itemStyles = glassTimePickerItemStyles(),
        )

        GlassDialogButtons(
            cancelText = cancelText,
            onCancel = {
                onCancel?.invoke()
                dialogState.show = false
            },
            confirmText = confirmText,
            onConfirm = {
                onConfirm.invoke(timeVal)
                dialogState.show = false
            },
        )
    }
}

/**
 * 时间轮的文字样式：默认 16sp 次级色、选中 22sp 主文字色。
 *
 * 尺寸比原来的 14sp/24sp 更贴近 iOS 时间轮（大字号选中项 + 次级未选中项），
 * 滚动与吸附逻辑完全没动。
 */
@Composable
private fun glassTimePickerItemStyles(): ItemStyles = ItemStyles(
    defaultTextStyle = MaterialTheme.typography.body1.copy(
        color = AppTokens.textSecondary,
        fontSize = 16.sp,
    ),
    selectedTextStyle = MaterialTheme.typography.h5.copy(
        color = AppTokens.text,
        fontSize = 22.sp,
    ),
)
