package com.huanchengfly.tieba.post.ui.page.search

import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPageHost
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.database.SearchHistory
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPageHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.HarmonyTextProvider
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SearchUserPageDestination
import com.huanchengfly.tieba.post.ui.page.search.forum.SearchForumPage
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadPage
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadSortType
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadUiEvent
import com.huanchengfly.tieba.post.ui.page.search.user.SearchUserPage
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.ui.widgets.compose.AppScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchBox
import androidx.compose.material.TabRow
import io.github.neoruaa.glasmio.core.GlasmioTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarContainer
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassComponent
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassTabMenu
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassTabRow
import com.huanchengfly.tieba.post.ui.common.components.glass.useGlassComponent

@Immutable
data class SearchPageItem(
    val id: String,
    val text: @Composable (selected: Boolean) -> Unit,
    val content: @Composable () -> Unit,
    val supportSort: Boolean = false,
    val sortTypes: ImmutableMap<String, Int> = persistentMapOf(),
    val selectedSortType: () -> Int = { -1 },
    val onSelectedSortTypeChange: (Int) -> Unit = {},
)

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://search")
    ]
)
@Composable
fun SearchPage(
    navigator: DestinationsNavigator,
    viewModel: SearchViewModel = pageViewModel<SearchUiIntent, SearchViewModel>(
        listOf(SearchUiIntent.Init)
    ),
) {
        val context = LocalContext.current
            val searchHistories by viewModel.uiState.collectPartialAsState(
                prop1 = SearchUiState::searchHistories,
                initial = persistentListOf()
            )
            val keyword by viewModel.uiState.collectPartialAsState(
                prop1 = SearchUiState::keyword,
                initial = ""
            )
    
            val isKeywordEmpty by viewModel.uiState.collectPartialAsState(
                prop1 = SearchUiState::isKeywordEmpty,
                initial = true
            )
            val suggestions by viewModel.uiState.collectPartialAsState(
                prop1 = SearchUiState::suggestions,
                initial = persistentListOf()
            )
    
            val showSuggestions by remember {
                derivedStateOf { suggestions.isNotEmpty() }
            }
    
            var inputKeyword by remember { mutableStateOf("") }
    
            LaunchedEffect(Unit) {
                snapshotFlow { inputKeyword }
                    .debounce(500)
                    .collect {
                        viewModel.send(SearchUiIntent.KeywordInputChanged(it))
                    }
            }
    
            LaunchedEffect(keyword) {
                if (keyword.isNotEmpty() && keyword != inputKeyword) {
                    inputKeyword = keyword
                }
            }
    
            MyBackHandler(
                enabled = !isKeywordEmpty,
                currentScreen = SearchPageDestination
            ) {
                viewModel.send(SearchUiIntent.SubmitKeyword(""))
            }
    
            var expanded by remember { mutableStateOf(false) }
    
            val initialSortType = remember { SearchThreadSortType.SORT_TYPE_NEWEST }
            var searchThreadSortType by remember { mutableIntStateOf(initialSortType) }
            LaunchedEffect(searchThreadSortType) {
                emitGlobalEvent(SearchThreadUiEvent.SwitchSortType(searchThreadSortType))
            }
            viewModel.onEvent<SearchUiEvent.KeywordChanged> {
                inputKeyword = it.keyword
                if (it.keyword.isNotBlank()) emitGlobalEventSuspend(it)
            }
    
            val pages by remember {
                derivedStateOf {
                    persistentListOf(
                        SearchPageItem(
                            id = "forum",
                            text = {
                                Text(
                                    text = stringResource(id = R.string.title_search_forum),
                                    fontWeight = if (it) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            content = { SearchForumPage(keyword = keyword) }
                        ),
                        SearchPageItem(
                            id = "thread",
                            text = {
                                Text(
                                    text = stringResource(id = R.string.title_search_thread),
                                    fontWeight = if (it) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            content = {
                                SearchThreadPage(
                                    keyword = keyword,
                                    initialSortType = initialSortType
                                )
                            },
                            supportSort = true,
                            sortTypes = persistentMapOf(
                                context.getString(R.string.title_search_order_new) to SearchThreadSortType.SORT_TYPE_NEWEST,
                                context.getString(R.string.title_search_order_old) to SearchThreadSortType.SORT_TYPE_OLDEST,
                                context.getString(R.string.title_search_order_relevant) to SearchThreadSortType.SORT_TYPE_RELATIVE,
                            ),
                            selectedSortType = { searchThreadSortType },
                            onSelectedSortTypeChange = { searchThreadSortType = it }
                        ),
                        SearchPageItem(
                            id = "user",
                            text = {
                                Text(
                                    text = stringResource(id = R.string.title_search_user),
                                    fontWeight = if (it) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            content = { SearchUserPage(keyword = keyword) }
                        ),
                    )
                }
            }
            val pagerState = rememberPagerState { pages.size }
            val glassmio = SearchPageUi.DefaultStyle == SearchPageUiStyle.GLASMIO
            LiquidGlassTheme(accent = AppTokens.primary) {
                HarmonyTextProvider {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (glassmio) GlasmioTheme.colors.groupedBackground
                        else AppTokens.background
                    ),
            ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlasmioTheme.colors.groupedBackground)
                            .statusBarsPadding(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GlassIconButton(
                                onClick = { navigator.navigateUp() },
                                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(id = R.string.button_back),
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.title_search),
                            style = LocalTextStyle.current.copy(
                                fontSize = 34.sp,
                                lineHeight = 40.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = AppTokens.text,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .padding(horizontal = 16.dp)
                        ) {
                            SearchTopBar(
                                keyword = inputKeyword,
                                onKeywordChange = { inputKeyword = it },
                                onKeywordSubmit = {
                                    viewModel.send(SearchUiIntent.SubmitKeyword(it))
                                },
                                glassmio = glassmio,
                            )
                        }
                        if (!isKeywordEmpty) {
                            SearchTabRow(pagerState = pagerState, pages = pages)
                        }
                    }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (!isKeywordEmpty) {
                        ProvideNavigator(navigator = navigator) {
                            LazyLoadHorizontalPager(
                                state = pagerState,
                                key = { pages[it].id },
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                pages[it].content()
                            }
                        }
                    } else {
                        if (showSuggestions) {
                            SearchSuggestionList(
                                suggestions = suggestions,
                                onItemClick = {
                                    inputKeyword = it
                                    viewModel.send(SearchUiIntent.SubmitKeyword(it))
                                }
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Container {
                                    Column {
                                    SearchUserEntry(
                                        onClick = {
                                            navigator.navigate(SearchUserPageDestination())
                                        }
                                    )
                                    SearchHistoryList(
                                        searchHistories = searchHistories,
                                        onSearchHistoryClick = {
                                            inputKeyword = it.content
                                            viewModel.send(SearchUiIntent.SubmitKeyword(it.content))
                                        },
                                        expanded = expanded,
                                        onToggleExpand = { expanded = !expanded },
                                        onDelete = { viewModel.send(SearchUiIntent.DeleteSearchHistory(it.id)) },
                                        onClear = { viewModel.send(SearchUiIntent.ClearSearchHistory) },
                                        glassmio = glassmio,
                                    )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchSuggestionList(
    suggestions: ImmutableList<String>,
    onItemClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        items(
            items = suggestions,
            key = { it }
        ) {
            Container(
                modifier = Modifier.animateItem()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .debounceClickable(onClick =  {
                            onItemClick(it)
                        })
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons["magnifying-glass"] ?: Icons.Rounded.Search,
                        contentDescription = stringResource(id = R.string.desc_search_sug, it),
                        tint = AppTokens.text
                    )

                    Text(
                        text = it,
                        style = MaterialTheme.typography.subtitle2,
                        modifier = Modifier.weight(1f),
                        color = AppTokens.text
                    )
                }
            }
        }
    }
}

@Preview("SearchSuggestionList", backgroundColor = 0xFFFFFFFF)
@Composable
private fun SearchSuggestionListPreview() {
    TiebaLiteTheme {
        Box(
            modifier = Modifier.background(AppTokens.topBar)
        ) {
            SearchSuggestionList(
                suggestions = persistentListOf("1", "2", "3"),
                onItemClick = {}
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnScope.SearchTabRow(
    pagerState: PagerState,
    pages: ImmutableList<SearchPageItem>,
) {
    val coroutineScope = rememberCoroutineScope()
    // T3：TabRow 组件级回滚开关（false → 下面的 M2 官方 TabRow）
    val useGlassTabRow = useGlassComponent(GlassComponent.TabRow)
    if (useGlassTabRow) {
        GlassTabRow(
            selectedTabIndex = pagerState.currentPage,
            onTabSelected = { index ->
                coroutineScope.launch { pagerState.animateScrollToPage(index) }
            },
            tabCount = pages.size,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            // 「点已选中的 tab → 排序菜单」，与原 TabClickMenu 语义一致
            tabMenu = GlassTabMenu(hasMenu = { pages[it].supportSort }) { index ->
                val item = pages[index]
                ListSinglePicker(
                    itemTitles = item.sortTypes.keys.toImmutableList(),
                    itemValues = item.sortTypes.values.toImmutableList(),
                    selectedPosition = item.sortTypes.values.indexOf(item.selectedSortType()),
                    onItemSelected = { _, _, value, changed ->
                        if (changed) item.onSelectedSortTypeChange(value)
                        dismiss()
                    }
                )
            },
        ) { index, selected ->
            // 保留页面自己的 tab 文案（含选中态字重），只统一颜色
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material.LocalContentColor provides
                    if (selected) AppTokens.primary else AppTokens.textSecondary
            ) {
                pages[index].text(selected)
            }
        }
    } else {
        // 回滚：M2 官方 TabRow（原来的 M2 分叉实现已随 Tabs.kt 删除）
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            divider = {},
            backgroundColor = Color.Transparent,
            contentColor = AppTokens.onTopBar,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(76.dp * pages.size),
        ) {
            pages.fastForEachIndexed { index, item ->
                val tabTextStyle = MaterialTheme.typography.button.copy(
                    fontSize = 15.sp,
                    letterSpacing = 0.sp,
                    fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold
                    else FontWeight.Normal,
                )
                Tab(
                    text = {
                        ProvideTextStyle(value = tabTextStyle) {
                            item.text(pagerState.currentPage == index)
                        }
                    },
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun SearchHistoryList(
    searchHistories: ImmutableList<SearchHistory>,
    onSearchHistoryClick: (SearchHistory) -> Unit,
    expanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    onDelete: (SearchHistory) -> Unit = {},
    onClear: () -> Unit = {},
    glassmio: Boolean = false,
) {
    val hasItem = remember(searchHistories) {
        searchHistories.isNotEmpty()
    }
    val hasMore = remember(searchHistories) {
        searchHistories.size > 6
    }
    val showItem = remember(expanded, hasMore, searchHistories) {
        if (!expanded && hasMore) searchHistories.take(6) else searchHistories
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.title_search_history),
                modifier = Modifier
                    .weight(1f),
                style = MaterialTheme.typography.subtitle1
            )
            if (hasItem) {
                Text(
                    text = stringResource(id = R.string.button_clear_all),
                    modifier = Modifier.debounceClickable(onClick = onClear),
                    style = MaterialTheme.typography.button
                )
            }
        }
        FlowRow(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            showItem.fastForEach { searchHistory ->
                Box(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(100))
                        .combinedClickable(
                            onClick = { onSearchHistoryClick(searchHistory) },
                            onLongClick = { onDelete(searchHistory) }
                        )
                        .background(
                            if (glassmio) GlasmioTheme.colors.elevatedBackground
                            else AppTokens.chip
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = searchHistory.content
                    )
                }
            }
        }
        if (hasMore) {
            Button(
                onClick = onToggleExpand,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = AppTokens.text
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) PhosphorIcons["caret-up"] ?: Icons.Rounded.ExpandLess else PhosphorIcons["caret-down"] ?: Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(
                            id = if (expanded) R.string.button_expand_less_history else R.string.button_expand_more_history
                        ),
                        style = MaterialTheme.typography.button,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (!hasItem) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.tip_empty),
                    color = AppTokens.textDisabled,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onKeywordSubmit: (String) -> Unit = {},
    onBack: () -> Unit = {},
    glassmio: Boolean = false,
) {
    SearchBox(
        keyword = keyword,
        onKeywordChange = onKeywordChange,
        modifier = Modifier.fillMaxSize(),
        onKeywordSubmit = onKeywordSubmit,
        glassmio = false,
        showSubmitIcon = true,
        color = if (glassmio) GlasmioTheme.colors.elevatedBackground
        else AppTokens.topBarSurface,
        placeholder = {
            Text(
                text = stringResource(id = R.string.hint_search),
                color = AppTokens.onTopBarSurface.copy(alpha = ContentAlpha.medium)
            )
        },
        prependIcon = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .debounceClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (glassmio) PhosphorIcons["magnifying-glass"] ?: Icons.Rounded.Search
                    else Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = if (glassmio) null
                    else stringResource(id = R.string.button_back),
                    modifier = Modifier.size(if (glassmio) 18.dp else 24.dp),
                )
            }
        },
        shape = RoundedCornerShape(if (glassmio) 22.dp else 6.dp)
    )
}

@Preview("SearchBox")
@Composable
fun PreviewSearchBox() {
    var keyword by remember { mutableStateOf("") }
    TiebaLiteTheme {
        Box(
            modifier = Modifier
                .height(64.dp)
                .background(AppTokens.topBar)
                .padding(vertical = 8.dp, horizontal = 16.dp)
        ) {
            SearchTopBar(
                keyword = keyword,
                onKeywordChange = { keyword = it }
            )
        }
    }
}

@Composable
private fun SearchUserEntry(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GlasmioTheme.colors.elevatedBackground)
            .debounceClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = PhosphorIcons["user-list"] ?: Icons.Outlined.PersonSearch,
            contentDescription = null,
            tint = AppTokens.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.title_search_user),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTokens.text,
            )
            Text(
                text = "输入 UID 查用户",
                fontSize = 13.sp,
                color = AppTokens.textSecondary,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = AppTokens.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview("SearchHistoryList")
@Composable
fun PreviewSearchHistoryList() {
    TiebaLiteTheme {
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.background(AppTokens.background)) {
            SearchHistoryList(
                searchHistories = (0..20).map {
                    SearchHistory(content = if (it % 2 == 0) "记录$it" else "搜索记录$it")
                }.toImmutableList(),
                onSearchHistoryClick = {},
                expanded = expanded,
                onToggleExpand = { expanded = !expanded },
            )
        }
    }
}
