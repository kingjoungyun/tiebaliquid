package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.frsPage.ForumInfo
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.common.theme.glass.HarmonyTextProvider
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuScope
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassCircleSurface
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPageHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassOverlayPage
import com.huanchengfly.tieba.post.ui.common.theme.glass.useOverlayTopBarHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTokens
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.glass.render.backdrops.emptyBackdrop
import io.github.neoruaa.glasmio.ui.GlasmioCard
import kotlin.math.max
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import com.huanchengfly.tieba.post.ui.widgets.compose.AnchorMenu
import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

/**
 * 吧页的 Glasmio 皮肤（`ForumPageUiStyle.GLASMIO`）。
 *
 * 版式照「我的」页：
 *
 * ```
 *  (←)                                        (⋯)      ← 悬浮按钮，压在列表上
 *  吧名                                               ← 大标题 34/41 w700（随列表收起）
 *  ┌────────────────────────────────────┐
 *  │ (56) 关注 12.3万 · 帖子 8.2万  [关注/签到] │     ← 26dp 实色 GlasmioCard
 *  │      等级 8 · 距升级还差 120                    │
 *  └────────────────────────────────────┘
 *  最新 | 精品 | 分类                                 ← tab 15sp，选中 w600
 *  ┌───────────────┐
 *  │ 推特式 PostCard │                                ← 16dp 页边 / 10dp 卡距
 *  └───────────────┘
 * ```
 *
 * 这里只放「壳」：皮肤、大标题、吧头卡片、悬浮按钮条。页面逻辑（事件、弹窗、下拉刷新、FAB、
 * 分页器、tab 菜单）仍然在 `ForumPage` 里，两条风格分支共用同一份。
 */
@Composable
fun ForumPageSkin(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    LiquidGlassTheme(accent = AppTokens.primary) {
        HarmonyTextProvider {
            // 页级 backdrop host：本页的玻璃弹层（⋯ 菜单等）要采到「这一页的真实内容」。
            // 页面内容进 sampledContent（被录进 LayerBackdrop）；页面里的玻璃控件（顶栏圆钮）
            // 会自动走「自带纯色小 host」那一支，不会自己采样自己。
            if (useOverlayTopBarHost(GlassOverlayPage.FORUM)) {
                // P2 试点：页面自己在内部用双层 host（列表 → sampledContent、顶栏 → overlayContent），
                // 这里只提供底色与主题，不再多包一层整页录制。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GlasmioTheme.colors.groupedBackground),
                ) {
                    content()
                }
            } else {
                GlassPageHost(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GlasmioTheme.colors.groupedBackground),
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * 大标题（吧名）：34/41 w700，一行省略。
 *
 * 直接吃 `IosTypography.largeTitle`（Glasmio 的 Dynamic Type 表，字族是鸿蒙），
 * 与「我的」页的 `PrefsLargeTitle` 同一档字号 / 行高 / 字重。
 */
@Composable
fun ForumLargeTitle(text: String) {
    Text(
        text = text,
        style = IosTypography.largeTitle,
        color = GlasmioTheme.colors.content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
    )
}

/**
 * 吧头卡片：26dp 实色卡 + 56dp 圆头像 + 一行统计 + （已关注时）等级进度 + 关注/签到按钮。
 *
 * 整卡可点 → 吧资料页（沿用旧版 `onOpenForumInfo`）。
 * `GlasmioCard` 的 `backdrop` 显式给 `emptyBackdrop()`：这个页面不在 `GlasmioBackdropHost` 里，
 * 让默认值去读 `LocalGlassBackdrop` 会直接抛异常（同 `PostCard` 的处理）。
 */
@Composable
fun GlasmioForumHeaderCard(
    forumInfoImmutableHolder: ImmutableHolder<ForumInfo>,
    onOpenForumInfo: () -> Unit,
    onBtnClick: () -> Unit,
    showAction: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val forum = remember(forumInfoImmutableHolder) { forumInfoImmutableHolder.get() }
    val backdrop = remember { emptyBackdrop() }

    GlasmioCard(
        modifier = modifier.fillMaxWidth(),
        backdrop = backdrop,
        onClick = onOpenForumInfo,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                data = forum.avatar,
                size = 56.dp,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = forumStatsText(forum, colors.secondaryContent, colors.content),
                    style = IosTypography.footnote,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (forum.is_like == 1) {
                    Spacer(modifier = Modifier.height(7.dp))
                    ForumLevelProgress(forum = forum)
                }
            }
            if (showAction) {
                Spacer(modifier = Modifier.width(10.dp))
                ForumActionPill(
                    text = forumActionLabel(forum),
                    enabled = (forum.is_like != 1) ||
                        (forum.sign_in_info?.user_info?.is_sign_in != 1),
                    onClick = onBtnClick,
                )
            }
        }
    }
}

/** 「关注 12.3万 · 回贴 8.2万」：标签走次级色、数字走主内容色 semibold（与「我的」页统计行同构）。 */
@Composable
private fun forumStatsText(
    forum: ForumInfo,
    labelColor: Color,
    valueColor: Color,
): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = labelColor)) { append(stringResource(id = R.string.text_stat_follow)) }
    append(" ")
    withStyle(SpanStyle(color = valueColor, fontWeight = FontWeight.SemiBold)) {
        append(forum.member_num.getShortNumString())
    }
    withStyle(SpanStyle(color = labelColor)) { append("  ·  ") }
    withStyle(SpanStyle(color = labelColor)) { append(stringResource(id = R.string.title_stat_posts_num)) }
    append(" ")
    withStyle(SpanStyle(color = valueColor, fontWeight = FontWeight.SemiBold)) {
        append(forum.post_num.getShortNumString())
    }
}

