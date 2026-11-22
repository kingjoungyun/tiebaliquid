package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.icons.SfIcon
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalGlassTokens

/**
 * 设置行的左侧图标：Glasmio 的 `SfIcon`（23dp、accent 着色），与 B 版样板页
 * （`GlassSettingsPreviewActivity`）完全一致。
 *
 * 为什么不再用 App 原来的 `LeadingIcon { AvatarIcon(...) }`：
 * 那套是「36dp 圆形 + 56dp 占位 Spacer」的 FlClash 版式，图标本身是 `Icons.*` 的 Material
 * 矢量，跟 Glasmio 的线性图标语言不统一（B 版对照时已经确认过）。这里只保留**语义**
 * （每个业务行传一个 SF Symbols 名称），渲染交给 Glasmio：
 *
 * - 本工程没有 SF Symbols 字体（无授权），`SfIcon` 会自动回落到 Glasmio 自带的
 *   Material 矢量表（`MaterialGlasmioIconResolver`），颜色/尺寸仍由这里控制；
 * - 图标尺寸 23dp、`accent` 着色 —— 主题色切换时图标跟着变（验收项之一）。
 *
 * 唯一例外是「账号头像」：真实头像（`Avatar`）仍走 App 原来的图片渲染，
 * 因为那不是图标而是用户内容。
 */
@Composable
fun PrefsIcon(
    symbol: String,
    /** 着色；默认 accent（主题色）。破坏性操作行传 [com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens.destructive]。 */
    tint: Color = LocalGlassTokens.current.accent,
) {
    SfIcon(
        systemName = symbol,
        contentDescription = null,
        size = 23.dp,
        tint = tint,
    )
}
