package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.HarmonyTextProvider
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassBackdropHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import com.huanchengfly.tieba.post.ui.widgets.compose.AppMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuScope
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideContentColor
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons

/**
 * 帖子详情页的 Glasmio 外壳（`ThreadPageUiStyle.GLASMIO`）。
 *
 * 顶栏/底栏都在 overlay 层：玻璃控件能采到页面真实内容。
 * 材质直接使用 Glasmio 组件默认值，不再叠加 tint / rim / innerGlow 等自定义参数：
 * - 返回 / ⋯：`GlasmioIconButton` 默认尺寸与默认材质；
 * - 吧名胶囊、底栏两个胶囊：`GlassSurface` 默认参数 + 胶囊 shape。
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
        HarmonyTextProvider {
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
    val menuState = rememberMenuState()
    val menuScope = remember(menuState) { MenuScope(menuState) }
    var menuTriggerBounds by remember { mutableStateOf<Rect?>(null) }

    Box(modifier = modifier.fillMaxWidth()) {
        if (scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scrimHeight)
                    .background(scrimBrush)
                    .alpha(scrimAlpha)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(ThreadTopBarHeight)
                .padding(horizontal = 8.dp)
        ) {
            GlasmioIconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(id = R.string.button_back),
                    tint = colors.content,
                    modifier = Modifier.size(20.dp),
                )
            }

            val forumShape = RoundedCornerShape(ThreadTopActionSize / 2)
            GlassSurface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(ThreadTopActionSize)
                    .widthIn(max = 220.dp)
                    .wrapContentWidth(),
                shape = forumShape,
                onClick = onForumClick,
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
                GlasmioIconButton(
                    onClick = { menuState.expanded = true },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        if (coordinates.isAttached) {
                            menuTriggerBounds = coordinates.boundsInWindow()
                        }
                    },
                ) {
                    Icon(
                        imageVector = (PhosphorIcons["dots-three-vertical"] ?: Icons.Rounded.MoreVert),
                        contentDescription = null,
                        tint = colors.content,
                        modifier = Modifier.size(20.dp),
                    )
                }
                AppMenu(
                    expanded = menuState.expanded,
                    onDismissRequest = { menuScope.dismiss() },
                    menuWidth = 330.dp,
                    anchorBounds = menuTriggerBounds?.let {
                        Rect(left = it.left, top = it.bottom, right = it.right, bottom = it.bottom)
                    },
                ) {
                    ProvideContentColor(color = colors.content) {
                        menuScope.menuContent()
                    }
                }
            }
        }
    }
}

/**
 * 底栏：两个 GlassSurface 默认胶囊。这个 composable 只在 `ThreadPageSkin` 的 overlay 槽里调用，
 * 所以玻璃采到的是页面真实内容。
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
            GlassSurface(
                modifier = Modifier
                    .weight(1f)
                    .height(ThreadBottomBarHeight),
                shape = shape,
                onClick = onClickReply,
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

            GlassSurface(
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
                        icon = if (hasAgreed) (PhosphorIcons["heart-fill"] ?: Icons.Rounded.Favorite) else (PhosphorIcons["heart"] ?: (PhosphorIcons["heart"] ?: Icons.Rounded.FavoriteBorder)),
                        count = agreeNum,
                        color = if (hasAgreed) accent else normal,
                        contentDescription = stringResource(id = R.string.title_agree),
                        onClick = onAgree,
                    )
                    ThreadInfoGroup(
                        icon = (PhosphorIcons["user"] ?: Icons.Rounded.Person),
                        count = null,
                        color = if (isSeeLz) accent else normal,
                        contentDescription = stringResource(id = R.string.title_see_lz),
                        onClick = onSeeLzClick,
                    )
                    ThreadInfoGroup(
                        icon = (PhosphorIcons["arrows-down-up"] ?: Icons.Rounded.SwapVert),
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
