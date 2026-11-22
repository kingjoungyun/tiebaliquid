package com.huanchengfly.tieba.post.ui.page.hottopic.list

enum class HotTopicListUiStyle {
    GLASMIO,
    LEGACY,
}

object HotTopicListUi {
    /** 改这一行即可回滚到旧 Toolbar + 图片列表。 */
    val DefaultStyle: HotTopicListUiStyle = HotTopicListUiStyle.GLASMIO
}
