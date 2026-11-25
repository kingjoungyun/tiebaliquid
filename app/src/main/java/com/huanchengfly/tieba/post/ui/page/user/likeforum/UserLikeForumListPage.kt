package com.huanchengfly.tieba.post.ui.page.user.likeforum

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
 * 独立的「关注的吧」列表页。
 *
 * 内容复用 [UserLikeForumPage]，该组件内部通过 `LocalNavigator.current`
 * 获取导航器，因此这里必须用 [ProvideNavigator] 包裹，否则会抛
 * 「No navigator is available」崩溃。
 *
 * T5：骨架换成 [GlassPage]（折叠大标题 + 悬浮返回）；行样式同时换成
 * 「玻璃卡片 + 吧头像 + 吧名/简介 + 右侧 chevron」的偏好行形态。
 */
@Destination
@Composable
fun UserLikeForumListPage(
    uid: Long = 0,
    navigator: DestinationsNavigator,
) {
    ProvideNavigator(navigator = navigator) {
        GlassPage(
            title = stringResource(id = R.string.title_toolbox_like_forum),
            onBack = { navigator.navigateUp() },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                GlassLargeTitle(
                    title = stringResource(id = R.string.title_toolbox_like_forum),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    UserLikeForumPage(
                        uid = uid,
                        enablePullRefresh = true
                    )
                }
            }
        }
    }
}
