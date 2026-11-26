package com.huanchengfly.tieba.post.ui.page.main.user

import androidx.compose.runtime.Composable
import com.huanchengfly.tieba.post.arch.pageViewModel

/**
 * 「我的」页入口。
 *
 * 现在只有一套实现：玻璃版（`UserPageGlasmio.kt`，账户头卡片 + 分组列表）。
 * 迁移前的 Material 版式已随同款 UI 开关一并删除。
 */
@Composable
fun UserPage(
    viewModel: UserViewModel = pageViewModel<UserUiIntent, UserViewModel>(
        listOf(
            UserUiIntent.Refresh
        )
    )
) {
    GlasmioUserPage(viewModel = viewModel)
}
