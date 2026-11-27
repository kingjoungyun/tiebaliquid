package com.huanchengfly.tieba.post.ui.common.theme.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.glass.GlassSurface

/**
 * 全站统一的页面骨架（T1）。
 *
 * 把原来三套并存的页面外壳收敛成一套：
 * 1. `ThreadPageSkin`（15 页手写：overlay 双层 host + 自己拼顶栏/底栏）；
 * 2. `glasmio-scaffold` 的导航页骨架（设置页试点）；
 * 3. M2 `AppScaffold` + 手写 34sp 大标题（工具箱 / 分析 / 长尾 11 页）。
 *
 * 骨架负责的（都从 `ThreadPageSkin` 搬过来，行为保持一致）：
 * - **overlay 双层结构**：`LiquidGlassBackdropHost(sampledContent, overlayContent)`。
 *   页面内容进 sampledContent（被录进 LayerBackdrop），顶栏/底栏/悬浮钮进 overlayContent。
 *   这是玻璃控件「采到真实内容」与菜单形变（`AnchorMenu` 从触发器长出来）的前提，
 *   **不能省**：一旦把玻璃控件放回录制层，它就会自己采样自己。
 * - **滚动 scrim**：`groupedBackground` 82% → 48% → 0 的三段渐变，高度 = 状态栏 + 88dp，
 *   透明度由调用方按自己的滚动状态给（[scrimAlpha]）。
 * - **悬浮返回圆钮**：`GlassIconButton`，在 overlay 层（采真实内容，真玻璃）。
 * - **收起后的紧凑胶囊标题**：36dp 高、`IosTypography.subheadline`、默认材质 —
 *   即 `SubPostsGlassTopBar` 里那颗，`collapsed` 为真时淡入。
 * - **顶栏右侧按钮槽**、**可选底栏**、**可选悬浮层**。
 * - **content 的 `PaddingValues`**：状态栏 + 顶栏高度（52dp），内容从顶栏下穿过。
 *
 * ## 大标题（[largeTitle]，T5 批次 1 后补）
 *
 * 三种形态，`largeTitle = null` 时行为与以前**完全一致**（现有页面零影响）：
 * 1. `largeTitle != null` + `largeTitleScrollState == null` → **常显**：标题固定在顶栏下方，
 *    内容在一个占满剩余高度的 Box 里滚动（给 `Column + verticalScroll` / 内嵌列表的页面用）；
 * 2. `largeTitle != null` + `largeTitleScrollState != null` → **自动折叠**：内部用
 *    [rememberGlassTitleCollapse] 判定，滚过大标题就把它淡出、让顶栏胶囊标题淡入；
 * 3. `largeTitle == null` → 不画大标题（原来的行为）。
 *
 * 大标题**不掺进调用方的列表**（不要求列表首项），所以页面迁移时不用再手写
 * `GlassLargeTitle` + 折叠判定那段样板；[content] 拿到的仍是「状态栏 + 顶栏」的 padding。
 * 长尾 11 页在 T5 迁移，本轮不动。
 *
 * 回滚：`enabled = false` 时只渲染 `content(PaddingValues.Zero)`（等价于旧 `ThreadPageSkin`
 * 关掉玻璃分支的行为），各页的 `*Ui.DefaultStyle = LEGACY` 开关继续有效。
 *
 * @param title 页面标题：紧凑胶囊用它
 * @param onBack 非空时画左上角悬浮返回钮
 * @param enabled 玻璃外壳开关（回滚用）
 * @param collapsed 大标题是否已滚走（决定紧凑胶囊标题的出现）
 * @param scrimAlpha 滚动 scrim 透明度 0..1（页面自己的滚动状态驱动）
 * @param actions 顶栏右侧按钮槽
 * @param center 顶栏中央的自定义内容（帖子页的吧名胶囊等）；为空时走紧凑胶囊标题
 * @param bottomBar 可选底栏（帖子页 / 楼中楼用）
 * @param floating 可选悬浮层（可对齐到 overlay 的任意位置）
 * @param snackbarHost 可选 snackbar 宿主槽（配合 [GlassSnackbarHost]）
 * @param content 页面内容；`padding` = 状态栏 + 顶栏高度
 */
