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
import androidx.compose.foundation.layout.Box
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPage
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassSnackbarHost

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
            // 与帖子页一致：GlassPage 的顶栏是 overlay（返回 / 删除 / 大标题），
            // 列表从状态栏下方起排、可滚动到状态栏与顶栏下方（不再用根布局的 statusBarsPadding 裁断）。
            GlassPage(
                title = stringResource(id = R.string.title_history),
                onBack = { navigator.navigateUp() },
                largeTitle = stringResource(id = R.string.title_history),
                actions = {
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
                },
                snackbarHost = {
                    GlassSnackbarHost(hostState = scaffoldState.snackbarHostState)
                },
            ) { _ ->
                Column(modifier = Modifier.fillMaxSize()) {
                    if (useGlassTabRow) {
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
                        // 回滚：M2 官方 TabRow
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
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
    }
}
