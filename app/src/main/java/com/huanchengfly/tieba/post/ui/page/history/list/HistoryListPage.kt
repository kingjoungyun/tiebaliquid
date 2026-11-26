package com.huanchengfly.tieba.post.ui.page.history.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPageHost
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostsGlassTopBar
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageSkin
import com.huanchengfly.tieba.post.ui.page.thread.ThreadTopBarHeight
import io.github.neoruaa.glasmio.core.GlasmioTheme
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.models.ThreadHistoryInfoBean
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFrom
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionSheetItem
import com.huanchengfly.tieba.post.ui.widgets.compose.AppActionSheet
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import androidx.compose.material.SnackbarHostState
import com.huanchengfly.tieba.post.ui.common.components.glass.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryListPage(
    type: Int,
    viewModel: HistoryListViewModel = if (type == HistoryUtil.TYPE_THREAD) pageViewModel<ThreadHistoryListViewModel>() else pageViewModel<ForumHistoryListViewModel>()
) {
        val glassmio = HistoryListUi.DefaultStyle == HistoryListUiStyle.GLASMIO
        val navigator = LocalNavigator.current
        val lazyListState = rememberLazyListState()
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val topInset = statusBarTop + ThreadTopBarHeight
        val isCollapsed by remember {
            derivedStateOf {
                lazyListState.firstVisibleItemIndex > 0 ||
                    lazyListState.firstVisibleItemScrollOffset > 40
            }
        }
        val topScrimAlpha by animateFloatAsState(
            targetValue = if (isCollapsed) 1f else 0f,
            animationSpec = tween(180),
            label = "historyTopScrim",
        )
        GlassPageHost(modifier = Modifier.fillMaxSize()) {
            LazyLoad(loaded = viewModel.initialized) {
                viewModel.send(HistoryListUiIntent.Refresh)
                viewModel.initialized = true
            }
            onGlobalEvent<HistoryListUiEvent.DeleteAll> {
                viewModel.send(HistoryListUiIntent.DeleteAll)
            }
            val isLoadingMore by viewModel.uiState.collectPartialAsState(
                prop1 = HistoryListUiState::isLoadingMore,
                initial = false
            )
            val hasMore by viewModel.uiState.collectPartialAsState(
                prop1 = HistoryListUiState::hasMore,
                initial = true
            )
            val currentPage by viewModel.uiState.collectPartialAsState(
                prop1 = HistoryListUiState::currentPage,
                initial = 0
            )
            val todayHistoryData by viewModel.uiState.collectPartialAsState(
                prop1 = HistoryListUiState::todayHistoryData,
                initial = emptyList()
            )
            val beforeHistoryData by viewModel.uiState.collectPartialAsState(
                prop1 = HistoryListUiState::beforeHistoryData,
                initial = emptyList()
            )
    
            val context = LocalContext.current
            val snackbarHostState = LocalSnackbarHostState.current ?: remember { SnackbarHostState() }
            viewModel.onEvent<HistoryListUiEvent.Delete.Failure> {
                snackbarHostState.showSnackbar(
                    context.getString(
                        R.string.delete_history_failure,
                        it.errorMsg
                    )
                )
            }
            viewModel.onEvent<HistoryListUiEvent.Delete.Success> {
                snackbarHostState.showSnackbar(context.getString(R.string.delete_history_success))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                LoadMoreLayout(
                    isLoading = isLoadingMore,
                    onLoadMore = { viewModel.send(HistoryListUiIntent.LoadMore(currentPage + 1)) },
                    loadEnd = !hasMore,
                    lazyListState = lazyListState,
                    isEmpty = todayHistoryData.isEmpty() && beforeHistoryData.isEmpty()
                ) {
                    MyLazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        state = lazyListState,
                        contentPadding = PaddingValues(
                            top = 0.dp,
                        ),
                    ) {
                        if (todayHistoryData.isNotEmpty()) {
                            stickyHeader(key = "TodayHistoryHeader") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (glassmio) GlasmioTheme.colors.groupedBackground
                                            else AppTokens.background
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Chip(
                                        text = stringResource(id = R.string.title_history_today),
                                        invertColor = true
                                    )
                                }
                            }
                            items(
                                items = todayHistoryData,
                                key = { it.id }
                            ) { info ->
                                HistoryItem(
                                    info,
                                    glassmio = glassmio,
                                    onDelete = {
                                        viewModel.send(HistoryListUiIntent.Delete(it.id))
                                    },
                                    onClick = {
                                        when (it.type) {
                                            HistoryUtil.TYPE_FORUM -> {
                                                navigator.navigate(ForumPageDestination(it.data))
                                            }
    
                                            HistoryUtil.TYPE_THREAD -> {
                                                val extra =
                                                    if (it.extras != null) it.extras.fromJson<ThreadHistoryInfoBean>() else null
                                                navigator.navigate(
                                                    ThreadPageDestination(
                                                        it.data.toLong(),
                                                        postId = extra?.pid?.toLongOrNull() ?: 0L,
                                                        seeLz = extra?.isSeeLz ?: false,
                                                        from = ThreadPageFrom.FROM_HISTORY
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        if (beforeHistoryData.isNotEmpty()) {
                            stickyHeader(key = "BeforeHistoryHeader") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (glassmio) GlasmioTheme.colors.groupedBackground
                                            else AppTokens.background
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Chip(text = stringResource(id = R.string.title_history_before))
                                }
                            }
                            items(
                                items = beforeHistoryData,
                                key = { it.id }
                            ) { info ->
                                HistoryItem(
                                    info,
                                    glassmio = glassmio,
                                    onDelete = {
                                        viewModel.send(HistoryListUiIntent.Delete(it.id))
                                    },
                                    onClick = {
                                        when (it.type) {
                                            HistoryUtil.TYPE_FORUM -> {
                                                navigator.navigate(ForumPageDestination(it.data))
                                            }
    
                                            HistoryUtil.TYPE_THREAD -> {
                                                val extra =
                                                    if (it.extras != null) it.extras.fromJson<ThreadHistoryInfoBean>() else null
                                                navigator.navigate(
                                                    ThreadPageDestination(
                                                        it.data.toLong(),
                                                        postId = extra?.pid?.toLongOrNull() ?: 0L,
                                                        seeLz = extra?.isSeeLz ?: false,
                                                        from = ThreadPageFrom.FROM_HISTORY
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryItem(
    info: History,
    glassmio: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (History) -> Unit = {},
    onDelete: (History) -> Unit = {},
) {
    // P2/B：长按不再弹下拉菜单，改用 iOS 动作面板（底部升起 + 拖拽关闭 + 玻璃）。
    // 这是全 App 第一个接入 AppActionSheet 的真实流程；下拉菜单（AppMenu）保持不动，两者并存。
    var showActions by remember { mutableStateOf(false) }
    val itemTitle = (if (info.type == HistoryUtil.TYPE_THREAD) info.username else info.title) ?: ""
    AppActionSheet(
        visible = showActions,
        onDismiss = { showActions = false },
        title = itemTitle,
        message = stringResource(id = R.string.title_history),
        items = listOf(
            ActionSheetItem(
                label = stringResource(id = R.string.title_delete),
                systemName = "trash",
                destructive = true,
                onClick = { onDelete(info) },
            ),
        ),
    )
    if (glassmio) {
        val timeText = DateTimeUtils.getRelativeTimeString(LocalContext.current, info.timestamp)
        val primaryText = info.title ?: info.username.orEmpty()
        val secondaryText = if (info.type == HistoryUtil.TYPE_THREAD) {
            listOfNotNull(info.username, timeText).joinToString(" · ")
        } else {
            timeText
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick(info) },
                    onLongClick = { showActions = true },
                )
                .padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                data = info.avatar,
                size = 40.dp,
                shape = RoundedCornerShape(10.dp),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = primaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTokens.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = secondaryText,
                    fontSize = 13.sp,
                    color = AppTokens.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        return
    }
    Column(
        modifier = modifier
            .combinedClickable(
                onClick = { onClick(info) },
                onLongClick = { showActions = true },
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
            UserHeader(
                avatar = {
                    Avatar(
                        data = info.avatar,
                        size = Sizes.Small,
                        contentDescription = null
                    )
                },
                name = {
                    Text(
                        text = (if (info.type == HistoryUtil.TYPE_THREAD) info.username else info.title)
                            ?: ""
                    )
                },
            ) {
                Text(
                    text = DateTimeUtils.getRelativeTimeString(
                        LocalContext.current,
                        info.timestamp
                    ),
                    fontSize = 15.sp,
                    color = AppTokens.text,
                )
            }
            if (info.type == HistoryUtil.TYPE_THREAD) {
                Text(
                    text = info.title,
                    fontSize = 15.sp,
                    color = AppTokens.text,
                )
            }
        }
}
