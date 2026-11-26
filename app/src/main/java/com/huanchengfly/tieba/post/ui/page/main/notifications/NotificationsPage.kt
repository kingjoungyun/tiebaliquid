package com.huanchengfly.tieba.post.ui.page.main.notifications

import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import androidx.compose.material.Icon
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.common.theme.glass.HarmonyTextProvider
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.page.main.MainTabScaffold
import com.huanchengfly.tieba.post.ui.page.main.MainTopBarRow
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsListPage
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.AppScaffold
import androidx.compose.material.TabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import kotlinx.coroutines.launch
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassComponent
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassTabRow
import com.huanchengfly.tieba.post.ui.common.components.glass.useGlassComponent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues

@OptIn(ExperimentalFoundationApi::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://notifications/{initialTab}")
    ]
)
@Composable
fun NotificationsPage(
    navigator: DestinationsNavigator,
    initialTab: Int = 0,
) {
    val pages = listOf<Pair<String, (@Composable () -> Unit)>>(
        stringResource(id = R.string.title_reply_me) to @Composable {
            NotificationsListPage(type = NotificationsType.ReplyMe)
        },
        stringResource(id = R.string.title_at_me) to @Composable {
            NotificationsListPage(type = NotificationsType.AtMe)
        }
    )
    val pagerState = rememberPagerState(
        initialPage = initialTab,
    ) { pages.size }
    val glassmio = NotificationsUi.DefaultStyle == NotificationsUiStyle.GLASMIO
    LiquidGlassTheme(accent = AppTokens.primary) {
        HarmonyTextProvider {
            ProvideNavigator(navigator = navigator) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (glassmio) GlasmioTheme.colors.groupedBackground
                            else Color.Transparent
                        ),
                ) {
                    NotificationsHeader(
                        pagerState = pagerState,
                        pages = pages,
                        showBack = true,
                        withStatusBarPadding = true,
                        onBack = { navigator.navigateUp() },
                        onSearch = { navigator.navigate(SearchPageDestination) },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
            LazyLoadHorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(),
                key = { pages[it].first },
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                userScrollEnabled = true,
            ) {
                pages[it].second()
            }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationsPage(
    initialTab: Int = 0,
) {
    val navigator = LocalNavigator.current
    val pages = listOf<Pair<String, (@Composable () -> Unit)>>(
        stringResource(id = R.string.title_reply_me) to @Composable {
            NotificationsListPage(type = NotificationsType.ReplyMe)
        },
        stringResource(id = R.string.title_at_me) to @Composable {
            NotificationsListPage(type = NotificationsType.AtMe)
        }
    )
    val pagerState = rememberPagerState(
        initialPage = initialTab,
    ) { pages.size }
    val coroutineScope = rememberCoroutineScope()
    LiquidGlassTheme(accent = AppTokens.primary) {
        HarmonyTextProvider {
    MainTabScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            NotificationsHeader(
                pagerState = pagerState,
                pages = pages,
                showBack = false,
                withStatusBarPadding = false,
                onBack = {},
                onSearch = { navigator.navigate(SearchPageDestination) },
            )
        },
    ) { paddingValues ->
        LazyLoadHorizontalPager(
            state = pagerState,
            contentPadding = paddingValues,
            key = { pages[it].first },
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            userScrollEnabled = true,
        ) {
            pages[it].second()
        }
    }
}
}
}

@Composable
private fun NotificationsHeader(
    pagerState: PagerState,
    pages: List<Pair<String, (@Composable () -> Unit)>>,
    showBack: Boolean,
    withStatusBarPadding: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    // T3：TabRow 组件级回滚开关
    val useGlassTabRow = useGlassComponent(GlassComponent.TabRow)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlasmioTheme.colors.groupedBackground)
            .then(if (withStatusBarPadding) Modifier.statusBarsPadding() else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                GlassIconButton(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(id = R.string.button_back),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            GlassIconButton(
                onClick = onSearch,
                icon = PhosphorIcons["magnifying-glass"] ?: Icons.Rounded.Search,
                contentDescription = stringResource(id = R.string.title_search),
            )
        }
        Text(
            text = stringResource(id = R.string.title_notifications),
            style = LocalTextStyle.current.copy(
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = AppTokens.text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (useGlassTabRow) {
            // T3：iOS 分段控件风格（灰色轨道 + 白色滑块）。回滚见下面的 else 分支。
            GlassTabRow(
                tabs = pages.map { it.first },
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            // 回滚：M2 官方 TabRow（原来的 M2 分叉实现已随 Tabs.kt 删除）
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                divider = {},
                backgroundColor = Color.Transparent,
                contentColor = AppTokens.primary,
            ) {
                pages.forEachIndexed { index, pair ->
                    Tab(
                        text = {
                            Text(
                                text = pair.first,
                                fontSize = 15.sp,
                                fontWeight = if (pagerState.currentPage == index) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                },
                            )
                        },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        selectedContentColor = AppTokens.onTopBar,
                        unselectedContentColor = AppTokens.onTopBarSecondary,
                    )
                }
            }
        }
    }
}