/** 已关注时的一行：4dp 经验进度条 + 「Lv.8 · 某某」小字。 */
@Composable
private fun ForumLevelProgress(forum: ForumInfo) {
    val colors = GlasmioTheme.colors
    val fraction = (
        forum.cur_score.toFloat() / max(1f, forum.levelup_score.toFloat())
        ).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(100))
                .background(colors.accent.copy(alpha = 0.22f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(100))
                    .background(colors.accent),
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = stringResource(
                id = R.string.tip_forum_header_liked,
                forum.user_level.toString(),
                forum.level_name,
            ),
            style = IosTypography.caption2,
            color = colors.secondaryContent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 关注 / 已签到 / 签到 三种文案（沿用旧版逻辑，只是换成 iOS 小胶囊）。 */
@Composable
private fun forumActionLabel(forum: ForumInfo): String {
    return when {
        forum.is_like != 1 -> stringResource(R.string.button_follow)
        forum.sign_in_info?.user_info?.is_sign_in == 1 -> stringResource(
            R.string.button_signed_in,
            forum.sign_in_info.user_info.cont_sign_num,
        )

        else -> stringResource(R.string.button_sign_in)
    }
}

@Composable
private fun ForumActionPill(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(if (enabled) colors.accent else colors.accent.copy(alpha = 0.35f))
            .debounceClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTokens.onAccent,
            maxLines = 1,
        )
    }
}

/**
 * 吧页的「顶栏」：透明底 + 状态栏内边距 + 48dp 高的一行按钮（返回 / 吧内搜索 / ⋯）。
 *
 * 没有标题、没有背景、没有分割线——视觉上就是「无顶栏 + 两个悬浮按钮」，
 * 但仍占 48dp 高度，所以列表不会钻到按钮下面（也就不用做遮罩/避让）。
 *
 * 按钮统一走 [GlassIconButton] / [GlassCircleSurface]（全 App 一套悬浮玻璃钮）：
 * 本页内容在 Activity 级 host 的录制层里（采样不安全），组件会自动为按钮自带一个纯色小 host，
 * 所以这里是**真 Glasmio 材质**，不再需要「实色 + 1dp 描边」的临时方案。
 */
@Composable
fun ForumTopBar(
    showSearch: Boolean,
    menuContent: @Composable MenuScope.() -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(
            onClick = onBack,
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(id = R.string.button_back),
            size = GlassTokens.Spacing.topBarActionSize,
            iconSize = GlassTokens.Spacing.topBarActionIconSize,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (showSearch) {
            GlassIconButton(
                onClick = onSearch,
                icon = PhosphorIcons["magnifying-glass"] ?: Icons.Rounded.Search,
                contentDescription = stringResource(id = R.string.btn_search_in_forum),
                size = GlassTokens.Spacing.topBarActionSize,
                iconSize = GlassTokens.Spacing.topBarActionIconSize,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        // B：锚点式菜单 —— `GlassCircleSurface` 外观不变，外面套一层可点击 Box 作为 Glasmio 的
        // anchor，由它测量并做「从小圆钮长成菜单」的 source transfer。
        var forumMenuExpanded by remember { mutableStateOf(false) }
        AnchorMenu(
            expanded = forumMenuExpanded,
            onExpandedChange = { forumMenuExpanded = it },
            menuWidth = 240.dp,
            anchor = { onClick ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onClick),
                ) {
                    GlassCircleSurface(
                        size = GlassTokens.Spacing.topBarActionSize,
                    ) { tint ->
                        Icon(
                            imageVector = (PhosphorIcons["dots-three-vertical"] ?: Icons.Rounded.MoreVert),
                            contentDescription = stringResource(id = R.string.btn_more),
                            tint = tint,
                            modifier = Modifier.size(GlassTokens.Spacing.topBarActionIconSize),
                        )
                    }
                }
            },
        ) {
            // MenuScope.dismiss() 既要清自己的 expanded，也要关掉我们的 AnchorMenu。
            val dismissState = rememberMenuState()
            val scope = remember(dismissState) {
                MenuScope(dismissState, onDismiss = { forumMenuExpanded = false })
            }
            scope.menuContent()
        }
    }
}

// ForumCircleButton / ForumCircleSurface 已删除：统一走 ui/common/theme/glass/GlassIconButton.kt
