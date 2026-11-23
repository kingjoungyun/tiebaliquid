package com.huanchengfly.tieba.post.ui.common.prefs

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassAppBackground
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassOverlayPage
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassBackdropHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalLiquidGlassHosted
import com.huanchengfly.tieba.post.ui.common.theme.glass.useOverlayTopBarHost
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.scaffold.GlasmioHeaderAction
import io.github.neoruaa.glasmio.scaffold.GlasmioHeaderGroup
import io.github.neoruaa.glasmio.scaffold.GlasmioHeaderScrollState
import io.github.neoruaa.glasmio.scaffold.GlasmioHeaderSpec
import io.github.neoruaa.glasmio.scaffold.GlasmioListPage
import io.github.neoruaa.glasmio.scaffold.GlasmioNavigationOperation
import io.github.neoruaa.glasmio.scaffold.GlasmioNavigationScaffold
import io.github.neoruaa.glasmio.scaffold.GlasmioPageTitle
import io.github.neoruaa.glasmio.scaffold.navigation.GlasmioNavHeaderMotion
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioHeaderCollapse
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

lateinit var LocalPrefsDataStore: ProvidableCompositionLocal<DataStore<Preferences>>

/**
 * Main preference screen which holds [PrefsListItem]s
 *
 * 两种渲染路径由 [PrefsUiStyle] 决定（默认 [PrefsUiStyle.GLASSMIO]，DSL 与 8 个业务页不变）：
 *
 * - **GLASSMIO**：外层 `GlasmioTheme`（经 [LiquidGlassTheme] 适配层注入 accent / 鸿蒙字体 /
 *   纯黑深色底）+ `GlasmioBackdropHost`（Glasmio 的开关要从 backdrop 取像素，没有 host 会抛异常），
 *   页面底色直接用 Glasmio 的 `groupedBackground`；一张卡片 = 一个 `LazyColumn` item，
 *   内部是 `GlasmioGroupedList`（行自己画分隔线）。
 * - **LEGACY**：迁移前的 FlClash 版式（渐变底 + 不透明白卡 + 1dp 缩进分割线），原样保留。
 *
 * **二级页范式（无 M2 顶栏）**：这里不再有任何 `TopAppBar` —— 页面背景（`groupedBackground` /
 * 渐变底）直接铺到状态栏下面，返回是左上角浮起的 44dp 圆形玻璃钮（[PrefsBackButton]），
 * 大标题由调用方以 `header` 传入、从返回钮下方直接起排。滚动收起暂未做，大标题保持静态。
 *
 * @param dataStore DataStore which will be used to save all the preferences
 * @param modifier Modifier applied to the [LazyColumn] holding the list of Prefs
 * @param dividerThickness 组内分割线粗细，0 为不画（仅 LEGACY 使用；Glasmio 的分隔线是内建的）
 * @param dividerIndent 组内分割线左侧缩进（仅 LEGACY 使用）
 * @param listState 外部可观察的滚动状态
 * @param onBack 非空时在左上角画悬浮返回钮（传 `navigator.navigateUp()`）
 * @param header 列表最顶部的滚动内容（设置页的大标题）；它随列表一起滚走
 * @param uiStyle 渲染层实现，默认 [PrefsUi.DefaultStyle]（Glasmio）；出问题可切回 [PrefsUiStyle.LEGACY]
 * @param content 分组的 DSL
 */
