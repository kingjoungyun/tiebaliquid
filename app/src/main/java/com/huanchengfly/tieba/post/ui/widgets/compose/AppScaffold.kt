package com.huanchengfly.tieba.post.ui.widgets.compose

/*
 * AppScaffold：**本项目自研的页面脚手架**（不再是 M2 `Scaffold` 的包装）。
 *
 * 只做四件事：
 *  1. 槽位布局：topBar / content / bottomBar 纵向排列，content 拿 `weight(1f)`；
 *  2. `PaddingValues`：按实际测量的 topBar / bottomBar 高度给出；没有对应槽位时退回系统栏 inset
 *     （与迁移前 M2 Scaffold 的语义一致：有顶栏时 top = 顶栏高，无顶栏时 top = 状态栏高）；
 *  3. snackbar：`SnackbarHostState` + [com.huanchengfly.tieba.post.ui.common.components.glass.GlassSnackbarHost]，
 *     并用 `LocalSnackbarHostState` 提供给子树（类型可空，子页可 `?.` 或自兜底）；
 *  4. FAB：`Box` 内按 `FabPosition` 对齐，避让底栏与导航栏。
 *
 * 迁移历史：原名 MyScaffold，曾是 Material 2 Scaffold 的薄包装；v1.9.0 收口时改成这套自研布局，
 * 不再依赖 Material 2 的脚手架、状态对象与滑动消失 snackbar。
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.FabPosition
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassSnackbarHost

/** 由 [AppScaffold] 提供；未套 AppScaffold 的子树读到的是 null（调用方需自兜底）。 */
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState?> { null }

@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    /** snackbar 状态；页面自己 `remember { SnackbarHostState() }` 后传进来即可在外部 show。 */
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    /** 自定义 snackbar 外观（默认玻璃胶囊）。 */
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { GlassSnackbarHost(hostState = it) },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    backgroundColor: Color = MaterialTheme.colors.background,
    content: @Composable (PaddingValues) -> Unit,
) {
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var topBarHeightPx by remember { mutableIntStateOf(0) }
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }

    // 与 M2 Scaffold 相同的语义：有对应槽位时用它实测高度，没有时退回系统栏 inset。
    val topPadding: Dp = if (topBarHeightPx > 0) {
        with(density) { topBarHeightPx.toDp() }
    } else {
        statusBarTop
    }
    val bottomPadding: Dp = if (bottomBarHeightPx > 0) {
        with(density) { bottomBarHeightPx.toDp() }
    } else {
        navBarBottom
    }
    val contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding)

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { topBarHeightPx = it.size.height }
                ) {
                    topBar()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    content(contentPadding)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { bottomBarHeightPx = it.size.height }
                ) {
                    bottomBar()
                }
            }

            // snackbar 叠在底栏之上（不遮 FAB 与底栏按钮）
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding),
            ) {
                snackbarHost(snackbarHostState)
            }

            // FAB：避让底栏 + 导航栏
            if (floatingActionButtonPosition != FabPosition.Start) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = bottomPadding + 16.dp)
                ) {
                    floatingActionButton()
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = bottomPadding + 16.dp)
                ) {
                    floatingActionButton()
                }
            }
        }
    }
}
