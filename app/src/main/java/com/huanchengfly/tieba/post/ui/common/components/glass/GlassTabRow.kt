package com.huanchengfly.tieba.post.ui.common.components.glass

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassSurface
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuScope
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import kotlinx.coroutines.launch

/** 分段控件（track）的高度。 */
val GlassTabRowHeight: Dp = 36.dp

/** 滑块相对 track 的内缩。 */
private val GlassTabThumbInset = 2.dp

private val GlassTabThumbShadow = Color.Black.copy(alpha = 0.10f)

/**
 * 轨道描边：单个 token 撑不起「灰色轨道」的边界 —— 浅色下 `chip` 是 #F8F8F8、页面底 #F2F2F7，
 * 只差一点点；所以再补 0.5dp 的分隔线色描边，深色下则是 chip(#1E1E1E) 本身就有边界。
 */
@Composable
private fun glassTabTrackStroke(): Color = AppTokens.divider.copy(alpha = 0.45f)

/** 滑块描边：深色下用 1px 白高光把「很深的 card」从轨道里拉出来；浅色下不描边。 */
@Composable
private fun glassTabThumbStroke(): Color =
    if (AppTokens.isNightMode) Color.White.copy(alpha = 0.14f) else Color.Transparent

/**
 * tab 上的菜单能力（原 M2 分叉里的 `TabClickMenu` 语义）：
 * **点已选中的 tab 时弹出菜单**（吧页的「最新/回复排序」、搜索页的「排序方式」），
 * 未选中的 tab 仍然只是切换。
 *
 * 菜单用现有的 [ClickMenu] 承载，所以锚点、玻璃材质、点外部关闭这些行为与迁移前一致。
 *
 * @param hasMenu 哪些 tab 有菜单（吧页只有「最新」那个 tab 有排序菜单）
 * @param content 菜单内容，参数是 tab 下标
 */
@Immutable
class GlassTabMenu(
    val hasMenu: (index: Int) -> Boolean = { true },
    val content: @Composable MenuScope.(index: Int) -> Unit,
)

/**
 * iOS 分段控件风格的 TabRow（T2）。
 *
 * 结构：**灰色轨道 + 白色滑块**。
 * - 轨道：胶囊形，`AppTokens.chip` 填充 + 0.5dp 分隔线描边（浅色下 chip 与页面底只差三个灰阶，
 *   没有描边看不出「轨道」）；
 * - 滑块：`AppTokens.card` + 轻微阴影（复用 App 的伪玻璃 [GlassSurface] 自绘阴影），
 *   位置随选中项平移 220ms；深色下再补 1px 白高光描边；
 * - 文字：15sp，选中 w600 + `AppTokens.primary`，未选中 `AppTokens.textSecondary`。
 *
 * 不绑定 pager：调用方给 `selectedTabIndex` + `onTabSelected(index)`，自己决定是否
 * `pagerState.animateScrollToPage(...)`。
 *
 * @param tabCount tab 数量（2–3 个时用本函数；4+ 用 [GlassScrollableTabRow]）
 * @param tabMenu 可选：点已选中的 tab 弹菜单（吧页排序 / 搜索排序）
 * @param label tab 内容（默认用 [GlassTabLabel] 画文字）
 */
