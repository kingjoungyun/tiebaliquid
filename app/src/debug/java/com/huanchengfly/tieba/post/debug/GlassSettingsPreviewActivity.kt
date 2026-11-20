package com.huanchengfly.tieba.post.debug

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.HarmonyGlasmioTypography
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceEntry
import io.github.neoruaa.glasmio.preference.GlasmioPreferenceGroupTitle
import io.github.neoruaa.glasmio.preference.GlasmioSwitchPreference
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioGroupedTableFooter

/**
 * B 版设置页：**纯 Glasmio 组件 + 纯 Glasmio 参数**，仅用于 A/B 对照。
 *
 * 刻意不引用任何 `com.huanchengfly.tieba.post.ui.common.prefs.*`（PrefsCard / PrefsScreen）
 * 与 `GlassTokens` 里的 FlClash 数字，只借 App 的两样东西：
 * - `TiebaLiteTheme`：拿 `ExtendedTheme.colors.isNightMode` 作为明暗来源（App 不跟随系统）；
 * - 资源里的中文文案，保证 A/B 的文本完全一致。
 *
 * 页面结构、行高、内边距、圆角、分隔线、分组间距全部由 Glasmio 自己决定：
 * - 页面左右 16dp（Glasmio 的 page padding），组间距 10dp（它 LazyColumn 的默认 spacing）；
 * - 组标题 39dp 高、底部 9dp、17sp w590 次级色；
 * - 分组卡片 26dp 连续圆角；行高 52dp（无副标题）/ 68dp（有副标题）；
 * - 大标题 34/41（IosTypography.largeTitle），行内正文 17/22、副标题 15/20。
 *
 * 启动：
 * `adb shell am start -n com.tieba/com.huanchengfly.tieba.post.debug.GlassSettingsPreviewActivity`
 */
class GlassSettingsPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // edge-to-edge：页面底色（Glasmio 的 groupedBackground）铺到状态栏/导航栏下面
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = AndroidColor.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = AndroidColor.TRANSPARENT
        setContent {
            TiebaLiteTheme {
                GlassSettingsPreview(window = window, onBack = { finish() })
            }
        }
    }
}

@Composable
private fun GlassSettingsPreview(window: Window, onBack: () -> Unit) {
    // App 的明暗来自自己的主题（不是 isSystemInDarkTheme()），与生产设置页同源。
    val isDark = ExtendedTheme.colors.isNightMode
    SideEffect {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    GlasmioTheme(
        isDark = isDark,
        // 鸿蒙字族 + Glasmio 自己的语义排版表（字号/行高/字重全部来自 Glasmio）
        typography = HarmonyGlasmioTypography,
    ) {
        val colors = GlasmioTheme.colors
        // 只读预览状态：不写 DataStore，不影响生产设置
        var safeMode by remember { mutableStateOf(true) }

        // Glasmio 的玻璃控件（GlasmioSwitch）必须在 backdrop host 里才能取像素；
        // 本页是纯色底，用「uniformFill」那种 host（采一个纯色）代价最小，也不会自我递归。
        GlasmioBackdropHost(
            sampledColor = colors.groupedBackground,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.groupedBackground)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // ---- 返回按钮：Glasmio 的 44dp 图标按钮尺寸（不铺玻璃，只借它的命中区）----
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    SfIcon(
                        symbol = SfSymbol.ChevronBack,
                        contentDescription = "返回",
                        size = 20.dp,
                        mirrored = true,
                        tint = colors.content,
                    )
                }
    
                // ---- Glasmio 的大标题：34/41、w700、左对齐（与 iOS Large Title 同源）----
                Text(
                    text = stringResource(id = R.string.title_settings),
                    style = IosTypography.largeTitle,
                    color = colors.content,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
    
                // ================= 账号 =================
                GlasmioPreferenceGroupTitle(stringResource(id = R.string.settings_group_account))
                GlasmioGroupedList {
                    GlasmioPreferenceEntry(
                        title = { Text(stringResource(id = R.string.title_account_manage)) },
                        description = stringResource(id = R.string.summary_not_logged_in),
                        icon = { EntryIcon(SfSymbol.PersonFilled) },
                        trailingContent = { DisclosureChevron() },
                        onClick = {},
                        showTopSeparator = false,
                    )
                }
    
                // ================= 安全 =================
                GlasmioPreferenceGroupTitle(stringResource(id = R.string.settings_group_security))
                GlasmioGroupedList {
                    GlasmioSwitchPreference(
                        title = { Text(stringResource(id = R.string.title_safe_mode)) },
                        description = stringResource(id = R.string.summary_safe_mode),
                        icon = { EntryIcon("lock") },
                        checked = safeMode,
                        onCheckedChange = { safeMode = it },
                        showTopSeparator = false,
                    )
                    GlasmioPreferenceEntry(
                        title = { Text(stringResource(id = R.string.title_block_settings)) },
                        description = stringResource(id = R.string.summary_block_settings),
                        icon = { EntryIcon("eye.slash") },
                        trailingContent = { DisclosureChevron() },
                        onClick = {},
                    )
                }
    
                // ================= 外观与通用 =================
                GlasmioPreferenceGroupTitle(stringResource(id = R.string.settings_group_appearance))
                GlasmioGroupedList {
                    GlasmioPreferenceEntry(
                        title = { Text(stringResource(id = R.string.title_settings_custom)) },
                        description = stringResource(id = R.string.summary_settings_custom),
                        icon = { EntryIcon("paintpalette") },
                        trailingContent = { DisclosureChevron() },
                        onClick = {},
                        showTopSeparator = false,
                    )
                    GlasmioPreferenceEntry(
                        title = { Text(stringResource(id = R.string.title_settings_read_habit)) },
                        description = stringResource(id = R.string.summary_settings_habit),
                        icon = { EntryIcon("slider.horizontal.3") },
                        trailingContent = { DisclosureChevron() },
                        onClick = {},
                    )
                    GlasmioPreferenceEntry(
                        title = { Text(stringResource(id = R.string.title_settings_more)) },
                        description = stringResource(id = R.string.summary_settings_more),
                        icon = { EntryIcon("ellipsis") },
                        trailingContent = { DisclosureChevron() },
                        onClick = {},
                    )
                }
    
                GlasmioGroupedTableFooter("B 版 · 纯 Glasmio 组件与参数（debug 预览）")
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Glasmio 分组行左侧图标：23dp、accent 着色（与 `GlasmioListRow` 的 systemName 槽一致）。
 * 没有 SF Symbols 字体时，`SfIcon` 会退回 Glasmio 自带的 Material 矢量图。
 */
@Composable
private fun EntryIcon(symbol: SfSymbol) = EntryIcon(symbol.systemName)

@Composable
private fun EntryIcon(systemName: String) {
    SfIcon(
        systemName = systemName,
        contentDescription = null,
        size = 23.dp,
        tint = GlasmioTheme.colors.accent,
    )
}

/** Glasmio `GlasmioListRow` 的 disclosure 槽：22dp、tertiaryContent 着色。 */
@Composable
private fun DisclosureChevron() {
    SfIcon(
        systemName = "chevron.forward",
        contentDescription = null,
        size = 22.dp,
        mirrored = true,
        tint = GlasmioTheme.colors.tertiaryContent,
    )
}
