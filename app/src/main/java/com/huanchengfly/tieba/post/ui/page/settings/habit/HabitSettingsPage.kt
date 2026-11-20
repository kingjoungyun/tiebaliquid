package com.huanchengfly.tieba.post.ui.page.settings.habit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.icons.automirrored.outlined.BrandingWatermark
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.outlined.AddModerator
import androidx.compose.material.icons.outlined.CalendarViewDay
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.PhotoSizeSelectActual
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsLargeTitle
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsIcon
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.ListPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.ListPrefInt
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.page.settings.LeadingIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.isPhotoPickerAvailable
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Destination
@Composable
fun HabitSettingsPage(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    PrefsScreen(
        dataStore = LocalContext.current.dataStore,
        modifier = Modifier.fillMaxSize(),
        onBack = { navigator.navigateUp() },
        header = {
            PrefsLargeTitle(text = stringResource(id = R.string.title_settings_read_habit))
        },
    ) {
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
                key = "imageDarkenWhenNightMode",
                title = stringResource(id = R.string.settings_image_darken_when_night_mode),
                defaultChecked = true,
            ) {
                PrefsIcon("moon")
            }
        }
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
            SwitchPref(
                key = "incognitoMode",
                title = stringResource(id = R.string.settings_incognito_mode),
                defaultChecked = false
            ) {
                PrefsIcon("eye.slash")
            }
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
//            prefsItem {
//                SwitchPref(
//                    key = "showShortcutInThread",
//                    title = stringResource(id = R.string.settings_show_shortcut),
//                    defaultChecked = true,
//                    leadingIcon = {
//                        LeadingIcon {
//                            AvatarIcon(
//                                icon = ImageVector.vectorResource(id = R.drawable.ic_quick_yellow),
//                                size = Sizes.Small,
//                                contentDescription = null,
//                            )
//                        }
//                    },
//                    summaryOn = stringResource(id = R.string.tip_show_shortcut_in_thread_on),
//                    summaryOff = stringResource(id = R.string.tip_show_shortcut_in_thread)
//                )
//            }
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
        prefsItem {
            SwitchPref(
                key = "show_both_username_and_nickname",
                title = stringResource(id = R.string.title_show_both_username_and_nickname),
                defaultChecked = false,
            ) {
                PrefsIcon("character")
            }
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
                summary = {
                    if (!isPhotoPickerAvailable()) {
                        context.getString(R.string.summary_photo_picker_not_supported)
                    } else if (it) {
                        context.getString(R.string.summary_do_not_use_photo_picker)
                    } else {
                        context.getString(R.string.summary_use_photo_picker)
                    }
                },
                defaultChecked = false,
                leadingIcon = {
                    PrefsIcon("photo.on.rectangle")
                },
                enabled = isPhotoPickerAvailable()
            )
        }
    }
}
