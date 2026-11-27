package com.huanchengfly.tieba.post.ui.page.main.user

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.common.prefs.GroupHeader
import com.huanchengfly.tieba.post.ui.common.prefs.LocalPrefsRowIndex
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsCard
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsIcon
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsLargeTitle
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.AppThemePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.HistoryPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadStorePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ToolboxPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserLikeForumListPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserPostListPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.utils.StringUtil
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.core.systemGray5
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues

/**
 * 「我的」页的玻璃版。
 *
 * 版式对齐 iOS「设置」首页的账户区：
 *
 * ```
 * 我的                                     ← 大标题 34sp / w700，静态，随列表滚走
 * ┌────────────────────────────────┐
 * │ (60dp 头像)  名字 22sp          › │     ← 一张分组卡片（GlasmioGroupedList）
 * │              关注 12 · 粉丝 34 · 回帖 56 │
 * └────────────────────────────────┘
 * 通用
 * ┌────────────────────────────────┐
 * │ 浏览记录                       › │
 * │ 主题选择                       › │
 * └────────────────────────────────┘
 * 更多
 * ┌────────────────────────────────┐
 * │ 设置                           › │
 * └────────────────────────────────┘
 * ```
 *
 * 深色模式只在「主题选择」页切（含「跟随系统」二次确认）；「关于」只在「设置 → 更多」里，
 * 本页不再重复放这两个入口。
 * 账户卡上的统计数字只做摘要；「关注 / 粉丝 / 我的帖子」三个入口在账户卡点进去的
 * 个人主页里（`UserProfilePage`），与本页的账户卡不重复。
 *
 * 头部卡片没有现成的 Glasmio 组件，所以用它的基础件自己拼：
 * - 卡片壳 = `GlasmioGroupedList`（26dp 连续圆角 + `elevatedBackground`）；
 * - 行内 = 普通 `Row` + `GlasmioTheme.colors` / `IosTypography` 令牌；
 * - 右侧箭头 = `SfIcon("chevron.right")`，着色 `colors.accent`（与设置页的行 chevron 同源）。
 *
 * 头部以下的功能列表**完全复用设置页的渲染层**（`PrefsCard` + `GroupHeader` + `TextPref`），
 * 只是这里没有 `PrefsScreen`，所以直接给每行补
 * `LocalPrefsRowIndex`（Glasmio 行自己画分隔线，且会吞掉组内第一行那条线）。
 *
 * 视觉约束：
 * - 页面底色 = `GlasmioTheme.colors.groupedBackground`（深色纯黑、浅色 #F2F2F7），卡片 = #1C1C1E / 白；
 * - 图标一律 `PrefsIcon`（`SfIcon`，23dp、accent），随 `AppTokens.primary`
 *   注入的 accent 变（[LiquidGlassTheme] 负责注入）；
 * - 不加装饰性灰色副标题；状态/数值（统计数字、未登录…）只放行尾或头部那一行小字。
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun GlasmioUserPage(viewModel: UserViewModel) {
    val navigator = LocalNavigator.current
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = UserUiState::isLoading,
        initial = false
    )
    val account by viewModel.uiState.collectPartialAsState(
        prop1 = UserUiState::account,
        initial = null
    )

    // 夜间开关与「跟随系统」确认逻辑不在本页：深色只从「主题选择」页切，
    // 确认逻辑也随开关一起搬到了 AppThemePage。

    val currentAccount = account

    // 统计数字用与旧版一致的入场动画；未登录时保持在 0。
    val concernNum by animateIntAsState(
        targetValue = currentAccount?.concernNum?.toIntOrNull() ?: 0,
        label = "mineConcern",
    )
    val fansNum by animateIntAsState(
        targetValue = currentAccount?.fansNum?.toIntOrNull() ?: 0,
        label = "mineFans",
    )
    val postsNum by animateIntAsState(
        targetValue = currentAccount?.postNum?.toIntOrNull() ?: 0,
        label = "minePosts",
    )

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.send(UserUiIntent.Refresh) },
    )

    LiquidGlassTheme(accent = AppTokens.primary) {
        // 设置页的前置条件在这里手动补齐（这个页面不是 PrefsScreen 的子树）：
        // PrefsCard / TextPref / PrefsLargeTitle 固定走玻璃渲染层；
        // 图标仍走 Glasmio 自带的 `SfIcon` → Material 解析表（本页没有自定义符号）。
        run {
            val colors = GlasmioTheme.colors
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.groupedBackground)
                    .pullRefresh(pullRefreshState),
            ) {
                MyLazyColumn(
                    // 与帖子页（GlassPage）一致：列表铺满整屏，内容可以滚到状态栏下方，
                    // 只把「状态栏高度」塞进 contentPadding，让首项起始位置在状态栏之下。
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        // 状态栏之下留 8dp 直接起大标题（PrefsLargeTitle 自己还有 4dp）。
                        top = statusBarTop + 8.dp,
                        // 底部再叠加 LocalMainTabBottomInset（MyLazyColumn 自动加），让最后一行能滚到 dock 上方。
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "mine-title") {
                        PrefsLargeTitle(text = stringResource(id = R.string.mine_title))
                    }

                    item(key = "mine-account-card") {
                        if (isLoading && currentAccount == null) {
                            AccountHeaderSkeleton()
                        } else {
                            AccountHeaderCard(
                                account = currentAccount,
                                concernNum = concernNum,
                                fansNum = fansNum,
                                postsNum = postsNum,
                                onClick = {
                                    val acc = currentAccount
                                    if (acc != null) {
                                        navigator.navigate(
                                            UserProfilePageDestination(acc.uid.toLong())
                                        )
                                    } else {
                                        navigator.navigate(LoginPageDestination)
                                    }
                                },
                            )
                        }
                    }

                    item(key = "mine-group-content") {
                        UserPrefsGroup(title = stringResource(id = R.string.title_toolbox_group_content)) {
                            val selfUid = currentAccount?.uid?.toLongOrNull() ?: 0L
                            val authorName = currentAccount?.name.orEmpty()
                            val authorNameShow = currentAccount?.nameShow.orEmpty()
                            val authorPortrait = currentAccount?.portrait.orEmpty()
                            if (currentAccount != null) {
                                UserRow(index = 0) {
                                    TextPref(
                                        title = stringResource(id = R.string.title_toolbox_threads),
                                        leadingIcon = { PrefsIcon("square.and.pencil") },
                                        onClick = {
                                            navigator.navigate(
                                                UserPostListPageDestination(
                                                    uid = selfUid,
                                                    isThread = true,
                                                    authorName = authorName,
                                                    authorNameShow = authorNameShow,
                                                    authorPortrait = authorPortrait,
                                                )
                                            )
                                        },
                                    )
                                }
                                UserRow(index = 1) {
                                    TextPref(
                                        title = stringResource(id = R.string.title_toolbox_posts),
                                        leadingIcon = { PrefsIcon("bubble.left.and.bubble.right") },
                                        onClick = {
                                            navigator.navigate(
                                                UserPostListPageDestination(
                                                    uid = selfUid,
                                                    isThread = false,
                                                    authorName = authorName,
                                                    authorNameShow = authorNameShow,
                                                    authorPortrait = authorPortrait,
                                                )
                                            )
                                        },
                                    )
                                }
                            }
                            if (currentAccount != null) {
                                UserRow(index = 2) {
                                    TextPref(
                                        title = stringResource(id = R.string.title_my_collect),
                                        leadingIcon = { PrefsIcon("star") },
                                        onClick = {
                                            navigator.navigate(ThreadStorePageDestination)
                                        },
                                    )
                                }
                                UserRow(index = 3) {
                                    TextPref(
                                        title = stringResource(id = R.string.title_toolbox_like_forum),
                                        leadingIcon = { PrefsIcon("rectangle.stack") },
                                        onClick = {
                                            navigator.navigate(
                                                UserLikeForumListPageDestination(uid = selfUid)
                                            )
                                        },
                                    )
                                }
                            }
                            UserRow(index = if (currentAccount != null) 4 else 0) {
                                TextPref(
                                    title = stringResource(id = R.string.title_history),
                                    leadingIcon = { PrefsIcon("clock.arrow.circlepath") },
                                    onClick = {
                                        navigator.navigate(HistoryPageDestination)
                                    },
                                )
                            }
                        }
                    }

                    item(key = "mine-group-tools") {
                        UserPrefsGroup(title = stringResource(id = R.string.mine_group_tools)) {
                            UserRow(index = 0) {
                                TextPref(
                                    title = stringResource(id = R.string.title_toolbox),
                                    leadingIcon = { PrefsIcon("wrench.and.screwdriver") },
                                    onClick = {
                                        navigator.navigate(ToolboxPageDestination)
                                    },
                                )
                            }
                        }
                    }

                    item(key = "mine-group-app") {
                        UserPrefsGroup(title = stringResource(id = R.string.title_toolbox_group_app)) {
                            UserRow(index = 0) {
                                TextPref(
                                    title = stringResource(id = R.string.my_info_settings),
                                    leadingIcon = { PrefsIcon("gearshape") },
                                    onClick = {
                                        navigator.navigate(SettingsPageDestination)
                                    },
                                )
                            }
                            UserRow(index = 1) {
                                TextPref(
                                    title = stringResource(id = R.string.title_theme),
                                    leadingIcon = { PrefsIcon("paintpalette") },
                                    onClick = {
                                        navigator.navigate(AppThemePageDestination)
                                    },
                                )
                            }
                        }
                    }
                }

                PullRefreshIndicator(
                    refreshing = isLoading,
                    state = pullRefreshState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding(),
                    backgroundColor = colors.elevatedBackground,
                    contentColor = colors.accent,
                )
            }
        }
    }
}

/**
 * 头部卡片：60dp 圆头像 + 名字（22sp semibold）+ 一行统计 / 「点击登录」+ 右侧 chevron。
 *
 * 整卡可点：已登录 → 个人主页；未登录 → 登录页。统计数字与个人主页「账号」组里的
 * 行尾数字同源，都是 [Account] 上由 `UserViewModel` 拉回来的真实字段
 * （`concernNum` / `fansNum` / `postNum`）。
 */