@Composable
fun PrefsScreen(
    dataStore: DataStore<Preferences>,
    modifier: Modifier = Modifier,
    dividerThickness: Dp = PrefsCardDefaults.DividerThickness,
    dividerIndent: Dp = PrefsCardDefaults.DividerIndent,
    listState: LazyListState = rememberLazyListState(),
    onBack: (() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    /** Stage 3 试点：非空时启用 Glasmio 页面骨架（折叠大标题 + 顶栏按钮槽），大标题由骨架负责，
     *  此时 [header] 不再渲染。为 null 时完全走旧版式（回滚用）。 */
    title: String? = null,
    uiStyle: PrefsUiStyle = PrefsUi.DefaultStyle,
    content: PrefsScope.() -> Unit
) {
    LocalPrefsDataStore = staticCompositionLocalOf { dataStore }
    val prefsScope = PrefsScopeImpl().apply(content)

    // Now the dataStore can be accessed by calling LocalPrefsDataStore.current from any child Pref
    CompositionLocalProvider(
        LocalPrefsDataStore provides dataStore,
        LocalPrefsUiStyle provides uiStyle,
    ) {
        // 只在这一支包 GlasmioTheme：开关要读它的明暗令牌（明暗跟随 App 自己的
        // AppTokens.isNightMode，不是系统）。不包整个 App，避免 M3 主题污染其它页面。
        LiquidGlassTheme(accent = AppTokens.primary) {
            when (uiStyle) {
                PrefsUiStyle.GLASSMIO -> GlasmioPrefsPage(
                    prefsScope = prefsScope,
                    listState = listState,
                    header = header,
                    title = title,
                    onBack = onBack,
                    modifier = modifier,
                )

                PrefsUiStyle.LEGACY -> LegacyPrefsPage(
                    prefsScope = prefsScope,
                    listState = listState,
                    header = header,
                    onBack = onBack,
                    modifier = modifier,
                    dividerThickness = dividerThickness,
                    dividerIndent = dividerIndent,
                )
            }
        }
    }
}

/** 页面上方给悬浮返回钮 + 状态栏留的空间；下方给导航栏留白。 */
@Composable
private fun prefsPageContentPadding(
    horizontal: Dp,
    topGap: Dp,
    bottomGap: Dp,
): PaddingValues {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(
        start = horizontal,
        end = horizontal,
        top = statusBar + PrefsGlasmioDefaults.BackButtonTopSpace + topGap,
        bottom = navigationBar + bottomGap,
    )
}

/**
 * Glasmio 版渲染：分组标题 + 26dp 连续圆角分组列表 + 组尾 footnote。
 *
 * 为什么整页只开一个 [GlasmioBackdropHost] 而不是每个开关一个：
 * `GlasmioSwitch` 默认从 `LocalGlassBackdrop` 取采样源，而这个 local 的默认实现是
 * `error(...)`。页面级 host 用 `sampledColor`（`fillsBoundsUniformly`，Glasmio 专门给
 * 「滚动内容里的控件」留的路径，移动时复用已渲染结果）采样一个纯色底，代价最小；
 * 同时这里把 App 自己的 [LocalLiquidGlassHosted] 置为 true，于是 `widgets/PrefsSwitch.kt`
 * 不会再为每个开关套一层 host，返回钮也能直接用 Glasmio 的玻璃圆钮。
 */
@Composable
private fun GlasmioPrefsPage(
    prefsScope: PrefsScopeImpl,
    listState: LazyListState,
    header: (@Composable () -> Unit)?,
    title: String?,
    onBack: (() -> Unit)?,
    modifier: Modifier,
) {
    val colors = GlasmioTheme.colors

    // 列表本体（三种方案共用，避免复制一份）：分组标题 + 卡片 + 组尾 footnote
    val listItems: LazyListScope.() -> Unit = {
        if (header != null) {
            // 大标题仍然由页面自己发布（Glasmio 的 GlasmioPageTitle 在「非 Nav3 entry」场景下
            // 拿不到 leading 端点的测量值，会静默返回、不画标题 —— 见简报「遗留」）。
            // 所以这里保留原来的大标题，骨架负责顶栏（返回钮槽 + 折叠后的紧凑标题）。
            item(key = "prefs-header") {
                // 大标题与卡片左边缘对齐（16dp）；分组标题自己再缩 16dp 对齐卡片内容
                header()
            }
        }
        items(
            count = prefsScope.sections.size,
            key = { index -> "prefs-card-$index" },
        ) { index ->
            val section = prefsScope.sections[index]
            if (section.itemIndexes.isEmpty()) return@items
            val cardHeader: (@Composable () -> Unit)? =
                section.headerIndex?.let { headerIndex ->
                    { prefsScope.getPrefsItem(headerIndex)() }
                }
            val footnotes = remember(index) { PrefsFootnoteState() }
            CompositionLocalProvider(LocalPrefsFootnotes provides footnotes) {
                PrefsCard(
                    header = cardHeader,
                    headerTopSpacing = 0.dp,
                    footer = section.footer,
                ) {
                    section.itemIndexes.forEachIndexed { rowIndex, itemIndex ->
                        CompositionLocalProvider(LocalPrefsRowIndex provides rowIndex) {
                            prefsScope.getPrefsItem(itemIndex)()
                        }
                    }
                }
            }
        }
    }

    // 旧版式（纯色/活 backdrop 两条路径）共用的列表容器
    val pageList: @Composable () -> Unit = {
        Container {
            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding = prefsPageContentPadding(
                    horizontal = PrefsGlasmioDefaults.PageHorizontalPadding,
                    topGap = 0.dp,
                    bottomGap = PrefsGlasmioDefaults.PageBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(PrefsGlasmioDefaults.GroupGap),
            ) {
                listItems()
            }
        }
    }

    if (title != null && prefsScaffoldSkeletonEnabled()) {
        GlasmioPrefsScaffold(
            title = title,
            listState = listState,
            onBack = onBack,
            modifier = modifier,
            listItems = listItems,
        )
        return
    }

    if (useOverlayTopBarHost(GlassOverlayPage.PREFS)) {
        // P2 试点：双层 host —— 列表进 sampledContent（被录制），返回钮进 overlayContent。
        // overlay 里的按钮不在录制层内，GlassIconButton 会走「采真实内容」分支 → 真玻璃，
        // 而列表从返回钮下面滚过时玻璃能折射出下面的卡片。
        LiquidGlassBackdropHost(
            modifier = Modifier.fillMaxSize(),
            sampledContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.groupedBackground),
                ) {
                    pageList()
                }
            },
            overlayContent = {
                if (onBack != null) {
                    PrefsBackButtonOverlay(onBack = onBack)
                }
            },
        )
    } else {
        // 回退路径（原来的纯色 host）：按钮只能自采样纯色 + rim。
        GlasmioBackdropHost(
            sampledColor = colors.groupedBackground,
            modifier = Modifier.fillMaxSize(),
        ) {
            CompositionLocalProvider(LocalLiquidGlassHosted provides true) {
                Box(modifier = Modifier.fillMaxSize()) {
                    pageList()
                    if (onBack != null) {
                        PrefsBackButtonOverlay(onBack = onBack)
                    }
                }
            }
        }
    }
}

