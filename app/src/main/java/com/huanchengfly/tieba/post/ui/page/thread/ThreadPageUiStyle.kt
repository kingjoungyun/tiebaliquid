package com.huanchengfly.tieba.post.ui.page.thread

/**
 * 帖子详情页的渲染风格开关（与 `PostCardUiStyle` / `UserPageUiStyle` / `ForumPageUiStyle` 同构）。
 *
 * - [GLASMIO]：只换「外壳」——页级玻璃 host、无顶栏 + 大标题 + 悬浮返回、玻璃底部操作栏、
 *   玻璃菜单。楼层 / 图片 / 视频 / 点赞 / 楼中楼 / 下拉刷新 / 数据流全部沿用原实现。
 * - [LEGACY]：迁移前的 Material 外观（旧顶栏、旧底部栏、旧更多菜单），**代码全部保留**。
 *
 * 回滚一行：把 [ThreadPageUi.DefaultStyle] 改成 [ThreadPageUiStyle.LEGACY]。
 */
enum class ThreadPageUiStyle {
    GLASMIO,
    LEGACY,
}

object ThreadPageUi {
    /** 帖子详情页默认风格。这是本页 A/B 切回的唯一开关。 */
    val DefaultStyle: ThreadPageUiStyle = ThreadPageUiStyle.GLASMIO
}
