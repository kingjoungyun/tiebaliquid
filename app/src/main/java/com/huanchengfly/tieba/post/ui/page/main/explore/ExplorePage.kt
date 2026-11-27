package com.huanchengfly.tieba.post.ui.page.main.explore

import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.page.main.MainTabScaffold
import com.huanchengfly.tieba.post.ui.page.main.MainTopBarRow
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernPage
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.PersonalizedPage
import com.huanchengfly.tieba.post.ui.page.searchuser.SearchUserTabPage
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import androidx.compose.material.TabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.toolboxAccountNavIconIfCompact
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import io.github.neoruaa.glasmio.glass.GlassSurface
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalLiquidGlassHosted


@Immutable
data class ExplorePageItem(
    val id: String,
    val name: @Composable (selected: Boolean) -> Unit,
    val content: @Composable () -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExplorePageTab(
    pagerState: PagerState,
    pages: ImmutableList<ExplorePageItem>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // LEGACY 回滚分支：用 M2 官方 TabRow（原来的 M2 分叉 Tabs.kt 已删）
    TabRow(
        selectedTabIndex = pagerState.currentPage,
        divider = {},
        backgroundColor = Color.Transparent,
        contentColor = AppTokens.onTopBar,
        modifier = modifier.width(76.dp * pages.size),
    ) {
        pages.fastForEachIndexed { index, item ->
            Tab(
                text = { item.name(pagerState.currentPage == index) },
                selected = pagerState.currentPage == index,
                onClick = {
                    coroutineScope.launch {
                        if (pagerState.currentPage == index) {
                            emitGlobalEvent(GlobalEvent.Refresh(item.id))
                        } else {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun TabText(
    text: String,
    selected: Boolean
) {
    val style = MaterialTheme.typography.button.copy(
        letterSpacing = 0.75.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center
    )
    Text(text = text, style = style)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExplorePage() {
    val account = LocalAccount.current
    val navigator = LocalNavigator.current
    val glassmio = ExplorePageUi.DefaultStyle == ExplorePageUiStyle.GLASMIO

    if (glassmio) {
        MainTabScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    // 这个玻璃搜索框在「顶栏槽」里渲染，槽内容可能不在 `GlasmioBackdropHost` 之内
                    // （宽窗口下顶栏槽走 MainTopBarHost 的 overlay 分支时就是这种情况），
                    // 那样 GlassSurface 会抛 `IllegalStateException: Glass controls must be hosted by …`。
                    // 已经处于 host 内就直接画（采样真实页面），否则自备一个纯色 host。
                    val searchBox: @Composable () -> Unit = {
                        GlassSurface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(24.dp),
                            onClick = { navigator.navigate(SearchPageDestination) },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons["magnifying-glass"] ?: Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = AppTokens.textSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.title_search),
                                    fontSize = 15.sp,
                                    color = AppTokens.textSecondary,
                                )
                            }
                        }
                    }
                    if (LocalLiquidGlassHosted.current) {
                        searchBox()
                    } else {
                        GlasmioBackdropHost(sampledColor = AppTokens.groupedBackground) {
                            searchBox()
                        }
                    }
                }
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                PersonalizedPage()
            }
        }
        return
    }

    val loggedIn = remember(account) { account != null }

    val pages = remember {
        listOfNotNull(
            ExplorePageItem(
                "personalized",
                { TabText(text = stringResource(id = R.string.title_personalized), selected = it) },
                { PersonalizedPage() }
            ),
            if (loggedIn) ExplorePageItem(
                "concern",
                { TabText(text = stringResource(id = R.string.title_concern), selected = it) },
                { ConcernPage() }
            ) else null,
            ExplorePageItem(
                "searchuser",
                { TabText(text = stringResource(id = R.string.title_search_user), selected = it) },
                { SearchUserTabPage() }
            ),
        ).toImmutableList()
    }
    val pagerState = rememberPagerState(initialPage = 0) { pages.size }
    val coroutineScope = rememberCoroutineScope()

    onGlobalEvent<GlobalEvent.Refresh>(
        filter = { it.key == "home" }
    ) {
        coroutineScope.emitGlobalEvent(GlobalEvent.Refresh(pages[pagerState.currentPage].id))
    }

    MainTabScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // 顶栏内容登记给 MainPage 的 overlay host（透明 + 滚动后薄玻璃），这里只排布局。
            MainTopBarRow(
                navigationIcon = toolboxAccountNavIconIfCompact(navigator),
                actions = {
                    ActionItem(
                        icon = PhosphorIcons["magnifying-glass"] ?: Icons.Rounded.Search,
                        contentDescription = stringResource(id = R.string.title_search)
                    ) {
                        navigator.navigate(SearchPageDestination)
                    }
                },
            ) {
                ExplorePageTab(pagerState = pagerState, pages = pages)
            }
        },
    ) { paddingValues ->
        LazyLoadHorizontalPager(
            contentPadding = paddingValues,
            state = pagerState,
            key = { pages[it].id },
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            userScrollEnabled = true,
        ) {
            pages[it].content()
        }
    }
}