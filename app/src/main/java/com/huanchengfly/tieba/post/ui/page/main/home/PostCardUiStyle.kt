package com.huanchengfly.tieba.post.ui.page.main.home

/**
 * 首页信息流的卡片实现（与 `PrefsUiStyle` / `UserPageUiStyle` 同构的一页式开关）。
 *
 * - [TWITTER]：推特式帖子卡（`PostCard.kt`，GlasmioCard 26dp 实色壳 + 头像左列版式）。
 * - [LEGACY]：迁移前的 `FeedCard`（`ui/widgets/compose/FeedCard.kt`，**代码原样保留**，
 *   吧页 / 收藏 / 历史 / 关注等其它页面仍然只用它）。
 *
 * 回滚一行：把 [PostCardUi.DefaultStyle] 改成 [PostCardUiStyle.LEGACY]，
 * `PersonalizedPage` 里列表、间距、分隔线、页面底色都会一起退回旧版，不用改别的文件。
 */
enum class PostCardUiStyle {
    TWITTER,
    LEGACY,
}

object PostCardUi {
    /**
     * 首页信息流默认使用的卡片风格。
     *
     * 这是本页 A/B 切回的唯一开关。
     */
    val DefaultStyle: PostCardUiStyle = PostCardUiStyle.TWITTER
}