@Composable
fun GlassPage(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    collapsed: Boolean = false,
    scrimAlpha: Float = if (collapsed) 1f else 0f,
    actions: @Composable RowScope.() -> Unit = {},
    center: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable (() -> Unit))? = null,
    floating: @Composable BoxScope.() -> Unit = {},
    /** 34sp 大标题；null 时行为与旧版完全一致。见类注释的「大标题」三种形态。 */
    largeTitle: String? = null,
    /** 非空时大标题自动折叠（滚过它即淡出）；为空时常显。 */
    largeTitleScrollState: LazyListState? = null,
    /** 大标题自己的内边距（默认横向 16dp + 纵向 6dp，与手写版一致）。 */
    largeTitlePadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    /**
     * 底部 snackbar 宿主槽（T5 批次 3）。
     *
     * 骨架原来没有 snackbar host：页面一旦不套 `AppScaffold`，读 `LocalSnackbarHostState`
     * 就会抛 `no scaffold here!`。传这个槽 + [GlassSnackbarHost] 就能在玻璃层级里弹提示。
     */
    snackbarHost: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    if (!enabled) {
        content(PaddingValues.Zero)
        return
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val contentPadding = remember(statusBarTop) {
        PaddingValues(top = statusBarTop + GlassTopBarHeight)
    }

    // 大标题的折叠判定：给了 LazyListState 才自动折叠（阈值退化成「滚过 32dp」），否则常显。
    val autoCollapsed = if (largeTitleScrollState != null) {
        rememberGlassTitleCollapse(largeTitleScrollState, largeTitleHeightPx = 0)
    } else {
        false
    }
    val resolvedCollapsed = collapsed || autoCollapsed
    val resolvedScrimAlpha = maxOf(scrimAlpha, if (resolvedCollapsed) 1f else 0f)
    val largeTitleVisible = largeTitle != null && !(largeTitleScrollState != null && resolvedCollapsed)

    // 有大标题时把内容整体下移一行：大标题固定在顶栏下方，内容在自己的 Box 里滚动，
    // 这样两种形态（常显 / 自动折叠）都不需要调用方的列表配合。
    val pageContent: @Composable (PaddingValues) -> Unit = if (largeTitle == null) {
        content
    } else {
        { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
            ) {
                AnimatedVisibility(
                    visible = largeTitleVisible,
                    enter = fadeIn(animationSpec = tween(180)),
                    exit = fadeOut(animationSpec = tween(120)),
                ) {
                    GlassLargeTitle(
                        title = largeTitle,
                        contentPadding = largeTitlePadding,
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    content(PaddingValues.Zero)
                }
            }
        }
    }

    if (!isLiquidGlassSupported()) {
        // 不支持真玻璃的机器：不套 host，但骨架结构（返回钮 / 顶栏 / 底栏）保留，
        // 玻璃控件自己会降级成 LiquidGlassSurface 的伪玻璃。
        Box(modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GlasmioTheme.colors.groupedBackground)
            ) {
                pageContent(contentPadding)
            }
            GlassPageTopBar(
                title = title,
                onBack = onBack,
                collapsed = resolvedCollapsed,
                scrimAlpha = resolvedScrimAlpha,
                center = center,
                actions = actions,
            )
            // 底栏与 snackbar 叠在同一个 Column 里：snackbar 在底栏之上，不会盖住按钮
            Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                if (snackbarHost != null) { Box { snackbarHost() } }
                if (bottomBar != null) { bottomBar() }
            }
            floating()
        }
        return
    }

    LiquidGlassTheme(accent = AppTokens.primary) {
        HarmonyTextProvider {
            LiquidGlassBackdropHost(
                modifier = modifier.fillMaxSize(),
                sampledContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(GlasmioTheme.colors.groupedBackground)
                    ) {
                        pageContent(contentPadding)
                    }
                },
                overlayContent = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        GlassPageTopBar(
                            title = title,
                            onBack = onBack,
                            collapsed = resolvedCollapsed,
                            scrimAlpha = resolvedScrimAlpha,
                            center = center,
                            actions = actions,
                        )
                        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                            if (snackbarHost != null) { Box { snackbarHost() } }
                            if (bottomBar != null) { bottomBar() }
                        }
                        floating()
                    }
                },
            )
        }
    }
}

/** 悬浮顶栏（含 scrim）的总高度：44dp 控件 + 上下各 4dp。 */
val GlassTopBarHeight = 52.dp

/** 紧凑胶囊标题的高度。 */
val GlassCollapsedTitleHeight = 36.dp

/** scrim 渐变下方多探出的高度（状态栏之上再加 88dp，滚过顶栏的内容仍被压暗）。 */
private val GlassScrimExtraHeight = 88.dp

/** 紧凑标题胶囊的最大宽度。 */
private val GlassCollapsedTitleMaxWidth = 240.dp

/**
 * 顶栏本体：scrim + 返回钮 + 中央（自定义内容 / 紧凑胶囊标题）+ 右侧按钮槽。
 *
 * 帖子页、楼中楼页此前的顶栏就是这一份的三段代码，这里合并成一套，参数一一对应。
 */
