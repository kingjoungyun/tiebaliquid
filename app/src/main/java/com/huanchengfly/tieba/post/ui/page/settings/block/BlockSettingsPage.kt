package com.huanchengfly.tieba.post.ui.page.settings.block

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsIcon
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.destinations.BlockKeywordPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.BlockListPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterialApi::class)
@Destination
@Composable
fun BlockSettingsPage(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_block_settings),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        },
    ) { paddingValues ->
        PrefsScreen(
            dataStore = context.dataStore,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_block_list),
                    leadingIcon = {
                        PrefsIcon("xmark.circle")
                    },
                    onClick = { navigator.navigate(BlockListPageDestination) }
                )
            }
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_keyword_list_manage),
                    summary = stringResource(id = R.string.summary_keyword_list_manage),
                    leadingIcon = {
                        PrefsIcon("text.badge.xmark")
                    },
                    onClick = { navigator.navigate(BlockKeywordPageDestination) }
                )
            }
            prefsItem {
                SwitchPref(
                    key = "hideBlockedContent",
                    title = stringResource(id = R.string.settings_hide_blocked_content),
                    defaultChecked = false
                ) {
                    PrefsIcon("eye.slash")
                }
            }
            prefsItem {
                SwitchPref(
                    key = "blockVideo",
                    title = stringResource(id = R.string.settings_block_video),
                    summary = stringResource(id = R.string.settings_block_video_summary),
                    defaultChecked = false,
                    leadingIcon = {
                        PrefsIcon("play.slash")
                    }
                )
            }

            prefsItem {
                SwitchPref(
                    key = "showFollowedOnly",
                    title = stringResource(id = R.string.settings_show_followed_only),
                    summary = stringResource(id = R.string.settings_show_followed_only_summary),
                    defaultChecked = false,
                    leadingIcon = {
                        PrefsIcon("star.fill")
                    }
                )
            }
        }
    }
}
