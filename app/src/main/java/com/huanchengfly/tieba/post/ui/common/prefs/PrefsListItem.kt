package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.clickable
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
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceEntry
import com.huanchengfly.tieba.post.ui.common.theme.glass.LocalGlassTokens

/**
 * 设置项的一行。
 *
 * 渲染层由 [LocalPrefsUiStyle] 决定，**签名对所有 pref widget 保持不变**：
 *
 * - `PrefsUiStyle.GLASSMIO`（默认）：一个 `GlasmioPreferenceEntry`
 *   （52dp 无副标题 / 68dp 有副标题、左右 16dp、icon 槽、内建分隔线），
 *   右侧 chevron 用 Glasmio 的 `SfIcon`；
 * - `PrefsUiStyle.LEGACY`：迁移前的 FlClash 行（56dp、17sp 标题、副标题最多 2 行）。
 *
 * **「副标题最多几行」的规则（新）**：行内副标题只留 1 行以内；超过 1 行的说明不再把行撑高，
 * 而是登记到组尾 footnote（见 [PrefsFootnoteState] / [PrefsCard]）。判定用的是真实的
 * `TextMeasurer` 折行结果（按行的实测宽度），不是字符数估算。
 */
@Composable
@ExperimentalMaterialApi
fun PrefsListItem(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    darkenOnDisable: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    /** 行尾右侧的「状态/数值」（小号次级色）：当前选项、缓存大小、未登录… 不再占行内副标题 */
    trailingValue: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    textColor: Color = LocalGlassTokens.current.textPrimary,
    minimalHeight: Boolean = false,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    text: @Composable () -> Unit
) {
    if (LocalPrefsUiStyle.current == PrefsUiStyle.GLASSMIO) {
        GlasmioPrefsRow(
            modifier = modifier,
            enabled = enabled,
            darkenOnDisable = darkenOnDisable,
            icon = icon,
            summary = summary,
            trailingValue = trailingValue,
            trailing = trailing,
            textColor = textColor,
            showChevron = showChevron,
            onClick = onClick,
            text = text,
        )
    } else {
        LegacyPrefsRow(
            modifier = modifier,
            enabled = enabled,
            darkenOnDisable = darkenOnDisable,
            icon = icon,
            summary = summary,
            trailingValue = trailingValue,
            trailing = trailing,
            textColor = textColor,
            minimalHeight = minimalHeight,
            showChevron = showChevron,
            onClick = onClick,
            text = text,
        )
    }
}

/**
 * Glasmio 分组列表里的一行。
 *
 * 几何全部交给 `GlasmioPreferenceEntry`（52/68dp 行高、左右 16dp、图标槽、内建分隔线、
 * disabled 统一 0.5 alpha），这里只做四件「语义」上的接线：
 *
 * 1. **文字样式**：Glasmio 的 `GlasmioPreferenceEntry` 用 M3 的 `ProvideTextStyle`，
 *    而业务层的 `Text(title)` 是 M2 Text，只会读 M2 的 `LocalTextStyle` / `LocalContentColor`，
 *    所以这里显式把它们指到 Glasmio 的 body 样式与 content 色（鸿蒙字体来自
 *    `HarmonyGlasmioTypography`，已经在 `GlasmioTheme` 里）。
 * 2. **图标槽**：沿用业务入口给的图标，槽宽 28dp（行高因此仍是 52dp），
 *    图标本体按原尺寸（36dp）溢出槽位居中绘制，不做二次缩放/换图。
 * 3. **chevron**：M2 的圆角 chevron 换成 Glasmio 的 `SfIcon`（无 SF 字体时回落成
 *    Material 的 chevron 字形），着色跟 accent。
 * 4. **长描述 → 组尾 footnote**：见 [PrefsListItem] 的 KDoc。
 */
