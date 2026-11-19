package com.huanchengfly.tieba.post.ui.page.settings

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.SecurityUpdateWarning
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors
import com.huanchengfly.tieba.post.ui.common.prefs.toTextStyle
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.AccountManagePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.BlockSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.CustomSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.HabitSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.MoreSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.OKSignSettingsPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Composable
internal fun LeadingIcon(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides ExtendedTheme.colors.primary) {
        content()
        Spacer(modifier = Modifier.width(56.dp))
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NowAccountItem(
    account: Account?,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavigator.current
    if (account != null) {
        TextPref(
            title = stringResource(id = R.string.title_account_manage),
            summary = stringResource(id = R.string.summary_now_account, account.nameShow ?: account.name),
            enabled = true,
            onClick = { navigator.navigate(AccountManagePageDestination) },
            leadingIcon = {
                LeadingIcon {
                    Avatar(
                        data = StringUtil.getAvatarUrl(account.portrait),
                        size = Sizes.Small,
                        contentDescription = null
                    )
                }
            },
            modifier = modifier,
        )
    } else {
        TextPref(
            title = stringResource(id = R.string.title_account_manage),
            summary = stringResource(id = R.string.summary_not_logged_in),
            enabled = true,
            onClick = { navigator.navigate(LoginPageDestination) },
            leadingIcon = {
                LeadingIcon {
                    AvatarIcon(
                        icon = Icons.Rounded.AccountCircle,
                        size = Sizes.Small,
                        contentDescription = stringResource(id = R.string.title_new_account),
                        color = ExtendedTheme.colors.onChip,
                        backgroundColor = ExtendedTheme.colors.chip,
                    )
                }
            },
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Destination
@Composable
fun SettingsPage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    // 「大标题滚动收进顶栏」：列表滚过标题高度后，顶栏标题从 0 渐变到 1。
    // 明暗跟随 App 自己的 ExtendedTheme（不是系统），所以颜色都取自 glass 令牌。
    val listState = rememberLazyListState()
    val collapseDistancePx = with(LocalDensity.current) { LargeTitleCollapseDistance.toPx() }
    val collapseProgress by remember(collapseDistancePx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
        }
    }
    ProvideNavigator(navigator = navigator) {
        Scaffold(
            backgroundColor = Color.Transparent,
            topBar = {
                TitleCentredToolbar(
                    title = {
                        // 收起后才出现：17sp / w600（FlClash 的导航栏标题）
                        Text(
                            text = stringResource(id = R.string.title_settings),
                            style = GlassTokens.Typography.headline.toTextStyle(glassColors().textPrimary),
                            modifier = Modifier.alpha(collapseProgress),
                            maxLines = 1,
                        )
                    },
                    navigationIcon = {
                        BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                    }
                )
            },
        ) {
            PrefsScreen(
                dataStore = LocalContext.current.dataStore,
                listState = listState,
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize(),
                header = { SettingsLargeTitle() },
            ) {
                prefsItem {
                    NowAccountItem(account = LocalAccount.current)
                }
                // 安全模式（只读）总开关：影响全局入口显隐与所有写操作拦截，
                // 属于最常用的开关之一，故直接放在设置主页顶部，不再埋进二级页面。
                prefsItem {
                    SwitchPref(
                        key = "safeMode",
                        title = stringResource(id = R.string.title_safe_mode),
                        summary = stringResource(id = R.string.summary_safe_mode),
                        defaultChecked = true,
                        onCheckedChange = { checked ->
                            // 关闭安全模式时重置「不再提示」，下次重新开启时仍会提醒一次
                            if (!checked) {
                                context.appPreferences.safeModeDialogDontShow = false
                            }
                            Toast.makeText(context, R.string.toast_restart_app, Toast.LENGTH_SHORT)
                                .show()
                            Handler(Looper.getMainLooper()).postDelayed({
                                restartApp(context)
                            }, 800)
                        },
                    ) {
                        LeadingIcon {
                            AvatarIcon(
                                icon = Icons.Outlined.SecurityUpdateWarning,
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    }
                }
                // 「不再提示」的恢复入口：仅当用户此前点过「不再提示」时才显示，
                // 打开该开关即清掉标记，重新开启进入贴子/发贴页的安全模式提醒。
                if (context.appPreferences.safeModeDialogDontShow) {
                    prefsItem {
                        SwitchPref(
                            key = "safeModeDialogDontShow",
                            title = stringResource(id = R.string.title_dialog_safe_mode_hint_again),
                            summary = stringResource(id = R.string.summary_dialog_safe_mode_hint_again),
                            defaultChecked = false,
                            onCheckedChange = {
                                context.appPreferences.safeModeDialogDontShow = false
                                Toast.makeText(
                                    context,
                                    R.string.toast_safe_mode_hint_restored,
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                        ) {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = Icons.Outlined.Campaign,
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_block_settings),
                        summary = stringResource(id = R.string.summary_block_settings),
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_settings_block),
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        darkenOnDisable = false,
                        onClick = { navigator.navigate(BlockSettingsPageDestination) }
                    )
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_settings_custom),
                        summary = stringResource(id = R.string.summary_settings_custom),
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_brush_black_24dp),
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        darkenOnDisable = false,
                        onClick = { navigator.navigate(CustomSettingsPageDestination) }
                    )
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_settings_read_habit),
                        summary = stringResource(id = R.string.summary_settings_habit),
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_dashboard_customize_black_24),
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        darkenOnDisable = false,
                        onClick = { navigator.navigate(HabitSettingsPageDestination) }
                    )
                }
                // 安全模式下该页所有签到设置均无意义（签到已被拦截），整项隐藏
                if (!context.appPreferences.safeMode) {
                    prefsItem {
                        TextPref(
                            title = stringResource(id = R.string.title_oksign),
                            summary = stringResource(id = R.string.summary_settings_oksign),
                            leadingIcon = {
                                LeadingIcon {
                                    AvatarIcon(
                                        icon = ImageVector.vectorResource(id = R.drawable.ic_rocket_launch_black_24),
                                        size = Sizes.Small,
                                        contentDescription = null,
                                    )
                                }
                            },
                            darkenOnDisable = false,
                            onClick = {
                                navigator.navigate(OKSignSettingsPageDestination)
                            }
                        )
                    }
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_settings_more),
                        summary = stringResource(id = R.string.summary_settings_more),
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_more_horiz_black_24),
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        darkenOnDisable = false,
                        onClick = {
                            navigator.navigate(MoreSettingsPageDestination)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 大标题滚出多少距离后，顶栏标题完全显示。
 * 取 32dp 而不是「标题高度」：手指刚一动顶栏就开始接字，观感更接近 iOS 大标题。
 */
private val LargeTitleCollapseDistance = 32.dp

/**
 * 设置页大标题：FlClash 的 `pageTitle`，24sp / w600，左对齐，跟着列表一起滚走。
 *
 * 左右留白由 [PrefsScreen] 的 `contentPadding`（16dp）给，这里只补上下的小间距，
 * 保证标题与卡片的左边缘对齐。
 */
@Composable
private fun SettingsLargeTitle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
    ) {
        Text(
            text = stringResource(id = R.string.title_settings),
            style = GlassTokens.Typography.pageTitle.toTextStyle(glassColors().textPrimary),
            maxLines = 1,
        )
    }
}

/**
 * 重启应用：先以 NEW_TASK | CLEAR_TASK 启动 Launcher Intent，再结束当前进程。
 * 用于「安全模式」等影响全局入口显隐与拦截链的开关切换后立即生效
 * （拦截器实例在进程启动时确定，见 [com.huanchengfly.tieba.post.capabilities.Capability.currentInterceptor]）。
 */
internal fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    if (intent != null) {
        context.startActivity(intent)
    }
    Runtime.getRuntime().exit(0)
}