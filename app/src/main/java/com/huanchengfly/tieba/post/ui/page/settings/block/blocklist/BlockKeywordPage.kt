package com.huanchengfly.tieba.post.ui.page.settings.block.blocklist

import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassPromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogScope
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
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
import androidx.compose.material.SnackbarHostState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassSnackbarHost
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.rounded.Check

/**
 * 屏蔽词管理：独立页面，与「黑名单」分开。
 *
 * 添加的词组只作用于评论（2 楼及以后的楼层、楼中楼），命中后该条评论直接不渲染；
 * 帖子本身（1 楼正文、帖子列表标题/摘要）不受屏蔽词影响。
 */
@Destination
@Composable
fun BlockKeywordPage(
    viewModel: BlockListViewModel = pageViewModel<BlockListUiIntent, BlockListViewModel>(
        listOf(BlockListUiIntent.Load)
    ),
    navigator: DestinationsNavigator,
) {
        // P1：页级 backdrop host —— 本页的菜单/动作面板要能采到这一页的真实内容。
        // 页面内容进 sampledContent（活 backdrop，整页录制）；页内玻璃控件会自动走自带纯色 host。
        GlassPageHost(modifier = Modifier.fillMaxSize()) {
            val dialogState = rememberDialogState()
            var isRegex by remember { mutableStateOf(false) } // 记录是否为正则
    
            val onConfirmKeyword: (String) -> Unit = {
                viewModel.send(
                    BlockListUiIntent.Add(
                        category = Block.CATEGORY_BLACK_LIST,
                        type = Block.TYPE_KEYWORD,
                        keywords = if (isRegex) listOf(it) else it.split(" "), // 避免正则中的空格导致正则失效
                        isRegex = isRegex
                    )
                )
                isRegex = false // 重置
            }
            val promptTitle: @Composable (DialogScope.() -> Unit) = {
                Text(text = stringResource(id = R.string.title_add_keyword))
            }
            val promptContent: @Composable (DialogScope.() -> Unit) = {
                Column {
                    Text(text = stringResource(id = R.string.tip_add_keyword))
                    Spacer(modifier = Modifier.height(8.dp))
                    // 方案 A：整行可点的「✓ 作为正则表达式」——勾选态用 accent 勾 + accent 文字，
                    // 不再用 M2 Checkbox（蓝方块与玻璃弹窗割裂）。
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isRegex = !isRegex }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(22.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isRegex) {
                                Icon(
                                    imageVector = PhosphorIcons["check"] ?: Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = AppTokens.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                // 未选中：给一个空框，保留下手感（纯文字会让人不知道能点）
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .border(
                                            width = 1.5.dp,
                                            color = AppTokens.textSecondary,
                                            shape = RoundedCornerShape(4.dp),
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "作为正则表达式",
                            fontSize = 15.sp,
                            color = if (isRegex) AppTokens.primary else AppTokens.text,
                        )
                    }
                }
            }
            // T3：添加屏蔽词的输入弹窗走组件层 GlassPromptDialog
            // （玻璃卡片 + GlassTextField + 取消/确定胶囊按钮）；旧 M2 PromptDialog 已删。

            GlassPromptDialog(
                onConfirm = onConfirmKeyword,
                dialogState = dialogState,
                title = promptTitle,
                content = promptContent,
            )
    
            val context = LocalContext.current
            val blackList by viewModel.uiState.collectPartialAsState(
                prop1 = BlockListUiState::blackList,
                initial = emptyList()
            )
            val isLoading by viewModel.uiState.collectPartialAsState(
                prop1 = BlockListUiState::isLoading,
                initial = false
            )
            val items = remember(blackList) { blackList.filter { it.type == Block.TYPE_KEYWORD } }
    
            // 骨架现在自带 snackbarHost 槽（见 GlassPage）：页面自己造 state，
            // 由骨架放在 overlay 底部，玻璃胶囊样式。
            val snackbarHostState = remember { SnackbarHostState() }
            // 列表 state 提到页面层，给 largeTitle 的自动折叠用（滚动时标题收起、胶囊出现）
            val listState = rememberLazyListState()
            GlassPage(
                title = stringResource(id = R.string.title_keyword_list_manage),
                largeTitle = stringResource(id = R.string.title_keyword_list_manage),
                largeTitleScrollState = listState,
                onBack = { navigator.navigateUp() },
                snackbarHost = {
                    GlassSnackbarHost(hostState = snackbarHostState)
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
                                imageVector = PhosphorIcons["text-t"] ?: Icons.Outlined.TextFields,
                                contentDescription = null,
                                tint = AppTokens.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.title_add_keyword),
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
                                text = stringResource(id = R.string.tip_empty_block_keyword),
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
                                BlockKeywordItemPlaceholder()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    val clipboardManager = LocalClipboardManager.current
                    MyLazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 6.dp),
                    ) {
                        items(items, key = { it.id }) { item ->
                            LongClickMenu(menuContent = {
                                DropdownMenuItem(onClick = {
                                    val copied = keywordsOf(item).let { list ->
                                        if (item.isRegex) list.joinToString(separator = "")
                                        else list.joinToString(separator = " ")
                                    }
                                    clipboardManager.setText(AnnotatedString(copied))
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
                                    BlockKeywordItem(item = item)
                                }
                            }
                        }
                    }
                }
            }
        }
}

private fun keywordsOf(item: Block): List<String> = item.getKeywords()

@Composable
private fun BlockKeywordItemPlaceholder() {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = PhosphorIcons["text-t"] ?: Icons.Outlined.TextFields,
            contentDescription = null,
            modifier = Modifier.placeholder(visible = true, color = MaterialTheme.colors.surface)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(id = R.string.title_keyword_list),
            modifier = Modifier.placeholder(visible = true, color = MaterialTheme.colors.surface)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockKeywordItem(
    item: Block,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_comment_new),
            contentDescription = stringResource(id = R.string.block_type_keywords),
            tint = if (item.isRegex) MaterialTheme.colors.secondary else MaterialTheme.colors.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = keywordsOf(item).joinToString(" "),
                    style = MaterialTheme.typography.subtitle1,
                    color = if (item.isRegex) MaterialTheme.colors.secondary
                    else MaterialTheme.colors.onSurface,
                    maxLines = Int.MAX_VALUE, // 支持多行
                    overflow = TextOverflow.Visible
                )
                if (item.isRegex) {
                    Text(
                        text = "正则",
                        color = MaterialTheme.colors.secondary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colors.secondary.copy(alpha = 0.13f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
