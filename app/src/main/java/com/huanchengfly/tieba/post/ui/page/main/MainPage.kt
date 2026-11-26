package com.huanchengfly.tieba.post.ui.page.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.huanchengfly.tieba.post.LocalDevicePosture
import com.huanchengfly.tieba.post.LocalNotificationCountFlow
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassAppBackground
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassBackdropHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowHeightSizeClass
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePage
import com.huanchengfly.tieba.post.ui.page.main.home.HomePage
import com.huanchengfly.tieba.post.ui.page.main.home.HomeUiIntent
import com.huanchengfly.tieba.post.ui.page.main.home.HomeViewModel
import com.huanchengfly.tieba.post.ui.page.main.notifications.NotificationsPage
import com.huanchengfly.tieba.post.ui.page.main.user.UserPage
import com.huanchengfly.tieba.post.ui.utils.DevicePosture
import com.huanchengfly.tieba.post.ui.utils.MainNavigationContentPosition
import com.huanchengfly.tieba.post.ui.utils.MainNavigationType
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.AppScaffold
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

@Composable
private fun NavigationWrapper(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    navigationType: MainNavigationType,
    navigationContentPosition: MainNavigationContentPosition,
    content: @Composable () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = navigationType == MainNavigationType.PERMANENT_NAVIGATION_DRAWER) {
            NavigationDrawerContent(
                currentPosition = currentPosition,
                onChangePosition = onChangePosition,
                onReselected = onReselected,
                navigationItems = navigationItems,
                navigationContentPosition = navigationContentPosition
            )
        }
        AnimatedVisibility(visible = navigationType == MainNavigationType.NAVIGATION_RAIL) {
            NavigationRail(
                currentPosition = currentPosition,
                onChangePosition = onChangePosition,
                onReselected = onReselected,
                navigationItems = navigationItems,
                navigationContentPosition = navigationContentPosition
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationGraphicsApi::class)
@RootNavGraph(start = true)
@Destination
@Composable
fun MainPage(
    navigator: DestinationsNavigator,
    viewModel: MainViewModel = pageViewModel<MainUiIntent, MainViewModel>(emptyList()),
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val account = LocalAccount.current
    val windowHeightSizeClass by rememberUpdatedState(newValue = windowSizeClass.heightSizeClass)
    val windowWidthSizeClass by rememberUpdatedState(newValue = windowSizeClass.widthSizeClass)
    val foldingDevicePosture by LocalDevicePosture.current

    val messageCount by viewModel.uiState.collectPartialAsState(
        prop1 = MainUiState::messageCount,
        initial = 0
    )

    val notificationCountFlow = LocalNotificationCountFlow.current
    LaunchedEffect(null) {
        notificationCountFlow.collect {
            viewModel.send(MainUiIntent.NewMessage.Receive(it))
        }
    }

    val hideExplore by rememberPreferenceAsState(
        key = booleanPreferencesKey("hideExplore"),
        defaultValue = LocalContext.current.appPreferences.hideExplore
    )
    val defaultStart by rememberPreferenceAsState(
        key = intPreferencesKey("defaultStart"),
        defaultValue = LocalContext.current.appPreferences.defaultStart
    )
    val pageCount by remember {
        derivedStateOf {
            if (hideExplore) 3 else 4
        }
    }
    val pagerState = rememberPagerState(
        pageCount = { pageCount },
        // 设置项 defaultStart：0=首页(动态)、1=进吧、2=消息。
        // 隐藏「动态」后页签前移一位，默认起始页需要跟着映射，否则会越界或被跳过。
        initialPage = if (hideExplore) (defaultStart - 1).coerceIn(0, pageCount - 1) else defaultStart
    )

    // issue #6：hideExplore 隐藏的是底部第一个页面（「动态」），它之后的页签索引整体前移一位。
    // 开关切换时按位移修正当前页，避免用户停留的内容被悄悄换掉或越界。
    var lastHideExplore by remember { mutableStateOf(hideExplore) }
    LaunchedEffect(hideExplore) {
        if (hideExplore != lastHideExplore) {
            val current = pagerState.currentPage
            lastHideExplore = hideExplore
            val target = if (hideExplore) current - 1 else current + 1
            pagerState.scrollToPage(target.coerceIn(0, if (hideExplore) 2 else 3))
        }
    }

    val homeViewModel = if (account != null) {
        pageViewModel<HomeUiIntent, HomeViewModel>(listOf(HomeUiIntent.Refresh))
    } else pageViewModel()

    val coroutineScope = rememberCoroutineScope()
    val themeColors = AppTokens
    val navigationItems by remember {
        derivedStateOf {
            listOfNotNull(
                if (hideExplore) null
                else NavigationItem(
                    id = "home",
                    icon = { AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_home) },
                    title = { stringResource(id = R.string.title_main) },
                    content = {
                        ExplorePage()
                    }
                ),
                NavigationItem(
                    id = "explore",
                    icon = {
                        AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_inventory_2)
                    },
                    title = { stringResource(id = R.string.title_explore) },
                    content = {
                        HomePage(viewModel = homeViewModel)
                    }
                ),
                NavigationItem(
                    id = "notification",
                    icon = {
                        AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_notifications)
                    },
                    title = { stringResource(id = R.string.title_notifications) },
                    badge = messageCount > 0,
                    badgeText = "$messageCount",
                    onClick = {
                        viewModel.send(MainUiIntent.NewMessage.Clear)
                    },
                    content = {
                        NotificationsPage()
                    }
                ),
                NavigationItem(
                    id = "user",
                    icon = {
                        AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_person)
                    },
                    title = { stringResource(id = R.string.title_user) },
                    content = {
                        UserPage()
                    }
                ),
            ).toImmutableList()
        }
    }
    val navigationType by remember {
        derivedStateOf {
            when (windowWidthSizeClass) {
                WindowWidthSizeClass.Compact -> {
                    MainNavigationType.BOTTOM_NAVIGATION
                }

                WindowWidthSizeClass.Medium -> {
                    MainNavigationType.NAVIGATION_RAIL
                }

                WindowWidthSizeClass.Expanded -> {
                    if (foldingDevicePosture is DevicePosture.BookPosture) {
                        MainNavigationType.NAVIGATION_RAIL
                    } else {
                        MainNavigationType.PERMANENT_NAVIGATION_DRAWER
                    }
                }

                else -> {
                    MainNavigationType.BOTTOM_NAVIGATION
                }
            }
        }
    }

    /**
     * Content inside Navigation Rail/Drawer can also be positioned at top, bottom or center for
     * ergonomics and reachability depending upon the height of the device.
     */
    val navigationContentPosition by remember {
        derivedStateOf {
            when (windowHeightSizeClass) {
                WindowHeightSizeClass.Compact -> {
                    MainNavigationContentPosition.TOP
                }

                WindowHeightSizeClass.Medium,
                WindowHeightSizeClass.Expanded -> {
                    MainNavigationContentPosition.CENTER
                }

                else -> {
                    MainNavigationContentPosition.TOP
                }
            }
        }
    }
    val onReselected: (Int) -> Unit = {
        coroutineScope.emitGlobalEvent(
            GlobalEvent.Refresh(key = navigationItems[it].id)
        )
    }
    // 主界面顶栏的状态：各 tab 页把顶栏内容登记进来，MainPage 在玻璃 overlay 层渲染当前页那一份。
    // 放在 navigationItems 之后初始化，这样第一帧就有 key，不会缺一帧顶栏。
    val topBarState = remember(navigationItems) {
        MainTopBarState(initialKey = navigationItems[pagerState.currentPage].id)
    }
    LaunchedEffect(pagerState.currentPage, navigationItems) {
        topBarState.setCurrent(navigationItems[pagerState.currentPage].id)
    }
    // 双击顶栏 = 回顶：列表收到 GlobalEvent.ScrollToTop 会滚回去，这里同步把玻璃/大标题状态复位。
    onGlobalEvent<GlobalEvent.ScrollToTop> {
        topBarState.resetScroll()
    }
    ProvideNavigator(navigator = navigator) {
        val showDock = navigationType == MainNavigationType.BOTTOM_NAVIGATION
        val onSelectPosition: (Int) -> Unit = {
            coroutineScope.launch { pagerState.scrollToPage(it) }
        }
        // 浮动胶囊本体。签名不变，只是多给一个 Modifier（玻璃叠加层里要 align 到底部）。
        //
        // DockStyle 是转正开关：GLASMIO = GlasmioNavigationBar（镜头/形变/收缩），
        // LEGACY = 原来的自绘胶囊。两条分支吃同一份参数、同一个槽位，回滚只改 DockStyle.kt。
        //
        // 滚动→收起的数据源复用顶栏那套：MainTopBarState 已经把每个 tab 的累计滚动量记下来了
        // （顶栏用它淡入 scrim、收大标题），这里只读当前 tab 的那一个，不额外挂 nestedScroll。
        // 注意 lambda 形式传下去：读数发生在 dock 自己的组合里，不会让 MainPage 每帧重组。
        val dockScrollOffset: () -> Float = remember(topBarState) {
            { topBarState.offsetFor(topBarState.currentKey) }
        }
        val dock: @Composable (Modifier) -> Unit = { dockModifier ->
            when (DefaultDockStyle) {
                DockStyle.GLASMIO -> GlasmioDock(
                    currentPosition = pagerState.currentPage,
                    onChangePosition = onSelectPosition,
                    onReselected = onReselected,
                    navigationItems = navigationItems,
                    modifier = dockModifier,
                    scrollOffsetPx = dockScrollOffset,
                )

                DockStyle.LEGACY -> BottomNavigation(
                    currentPosition = pagerState.currentPage,
                    onChangePosition = onSelectPosition,
                    onReselected = onReselected,
                    navigationItems = navigationItems,
                    themeColors = themeColors,
                    modifier = dockModifier,
                )
            }
        }
        // 玻璃叠加层里的胶囊不占 Scaffold 的 bottomBar 槽位：
        // 内容要能从胶囊**下面**滚过去（FlClash 就是这样），所以不能把这段高度做成 bottomBar
        // （那会把列表 viewport 切在胶囊上沿，胶囊后面永远是纯背景）。
        // 改成把「胶囊高度 + 底部边距」作为 contentPadding.bottom 交给各页的列表
        // （LocalMainTabBottomInset，MyLazyColumn / MyLazyVerticalGrid 自动消费）：
        // 最后一项能被滚到胶囊上方，滚到中途时内容正好从玻璃后面经过。
        val dockBottomMargin = navigationDockBottomMargin()
        val dockInset = GlassTokens.Spacing.dockHeight + dockBottomMargin

        NavigationWrapper(
            currentPosition = pagerState.currentPage,
            onChangePosition = onSelectPosition,
            onReselected = onReselected,
            navigationItems = navigationItems,
            navigationType = navigationType,
            navigationContentPosition = navigationContentPosition
        ) {
            // 真玻璃路径（仅底部导航 + API 33+）：
            // - sampledContent = 渐变底 + 整个页面（顶栏 + 分页内容），玻璃从这里取像素
            // - overlayContent = 胶囊 dock，画在录制层之上（放进去会自己采样自己）
            if (showDock && isLiquidGlassSupported()) {
                LiquidGlassBackdropHost(
                    modifier = Modifier.fillMaxSize(),
                    sampledContent = {
                        Box(modifier = Modifier.fillMaxSize()) {
                            GlassAppBackground(modifier = Modifier.fillMaxSize())
                            MainPageScaffold(
                                pagerState = pagerState,
                                navigationItems = navigationItems,
                                topBarState = topBarState,
                                bottomInset = dockInset,
                                bottomBar = {},
                            )
                        }
                    },
                    overlayContent = {
                        // 顶栏在玻璃层：本体透明、内容从下面流过；滚动后才渐显柔和 scrim。
                        MainTopBarHost(
                            state = topBarState,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                        dock(Modifier.align(Alignment.BottomCenter))
                    },
                )
            } else {
                // 降级 / 宽屏路径：胶囊照旧放回 bottomBar 槽位，玻璃表面自动退回伪玻璃。
                // 顶栏仍然走同一个 host，只是这里不在 Glasmio host 里，玻璃会自动降级成实底。
                Box(modifier = Modifier.fillMaxSize()) {
                    MainPageScaffold(
                        pagerState = pagerState,
                        navigationItems = navigationItems,
                        topBarState = topBarState,
                        bottomInset = 0.dp,
                        bottomBar = {
                            if (showDock) {
                                dock(Modifier)
                            }
                        },
                    )
                    MainTopBarHost(
                        state = topBarState,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        }
    }
    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.scrollToPage(0)
        }
    }
}

/**
 * 页面骨架：各页自己的顶栏（在 page content 里的 AppScaffold）+ 分页容器。
 *
 * [bottomBar] 由调用方决定：
 * - 真玻璃路径 → 空（胶囊在玻璃叠加层里，内容从它下面滚过去）；
 * - 降级路径 → 胶囊本身。
 *
 * [bottomInset] 是真玻璃路径下「胶囊高度 + 底部边距」，塞进各页列表的 contentPadding.bottom，
 * 保证最后一项能滚到胶囊上方，同时中途的内容可以从玻璃后面经过。
 */
@Composable
private fun MainPageScaffold(
    pagerState: PagerState,
    navigationItems: ImmutableList<NavigationItem>,
    topBarState: MainTopBarState,
    bottomInset: Dp,
    bottomBar: @Composable () -> Unit,
) {
    AppScaffold(
        backgroundColor = Color.Transparent,
        modifier = Modifier.fillMaxSize(),
        bottomBar = bottomBar,
    ) { paddingValues ->
        LazyLoadHorizontalPager(
            contentPadding = paddingValues,
            state = pagerState,
            key = { navigationItems[it].id },
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            userScrollEnabled = false
        ) {
            // 顶栏内容按当前 tab 登记：各页的 MainTabScaffold 通过这两个 local 找到 host。
            CompositionLocalProvider(
                LocalMainTopBarState provides topBarState,
                LocalMainTabKey provides navigationItems[it].id,
                LocalMainTabBottomInset provides bottomInset,
            ) {
                navigationItems[it].content()
            }
        }
    }
}

