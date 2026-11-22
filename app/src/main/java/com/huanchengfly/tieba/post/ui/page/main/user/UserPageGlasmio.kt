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
import androidx.compose.ui.platform.LocalContext
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
import com.huanchengfly.tieba.post.ui.common.prefs.LocalPrefsUiStyle
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsCard
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsIcon
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsLargeTitle
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsUiStyle
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.PrefsSwitch
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.AboutPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.AppThemePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.HistoryPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadStorePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ToolboxPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.WebViewPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.CuidUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.core.systemGray5
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

/**
 * 「我的」页的 Glasmio 版（`UserPageUiStyle.GLASSMIO`）。
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
 * │ 夜间                  (开关)     │
 * └────────────────────────────────┘
 * 更多
 * ┌────────────────────────────────┐
 * │ 设置                           › │
 * │ 关于                           › │
 * └────────────────────────────────┘
 * ```
 *
 * 账户卡上的统计数字只做摘要；「关注 / 粉丝 / 我的帖子」三个入口在账户卡点进去的
 * 个人主页里（`UserProfilePage`），与本页的账户卡不重复。
 *
 * 头部卡片没有现成的 Glasmio 组件，所以用它的基础件自己拼：
 * - 卡片壳 = `GlasmioGroupedList`（26dp 连续圆角 + `elevatedBackground`）；
 * - 行内 = 普通 `Row` + `GlasmioTheme.colors` / `IosTypography` 令牌；
 * - 右侧箭头 = `SfIcon("chevron.right")`，着色 `colors.accent`（与设置页的行 chevron 同源）。
 *
 * 头部以下的功能列表**完全复用设置页的渲染层**（`PrefsCard` + `GroupHeader` + `TextPref`），
 * 只是这里没有 `PrefsScreen`，所以显式把 `LocalPrefsUiStyle` 置成 GLASSMIO、并给每行补
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
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = UserUiState::isLoading,
        initial = false
    )
    val account by viewModel.uiState.collectPartialAsState(
        prop1 = UserUiState::account,
        initial = null
    )

    // 与旧版同一套「跟随系统夜间」确认逻辑：只有用户开了「跟随系统」才需要二次确认。
    val switchToNightDialogState = rememberDialogState()
    ConfirmDialog(
        dialogState = switchToNightDialogState,
        onConfirm = {},
        onCancel = {
            context.appPreferences.followSystemNight = false
            ThemeUtil.switchNightMode()
        },
        confirmText = stringResource(id = R.string.btn_keep_following),
        cancelText = stringResource(id = R.string.btn_close_following)
    ) {
        Text(text = stringResource(id = R.string.message_dialog_follow_system_night))
    }

    val currentAccount = account
    val nightChecked = ThemeUtil.isNightMode(ThemeUtil.themeState.value)
    val onNightToggle: () -> Unit = {
        if (context.appPreferences.followSystemNight) {
            switchToNightDialogState.show()
        } else {
            ThemeUtil.switchNightMode()
        }
    }

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
        // 设置页的两层前置条件在这里手动补齐（这个页面不是 PrefsScreen 的子树）：
        // GLASSMIO 决定 PrefsCard / TextPref / PrefsLargeTitle 走 Glasmio 渲染层；
        // 图标仍走 Glasmio 自带的 `SfIcon` → Material 解析表（本页没有自定义符号）。
        CompositionLocalProvider(
            LocalPrefsUiStyle provides PrefsUiStyle.GLASSMIO,
        ) {
            val colors = GlasmioTheme.colors
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.groupedBackground)
                    .pullRefresh(pullRefreshState),
            ) {
                MyLazyColumn(
                    // 一级页没有顶栏：列表 viewport 裁在状态栏下方（滚动内容不会钻到状态栏图标下面），
                    // 状态栏那一块仍由外层 Box 的 groupedBackground 铺满。
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        // 状态栏之下留 8dp 直接起大标题（PrefsLargeTitle 自己还有 4dp）。
                        top = 8.dp,
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

                    item(key = "mine-group-general") {
                        UserPrefsGroup(title = stringResource(id = R.string.mine_group_general)) {
                            UserRow(index = 0) {
                                TextPref(
                                    title = stringResource(id = R.string.title_history),
                                    leadingIcon = { PrefsIcon("clock.arrow.circlepath") },
                                    onClick = {
                                        navigator.navigate(HistoryPageDestination)
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
                            UserRow(index = 2) {
                                TextPref(
                                    title = stringResource(id = R.string.my_info_night),
                                    leadingIcon = { PrefsIcon("moon") },
                                    darkenOnDisable = false,
                                    onClick = onNightToggle,
                                ) {
                                    PrefsSwitch(
                                        checked = nightChecked,
                                        onCheckedChange = { onNightToggle() },
                                    )
                                }
                            }
                        }
                    }

                    item(key = "mine-group-more") {
                        UserPrefsGroup(title = stringResource(id = R.string.mine_group_more)) {
                            // 收藏 / 服务中心只有登录后才存在（原版同样如此），放在「更多」里，
                            // 避免挤占「通用」组；三个统计入口已移到个人主页。
                            if (currentAccount != null) {
                                UserRow(index = 0) {
                                    TextPref(
                                        title = stringResource(id = R.string.title_my_collect),
                                        leadingIcon = { PrefsIcon("star") },
                                        onClick = {
                                            navigator.navigate(ThreadStorePageDestination)
                                        },
                                    )
                                }
                                UserRow(index = 1) {
                                    TextPref(
                                        title = stringResource(id = R.string.my_info_service_center),
                                        leadingIcon = { PrefsIcon("questionmark.circle") },
                                        onClick = {
                                            navigator.navigate(
                                                WebViewPageDestination(
                                                    initialUrl = "https://tieba.baidu.com/mo/q/hybrid-main-service/uegServiceCenter?cuid=${CuidUtils.getNewCuid()}&cuid_galaxy2=${CuidUtils.getNewCuid()}&cuid_gid=&timestamp=${System.currentTimeMillis()}&_client_version=12.52.1.0&nohead=1"
                                                )
                                            )
                                        },
                                    )
                                }
                            }
                            UserRow(index = if (currentAccount != null) 2 else 0) {
                                TextPref(
                                    title = stringResource(id = R.string.title_toolbox),
                                    leadingIcon = { PrefsIcon("wrench.and.screwdriver") },
                                    onClick = {
                                        navigator.navigate(ToolboxPageDestination)
                                    },
                                )
                            }
                            UserRow(index = if (currentAccount != null) 3 else 1) {
                                TextPref(
                                    title = stringResource(id = R.string.my_info_settings),
                                    leadingIcon = { PrefsIcon("gearshape") },
                                    onClick = {
                                        navigator.navigate(SettingsPageDestination)
                                    },
                                )
                            }
                            UserRow(index = if (currentAccount != null) 4 else 2) {
                                TextPref(
                                    title = stringResource(id = R.string.my_info_about),
                                    leadingIcon = { PrefsIcon("info.circle") },
                                    onClick = {
                                        navigator.navigate(AboutPageDestination)
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