@Composable
private fun GlassPageTopBar(
    title: String,
    onBack: (() -> Unit)?,
    collapsed: Boolean,
    scrimAlpha: Float,
    center: (@Composable () -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scrimHeight = statusBarTop + GlassScrimExtraHeight
    val scrimBrush = Brush.verticalGradient(
        0f to colors.groupedBackground.copy(alpha = 0.82f),
        0.58f to colors.groupedBackground.copy(alpha = 0.48f),
        1f to Color.Transparent,
    )

    Box(modifier = modifier.fillMaxWidth()) {
        if (scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scrimHeight)
                    .background(scrimBrush)
                    .alpha(scrimAlpha)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // insets：这里属于【顶栏/悬浮控件自身】，勿改（见 docs/INSETS-WHITELIST.md）
                .statusBarsPadding()
                .height(GlassTopBarHeight)
                .padding(horizontal = 8.dp)
        ) {
            if (onBack != null) {
                GlassIconButton(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(id = R.string.button_back),
                    tint = colors.content,
                    navigationSurfaceColor = null,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }

            if (center != null) {
                Box(modifier = Modifier.align(Alignment.Center)) { center() }
            } else {
                AnimatedVisibility(
                    visible = collapsed,
                    modifier = Modifier.align(Alignment.Center),
                    enter = fadeIn(animationSpec = tween(180)),
                    exit = fadeOut(animationSpec = tween(120)),
                ) {
                    GlassCollapsedTitle(text = title)
                }
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

/**
 * 收起后顶栏中央的紧凑胶囊标题：36dp 高、默认玻璃材质、单行省略。
 *
 * 与 `SubPostsGlassTopBar` 里原来那颗逐参数一致（shape 18dp / 高 36dp / max 240dp /
 * `IosTypography.subheadline` / 左右 14dp）。
 */
@Composable
fun GlassCollapsedTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    GlassSurface(
        modifier = modifier
            .height(GlassCollapsedTitleHeight)
            .widthIn(max = GlassCollapsedTitleMaxWidth)
            .wrapContentWidth(),
        shape = RoundedCornerShape(GlassCollapsedTitleHeight / 2),
    ) {
        Text(
            text = text,
            style = IosTypography.subheadline,
            color = colors.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
    }
}

/**
 * 列表首项里的大标题（34sp / 40sp / w700 + 鸿蒙字族，经 `HarmonyTextProvider` 生效）。
 *
 * 与 `SubPostsLargeTitle`、各页手写的 34sp 标题同源；[progress] 是「顶栏紧凑标题淡入进度」，
 * 大标题按 `1 - progress` 反向淡出，免得起飞的大标题被顶栏压住后留半截残字。
 *
 * 颜色 / 样式 / 内外边距都留了参数：迁移过程中要保证逐像素一致，各页原来的写法不同
 * （有的用 `AppTokens.text`，有的用 `GlasmioTheme.colors.content`；有的横向 16dp 由列表给）。
 *
 * @param subtitle 大标题上方的小字（楼中楼页的「2 楼的回复」）
 * @param contentPadding 大标题自己的内边距（默认横向 16dp + 纵向 8dp，与手写版一致）
 * @param onHeightChanged 回传大标题高度，供 [rememberGlassTitleCollapse] 判定折叠
 */
@Composable
fun GlassLargeTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    color: Color = GlasmioTheme.colors.content,
    style: TextStyle = LocalTextStyle.current.copy(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
    ),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    progress: Float = 0f,
    maxLines: Int = 2,
    onHeightChanged: ((Int) -> Unit)? = null,
) {
    val colors = GlasmioTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onHeightChanged != null) {
                    Modifier.onSizeChanged { onHeightChanged(it.height) }
                } else {
                    Modifier
                }
            )
            .padding(contentPadding)
            .alpha((1f - progress).coerceIn(0f, 1f)),
    ) {
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = colors.secondaryContent,
            )
        }
        Text(
            text = title,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 折叠判定的通用实现：大标题高度（px）已知时，滚过它就算收起；否则退化成「滚过 [fallback]」。
 *
 * 与 `SubPostsPage` 里那段 `derivedStateOf { firstVisibleItemIndex > 0 || offset >= h }` 一致。
 */
@Composable
fun rememberGlassTitleCollapse(
    listState: LazyListState,
    largeTitleHeightPx: Int,
    fallback: Dp = 32.dp,
): Boolean {
    val fallbackPx = with(LocalDensity.current) { fallback.toPx() }.toInt()
    val collapsed by remember(listState, largeTitleHeightPx, fallbackPx) {
        derivedStateOf {
            val threshold = if (largeTitleHeightPx > 0) largeTitleHeightPx else fallbackPx
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset >= threshold
        }
    }
    return collapsed
}
