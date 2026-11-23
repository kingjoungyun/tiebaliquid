package com.huanchengfly.tieba.post.ui.page.settings

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.SecurityUpdateWarning
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsLargeTitle
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsIcon
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
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
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

@Composable
internal fun LeadingIcon(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides AppTokens.primary) {
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
            // 已登录账号名是「状态」→ 行尾右侧，不再占行内副标题
            trailingValue = account.nameShow ?: account.name,
            enabled = true,
            onClick = { navigator.navigate(AccountManagePageDestination) },
            leadingIcon = {
                // 唯一保留「图片」的图标位：已登录时显示真实头像（不是图标）
                Avatar(
                    data = StringUtil.getAvatarUrl(account.portrait),
                    size = Sizes.Small,
                    contentDescription = null
                )
            },
            modifier = modifier,
        )
    } else {
        TextPref(
            title = stringResource(id = R.string.title_account_manage),
            trailingValue = stringResource(id = R.string.status_not_logged_in),
            enabled = true,
            onClick = { navigator.navigate(LoginPageDestination) },
            // 未登录时没有头像可用 → 用 Glasmio 的人形图标（不再画灰底圆形占位）
            leadingIcon = { PrefsIcon("person.fill") },
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
    // 分组标题文案要在这里（composable 上下文）取：DSL 的 content 不是 composable lambda，
    // 在里面调 stringResource 编译不过。
    val groupAccount = stringResource(id = R.string.settings_group_account)
    val groupSecurity = stringResource(id = R.string.settings_group_security)
    val groupAppearance = stringResource(id = R.string.settings_group_appearance)
    // 安全模式的长解释（5 行）不再塞在行内，改作组尾 footnote：行内只剩「标题 + 开关」。
    val safeModeSummary = stringResource(id = R.string.summary_safe_mode)
    ProvideNavigator(navigator = navigator) {
        // 「二级页范式」：不再有 M2 顶栏（白底会和 groupedBackground 撞成白带），
        // 页面从状态栏下方直接起排，返回是左上角浮起的玻璃圆钮，大标题静态。
        PrefsScreen(
            dataStore = LocalContext.current.dataStore,
            modifier = Modifier.fillMaxSize(),
            onBack = { navigator.navigateUp() },
            title = stringResource(id = R.string.title_settings),
            header = {
                PrefsLargeTitle(text = stringResource(id = R.string.title_settings))
            },
        ) {
            // 三张分组卡片：账号 / 安全 / 外观与通用（组间距 8dp 由 PrefsScreen 负责）
            prefsGroup(title = groupAccount) {
                prefsItem {
                    NowAccountItem(account = LocalAccount.current)
                }
            }
            prefsGroup(title = groupSecurity, footer = safeModeSummary) {
                // 安全模式（只读）总开关：影响全局入口显隐与所有写操作拦截，
                // 属于最常用的开关之一，故直接放在设置主页，不再埋进二级页面。
                prefsItem {
                    SwitchPref(
                        key = "safeMode",
                        title = stringResource(id = R.string.title_safe_mode),
                        // 解释性长文案：组尾 footnote（prefsGroup 的 footer），行内不放
                        defaultChecked = true,
                        onCheckedChange = { checked ->
                            // 关闭安全模式时重置「不再提示」，下次重新开启时仍会提醒一次
                            if (!checked) {
                                context.appPreferences.safeModeDialogDontShow = false
                            }
                            Toast.makeText(
                                context,
                                R.string.toast_restart_app,
                                Toast.LENGTH_SHORT
                            ).show()
                            Handler(Looper.getMainLooper()).postDelayed({
                                restartApp(context)
                            }, 800)
                        },
                    ) {
                        PrefsIcon("lock.fill")
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
                            PrefsIcon("bell.fill")
                        }
                    }
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_block_settings),
                        leadingIcon = {
                            PrefsIcon("eye.slash")
                        },
                        darkenOnDisable = false,
                        onClick = { navigator.navigate(BlockSettingsPageDestination) }
                    )
                }
            }
            prefsGroup(title = groupAppearance) {
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_settings_custom),
                        leadingIcon = {
                            PrefsIcon("paintpalette")
                        },
                        darkenOnDisable = false,
                        onClick = { navigator.navigate(CustomSettingsPageDestination) }
                    )
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_settings_read_habit),
                        leadingIcon = {
                            PrefsIcon("slider.horizontal.3")
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
                            leadingIcon = {
                                PrefsIcon("checkmark.circle")
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
                        leadingIcon = {
                            PrefsIcon("ellipsis")
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

