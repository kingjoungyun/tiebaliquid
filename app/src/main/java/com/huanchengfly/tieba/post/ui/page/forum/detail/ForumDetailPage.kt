package com.huanchengfly.tieba.post.ui.page.forum.detail

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.RecommendForumInfo
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.getOrNull
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.HorizontalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.huanchengfly.tieba.post.ui.common.components.GlassListCard
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import io.github.neoruaa.glasmio.core.GlasmioTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPage

@Destination
@Composable
fun ForumDetailPage(
    forumId: Long,
    navigator: DestinationsNavigator,
    viewModel: ForumDetailViewModel = pageViewModel(),
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(ForumDetailUiIntent.Load(forumId))
        viewModel.initialized = true
    }

    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = ForumDetailUiState::isLoading,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ForumDetailUiState::error,
        initial = null
    )
    val forumInfo by viewModel.uiState.collectPartialAsState(
        prop1 = ForumDetailUiState::forumInfo,
        initial = null
    )

    val isEmpty by remember {
        derivedStateOf { forumInfo == null }
    }
    val isError by remember {
        derivedStateOf { error != null }
    }

    StateScreen(
        isEmpty = isEmpty,
        isError = isError,
        isLoading = isLoading,
        onReload = {
            viewModel.send(ForumDetailUiIntent.Load(forumId))
        },
        errorScreen = {
            ErrorScreen(error = error.getOrNull())
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        GlassPage(
            title = stringResource(id = R.string.title_forum_info),
            largeTitle = stringResource(id = R.string.title_forum_info),
            onBack = { navigator.navigateUp() },
        ) { paddingValues ->
            forumInfo?.let {
                ForumDetailContent(
                    forumInfo = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingValues.calculateTopPadding()),
                )
            }
        }
    }
}

@Composable
private fun ForumDetailContent(
    forumInfo: ImmutableHolder<RecommendForumInfo>,
    modifier: Modifier = Modifier,
) {
    val intro = remember(forumInfo) {
        forumInfo.get { content.plainText }
    }
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- 信息卡：吧头像 + 吧名 + 关注/帖子数 ----
        GlassListCard(modifier = Modifier.padding(top = 4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(
                    data = forumInfo.get { avatar },
                    size = 44.dp,
                    contentDescription = null,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = forumInfo.get { forum_name },
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTokens.text,
                        maxLines = 2,
                    )
                    val slogan = forumInfo.get { slogan }
                    if (slogan.isNotBlank()) {
                        Text(
                            text = slogan,
                            fontSize = 13.sp,
                            color = AppTokens.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatCardItem(
                    statNum = forumInfo.get { member_count },
                    statText = stringResource(id = R.string.text_stat_follow)
                )
                HorizontalDivider(
                    color = GlasmioTheme.colors.subtleStroke,
                    modifier = Modifier.height(28.dp),
                )
                StatCardItem(
                    statNum = forumInfo.get { thread_count },
                    statText = stringResource(id = R.string.text_stat_threads)
                )
            }
        }
        // ---- 简介卡 ----
        GlassListCard {
            Text(
                text = stringResource(id = R.string.title_forum_intro),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppTokens.textSecondary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = intro,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = AppTokens.text,
            )
        }
    }
}

@Composable
private fun RowScope.StatCardItem(
    statNum: Int,
    statText: String,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = statNum.getShortNumString(),
            fontSize = 20.sp,
            fontFamily = FontFamily(
                Typeface.createFromAsset(
                    LocalContext.current.assets,
                    "bebas.ttf"
                )
            ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = statText,
            fontSize = 12.sp,
            color = AppTokens.textSecondary
        )
    }
}

@Preview("ForumDetailPage", backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
fun PreviewForumDetailPage() {
    TiebaLiteTheme {
        ForumDetailContent(
            forumInfo = RecommendForumInfo(
                forum_name = "minecraft",
                slogan = "位于百度贴吧的像素点之家",
                content = listOf(
                    PbContent(
                        type = 0,
                        text = "minecraft……",
                    )
                ),
                member_count = 2520287,
                thread_count = 31531580
            ).wrapImmutable(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}