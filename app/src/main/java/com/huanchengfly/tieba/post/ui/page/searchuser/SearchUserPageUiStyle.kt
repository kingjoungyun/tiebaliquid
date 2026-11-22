package com.huanchengfly.tieba.post.ui.page.searchuser

enum class SearchUserPageUiStyle {
    GLASMIO,
    LEGACY,
}

object SearchUserPageUi {
    /** 改这一行即可回滚到旧查人页 Toolbar。 */
    val DefaultStyle: SearchUserPageUiStyle = SearchUserPageUiStyle.GLASMIO
}
