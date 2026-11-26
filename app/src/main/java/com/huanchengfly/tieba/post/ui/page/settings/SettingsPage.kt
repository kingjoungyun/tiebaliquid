package com.huanchengfly.tieba.post.ui.page.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsLargeTitle
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsIcon
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.dependNot
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.ListPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.ListPrefInt
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.AboutPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.AccountManagePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.AppThemePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.BlockSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.OKSignSettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.WebViewPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.activities.AppFontSizeActivity
import com.huanchengfly.tieba.post.utils.AppIconUtil
import com.huanchengfly.tieba.post.utils.CuidUtils
import com.huanchengfly.tieba.post.utils.ImageCacheUtil
import com.huanchengfly.tieba.post.utils.LauncherIcons
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.utils.isPhotoPickerAvailable
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import kotlinx.coroutines.launch
import kotlin.concurrent.thread
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import com.huanchengfly.tieba.post.utils.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

/**
 * 设置主页（IA 六分组，PLAN §21.3）。
 *
 * 组织方式：账号 / 安全与隐私 / 外观 / 阅读 / 通用 / 关于。
 * 原来分散在「个性化」「使用习惯」「更多」三个二级页的偏好项已全部下沉到本页，
 * **偏好 key 保持不变**（用户已保存的设置不受影响）；三个二级页已删除。
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Destination
@Composable
fun SettingsPage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var cacheSize by remember { mutableStateOf("0.0B") }
    LaunchedEffect(Unit) {
        thread {
            cacheSize = ImageCacheUtil.getCacheSize(context)
        }
    }

    // 分组标题文案要在这里（composable 上下文）取：DSL 的 content 不是 composable lambda，
    // 在里面调 stringResource 编译不过。
    val groupAccount = stringResource(id = R.string.settings_group_account)
    val groupSecurity = stringResource(id = R.string.settings_group_security)
    val groupAppearance = stringResource(id = R.string.settings_group_appearance)
    val groupRead = stringResource(id = R.string.settings_group_read)
    val groupGeneral = stringResource(id = R.string.mine_group_general)
    val groupAbout = stringResource(id = R.string.settings_group_about)
    // 安全模式的长解释（5 行）不再塞在行内，改作组尾 footnote：行内只剩「标题 + 开关」。
    val safeModeSummary = stringResource(id = R.string.summary_safe_mode)

    // 检查更新（§21.5）：点一次读 GitHub Releases，有新版弹提示，否则 snackbar。
    var checkingUpdate by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf<UpdateChecker.Release?>(null) }

    ProvideNavigator(navigator = navigator) {
        PrefsScreen(
            dataStore = LocalContext.current.dataStore,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize(),
            onBack = { navigator.navigateUp() },
            title = stringResource(id = R.string.title_settings),
            header = {
                PrefsLargeTitle(text = stringResource(id = R.string.title_settings))
            },
        ) {
            // ── 1. 账号 ─────────────────────────────────────────────
            prefsGroup(title = groupAccount) {
                prefsItem {
                    NowAccountItem(account = LocalAccount.current)
                }
                // 安全模式下签到设置无意义（签到已被拦截），整项隐藏
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
            }

            // ── 2. 安全与隐私 ────────────────────────────────────────
            prefsGroup(title = groupSecurity, footer = safeModeSummary) {
                // 安全模式（只读）总开关：影响全局入口显隐与所有写操作拦截
                prefsItem {
                    SwitchPref(
                        key = "safeMode",
                        title = stringResource(id = R.string.title_safe_mode),
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
                // 「不再提示」的恢复入口：仅当用户此前点过「不再提示」时才显示
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
                // 从「使用习惯」移入
                prefsItem {
                    SwitchPref(
                        key = "incognitoMode",
                        title = stringResource(id = R.string.settings_incognito_mode),
                        defaultChecked = false
                    ) {
                        PrefsIcon("eye.slash")
                    }
                }
            }

            // ── 3. 外观 ─────────────────────────────────────────────
            prefsGroup(title = groupAppearance) {
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_theme),
                        leadingIcon = {
                            PrefsIcon("paintpalette")
                        },
                        darkenOnDisable = false,
                        onClick = { navigator.navigate(AppThemePageDestination) }
                    )
                }
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_custom_font_size),
                        leadingIcon = {
                            PrefsIcon("textformat.size")
                        },
                        onClick = {
                            context.goToActivity<AppFontSizeActivity>()
                        }
                    )
                }
                prefsItem {
                    ListPref(
                        key = "app_icon",
                        title = stringResource(id = R.string.settings_app_icon),
                        defaultValue = LauncherIcons.DEFAULT_ICON,
                        leadingIcon = {
                            PrefsIcon("rectangle.3.group")
                        },
                        entries = mapOf(
                            LauncherIcons.NEW_ICON to "新图标",
                            LauncherIcons.NEW_ICON_INVERT to "新图标（反色）",
                            LauncherIcons.OLD_ICON to "旧图标",
                        ),
                        onValueChange = { AppIconUtil.setIcon(icon = it) },
                        useSelectedAsSummary = true,
                    )
                }
                prefsItem {
                    SwitchPref(
                        key = "listSingle",
                        title = stringResource(id = R.string.settings_forum_single),
                        defaultChecked = false,
                    ) {
                        PrefsIcon("list.bullet")
                    }
                }
                prefsItem {
                    SwitchPref(
                        key = "liftUpBottomBar",
                        title = stringResource(id = R.string.title_lift_up_bottom_bar),
                        defaultChecked = true,
                        leadingIcon = {
                            PrefsIcon("arrow.up.left.and.arrow.down.right")
                        },
                        summary = stringResource(id = R.string.summary_lift_up_bottom_bar),
                    )
                }
                prefsItem {
                    SwitchPref(
                        key = "status_bar_darker",
                        title = stringResource(id = R.string.title_settings_status_bar_darker),
                        summary = stringResource(id = R.string.summary_settings_status_bar_darker),
                        defaultChecked = true,
                    ) {
                        PrefsIcon("rectangle.topthird.inset.filled")
                    }
                }
                prefsItem {
                    SwitchPref(
                        key = "show_both_username_and_nickname",
                        title = stringResource(id = R.string.title_show_both_username_and_nickname),
                        defaultChecked = false,
                    ) {
                        PrefsIcon("character")
                    }
                }
            }

            // ── 4. 阅读 ─────────────────────────────────────────────
            prefsGroup(title = groupRead) {
                prefsItem {
                    ListPref(
                        key = "default_sort_type",
                        title = stringResource(id = R.string.title_settings_default_sort_type),
                        entries = mapOf(
                            "0" to stringResource(id = R.string.title_sort_by_reply),
                            "1" to stringResource(id = R.string.title_sort_by_send),
                        ),
                        useSelectedAsSummary = true,
                        defaultValue = "0",
                        leadingIcon = {
                            PrefsIcon("arrow.up.arrow.down")
                        },
                    )
                }
                prefsItem {
                    SwitchPref(
                        key = "hideMedia",
                        title = stringResource(id = R.string.title_hide_media),
                        defaultChecked = false
                    ) {
                        PrefsIcon("chevron.down.circle")
                    }
                }
                prefsItem {
                    SwitchPref(
                        key = "imageDarkenWhenNightMode",
                        title = stringResource(id = R.string.settings_image_darken_when_night_mode),
                        defaultChecked = true,
                    ) {
                        PrefsIcon("moon")
                    }
                }
                prefsItem {
                    ListPref(
                        key = "image_load_type",
                        title = stringResource(id = R.string.title_settings_image_load_type),
                        entries = mapOf(
                            "0" to stringResource(id = R.string.title_image_load_type_smart_origin),
                            "1" to stringResource(id = R.string.title_image_load_type_smart_load),
                            "2" to stringResource(id = R.string.title_image_load_type_all_origin),
                            "3" to stringResource(id = R.string.title_image_load_type_all_no)
                        ),
                        useSelectedAsSummary = true,
                        defaultValue = "0",
                        leadingIcon = {
                            PrefsIcon("photo")
                        },
                    )
                }
                prefsItem {
                    ListPref(
                        key = "pic_watermark_type",
                        title = stringResource(id = R.string.title_settings_image_watermark),
                        entries = mapOf(
                            "0" to stringResource(id = R.string.title_image_watermark_none),
                            "1" to stringResource(id = R.string.title_image_watermark_user_name),
                            "2" to stringResource(id = R.string.title_image_watermark_forum_name)
                        ),
                        useSelectedAsSummary = true,
                        defaultValue = "2",
                        leadingIcon = {
                            PrefsIcon("textformat")
                        },
                    )
                }
                prefsItem {
                    SwitchPref(
                        key = "collect_thread_see_lz",
                        title = stringResource(id = R.string.settings_collect_thread_see_lz),
                        defaultChecked = true,
                        leadingIcon = {
                            PrefsIcon("person.fill")
                        },
                        summaryOn = stringResource(id = R.string.tip_collect_thread_see_lz_on),
                        summaryOff = stringResource(id = R.string.tip_collect_thread_see_lz)
                    )
                }
                prefsItem {
                    SwitchPref(
                        key = "collect_thread_desc_sort",
                        title = stringResource(id = R.string.settings_collect_thread_desc_sort),
                        defaultChecked = false,
                        leadingIcon = {
                            PrefsIcon("arrow.up.arrow.down")
                        },
                        summaryOn = stringResource(id = R.string.tip_collect_thread_desc_sort_on),
                        summaryOff = stringResource(id = R.string.tip_collect_thread_desc_sort)
                    )
                }
            }

            // ── 5. 通用 ─────────────────────────────────────────────
            prefsGroup(title = groupGeneral) {
                prefsItem {
                    ListPrefInt(
                        key = "defaultStart",
                        title = stringResource(id = R.string.settings_default_start),
                        defaultValue = 0,
                        leadingIcon = {
                            PrefsIcon("house")
                        },
                        useSelectedAsSummary = true,
                        entries = mapOf(
                            0 to stringResource(id = R.string.title_main),
                            1 to stringResource(id = R.string.title_explore),
                            2 to stringResource(id = R.string.title_notifications)
                        )
                    )
                }
                prefsItem {
                    ListPref(
                        key = "forumFabFunction",
                        title = stringResource(id = R.string.settings_forum_fab_function),
                        defaultValue = "post",
                        leadingIcon = {
                            PrefsIcon("plus.circle")
                        },
                        useSelectedAsSummary = true,
                        entries = mapOf(
                            "post" to stringResource(id = R.string.btn_post),
                            "refresh" to stringResource(id = R.string.btn_refresh),
                            "back_to_top" to stringResource(id = R.string.btn_back_to_top),
                            "hide" to stringResource(id = R.string.btn_hide)
                        )
                    )
                }
                prefsItem {
                    SwitchPref(
                        leadingIcon = {
                            PrefsIcon("safari")
                        },
                        key = "use_webview",
                        title = stringResource(id = R.string.title_use_webview),
                        defaultChecked = true,
                        summary = {
                            if (it) stringResource(id = R.string.tip_use_webview_on)
                            else stringResource(id = R.string.tip_use_webview)
                        },
                    )
                }
                prefsItem {
                    SwitchPref(
                        leadingIcon = {
                            PrefsIcon("rectangle.3.group")
                        },
                        enabled = dependNot(key = "use_webview"),
                        key = "use_custom_tabs",
                        title = stringResource(id = R.string.title_use_custom_tabs),
                        defaultChecked = true,
                        summary = {
                            if (it) stringResource(id = R.string.tip_use_custom_tab_on)
                            else stringResource(id = R.string.tip_use_custom_tab)
                        },
                    )
                }
                prefsItem {
                    SwitchPref(
                        key = "homePageShowHistoryForum",
                        title = stringResource(id = R.string.settings_home_page_show_history_forum),
                        defaultChecked = true,
                        leadingIcon = {
                            PrefsIcon("clock")
                        },
                    )
                }
                prefsItem {
                    SwitchPref(
                        key = "doNotUsePhotoPicker",
                        title = stringResource(id = R.string.title_do_not_use_photo_picker),
                        trailingValue = if (isPhotoPickerAvailable()) {
                            null
                        } else {
                            context.getString(R.string.status_unsupported)
                        },
                        defaultChecked = false,
                        leadingIcon = {
                            PrefsIcon("photo.on.rectangle")
                        },
                        enabled = isPhotoPickerAvailable()
                    )
                }
                prefsItem {
                    TextPref(
                        leadingIcon = {
                            PrefsIcon("trash")
                        },
                        enabled = true,
                        title = stringResource(id = R.string.title_clear_picture_cache),
                        onClick = {
                            coroutineScope.launch {
                                ImageCacheUtil.clearImageAllCache(context)
                                cacheSize = "0.0B"
                                snackbarHostState.showSnackbar(context.getString(R.string.toast_clear_picture_cache_success))
                            }
                        },
                        trailingValue = cacheSize,
                    )
                }
                prefsItem {
                    TextPref(
                        leadingIcon = {
                            PrefsIcon("square.and.arrow.up")
                        },
                        enabled = true,
                        title = stringResource(id = R.string.title_open_by_default),
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                            )
                        },
                        summary = stringResource(id = R.string.tip_open_by_default)
                    )
                }
                // 实验性：CI 更新检查（原「更多」页，仅实验特性开启时显示）
                if (context.appPreferences.showExperimentalFeatures) {
                    prefsItem {
                        SwitchPref(
                            leadingIcon = {
                                PrefsIcon("arrow.clockwise")
                            },
                            key = "checkCIUpdate",
                            title = stringResource(id = R.string.title_check_ci_update),
                            defaultChecked = false,
                            summary = stringResource(id = R.string.tip_check_ci_update)
                        )
                    }
                }
            }

            // ── 6. 关于 ─────────────────────────────────────────────
            prefsGroup(title = groupAbout) {
                prefsItem {
                    TextPref(
                        leadingIcon = {
                            PrefsIcon("info.circle")
                        },
                        enabled = true,
                        title = stringResource(id = R.string.title_about),
                        onClick = {
                            navigator.navigate(AboutPageDestination)
                        },
                        trailingValue = BuildConfig.VERSION_NAME,
                    )
                }
                prefsItem {
                    TextPref(
                        leadingIcon = {
                            PrefsIcon("arrow.triangle.2.circlepath")
                        },
                        enabled = !checkingUpdate,
                        title = stringResource(id = R.string.title_check_update),
                        trailingValue = if (checkingUpdate) {
                            stringResource(id = R.string.status_checking)
                        } else {
                            null
                        },
                        onClick = {
                            if (checkingUpdate) return@TextPref
                            checkingUpdate = true
                            coroutineScope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    UpdateChecker.fetchLatest()
                                }
                                checkingUpdate = false
                                result
                                    .onSuccess { release ->
                                        if (UpdateChecker.isNewer(release.version)) {
                                            latestRelease = release
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                context.getString(
                                                    R.string.toast_already_latest,
                                                    UpdateChecker.currentVersion(),
                                                )
                                            )
                                        }
                                    }
                                    .onFailure {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.toast_check_update_failed)
                                        )
                                    }
                            }
                        },
                    )
                }
                // 服务中心：不要求登录（原在「我的」页，且只有登录后可见）
                prefsItem {
                    TextPref(
                        leadingIcon = {
                            PrefsIcon("questionmark.circle")
                        },
                        enabled = true,
                        title = stringResource(id = R.string.my_info_service_center),
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
        }

        // 有新版：弹玻璃风格对话框（显示版本号，「确定」打开发布页）
        latestRelease?.let { release ->
            AlertDialog(
                onDismissRequest = { latestRelease = null },
                title = { Text(text = stringResource(id = R.string.title_update_available)) },
                text = {
                    Text(
                        text = stringResource(
                            id = R.string.summary_update_available,
                            release.version,
                            UpdateChecker.currentVersion(),
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            latestRelease = null
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    ) {
                        Text(text = stringResource(id = R.string.btn_update_now))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { latestRelease = null }) {
                        Text(text = stringResource(id = R.string.button_cancel))
                    }
                },
            )
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
