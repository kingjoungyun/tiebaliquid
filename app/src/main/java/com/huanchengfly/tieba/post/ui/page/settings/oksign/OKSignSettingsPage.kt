package com.huanchengfly.tieba.post.ui.page.settings.oksign

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BrowseGallery
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsIcon
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsLargeTitle
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.depend
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TimePickerPerf
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.utils.isIgnoringBatteryOptimizations
import com.huanchengfly.tieba.post.utils.powerManager
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Destination
@Composable
fun OKSignSettingsPage(
    navigator: DestinationsNavigator,
) {
    val coroutineScope = rememberCoroutineScope()
    // 仍需要 MyScaffold：它提供 LocalSnackbarHostState（提示用）。
    // 但不再有 topBar，也不 apply paddingValues —— insets 统一由 PrefsScreen 处理。
    MyScaffold(backgroundColor = Color.Transparent) { _ ->
        val context = LocalContext.current
        val dataStore = context.dataStore
        val snackbarHostState = LocalSnackbarHostState.current
        PrefsScreen(
            dataStore = dataStore,
            modifier = Modifier.fillMaxSize(),
            onBack = { navigator.navigateUp() },
            title = stringResource(id = R.string.title_oksign),
            header = {
                PrefsLargeTitle(text = stringResource(id = R.string.title_oksign))
            },
        ) {
            prefsItem {
                SwitchPref(
                    leadingIcon = {
                        PrefsIcon("clock.arrow.circlepath")
                    },
                    key = "oksign_slow_mode",
                    title = stringResource(id = R.string.title_oksign_slow_mode),
                    defaultChecked = true,
                    summaryOn = stringResource(id = R.string.summary_oksign_slow_mode_on),
                    summaryOff = stringResource(id = R.string.summary_oksign_slow_mode),
                )
            }
            prefsItem {
                SwitchPref(
                    leadingIcon = {
                        PrefsIcon("checkmark.seal")
                    },
                    key = "oksign_use_official_oksign",
                    title = stringResource(id = R.string.title_oksign_use_official_oksign),
                    defaultChecked = true,
                    summary = stringResource(id = R.string.summary_oksign_use_official_oksign),
                )
            }
            prefsItem {
                SwitchPref(
                    leadingIcon = {
                        PrefsIcon("xmark.octagon")
                    },
                    key = "oksign_fail_auto_stop",
                    title = stringResource(id = R.string.title_oksign_fail_auto_stop),
                    defaultChecked = true,
                    summary = stringResource(id = R.string.summary_oksign_fail_auto_stop),
                )
            }
            prefsItem {
                SwitchPref(
                    leadingIcon = {
                        PrefsIcon("calendar.badge.clock")
                    },
                    key = "auto_sign",
                    title = stringResource(id = R.string.title_auto_sign),
                    defaultChecked = false,
                    summaryOn = stringResource(id = R.string.summary_auto_sign_on),
                    summaryOff = stringResource(id = R.string.summary_auto_sign),
                )
            }
            prefsItem {
                TimePickerPerf(
                    leadingIcon = {
                        PrefsIcon("clock")
                    },
                    key = "auto_sign_time",
                    title = stringResource(id = R.string.title_auto_sign_time),
                    defaultValue = "09:00",
                    // 当前时间挪到行尾右侧（"09:00"）
                    trailingValue = { it },
                    dialogTitle = stringResource(id = R.string.title_auto_sign_time),
                    enabled = depend(key = "auto_sign")
                )
            }
            prefsItem {
                TextPref(
                    leadingIcon = {
                        PrefsIcon("exclamationmark.triangle")
                    },
                    title = stringResource(id = R.string.title_ignore_battery_optimization),
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !context.isIgnoringBatteryOptimizations(),
                    // 只留「状态」在行尾；动作本身由标题表达，不再写行内说明
                    trailingValue =
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) stringResource(id = R.string.status_unsupported)
                    else if (context.isIgnoringBatteryOptimizations()) stringResource(id = R.string.status_ignored)
                    else null,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (!context.powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                context.requestIgnoreBatteryOptimizations()
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.toast_ignore_battery_optimization_already))
                                }
                            }
                        }
                    }
                )
            }

            prefsItem {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(stringResource(id = R.string.tip_start))
                        }
                        append(stringResource(id = R.string.tip_auto_sign))
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color = AppTokens.chip)
                        .padding(12.dp),
                    color = AppTokens.onChip,
                    fontSize = 12.sp
                )
            }
        }
    }
}
