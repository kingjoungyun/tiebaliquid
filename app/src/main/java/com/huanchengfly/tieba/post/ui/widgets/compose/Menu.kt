package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.menuBackground
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

class MenuScope(
    private val menuState: MenuState,
    private val onDismiss: (() -> Unit)? = null,
) {
    fun dismiss() {
        onDismiss?.invoke()
        menuState.expanded = false
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClickMenu(
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    menuState: MenuState = rememberMenuState(),
    menuShape: Shape = RoundedCornerShape(14.dp),
    menuWidth: Dp = 240.dp,
    menuHeight: Dp? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    triggerShape: Shape? = null,
    onDismiss: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val menuScope = MenuScope(menuState, onDismiss)
    var lastClickTime by remember { mutableLongStateOf(0L) }
    // 真实触发器的窗口坐标。AppMenu 的 0 尺寸锚点盒算不出这个位置（boundsInWindow 会返回 Zero），
    // 必须由触发器自己上报，否则玻璃菜单会跑到屏幕左上角。
    var triggerBounds by remember { mutableStateOf<Rect?>(null) }
    val clickableInterval = 500L
    LaunchedEffect(key1 = null) {
        launch {
            interactionSource.interactions
                .filterIsInstance<PressInteraction.Press>()
                .collect {
                    menuState.offset = it.pressPosition
                }
        }
    }
    Box {
        val triggerModifier = if (triggerShape != null) Modifier.clip(triggerShape) else Modifier
        Box(
            modifier = triggerModifier
                .onGloballyPositioned { coordinates ->
                    if (coordinates.isAttached) {
                        triggerBounds = coordinates.boundsInWindow()
                    }
                }
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = indication,
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime >= clickableInterval) {
                            menuState.expanded = true
                        }
                        lastClickTime = currentTime
                    }
                )
        ) {
            content()
        }
        Box(
            modifier = Modifier
                .clip(menuShape)
                .offset {
                    IntOffset(
                        menuState.offset.x.roundToInt(),
                        menuState.offset.y.roundToInt()
                    )
                }
        ) {
            // 玻璃菜单：有 backdrop 时是 Glasmio 的材质/动效，没有时 AppMenu 内部自动退回 M2。
            AppMenu(
                expanded = menuState.expanded,
                onDismissRequest = { menuScope.dismiss() },
                modifier = modifier,
                menuWidth = menuWidth,
                heightOverride = menuHeight,
                // 用触发器的「底边」当锚点：菜单右边缘对齐按钮右边缘，从按钮下方展开；
                // 空间不够时 Glasmio 的 IosPopupPositionProvider 会自己翻到上方。
                anchorBounds = triggerBounds?.let {
                    Rect(left = it.left, top = it.bottom, right = it.right, bottom = it.bottom)
                },
            ) {
                ProvideContentColor(color = AppTokens.text) {
                    menuScope.menuContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LongClickMenu(
    menuContent: @Composable (ColumnScope.() -> Unit),
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuState: MenuState = rememberMenuState(),
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(0.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    content: @Composable () -> Unit,
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val clickableInterval = 500L
    LaunchedEffect(Unit) {
        launch {
            interactionSource.interactions
                .filterIsInstance<PressInteraction.Press>()
                .collect {
                    menuState.offset = it.pressPosition
                }
        }
    }
    Box(
        modifier = modifier
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                enabled = enabled,
                onLongClick = {
                    menuState.expanded = true
                }
            ) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime >= clickableInterval) {
                    onClick?.invoke()
                }
                lastClickTime = currentTime
            }
    ) {
        content()
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        menuState.offset.x.roundToInt(),
                        menuState.offset.y.roundToInt()
                    )
                }
        ) {
            AppMenu(
                expanded = menuState.expanded,
                onDismissRequest = { menuState.expanded = false },
            ) {
                ProvideContentColor(color = AppTokens.text) {
                    menuContent()
                }
            }
        }
    }
}

@Composable
fun rememberMenuState(): MenuState {
    return rememberSaveable(
        saver = MenuState.Saver,
        init = { MenuState() }
    )
}

@Stable
class MenuState internal constructor() {
    private var _expanded by mutableStateOf(false)

    fun toggle() {
        expanded = !expanded
    }

    fun show() {
        expanded = true
    }

    fun dismiss() {
        expanded = false
    }

    var expanded: Boolean
        get() = _expanded
        set(value) {
            if (value != _expanded) {
                _expanded = value
            }
        }

    private var _offset by mutableStateOf(Offset(0f, 0f))

    var offset: Offset
        get() = _offset
        set(value) {
            if (value != _offset) {
                _offset = value
            }
        }

    companion object {
        val Saver: Saver<MenuState, *> = listSaver(
            save = {
                listOf<Any>(
                    it.expanded,
                    it.offset.x,
                    it.offset.y
                )
            },
            restore = {
                MenuState().apply {
                    expanded = it[0] as Boolean
                    offset = Offset(it[1] as Float, it[2] as Float)
                }
            }
        )
    }
}