package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.White
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassSurface
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalLiquidGlassHosted
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassNavigationTint
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import io.github.neoruaa.glasmio.core.LocalGlassColors as GlasmioLocalGlassColors
import io.github.neoruaa.glasmio.glass.NavigationGlassMaterial
import com.huanchengfly.tieba.post.ui.utils.MainNavigationContentPosition
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import kotlinx.collections.immutable.ImmutableList

enum class LayoutType {
    HEADER, CONTENT
}

@Composable
fun PermanentNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(modifier.fillMaxSize()) {
        drawerContent()
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

private val ActiveIndicatorHeight = 56.dp
private val ActiveIndicatorWidth = 240.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NavigationDrawerItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = Color.Transparent,
    selectedBackgroundColor: Color = MaterialTheme.colors.primary.copy(0.25f),
    itemColor: Color = MaterialTheme.colors.onSurface,
    selectedItemColor: Color = MaterialTheme.colors.primary,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier
            .height(ActiveIndicatorHeight)
            .fillMaxWidth(),
        shape = shape,
        color = if (selected) selectedBackgroundColor else backgroundColor,
        interactionSource = interactionSource,
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                val iconColor = if (selected) selectedItemColor else itemColor
                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
                Spacer(Modifier.width(12.dp))
            }
            Box(Modifier.weight(1f)) {
                val labelColor = if (selected) selectedItemColor else itemColor
                CompositionLocalProvider(LocalContentColor provides labelColor, content = label)
            }
            if (badge != null) {
                Spacer(Modifier.width(12.dp))
                val badgeColor = if (selected) selectedItemColor else itemColor
                CompositionLocalProvider(LocalContentColor provides badgeColor, content = badge)
            }
        }
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun NavigationDrawerContent(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    navigationContentPosition: MainNavigationContentPosition
) {
    PositionLayout(
        modifier = Modifier
            .width(ActiveIndicatorWidth)
            .background(ExtendedTheme.colors.bottomBar)
            .padding(16.dp),
        content = {
            Column(
                modifier = Modifier
                    .layoutId(LayoutType.HEADER)
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp) // NavigationRailVerticalPadding
            ) {
                val account = LocalAccount.current
                if (account != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        AccountNavIcon(spacer = false, size = Sizes.Large)
                        Text(
                            text = account.nameShow ?: account.name,
                            style = MaterialTheme.typography.subtitle1,
                            color = ExtendedTheme.colors.text
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(
                            data = R.drawable.ic_launcher_new_round,
                            size = Sizes.Small,
                            contentDescription = stringResource(id = R.string.app_name)
                        )
                        Text(
                            text = stringResource(id = R.string.app_name).uppercase(),
                            style = MaterialTheme.typography.h6,
                            color = ExtendedTheme.colors.primary
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .layoutId(LayoutType.CONTENT)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                navigationItems.fastForEachIndexed { index, navigationItem ->
                    NavigationDrawerItem(
                        selected = index == currentPosition,
                        onClick = {
                            if (index == currentPosition) {
                                onReselected(index)
                            } else {
                                onChangePosition(index)
                            }
                        },
                        label = { Text(text = navigationItem.title(index == currentPosition)) },
                        icon = {
                            Box {
                                Icon(
                                    painter = rememberAnimatedVectorPainter(
                                        animatedImageVector = navigationItem.icon(),
                                        atEnd = index == currentPosition
                                    ),
                                    contentDescription = navigationItem.title(index == currentPosition),
                                    modifier = Modifier.size(24.dp),
                                )
                                if (navigationItem.badge) {
                                    Text(
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        color = White,
                                        text = navigationItem.badgeText ?: "",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .align(Alignment.TopEnd)
                                            .background(
                                                color = MaterialTheme.colors.secondary,
                                                shape = CircleShape
                                            ),
                                    )
                                }
                            }
                        }
                    )
                }
            }
        },
        navigationContentPosition = navigationContentPosition
    )
}

@Composable
private fun PositionLayout(
    navigationContentPosition: MainNavigationContentPosition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
        measurePolicy = { measurables, constraints ->
            lateinit var headerMeasurable: Measurable
            lateinit var contentMeasurable: Measurable
            measurables.forEach {
                when (it.layoutId) {
                    LayoutType.HEADER -> headerMeasurable = it
                    LayoutType.CONTENT -> contentMeasurable = it
                    else -> error("Unknown layoutId encountered!")
                }
            }

            val headerPlaceable = headerMeasurable.measure(constraints)
            val contentPlaceable = contentMeasurable.measure(
                constraints.offset(vertical = -headerPlaceable.height)
            )
            layout(constraints.maxWidth, constraints.maxHeight) {
                // Place the header, this goes at the top
                headerPlaceable.placeRelative(0, 0)

                // Determine how much space is not taken up by the content
                val nonContentVerticalSpace = constraints.maxHeight - contentPlaceable.height

                val contentPlaceableY = when (navigationContentPosition) {
                    // Figure out the place we want to place the content, with respect to the
                    // parent (ignoring the header for now)
                    MainNavigationContentPosition.TOP -> 0
                    MainNavigationContentPosition.CENTER -> nonContentVerticalSpace / 2
                }
                    // And finally, make sure we don't overlap with the header.
                    .coerceAtLeast(headerPlaceable.height)

                contentPlaceable.placeRelative(0, contentPlaceableY)
            }
        }
    )
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun NavigationRail(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    navigationContentPosition: MainNavigationContentPosition
) {
    NavigationRail(
        backgroundColor = ExtendedTheme.colors.bottomBar,
        contentColor = ExtendedTheme.colors.unselected,
        modifier = Modifier
            .fillMaxHeight()
            .statusBarsPadding(),
        elevation = 0.dp,
        header = {
            AccountNavIcon(spacer = false)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = if (navigationContentPosition == MainNavigationContentPosition.TOP) Arrangement.Top else Arrangement.Center
        ) {
            navigationItems.forEachIndexed { index, navigationItem ->
                NavigationRailItem(
                    selected = index == currentPosition,
                    onClick = {
                        if (index == currentPosition) {
                            onReselected(index)
                        } else {
                            onChangePosition(index)
                        }
                    },
                    icon = {
                        Box {
                            Icon(
                                painter = rememberAnimatedVectorPainter(
                                    animatedImageVector = navigationItem.icon(),
                                    atEnd = index == currentPosition
                                ),
                                contentDescription = navigationItem.title(index == currentPosition),
                                modifier = Modifier.size(16.dp),
                            )
                            if (navigationItem.badge) {
                                Text(
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    color = White,
                                    text = navigationItem.badgeText ?: "",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .align(Alignment.TopEnd)
                                        .background(
                                            color = MaterialTheme.colors.secondary,
                                            shape = CircleShape
                                        ),
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationDivider(
    themeColors: ExtendedColors = ExtendedTheme.colors
) {
    if (!themeColors.isNightMode) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(themeColors.divider)
        )
    }
}

/**
 * 浮动胶囊距屏幕底的距离，抄 FlClash `navigation_dock.dart:329`：
 * `max(_edgeMargin = 21dp, 系统导航栏 inset)`。
 *
 * 关键点是「取大」而不是「相加」：手势条本身已经占了 inset 那一段，再加 12dp 就会在
 * 手势条上方多出一条纯背景的带。
 */
@Composable
fun navigationDockBottomMargin(): Dp {
    val density = LocalDensity.current
    val navInset = WindowInsets.navigationBars.getBottom(density)
    return with(density) {
        maxOf(GlassTokens.Spacing.dockMarginBottom, navInset.toDp())
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun BottomNavigation(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    // 签名保持不变：调用方（MainPage）和宽屏 Rail / Drawer 分支都不受影响。
    // 浮动胶囊用的是 glass 令牌，不再需要这个 M2 色板，这里只为兼容保留。
    themeColors: ExtendedColors = ExtendedTheme.colors,
    modifier: Modifier = Modifier,
) {
    val glass = glassColors()
    // 选中态 accent 的唯一来源：App 主题色（glassColors().primary 已同步，这里显式取名便于阅读）。
    val accent = ExtendedTheme.colors.primary.let { if (it == Color.Unspecified) glass.primary else it }
    // FlClash `LiquidGlassStyle.regular` 的 tint：浅色 #8AFDFDFD，深色 container +7 tone / .62。
    // 传进 Glasmio 的 navigationSurfaceColor，材质会把这层 tint 叠在自身的 fill 上，
    // 浅色下从「灰板子」变成发亮的玻璃；深色下靠这一步把壳从纯黑里提出来。
    val navigationTint = glassNavigationTint()
    // 浮动玻璃胶囊，由 MainPage 放进 GlasmioBackdropHost 的 overlayContent（玻璃层）绘制。
    //
    // 位置对齐 FlClash `navigation_dock.dart:329`：
    //   _bottomMarginOf = max(_edgeMargin(21), MediaQuery.padding.bottom)
    // 也就是「距屏幕底 21dp，系统导航栏更高时以 inset 为准」，**不是** inset + 12dp 的叠加
    // （叠加会在手势条上面多留一条纯背景的带）。所以这里不再用 navigationBarsPadding()，
    // 而是把 inset 的**高度值**取出来参与 offset 计算。
    //
    // 背景走 LiquidGlassSurface：
    // - 有 Glasmio backdrop host → 真玻璃（折射 + 模糊，材质 NavigationGlassMaterial.RegularSmall）
    // - 没有（宽屏分支 / API < 33 / 玻璃层被关掉）→ 自动退回 P2 的伪玻璃
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = GlassTokens.Spacing.dockMarginH,
                end = GlassTokens.Spacing.dockMarginH,
                top = GlassTokens.Spacing.dockMarginBottom,
                bottom = navigationDockBottomMargin(),
            )
    ) {
        LiquidGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            material = GlassTokens.dockMaterial,
            navigationSurfaceColor = navigationTint,
            // FlClash 的 58% 是它自己管线里的**最终**不透明度；Glasmio 会在这层 tint 之上
            // 再叠材质自身的 fill，照搬 1f 会叠到 ~0.75，胶囊后面什么都看不见。
            // 压到 dockTintMultiplier 后内容能隐约透出（滚动时看得出底下有图/文字在动）。
            navigationSurfaceAlphaMultiplier = GlassTokens.dockTintMultiplier(dark = glass.isDark),
            innerGlow = true,
        ) {
            // 前景色跟随「材质自身的明暗」：
            // Glasmio 会按采样到的背景解析材质的明暗（深色模式下既可能是浅灰材质、也可能是深玻璃），
            // 用 App 色板里固定的 textSecondary 会在这两种情况下糊掉；
            // GlassSurface 会在 content 里提供按材质解析过的 LocalGlassColors（浅玻璃→黑、深玻璃→白）。
            // 伪玻璃降级路径没有这层语义，继续用 App 自己的色板。
            val hosted = LocalLiquidGlassHosted.current && isLiquidGlassSupported()
            val unselectedTint = if (hosted) {
                GlasmioLocalGlassColors.current.content.copy(alpha = 0.62f)
            } else {
                glass.textSecondary
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GlassTokens.Spacing.dockHeight),
            ) {
                navigationItems.fastForEachIndexed { index, navigationItem ->
                    val selected = index == currentPosition
                    DockItem(
                        navigationItem = navigationItem,
                        selected = selected,
                        tint = if (selected) accent else unselectedTint,
                        pillColor = accent.copy(alpha = GlassTokens.Glass.selectedPillAlpha),
                        onClick = {
                            if (index == currentPosition) {
                                onReselected(index)
                            } else {
                                onChangePosition(index)
                            }
                            navigationItem.onClick?.invoke()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * 胶囊里的一项：24dp 图标 + 11sp 文字。
 * 选中时整块套一层「主色 12%」的胶囊高亮，图标/文字用主色；未选中用文字次色。
 */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun DockItem(
    navigationItem: NavigationItem,
    selected: Boolean,
    tint: Color,
    pillColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = navigationItem.title(selected)
    Column(
        modifier = modifier
            .fillMaxHeight()
            // FlClash `navigation_dock.dart:25`：_barPadding = 4 是「上下左右」同一个值，
            // 滑块高度 = 62 - 4*2 = 54dp，与胶囊内缘只留 4dp。
            .padding(
                horizontal = GlassTokens.Spacing.dockInsetH,
                vertical = GlassTokens.Spacing.dockInsetH,
            )
            .clip(CircleShape)
            .then(if (selected) Modifier.background(pillColor) else Modifier)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(
                painter = rememberAnimatedVectorPainter(
                    animatedImageVector = navigationItem.icon(),
                    atEnd = selected,
                ),
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
            if (navigationItem.badge) {
                Text(
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = White,
                    text = navigationItem.badgeText ?: "",
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .align(Alignment.TopEnd)
                        .background(
                            color = MaterialTheme.colors.secondary,
                            shape = CircleShape
                        ),
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Immutable
data class NavigationItem @OptIn(ExperimentalAnimationGraphicsApi::class) constructor(
    val id: String,
    val icon: @Composable () -> AnimatedImageVector,
    val title: @Composable (selected: Boolean) -> String,
    val badge: Boolean = false,
    val badgeText: String? = null,
    val onClick: (() -> Unit)? = null,
    val content: @Composable () -> Unit = {},
)