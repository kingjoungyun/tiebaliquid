package com.huanchengfly.tieba.post.ui.page.forum.rule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.getOrNull
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassComponentSurface
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassLargeTitle
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPage
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil
import com.kyant.capsule.ContinuousRoundedRectangle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf

/**
 * 吧规详情（T5 重排）。
 *
 * 骨架：旧的 `MyScaffold` + M2 顶栏 → [GlassPage]（折叠胶囊标题 + 悬浮返回）。
 * 内容：标题/作者一张玻璃卡，正文另一张玻璃卡（段落 15sp / 行高 24sp ≈ 1.6，
 * 标题 17sp SemiBold）。富文本节点仍走 `PbContentRender`（图片/链接/加粗都在）。
 */
@OptIn(ExperimentalMaterialApi::class)
@Destination
@Composable
fun ForumRuleDetailPage(
    forumId: Long,
    navigator: DestinationsNavigator,
    viewModel: ForumRuleDetailViewModel = pageViewModel(),
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(ForumRuleDetailUiIntent.Load(forumId))
        viewModel.initialized = true
    }

    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::isLoading,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::error,
        initial = null
    )
    val title by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::title,
        initial = ""
    )
    val publishTime by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::publishTime,
        initial = ""
    )
    val preface by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::preface,
        initial = ""
    )
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::data,
        initial = persistentListOf()
    )
    val author by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::author,
        initial = null
    )

    ProvideNavigator(navigator = navigator) {
        StateScreen(
            modifier = Modifier.fillMaxSize(),
            isEmpty = data.isEmpty(),
            isError = error != null,
            isLoading = isLoading,
            onReload = {
                viewModel.send(ForumRuleDetailUiIntent.Load(forumId))
            },
            errorScreen = { ErrorScreen(error = error.getOrNull()) }
        ) {
            GlassPage(
                title = stringResource(id = R.string.title_forum_rule),
                onBack = { navigator.navigateUp() },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = padding.calculateTopPadding() + 8.dp,
                            bottom = 24.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // GlassPage 只负责折叠后的小胶囊标题；未折叠时的大标题由页面画（与楼中楼/工具箱同一套）。
                    GlassLargeTitle(
                        title = stringResource(id = R.string.title_forum_rule),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp),
                    )
                    // ---- 头部卡：标题 + 作者 + 发布时间 ----
                    RuleCard {
                        Text(
                            text = title,
                            fontSize = 20.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTokens.text,
                        )
                        author?.let {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(
                                    data = StringUtil.getAvatarUrl(it.get { portrait }),
                                    size = 24.dp,
                                    contentDescription = null,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = StringUtil.getUsernameAnnotatedString(
                                            LocalContext.current,
                                            it.get { user_name },
                                            it.get { name_show },
                                            LocalContentColor.current,
                                        ),
                                        fontSize = 13.sp,
                                        color = AppTokens.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (publishTime.isNotEmpty()) {
                                        Text(
                                            text = publishTime,
                                            fontSize = 12.sp,
                                            color = AppTokens.textSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ---- 正文卡：前言 + 分组（组标题 17sp，正文 15sp/24sp） ----
                    RuleCard {
                        ProvideTextStyle(
                            value = LocalTextStyle.current.copy(
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                color = AppTokens.text,
                            )
                        ) {
                            if (preface.isNotEmpty()) {
                                Text(text = preface)
                            }
                            data.fastForEach { section ->
                                if (section.title.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = section.title,
                                        fontSize = 17.sp,
                                        lineHeight = 24.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppTokens.text,
                                    )
                                }
                                section.contentRenders.fastForEach { render ->
                                    render.Render()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 吧规页的两张卡片：同圆角、同内边距、同玻璃材质。 */
@Composable
private fun RuleCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val shape = ContinuousRoundedRectangle(20.dp)
    GlassComponentSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        tint = AppTokens.card,
        hostFill = AppTokens.groupedBackground,
        innerGlow = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .padding(16.dp),
            content = content,
        )
    }
}
