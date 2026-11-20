package com.huanchengfly.tieba.post.ui.page.settings.theme

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.util.Locale

/**
 * 主题色预设（iOS 系统色）。App 的主题色只有这 6 个可选值。
 *
 * 颜色写死在这里而不是拆成 6 个 XML theme id：现有主题机制按 theme id 走，
 * 逐个加 6 套色板会引入 6×N 个颜色资源与 6 个 style，维护成本高于收益。
 * 这里复用已有的 `custom_primary_color`（旧「自定义主题」的存储键）作为**全局 accent 覆盖**，
 * 写入逻辑走 `ThemeUtil.setCustomAccent` / `appPreferences`，没有新造一套存储。
 */
private data class PresetAccent(
    val nameRes: Int,
    val color: Color,
) {
    val hex: String
        get() = String.format(Locale.ROOT, "#%08X", color.toArgb())

    val rgbHex: String
        get() = String.format(Locale.ROOT, "#%06X", color.toArgb() and 0xFFFFFF)
}

private val PresetAccents = listOf(
    PresetAccent(R.string.theme_accent_blue, Color(0xFF0A84FF)),
    PresetAccent(R.string.theme_accent_cyan, Color(0xFF32ADE6)),
    PresetAccent(R.string.theme_accent_green, Color(0xFF34C759)),
    PresetAccent(R.string.theme_accent_orange, Color(0xFFFF9500)),
    PresetAccent(R.string.theme_accent_pink, Color(0xFFFF2D55)),
    PresetAccent(R.string.theme_accent_purple, Color(0xFFAF52DE)),
)

/**
 * 主题选择页（收敛版）。
 *
 * 只保留两项：
 * 1. **主题色**：6 个预设色块，写入全局 accent（`custom_primary_color`），全站 Compose / 玻璃 /
 *    dock / 设置页开关一起跟随；
 * 2. **深色**：沿用 App 原有的日夜切换（`ThemeUtil.switchNightMode()`），深色背景统一纯黑。
 *
 * 移除的东西：动态取色、自定义调色盘、半透明主题入口、按 theme id 的整套主题列表。
 * 半透明主题的引擎（`TranslucentThemeActivity` / `translucent_*` 分支）保持不动：
 * 如果进入本页时用户正处于半透明主题，选色只写 `translucentPrimaryColor`，不会破坏背景图。
 */
@Destination
@Composable
fun AppThemePage(
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val currentTheme by remember { ThemeUtil.themeState }
    val isDark = ThemeUtil.isNightMode(currentTheme)

    // 读取全局 accent（snapshot state：写入后本页与全站会重组）。
    val accentRaw = ThemeUtil.accentState.value
    val overrideAccent = remember(accentRaw) {
        accentRaw?.let { runCatching { Color(it.toColorInt()) }.getOrNull() }
    }
    val effectiveAccent = overrideAccent ?: ExtendedTheme.colors.primary
    val selectedPreset = PresetAccents.firstOrNull {
        it.color.toArgb() == effectiveAccent.toArgb()
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_theme),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.h6,
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                },
            )
        },
    ) { paddingValues ->
        // 浅色主题里 windowBackground/background 都是纯白，和卡片同色会看不出分组；
        // 用 chip（浅灰）当页面底。深色下 chip 仍是深灰，会破坏 A2 的纯黑，所以深色固定黑。
        // 半透明主题用 background（transparent），让背景图透出来，避免深灰 chip 配深色文字。
        val pageBackground = when {
            ThemeUtil.isTranslucentTheme(currentTheme) -> ExtendedTheme.colors.background
            isDark -> ExtendedTheme.colors.background
            else -> ExtendedTheme.colors.chip
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackground)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ---------------- 主题色 ----------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(id = R.string.title_theme_color),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ExtendedTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = 4.dp),
                )
                Text(
                    text = selectedPreset?.let {
                        "${stringResource(id = it.nameRes)}  ${it.rgbHex}"
                    } ?: String.format(
                        Locale.ROOT,
                        "%s  #%06X",
                        stringResource(id = R.string.summary_theme_color_follow),
                        effectiveAccent.toArgb() and 0xFFFFFF,
                    ),
                    fontSize = 12.sp,
                    color = ExtendedTheme.colors.textSecondary,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = ExtendedTheme.colors.card,
                shape = RoundedCornerShape(16.dp),
                elevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PresetAccents.forEach { preset ->
                        AccentSwatch(
                            preset = preset,
                            selected = preset == selectedPreset,
                            onClick = { applyAccent(context, preset) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---------------- 深色 ----------------
            Text(
                text = stringResource(id = R.string.title_theme_dark_section),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ExtendedTheme.colors.textSecondary,
                modifier = Modifier.padding(start = 4.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = ExtendedTheme.colors.card,
                shape = RoundedCornerShape(16.dp),
                elevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { ThemeUtil.switchNightMode() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.title_theme_dark),
                            color = ExtendedTheme.colors.text,
                            fontSize = 17.sp,
                        )
                        Text(
                            text = stringResource(id = R.string.summary_theme_dark),
                            color = ExtendedTheme.colors.textSecondary,
                            fontSize = 13.sp,
                        )
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { ThemeUtil.switchNightMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = effectiveAccent,
                            uncheckedThumbColor = Color.White,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AccentSwatch(
    preset: PresetAccent,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (selected) preset.color else Color.Transparent,
                shape = CircleShape,
            )
            .padding(4.dp)
            .clip(CircleShape)
            .background(preset.color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(id = R.string.desc_checked),
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * 写入全局 accent。
 *
 * - 普通主题：写 `custom_primary_color`，`ThemeDelegate` 的 colorPrimary/colorNewPrimary/colorAccent
 *   统一返回它，全站与玻璃层一起变色；
 * - 半透明主题：额外写 `translucentPrimaryColor`（该分支不被全局覆盖），背景图不受影响；
 * - 顺手关掉动态取色（它优先级更高，否则在新系统上会盖住预设色）。
 */
private fun applyAccent(context: Context, preset: PresetAccent) {
    if (ThemeUtil.isTranslucentTheme()) {
        context.appPreferences.translucentPrimaryColor = preset.hex
    }
    ThemeUtil.setCustomAccent(preset.hex)
    ThemeUtil.setUseDynamicTheme(false)
}
