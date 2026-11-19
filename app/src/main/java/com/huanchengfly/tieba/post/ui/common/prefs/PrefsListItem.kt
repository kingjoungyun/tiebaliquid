package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors

/**
 * 设置项的一行。
 *
 * 几何 / 字号对齐 FlClash `prism_ui` 的 settings 行：
 * - 行最小高 56dp、左右内边距 16dp；
 * - 标题 17sp / w400（[GlassTokens.Typography.body]）；
 * - 副标题 15sp / secondary（[GlassTokens.Typography.subheadline] + `textSecondary`），
 *   **最多 2 行、超出省略**，所以再长的说明也只会把行撑到 74dp（22 + 20×2 + 6×2）；
 * - 右侧 chevron 20dp，只在「可点但没有 trailing 控件」时出现。
 */
@Composable
@ExperimentalMaterialApi
fun PrefsListItem(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    darkenOnDisable: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    textColor: Color = glassColors().textPrimary,
    minimalHeight: Boolean = false,
    showChevron: Boolean = false,
    text: @Composable () -> Unit
) {

    val contentAlpha = when {
        enabled || !darkenOnDisable -> ContentAlpha.high
        else -> ContentAlpha.disabled
    }
    val secondaryColor = glassColors().textSecondary.copy(
        alpha = glassColors().textSecondary.alpha * contentAlpha
    )

    val styledIcon = @Composable {
        CompositionLocalProvider(LocalContentAlpha provides contentAlpha) {
            icon?.invoke()
        }
    }

    val styledText = applyTextStyle(
        GlassTokens.Typography.body.toTextStyle(textColor.copy(alpha = textColor.alpha * contentAlpha)),
        contentAlpha,
        text
    )!!
    val styledTrailing = applyTextStyle(
        GlassTokens.Typography.caption1.toTextStyle(textColor),
        contentAlpha,
        trailing
    )

    Row(
        modifier
            .heightIn(
                min = if (minimalHeight) PrefsCardDefaults.MinimalRowMinHeight
                else PrefsCardDefaults.RowMinHeight
            )
            .padding(
                start = PrefsCardDefaults.ContentPadding,
                end = PrefsCardDefaults.ContentPadding,
                top = if (minimalHeight) 0.dp else PrefsCardDefaults.RowVerticalPadding,
                bottom = if (minimalHeight) 0.dp else PrefsCardDefaults.RowVerticalPadding,
            )
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (icon != null) {
            Box(
                Modifier.sizeIn(minWidth = IconMinPaddedWidth, minHeight = IconMinPaddedWidth),
                contentAlignment = Alignment.CenterStart
            ) { styledIcon() }
        }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            styledText()
            if (summary != null) {
                CompositionLocalProvider(
                    LocalContentAlpha provides contentAlpha,
                    LocalTextStyle provides GlassTokens.Typography.subheadline
                        .toTextStyle(secondaryColor),
                ) {
                    Text(
                        text = summary,
                        maxLines = SummaryMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        when {
            styledTrailing != null -> {
                Spacer(modifier = Modifier.width(24.dp))
                styledTrailing.invoke()
            }

            showChevron -> {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = glassColors().textSecondary.copy(alpha = ChevronAlpha * contentAlpha),
                    modifier = Modifier.size(PrefsCardDefaults.ChevronSize),
                )
            }
        }
    }
}

private val IconMinPaddedWidth = 40.dp
private const val ChevronAlpha = 0.7f

/** 副标题最多两行：再长的说明也只会把行撑到 74dp。 */
private const val SummaryMaxLines = 2

/**
 * 只注入样式，不改变调用方写的内容：`Text(title)` 会自己取 `LocalTextStyle`。
 */
private fun applyTextStyle(
    textStyle: TextStyle,
    contentAlpha: Float,
    content: @Composable (() -> Unit)?
): @Composable (() -> Unit)? {
    if (content == null) return null
    return {
        CompositionLocalProvider(
            LocalContentAlpha provides contentAlpha,
            LocalTextStyle provides textStyle
        ) {
            content()
        }
    }
}
