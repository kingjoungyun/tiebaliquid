package com.huanchengfly.tieba.post.ui.page.settings.more

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OfflineBolt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsIcon
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsLargeTitle
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.dependNot
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.destinations.AboutPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.utils.ImageCacheUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import kotlin.concurrent.thread
import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold

@OptIn(ExperimentalMaterialApi::class)
@Destination
@Composable
fun MoreSettingsPage(
    navigator: DestinationsNavigator,
) {
    val coroutineScope = rememberCoroutineScope()
    // 仍需要 MyScaffold：它提供 LocalSnackbarHostState（清缓存后的提示用）。
    // 但不再有 topBar，也不 apply paddingValues —— insets 统一由 PrefsScreen 处理。
    MyScaffold(backgroundColor = Color.Transparent) { _ ->
        val snackbarHostState = LocalSnackbarHostState.current
        val context = LocalContext.current
        var cacheSize by remember { mutableStateOf("0.0B") }
        LaunchedEffect(Unit) {
            thread {
                cacheSize = ImageCacheUtil.getCacheSize(context)
            }
        }
        PrefsScreen(
            dataStore = LocalContext.current.dataStore,
            modifier = Modifier.fillMaxSize(),
            onBack = { navigator.navigateUp() },
            title = stringResource(id = R.string.title_settings_more),
            header = {
                PrefsLargeTitle(
                    text = stringResource(id = R.string.title_settings_more),
                )
            },
        ) {
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
                    // 缓存大小是「状态」→ 行尾右侧
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
                    // 版本号是「状态」→ 行尾右侧
                    trailingValue = BuildConfig.VERSION_NAME,
                )
            }
    }
        }
    }
