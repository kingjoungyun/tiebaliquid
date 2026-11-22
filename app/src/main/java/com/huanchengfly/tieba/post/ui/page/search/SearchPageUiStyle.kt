package com.huanchengfly.tieba.post.ui.page.search

enum class SearchPageUiStyle {
    GLASMIO,
    LEGACY,
}

object SearchPageUi {
    /** 改这一行即可回滚到旧搜索页 Toolbar。 */
    val DefaultStyle: SearchPageUiStyle = SearchPageUiStyle.GLASMIO
}
