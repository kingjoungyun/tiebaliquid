package com.huanchengfly.tieba.post.ui.page.forum

/**
 * 吧页（`ForumPage`）的渲染风格开关（与 `PrefsUiStyle` / `UserPageUiStyle` / `PostCardUiStyle` 同构）。
 *
 * - [GLASMIO]：按「我的」页那套做 —— 无顶栏、页面底 `groupedBackground`、吧头是一张 26dp 实色
 *   `GlasmioCard`、大标题（吧名 34sp/w700）随列表收起、悬浮返回 / ⋯ 按钮、帖子列表用推特式 `PostCard`。
 * - [LEGACY]：迁移前的形态 —— 顶部 `ForumToolbar` + `infoCard` 式吧头 + 分隔线帖列表（`FeedCard`）。
 *   **旧代码全部保留**，两条分支共用同一份页面逻辑（事件 / 弹窗 / 下拉刷新 / FAB / 分页器）。
 *
 * 回滚一行：把 [ForumPageUi.DefaultStyle] 改成 [ForumPageUiStyle.LEGACY]。
 */
enum class ForumPageUiStyle {
    GLASMIO,
    LEGACY,
}

object ForumPageUi {
    /** 吧页默认风格。这是本页 A/B 切回的唯一开关。 */
    val DefaultStyle: ForumPageUiStyle = ForumPageUiStyle.GLASMIO
}
