package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassBackdropHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassSurface
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalGlassSamplingSafe
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalLiquidGlassHosted
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassNavigationTint
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuScope
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost

/**
 * 帖子详情页的 Glasmio 外壳（`ThreadPageUiStyle.GLASMIO`）。
 *
 * 这一层只做三件事，楼层内容一行都不动：
 * 1. 注入 [LiquidGlassTheme]（App 自己的明暗 / 主题色 / 鸿蒙字表）；
 * 2. 页级 backdrop host：页面内容进 `sampledContent`（被录进 LayerBackdrop），
 *    页面里的玻璃浮层（顶栏悬浮钮、菜单、底部操作栏）就能采到这一页的真实内容；
 * 3. 给 [topBar] 一个 **overlay 槽**：它画在录制层之上，因此可以安全地采样整页，
 *    也才能做到「内容从顶栏下穿过」。
 *
 * 和 `GlassPageHost` 的区别只有第 3 点——`GlassPageHost` 只接受页面内容，没有 overlay 槽。
 *
 * 背景画在 `sampledContent` **里面**（而不是 host 的 modifier 上），否则 backdrop 里只有
 * 透明底，玻璃顶栏/菜单采样出来会发黑。
 */
@Composable
fun ThreadPageSkin(
    enabled: Boolean,
    topBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    if (!enabled || !isLiquidGlassSupported()) {
        content()
        return
    }
    LiquidGlassTheme(accent = ExtendedTheme.colors.primary) {
        LiquidGlassBackdropHost(
            modifier = Modifier.fillMaxSize(),
            sampledContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GlasmioTheme.colors.groupedBackground)
                ) {
                    content()
                }
            },
            overlayContent = { topBar() },
        )
    }
}

/** 悬浮顶栏的高度（44dp 控件 + 上下各 4dp）。列表的顶部 contentPadding 也用它。 */
val ThreadTopBarHeight = 52.dp

private val ThreadTopActionSize = 44.dp

/**
 * 帖子页的悬浮顶栏（只在 [ThreadPageSkin] 的 overlay 槽里使用）。
 *
 * 版式：`(←)  [吧名]  (⋯)` 三个 44dp 控件，左右两个圆钮、中间一个固定 44dp 的胶囊。
 * 三个控件都是「透明底 + 1px 描边」，不再用白色实心玻璃；浅色黑/深灰描边，深色浅描边。
 *
 * 滚动时在控件后面淡入一层柔和 scrim（页面底色 → 透明），避免胶囊与内容叠字；
 * scrim 只在 y 方向覆盖状态栏 + 顶栏 + 一小段渐隐，不是实心条。
 */
