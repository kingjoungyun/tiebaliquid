package com.huanchengfly.tieba.post.ui.page.user

enum class UserProfileUiStyle {
    GLASMIO,
    LEGACY,
}

object UserProfileUi {
    /** 改这一行即可回滚到旧 Toolbar + 旧资料头。 */
    val DefaultStyle: UserProfileUiStyle = UserProfileUiStyle.GLASMIO
}
