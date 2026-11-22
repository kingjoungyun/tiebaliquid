package com.huanchengfly.tieba.post.ui.page.settings.custom

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Brightness2
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.Upcoming
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.AppFontSizeActivity
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsIcon
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsLargeTitle
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.ListPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.AppIconUtil
import com.huanchengfly.tieba.post.utils.LauncherIcons
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Destination
@Composable
fun CustomSettingsPage(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    PrefsScreen(
        dataStore = context.dataStore,
        modifier = Modifier.fillMaxSize(),
        onBack = { navigator.navigateUp() },
        title = stringResource(id = R.string.title_settings_custom),
        header = {
            PrefsLargeTitle(
                text = stringResource(id = R.string.title_settings_custom),
            )
        },
    ) {
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
            val darkThemeValues = stringArrayResource(id = R.array.dark_theme_values)
            val darkThemeNames = stringArrayResource(id = R.array.dark_theme_names)
            ListPref(
                key = "dark_theme",
                title = stringResource(id = R.string.settings_night_mode),
                defaultValue = ThemeUtil.THEME_AMOLED_DARK,
                leadingIcon = {
                    PrefsIcon("moon.fill")
                },
                entries = darkThemeValues.associateWith { value ->
                    darkThemeNames[darkThemeValues.indexOf(
                        value
                    )]
                },
                useSelectedAsSummary = true,
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
                icons = mapOf(
                    LauncherIcons.NEW_ICON to {
                        Image(
                            painter = rememberDrawablePainter(
                                drawable = LocalContext.current.getDrawable(
                                    R.drawable.ic_launcher_new_round
                                )
                            ),
                            contentDescription = "新图标",
                            modifier = Modifier.size(Sizes.Medium)
                        )
                    },
                    LauncherIcons.NEW_ICON_INVERT to {
                        Image(
                            painter = rememberDrawablePainter(
                                drawable = LocalContext.current.getDrawable(
                                    R.drawable.ic_launcher_new_invert_round
                                )
                            ),
                            contentDescription = "新图标（反色）",
                            modifier = Modifier.size(Sizes.Medium)
                        )
                    },
                    LauncherIcons.OLD_ICON to {
                        Image(
                            painter = rememberDrawablePainter(
                                drawable = LocalContext.current.getDrawable(
                                    R.drawable.ic_launcher_round
                                )
                            ),
                            contentDescription = "旧图标",
                            modifier = Modifier.size(Sizes.Medium)
                        )
                    },
                ),
                onValueChange = { AppIconUtil.setIcon(icon = it) },
                useSelectedAsSummary = true,
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            prefsItem {
                val supportThemedIcons = remember {
                    persistentListOf(
                        LauncherIcons.NEW_ICON,
//                            LauncherIcons.NEW_ICON_INVERT,
                    )
                }
                val currentLauncherIcon by rememberPreferenceAsState(
                    key = stringPreferencesKey("app_icon"),
                    defaultValue = LauncherIcons.NEW_ICON
                )
                val isCurrentSupportThemedIcon by remember {
                    derivedStateOf {
                        supportThemedIcons.contains(currentLauncherIcon)
                    }
                }
                SwitchPref(
                    key = "useThemedIcon",
                    title = stringResource(id = R.string.title_settings_use_themed_icon),
                    defaultChecked = false,
                    enabled = isCurrentSupportThemedIcon,
                    leadingIcon = {
                        PrefsIcon("paintpalette")
                    },
                    onCheckedChange = {
                        AppIconUtil.setIcon(isThemed = it)
                    },
                    summary = stringResource(id = R.string.tip_settings_use_themed_icon_summary_not_supported).takeUnless { isCurrentSupportThemedIcon },
                )
            }
        }
        prefsItem {
            SwitchPref(
                key = "follow_system_night",
                title = stringResource(id = R.string.title_settings_follow_system_night),
                defaultChecked = true,
            ) {
                PrefsIcon("sun.max")
            }
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
                key = "custom_toolbar_primary_color",
                title = stringResource(id = R.string.tip_toolbar_primary_color),
                defaultChecked = false,
                leadingIcon = {
                    PrefsIcon("paintpalette.fill")
                },
                // 「重启生效」是前置状态，不是说明文 → 行尾右侧
                    trailingValue = stringResource(id = R.string.status_restart_to_apply),
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
                key = "hideExplore",
                title = stringResource(id = R.string.title_hide_explore),
                defaultChecked = false,
            ) {
                PrefsIcon("eye.slash")
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
    }
}
