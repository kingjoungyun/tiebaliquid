package com.huanchengfly.tieba.post.ui.page.main.notifications

enum class NotificationsUiStyle {
    GLASMIO,
    LEGACY,
}

object NotificationsUi {
    /** 改这一行即可回滚到旧 Toolbar + 旧消息列表。 */
    val DefaultStyle: NotificationsUiStyle = NotificationsUiStyle.GLASMIO
}
