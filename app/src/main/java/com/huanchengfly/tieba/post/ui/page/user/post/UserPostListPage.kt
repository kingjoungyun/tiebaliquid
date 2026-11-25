package com.huanchengfly.tieba.post.ui.page.user.post

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassLargeTitle
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPage
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/**
 * 独立的「我的发帖 / 我的回帖」列表页。
 *
 * 内容复用 [UserPostPage]，`isThread = true` 为主题帖，`false` 为回帖。
 *
 * T5：骨架从 `MyScaffold + TitleCentredToolbar` 换成 [GlassPage]（折叠大标题 + 悬浮返回按钮），
 * 列表本身没动 —— 里面的卡片已经是 `PostCard`（T3.5 统一过）。
 */
@Destination
@Composable
fun UserPostListPage(
    uid: Long = 0,
    isThread: Boolean = true,
    authorName: String = "",
    authorNameShow: String = "",
    authorPortrait: String = "",
    navigator: DestinationsNavigator,
) {
    ProvideNavigator(navigator = navigator) {
        GlassPage(
            title = stringResource(
                id = if (isThread) R.string.title_toolbox_threads else R.string.title_toolbox_posts
            ),
            onBack = { navigator.navigateUp() },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                GlassLargeTitle(
                    title = stringResource(
                        id = if (isThread) R.string.title_toolbox_threads else R.string.title_toolbox_posts
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    UserPostPage(
                    uid = uid,
                    isThread = isThread,
                    enablePullRefresh = true,
                    authorName = authorName,
                    authorNameShow = authorNameShow,
                        authorPortrait = authorPortrait
                    )
                }
            }
        }
    }
}
