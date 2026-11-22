package com.huanchengfly.tieba.post.ui.page.toolbox

enum class ToolboxUiStyle {
    GLASMIO,
    LEGACY,
}

object ToolboxUi {
    /** 改这一行即可回滚到旧 Toolbar。 */
    val DefaultStyle: ToolboxUiStyle = ToolboxUiStyle.GLASMIO
}
