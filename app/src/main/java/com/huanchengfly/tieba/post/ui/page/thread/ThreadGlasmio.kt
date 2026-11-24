package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.HarmonyTextProvider
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassBackdropHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import com.huanchengfly.tieba.post.ui.widgets.compose.AnchorMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuScope
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideContentColor
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.ui.GlasmioIconButton

/**
 * 帖子详情页的 Glasmio 外壳（`ThreadPageUiStyle.GLASMIO`）。
 *
 * T1 之后帖子页本身已经改用统一的 `GlassPage`；这个薄壳仍被「热榜」页
 * （`HotTopicListPage`，属于长尾 11 页，留给 T5）使用，所以保留。
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
    LiquidGlassTheme(accent = AppTokens.primary) {
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

/**
 * 吧名胶囊 —— [com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPage] 顶栏的 `center` 槽。
 *
 * 原来写在 `ThreadGlassTopBar` 里，参数（44dp 高、胶囊 shape、13sp SemiBold、左右 13dp、
 * 最大 220dp）一个没动，只是搬到了骨架的槽位上。
 */
@Composable
fun ThreadForumCapsule(
    forumName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val shape = RoundedCornerShape(ThreadTopActionSize / 2)
    GlassSurface(
        modifier = modifier
            .height(ThreadTopActionSize)
            .widthIn(max = 220.dp)
            .wrapContentWidth(),
        shape = shape,
        onClick = onClick,
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
}

/**
 * ⋯ 锚点菜单 —— [com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPage] 顶栏的 `actions` 槽。
 *
 * 仍是 **锚点式菜单**：触发器交给 Glasmio，菜单从这颗按钮「长出来」（source transfer）。
 * 骨架只提供槽位与 overlay 层，`AnchorMenu` / `rememberMenuState` 的接法与迁移前完全一致，
 * 所以「菜单从触发器形变」的效果不受影响。
 */
@Composable
fun ThreadTopBarMenu(
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val moreDescription = stringResource(id = R.string.btn_more)
    val menuState = rememberMenuState()
    val menuScope = remember(menuState) { MenuScope(menuState) }

    Box(
        modifier = modifier
            .semantics { contentDescription = moreDescription },
    ) {
        AnchorMenu(
            expanded = menuState.expanded,
            onExpandedChange = { opened ->
                if (opened) menuState.expanded = true else menuScope.dismiss()
            },
            menuWidth = 330.dp,
            anchor = { onClick ->
                GlasmioIconButton(onClick = onClick) {
                    Icon(
                        imageVector = (PhosphorIcons["dots-three-vertical"] ?: Icons.Rounded.MoreVert),
                        contentDescription = null,
                        tint = colors.content,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        ) {
            ProvideContentColor(color = colors.content) {
                menuScope.menuContent()
            }
        }
    }
}

/**
 * 底栏：两个 GlassSurface 默认胶囊。这个 composable 只在 `GlassPage` 的 bottomBar 槽里调用，
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
