package com.huanchengfly.tieba.post.ui.page.analysis

enum class AnalysisUiStyle {
    GLASMIO,
    LEGACY,
}

object AnalysisUi {
    /** 改这一行即可回滚到旧 Toolbar。 */
    val DefaultStyle: AnalysisUiStyle = AnalysisUiStyle.GLASMIO
}
