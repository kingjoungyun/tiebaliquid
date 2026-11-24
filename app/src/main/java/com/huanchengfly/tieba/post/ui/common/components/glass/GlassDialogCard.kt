package com.huanchengfly.tieba.post.ui.common.components.glass

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState

/** 玻璃弹窗的圆角。 */
val GlassDialogCorner = 24.dp

/** 弹窗按钮高度。 */
val GlassDialogButtonHeight = 44.dp

/**
 * Glass 组件层共用的**玻璃弹窗外壳**（T2）。
 *
 * 结构与 `ui/widgets/compose/Dialogs.kt` 里的共享壳一致（同一套 `AnyPopDialog` +
 * `DialogState` 显隐动画），只是内容换成组件层的玻璃卡片：
 * - 外壳：`AnyPopDialog`（独立 Window + 跨窗口 backdrop，`GlassDialogBackdropScope`
 *   会把 `LocalLiquidGlassHosted` 置真，所以卡片里是真玻璃而不是白盘）；
 * - 卡片：`GlassComponentSurface`（圆角 24dp，tint = `windowBackground`）+ 一层高不透明度
 *   实底保证文字可读（玻璃只负责质感与边缘高光）；
 * - 显隐动画：沿用 `BaseDialog` 的那套 `showDialog` / `isActiveClose` 状态机，行为不变。
 *
 * 手机（compact）从底部弹出，宽屏居中，与 `Dialog()` 的判断一致。
 */
@Composable
fun GlassDialogCard(
    dialogState: DialogState,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    cancelable: Boolean = true,
    cancelableOnTouchOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var isActiveClose by remember { mutableStateOf(false) }
    LaunchedEffect(dialogState.show) {
        if (dialogState.show) {
            showDialog = true
            isActiveClose = false
        } else {
            isActiveClose = true
        }
    }
    if (!showDialog) return

    val windowWidthSizeClass = LocalWindowSizeClass.current.widthSizeClass
    val direction = if (windowWidthSizeClass == WindowWidthSizeClass.Compact) {
        DirectionState.BOTTOM
    } else {
        DirectionState.CENTER
    }
    val shape = RoundedCornerShape(GlassDialogCorner)

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
        ),
    ) {
        GlassComponentSurface(
            modifier = modifier
                .wrapContentHeight()
                .animateContentSize()
                .fillMaxWidth()
                .padding(16.dp),
            shape = shape,
            tint = AppTokens.windowBackground,
            hostFill = AppTokens.windowBackground,
            innerGlow = true,
        ) {
            // 可读性底：玻璃下面垫一层 windowBackground 的高不透明度实底
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = AppTokens.windowBackground.copy(
                            alpha = if (AppTokens.isNightMode) 0.86f else 0.85f
                        ),
                        shape = shape,
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}

/**
 * 弹窗标题：17sp / w600 + 主文字色，与 iOS alert 的标题档位一致。
 *
 * 调用方仍然传 `@Composable` 的标题内容（和 `Dialog(title = { ... })` 的写法一样），
 * 这里只提供文字样式，所以迁移时不用改调用点。
 */
@Composable
fun GlassDialogTitle(content: @Composable () -> Unit) {
    ProvideTextStyle(
        value = MaterialTheme.typography.body1.copy(
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTokens.text,
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

/**
 * 弹窗底部两个按钮（取消 / 确定）：一行两个、等分、44dp 胶囊。
 *
 * 确定 = accent 实底 + `onAccent` 文字；取消 = chip 底 + 主文字色。点确定会先回调再
 * `dialogState.show = false`（与 `DialogPositiveButton` 的 dismiss 语义一致）。
 */
@Composable
fun GlassDialogButtons(
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String? = null,
    onCancel: (() -> Unit)? = null,
    cancelTextEnabled: Boolean = true,
    confirmEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (cancelText != null) {
            GlassDialogButton(
                text = cancelText,
                onClick = { onCancel?.invoke() },
                enabled = cancelTextEnabled,
                primary = false,
                modifier = Modifier.weight(1f),
            )
        }
        GlassDialogButton(
            text = confirmText,
            onClick = onConfirm,
            enabled = confirmEnabled,
            primary = true,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 单个弹窗按钮。 */
@Composable
fun GlassDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
) {
    val shape = RoundedCornerShape(GlassDialogButtonHeight / 2)
    val background = when {
        !enabled -> AppTokens.chip.copy(alpha = 0.5f)
        primary -> AppTokens.primary
        else -> AppTokens.chip
    }
    val contentColor = when {
        !enabled -> AppTokens.textDisabled
        primary -> AppTokens.onPrimary
        else -> AppTokens.text
    }
    Box(
        modifier = modifier
            .height(GlassDialogButtonHeight)
            .clip(shape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
        )
    }
}
