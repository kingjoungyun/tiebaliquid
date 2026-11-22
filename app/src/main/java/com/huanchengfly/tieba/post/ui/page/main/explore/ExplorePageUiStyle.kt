package com.huanchengfly.tieba.post.ui.page.main.explore

enum class ExplorePageUiStyle {
    GLASMIO,
    LEGACY,
}

object ExplorePageUi {
    /** 改这一行即可回滚到旧头像 + TabRow + 圆形搜索钮。 */
    val DefaultStyle: ExplorePageUiStyle = ExplorePageUiStyle.GLASMIO
}
