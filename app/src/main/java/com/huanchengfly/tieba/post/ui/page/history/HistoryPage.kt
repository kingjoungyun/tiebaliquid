package com.huanchengfly.tieba.post.ui.page.history

import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassComponent
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassTabRow
import com.huanchengfly.tieba.post.ui.common.components.glass.useGlassComponent
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.common.theme.glass.HarmonyTextProvider
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListPage
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListUi
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListUiEvent
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListUiStyle
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.AppScaffold
import androidx.compose.material.TabRow
import com.huanchengfly.tieba.post.utils.HistoryUtil
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

@OptIn(ExperimentalFoundationApi::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://history")
    ]
)
@Composable
fun HistoryPage(
    navigator: DestinationsNavigator
) {
    val pagerState = rememberPagerState { 2 }
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    val context = LocalContext.current
    val glassmio = HistoryListUi.DefaultStyle == HistoryListUiStyle.GLASMIO
    // T2：TabRow 的组件级回滚开关（见 GlassComponent 的说明）
    val useGlassTabRow = useGlassComponent(GlassComponent.TabRow)

    LiquidGlassTheme(accent = AppTokens.primary) {
        HarmonyTextProvider {
            AppScaffold(
                backgroundColor = if (glassmio) GlasmioTheme.colors.groupedBackground
                else Color.Transparent,
                scaffoldState = scaffoldState,
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlasmioTheme.colors.groupedBackground)
                            .statusBarsPadding(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GlassIconButton(
                                onClick = { navigator.navigateUp() },
                                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(id = R.string.button_back),
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            GlassIconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        HistoryUtil.deleteAll()
                                        emitGlobalEvent(HistoryListUiEvent.DeleteAll)
                                        launch {
                                            scaffoldState.snackbarHostState.showSnackbar(
                                                context.getString(R.string.toast_clear_success)
                                            )
                                        }
                                    }
                                },
                                icon = PhosphorIcons["trash"] ?: Icons.Outlined.Delete,
                                contentDescription = stringResource(id = R.string.title_history_delete),
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.title_history),
                            style = LocalTextStyle.current.copy(
                                fontSize = 34.sp,
                                lineHeight = 40.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = AppTokens.text,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        if (useGlassTabRow) {
                            // T2 试点：iOS 分段控件风格（灰色轨道 + 白色滑块）。
                            // 回滚：`adb shell settings put global tieba_glass_tabrow 0`，或用下面那个
                            // `else` 分支里的 M2 TabRow（代码原样保留）。
                            GlassTabRow(
                                tabs = listOf(
                                    stringResource(id = R.string.title_history_thread),
                                    stringResource(id = R.string.title_history_forum),
                                ),
                                selectedTabIndex = pagerState.currentPage,
                                onTabSelected = { index ->
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
                                    bottom = 8.dp,
                                ),
                            )
                        } else {
                        // 回滚：M2 官方 TabRow（原来的 M2 分叉实现已随 Tabs.kt 删除）
                        TabRow(
                            selectedTabIndex = pagerState.currentPage,
                            divider = {},
                            backgroundColor = Color.Transparent,
                            contentColor = AppTokens.primary,
                            modifier = Modifier
                                .width(100.dp * 2)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            listOf(
                                R.string.title_history_thread to 0,
                                R.string.title_history_forum to 1,
                            ).forEach { (label, index) ->
                                Tab(
                                    text = {
                                        Text(
                                            text = stringResource(id = label),
                                            fontSize = 15.sp,
                                            fontWeight = if (pagerState.currentPage == index) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Normal
                                            },
                                        )
                                    },
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    selectedContentColor = AppTokens.onTopBar,
                                    unselectedContentColor = AppTokens.onTopBarSecondary,
                                )
                            }
                        }
                        }
                    }
                }
            ) {
                ProvideNavigator(navigator = navigator) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        key = { it },
                        verticalAlignment = Alignment.Top,
                        userScrollEnabled = true,
                    ) {
                        if (it == 0) {
                            HistoryListPage(type = HistoryUtil.TYPE_THREAD)
                        } else {
                            HistoryListPage(type = HistoryUtil.TYPE_FORUM)
                        }
                    }
                }
            }
        }
    }
}