@Composable
fun ThreadGlassTopBar(
    forumName: String,
    scrimAlpha: Float,
    onBack: () -> Unit,
    onForumClick: () -> Unit,
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scrimHeight = statusBarTop + 88.dp
    val scrimBrush = Brush.verticalGradient(
        0f to colors.groupedBackground.copy(alpha = 0.88f),
        0.58f to colors.groupedBackground.copy(alpha = 0.56f),
        1f to Color.Transparent,
    )

    Box(modifier = modifier.fillMaxWidth()) {
        if (scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scrimHeight)
                    .alpha(scrimAlpha)
                    .background(scrimBrush)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(ThreadTopBarHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThreadGlassOutlineButton(
                onClick = onBack,
                contentDescription = stringResource(id = R.string.button_back),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = colors.content,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            ThreadGlassOutlineButton(
                onClick = onForumClick,
                contentDescription = forumName,
            ) {
                Text(
                    text = forumName,
                    // 44dp 宽度的胶囊里，10sp 刚好能放下 4 个汉字（“动漫壁纸”不省略）；
                    // 更长的吧名按 spec 截断省略。
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 1.dp)
                        .semantics { contentDescription = forumName },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            ClickMenu(
                menuContent = menuContent,
                menuWidth = 330.dp,
                // 不写死高度：AppMenu 会按内容实测（ThreadMenu 有 9 项时约 500dp）。
                triggerShape = CircleShape,
            ) {
                ThreadGlassOutlineShell {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(id = R.string.btn_more),
                        tint = colors.content,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * 顶栏控件的统一壳：44dp 圆形、透明底、1px 描边。
 *
 * 菜单触发器不能带 clickable（外层 `ClickMenu` 要自己处理点击），所以拆成
 * [ThreadGlassOutlineShell]（只画壳）与 [ThreadGlassOutlineButton]（壳 + 点击）两个入口。
 */
@Composable
private fun ThreadGlassOutlineShell(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = glassColors().isDark
    val stroke = if (dark) {
        Color.White.copy(alpha = 0.46f)
    } else {
        Color.Black.copy(alpha = 0.28f)
    }
    Box(
        modifier = modifier
            .size(ThreadTopActionSize)
            .clip(CircleShape)
            .border(width = 1.dp, color = stroke, shape = CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun ThreadGlassOutlineButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    ThreadGlassOutlineShell(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription.orEmpty() }
            .debounceClickable(onClick = onClick),
        content = content,
    )
}

/**
 * 帖子页的玻璃底部操作栏（`ThreadPageUiStyle.GLASMIO`）。
 *
 * 两个等高的胶囊：
 * ```
 * [ 评论一番（占剩余宽度） ]   [ 💬12  ♡100  👤  ⇅ ]
 * ```
 * 右侧信息胶囊按顺序放四组：回复数 / 点赞 / 只看楼主 / 正序倒序，宽度按内容估算为 196dp，
 * 不做 `weight` 撑满；点输入胶囊仍进回复页。
 */
@Composable
fun ThreadGlassBottomBar(
    onClickReply: () -> Unit,
    onAgree: () -> Unit,
    onReplyCountClick: () -> Unit,
    onSeeLzClick: () -> Unit,
    onSortClick: () -> Unit,
    hasAgreed: Boolean,
    agreeNum: Long,
    replyNum: Int,
    isSeeLz: Boolean,
    isDesc: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val normal = colors.secondaryContent
    val accent = colors.accent
    val sortColor = if (isDesc) accent else colors.content

    Box(
        modifier = modifier
            .fillMaxWidth()
            // 导航栏那一条是透明的：铺上页面底色，列表内容不会从胶囊下方漏出来。
            .background(colors.groupedBackground)
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThreadGlassBarSurface(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .debounceClickable(onClick = onClickReply)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = stringResource(id = R.string.tip_reply_thread),
                        style = IosTypography.subheadline,
                        color = normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            ThreadGlassBarSurface(
                modifier = Modifier
                    .width(196.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ThreadInfoGroup(
                        icon = Icons.AutoMirrored.Rounded.Comment,
                        count = replyNum.toLong(),
                        color = normal,
                        contentDescription = stringResource(
                            id = R.string.title_thread_header,
                            replyNum.toString(),
                        ),
                        onClick = onReplyCountClick,
                    )
                    ThreadInfoGroup(
                        icon = if (hasAgreed) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        count = agreeNum,
                        color = if (hasAgreed) accent else normal,
                        contentDescription = stringResource(id = R.string.title_agree),
                        onClick = onAgree,
                    )
                    ThreadInfoGroup(
                        icon = Icons.Rounded.Person,
                        count = null,
                        color = if (isSeeLz) accent else normal,
                        contentDescription = stringResource(id = R.string.title_see_lz),
                        onClick = onSeeLzClick,
                    )
                    ThreadInfoGroup(
                        icon = Icons.Rounded.SwapVert,
                        count = null,
                        color = sortColor,
                        contentDescription = stringResource(
                            id = if (isDesc) R.string.title_desc else R.string.title_asc,
                        ),
                        onClick = onSortClick,
                        iconRotation = if (isDesc) 180f else 0f,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThreadInfoGroup(
    icon: ImageVector,
    count: Long?,
    color: Color,
    contentDescription: String,
    onClick: () -> Unit,
    iconRotation: Float = 0f,
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .semantics { this.contentDescription = contentDescription }
            .debounceClickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(18.dp)
                .rotate(iconRotation),
        )
        if (count != null && count > 0) {
            Text(
                text = count.getShortNumString(),
                fontSize = 13.sp,
                color = color,
                maxLines = 1,
            )
        }
    }
}

/**
 * 底部玻璃壳的采样环境适配。
 *
 * 它画在页面内容层（`sampledContent`）里，读外层的 `LocalGlassBackdrop` 会「自己采样自己」，
 * 所以和 `GlassIconButton` 一样：自带一个只有 tint 纯色的小 host，再用 Glasmio 材质去采它。
 * 观感是真玻璃（有霜化 / 边缘高光），代价只是一张 56dp 高的纯色纹理。
 */
@Composable
private fun ThreadGlassBarSurface(
    modifier: Modifier,
    shape: Shape,
    content: @Composable BoxScope.() -> Unit,
) {
    val tint = glassNavigationTint()
    if (LocalGlassSamplingSafe.current) {
        LiquidGlassSurface(
            modifier = modifier,
            shape = shape,
            navigationSurfaceColor = tint,
            emphasis = GlassEmphasis.Regular,
            innerGlow = true,
            content = content,
        )
        return
    }
    GlasmioBackdropHost(
        sampledColor = tint,
        modifier = modifier.clip(shape),
    ) {
        CompositionLocalProvider(
            LocalLiquidGlassHosted provides true,
            LocalGlassSamplingSafe provides true,
        ) {
            LiquidGlassSurface(
                modifier = Modifier.fillMaxSize(),
                shape = shape,
                navigationSurfaceColor = tint,
                emphasis = GlassEmphasis.Regular,
                innerGlow = true,
                content = content,
            )
        }
    }
}
