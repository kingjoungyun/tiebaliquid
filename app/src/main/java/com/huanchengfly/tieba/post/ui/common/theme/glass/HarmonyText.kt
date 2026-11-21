package com.huanchengfly.tieba.post.ui.common.theme.glass

import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle

/**
 * 鸿蒙字族的 M2 通道注入。
 *
 * 为什么需要这一步：`GlasmioTheme(typography = HarmonyGlasmioTypography)` 装的是 **M3** 的
 * `LocalTextStyle`（`androidx.compose.material3.LocalTextStyle`）；而业务页里的 `Text` 基本都是
 * **M2** 的 `Text`（`androidx.compose.material.Text`），它只读 **M2** 的 `LocalTextStyle`
 * ——也就是 `TiebaLiteTheme` 里那张系统字体表。所以「页面已经包在 LiquidGlassTheme 里」并不等于
 * 文字用上了鸿蒙（设置页 / 我的页也是显式把 M2 `LocalTextStyle` 指到 Glasmio 样式的，
 * 见 `PrefsListItem.kt` 的注释）。
 *
 * 字号 / 行高 / 字重 / 颜色仍然由每个 `Text` 自己写死，这里只换 `fontFamily`。
 * 鸿蒙字族声明了 400 / 500 / 600 三档真实字重（`HarmonyFontFamily`），所以 `FontWeight(600)`
 * 会命中 Semibold 字体文件，不会像系统字族那样由引擎合成加粗（观感就是「字发虚」）。
 */
val harmonyTextStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = LocalTextStyle.current.copy(fontFamily = HarmonyFontFamily)

/**
 * 把 [harmonyTextStyle] 注入这棵子树（只影响 M2 的 `Text`）。
 *
 * 用在 Glasmio 化的业务页：信息流（`FeedPageSkin`）、吧页（`ForumPageSkin`）等。
 */
@Composable
fun HarmonyTextProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextStyle provides harmonyTextStyle) {
        content()
    }
}
