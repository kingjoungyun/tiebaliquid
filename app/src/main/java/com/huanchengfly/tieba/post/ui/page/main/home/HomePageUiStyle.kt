package com.huanchengfly.tieba.post.ui.page.main.home

/** 「进吧」根页的视觉样式开关。 */
enum class HomePageUiStyle {
    GLASMIO,
    LEGACY,
}

object HomePageUi {
    /** 改这一行即可整体回滚到旧版 Toolbar + 列表行。 */
    val DefaultStyle: HomePageUiStyle = HomePageUiStyle.GLASMIO
}
