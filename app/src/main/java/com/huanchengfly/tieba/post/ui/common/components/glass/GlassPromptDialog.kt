package com.huanchengfly.tieba.post.ui.common.components.glass

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogScope
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideContentColor
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalLiquidGlassHosted

/**
 * 带输入框的玻璃弹窗（T2）：`AnyPopDialog` + 玻璃卡片 + [GlassTextField] + 两个玻璃按钮。
 *
 * **对外签名与 `ui/widgets/compose/Dialogs.kt` 的 `PromptDialog` 完全一致**
 * （含 `onValueChange` 这个「返回 false 就不接受这次改动」的钩子、`initialValue` 每次显示时重置），
 * 所以试点页只换函数名，回滚时换回来即可。
 *
 * 与原来 `PromptDialog` 的差异只在**壳**：原来是 M2 共享 `Dialog`（白框 / 半玻璃 + 两个
 * 满宽 `TextButton`），这里是玻璃卡片 + 17sp 标题 + 并排的取消/确定胶囊按钮 + 玻璃输入框。
 * 输入框仍然自动聚焦并弹键盘（保持原来的时序：聚焦后 300ms 弹 IME）。
 */
@Composable
fun GlassPromptDialog(
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
    dialogState: DialogState = rememberDialogState(),
    initialValue: String = "",
    onValueChange: (newVal: String, oldVal: String) -> Boolean = { _, _ -> true },
    onCancel: (() -> Unit)? = null,
    confirmText: String = stringResource(id = R.string.button_sure_default),
    cancelText: String = stringResource(id = R.string.button_cancel),
    title: (@Composable (DialogScope.() -> Unit))? = null,
    content: @Composable (DialogScope.() -> Unit) = {},
) {
    var textVal by remember { mutableStateOf(initialValue) }
    // 每次显示时重置输入框内容（与 PromptDialog 的 LaunchedEffect(dialogState.show) 等价）
    LaunchedEffect(dialogState.show) {
        if (dialogState.show) textVal = initialValue
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

        if (content !== EmptyPromptContent) {
            ProvideTextStyle(value = MaterialTheme.typography.body2) {
                ProvideContentColor(color = AppTokens.textSecondary) {
                    with(dialogScope) { content() }
                }
            }
        }

        val focusRequester = remember { FocusRequester() }
        val softwareKeyboardController = LocalSoftwareKeyboardController.current
        // 输入框自己不会开 host：弹窗里若没有 backdrop（实测会退化成蓝色描边的 M3 输入框，
        // 与玻璃弹窗风格割裂），就在这里补一个纯色 host。
        val field = @Composable {
            GlassTextField(
                value = textVal,
                onValueChange = {
                    if (onValueChange.invoke(it, textVal)) textVal = it
                },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { softwareKeyboardController?.hide() }
                ),
                focusRequester = focusRequester,
                hostFill = AppTokens.windowBackground,
            )
        }
        if (LocalLiquidGlassHosted.current) {
            field()
        } else {
            GlasmioBackdropHost(sampledColor = AppTokens.windowBackground) { field() }
        }
        LaunchedEffect(focusRequester, dialogState.show) {
            if (!dialogState.show) return@LaunchedEffect
            focusRequester.requestFocus()
            launch {
                delay(300)
                softwareKeyboardController?.show()
            }
        }

        GlassDialogButtons(
            cancelText = cancelText,
            onCancel = {
                onCancel?.invoke()
                dialogState.show = false
            },
            confirmText = confirmText,
            onConfirm = {
                onConfirm.invoke(textVal)
                dialogState.show = false
            },
        )
    }
}

private val EmptyPromptContent: @Composable (DialogScope.() -> Unit) = {}
