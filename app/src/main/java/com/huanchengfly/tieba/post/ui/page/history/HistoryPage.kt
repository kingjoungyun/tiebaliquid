package com.huanchengfly.tieba.post.ui.page.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.common.theme.glass.HarmonyTextProvider
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListPage
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListUi
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListUiEvent
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListUiStyle
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.TabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.HistoryUtil
import io.github.neoruaa.glasmio.core.GlasmioTheme
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

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

    LiquidGlassTheme(accent = ExtendedTheme.colors.primary) {
        HarmonyTextProvider {
            MyScaffold(
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
                                icon = Icons.Outlined.Delete,
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
                            color = ExtendedTheme.colors.text,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        TabRow(
                            selectedTabIndex = pagerState.currentPage,
                            indicator = { tabPositions ->
                                PagerTabIndicator(
                                    pagerState = pagerState,
                                    tabPositions = tabPositions
                                )
                            },
                            divider = {},
                            backgroundColor = Color.Transparent,
                            contentColor = ExtendedTheme.colors.primary,
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
                                    selectedContentColor = ExtendedTheme.colors.onTopBar,
                                    unselectedContentColor = ExtendedTheme.colors.onTopBarSecondary,
                                )
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