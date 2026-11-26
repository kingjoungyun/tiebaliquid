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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalLiquidGlassHosted
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassNavigationTint
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import com.huanchengfly.tieba.post.ui.widgets.compose.AppScaffold
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

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
 * 当前这层 `actions` 是不是画在主界面顶栏里。
 *
 * 是 → `ActionItem` 画成 44dp 悬浮玻璃圆钮（FlClash 右上角「🖊」）；
 * 否 → 保持原来的 48dp IconButton，设置页 / 其它页面不受影响。
 */
val LocalMainTopBarGlassAction = staticCompositionLocalOf { false }

/**
 * 内容需要让给顶栏的高度（含状态栏）。
 *
 * 注意要把它塞进**滚动容器的 contentPadding**，而不是给页面根节点加 layout padding：
 * 后者会把列表 viewport 整个推到顶栏下面，滚动内容就永远画不到顶栏底下（也就没有像素可采）。
 * `MyLazyColumn` / `MyLazyVerticalGrid` 会自动消费这个值。
 */
val LocalMainTabTopInset = compositionLocalOf { 0.dp }

/**
 * 内容需要让给底部 dock 的高度（= 胶囊高度 + 底部边距）。
 *
 * 和 [LocalMainTabTopInset] 同理，要塞进滚动容器的 `contentPadding.bottom`，不能做成
 * Scaffold 的 bottomBar：后者会把列表 viewport 切在胶囊上沿，滚动时内容永远到不了胶囊后面，
 * 玻璃就成了一块实心板（FlClash 的胶囊下面是有内容滚过去的）。
 * `MyLazyColumn` / `MyLazyVerticalGrid` 会自动消费这个值。
 */
val LocalMainTabBottomInset = compositionLocalOf { 0.dp }

/** 顶栏收起距离：滚过这么多内容后大标题完全收进栏里。 */
private val CollapseDistance = 48.dp

/** scrim 开始显现的滚动阈值。 */
private val GlassStartDistance = 8.dp

/**
 * 顶栏 scrim 的渐变：从顶部较实到底部完全透明。
 *
 * FlClash 的顶栏**本身不是玻璃条**（参考设置页：顶栏没有玻璃条，内容从标题下直接穿过、
 * 顶部是柔和淡出）。所以这里不铺材质，只画一层同色的垂直渐变，边界天然没有硬直线。
 * 数值是「相对 scrim 高度的位置 -> alpha」，最后一段收在 0，保证下沿看不到分界。
 */
private val ScrimStops = arrayOf(
    0.00f to 0.96f,
    0.42f to 0.90f,
    0.68f to 0.62f,
    0.86f to 0.22f,
    1.00f to 0.00f,
)

/** 无玻璃时（AMOLED / 半透明主题）的降级渐变：几乎实底，只在底部柔和收掉。 */
private val SolidScrimStops = arrayOf(
    0.00f to 0.98f,
    0.55f to 0.94f,
    0.85f to 0.58f,
    1.00f to 0.00f,
)

/** scrim 在「状态栏 + 顶栏」之外再往下多渐隐这么多，避免下沿成为一条看得见的边界。 */
private val ScrimTail = 16.dp

