package com.huanchengfly.tieba.post.ui.page.hottopic.list

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.topicList.NewTopicList
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.OrangeA700
import com.huanchengfly.tieba.post.ui.common.theme.compose.RedA700
import com.huanchengfly.tieba.post.ui.common.theme.compose.Shapes
import com.huanchengfly.tieba.post.ui.common.theme.compose.White
import com.huanchengfly.tieba.post.ui.common.theme.compose.Yellow
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.destinations.TopicDetailPageDestination
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageSkin
import com.huanchengfly.tieba.post.ui.page.thread.ThreadTopBarHeight
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPage

@Composable
private fun TopicImage(
    index: Int,
    imageUri: String
) {
    val boxModifier = if (index < 3) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(2.39f)
            .clip(Shapes.medium)
    } else {
        Modifier
            .size(Sizes.Medium)
            .aspectRatio(1f)
            .clip(Shapes.small)
    }
    Box(
        modifier = boxModifier
    ) {
        NetworkImage(
            imageUri = imageUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Text(
            text = "${index + 1}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.background,
            fontFamily = FontFamily(
                Typeface.createFromAsset(
                    LocalContext.current.assets,
                    "bebas.ttf"
                )
            ),
            modifier = Modifier
                .background(
                    when (index) {
                        0 -> RedA700
                        1 -> OrangeA700
                        2 -> Yellow
                        else -> MaterialTheme.colors.onBackground.copy(ContentAlpha.medium)
                    }
                )
                .padding(4.dp)
        )
    }
}

@Composable
private fun TopicBody(
    index: Int,
    item: NewTopicList
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = item.topic_name, style = MaterialTheme.typography.subtitle1)
            when (item.topic_tag) {
                2 -> Text(
                    text = stringResource(id = R.string.topic_tag_hot),
                    fontSize = 10.sp,
                    color = White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(RedA700)
                        .padding(vertical = 2.dp, horizontal = 4.dp)
                )

                1 -> Text(
                    text = stringResource(id = R.string.topic_tag_new),
                    fontSize = 10.sp,
                    color = White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(OrangeA700)
                        .padding(vertical = 2.dp, horizontal = 4.dp)
                )
            }
        }
        Text(
            text = item.topic_desc,
            maxLines = if (index < 3) 3 else 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.body2
        )
        Text(
            text = stringResource(id = R.string.hot_num, item.discuss_num.getShortNumString()),
            style = MaterialTheme.typography.caption,
            color = AppTokens.textSecondary
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Destination
@Composable
fun HotTopicListPage(
    viewModel: HotTopicListViewModel = pageViewModel<HotTopicListUiIntent, HotTopicListViewModel>(
        listOf(HotTopicListUiIntent.Load)
    ),
    navigator: DestinationsNavigator,
) {
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = HotTopicListUiState::isRefreshing,
        initial = false
    )
    val topicList by viewModel.uiState.collectPartialAsState(
        prop1 = HotTopicListUiState::topicList,
        initial = emptyList()
    )
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val topInset = statusBarTop + ThreadTopBarHeight
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = { viewModel.send(HotTopicListUiIntent.Load) }
        )
        GlassPage(
            title = stringResource(id = R.string.title_hot_message_list),
            largeTitle = stringResource(id = R.string.title_hot_message_list),
            onBack = { navigator.navigateUp() },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                MyLazyColumn(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(
                        top = topInset + padding.calculateTopPadding(),
                        bottom = 16.dp,
                    ),
                ) {
                    itemsIndexed(
                        items = topicList,
                        key = { _, item -> item.topic_id },
                    ) { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .debounceClickable(onClick = {
                                    navigator.navigate(
                                        TopicDetailPageDestination(item.topic_id, item.topic_name)
                                    )
                                })
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = "${index + 1}",
                                modifier = Modifier.width(30.dp),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (index < 3) AppTokens.accent else AppTokens.textSecondary,
                            )
                            Text(
                                text = item.topic_name,
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTokens.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.discuss_num.getShortNumString(),
                                fontSize = 13.sp,
                                color = AppTokens.textSecondary,
                            )
                        }
                    }
                }
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topInset),
                    backgroundColor = AppTokens.pullRefreshIndicator,
                    contentColor = AppTokens.primary,
                )
            }
        }
}
