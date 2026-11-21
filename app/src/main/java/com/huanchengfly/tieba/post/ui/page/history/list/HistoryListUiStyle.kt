package com.huanchengfly.tieba.post.ui.page.history.list

enum class HistoryListUiStyle {
    GLASMIO,
    LEGACY,
}

object HistoryListUi {
    /** 改这一行即可回滚到旧浏览记录列表。 */
    val DefaultStyle: HistoryListUiStyle = HistoryListUiStyle.GLASMIO
}