/**
 * Stage 3 试点：用 `glasmio-scaffold` 的页面骨架承载设置页。
 *
 * 只换**外壳**，列表内容（[listItems]：分组卡 + 行 + 组尾 footnote）原样复用：
 * - 大标题由骨架的 `GlasmioHeaderSpec.title` 负责（折叠成顶栏小标题），不再画 34sp 手写标题；
 * - 返回钮放进骨架的 `leading` 按钮槽（`GlasmioHeaderAction`），玻璃由骨架自己的
 *   `layerBackdrop(reuseContent = true)` 录制，采的是真实页面内容；
 * - 折叠进度用 `rememberGlasmioHeaderCollapse(listState)` 桥接我们自己的 `LazyListState`；
 * - 列表仍是我们自己的 `LazyColumn`（`GlashmioListPage` 只是给 `LazyColumn` 套一层 overscroll，
 *   形态与 `PrefsCard` 不冲突，所以没被迫改数据结构）。
 *
 * 骨架内部自己会录一层 backdrop，因此这里**不再**套 `LiquidGlassBackdropHost`（避免双录）。
 * 代价：页面内的 `GlasmioSwitch` 拿不到 `LocalGlassBackdrop`，会各自退化成
 * 「自采样纯色 + rim」的小 host（观感与改造前一致，不是回归）。
 */
@Composable
private fun GlasmioPrefsScaffold(
    title: String,
    listState: LazyListState,
    onBack: (() -> Unit)?,
    modifier: Modifier,
    listItems: LazyListScope.() -> Unit,
) {
    val collapseProgress = rememberGlasmioHeaderCollapse(listState)
    val headerScrollState = remember(collapseProgress) {
        GlasmioHeaderScrollState(collapseProgress = { collapseProgress })
    }
    // 非 Nav3 页面：给骨架一个恒定 entryKey，导航帧永远 Idle（不做跨页 header 过渡）
    val entryKeyForTitle = remember { Any() }
    val motion = remember(entryKeyForTitle) {
        GlasmioNavHeaderMotion(entryKeyForTitle) { _, _, _ -> GlasmioNavigationOperation.Push }
    }
    GlasmioNavigationScaffold(
        motion = motion,
        modifier = modifier.fillMaxSize(),
        header = {
            GlasmioHeaderSpec(
                title = title,
                leading = if (onBack != null) {
                    listOf(
                        GlasmioHeaderGroup(
                            key = "navigation",
                            actions = listOf(
                                GlasmioHeaderAction(
                                    key = "navigation.back",
                                    visualKey = "chevron.backward",
                                    contentDescription = PrefsBackButtonLabel,
                                    onClick = onBack,
                                ) {
                                    SfIcon(SfSymbol.ChevronBack, null, mirrored = true)
                                },
                            ),
                        )
                    )
                } else {
                    emptyList()
                },
                scrollState = headerScrollState,
            )
        },
        content = { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                GlasmioListPage(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = padding,
                    state = listState,
                ) {
                    listItems()
                }
                // 骨架的「大标题」是页面自己发布出来的（放在 body 之后、盖在内容上），
                // 不是 header 槽画出来的 —— 少了这一句页面就没有大标题（只有顶栏返回钮）。
                GlasmioPageTitle(entryKey = entryKeyForTitle)
            }
        },
    )
}

