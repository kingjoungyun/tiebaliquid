package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassCircleSurface
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassBackdropHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassSurface
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalGlassSamplingSafe
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalLiquidGlassHosted
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassNavigationTint
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuScope
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.core.systemGray5
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
 * 页面里的玻璃控件（普通内容层）仍然按 `LocalGlassSamplingSafe=false` 走
 * `GlassIconButton` 自带的纯色小 host，不会自己采样自己。
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

/** 悬浮顶栏的高度（44dp 按钮 + 上下各 4dp）。列表的顶部 contentPadding 也用它。 */
val ThreadTopBarHeight = 52.dp

/**
 * 帖子页的悬浮顶栏（只在 [ThreadPageSkin] 的 overlay 槽里使用）。
 *
 * 静止时只有左上角一个 44dp 玻璃返回钮，没有标题、没有底色；大标题在列表里，随列表滚走。
 * 滚过大标题后，返回钮右侧淡入一条玻璃胶囊，显示缩小后的标题（17sp w600）——
 * 「大字号 → 小字号 + 毛玻璃」的那条识别特征。
 *
 * 因为这个 composable 画在录制层之外（overlay），[LiquidGlassSurface] /
 * [GlassIconButton] 采样的是整页真实内容，不需要自带纯色 host。
 */
@Composable
fun ThreadGlassTopBar(
    collapsed: Boolean,
    title: String,
    onBack: () -> Unit,
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(52.dp)
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            GlassIconButton(
                onClick = onBack,
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(id = R.string.button_back),
                size = GlassTokens.Spacing.topBarActionSize,
                iconSize = GlassTokens.Spacing.topBarActionIconSize,
            )
            Spacer(modifier = Modifier.width(8.dp))
            AnimatedVisibility(
                visible = collapsed,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(120)),
            ) {
                LiquidGlassSurface(
                    modifier = Modifier
                        .height(36.dp)
                        .wrapContentWidth(),
                    shape = RoundedCornerShape(18.dp),
                    navigationSurfaceColor = glassNavigationTint(),
                    emphasis = GlassEmphasis.Regular,
                    innerGlow = true,
                ) {
                    // 只包住标题本身：不铺满整行，避免被误认为搜索框。
                    Text(
                        text = title,
                        style = IosTypography.headline,
                        color = GlasmioTheme.colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .wrapContentHeight(),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(4.dp))
            // 页面菜单落点：右上角玻璃圆钮，菜单从顶栏向下展开，不会像贴底展开那样被屏幕裁掉。
            ClickMenu(
                menuContent = menuContent,
                menuWidth = 330.dp,
                menuHeight = 300.dp,
                triggerShape = CircleShape,
            ) {
                GlassCircleSurface(size = GlassTokens.Spacing.topBarActionSize) { tint ->
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(id = R.string.btn_more),
                        tint = tint,
                        modifier = Modifier.size(GlassTokens.Spacing.topBarActionIconSize),
                    )
                }
            }
        }
    }
}

/**
 * 列表里的静态大标题（吧名 34/41 w700）。
 *
 * 放在列表第一个 item 的最前面（和第一条楼层同一个 item，不新增 index，
 * 因此页面里所有 `animateScrollToItem(1/2/+n)` 的位置语义保持不变）。
 * 点击等价于旧顶栏里的吧 chip → 吧页。
 */
@Composable
fun ThreadLargeTitle(
    title: String,
    onForumClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = IosTypography.largeTitle,
        color = GlasmioTheme.colors.content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .debounceClickable(onClick = onForumClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

/**
 * 帖子页的玻璃底部操作栏（`ThreadPageUiStyle.GLASMIO`）。
 *
 * 版式：`[头像] [评论一番胶囊 ........] [♡ 92] [⋯]`，整条是 26dp 圆角的玻璃壳，
 * 距屏幕左右 12dp、底部 8dp + 导航栏 inset。
 *
 * 行为完全复用旧栏的回调：
 * - 胶囊 → `onClickReply`（进回复页）；
 * - 心形 → `onAgree`（`ThreadUiIntent.AgreeThread`）；
 * - ⋯ → `onClickMore`（帖子菜单）。
 *
 * 未登录 / 安全模式时旧版会把输入框整块换成 Spacer；这里仍显示只读的「评论一番」胶囊，
 * 点击走同一条 `onClickReply` 路径（回复页自己处理登录），保证输入框在真机上始终可见。
 */
@Composable
fun ThreadGlassBottomBar(
    user: ImmutableHolder<User>,
    onClickReply: () -> Unit,
    onAgree: () -> Unit,
    hasAgreed: Boolean,
    agreeNum: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = GlasmioTheme.colors
    val showAvatar = user.get { is_login } == 1 && !context.appPreferences.safeMode

    Box(
        modifier = modifier
            .fillMaxWidth()
            // 导航栏那一条是透明的：铺上页面底色，列表内容不会从玻璃条下方漏出来。
            .background(colors.groupedBackground)
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp)
    ) {
        ThreadGlassBarSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showAvatar) {
                    Avatar(
                        data = StringUtil.getAvatarUrl(user.get { portrait }),
                        size = 30.dp,
                        contentDescription = user.get { name },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(colors.systemGray5)
                        .debounceClickable(onClick = onClickReply)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = stringResource(id = R.string.tip_reply_thread),
                        style = IosTypography.subheadline,
                        color = colors.secondaryContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                BottomBarAgreeBtn(
                    hasAgreed = hasAgreed,
                    agreeNum = agreeNum,
                    onClick = onAgree,
                    transparentBackground = true,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

/**
 * 底部玻璃壳的采样环境适配。
 *
 * 它画在页面内容层（`sampledContent`）里，读外层的 `LocalGlassBackdrop` 会「自己采样自己」，
 * 所以和 `GlassIconButton` 一样：自带一个只有 tint 纯色的小 host，再用 Glasmio 材质去采它。
 * 观感是真玻璃（有霜化 / 边缘高光），代价只是每次复用一张 60dp 高的纯色纹理。
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