@Composable
private fun AccountHeaderCard(
    account: Account?,
    concernNum: Int,
    fansNum: Int,
    postsNum: Int,
    onClick: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val name = account?.let { acc ->
        acc.nameShow?.takeIf { it.isNotBlank() } ?: acc.name
    }
    GlasmioGroupedList {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderAvatar(account = account)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name ?: stringResource(id = R.string.status_not_logged_in),
                    style = IosTypography.title2.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                if (account != null) {
                    HeaderStatsLine(
                        concernNum = concernNum,
                        fansNum = fansNum,
                        postsNum = postsNum,
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.mine_click_to_login),
                        style = IosTypography.footnote,
                        color = colors.secondaryContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            // 与设置页的行 chevron 完全同源：22dp、accent 着色、RTL 下镜像。
            SfIcon(
                systemName = "chevron.right",
                contentDescription = null,
                size = 22.dp,
                mirrored = true,
                tint = colors.accent,
            )
        }
    }
}

/** 60dp 圆头像；未登录（或账号没有 portrait 字段）用 `person.fill` 占位。 */
@Composable
private fun HeaderAvatar(account: Account?) {
    val colors = GlasmioTheme.colors
    val portrait = account?.portrait?.takeIf { it.isNotBlank() }
    if (portrait != null) {
        Avatar(
            data = StringUtil.getAvatarUrl(portrait),
            size = 60.dp,
            contentDescription = stringResource(id = R.string.desc_user_avatar),
        )
    } else {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(colors.systemGray5),
            contentAlignment = Alignment.Center,
        ) {
            SfIcon(
                systemName = "person.fill",
                contentDescription = null,
                size = 30.dp,
                tint = colors.secondaryContent,
            )
        }
    }
}

