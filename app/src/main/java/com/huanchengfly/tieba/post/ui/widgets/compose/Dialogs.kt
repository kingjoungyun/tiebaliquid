package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassSurface
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import io.github.neoruaa.glasmio.glass.NavigationGlassMaterial
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.TimePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.CompositionLocalProvider
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalLiquidGlassHosted
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

@Composable
fun DialogScope.DialogPositiveButton(
    text: String,
    onClick: () -> Unit = {}
) {
    DialogTextButton(
        text = text,
        color = AppTokens.primary,
        onClick = {
            onClick()
            dismiss()
        },
    )
}

@Composable
fun DialogScope.DialogNegativeButton(
    text: String,
    onClick: (() -> Unit)? = null
) {
    DialogTextButton(
        text = text,
        color = AppTokens.text,
        onClick = {
            dismiss()
            onClick?.invoke()
        },
    )
}

/**
 * 弹窗里的一个文字按钮（iOS action-sheet 风格）。
 *
 * 选这个形态而不是「两个胶囊按钮」的原因：本工程的 `AnyPopDialog` 在手机上是从**底部**弹出的
 * （`DirectionState.BOTTOM`），和 iOS action sheet 一致；action sheet 的按钮就是「整宽、文字居中、
 * 相邻按钮之间一条 0.5dp 分隔线」，不需要实色底。宽屏居中时同一套按钮仍然成立（只是变成弹窗底部两行）。
 *
 * 交互：52dp 行高、按下时 6% 主文字色压底（无涟漪）、禁用态不在此处理。
 */
@Composable
private fun DialogTextButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                color = if (pressed) AppTokens.text.copy(alpha = 0.06f) else Color.Transparent
            )
            .drawBehind {
                // 顶部发丝线：相邻按钮之间自然形成分隔（第一个按钮由容器再补一条）
                drawLine(
                    color = AppTokens.divider,
                    strokeWidth = 1.dp.toPx() / 2f,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                )
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
fun AlertDialog(
    dialogState: DialogState,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    confirmText: String = stringResource(id = R.string.button_ok),
    title: @Composable (DialogScope.() -> Unit) = {},
    content: @Composable (DialogScope.() -> Unit) = {},
) {
    Dialog(
        modifier = modifier,
        dialogState = dialogState,
        onDismiss = onDismiss,
        title = title,
        buttons = {
            DialogPositiveButton(text = confirmText)
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            ProvideTextStyle(
                value = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = AppTokens.textSecondary,
                )
            ) {
                ProvideContentColor(color = AppTokens.text) {
                    content()
                }
            }
        }
    }
}

@Composable
fun AlertDialog(
    dialogState: DialogState,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    title: @Composable (DialogScope.() -> Unit) = {},
    content: @Composable (DialogScope.() -> Unit) = {},
    buttons: @Composable (DialogScope.() -> Unit) = {},
) {
    Dialog(
        modifier = modifier,
        dialogState = dialogState,
        onDismiss = onDismiss,
        title = title,
        buttons = buttons,
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            ProvideTextStyle(
                value = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = AppTokens.textSecondary,
                )
            ) {
                ProvideContentColor(color = AppTokens.text) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    dialogState: DialogState,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    confirmText: String = stringResource(id = R.string.button_sure_default),
    cancelText: String = stringResource(id = R.string.button_cancel),
    title: @Composable (DialogScope.() -> Unit)? = null,
    content: @Composable (DialogScope.() -> Unit) = {},
) {
    Dialog(
        modifier = modifier,
        dialogState = dialogState,
        onDismiss = onDismiss,
        title = title,
        buttons = {
            DialogPositiveButton(text = confirmText, onClick = onConfirm)
            DialogNegativeButton(text = cancelText, onClick = onCancel)
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            ProvideTextStyle(
                value = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = AppTokens.textSecondary,
                )
            ) {
                ProvideContentColor(color = AppTokens.text) {
                    content()
                }
            }
        }
    }
}

@Composable
fun BaseDialog(
    modifier: Modifier = Modifier,
    dialogState: DialogState = rememberDialogState(),
    onDismiss: (() -> Unit)? = null,
    direction: DirectionState = DirectionState.BOTTOM,
    cancelable: Boolean = true,
    cancelableOnTouchOutside: Boolean = true,
    imePadding: Boolean = true,
    content: @Composable (DialogScope.() -> Unit),
) {
    var showDialog by remember {
        mutableStateOf(false)
    }
    var isActiveClose by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(dialogState.show) {
        if (dialogState.show) {
            showDialog = true
            isActiveClose = false
        } else {
            isActiveClose = true
        }
    }
    if (showDialog) {
        val dialogScope = DialogScope(
            onDismiss = {
                isActiveClose = true
            },
        )
        AnyPopDialog(
            isActiveClose = isActiveClose,
            onDismiss = {
                onDismiss?.invoke()
                dialogState.show = false
                showDialog = false
            },
            properties = AnyPopDialogProperties(
                direction = direction,
                dismissOnBackPress = cancelable,
                dismissOnClickOutside = cancelableOnTouchOutside,
                imePadding = imePadding
            )
        ) {
            ProvideContentColor(color = AppTokens.text) {
                dialogScope.content()
            }
        }
    }
}

