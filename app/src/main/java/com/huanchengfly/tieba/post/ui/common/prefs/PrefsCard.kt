package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
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
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioGroupedTableFooter

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

/**
 * Glasmio 分组列表（B 版）的几何常量。
 *
 * 这些数字**不是**我们定的：`GlasmioGroupedList` / `GlasmioPreferenceEntry` /
 * `GlasmioGroupedTableFooter` 内部就写死了 26dp 连续圆角、行左右 16dp、行高 52/68dp、
 * footnote 左右 16dp + 上间距 8dp。这里只记录「页面这一层」需要配合的四个量，
 * 方便和 [PrefsCardDefaults] 对照，不要另起一套视觉体系。
 */
object PrefsGlasmioDefaults {

    /**
     * 页面左右留白：16dp（与 B 版样板页一致）。
     *
     * 注意这是**页面这一层**的留白：分组卡片的圆角要能看见、不能贴屏幕边，
     * 行内容再在卡片内部吃 16dp（`GlasmioPreferenceEntry` 自带），所以行内文字距屏幕
     * 32dp、组标题与 footnote 同理对齐 32dp —— 这正是 iOS「inset grouped」的层级。
     */
    val PageHorizontalPadding: Dp = 16.dp

    /** 列表顶部留白（大标题之后由 LazyColumn 的 item 间距接管）。 */
    val PageTopPadding: Dp = 4.dp

    /** 列表底部留白，保证最后一条 footnote 不贴屏幕底。 */
    val PageBottomPadding: Dp = 24.dp

    /** 悬浮返回钮尺寸：44dp（与主界面顶栏的玻璃圆钮同规格）。 */
    val BackButtonSize: Dp = 44.dp

    /** 返回钮里的图标：20dp。 */
    val BackButtonIconSize: Dp = 20.dp

    /** 返回钮左边距（与卡片的 16dp 页面留白视觉对齐，浮起时略微内收）。 */
    val BackButtonStart: Dp = 12.dp

    /** 返回钮相对状态栏的下边距。 */
    val BackButtonTop: Dp = 6.dp

    /**
     * 列表内容在状态栏之下的起始留白 = 返回钮（6 + 44）+ 大标题前的 12dp 间隙。
     *
     * 悬浮返回钮占位但不吃布局：内容整体从这个位置起排，所以按钮不会压住大标题，
     * 也不会在顶栏位置留一条空白带。
     */
    val BackButtonTopSpace: Dp = 62.dp

    /** 组与组的间距：Glasmio 自己的分组列表节奏。 */
    val GroupGap: Dp = 10.dp

    /**
     * 行左侧图标槽的最小尺寸：28dp。
     *
     * Glasmio 的行高是写死的 52dp（= 12dp 上下内边距 + 28dp 内容），而 [PrefsIcon] 画的是
     * 23dp 的 `SfIcon`，正好落在 28dp 的槽里 → 行高回到 52dp。
     * 槽是「最小尺寸」不是固定尺寸：真实头像（36dp 的 `Avatar`，唯一保留图片的例外）
     * 会把这一行撑到 60dp，但只有那一行。
     *
     * 文字起点 = 16(行内边距) + 4 + 28 + 4 + 12(槽后间距) = 64dp。
     */
    val IconSlot: Dp = 28.dp

    /** 行左右内边距（Glasmio 写死 16dp，仅用于宽度估算）。 */
    val RowHorizontalPadding: Dp = 16.dp
}

/** 卡片形状：16dp 连续圆角（超椭圆）。 */
private val PrefsCardShape: Shape by lazy { SquircleShape(PrefsCardDefaults.CornerRadius) }

/**
 * 一张分组卡片。
 *
 * 两种渲染路径：
 * - `PrefsUiStyle.GLASSMIO`（默认）：组标题 = `GlasmioPreferenceGroupTitle`，
 *   卡片 = `GlasmioGroupedList`（26dp 连续圆角 + `elevatedBackground` + 行内建分隔线），
 *   组尾 = `GlasmioGroupedTableFooter`（13sp 次级色、左右 16dp、上间距 8dp）。
 * - `PrefsUiStyle.LEGACY`：迁移前的 FlClash 卡片（16dp 超椭圆 + 不透明表层色 + 1dp 分割线），
 *   组尾 footnote 用同字号（13sp）的次级文字手绘，保证「切回 A 版」时 footer 语义不丢。
 *
 * 组尾 footnote 有**两个来源**，按顺序渲染：
 * 1. `footer`：DSL 里 `prefsGroup(title, footer = ...)` 显式给的（组级说明）；
 * 2. `PrefsFootnoteState` 里自动登记的：行内 `summary` 超过 1 行时，行会把它挪到这里
 *    （见 [PrefsListItem]），顺序 = 行顺序。
 */
@Composable
internal fun PrefsCard(
    modifier: Modifier = Modifier,
    headerTopSpacing: Dp = 0.dp,
    header: (@Composable () -> Unit)? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // 自动转义的 footnote（行登记进来的，组尾统一渲染）
    val autoFootnotes = LocalPrefsFootnotes.current?.ordered().orEmpty()
    val explicitFooter = footer?.takeIf { it.isNotBlank() }

    if (LocalPrefsUiStyle.current == PrefsUiStyle.GLASSMIO) {
        Column(modifier = modifier.fillMaxWidth()) {
            if (header != null) {
                if (headerTopSpacing > 0.dp) {
                    Spacer(modifier = Modifier.height(headerTopSpacing))
                }
                header()
            }
            GlasmioGroupedList(content = content)
            // 显式 footer 在自动 footnote 之前：前者是「这一组是什么」，后者是「某一行补充什么」
            explicitFooter?.let { GlasmioGroupedTableFooter(it) }
            autoFootnotes.forEach { GlasmioGroupedTableFooter(it) }
        }
        return
    }

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
        // A 版没有 Glasmio 的 footnotes 组件，用同字号手绘：13sp / 次级色 / 左右 16dp / 上间距 8dp
        explicitFooter?.let { PrefsFootnote(it) }
        autoFootnotes.forEach { PrefsFootnote(it) }
    }
}

/** LEGACY 路径的组尾 footnote：与 Glasmio 的 `GlasmioGroupedTableFooter` 同参数。 */
@Composable
private fun PrefsFootnote(text: String) {
    Text(
        text = text,
        style = GlassTokens.Typography.footnote.toTextStyle(glassColors().textSecondary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
    )
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