/**
 * 名字下面那一行：「关注 12 · 粉丝 34 · 回帖 56」。
 *
 * 标签用次级色、数字用主内容色（semibold），分隔点用次级色 —— 既保持一行 13sp 的克制，
 * 又让三个数字成为视觉锚点。整行单行省略，不换行。
 */
@Composable
private fun HeaderStatsLine(concernNum: Int, fansNum: Int, postsNum: Int) {
    val colors = GlasmioTheme.colors
    val followLabel = stringResource(id = R.string.text_stat_follow)
    val fansLabel = stringResource(id = R.string.text_stat_fans)
    val postsLabel = stringResource(id = R.string.mine_stat_replies)
    val labelColor = colors.secondaryContent
    val valueColor = colors.content
    val text = buildAnnotatedString {
        appendStatSegment(followLabel, concernNum, labelColor, valueColor, first = true)
        appendStatSegment(fansLabel, fansNum, labelColor, valueColor, first = false)
        appendStatSegment(postsLabel, postsNum, labelColor, valueColor, first = false)
    }
    Text(
        text = text,
        style = IosTypography.footnote,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun AnnotatedString.Builder.appendStatSegment(
    label: String,
    value: Int,
    labelColor: Color,
    valueColor: Color,
    first: Boolean,
) {
    if (!first) {
        withStyle(SpanStyle(color = labelColor)) { append(" · ") }
    }
    withStyle(SpanStyle(color = labelColor)) { append(label) }
    append(" ")
    withStyle(SpanStyle(color = valueColor, fontWeight = FontWeight.SemiBold)) {
        append(value.toString())
    }
}

/**
 * 加载中的头部骨架：与真卡片同尺寸（60dp 圆 + 两行文字 + chevron），
 * 避免「先弹出一张未登录卡、再跳成已登录」的闪变。
 */
@Composable
private fun AccountHeaderSkeleton() {
    val colors = GlasmioTheme.colors
    GlasmioGroupedList {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(colors.systemGray5)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .width(128.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.systemGray5)
                )
                Spacer(modifier = Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .height(13.dp)
                        .width(190.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.systemGray5)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            SfIcon(
                systemName = "chevron.right",
                contentDescription = null,
                size = 22.dp,
                mirrored = true,
                tint = colors.accent.copy(alpha = 0.4f),
            )
        }
    }
}

/**
 * 一张分组卡片 = 组标题 + `GlasmioGroupedList`。
 *
 * 直接复用设置页的 [PrefsCard]：GLASSMIO 路径下它负责 26dp 连续圆角、卡片底色、
 * 组尾 footnote（本页没有 footnote），以及组标题与卡片之间的间距。
 */
@Composable
private fun UserPrefsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    PrefsCard(
        header = { GroupHeader(title = title) },
        content = content,
    )
}

/**
 * 组内行的下标注入。
 *
 * `GlasmioGroupedList` 由**行自己**画上边界分隔线，并吞掉「组的第一行」那条线；
 * 没有 `PrefsScreen` 的 DSL 包办时，这里手动给每行补一个 0 起的下标。
 */
@Composable
private fun UserRow(index: Int, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPrefsRowIndex provides index) {
        content()
    }
}
