package com.huanchengfly.tieba.post.ui.common.theme.glass

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * **AppTokens —— 全 App 唯一的颜色令牌（运行时填充）。**
 *
 * 定位（Stage 6）：XML 主题 继续负责「从 XML 主题 + 6 色 accent + AMOLED 解析出颜色」
 * （它是 attr 机制的产物，不能删），解析完成后**把同一批值灌进这里**；页面统一读
 * `AppTokens.xxx`。两套并存期间值严格一致（同一个 XML 主题 实例填出来）。
 *
 * 为什么字段是 `mutableStateOf` 而不是常量：主题（亮/暗）、accent（6 色）、AMOLED 都会
 * 在运行中切换。用 snapshot state 才能让读 `AppTokens.xxx` 的 Composable 自动重组；
 * 写死成常量会让换主题/换色失效。
 *
 * 字段与 XML 主题 1:1（命名保持一致，便于机械替换）；另外提供
 * [textPrimary] / [textTertiary] / [separator] 三个别名，对应设计规范里的叫法
 * （分别是 text / textDisabled / divider）。
 *
 * [groupedBackground] 是唯一一个**不来自** XML attr 的 token：它是设计规范里的
 * iOS grouped background（浅色 `#F2F2F7` / 深色 `#000000`），在 [fill] 里按明暗计算。
 */
object AppTokens {

    // ── 主题元信息 ──
    var theme by mutableStateOf("tieba")
        private set
    var isNightMode by mutableStateOf(false)
        private set

    // ── 主色 ──
    var primary by mutableStateOf(Color.Unspecified)
        private set
    var onPrimary by mutableStateOf(Color.Unspecified)
        private set
    var accent by mutableStateOf(Color.Unspecified)
        private set
    var onAccent by mutableStateOf(Color.Unspecified)
        private set

    // ── 顶栏 ──
    var topBar by mutableStateOf(Color.Unspecified)
        private set
    var onTopBar by mutableStateOf(Color.Unspecified)
        private set
    var onTopBarSecondary by mutableStateOf(Color.Unspecified)
        private set
    var onTopBarActive by mutableStateOf(Color.Unspecified)
        private set
    var topBarSurface by mutableStateOf(Color.Unspecified)
        private set
    var onTopBarSurface by mutableStateOf(Color.Unspecified)
        private set

    // ── 底栏 ──
    var bottomBar by mutableStateOf(Color.Unspecified)
        private set
    var bottomBarSurface by mutableStateOf(Color.Unspecified)
        private set
    var onBottomBarSurface by mutableStateOf(Color.Unspecified)
        private set

    // ── 文字 ──
    var text by mutableStateOf(Color.Unspecified)
        private set
    var textSecondary by mutableStateOf(Color.Unspecified)
        private set
    var textDisabled by mutableStateOf(Color.Unspecified)
        private set

    // ── 表面 ──
    var background by mutableStateOf(Color.Unspecified)
        private set

    /**
     * 分组卡列表页的页面底（iOS grouped background）。
     *
     * `background` 是 Material 的「页面底色」，浅色主题里就是纯白，和 `card` 同色；
     * 设置 / 工具箱 / 分析 / 我的 / 历史这类「白卡片分组列表」需要再低一层的灰底才能看出边界。
     * 浅色固定 `#F2F2F7`，深色固定 `#000000`（A2 的 AMOLED 纯黑）。
     */
    var groupedBackground by mutableStateOf(Color.Unspecified)
        private set
    var chip by mutableStateOf(Color.Unspecified)
        private set
    var onChip by mutableStateOf(Color.Unspecified)
        private set
    var unselected by mutableStateOf(Color.Unspecified)
        private set
    var card by mutableStateOf(Color.Unspecified)
        private set
    var floorCard by mutableStateOf(Color.Unspecified)
        private set
    var divider by mutableStateOf(Color.Unspecified)
        private set
    var shadow by mutableStateOf(Color.Unspecified)
        private set
    var indicator by mutableStateOf(Color.Unspecified)
        private set
    var windowBackground by mutableStateOf(Color.Unspecified)
        private set
    var placeholder by mutableStateOf(Color.Unspecified)
        private set

    // ── 设计规范叫法的别名（不与上面重复存储）──
    val textPrimary: Color get() = text
    val textTertiary: Color get() = textDisabled
    val separator: Color get() = divider

    /**
     * 破坏性操作色（删除 / 退出账号 / 举报 …）。
     *
     * **不来自 XML 主题**：XML 主题 没有对应语义（历史原因，danger 只在个别页面
     * 硬编码成红色），所以取 [GlassTokenSet] 的规范值（浅 `#FF3B30` / 深 `#FF453A`，
     * iOS 系统红），这样全站危险操作只有一处定义，且与 Glasmio 组件（`GlasmioTheme.colors
     * .destructive`、`AppActionSheet` 的 `destructive` 项）保持同一档颜色。
     */
    var destructive by mutableStateOf(Color.Unspecified)
        private set