@Composable
fun GlassTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabCount: Int,
    modifier: Modifier = Modifier,
    tabMenu: GlassTabMenu? = null,
    label: @Composable (index: Int, selected: Boolean) -> Unit = { index, selected ->
        GlassTabLabel(text = index.toString(), selected = selected)
    },
) {
    if (tabCount <= 0) return
    val shape = RoundedCornerShape(GlassTabRowHeight / 2)
    val density = LocalDensity.current
    val insetPx = with(density) { GlassTabThumbInset.toPx() }
    var rowWidthPx by remember { mutableIntStateOf(0) }

    val segmentWidthPx = if (rowWidthPx > 0) rowWidthPx / tabCount else 0
    val thumbWidthPx = (segmentWidthPx - insetPx * 2).coerceAtLeast(0f)
    val targetOffsetPx = insetPx + selectedTabIndex.coerceIn(0, tabCount - 1) * segmentWidthPx
    val animatedOffsetPx by animateDpAsState(
        targetValue = with(density) { targetOffsetPx.toDp() },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "glassTabThumbOffset",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(GlassTabRowHeight)
            .onSizeChanged { rowWidthPx = it.width }
            .selectableGroup(),
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            fill = AppTokens.chip,
            strokeColor = glassTabTrackStroke(),
            shadowElevation = 0.dp,
            shadowOffsetY = 0.dp,
            strokeWidth = 0.5.dp,
        ) {}

        if (thumbWidthPx > 0) {
            GlassSurface(
                modifier = Modifier
                    .offset(x = animatedOffsetPx, y = GlassTabThumbInset)
                    .width(with(density) { thumbWidthPx.toDp() })
                    .height(GlassTabRowHeight - GlassTabThumbInset * 2),
                shape = RoundedCornerShape((GlassTabRowHeight - GlassTabThumbInset * 2) / 2),
                fill = AppTokens.card,
                strokeColor = glassTabThumbStroke(),
                shadowColor = GlassTabThumbShadow,
                shadowElevation = 6.dp,
                shadowOffsetY = 1.dp,
                strokeWidth = 0.dp,
            ) {}
        }

        Row(modifier = Modifier.fillMaxSize()) {
            repeat(tabCount) { index ->
                key(index) {
                    GlassTabItem(
                        index = index,
                        selected = index == selectedTabIndex,
                        onSelect = { onTabSelected(index) },
                        tabMenu = tabMenu,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        label = label,
                    )
                }
            }
        }
    }
}

/** 纯文字分段控件的便捷重载。 */
@Composable
fun GlassTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabMenu: GlassTabMenu? = null,
) {
    GlassTabRow(
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        tabCount = tabs.size,
        modifier = modifier,
        tabMenu = tabMenu,
    ) { index, selected ->
        GlassTabLabel(text = tabs[index], selected = selected)
    }
}

/**
 * 可滚动的分段控件（4+ tab）。
 *
 * 轨道跟着内容一起横向滚动（内容宽度 = 所有 tab 宽度之和），滑块位置由各 tab 的实测宽度
 * 累加得到；选中项变化时自动把它滚到视口中间。tabs 少于 3 个时建议用 [GlassTabRow]
 * （等分、宽度铺满）。
 */