private const val PrefsBackButtonLabel = "返回"

/**
 * 骨架开关的调试覆盖：`adb shell settings put global tieba_prefs_skeleton 0` 后重进页面即为旧版式
 * （同一 APK 免重装；release 恒为 true）。LEGACY 版式仍由 [PrefsUi.DefaultStyle] 控制，互不影响。
 */
@Composable
private fun prefsScaffoldSkeletonEnabled(): Boolean {
    if (!BuildConfig.DEBUG) return true
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getInt(context.contentResolver, "tieba_prefs_skeleton", 1) == 1
        }.getOrDefault(true)
    }
}

/**
 * A 版（FlClash 卡片）渲染：保留原实现，只把 `footer` / `onBack` 一起传下去。
 *
 * 每张卡片 = 一个 LazyColumn item，卡片内部是普通 Column + 1dp 缩进分割线；
 * 卡片本身是不透明表层色 + 16dp 连续圆角 + 无阴影（玻璃只给浮层用）。
 * 顶栏同样去掉，返回用同一个悬浮圆钮，保证切回 A 版时页面不「丢导航」。
 */
@Composable
private fun LegacyPrefsPage(
    prefsScope: PrefsScopeImpl,
    listState: LazyListState,
    header: (@Composable () -> Unit)?,
    onBack: (() -> Unit)?,
    modifier: Modifier,
    dividerThickness: Dp,
    dividerIndent: Dp,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 页面底色：App 统一的渐变底（不是玻璃），白卡片压在上面才有层次
        GlassAppBackground(modifier = Modifier.fillMaxSize())
        Container {
            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding = prefsPageContentPadding(
                    horizontal = PrefsCardDefaults.PagePadding,
                    topGap = 0.dp,
                    bottomGap = PrefsCardDefaults.PagePadding,
                ),
                verticalArrangement = Arrangement.spacedBy(PrefsCardDefaults.CardGap),
            ) {
                if (header != null) {
                    item(key = "prefs-header") { header() }
                }
                items(
                    count = prefsScope.sections.size,
                    key = { index -> "prefs-card-$index" },
                ) { index ->
                    val section = prefsScope.sections[index]
                    if (section.itemIndexes.isEmpty()) return@items
                    // 一张卡片：可选分组标题 + 卡片本体（内部带分割线）
                    val cardHeader: (@Composable () -> Unit)? =
                        section.headerIndex?.let { headerIndex ->
                            { prefsScope.getPrefsItem(headerIndex)() }
                        }
                    val footnotes = remember(index) { PrefsFootnoteState() }
                    CompositionLocalProvider(LocalPrefsFootnotes provides footnotes) {
                        PrefsCard(
                            header = cardHeader,
                            // 首组的上间距由 contentPadding 提供；其余组 = 组间距 8dp + 16dp = 24dp
                            headerTopSpacing = if (index == 0) 0.dp
                            else PrefsCardDefaults.HeaderTopSpacing,
                            footer = section.footer,
                        ) {
                            section.itemIndexes.forEachIndexed { rowIndex, itemIndex ->
                                if (rowIndex > 0 && dividerThickness > 0.dp) {
                                    Divider(
                                        thickness = dividerThickness,
                                        indent = dividerIndent,
                                        color = PrefsCardDefaults.divider(),
                                    )
                                }
                                CompositionLocalProvider(
                                    LocalPrefsRowIndex provides rowIndex
                                ) {
                                    prefsScope.getPrefsItem(itemIndex)()
                                }
                            }
                        }
                    }
                }
            }
        }
        if (onBack != null) {
            PrefsBackButtonOverlay(onBack = onBack)
        }
    }
}
