package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTextStyle
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.SquircleShape
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors

/**
 * 设置页分组卡片的视觉常量（FlClash `prism_ui` 的 settings 组）。
 *
 * 数值能对上 [GlassTokens] 的就直接引用令牌，避免这里再长出一套「第三份数值」；
 * 只有 FlClash 有、令牌里没有的（分割线颜色 / 缩进 / chevron 尺寸）才在这里定义。
 */
object PrefsCardDefaults {

    /** 页面左右留白：16dp。 */
    val PagePadding: Dp = GlassTokens.Spacing.pagePadding

    /** 组与组的间距：8dp。 */
    val CardGap: Dp = GlassTokens.Spacing.cardGap

    /** 行最小高度：56dp（FlClash rowMinHeight）。 */
    val RowMinHeight: Dp = GlassTokens.Spacing.rowMinHeight

    /** 卡片圆角：16dp，用超椭圆（连续圆角）而不是正圆弧。 */
    val CornerRadius: Dp = GlassTokens.Radii.md

    /** 组内分割线：1dp，左侧缩进 16dp。 */
    val DividerThickness: Dp = 1.dp

    val DividerIndent: Dp = 16.dp

    /** 行左右内边距。 */
    val ContentPadding: Dp = GlassTokens.Spacing.pagePadding

    /**
     * 行内上下内边距。
     *
     * 取 6dp 是为了让「标题 22sp + 副标题最多 2 行 40sp」的行也不超过 74dp：
     * 22 + 40 + 6×2 = 74；单行/无副标题的行由 [RowMinHeight] 兜到 56dp。
     */
    val RowVerticalPadding: Dp = 6.dp

    /** 次级行（`minimalHeight`）最小高度。 */
    val MinimalRowMinHeight: Dp = 32.dp

    /** 右侧 chevron 尺寸。 */
    val ChevronSize: Dp = 20.dp

    /** 分组标题与卡片之间的间距。 */
    val HeaderGap: Dp = 8.dp

    /** 非首个分组的标题上方额外间距（组间距 8dp + 16dp = 24dp）。 */
    val HeaderTopSpacing: Dp = 16.dp

    /**
     * 卡片表层色：浅色 `#FFFFFF`、深色 `#1C1C1E`。
     *
     * 刻意走 `glassColors().surface`（同一套令牌），但**不**走玻璃材质：
     * 列表与卡片永不用玻璃，只有浮层（dock / 弹层）用。
     */
    @Composable
    fun surface(): Color = glassColors().surface

    /**
     * 组内分割线颜色：浅色 `#00000014`、深色 `#FFFFFF1F`。
     *
     * 用半透明黑 / 白而不是实色灰，是为了在「半透明主题」的背景图上也保持正确的层次。
     */
    @Composable
    fun divider(): Color =
        if (glassColors().isDark) Color(0x1FFFFFFF) else Color(0x14000000)
}

/** 卡片形状：16dp 连续圆角（超椭圆）。 */
private val PrefsCardShape: Shape by lazy { SquircleShape(PrefsCardDefaults.CornerRadius) }

/**
 * 一张分组卡片。
 *
 * - 卡片本体：表层色 + 16dp 连续圆角 + **无阴影**（FlClash settings 组就是平铺卡片）；
 * - 可选分组标题：渲染在卡片**上方**（FlClash / iOS 的分组标题不在卡片里）；
 *   [headerTopSpacing] 是标题到「上一张卡片/页面顶部」的距离，由 [PrefsScreen] 按
 *   「首组 16dp、其余组 24dp（已扣掉组间距 8dp）」传入；
 * - 卡片内部的分割线由调用方（[PrefsScreen]）插在行与行之间。
 */
@Composable
internal fun PrefsCard(
    modifier: Modifier = Modifier,
    headerTopSpacing: Dp = 0.dp,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (header != null) {
            if (headerTopSpacing > 0.dp) {
                Spacer(modifier = Modifier.height(headerTopSpacing))
            }
            header()
            Spacer(modifier = Modifier.height(PrefsCardDefaults.HeaderGap))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PrefsCardShape)
                .background(color = PrefsCardDefaults.surface())
        ) {
            Column(content = content)
        }
    }
}

/**
 * 把 [GlassTextStyle]（纯数值）转成 Compose 的 [TextStyle]。
 *
 * `GlassTokens` 里只放数值、不放 `TextStyle`，是为了不依赖 `sp` 以外的主题状态；
 * 真正的字号注入在这里做，颜色由调用方给出（浅深色不同）。
 */
internal fun GlassTextStyle.toTextStyle(color: Color = Color.Unspecified): TextStyle =
    TextStyle(
        color = color,
        fontSize = size,
        lineHeight = lineHeight,
        fontWeight = weight,
    )