@Composable
private fun GlasmioPrefsRow(
    modifier: Modifier,
    enabled: Boolean,
    darkenOnDisable: Boolean,
    icon: (@Composable () -> Unit)?,
    summary: String?,
    trailingValue: String?,
    trailing: (@Composable () -> Unit)?,
    textColor: Color,
    showChevron: Boolean,
    onClick: (() -> Unit)?,
    text: @Composable () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val rowIndex = LocalPrefsRowIndex.current
    val footnotes = LocalPrefsFootnotes.current

    // 行的实测宽度（分组行最后一定 fillMaxWidth）——用它给 TextMeasurer 一个真实可用宽度
    var rowWidthPx by remember { mutableIntStateOf(0) }

    val bodyStyle: TextStyle = IosTypography.body
    val subheadlineStyle: TextStyle = IosTypography.subheadline
    val measurer: TextMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val availableWidthPx = remember(rowWidthPx, icon != null, trailing != null, showChevron, density) {
        if (rowWidthPx <= 0) {
            0
        } else {
            val reservedDp = PrefsGlasmioDefaults.RowHorizontalPadding + // 左
                PrefsGlasmioDefaults.RowHorizontalPadding + // 右
                if (icon != null) {
                    // 图标槽 + Glasmio 的 4dp×2 内边距 + 槽后 12dp
                    PrefsGlasmioDefaults.IconSlot + 8.dp + 12.dp
                } else {
                    0.dp
                } + if (trailing != null) {
                // 槽后 12dp + 开关宽度（GlasmioSwitch 固定 63dp）
                12.dp + 63.dp
            } else if (showChevron) {
                12.dp + 22.dp
            } else {
                0.dp
            }
            with(density) { (rowWidthPx - reservedDp.roundToPx()).coerceAtLeast(0) }
        }
    }

    // 真实折行判定：> 1 行 => 挪到组尾 footnote
    val wrapsToSecondLine = remember(summary, availableWidthPx, subheadlineStyle, measurer) {
        val candidate = summary
        if (candidate.isNullOrBlank() || availableWidthPx <= 0) {
            false
        } else {
            wrapCount(
                measurer = measurer,
                text = candidate,
                style = subheadlineStyle,
                maxWidthPx = availableWidthPx,
            ) > 1
        }
    }
    val footnoteText = summary?.takeIf { wrapsToSecondLine && it.isNotBlank() }
    val inlineSummary = summary?.takeIf { !wrapsToSecondLine && it.isNotBlank() }

    // 登记/撤销 footnote：SideEffect 里写（不在组合期间改状态），report 内部做等值判断避免重组循环
    SideEffect { footnotes?.report(rowIndex, footnoteText) }
    DisposableEffect(footnotes, rowIndex) {
        onDispose { footnotes?.clear(rowIndex) }
    }

    // 业务层默认传过来的就是 App 的 textPrimary；这种情况下让 Glasmio 的 content 色接管
    val entryTextColor = if (textColor == Color.Unspecified || textColor == glassColors().textPrimary) {
        colors.content
    } else {
        textColor
    }

    GlasmioPreferenceEntry(
        modifier = modifier.onSizeChanged { rowWidthPx = it.width },
        title = {
            CompositionLocalProvider(
                LocalTextStyle provides bodyStyle,
                LocalContentColor provides entryTextColor,
                LocalContentAlpha provides ContentAlpha.high,
            ) {
                text()
            }
        },
        description = inlineSummary,
        icon = icon?.let { iconContent ->
            {
                Box(
                    modifier = Modifier.sizeIn(
                        minWidth = PrefsGlasmioDefaults.IconSlot,
                        minHeight = PrefsGlasmioDefaults.IconSlot,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    iconContent()
                }
            }
        },
        trailingContent = when {
            trailing != null || trailingValue != null || showChevron -> {
                {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 状态/数值：小号次级色，排在 chevron 左边（Apple 的 value 位）
                        if (trailingValue != null) {
                            Text(
                                text = trailingValue,
                                style = subheadlineStyle,
                                color = colors.secondaryContent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        when {
                            trailing != null -> CompositionLocalProvider(
                                LocalTextStyle provides bodyStyle,
                                LocalContentColor provides colors.secondaryContent,
                            ) { trailing() }

                            showChevron -> GlasmioChevron(tint = colors.accent)
                        }
                    }
                }
            }

            else -> null
        },
        // 点击必须交给 GlasmioPreferenceEntry 自己的 clickable：它内部**总是**挂一个
        // `clickable(enabled = isEnabled && onClick != null)`，即使 disabled 也会消费手势，
        // 把外层（调用方 modifier 上的）clickable 的 up 事件吃掉 —— 于是整行点不动。
        // 所以 `PrefsListItem` 显式收 `onClick`，两种渲染层各自挂在自己的点击源上。
        onClick = if (enabled) onClick else null,
        isEnabled = if (darkenOnDisable) enabled else true,
        showTopSeparator = rowIndex > 0,
    )
}

/** Glasmio 分组列表的 disclosure chevron（22dp，accent 着色）。 */
@Composable
private fun GlasmioChevron(tint: Color) {
    SfIcon(
        systemName = "chevron.right",
        contentDescription = null,
        size = 22.dp,
        mirrored = true,
        tint = tint,
    )
}

/** 只量行数，不保留布局结果；`?` 说明：`TextMeasurer` 是稳定对象，结果只用于 >1 行判定。 */
private fun wrapCount(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    maxWidthPx: Int,
): Int {
    val result: TextLayoutResult = measurer.measure(
        text = text,
        style = style,
        maxLines = Int.MAX_VALUE,
        constraints = Constraints(maxWidth = maxWidthPx),
    )
    return result.lineCount
}

/**
 * A 版（FlClash）行：迁移前的实现，原样保留。
 *
 * - 行最小高 56dp、左右内边距 16dp；
 * - 标题 17sp / w400（[GlassTokens.Typography.body]）；
 * - 副标题 15sp / secondary（[GlassTokens.Typography.subheadline] + `textSecondary`），
 *   **最多 2 行、超出省略**；
 * - 右侧 chevron 20dp，只在「可点但没有 trailing 控件」时出现。
 */
@Composable
@ExperimentalMaterialApi
private fun LegacyPrefsRow(
    modifier: Modifier,
    enabled: Boolean,
    darkenOnDisable: Boolean,
    icon: (@Composable () -> Unit)?,
    summary: String?,
    trailingValue: String?,
    trailing: (@Composable () -> Unit)?,
    textColor: Color,
    minimalHeight: Boolean,
    showChevron: Boolean,
    onClick: (() -> Unit)?,
    text: @Composable () -> Unit,
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
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
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
                if (trailingValue != null) {
                    LegacyTrailingValue(trailingValue, secondaryColor, contentAlpha)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                styledTrailing.invoke()
            }

            showChevron -> {
                Spacer(modifier = Modifier.width(8.dp))
                if (trailingValue != null) {
                    LegacyTrailingValue(trailingValue, secondaryColor, contentAlpha)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = glassColors().textSecondary.copy(alpha = ChevronAlpha * contentAlpha),
                    modifier = Modifier.size(PrefsCardDefaults.ChevronSize),
                )
            }

            // 只有状态值、没有 trailing 控件也没有 chevron（例如不可点的信息行）
            trailingValue != null -> {
                Spacer(modifier = Modifier.width(16.dp))
                LegacyTrailingValue(trailingValue, secondaryColor, contentAlpha)
            }
        }
    }
}

private val IconMinPaddedWidth = 40.dp
private const val ChevronAlpha = 0.7f

/** A 版的行尾状态值（15sp / 次级色），与 Glasmio 路径的 value 位保持同样语义。 */
@Composable
private fun LegacyTrailingValue(text: String, color: Color, contentAlpha: Float) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = GlassTokens.Typography.subheadline.toTextStyle(color.copy(alpha = color.alpha * contentAlpha)),
    )
}

/** 副标题最多两行（仅 A 版）：再长的说明也只会把行撑到 74dp。 */
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
