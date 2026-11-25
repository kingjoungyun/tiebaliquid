package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eygraber.compose.placeholder.material.placeholder
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPageHost
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.models.database.Block.Companion.getKeywords
import com.huanchengfly.tieba.post.ui.page.main.BottomNavigationDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassPromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextAlign
import com.huanchengfly.tieba.post.ui.common.components.GlassListCard
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassComponentSurface
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPage
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.kyant.capsule.ContinuousRoundedRectangle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.rememberScaffoldState

@Destination
@Composable
fun BlockListPage(
    viewModel: BlockListViewModel = pageViewModel<BlockListUiIntent, BlockListViewModel>(
        listOf(BlockListUiIntent.Load)
    ),
    navigator: DestinationsNavigator,
) {
        // P1：页级 backdrop host —— 本页的菜单/动作面板要能采到这一页的真实内容。
        // 页面内容进 sampledContent（活 backdrop，整页录制）；页内玻璃控件会自动走自带纯色 host。
        GlassPageHost(modifier = Modifier.fillMaxSize()) {
            val dialogState = rememberDialogState()
    
            GlassPromptDialog(
                onConfirm = {
                    val input = it.trim()
                    if (input.isNotEmpty()) {
                        val asUid = input.toLongOrNull()
                        viewModel.send(
                            BlockListUiIntent.Add(
                                category = Block.CATEGORY_BLACK_LIST,
                                type = Block.TYPE_USER,
                                userName = if (asUid == null) input else null,
                                userId = asUid
                            )
                        )
                    }
                },
                dialogState = dialogState,
                title = { Text(text = stringResource(id = R.string.title_add_black)) }
            ) {
                Column {
                    Text(text = stringResource(id = R.string.tip_add_black))
                }
            }
    
            val context = LocalContext.current
            val blackList by viewModel.uiState.collectPartialAsState(
                prop1 = BlockListUiState::blackList,
                initial = emptyList()
            )
            val isLoading by viewModel.uiState.collectPartialAsState(
                prop1 = BlockListUiState::isLoading,
                initial = false
            )
            val items = remember(blackList) { blackList.filter { it.type == Block.TYPE_USER } }
    
            // 去掉 MyScaffold 后 `LocalSnackbarHostState` 没有提供者了（它的默认实现会抛
            // "no scaffold here!"），所以这里自己造一个 state，并在 floating 槽里挂 SnackbarHost。
            val snackbarHostState = rememberScaffoldState().snackbarHostState
            GlassPage(
                title = stringResource(id = R.string.title_black_list),
                largeTitle = stringResource(id = R.string.title_black_list),
                onBack = { navigator.navigateUp() },
                floating = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                },
                bottomBar = {
                    // 底部动作条：玻璃胶囊 + 图标 + 文案（原来是 M2 的一条底栏）
                    GlassComponentSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = ContinuousRoundedRectangle(22.dp),
                        tint = AppTokens.card,
                        hostFill = AppTokens.groupedBackground,
                        innerGlow = true,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .debounceClickable(onClick = { dialogState.show() })
                                .padding(vertical = 13.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = PhosphorIcons["prohibit"] ?: Icons.Outlined.Block,
                                contentDescription = null,
                                tint = AppTokens.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.title_add_black),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppTokens.primary,
                            )
                        }
                    }
                },
            ) { paddingValues ->
                viewModel.onEvent<BlockListUiEvent.Success> {
                    snackbarHostState.showSnackbar(
                        when (it) {
                            is BlockListUiEvent.Success.Add -> context.getString(R.string.toast_add_success)
                            is BlockListUiEvent.Success.Delete -> context.getString(R.string.toast_delete_success)
                        }
                    )
                }
                StateScreen(
                    isEmpty = items.isEmpty(),
                    isError = false,
                    isLoading = isLoading,
                    emptyScreen = {
                        Column(
                            modifier = Modifier.padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.tip_empty_block_user),
                                fontSize = 15.sp,
                                color = AppTokens.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    },
                    loadingScreen = {
                        MyLazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            items(4) {
                                BlockUserItemPlaceholder()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    val clipboardManager = LocalClipboardManager.current
                    MyLazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 6.dp),
                    ) {
                        items(items, key = { it.id }) { item ->
                            LongClickMenu(menuContent = {
                                DropdownMenuItem(onClick = {
                                    clipboardManager.setText(AnnotatedString(item.username.orEmpty()))
                                    Toast.makeText(context, R.string.toast_copy_success, Toast.LENGTH_SHORT).show()
                                }) {
                                    Text(text = stringResource(id = R.string.title_copy))
                                }
                                DropdownMenuItem(onClick = {
                                    viewModel.send(BlockListUiIntent.Delete(item.id))
                                }) {
                                    Text(text = stringResource(id = R.string.title_delete))
                                }
                            }) {
                                GlassListCard(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    BlockUserItem(item = item)
                                }
                            }
                        }
                    }
                }
            }
        }
}

@Composable
private fun BlockUserItemPlaceholder() {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = PhosphorIcons["prohibit"] ?: Icons.Outlined.Block,
            contentDescription = null,
            modifier = Modifier.placeholder(visible = true, color = MaterialTheme.colors.surface)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(id = R.string.title_block_settings),
            modifier = Modifier.placeholder(visible = true, color = MaterialTheme.colors.surface)
        )
    }
}

@Composable
private fun BlockUserItem(
    item: Block,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = PhosphorIcons["user-circle"] ?: Icons.Outlined.AccountCircle,
            contentDescription = stringResource(id = R.string.block_type_user),
            tint = MaterialTheme.colors.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.username.orEmpty(),
                style = MaterialTheme.typography.subtitle1
            )
            Text(
                text = "UID: ${item.uid}",
                style = MaterialTheme.typography.caption
            )
        }
    }
}
