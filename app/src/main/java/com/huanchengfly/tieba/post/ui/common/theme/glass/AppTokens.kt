package com.huanchengfly.tieba.post.ui.common.theme.glass

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors

/**
 * **AppTokens —— 全 App 唯一的颜色令牌（运行时填充）。**
 *
 * 定位（Stage 6）：`ExtendedColors` 继续负责「从 XML 主题 + 6 色 accent + AMOLED 解析出颜色」
 * （它是 attr 机制的产物，不能删），解析完成后**把同一批值灌进这里**；页面统一读
 * `AppTokens.xxx`。两套并存期间值严格一致（同一个 [ExtendedColors] 实例填出来）。
 *
 * 为什么字段是 `mutableStateOf` 而不是常量：主题（亮/暗）、accent（6 色）、AMOLED 都会
 * 在运行中切换。用 snapshot state 才能让读 `AppTokens.xxx` 的 Composable 自动重组；
 * 写死成常量会让换主题/换色失效。
 *
 * 字段与 [ExtendedColors] 1:1（命名保持一致，便于机械替换）；另外提供
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

    // ── 语义色（Glasmio 侧用，来自 token 集合）──
    var destructive by mutableStateOf(Color.Unspecified)
        private set

    /**
     * 用一次主题解析的结果填充所有 token。
     *
     * 调用点只有一个：[com.huanchengfly.tieba.post.ui.common.theme.compose.getThemeColorForTheme]
     * —— 它本来就是「XML 主题 + accent 覆盖 + AMOLED」的唯一收敛点，在它构造出
     * [ExtendedColors] 之后调用这里，保证两套值来自同一份计算。
     *
     * 注意：参数顺序/字段名与 [ExtendedColors] 一致，将来加字段两边一起加。
     */
    fun fill(from: ExtendedColors) {
        theme = from.theme
        isNightMode = from.isNightMode
        primary = from.primary
        onPrimary = from.onPrimary
        accent = from.accent
        onAccent = from.onAccent
        topBar = from.topBar
        onTopBar = from.onTopBar
        onTopBarSecondary = from.onTopBarSecondary
        onTopBarActive = from.onTopBarActive
        topBarSurface = from.topBarSurface
        onTopBarSurface = from.onTopBarSurface
        bottomBar = from.bottomBar
        bottomBarSurface = from.bottomBarSurface
        onBottomBarSurface = from.onBottomBarSurface
        text = from.text
        textSecondary = from.textSecondary
        textDisabled = from.textDisabled
        background = from.background
        groupedBackground = if (from.isNightMode) Color.Black else Color(0xFFF2F2F7)
        chip = from.chip
        onChip = from.onChip
        unselected = from.unselected
        card = from.card
        floorCard = from.floorCard
        divider = from.divider
        shadow = from.shadow
        indicator = from.indicator
        windowBackground = from.windowBackground
        placeholder = from.placeholder
        destructive = from.accent
    }
}
