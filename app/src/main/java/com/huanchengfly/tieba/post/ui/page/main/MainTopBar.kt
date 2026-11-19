package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassSurface
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalLiquidGlassHosted
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassNavigationTint
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.utils.ThemeUtil
import io.github.neoruaa.glasmio.glass.NavigationGlassMaterial

/**
 * 主界面顶栏的运行状态。
 *
 * 顶栏本身画在 `GlasmioBackdropHost` 的 overlay 层（玻璃必须画在录制层之外，否则会自己采样自己），
 * 但标题/按钮属于各自的 tab 页。所以各页通过 [MainTabScaffold] 把顶栏内容登记到这里，
 * host 只渲染当前页那一份。
 *
 * 同时它还是滚动进度的单一来源：内容层的 nestedScroll 只负责累加偏移，
 * host 据此淡入薄玻璃、收起大标题。
 */
@Stable
class MainTopBarState(initialKey: String = "") {
    internal val slots = mutableStateMapOf<String, @Composable () -> Unit>()

    var currentKey by mutableStateOf(initialKey)
        internal set

    /** 当前页内容已向顶栏方向滚动的像素数（0 = 在顶部）。 */
    private val scrollOffsets = mutableStateMapOf<String, Float>()

    /** overlay 顶栏的实测高度（含状态栏）。0 表示还没量到。 */
    var barHeightPx by mutableIntStateOf(0)
        internal set

    internal fun setCurrent(key: String) {
        currentKey = key
    }

    internal fun offsetFor(key: String): Float = scrollOffsets[key] ?: 0f

    internal fun addScroll(key: String, consumedY: Float) {
        // LazyColumn / Grid 的 nestedScroll 约定：手指上滑（看后面的内容）consumed.y 为负。
        scrollOffsets[key] = (offsetFor(key) - consumedY).coerceAtLeast(0f)
    }

    /** 页面离开组合（被 pager 回收）时清掉它的滚动量，下次回到顶部不会带着旧进度。 */
    internal fun clearScroll(key: String) {
        scrollOffsets.remove(key)
    }

    internal fun resetScroll() {
        scrollOffsets[currentKey] = 0f
    }
}

/** 当前 tab 的 key（`NavigationItem.id`）；由 MainPage 的 pager 包裹提供。 */
val LocalMainTabKey = staticCompositionLocalOf<String?> { null }

/** 主界面顶栏状态；由 MainPage 提供。 */
val LocalMainTopBarState = staticCompositionLocalOf<MainTopBarState?> { null }

/**
 * 顶栏收起进度 0..1（大标题 24sp → 17sp、薄玻璃淡入都由它驱动）。
 * 在 [MainTopBarHost] 里注入，登记进来的顶栏内容直接读。
 */
val LocalMainTopBarCollapse = compositionLocalOf { 0f }

/**
 * 内容需要让给顶栏的高度（含状态栏）。
 *
 * 注意要把它塞进**滚动容器的 contentPadding**，而不是给页面根节点加 layout padding：
 * 后者会把列表 viewport 整个推到顶栏下面，滚动内容就永远画不到顶栏底下（也就没有像素可采）。
 * `MyLazyColumn` / `MyLazyVerticalGrid` 会自动消费这个值。
 */
val LocalMainTabTopInset = compositionLocalOf { 0.dp }

/** 顶栏收起距离：滚过这么多内容后大标题完全收进栏里。 */
private val CollapseDistance = 48.dp

/** 玻璃开始显现的滚动阈值。 */
private val GlassStartDistance = 8.dp

