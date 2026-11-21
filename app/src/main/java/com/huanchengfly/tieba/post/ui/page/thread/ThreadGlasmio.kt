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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassBackdropHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassSurface
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
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
import io.github.neoruaa.glasmio.glass.NavigationGlassMaterial

/**
 * 帖子详情页的 Glasmio 外壳（`ThreadPageUiStyle.GLASMIO`）。
 *
 * 架构：
 * - `sampledContent`：页面内容被录进 LayerBackdrop；
 * - `overlayContent`：顶栏 + 底栏都画在录制层之上，因此两处的玻璃都能采到**整页真实内容**；
 * - 页面列表用 `contentPadding.top/bottom` 给两层浮层让位，滚动时内容从它们下面穿过。
 *
 * 材质只有一种：`LiquidGlassSurface(material = RegularSmall)` + `glassNavigationTint()` +
 * 1dp 浅色 rim。顶栏三控件、底栏两个胶囊全部复用 [ThreadGlassPill]。
 */
@Composable
fun ThreadPageSkin(
    enabled: Boolean,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
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
            overlayContent = {
                Box(modifier = Modifier.fillMaxSize()) {
                    topBar()
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        bottomBar()
                    }
                }
            },
        )
    }
}

/** 悬浮顶栏高度（44dp 控件 + 上下各 4dp）。 */
val ThreadTopBarHeight = 52.dp

/** 底栏胶囊高度。 */
val ThreadBottomBarHeight = 56.dp

/**
 * 列表底部 contentPadding 的基准值：
 * 胶囊 56dp + 上下 8dp 间距 = 72dp，再多留 8dp 让最后一条内容能完整停在胶囊上方。
 * 真正使用时还要叠加 `navigationBars` 的高度。
 */
val ThreadBottomBarInset = 80.dp

private val ThreadTopActionSize = 44.dp

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
    val moreDescription = stringResource(id = R.string.btn_more)
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scrimHeight = statusBarTop + 88.dp
    val scrimBrush = Brush.verticalGradient(
        0f to colors.groupedBackground.copy(alpha = 0.82f),
        0.58f to colors.groupedBackground.copy(alpha = 0.48f),
        1f to Color.Transparent,
    )

    Box(modifier = modifier.fillMaxWidth()) {
        if (scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scrimHeight)
                    .background(scrimBrush)
                    .then(Modifier.alpha(scrimAlpha))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(ThreadTopBarHeight)
                .padding(horizontal = 8.dp)
        ) {
            ThreadGlassIconButton(
                onClick = onBack,
                contentDescription = stringResource(id = R.string.button_back),
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = colors.content,
                    modifier = Modifier.size(20.dp),
                )
            }

            val forumShape = RoundedCornerShape(ThreadTopActionSize / 2)
            ThreadGlassPill(
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(ThreadTopActionSize)
                    .widthIn(max = 220.dp)
                    .wrapContentWidth()
                    .clip(forumShape)
                    .debounceClickable(onClick = onForumClick),
                shape = forumShape,
            ) {
                Text(
                    text = forumName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 13.dp)
                        .semantics { contentDescription = forumName },
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .semantics { contentDescription = moreDescription },
            ) {
                ClickMenu(
                    menuContent = menuContent,
                    menuWidth = 330.dp,
                    triggerShape = CircleShape,
                ) {
                    ThreadGlassIconSurface {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = null,
                            tint = colors.content,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 全页唯一的玻璃壳：`RegularSmall` + `glassNavigationTint()` + 细 rim。
 * 顶栏圆钮、吧名胶囊、底栏输入/信息胶囊全部走这里，避免再出现第二种材质。
 */
@Composable
private fun ThreadGlassPill(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    content: @Composable BoxScope.() -> Unit,
) {
    val rim = if (glassColors().isDark) {
        Color.White.copy(alpha = 0.40f)
    } else {
        Color.White.copy(alpha = 0.58f)
    }
    LiquidGlassSurface(
        modifier = modifier,
        shape = shape,
        material = NavigationGlassMaterial.RegularSmall,
        navigationSurfaceColor = glassNavigationTint(),
        emphasis = GlassEmphasis.Regular,
        innerGlow = true,
    ) {
        content()
        // rim 叠在玻璃之上，只做 1dp 轮廓，不做「透明线框」。
        Box(modifier = Modifier.matchParentSize().border(1.dp, rim, shape))
    }
}

@Composable
private fun ThreadGlassIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(ThreadTopActionSize)
            .clip(CircleShape)
            .semantics { this.contentDescription = contentDescription.orEmpty() }
            .debounceClickable(onClick = onClick)
    ) {
        ThreadGlassPill(
            modifier = Modifier.matchParentSize(),
            shape = CircleShape,
        ) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ThreadGlassIconSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ThreadGlassPill(
        modifier = modifier.size(ThreadTopActionSize),
        shape = CircleShape,
    ) {
        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * 底栏：两个玻璃胶囊。这个 composable 只在 `ThreadPageSkin` 的 overlay 槽里调用，
 * 所以 `LiquidGlassSurface` 采到的是页面真实内容，不再有「纯色小 host」的白塑料感。
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
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ThreadBottomBarHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(ThreadBottomBarHeight)
                    .clip(shape)
                    .debounceClickable(onClick = onClickReply)
            ) {
                ThreadGlassPill(
                    modifier = Modifier.matchParentSize(),
                    shape = shape,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
            }

            ThreadGlassPill(
                modifier = Modifier
                    .width(196.dp)
                    .height(ThreadBottomBarHeight),
                shape = shape,
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
