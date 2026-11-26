package com.huanchengfly.tieba.post.ui.page.main.user

/**
 * 「我的」页的渲染层实现（一页式开关）。
 *
 * - [GLASSMIO]：账户头卡片 + Glasmio 分组列表（当前默认值，见 [UserPageUi.DefaultStyle]）。
 * - [LEGACY]：迁移前的 Material 版（`InfoCard` + `StatCard` + `ListMenuItem`），
 *   **代码原样保留**在 `UserPage.kt` 的 `LegacyUserPage` 里。
 *
 * 回滚只需要把 [UserPageUi.DefaultStyle] 改成 [LEGACY]，`MainPage` 一行都不用动。
 */
enum class UserPageUiStyle {
    GLASSMIO,
    LEGACY,
}

object UserPageUi {
    /**
     * 「我的」页默认渲染风格。
     *
     * 这是本页 A/B 切回的唯一开关：改成 [UserPageUiStyle.LEGACY] 就回到迁移前的版式。
     */
    val DefaultStyle: UserPageUiStyle = UserPageUiStyle.GLASSMIO
}
