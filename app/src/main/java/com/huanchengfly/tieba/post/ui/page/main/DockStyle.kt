package com.huanchengfly.tieba.post.ui.page.main

/**
 * 底部 dock 的实现选型（转正试验的开关）。
 */
enum class DockStyle {
    /**
     * Glasmio 原生 `GlasmioNavigationBar`：
     * 镜头（lens）滑动 + 拉伸、按压形变、胶囊 ↔ 圆球收缩、与相邻控件的接触融合。
     */
    GLASMIO,

    /**
     * 旧的自绘浮动胶囊（`NavigationComponents.BottomNavigation()`）：
     * Glasmio 的 `LiquidGlassSurface` 材质 + 自绘 `Row` 四等分布局，没有任何动效。
     *
     * 保留为回滚路径：`DockStyle.LEGACY` 与转正前的行为逐像素一致。
     */
    LEGACY,
}

/**
 * 当前生效的 dock 实现。
 *
 * 转正后默认 [DockStyle.GLASMIO]；要整体回滚只需把这里改成 [DockStyle.LEGACY]，
 * `MainPage` 的调用处不用动（两条分支共用同一个 dock 槽位与同一份参数）。
 */
val DefaultDockStyle: DockStyle = DockStyle.GLASMIO
