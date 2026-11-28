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
    BaseDialog(
        modifier = modifier,
        dialogState = dialogState,
        onDismiss = onDismiss,
        direction = if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
            DirectionState.BOTTOM
        } else {
            DirectionState.CENTER
        },
        cancelable = cancelable,
        cancelableOnTouchOutside = cancelableOnTouchOutside,
    ) {
        // P2：共享弹窗外壳（玻璃 / 老白框共用同一份布局，只是外壳不同）。
        // 所有 AlertDialog / ConfirmDialog / PromptDialog / TimePickerDialog 都走这里，
        // 所以玻璃化只改这一处，业务调用点零改动。
        val cardContent: @Composable (Modifier) -> Unit = { cardModifier ->
            ConstraintLayout(modifier = cardModifier) {
            val (titleRef, contentRef, buttonsRef) = createRefs()
            Column(
                modifier = Modifier
                    .constrainAs(titleRef) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                        visibility = if (title == null) {
                            Visibility.Gone
                        } else {
                            Visibility.Visible
                        }
                    }
            ) {
                if (title != null) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        ProvideTextStyle(
                            value = androidx.compose.ui.text.TextStyle(
                                fontSize = 17.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTokens.text,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        ) {
                            title()
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .constrainAs(contentRef) {
                        top.linkTo(titleRef.bottom, margin = 16.dp, goneMargin = 0.dp)
                        bottom.linkTo(buttonsRef.top, margin = 16.dp, goneMargin = 0.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        height = Dimension.wrapContent
                    }
            ) {
                content()
            }
            Column(
                modifier = Modifier
                    .constrainAs(buttonsRef) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.fillToConstraints
                        // 注意：这里**不能**再加 top.linkTo(contentRef.bottom)。
                        // ConstraintLayout 里同时给上下两条约束又没给高度 = 按钮区被拉伸，
                        // 卡片底部会出现一大块空白（本轮踩过）。底部对齐 parent.bottom + 卡片
                        // 高度 wrapContent 时，按钮天然贴在内容下方。
                    }
                    .drawBehind {
                        // 内容与按钮区之间的发丝线（按钮自身再各画一条顶部线）
                        drawLine(
                            color = AppTokens.divider,
                            strokeWidth = 1.dp.toPx() / 2f,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                        )
                    },
            ) {
                buttons()
            }
            }
        }
        val cardOuterModifier = modifier
            .wrapContentHeight()
            .animateContentSize()
            .fillMaxWidth(
                fraction = if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
                    1f
                } else {
                    0.6f
                }
            )
            .padding(16.dp)
        if (dialogUsesGlassShell()) {
            val cardShape = RoundedCornerShape(26.dp)
            // 关键：玻璃表面必须放在 matchParentSize() 层，**不参与测量**。
            // 一旦把它当容器（内容放它里面），它会被外层撑成一屏高 —— 卡片底部就出现一大块空白
            // （列表弹窗上最明显）。这里用 Box 让 cardOuterModifier（wrapContentHeight）决定高度，
            // 玻璃与可读性实底都铺在它上面 —— 与 GlassDialogCard 同一套结构。
            Box(modifier = cardOuterModifier) {
                LiquidGlassSurface(
                    modifier = Modifier.matchParentSize(),
                    shape = cardShape,
                    // 苹果的 alert 是很厚的材质：Medium 档（文档口径「Medium 服务菜单和 alert」）
                    material = NavigationGlassMaterial.RegularMedium,
                    innerGlow = true,
                ) {}
                // 可读性实底：玻璃只负责质感 + 边缘高光/折射，正文要能读清
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            color = AppTokens.windowBackground.copy(
                                alpha = if (AppTokens.isNightMode) 0.72f else 0.62f
                            ),
                            shape = cardShape,
                        )
                )
                // 弹窗是独立 Window，不在页面的 backdrop host 里。
                // 这里给卡片内容补一个 host，弹窗内**所有**玻璃控件（输入框 / 开关 / 勾选行）
                // 就都有采样源，不用逐个兜底（同 PrefsSwitch 的修法）。
                GlasmioBackdropHost(sampledColor = AppTokens.windowBackground) {
                    cardContent(Modifier.fillMaxWidth())
                }
            }
        } else {
            // 降级 / A-B：改造前的白框
            cardContent(
                cardOuterModifier
                    .background(
                        color = AppTokens.windowBackground,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(vertical = 24.dp)
            )
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
