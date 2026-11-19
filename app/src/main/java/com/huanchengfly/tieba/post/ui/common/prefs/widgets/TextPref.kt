package com.huanchengfly.tieba.post.ui.common.prefs.widgets

import androidx.compose.foundation.clickable
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsListItem
import com.huanchengfly.tieba.post.ui.common.prefs.ifNotNullThen
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors

//TODO add single line title?

/**
 * Simple Text with title and summary.
 * Used to show some information to the user and is the basis of all other preferences.
 *
 * @param title Main text which describes the Pref
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param darkenOnDisable If true, text colors have lower opacity when the Pref is not [enabled]
 * @param minimalHeight If true, the height of the pref is reduced, such that other content can be more easily included in the pref.
 * Mostly for internal use with custom Prefs
 * @param onClick Callback for when this Pref is clicked. Will not be called if Pref is not [enabled]
 * @param textColor Text colour of the [title] and [summary]
 * @param enabled If false, this Pref cannot be checked/unchecked
 * @param leadingIcon Icon which is positioned at the start of the Pref
 * @param showChevron 是否在右侧画 chevron（FlClash / iOS 的「可进入」提示）。
 * 默认：可点就画；但如果调用方给了 trailing 内容（开关 / 滑块 / 值），[PrefsListItem] 会跳过它
 * @param trailingContent Composable content which is positioned at the end of the Pref
 */
@ExperimentalMaterialApi
@Composable
fun TextPref(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    darkenOnDisable: Boolean = true,
    minimalHeight: Boolean = false,
    onClick: (() -> Unit)? = null,
    textColor: Color = glassColors().textPrimary,
    enabled: Boolean = onClick != null,
    leadingIcon: @Composable (() -> Unit)? = null,
    showChevron: Boolean = onClick != null && enabled,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    PrefsListItem(
        text = { Text(title) },
        modifier = if (onClick != null && enabled) modifier.clickable { onClick() } else modifier,
        enabled = enabled,
        darkenOnDisable = darkenOnDisable,
        textColor = textColor,
        minimalHeight = minimalHeight,
        icon = leadingIcon,
        secondaryText = summary.ifNotNullThen { Text(summary!!) },
        trailing = trailingContent,
        // trailing 已经占了右侧（开关 / 值），再画 chevron 就重复了
        showChevron = showChevron && trailingContent == null,
    )
}
