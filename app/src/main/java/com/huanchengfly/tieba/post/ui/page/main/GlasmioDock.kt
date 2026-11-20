package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.White
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalLiquidGlassHosted
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import io.github.neoruaa.glasmio.icons.LocalGlasmioIconResolver
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.ui.CompactBottomControlSize
import io.github.neoruaa.glasmio.ui.GlasmioNavigationBar
import io.github.neoruaa.glasmio.ui.GlasmioTabItem
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

/**
 * 滚动超过这个距离就收起成圆球（iOS 26 的「滚动最小化」）。
 * 用「进入/退出两个阈值」做迟滞，避免在阈值附近来回抖。
 */
private val CompactEnterDistance = 96.dp
private val CompactExitDistance = 8.dp

/** 未读小红点：贴吧原版是 14dp 圆 + 10sp 数字，这里沿用尺寸，宽度允许被「99+」撑开。 */
private val BadgeHeight = 14.dp
private val BadgeMinWidth = 14.dp

/** 小红点的定位锚框（固定 28dp，好让「以图标右上角为中心」的偏移量算得出来）。 */
private val BadgeAnchorSize = 28.dp

/**
 * Glasmio 原生 `GlasmioNavigationBar` 版本的底部 dock。
 *
 * 参数与旧的自绘 [BottomNavigation] 对齐（currentPosition / onChangePosition / onReselected /
 * navigationItems / modifier），`MainPage` 的调用处只需要换函数名。
 *
 * 与旧版的差别（都由 Glasmio 组件自带，不是这里实现的）：
 * - 选中态是**镜头**（lens）在胶囊里弹簧滑过去，带拉伸/回弹；不是静态的 12% accent 胶囊；
 * - 按下时整块玻璃**形变**（puff）并抬起镜头；
 * - 滚动时胶囊 ↔ 圆球收缩（[scrollOffsetPx] 接上才生效，见下）。
 *
 * @param scrollOffsetPx 当前页「已向顶栏方向滚动的像素」。接上才会收起成圆球；
 *   传 null（或恒为 0）就始终是展开态。
 */
