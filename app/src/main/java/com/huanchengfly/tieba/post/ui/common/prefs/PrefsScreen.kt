package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassAppBackground
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Container

lateinit var LocalPrefsDataStore: ProvidableCompositionLocal<DataStore<Preferences>>

/**
 * Main preference screen which holds [PrefsListItem]s
 *
 * 渲染模型（FlClash settings 组）：
 * - [LazyColumn] 只负责「按卡片分块」滚动：一张卡片 = 一个 item；
 * - 卡片内部是普通 `Column`，行与行之间插 1dp 缩进分割线；
 * - 卡片本身是**不透明表层色 + 16dp 连续圆角 + 无阴影**，没有玻璃（玻璃只给浮层用）。
 *
 * 卡片（组）的边界来自 [PrefsScope]：`prefsGroup { ... }` 一张卡；
 * 组与组之间直接写的 `prefsItem { ... }` 归入一张无标题的隐式卡。
 *
 * @param dataStore DataStore which will be used to save all the preferences
 * @param modifier Modifier applied to the [LazyColumn] holding the list of Prefs
 * @param dividerThickness 组内分割线粗细，0 为不画
 * @param dividerIndent 组内分割线左侧缩进（FlClash 为 16dp）
 * @param listState 外部可观察的滚动状态（大标题「收进顶栏」用）
 * @param header 列表最顶部的滚动内容（设置页的大标题）；它随列表一起滚走
 * @param content 分组的 DSL
 */
@Composable
fun PrefsScreen(
    dataStore: DataStore<Preferences>,
    modifier: Modifier = Modifier,
    dividerThickness: Dp = PrefsCardDefaults.DividerThickness,
    dividerIndent: Dp = PrefsCardDefaults.DividerIndent,
    listState: LazyListState = rememberLazyListState(),
    header: (@Composable () -> Unit)? = null,
    content: PrefsScope.() -> Unit
) {
    LocalPrefsDataStore = staticCompositionLocalOf { dataStore }
    val prefsScope = PrefsScopeImpl().apply(content)

    // Now the dataStore can be accessed by calling LocalPrefsDataStore.current from any child Pref
    CompositionLocalProvider(
        LocalPrefsDataStore provides dataStore,
    ) {
        // 只在这一支包 GlasmioTheme：开关要读它的明暗令牌（明暗跟随 App 自己的
        // ExtendedTheme.isNightMode，不是系统）。不包整个 App，避免 M3 主题污染其它页面。
        LiquidGlassTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                // 页面底色：App 统一的渐变底（不是玻璃），白卡片压在上面才有层次
                GlassAppBackground(modifier = Modifier.fillMaxSize())
                Container {
                    LazyColumn(
                        state = listState,
                        modifier = modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = PrefsCardDefaults.PagePadding,
                            vertical = PrefsCardDefaults.PagePadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(PrefsCardDefaults.CardGap),
                    ) {
                        if (header != null) {
                            item(key = "prefs-header") { header() }
                        }
                        items(
                            count = prefsScope.sections.size,
                            key = { index -> "prefs-card-$index" },
                        ) { index ->
                            val section = prefsScope.sections[index]
                            if (section.itemIndexes.isEmpty()) return@items
                            // 一张卡片：可选分组标题 + 卡片本体（内部带分割线）
                            val cardHeader: (@Composable () -> Unit)? =
                                section.headerIndex?.let { headerIndex ->
                                    { prefsScope.getPrefsItem(headerIndex)() }
                                }
                            PrefsCard(
                                header = cardHeader,
                                // 首组的上间距由 LazyColumn 的 contentPadding(16dp) 提供；
                                // 其余组 = 组间距 8dp + 16dp = 24dp
                                headerTopSpacing = if (index == 0) 0.dp
                                else PrefsCardDefaults.HeaderTopSpacing,
                            ) {
                                section.itemIndexes.forEachIndexed { rowIndex, itemIndex ->
                                    if (rowIndex > 0 && dividerThickness > 0.dp) {
                                        Divider(
                                            thickness = dividerThickness,
                                            indent = dividerIndent,
                                            color = PrefsCardDefaults.divider(),
                                        )
                                    }
                                    prefsScope.getPrefsItem(itemIndex)()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
