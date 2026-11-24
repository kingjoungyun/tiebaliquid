package com.huanchengfly.tieba.post.ui.common.components.glass

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

/** 单行玻璃胶囊的高度。 */
val GlassTextFieldHeight: Dp = 44.dp

/** 多行玻璃卡片的最小高度。 */
val GlassTextFieldMinHeightMultiline: Dp = 96.dp

private val GlassTextFieldCornerSingle = 22.dp
private val GlassTextFieldCornerMulti = 16.dp

/**
 * 玻璃输入框（T2）：Glasmio 材质 + AppTokens，单行 / 多行两种形态。
 *
 * - **单行**：44dp 玻璃胶囊（`LiquidGlassSurface`，tint = `AppTokens.card`），
 *   左侧图标槽、右侧清除/动作槽，placeholder 用次级色；
 * - **多行**：16dp 圆角玻璃卡片，最小 96dp，右下角带 `已输入/上限` 计数槽
 *   （[maxLength] 非空时同时强制截断）；
 * - **聚焦**：accent 1.5dp 描边（画在玻璃之上，不改玻璃材质，所以不会闪）；
 * - **降级**：API < 33，或页面没有活 backdrop（本组件自己起纯色 host）→
 *   `LiquidGlassSurface` 自动退回伪玻璃，布局与内容完全不变。
 *
 * 不用 M2 的 `TextField`：它自带下划线/填充色/内置 padding，玻璃化需要把
 * 颜色、描边、hint 全部重写一遍，反而比裸 `BasicTextField` 更绕。
 *
 * ## `chrome = false`（T3 新增）
 *
 * 只出「输入本体」（文字 / placeholder / 光标 / IME / 计数），**不画自己的玻璃壳**，
 * 用于嵌进调用方已经有的壳里：
 * - `SearchBox`（外壳是 Surface/玻璃 + 前置/后置图标 + 清除/提交按钮）；
 * - `EditProfileActivity` 的 `GlassField`（昵称/签名行的壳）；
 * - `ReplyPage` 的 `ReplyGlassInput`（回帖编辑器，壳里还要放图片面板联动）。
 *
 * 这样这些地方能继续用自己的外壳与布局，同时把「输入」的逻辑收在同一个组件里，
 * 不必再维护一份 M2 `BaseTextField` 分叉。
 *
 * @param maxLength 非空时限制输入长度并在右下角显示计数
 * @param showCounter 多行时是否显示计数（未给 [maxLength] 时显示纯字数）
 * @param hostFill 自带采样 host 的底色（组件所在页面的底色）
 * @param chrome 是否自带玻璃壳；false 时只有输入本体，壳与内边距由调用方给
 * @param contentPadding `chrome = false` 时输入本体的内边距
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    placeholderContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    maxLines: Int = 1,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    maxLength: Int? = null,
    showCounter: Boolean = maxLines > 1,
    counterText: ((String) -> String)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = TextStyle(fontSize = 15.sp),
    shape: Shape? = null,
    hostFill: Color = AppTokens.groupedBackground,
    focusRequester: FocusRequester? = null,
    interactionSource: MutableInteractionSource? = null,
    chrome: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
) {
    val singleLine = maxLines == 1
    val corner = shape ?: RoundedCornerShape(
        if (singleLine) GlassTextFieldCornerSingle else GlassTextFieldCornerMulti
    )
    val resolvedTextStyle = textStyle.copy(
        color = if (enabled) AppTokens.text else AppTokens.textDisabled,
    )
    var focused by remember { mutableStateOf(false) }
    val fieldModifier = Modifier
        .fillMaxWidth()
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .onFocusChanged { focused = it.isFocused }

    val inner: @Composable () -> Unit = {
        if (singleLine) {
            Row(
                modifier = if (chrome) {
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(contentPadding)
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Box(modifier = Modifier.padding(end = 8.dp)) { leadingIcon() }
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        if (placeholderContent != null) {
                            placeholderContent()
                        } else if (placeholder != null) {
                            Text(
                                text = placeholder,
                                style = resolvedTextStyle.copy(color = AppTokens.textSecondary),
                                maxLines = 1,
                            )
                        }
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = {
                            onValueChange(maxLength?.let(it::take) ?: it)
                        },
                        modifier = fieldModifier,
                        enabled = enabled,
                        readOnly = readOnly,
                        singleLine = true,
                        textStyle = resolvedTextStyle,
                        cursorBrush = SolidColor(AppTokens.primary),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        visualTransformation = visualTransformation,
                        interactionSource = interactionSource,
                    )
                }
                if (trailingIcon != null) {
                    Box(modifier = Modifier.padding(start = 8.dp)) { trailingIcon() }
                }
            }
        } else {
            Column(
                modifier = if (chrome) {
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(contentPadding)
                },
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        if (placeholderContent != null) {
                            placeholderContent()
                        } else if (placeholder != null) {
                            Text(
                                text = placeholder,
                                style = resolvedTextStyle.copy(color = AppTokens.textSecondary),
                            )
                        }
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = {
                            onValueChange(maxLength?.let(it::take) ?: it)
                        },
                        modifier = fieldModifier,
                        enabled = enabled,
                        readOnly = readOnly,
                        maxLines = maxLines,
                        minLines = minLines,
                        textStyle = resolvedTextStyle,
                        cursorBrush = SolidColor(AppTokens.primary),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        visualTransformation = visualTransformation,
                        interactionSource = interactionSource,
                    )
                }
                if (showCounter) {
                    Text(
                        text = if (maxLength != null) "${value.length}/$maxLength"
                        else "${value.length}",
                        fontSize = 11.sp,
                        color = AppTokens.textSecondary,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                    )
                }
            }
        }
    }

    if (!chrome) {
        // 无壳：壳由调用方提供（搜索框 / 设置行 / 回帖编辑器）。
        // 单行时在调用方给的槽位里垂直居中（SearchBox 传的是 fillMaxHeight）。
        Column(
            modifier = modifier,
            verticalArrangement = if (singleLine) Arrangement.Center else Arrangement.Top,
        ) {
            inner()
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        GlassComponentSurface(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (singleLine) Modifier.height(GlassTextFieldHeight)
                    else Modifier.heightIn(min = GlassTextFieldMinHeightMultiline)
                ),
            shape = corner,
            tint = AppTokens.card,
            hostFill = hostFill,
            innerGlow = true,
        ) {
            inner()
            if (focused) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(width = 1.5.dp, color = AppTokens.primary, shape = corner)
                )
            }
        }
    }
}