@Composable
fun GlasmioDock(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    modifier: Modifier = Modifier,
    scrollOffsetPx: (() -> Float)? = null,
) {
    // 没有 backdrop host（宽屏分支 / 玻璃层被关掉）或 API < 33 时，GlasmioNavigationBar 读
    // LocalGlassBackdrop 会直接抛异常。这里整块退回旧实现 —— 它自己会降级成伪玻璃，不会白屏，
    // 也就成了这套改造的 kill switch。
    if (!LocalLiquidGlassHosted.current || !isLiquidGlassSupported()) {
        BottomNavigation(
            currentPosition = currentPosition,
            onChangePosition = onChangePosition,
            onReselected = onReselected,
            navigationItems = navigationItems,
            modifier = modifier,
        )
        return
    }

    val density = LocalDensity.current
    // 「用户点开过」的冻结位：收起态点一下会展开，并保持展开直到滚回顶部附近。
    var userExpanded by remember { mutableStateOf(false) }
    val scrollOffset = scrollOffsetPx?.invoke() ?: 0f
    val enterPx = with(density) { CompactEnterDistance.toPx() }
    val exitPx = with(density) { CompactExitDistance.toPx() }
    val nearTop = scrollOffset <= exitPx
    LaunchedEffect(nearTop) {
        if (nearTop) userExpanded = false
    }
    val compactTarget = if (!userExpanded && scrollOffset > enterPx) 1f else 0f
    // 弹簧而不是 tween：Glasmio 的形变按「进度」取值，弹簧的回弹才有玻璃该有的手感。
    // 参数抄它自己 sample 的 NavigationDemo（dampingRatio .8 / stiffness 438.64908）。
    val compactProgress = animateFloatAsState(
        targetValue = compactTarget,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 438.64908f),
        label = "mainDockCompact",
    )

    // label 是 @Composable（要读 stringResource），只能在组合里取；再按内容做 remember key，
    // 避免每次重组都重建 items（Glasmio 内部按 items 实例做 memo）。
    val labels = navigationItems.mapIndexed { index, item -> item.title(index == currentPosition) }
    val ids = navigationItems.map { it.id }
    val items = remember(ids, labels) {
        ids.mapIndexed { index, id ->
            GlasmioTabItem(
                key = index,
                label = labels[index],
                symbol = dockSymbolFor(id),
                contentDescription = labels[index],
            )
        }
    }

    // 未读小红点：GlasmioTabItem 没有 badge 字段，用 onItemIconBoundsChanged 拿到的图标窗口
    // 坐标自己叠一层（在玻璃之上、不进录制层，否则会被采样）。颜色沿用旧 dock 的 secondary。
    val badgeColor = MaterialTheme.colors.secondary
    val badges = navigationItems.mapIndexedNotNull { index, item ->
        if (item.badge) index to (item.badgeText ?: "") else null
    }
    val iconBounds = remember { mutableStateMapOf<Int, Rect>() }
    val dockOrigin = remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            // 位置与旧 dock 完全一致：距屏幕底 max(21dp, 导航栏 inset)，左右 16dp。
            .padding(
                start = GlassTokens.Spacing.dockMarginH,
                end = GlassTokens.Spacing.dockMarginH,
                top = GlassTokens.Spacing.dockMarginBottom,
                bottom = navigationDockBottomMargin(),
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { dockOrigin.value = it.positionInWindow() }
        ) {
            LiquidGlassTheme {
                CompositionLocalProvider(LocalGlasmioIconResolver provides TiebaGlasmioIconResolver) {
                    // 收起时 bar 自己把**布局**宽度缩到 compactSize（48dp）；展开时宽度由
                    // constraints.maxWidth 撑满。所以这里不能给 bar 加 fillMaxWidth：
                    // 那会把 minWidth 也钉成全宽，收起态的玻璃只能贴在左边（CenterStart），
                    // 外面这层 center 的 Box 就失效了（Glasmio 的 sample 也是这么摆的）。
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        GlasmioNavigationBar(
                            items = items,
                            selectedKey = currentPosition,
                            onSelected = { index ->
                                if (index == currentPosition) {
                                    onReselected(index)
                                } else {
                                    onChangePosition(index)
                                }
                                // 与旧 dock 一致：选中即清未读（MainPage 在 item.onClick 里发 Intent）。
                                navigationItems.getOrNull(index)?.onClick?.invoke()
                            },
                            // 收起态点一下 = 展开。用户主动展开后不再自动收起，直到滚回顶部附近。
                            onExpand = { userExpanded = true },
                            compactProgress = compactProgress,
                            compactSize = CompactBottomControlSize,
                            onItemIconBoundsChanged = { index, bounds ->
                                if (iconBounds[index] != bounds) iconBounds[index] = bounds
                            },
                        )
                    }
                }
            }
            // 小红点画在玻璃之上；收起成圆球时淡出（圆球上没有 label，也没有第二排位置放它）。
            val badgeAlpha = {
                (1f - compactProgress.value * 2f).coerceIn(0f, 1f)
            }
            badges.forEach { (index, text) ->
                val bounds = iconBounds[index] ?: return@forEach
                // 旧 dock 是「14dp 圆贴着 24dp 图标的右上角」，即圆心 ≈ 图标右上角内缩 7dp。
                val anchor = Offset(
                    x = bounds.right - dockOrigin.value.x - with(density) { 7.dp.toPx() },
                    y = bounds.top - dockOrigin.value.y + with(density) { 7.dp.toPx() },
                )
                DockBadge(text = text, anchor = anchor, color = badgeColor, alpha = badgeAlpha)
            }
        }
    }
}

/**
 * tab id → Glasmio 的符号键。
 *
 * 键名只是 Glasmio 内部查找用的名字（真正的图形由 [TiebaGlasmioIconResolver] 给），
 * 但尽量挑语义接近的：house.fill / person.fill 正好对得上，消息借 `dot.radiowaves...`
 * （`SfSymbol` 没有 bell），进吧借 `square.3.layers.3d`。详见 TiebaDockIcons.kt 的注释。
 */
private fun dockSymbolFor(id: String): SfSymbol = when (id) {
    "home" -> SfSymbol.HouseFilled
    "explore" -> SfSymbol.Layers
    "notification" -> SfSymbol.RadioWaves
    "user" -> SfSymbol.PersonFilled
    // hideExplore/以后新增的 tab：给一个中性的兜底，至少不会崩。
    else -> SfSymbol.HouseFilled
}

/**
 * 未读小红点：14dp 高的胶囊（「99+」自动变宽），1dp 白描边让它压在玻璃/图标上也分得开。
 */
@Composable
private fun DockBadge(
    text: String,
    anchor: Offset,
    color: Color,
    alpha: () -> Float,
) {
    val density = LocalDensity.current
    val anchorPx = with(density) { BadgeAnchorSize.toPx() }
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (anchor.x - anchorPx / 2f).roundToInt(),
                    y = (anchor.y - anchorPx / 2f).roundToInt(),
                )
            }
            .size(BadgeAnchorSize)
            .graphicsLayer { this.alpha = alpha() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(BadgeHeight)
                .widthIn(min = BadgeMinWidth)
                .background(color = color, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = White,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 3.dp),
            )
        }
    }
}