@Composable
fun Dialog(
    modifier: Modifier = Modifier,
    dialogState: DialogState = rememberDialogState(),
    onDismiss: (() -> Unit)? = null,
    cancelable: Boolean = true,
    cancelableOnTouchOutside: Boolean = true,
    title: @Composable (DialogScope.() -> Unit)? = null,
    buttons: @Composable (DialogScope.() -> Unit) = {},
    content: @Composable (DialogScope.() -> Unit),
) {
    val windowWidthSizeClass = LocalWindowSizeClass.current.widthSizeClass
    val compact = windowWidthSizeClass == WindowWidthSizeClass.Compact
    val dialogScope = remember(dialogState) {
        DialogScope(onDismiss = { dialogState.show = false })
    }
    BaseDialog(
        modifier = modifier,
        dialogState = dialogState,
        onDismiss = onDismiss,
        direction = if (compact) DirectionState.BOTTOM else DirectionState.CENTER,
        cancelable = cancelable,
        cancelableOnTouchOutside = cancelableOnTouchOutside,
    ) {
        // 卡片形态（v1.9.2）：**实底卡片**，不再用 LiquidGlassSurface 铺底。
        //
        // 为什么放弃玻璃底：`LiquidGlassSurface` 一旦参与测量（哪怕是 matchParentSize 铺在
        // Box 里），在 `AnyPopDialog` 里都会被撑成整屏高 —— 卡片变成「一张通栏白/黑板」，
        // 圆角和内边距全丢（本轮踩了两次，列表/确认弹窗都复现）。iOS 的 alert 本身就是
        // 近乎不透明的卡，实底 + 1dp 发丝描边已经足够，且测量行为完全可预期。
        //
        // 卡片里仍然给内容补 `GlasmioBackdropHost`：输入框 / 开关 / 勾选行这些玻璃控件
        // 需要采样源（独立 Dialog 窗口不在页面 host 里）。
        val shape = RoundedCornerShape(26.dp)
        Box(
            modifier = modifier
                .wrapContentHeight()
                .animateContentSize()
                .fillMaxWidth(if (compact) 1f else 0.6f)
                // 底部不能贴屏幕边缘：iOS 的 alert / action sheet 离底部有「导航栏 + 8~16pt」。
                // 实测弹窗窗口里 systemBarsPadding() 的导航栏 insets 是 0（decorFitsSystemWindows=false），
                // 所以显式给一段底部留白：贴底布局 16dp，底部弹出再 +24dp。
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (compact) 40.dp else 16.dp)
                .clip(shape)
                .background(AppTokens.windowBackground)
                .border(width = 1.dp, color = AppTokens.divider, shape = shape),
        ) {
            // 注意：这里**不能**再包 GlasmioBackdropHost。
            // 它的实现是 Box { Box(Modifier.fillMaxSize()…) + content }，
            // 里面那个 fillMaxSize 会把卡片撑成整屏高（本轮第三次踩：卡片变通栏白板、圆角丢失）。
            // 弹窗里的玻璃控件（GlassTextField 等）自己会开 host，不需要外层再包。
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (title != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            ProvideTextStyle(
                                TextStyle(
                                    fontSize = 17.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppTokens.text,
                                    textAlign = TextAlign.Center,
                                )
                            ) {
                                with(dialogScope) { title() }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 24.dp,
                                end = 24.dp,
                                top = if (title == null) 20.dp else 0.dp,
                                bottom = 8.dp,
                            ),
                    ) {
                        with(dialogScope) { content() }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .drawBehind {
                                drawLine(
                                    color = AppTokens.divider,
                                    strokeWidth = 1.dp.toPx() / 2f,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                )
                            },
                    ) {
                        with(dialogScope) { buttons() }
                    }
                }
        }
    }
}

/**
 * 共享弹窗外壳是否玻璃化（P2）。
 *
 * 收编链路：`Dialog` → `BaseDialog` → `AnyPopDialog`（已带跨窗口 backdrop），
 * 因此 `AlertDialog` / `ConfirmDialog` / `PromptDialog` / `TimePickerDialog` 的**所有**调用点
 * 一次性拿到玻璃壳，业务代码零改动。
 *
 * 降级：API < 33 → 老白框（`windowBackground`），行为不变。
 * 调试开关（仅 debug 包）：`adb shell settings put global tieba_dlg_flat 1` 强制回白框做 A/B。
 */
@Composable
private fun dialogUsesGlassShell(): Boolean {
    if (!isLiquidGlassSupported()) return false
    if (!BuildConfig.DEBUG) return true
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getInt(context.contentResolver, "tieba_dlg_flat", 0) == 0
        }.getOrDefault(true)
    }
}

@Composable
fun rememberDialogState(): DialogState {
    return rememberSaveable(saver = DialogState.Saver) {
        DialogState()
    }
}

@Stable
class DialogState private constructor(
    show: Boolean
) {
    constructor() : this(show = false)

    var show by mutableStateOf(show)

    fun show() {
        show = true
    }

    companion object {
        val Saver: Saver<DialogState, *> = listSaver(
            save = {
                listOf<Any>(
                    it.show,
                )
            },
            restore = {
                DialogState(
                    show = it[0] as Boolean
                )
            }
        )
    }
}

class DialogScope(
    private val onDismiss: () -> Unit,
) {
    fun dismiss() {
        onDismiss()
    }
}