/**
 * 主界面四个 tab 的 Scaffold 包装。
 *
 * 与旧的 `MyScaffold(topBar = ...)` 的区别：
 * - 顶栏不再占布局高度（内容可以滚到它下面），而是登记给 overlay 层；
 * - 内容顶部补「状态栏 + 实测顶栏高度」的 padding，第一张卡不会被顶栏盖住；
 * - 内容上的 nestedScroll 把纵向滚动量喂给 [MainTopBarState]。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainTabScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val state = LocalMainTopBarState.current
    val key = LocalMainTabKey.current
    if (state == null || key == null) {
        // 防御：不在 MainPage 里（预览 / 单页调试）时退回实体顶栏，至少不丢内容。
        MyScaffold(
            modifier = modifier.fillMaxSize(),
            backgroundColor = Color.Transparent,
            topBar = {
                Column(Modifier.background(ExtendedTheme.colors.topBar)) {
                    Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                    Box(Modifier.height(GlassTokens.Spacing.barHeight)) { topBar() }
                }
            },
        ) { paddings ->
            content(paddings)
        }
        return
    }

    SideEffect { state.slots[key] = topBar }
    DisposableEffect(state, key) {
        onDispose {
            state.slots.remove(key)
            state.clearScroll(key)
        }
    }

    val density = LocalDensity.current
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    val defaultTopPx = statusBarPx + with(density) { GlassTokens.Spacing.barHeight.toPx() }.toInt()
    val measured = state.barHeightPx
    val topPadding = with(density) {
        (if (measured > 0) measured else defaultTopPx).toDp()
    }

    val scrollConnection = remember(state) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // consumed.y > 0 = 内容向上滚（手指上滑）。回到顶部/下拉刷新时为负，addScroll 会夹到 0。
                state.addScroll(key, consumed.y)
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollConnection)
    ) {
        CompositionLocalProvider(LocalMainTabTopInset provides topPadding) {
            Box(modifier = Modifier.fillMaxSize()) {
                content(PaddingValues(0.dp))
            }
        }
    }
}

/**
 * 主界面顶栏（overlay 层）。
 *
 * 形态对齐 FlClash：本体透明、内容从下面流过；静止时无底，向下滚动后渐显一层薄玻璃。
 * 双击整条顶栏 = 回顶（沿用 `TopAppBarContainer` 的 `GlobalEvent.ScrollToTop`）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainTopBarHost(
    state: MainTopBarState,
    modifier: Modifier = Modifier,
) {
    val slot = state.slots[state.currentKey]
    val colors = ExtendedTheme.colors
    val density = LocalDensity.current
    val glassStart = with(density) { GlassStartDistance.toPx() }
    val collapseDistance = with(density) { CollapseDistance.toPx() }

    val collapse = (state.offsetFor(state.currentKey) / collapseDistance).coerceIn(0f, 1f)
    val glassTarget = if (state.offsetFor(state.currentKey) > glassStart) 1f else 0f
    val glassAlpha by animateFloatAsState(
        targetValue = glassTarget,
        animationSpec = tween(durationMillis = 220),
        label = "mainTopBarGlass",
    )

    // PLAN 第 10 节 7/8：纯黑（AMOLED）与半透明主题下玻璃不可见 / 会发脏，退回半透明实底。
    // 注意不能用「深色主题的 background 很暗」来判断：普通深色主题的 background 也很暗，
    // 只有 AMOLED / 真·纯黑才需要放弃玻璃。
    val translucent = ThemeUtil.isTranslucentTheme(colors.theme)
    val amoled = colors.theme == ThemeUtil.THEME_AMOLED_DARK ||
        (colors.isNightMode && colors.background.luminance() < 0.005f)
    val glassAvailable = LocalLiquidGlassHosted.current &&
        isLiquidGlassSupported() &&
        !translucent &&
        !amoled

    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { if (slot != null) state.barHeightPx = it.height }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onDoubleClick = {
                    state.resetScroll()
                    coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop)
                },
            )
    ) {
        if (glassAlpha > 0.01f) {
            if (glassAvailable) {
                // 薄玻璃：矩形薄材质，tint 用 FlClash regular 那组值（浅 .54 / 深 .62），
                // 整层 alpha 跟随顶部滚动量淡入，不写死高度（含首页搜索框收起动画）。
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = glassAlpha }
                ) {
                    LiquidGlassSurface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RectangleShape,
                        material = NavigationGlassMaterial.RegularSmall,
                        navigationSurfaceColor = glassNavigationTint(),
                        navigationSurfaceAlphaMultiplier = 1f,
                    ) {}
                }
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = glassAlpha }
                        .background(colors.topBar.copy(alpha = 0.94f))
                )
            }
        }

        Column(Modifier.fillMaxWidth()) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            if (slot != null) {
                CompositionLocalProvider(LocalMainTopBarCollapse provides collapse) {
                    slot.invoke()
                }
            }
        }
    }
}

/**
 * 72dp 的顶栏内容行：左导航、中间标题、右操作。
 * 与旧的 `TopAppBar` 56dp 对齐 FlClash 的 72dp（不含状态栏）。
 */
@Composable
fun MainTopBarRow(
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    title: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(GlassTokens.Spacing.barHeight)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier,
            contentAlignment = Alignment.CenterStart,
        ) {
            navigationIcon?.invoke()
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            title()
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * 大标题 → 顶栏小标题：24sp/w600 → 17sp/w600，随 [LocalMainTopBarCollapse] 连续缩放。
 * 与设置页的「大标题滚走 + 顶栏小标题淡入」是同一套观感，这里是固定栏内收字。
 */
@Composable
fun MainCollapsingTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val collapse = LocalMainTopBarCollapse.current
    val colors = glassColors()
    Text(
        text = text,
        modifier = modifier,
        color = colors.textPrimary,
        fontSize = lerp(24f, 17f, collapse).sp,
        fontWeight = FontWeight(600),
        maxLines = 1,
    )
}
