package com.huanchengfly.tieba.post.ui.page.user.likeforum

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.api.models.UserLikeForumBean
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.getOrNull
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassComponentSurface
import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.kyant.capsule.ContinuousRoundedRectangle

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UserLikeForumPage(
    uid: Long,
    fluid: Boolean = false,
    enablePullRefresh: Boolean = false,
    viewModel: UserLikeForumViewModel = pageViewModel(),
) {
    val navigator = LocalNavigator.current

    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(UserLikeForumUiIntent.Refresh(uid))
        viewModel.initialized = true
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::isRefreshing,
        initial = true
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::isLoadingMore,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::error,
        initial = null
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::currentPage,
        initial = 1
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::hasMore,
        initial = false
    )
    val forums by viewModel.uiState.collectPartialAsState(
        prop1 = UserLikeForumUiState::forums,
        initial = persistentListOf()
    )

    val isEmpty by remember {
        derivedStateOf { forums.isEmpty() }
    }
    val isError by remember {
        derivedStateOf { error != null }
    }

    onGlobalEvent<GlobalEvent.Refresh>(
        filter = { it.key == "user_profile" }
    ) {
        viewModel.send(UserLikeForumUiIntent.Refresh(uid))
    }

    StateScreen(
        isEmpty = isEmpty,
        isError = isError,
        isLoading = isRefreshing,
        onReload = {
            viewModel.send(UserLikeForumUiIntent.Refresh(uid))
        },
        errorScreen = { ErrorScreen(error = error.getOrNull()) },
    ) {
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = ::reload
        )

        val lazyListState = rememberLazyListState()

        val pullRefreshModifier =
            if (enablePullRefresh) Modifier.pullRefresh(pullRefreshState) else Modifier

        Box(modifier = pullRefreshModifier) {
            LoadMoreLayout(
                isLoading = isLoadingMore,
                onLoadMore = {
                    viewModel.send(UserLikeForumUiIntent.LoadMore(uid, currentPage))
                },
                loadEnd = !hasMore,
                lazyListState = lazyListState
            ) {
                UserLikeForumList(
                    data = forums,
                    fluid = fluid,
                    onClickForum = { forumBean ->
                        forumBean.name?.let {
                            navigator.navigate(ForumPageDestination(it))
                        }
                    },
                    lazyListState = lazyListState
                )
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = AppTokens.pullRefreshIndicator,
                contentColor = AppTokens.primary,
            )
        }
    }
}

@Composable
private fun UserLikeForumList(
    data: ImmutableList<UserLikeForumBean.ForumBean>,
    onClickForum: (UserLikeForumBean.ForumBean) -> Unit,
    fluid: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    // T5：行样式换成「玻璃卡片 + 头像 + 吧名/简介 + chevron」，页边与卡距按偏好列表的规格。
    MyLazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = data,
            key = { it.id }
        ) {
            UserLikeForumItem(
                item = it,
                onClick = {
                    onClickForum(it)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 关注的吧行（T5 重排）：玻璃卡片 + 吧头像 + 吧名/简介 + 右侧 chevron。
 *
 * 与设置页的偏好行同构（同卡片圆角、同内边距、同 chevron 图标），
 * 这样「关注吧列表」和「设置列表」看起来是一套东西。
 */
@Composable
private fun UserLikeForumItem(
    item: UserLikeForumBean.ForumBean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = ContinuousRoundedRectangle(16.dp)
    GlassComponentSurface(
        modifier = modifier.clip(shape),
        shape = shape,
        tint = AppTokens.card,
        hostFill = AppTokens.groupedBackground,
        innerGlow = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .debounceClickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Avatar(
                data = item.avatar,
                size = 40.dp,
                contentDescription = null
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.name.orEmpty(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTokens.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.slogan.takeUnless { it.isNullOrEmpty() }?.let {
                    Text(
                        text = it,
                        fontSize = 13.sp,
                        color = AppTokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = PhosphorIcons["caret-right"] ?: Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = AppTokens.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}