@Composable
fun GlassScrollableTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabCount: Int,
    modifier: Modifier = Modifier,
    tabMenu: GlassTabMenu? = null,
    label: @Composable (index: Int, selected: Boolean) -> Unit = { index, selected ->
        GlassTabLabel(text = index.toString(), selected = selected)
    },
) {
    if (tabCount <= 0) return
    val shape = RoundedCornerShape(GlassTabRowHeight / 2)
    val density = LocalDensity.current
    val insetPx = with(density) { GlassTabThumbInset.toPx() }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val tabWidths = remember(tabCount) {
        mutableStateListOf<Int>().apply { repeat(tabCount) { add(0) } }
    }
    var viewportWidthPx by remember { mutableIntStateOf(0) }

    val selected = selectedTabIndex.coerceIn(0, tabCount - 1)
    val tabsBeforePx = (0 until selected).sumOf { tabWidths[it] }
    val thumbOffsetPx = insetPx + tabsBeforePx
    val thumbWidthPx = (tabWidths[selected] - insetPx * 2).coerceAtLeast(0f)

    val animatedOffset by animateDpAsState(
        targetValue = with(density) { thumbOffsetPx.toDp() },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "glassScrollableTabThumbOffset",
    )
    val animatedWidth by animateDpAsState(
        targetValue = with(density) { thumbWidthPx.toDp() },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "glassScrollableTabThumbWidth",
    )

    // 选中项变化时，把它滚到视口中间（内容不足一屏时 scrollState 自己会夹住）
    LaunchedEffect(selected, tabWidths.toList(), viewportWidthPx) {
        if (viewportWidthPx <= 0 || tabWidths[selected] <= 0) return@LaunchedEffect
        val target = (tabsBeforePx - (viewportWidthPx - tabWidths[selected]) / 2)
            .coerceIn(0, scrollState.maxValue)
        coroutineScope.launch { scrollState.animateScrollTo(target) }
    }

    Box(
        modifier = modifier
            .height(GlassTabRowHeight)
            .onSizeChanged { viewportWidthPx = it.width }
            .clip(shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .horizontalScroll(scrollState),
        ) {
            Box(modifier = Modifier.height(GlassTabRowHeight)) {
                GlassSurface(
                    modifier = Modifier.matchParentSize(),
                    shape = shape,
                    fill = AppTokens.chip,
                    strokeColor = glassTabTrackStroke(),
                    shadowElevation = 0.dp,
                    shadowOffsetY = 0.dp,
                    strokeWidth = 0.5.dp,
                ) {}
                if (thumbWidthPx > 0) {
                    GlassSurface(
                        modifier = Modifier
                            .offset(x = animatedOffset, y = GlassTabThumbInset)
                            .width(animatedWidth)
                            .height(GlassTabRowHeight - GlassTabThumbInset * 2),
                        shape = RoundedCornerShape((GlassTabRowHeight - GlassTabThumbInset * 2) / 2),
                        fill = AppTokens.card,
                        strokeColor = glassTabThumbStroke(),
                        shadowColor = GlassTabThumbShadow,
                        shadowElevation = 6.dp,
                        shadowOffsetY = 1.dp,
                        strokeWidth = 0.dp,
                    ) {}
                }
                Row(modifier = Modifier.fillMaxHeight()) {
                    repeat(tabCount) { index ->
                        key(index) {
                            GlassTabItem(
                                index = index,
                                selected = index == selected,
                                onSelect = { onTabSelected(index) },
                                tabMenu = tabMenu,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .onSizeChanged { tabWidths[index] = it.width },
                                contentPadding = 18.dp,
                                label = label,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 纯文字可滚动分段控件的便捷重载。 */
@Composable
fun GlassScrollableTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabMenu: GlassTabMenu? = null,
) {
    GlassScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        tabCount = tabs.size,
        modifier = modifier,
        tabMenu = tabMenu,
    ) { index, selected ->
        GlassTabLabel(text = tabs[index], selected = selected)
    }
}

/**
 * 单个 tab：可选菜单包装 + 选中/未选中语义。
 *
 * 有菜单时：未选中 → 切换；已选中 → 打开菜单（与迁移前的 `TabClickMenu` 行为一致）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlassTabItem(
    index: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    tabMenu: GlassTabMenu?,
    modifier: Modifier,
    contentPadding: Dp = 0.dp,
    label: @Composable (index: Int, selected: Boolean) -> Unit,
) {
    val hasMenu = tabMenu != null && tabMenu.hasMenu(index)
    if (!hasMenu) {
        Box(
            modifier = modifier
                .selectable(
                    selected = selected,
                    role = Role.Tab,
                    onClick = { if (!selected) onSelect() },
                )
                .padding(horizontal = contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            label(index, selected)
        }
        return
    }
    val menuState = rememberMenuState()
    ClickMenu(
        menuContent = { tabMenu!!.content(this, index) },
        menuState = menuState,
    ) {
        Row(
            modifier = modifier
                .selectable(
                    selected = selected,
                    role = Role.Tab,
                    onClick = {
                        if (selected) menuState.expanded = true else onSelect()
                    },
                )
                .padding(horizontal = contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            label(index, selected)
            // 有排序菜单的 tab 给一个小 caret（原 TabClickMenu 的箭头 affordance）
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                tint = if (selected) AppTokens.primary else AppTokens.textSecondary,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(14.dp),
            )
        }
    }
}

/** 分段控件的默认文字：15sp，选中 w600 + accent，未选中次级色。 */
@Composable
fun GlassTabLabel(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 15.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) AppTokens.primary else AppTokens.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 带计数的文字 tab（消息页那类「回复我的 3」）。
 *
 * 计数用 accent 胶囊显示在文字右侧，选中时与文字同色。
 */
@Composable
fun GlassTabLabel(
    text: String,
    selected: Boolean,
    count: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        GlassTabLabel(text = text, selected = selected)
        if (count != null && count > 0) {
            Box(
                modifier = Modifier
                    .padding(start = 5.dp)
                    .clip(RoundedCornerShape(100))
                    .background(
                        if (selected) AppTokens.primary.copy(alpha = 0.16f)
                        else AppTokens.chip
                    )
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(
                    text = if (count > 99) "99+" else count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) AppTokens.primary else AppTokens.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}