    /**
     * **从 XML 主题属性直接解析填充**（Stage 6：不再经过中间层）。
     *
     * 字段与属性一一对应（与迁移前 `getThemeColorForTheme` 完全相同的读法）：
     * 主题名走 [com.huanchengfly.tieba.post.utils.ThemeUtil.getCurrentTheme]，
     * accent 覆盖由 `App.ThemeDelegate.getColorByAttr` 内部处理，因此「同一主题 + 同一 accent
     * → 同一批颜色」这条不变式仍然成立。
     *
     * 深色纯黑覆盖（原 A2 逻辑）也在这里：只归零页面/窗口/顶底栏背景，卡片与文字保持主题语义。
     */
    fun fillFromTheme(context: android.content.Context, theme: String) {
        val nowTheme = com.huanchengfly.tieba.post.utils.ThemeUtil.getCurrentTheme(theme)
        fun attr(id: Int): Color =
            Color(com.huanchengfly.tieba.post.App.ThemeDelegate.getColorByAttr(context, id, nowTheme))

        val textColor = attr(com.huanchengfly.tieba.post.R.attr.colorText)
        this.theme = nowTheme
        isNightMode = com.huanchengfly.tieba.post.utils.ThemeUtil.isNightMode(nowTheme)
        primary = attr(com.huanchengfly.tieba.post.R.attr.colorNewPrimary)
        onPrimary = attr(com.huanchengfly.tieba.post.R.attr.colorOnAccent)
        accent = attr(com.huanchengfly.tieba.post.R.attr.colorAccent)
        onAccent = attr(com.huanchengfly.tieba.post.R.attr.colorOnAccent)
        topBar = attr(com.huanchengfly.tieba.post.R.attr.colorToolbar)
        onTopBar = attr(com.huanchengfly.tieba.post.R.attr.colorToolbarItem)
        onTopBarSecondary = attr(com.huanchengfly.tieba.post.R.attr.colorToolbarItemSecondary)
        onTopBarActive = attr(com.huanchengfly.tieba.post.R.attr.colorToolbarItemActive)
        topBarSurface = attr(com.huanchengfly.tieba.post.R.attr.colorToolbarSurface)
        onTopBarSurface = attr(com.huanchengfly.tieba.post.R.attr.colorOnToolbarSurface)
        bottomBar = attr(com.huanchengfly.tieba.post.R.attr.colorNavBar)
        bottomBarSurface = attr(com.huanchengfly.tieba.post.R.attr.colorNavBarSurface)
        onBottomBarSurface = attr(com.huanchengfly.tieba.post.R.attr.colorOnNavBarSurface)
        // ContentAlpha 在 M2 里是 @Composable getter，这里用它的固定值（high=0.87 / medium=0.60）
        text = textColor.copy(alpha = 0.87f)
        textSecondary = textColor.copy(alpha = 0.60f)
        textDisabled = attr(com.huanchengfly.tieba.post.R.attr.color_text_disabled)
        background = attr(com.huanchengfly.tieba.post.R.attr.colorBackground)
        chip = attr(com.huanchengfly.tieba.post.R.attr.colorChip)
        onChip = attr(com.huanchengfly.tieba.post.R.attr.colorOnChip)
        unselected = attr(com.huanchengfly.tieba.post.R.attr.colorUnselected)
        card = attr(com.huanchengfly.tieba.post.R.attr.colorCard)
        floorCard = attr(com.huanchengfly.tieba.post.R.attr.colorFloorCard)
        divider = attr(com.huanchengfly.tieba.post.R.attr.colorDivider)
        shadow = attr(com.huanchengfly.tieba.post.R.attr.shadow_color)
        indicator = attr(com.huanchengfly.tieba.post.R.attr.colorIndicator)
        windowBackground = attr(com.huanchengfly.tieba.post.R.attr.colorWindowBackground)
        placeholder = attr(com.huanchengfly.tieba.post.R.attr.colorPlaceholder)
        // 破坏性操作色：不跟随 accent（否则删除/退出会显示成主题色）
        destructive = if (isNightMode) Color(0xFFFF453A) else Color(0xFFFF3B30)
        // 分组卡列表页的页面底：设计规范固定值，不来自 XML attr
        // （漏掉这行会让所有分组页露出窗口底色，浅色下表现为整体偏灰）
        groupedBackground = if (isNightMode) Color.Black else Color(0xFFF2F2F7)

        // A2：深色统一纯黑（半透明主题有背景图，跳过）
        // 卡色/文字同时归到 iOS 深色中性色板，与玻璃层的 DarkGlassTokens(#1C1C1E / #2C2C2E / 纯白)
        // 完全对齐 —— 否则同一屏里「玻璃卡(中性灰) + M2 卡片(蓝调 #15202B)」会看出色相不一致。
        if (isNightMode && !com.huanchengfly.tieba.post.utils.ThemeUtil.isTranslucentTheme(nowTheme)) {
            background = Color.Black
            windowBackground = Color.Black
            topBar = Color.Black
            bottomBar = Color.Black
            // 卡片：中性灰（原 XML 深色是 #15202B 蓝调 / AMOLED 是 #101010，这里统一覆盖）
            card = Color(0xFF1C1C1E)
            // 楼层比卡片高一层（原 #1A2A39 蓝调）
            floorCard = Color(0xFF2C2C2E)
            // 主文字纯白（原 textColor@87% ≈ #DEDEDE）
            text = Color.White
        }
    }
}