/**
 * 主界面四个 tab 的 Scaffold 包装。
 *
 * 与旧的 `AppScaffold(topBar = ...)` 的区别：
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
        AppScaffold(
            modifier = modifier.fillMaxSize(),
            backgroundColor = Color.Transparent,
            topBar = {
                Column(Modifier.background(AppTokens.topBar)) {
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
 * 形态对齐 FlClash：本体透明、内容从下面流过；静止时**什么都不画**，
 * 向下滚动后只渐显一层同色的柔和渐变遮罩（见 [ScrimStops]），没有玻璃条、没有硬边。
 * 双击整条顶栏 = 回顶（沿用 `TopAppBarContainer` 的 `GlobalEvent.ScrollToTop`）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainTopBarHost(
    state: MainTopBarState,
    modifier: Modifier = Modifier,
) {
    val slot = state.slots[state.currentKey]
    val colors = AppTokens
    val glass = glassColors()
    val density = LocalDensity.current
    val glassStart = with(density) { GlassStartDistance.toPx() }
    val collapseDistance = with(density) { CollapseDistance.toPx() }

    val collapse = (state.offsetFor(state.currentKey) / collapseDistance).coerceIn(0f, 1f)
    val scrimTarget = if (state.offsetFor(state.currentKey) > glassStart) 1f else 0f
    val scrimAlpha by animateFloatAsState(
        targetValue = scrimTarget,
        animationSpec = tween(durationMillis = 220),
        label = "mainTopBarScrim",
    )

    // PLAN 第 10 节 7/8：纯黑（AMOLED）与半透明主题下玻璃不可见 / 会发脏，退回不带材质的实底。
    // 注意不能用「深色主题的 background 很暗」来判断：普通深色主题的 background 也很暗，
    // 只有 AMOLED / 真·纯黑才需要放弃玻璃。
    val translucent = ThemeUtil.isTranslucentTheme(colors.theme)
    val amoled = colors.theme == ThemeUtil.THEME_AMOLED_DARK ||
        (colors.isNightMode && colors.background.luminance() < 0.005f)
    val glassAvailable = LocalLiquidGlassHosted.current &&
        isLiquidGlassSupported() &&
        !translucent &&
        !amoled

    // scrim 用页面渐变底的顶部色：淡出处与底图同色，不会出现一条色带。
    // 降级路径改用自己的 topBar 色 + 更实的 stops（半透明主题下仍要能读清标题）。
    val scrimColor = if (glassAvailable) glass.gradientTop else colors.topBar
    val scrimStops = if (glassAvailable) ScrimStops else SolidScrimStops

    val statusBarPx = WindowInsets.statusBars.getTop(density).toFloat()
    val scrimHeightPx = statusBarPx +
        with(density) { GlassTokens.Spacing.barHeight.toPx() } +
        with(density) { ScrimTail.toPx() }

    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { if (slot != null) state.barHeightPx = it.height }
            .drawBehind {
                if (scrimAlpha <= 0.01f) return@drawBehind
                val height = scrimHeightPx.coerceAtMost(size.height)
                if (height <= 0f) return@drawBehind
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = scrimStops.map { (fraction, alpha) ->
                            fraction to scrimColor.copy(alpha = alpha * scrimAlpha)
                        }.toTypedArray(),
                        startY = 0f,
                        endY = height,
                    ),
                    size = Size(size.width, height),
                )
            }
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
 *
 * 行内的操作项被标记成「悬浮玻璃按钮」（[LocalMainTopBarGlassAction]），
 * 所以各 tab 页继续写 `ActionItem(...)` 就会得到 FlClash 那种 44dp 圆钮，不需要改页面。
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
        CompositionLocalProvider(LocalMainTopBarGlassAction provides true) {
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

/**
 * 顶栏上的悬浮液态玻璃按钮，形态对齐 FlClash 右上角「🖊」：
 * 44dp 圆形、20dp 图标、浮在内容上（不坐在一条玻璃条里），深色下靠内发光描边把壳拉出来。
 *
 * 实现已收敛到全 App 统一的 [GlassIconButton]（顶栏在 host 的 overlay 层里，
 * 也就是「采样安全」的那一支，所以能采到下面的真实内容）。
 *
 * 手势：**只处理 tap**。双击回顶留在顶栏容器的 `combinedClickable` 上（PLAN 第 10 节第 2 条），
 * 这里不注册 `combinedClickable`，避免把容器的手势吃掉。
 */
@Composable
fun MainTopBarGlassActionItem(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(horizontal = GlassTokens.Spacing.topBarActionGap),
        contentAlignment = Alignment.Center,
    ) {
        GlassIconButton(
            onClick = onClick,
            icon = icon,
            contentDescription = contentDescription,
            size = GlassTokens.Spacing.topBarActionSize,
            iconSize = GlassTokens.Spacing.topBarActionIconSize,
            navigationSurfaceColor = glassNavigationTint(),
        )
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
