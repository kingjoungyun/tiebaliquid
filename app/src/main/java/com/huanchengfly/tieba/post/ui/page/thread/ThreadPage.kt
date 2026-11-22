package com.huanchengfly.tieba.post.ui.page.thread

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.automirrored.rounded.ChromeReaderMode
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.AlignVerticalTop
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Face6
import androidx.compose.material.icons.rounded.FaceRetouchingOff
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.booleanToString
import com.huanchengfly.tieba.post.api.models.protos.PollInfo
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.bawuType
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.models.ThreadHistoryInfoBean
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.common.LocalThreadImageRadius
import com.huanchengfly.tieba.post.ui.common.LocalThreadImagesEnabled
import com.huanchengfly.tieba.post.ui.common.PicWaterfallContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.invertChipBackground
import com.huanchengfly.tieba.post.ui.common.theme.compose.invertChipContent
import com.huanchengfly.tieba.post.ui.common.theme.compose.loadMoreIndicator
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.common.theme.compose.threadBottomBar
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.CopyTextDialogPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ReplyPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SubPostsSheetPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.HorizontalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.ListMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.OriginThreadCard
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TextWithMinWidth
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.buildChipInlineContent
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.DateTimeUtils.getRelativeTimeString
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.Util.getIconColorByLevel
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.systemGray5
import io.github.neoruaa.glasmio.glass.render.backdrops.emptyBackdrop
import io.github.neoruaa.glasmio.ui.GlasmioCard
import kotlinx.serialization.Serializable
import kotlin.concurrent.thread
import kotlin.math.max
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

private fun getDescText(
    time: Long?,
    floor: Int,
    ipAddress: String?
): String {
    val texts = listOfNotNull(
        time?.let { getRelativeTimeString(App.INSTANCE, it) },
        if (floor > 1) App.INSTANCE.getString(R.string.tip_post_floor, floor) else null,
        if (ipAddress.isNullOrEmpty()) null else App.INSTANCE.getString(
            R.string.text_ip_location,
            ipAddress
        )
    )
    if (texts.isEmpty()) {
        return ""
    }
    return texts.joinToString(" · ")
}

@Composable
fun PostAgreeBtn(
    hasAgreed: Boolean,
    agreeNum: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = if (hasAgreed) AppTokens.accent else AppTokens.textSecondary,
        label = "postAgreeBtnColor"
    )
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = AppTokens.background,
            contentColor = animatedColor
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (hasAgreed) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(id = R.string.title_agree),
                tint = animatedColor,
                modifier = Modifier.size(16.dp)
            )
            if (agreeNum > 0) {
                Text(
                    text = agreeNum.getShortNumString(),
                    color = animatedColor,
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun BottomBarAgreeBtn(
    hasAgreed: Boolean,
    agreeNum: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    transparentBackground: Boolean = false,
) {
    val color = if (hasAgreed) AppTokens.accent else AppTokens.textSecondary
    val animatedColor by animateColorAsState(color, label = "agreeBtnColor")

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(0),
        contentPadding = PaddingValues(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (transparentBackground) Color.Transparent else AppTokens.bottomBar,
            contentColor = animatedColor
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterVertically),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (hasAgreed) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(id = R.string.title_agree),
                tint = animatedColor
            )
            if (agreeNum > 0) {
                Text(
                    text = agreeNum.getShortNumString(),
                    style = MaterialTheme.typography.caption,
                    color = animatedColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun BottomBarPlaceholder() {
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .background(AppTokens.bottomBar)
            // 拦截点击事件
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(AppTokens.bottomBarSurface)
                .padding(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.tip_reply_thread),
                style = MaterialTheme.typography.caption,
                color = AppTokens.onBottomBarSurface,
            )
        }

        BottomBarAgreeBtn(
            hasAgreed = false,
            agreeNum = 1,
            onClick = {},
            modifier = Modifier.fillMaxHeight()
        )

        Box(
            modifier = Modifier.fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(id = R.string.btn_more),
                tint = AppTokens.textSecondary,
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ToggleButton(
    text: @Composable (() -> Unit),
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    backgroundColor: Color = AppTokens.chip,
    contentColor: Color = AppTokens.text,
    selectedBackgroundColor: Color = AppTokens.invertChipBackground,
    selectedContentColor: Color = AppTokens.invertChipContent,
) {
    val animatedColor by animateColorAsState(
        if (checked) selectedContentColor else contentColor,
        label = "toggleBtnColor"
    )
    val animatedBackgroundColor by animateColorAsState(
        if (checked) selectedBackgroundColor else backgroundColor,
        label = "toggleBtnBackgroundColor"
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = true,
        shape = RoundedCornerShape(6.dp),
        color = animatedBackgroundColor,
        contentColor = animatedColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    icon()
                }
                ProvideTextStyle(
                    value = MaterialTheme.typography.subtitle1.copy(
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                ) {
                    text()
                }
            }
        }
    }
}

object ThreadPageFrom {
    const val FROM_FORUM = "forum"

    // 收藏
    const val FROM_STORE = "store_thread"
    const val FROM_PERSONALIZED = "personalized"
    const val FROM_HISTORY = "history"
}

@Serializable
sealed interface ThreadPageExtra

@Serializable
data object ThreadPageNoExtra : ThreadPageExtra

@Serializable
data class ThreadPageFromStoreExtra(
    val maxPid: Long,
    val maxFloor: Int,
) : ThreadPageExtra

@Composable
private fun ThreadLoadMoreIndicator(
    isLoading: Boolean,
    loadMoreEnd: Boolean,
    willLoad: Boolean,
    hasMore: Boolean,
) {
    Surface(
        elevation = 8.dp,
        shape = RoundedCornerShape(100),
        color = AppTokens.loadMoreIndicator,
        contentColor = AppTokens.text
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(10.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.body2.copy(fontSize = 13.sp)) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = AppTokens.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.text_loading),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    loadMoreEnd -> {
                        Text(
                            text = stringResource(id = R.string.no_more),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    hasMore -> {
                        Text(
                            text = if (willLoad) stringResource(id = R.string.release_to_load) else stringResource(
                                id = R.string.pull_to_load
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    else -> {
                        Text(
                            text = if (willLoad) stringResource(id = R.string.release_to_load_latest_posts) else stringResource(
                                id = R.string.pull_to_load_latest_posts
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://thread/{threadId}"),
    ]
)
@Composable
fun ThreadPage(
    threadId: Long,
    navigator: DestinationsNavigator,
    forumId: Long? = null,
    postId: Long = 0,
    seeLz: Boolean = false,
    sortType: Int = 0,
    from: String = "",
    extra: ThreadPageExtra? = null,
    threadInfo: ThreadInfo? = null,
    scrollToReply: Boolean = false,
    viewModel: ThreadViewModel = pageViewModel(),
) {
    // 帖子页外壳的风格开关（GLASMIO / LEGACY）。楼层内容、数据流两条分支完全共用。
    // API < 33 时 Glasmio 材质起不来，直接退回旧外观，避免玻璃组件读到不存在的 theme/host。
    val glassmio = ThreadPageUi.DefaultStyle == ThreadPageUiStyle.GLASMIO && isLiquidGlassSupported()
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(
            ThreadUiIntent.Init(
                threadId,
                forumId,
                postId,
                threadInfo,
                seeLz,
                sortType,
                scrollToReply,
            )
        )
        viewModel.send(
            ThreadUiIntent.Load(
                threadId,
                page = 0,
                // 只有「点回复数」进来才把 pid 带下去（服务端会返回该楼层所在页）。
                // 普通点帖子卡片进来必须传 0，否则服务端直接返回评论区那几页，
                // 表现就是「点进帖子直接跳到评论，看不到楼主内容」。
                postId = if (scrollToReply) postId else 0L,
                forumId = forumId,
                seeLz = seeLz,
                sortType = sortType,
                from = from
            )
        )
        viewModel.initialized = true
    }
    val scaffoldState = rememberScaffoldState()
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::data,
        initial = persistentListOf()
    )
    val author by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::author,
        initial = null
    )
    val thread by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::threadInfo,
        initial = null
    )
    val firstPost by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::firstPost,
        initial = null
    )
    val forum by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::forum,
        initial = null
    )
    val user by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::user,
        initial = wrapImmutable(User())
    )
    val anti by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::anti,
        initial = null
    )
    val firstPostContentRenders by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::firstPostContentRenders,
        initial = persistentListOf()
    )
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isLoadingMore,
        initial = false
    )
    val isError by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isError,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::error,
        initial = null
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::hasMore,
        initial = true
    )
    val nextPagePostId by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::nextPagePostId,
        initial = 0L
    )
    val hasPrevious by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::hasPrevious,
        initial = true
    )
    val currentPageMax by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::currentPageMax,
        initial = 0
    )
    val totalPage by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::totalPage,
        initial = 0
    )
    val isSeeLz by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::seeLz,
        initial = seeLz
    )
    val curSortType by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::sortType,
        initial = sortType
    )
    val isImmersiveMode by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::isImmersiveMode,
        initial = false
    )
    val latestPosts by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadUiState::latestPosts,
        initial = persistentListOf()
    )

    val isEmpty by remember {
        derivedStateOf { data.isEmpty() && firstPost == null }
    }
    val enablePullRefresh by remember {
        derivedStateOf {
            hasPrevious || curSortType == ThreadSortType.SORT_TYPE_DESC
        }
    }
    val loadMoreEnd by remember {
        derivedStateOf {
            !hasMore && curSortType == ThreadSortType.SORT_TYPE_DESC
        }
    }
    val loadMorePreloadCount by remember {
        derivedStateOf {
            if (hasMore) {
                3
            } else {
                0
            }
        }
    }
    val isCollected = remember(thread) {
        thread?.get { collectStatus != 0 } == true
    }
    val hasThreadAgreed = remember(thread) {
        thread?.get { agree?.hasAgree == 1 } == true
    }
    val threadAgreeNum = remember(thread) {
        thread?.get { agree?.diffAgreeNum } ?: 0L
    }
    val threadTitle = remember(thread) {
        thread?.get { title } ?: ""
    }
    val curForumId = remember(forumId, forum) {
        forumId ?: forum?.get { id }
    }
    val curForumName = remember(forum) { forum?.get { name } }
    val curTbs = remember(anti) { anti?.get { tbs } }
    var waitLoadSuccessAndScrollToFirstReply by remember { mutableStateOf(scrollToReply) }

    val lazyListState = rememberLazyListState()
    // 转场期间先不发起首屏图片请求：350ms 后再打开，保证最终一定会加载。
    var imagesEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(350)
        imagesEnabled = true
    }
    // GLASMIO 外壳：顶栏是 overlay，列表 contentPadding 只留「状态栏 + 顶栏」，
    // 首屏内容直接贴在悬浮控件下方，滚动时内容从顶栏下穿过。
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val threadTopInset = statusBarTop + ThreadTopBarHeight
    // 底栏在 overlay 层：列表 viewport 铺到屏幕底，用 contentPadding 让最后一条能滚到胶囊上方。
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val threadBottomInset = if (glassmio) ThreadBottomBarInset + navigationBarBottom else 0.dp
    val isContentScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 6
        }
    }
    val topScrimAlpha by animateFloatAsState(
        targetValue = if (isContentScrolled) 1f else 0f,
        animationSpec = tween(180),
        label = "threadTopScrim",
    )
    val threadForumName = curForumName?.takeIf { it.isNotBlank() } ?: "贴吧"
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val lastVisibilityPost by remember {
        derivedStateOf {
            data.firstOrNull { (post) ->
                val lastPostKey = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull { info ->
                    info.key is String && (info.key as String).startsWith("Post_")
                }?.key as String?
                lastPostKey?.endsWith(post.get { id }.toString()) == true
            }?.post ?: firstPost
        }
    }
    val lastVisibilityPostId by remember {
        derivedStateOf { lastVisibilityPost?.get { id } ?: 0L }
    }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val openBottomSheet = {
        coroutineScope.launch {
            bottomSheetState.show()
        }
    }
    val closeBottomSheet = {
        coroutineScope.launch {
            bottomSheetState.hide()
        }
    }

    // 顶栏/底栏共用的一组动作：GLASMIO 的底栏在 overlay 槽里，LEGACY 的底栏在 Scaffold 里。
    val onReplyClick: () -> Unit = {
        navigator.navigate(
            ReplyPageDestination(
                forumId = curForumId ?: 0,
                forumName = forum?.get { name }.orEmpty(),
                threadId = threadId,
            )
        )
    }
    val onAgreeClick: () -> Unit = {
        val firstPostId =
            thread?.get { firstPostId }.takeIf { it != 0L }
                ?: firstPost?.get { id }
                ?: 0L
        if (firstPostId != 0L) viewModel.send(
            ThreadUiIntent.AgreeThread(
                threadId,
                firstPostId,
                !hasThreadAgreed
            )
        )
    }
    val onMoreClick: () -> Unit = {
        if (bottomSheetState.isVisible) {
            closeBottomSheet()
        } else {
            openBottomSheet()
        }
    }
    val onReplyCountClick: () -> Unit = {
        coroutineScope.launch { lazyListState.animateScrollToItem(1) }
    }
    val onSeeLzClick: () -> Unit = {
        viewModel.send(
            ThreadUiIntent.LoadFirstPage(
                threadId,
                forumId,
                !isSeeLz,
                curSortType
            )
        )
    }
    val onSortClick: () -> Unit = {
        viewModel.send(
            ThreadUiIntent.LoadFirstPage(
                threadId,
                forumId,
                isSeeLz,
                if (curSortType != ThreadSortType.SORT_TYPE_DESC) {
                    ThreadSortType.SORT_TYPE_DESC
                } else {
                    ThreadSortType.SORT_TYPE_DEFAULT
                }
            )
        )
    }

    MyBackHandler(
        enabled = bottomSheetState.isVisible,
        currentScreen = ThreadPageDestination
    ) {
        closeBottomSheet()
    }

    viewModel.onEvent<ThreadUiEvent.ScrollToFirstReply> {
        lazyListState.animateScrollToItem(1)
    }
    viewModel.onEvent<ThreadUiEvent.ScrollToLatestReply> {
        if (curSortType != ThreadSortType.SORT_TYPE_DESC) {
            // 新回复已并入 data 末尾（非倒序，见 ThreadViewModel.LoadMyLatestReply.Success），
            // 滚到最后一个 data 项即可精确停在自己刚回的那一层楼。
            lazyListState.animateScrollToItem((2 + data.size - 1).coerceAtLeast(0))
        } else {
            // 倒序时新回复在 data 开头（index 2），滚到楼主正文下方即露出刚回的楼层。
            lazyListState.animateScrollToItem(2)
        }
    }
    viewModel.onEvent<ThreadUiEvent.LoadSuccess> {
        if (it.page > 1 || waitLoadSuccessAndScrollToFirstReply) {
            waitLoadSuccessAndScrollToFirstReply = false
            if (scrollToReply && postId != 0L) {
                // 带了具体楼层 pid：精确滚到该楼层，使其带上下的楼层上下文 + 楼中楼都可见，
                // 而不是只停在评论区顶部（item 1）。
                val targetIndex = data.indexOfFirst { it.post.get { id } == postId }
                if (targetIndex >= 0) {
                    lazyListState.animateScrollToItem(2 + targetIndex)
                } else {
                    lazyListState.animateScrollToItem(1)
                }
            } else {
                // 注意：普通「点回复数 / 跳到评论区」（scrollToReply=false 或没带 pid）才停在楼主正文下方第一条回复处。
                // 直接点帖子进来时 scrollToReply=false，必须停在楼主正文，绝不能自动跳到评论区。
                lazyListState.animateScrollToItem(1)
            }
        }
    }
    viewModel.onEvent<ThreadUiEvent.AddFavoriteSuccess> {
        scaffoldState.snackbarHostState.showSnackbar(
            context.getString(R.string.message_add_favorite_success, it.floor)
        )
    }
    viewModel.onEvent<ThreadUiEvent.RemoveFavoriteSuccess> {
        scaffoldState.snackbarHostState.showSnackbar(
            context.getString(R.string.message_remove_favorite_success)
        )
    }

    onGlobalEvent<GlobalEvent.ReplySuccess>(
        filter = { it.threadId == threadId }
    ) { event ->
        viewModel.send(
            ThreadUiIntent.LoadMyLatestReply(
                threadId = threadId,
                postId = event.newPostId,
                forumId = curForumId,
                isDesc = curSortType == ThreadSortType.SORT_TYPE_DESC,
                curLatestPostFloor = if (curSortType == ThreadSortType.SORT_TYPE_DESC) {
                    data.firstOrNull()?.post?.get { floor } ?: 1
                } else {
                    data.lastOrNull()?.post?.get { floor } ?: 1
                },
                curPostIds = data.map { it.post.get { id } },
            )
        )
    }

    val updateCollectMarkDialogState = rememberDialogState()
    var collectPromptHandled by remember { mutableStateOf(false) }
    var readFloorBeforeBack by remember {
        mutableIntStateOf(1)
    }
    ConfirmDialog(
        dialogState = updateCollectMarkDialogState,
        onConfirm = {
            collectPromptHandled = true
            coroutineScope.launch {
                navigator.navigateUp()
                if (lastVisibilityPostId != 0L) {
                    TiebaApi.getInstance()
                        .addStoreFlow(threadId, lastVisibilityPostId)
                        .catch {
                            context.toastShort(
                                R.string.message_update_collect_mark_failed,
                                it.getErrorMessage()
                            )
                        }
                        .collect {
                            context.toastShort(R.string.message_update_collect_mark_success)
                        }
                }
            }
        },
        onCancel = {
            collectPromptHandled = true
            navigator.navigateUp()
        },
        onDismiss = {
            if (!collectPromptHandled) {
                collectPromptHandled = true
                navigator.navigateUp()
            }
        }
    ) {
        Text(text = stringResource(R.string.message_update_collect_mark, readFloorBeforeBack))
    }
    MyBackHandler(
        enabled = isCollected && !bottomSheetState.isVisible && !updateCollectMarkDialogState.show,
        currentScreen = ThreadPageDestination
    ) {
        readFloorBeforeBack = lastVisibilityPost?.get { floor } ?: 0
        if (readFloorBeforeBack != 0) {
            updateCollectMarkDialogState.show()
        } else {
            navigator.navigateUp()
        }
    }

    val confirmDeleteDialogState = rememberDialogState()
    var deletePost by remember { mutableStateOf<ImmutableHolder<Post>?>(null) }
    ConfirmDialog(
        dialogState = confirmDeleteDialogState,
        onConfirm = {
            curForumId ?: return@ConfirmDialog
            if (deletePost == null) {
                val isSelfThread = author?.get { id } == user.get { id }
                viewModel.send(
                    ThreadUiIntent.DeleteThread(
                        forumId = curForumId,
                        forumName = curForumName.orEmpty(),
                        threadId = threadId,
                        deleteMyThread = isSelfThread,
                        tbs = curTbs
                    )
                )
            } else {
                val isSelfPost = deletePost!!.get { author_id } == user.get { id }
                viewModel.send(
                    ThreadUiIntent.DeletePost(
                        forumId = curForumId,
                        forumName = curForumName.orEmpty(),
                        threadId = threadId,
                        postId = deletePost!!.get { id },
                        deleteMyPost = isSelfPost,
                        tbs = curTbs
                    )
                )
            }
        }
    ) {
        Text(
            text = stringResource(
                id = R.string.message_confirm_delete,
                if (deletePost == null) stringResource(id = R.string.this_thread)
                else stringResource(id = R.string.tip_post_floor, deletePost!!.get { floor })
            )
        )
    }

    val jumpToPageDialogState = rememberDialogState()
    PromptDialog(
        onConfirm = {
            viewModel.send(
                ThreadUiIntent.Load(
                    threadId = threadId,
                    forumId = forum?.get { id } ?: forumId,
                    page = it.toInt(),
                    seeLz = isSeeLz,
                    sortType = curSortType
                )
            )
        },
        dialogState = jumpToPageDialogState,
        onValueChange = { newVal, _ -> "^[0-9]*$".toRegex().matches(newVal) },
        title = { Text(text = stringResource(id = R.string.title_jump_page)) },
        content = {
            Text(
                text = stringResource(
                    id = R.string.tip_jump_page,
                    currentPageMax,
                    totalPage
                )
            )
        }
    )

    LaunchedEffect(Unit) {
        if (from == ThreadPageFrom.FROM_STORE && extra is ThreadPageFromStoreExtra && extra.maxPid != postId) {
            val result = scaffoldState.snackbarHostState.showSnackbar(
                context.getString(R.string.message_store_thread_update, extra.maxFloor),
                context.getString(R.string.button_load_new),
                SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.send(
                    ThreadUiIntent.Load(
                        threadId,
                        page = 0,
                        postId = extra.maxPid,
                        forumId = forumId,
                        seeLz = seeLz,
                        sortType = sortType
                    )
                )
            }
        }
    }

    var savedHistory by remember { mutableStateOf(false) }
    LaunchedEffect(threadId, threadTitle, author, lastVisibilityPostId) {
        if ((!savedHistory || lastVisibilityPostId != 0L) && !context.appPreferences.incognitoMode) {
            runCatching {
                if (threadTitle.isNotBlank()) {
                    HistoryUtil.saveHistory(
                        History(
                            title = threadTitle,
                            data = threadId.toString(),
                            type = HistoryUtil.TYPE_THREAD,
                            extras = ThreadHistoryInfoBean(
                                isSeeLz = isSeeLz,
                                pid = lastVisibilityPostId.toString(),
                                forumName = forum?.get { name },
                                floor = lastVisibilityPost?.get { floor }?.toString()
                            ).toJson(),
                            avatar = StringUtil.getAvatarUrl(author?.get { portrait }),
                            username = author?.get { nameShow }
                        )
                    )
                    savedHistory = true
                    Log.i("ThreadPage", "saveHistory $lastVisibilityPostId")
                }
            }
        }
    }

    // 安全模式提示：进入帖子详情页时弹出（已登录才提示），提供「使用官方客户端」与「关闭安全模式」
    // 用户点过「不再提示」后不再弹出（safeModeDialogDontShow == true）
    val safeModeWarningDialogState = rememberDialogState()
    val isLoggedIn = LocalAccount.current != null
    LaunchedEffect(threadId, isLoggedIn) {
        if (context.appPreferences.safeMode &&
            isLoggedIn &&
            !context.appPreferences.safeModeDialogDontShow
        ) {
            safeModeWarningDialogState.show()
        }
    }
    Dialog(
        dialogState = safeModeWarningDialogState,
        title = { Text(text = stringResource(id = R.string.title_dialog_safe_mode)) },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.btn_use_official_client),
                onClick = { launchOfficialAppForThread(context, threadId, postId) }
            )
            DialogNegativeButton(
                text = stringResource(id = R.string.btn_turn_off_safe_mode),
                onClick = {
                    context.appPreferences.safeMode = false
                    // 关闭安全模式后重置「不再提示」，下次重新开启时仍会提醒一次
                    context.appPreferences.safeModeDialogDontShow = false
                    context.toastShort(R.string.toast_safe_mode_disabled)
                }
            )
            DialogNegativeButton(
                text = stringResource(id = R.string.btn_cancel_safe_mode),
                onClick = { }
            )
            DialogNegativeButton(
                text = stringResource(id = R.string.btn_safe_mode_dont_show_again),
                onClick = {
                    context.appPreferences.safeModeDialogDontShow = true
                    context.toastShort(R.string.toast_safe_mode_dont_show_again)
                }
            )
        },
    ) {
        Text(
            text = stringResource(id = R.string.message_dialog_safe_mode),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            viewModel.send(
                ThreadUiIntent.LoadFirstPage(
                    threadId,
                    forumId,
                    isSeeLz,
                    curSortType
                )
            )
        }
    )

    @Composable
    fun PollWidget(
        pollInfo: PollInfo,
        onPollSubmit: (selectedIds: Set<Int>) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var selectedIds by remember { mutableStateOf(setOf<Int>()) }

        val currentTime = (System.currentTimeMillis() / 1000).toInt()
        val isTimeExpired = pollInfo.end_time in 1..currentTime
        // 投票结果与票数始终可见（未登录/游客态同样可见，与官方客户端一致）；
        // 只有「已登录 + 未投过 + 未截止 + 状态正常」时才允许选择并提交投票。
        val isLoggedIn = LocalAccount.current != null
        val canVote = isLoggedIn && pollInfo.is_polled != 1 && !isTimeExpired && pollInfo.status == 0
        // total_poll 可能为 0（服务端未下发或无人参与），直接相除会得到 NaN
        val totalPoll = pollInfo.total_poll

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            // 标题与类型标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pollInfo.title.ifEmpty { "投票" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = if (pollInfo.is_multi == 1) "多选" else "单选",
                    fontSize = 11.sp,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colors.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            if (pollInfo.tips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pollInfo.tips,
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            pollInfo.options.forEach { option ->
                val ratio = if (totalPoll > 0) option.num.toFloat() / totalPoll else 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = ratio.coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 500),
                    label = "pollRatioAnim"
                )

                val isUserVotedOption =
                    pollInfo.polled_value.split(",").contains(option.id.toString())
                val isHighlighted =
                    selectedIds.contains(option.id) || (pollInfo.is_polled == 1 && isUserVotedOption)
                val itemBackground =
                    if (isHighlighted) MaterialTheme.colors.primary.copy(alpha = 0.08f) else MaterialTheme.colors.surface
                val itemBorderColor =
                    if (isHighlighted) MaterialTheme.colors.primary.copy(alpha = 0.7f) else MaterialTheme.colors.surface.copy(
                        alpha = 0.15f
                    )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(itemBackground)
                        .border(1.dp, itemBorderColor, RoundedCornerShape(8.dp))
                        .clickable {
                            if (canVote) {
                                selectedIds = if (pollInfo.is_multi != 1) {
                                    setOf(option.id)
                                } else {
                                    if (selectedIds.contains(option.id)) {
                                        selectedIds - option.id
                                    } else {
                                        selectedIds + option.id
                                    }
                                }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(MaterialTheme.colors.primary.copy(alpha = 0.15f))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.text,
                            fontSize = 14.sp,
                            color = if (isHighlighted) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
                            fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${option.num}票 (${(ratio * 100).toInt()}%)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "共有 ${pollInfo.total_num} 人参与",
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface
                )

                if (canVote) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selectedIds.isNotEmpty()) MaterialTheme.colors.primary
                                else MaterialTheme.colors.primary.copy(alpha = 0.4f)
                            )
                            .debounceClickable(
                                enabled = selectedIds.isNotEmpty(),
                                onClick = {
                                    onPollSubmit(selectedIds)
                                })
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "投票",
                            color = MaterialTheme.colors.onPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = when {
                            isTimeExpired -> "投票已截止"
                            pollInfo.is_polled == 1 -> "已参与投票"
                            else -> "登录后可参与投票"
                        },
                        fontSize = 12.sp,
                        color = if (isTimeExpired) MaterialTheme.colors.error
                        else if (pollInfo.is_polled == 1) MaterialTheme.colors.primary
                        else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    @Composable
    fun PostCard(
        item: ImmutableHolder<Post>,
        contentRenders: ImmutableList<PbContentRender>,
        subPosts: ImmutableList<SubPostItemData>,
        blocked: Boolean,
    ) {
        PostCard(
            postHolder = item,
            contentRenders = contentRenders,
            subPosts = subPosts,
            threadAuthorId = author?.get { id } ?: 0L,
            blocked = blocked,
            canDelete = { it.author_id == user.get { id } },
            immersiveMode = isImmersiveMode,
            isCollected = { it.id == thread?.get { collectMarkPid.toLongOrNull() } },
            onUserClick = {
                navigator.navigate(UserProfilePageDestination(it.id))
            },
            onAgree = {
                val postHasAgreed =
                    item.get { agree?.hasAgree == 1 }
                viewModel.send(
                    ThreadUiIntent.AgreePost(
                        threadId = threadId,
                        postId = item.get { id },
                        agree = !postHasAgreed
                    )
                )
            },
            onReplyClick = {
                navigator.navigate(
                    ReplyPageDestination(
                        forumId = curForumId ?: 0,
                        forumName = forum?.get { name } ?: "",
                        threadId = threadId,
                        postId = it.id,
                        replyUserId = it.author?.id ?: it.author_id,
                        replyUserName = it.author?.nameShow.takeIf { name -> !name.isNullOrEmpty() }
                            ?: it.author?.name,
                        replyUserPortrait = it.author?.portrait,
                    )
                )
            },
            glassmio = glassmio,
            onSubPostReplyClick = { post, subPost ->
                navigator.navigate(
                    ReplyPageDestination(
                        forumId = curForumId ?: 0,
                        forumName = forum?.get { name } ?: "",
                        threadId = threadId,
                        postId = post.id,
                        subPostId = subPost.id,
                        replyUserId = subPost.author?.id ?: subPost.author_id,
                        replyUserName = subPost.author?.nameShow.takeIf { name -> !name.isNullOrEmpty() }
                            ?: subPost.author?.name,
                        replyUserPortrait = subPost.author?.portrait,
                    )
                )
            },
            onOpenSubPosts = {
                if (curForumId != null) {
                    navigator.navigate(
                        SubPostsSheetPageDestination(
                            forumId = curForumId,
                            threadId = threadId,
                            postId = item.get { id },
                            subPostId = it,
                            loadFromSubPost = false
                        )
                    )
                }
            },
            onMenuCopyClick = {
                navigator.navigate(
                    CopyTextDialogPageDestination(it)
                )
            },
            onMenuFavoriteClick = {
                val isPostCollected =
                    it.id == thread?.get { collectMarkPid.toLongOrNull() }
                val fid = forum?.get { id } ?: forumId
                val tbs = anti?.get { tbs }
                if (fid != null) {
                    if (isPostCollected) {
                        viewModel.send(
                            ThreadUiIntent.RemoveFavorite(
                                threadId = threadId,
                                forumId = fid,
                                tbs = tbs
                            )
                        )
                    } else {
                        viewModel.send(
                            ThreadUiIntent.AddFavorite(
                                threadId = threadId,
                                postId = it.id,
                                floor = it.floor
                            )
                        )
                    }
                }
            },
            onMenuDeleteClick = {
                deletePost = it.wrapImmutable()
                confirmDeleteDialogState.show()
            },
        )
    }

    fun LazyListScope.latestPosts(desc: Boolean) {
        if (latestPosts.isNotEmpty()) {
            if (!desc) {
                item("LatestPostsTip") {
                    Container {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VerticalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = stringResource(id = R.string.below_is_latest_post),
                                color = AppTokens.textSecondary,
                                style = MaterialTheme.typography.caption,
                            )
                            VerticalDivider(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            items(
                items = latestPosts,
                key = { (item) -> "LatestPost_${item.get { id }}" },
                contentType = { "latestPost" }
            ) { postItem ->
                // 命中屏蔽词的楼层直接不渲染
                if (postItem.hidden) return@items
                Container {
                    PostCard(
                        postItem.post,
                        postItem.contentRenders,
                        postItem.subPosts,
                        postItem.blocked
                    )
                }
            }
            if (desc) {
                item("LatestPostsTip") {
                    Container {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VerticalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = stringResource(id = R.string.above_is_latest_post),
                                color = AppTokens.textSecondary,
                                style = MaterialTheme.typography.caption,
                            )
                            VerticalDivider(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // 页面「更多菜单」的唯一一份内容：LEGACY 走 ModalBottomSheet，GLASMIO 走 AppMenu 下拉。
    // 每个动作先 dismiss 再执行（跳页是先关菜单再弹输入框），所以这里不再各自调 closeBottomSheet。
    val threadMenuContent: @Composable (onDismiss: () -> Unit) -> Unit = { onDismiss ->
        ThreadMenu(
            isSeeLz = isSeeLz,
            isCollected = isCollected,
            isImmersiveMode = isImmersiveMode,
            isDesc = curSortType == ThreadSortType.SORT_TYPE_DESC,
            canDelete = { author?.get { id } == user.get { id } },
            onDismiss = onDismiss,
            showHandle = !glassmio,
            onSeeLzClick = {
                viewModel.send(
                    ThreadUiIntent.LoadFirstPage(
                        threadId,
                        forumId,
                        !isSeeLz,
                        curSortType
                    )
                )
            },
            onCollectClick = {
                if (isCollected) {
                    val fid = forum?.get { id } ?: forumId
                    val tbs = anti?.get { tbs }
                    if (fid != null) {
                        viewModel.send(
                            ThreadUiIntent.RemoveFavorite(
                                threadId,
                                fid,
                                tbs
                            )
                        )
                    }
                } else {
                    val readItem = lastVisibilityPost
                    if (readItem != null) {
                        viewModel.send(
                            ThreadUiIntent.AddFavorite(
                                threadId,
                                readItem.get { id },
                                readItem.get { floor }
                            )
                        )
                    }
                }
            },
            onImmersiveModeClick = {
                if (!isImmersiveMode && !isSeeLz) {
                    viewModel.send(
                        ThreadUiIntent.LoadFirstPage(
                            threadId,
                            forumId,
                            true,
                            curSortType
                        )
                    )
                }
                viewModel.send(ThreadUiIntent.ToggleImmersiveMode(!isImmersiveMode))
            },
            onDescClick = {
                viewModel.send(
                    ThreadUiIntent.LoadFirstPage(
                        threadId,
                        forumId,
                        isSeeLz,
                        if (curSortType != ThreadSortType.SORT_TYPE_DESC) ThreadSortType.SORT_TYPE_DESC else ThreadSortType.SORT_TYPE_DEFAULT
                    )
                )
            },
            onJumpPageClick = {
                jumpToPageDialogState.show()
            },
            onShareClick = {
                TiebaUtil.shareText(
                    context,
                    "https://tieba.baidu.com/p/$threadId",
                    threadTitle
                )
            },
            onCopyLinkClick = {
                TiebaUtil.copyText(
                    context,
                    "https://tieba.baidu.com/p/$threadId?see_lz=${isSeeLz.booleanToString()}"
                )
            },
            onReportClick = {
                val firstPostId =
                    thread?.get { firstPostId }.takeIf { it != 0L }
                        ?: firstPost?.get { id }
                        ?: 0L
                coroutineScope.launch {
                    TiebaUtil.reportPost(
                        context,
                        navigator,
                        firstPostId.toString()
                    )
                }
            },
            onDeleteClick = {
                deletePost = null
                confirmDeleteDialogState.show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (glassmio) 8.dp else 16.dp)
                .defaultMinSize(minHeight = 1.dp)
        )
    }

    ProvideNavigator(navigator = navigator) {
        ThreadPageSkin(
            enabled = glassmio,
            topBar = {
                ThreadGlassTopBar(
                    forumName = threadForumName,
                    scrimAlpha = topScrimAlpha,
                    onBack = { navigator.navigateUp() },
                    onForumClick = {
                        val forumName = curForumName
                        if (forumName != null) navigator.navigate(
                            ForumPageDestination(forumName)
                        )
                    },
                    menuContent = { threadMenuContent { dismiss() } },
                )
            },
            bottomBar = {
                ThreadGlassBottomBar(
                    onClickReply = onReplyClick,
                    onAgree = onAgreeClick,
                    onReplyCountClick = onReplyCountClick,
                    onSeeLzClick = onSeeLzClick,
                    onSortClick = onSortClick,
                    hasAgreed = hasThreadAgreed,
                    agreeNum = threadAgreeNum,
                    replyNum = (thread?.get { replyNum - 1 } ?: 0).coerceAtLeast(0),
                    isSeeLz = isSeeLz,
                    isDesc = curSortType == ThreadSortType.SORT_TYPE_DESC,
                )
            },
        ) {
            StateScreen(
                modifier = Modifier.fillMaxSize(),
                isEmpty = isEmpty,
                isError = isError,
                isLoading = isRefreshing || (isEmpty && !isError),
                loadingScreen = {
                    ThreadSkeleton(glassmio = glassmio)
                },
                errorScreen = {
                    error?.let {
                        val (e) = it
                        ErrorScreen(error = e)
                    }
                },
                onReload = {
                    viewModel.send(
                        ThreadUiIntent.Load(
                            threadId,
                            page = 0,
                            postId = postId,
                            forumId = forumId,
                            seeLz = seeLz,
                            sortType = sortType
                        )
                    )
                }
            ) {
                CompositionLocalProvider(LocalThreadImagesEnabled provides imagesEnabled) {
                MyScaffold(
                    scaffoldState = scaffoldState,
                    backgroundColor = if (glassmio) Color.Transparent else MaterialTheme.colors.background,
                    topBar = {
                        // GLASMIO 的顶栏是 overlay（见 ThreadPageSkin 的 topBar 槽），
                        // Scaffold 这里不再占高度，内容才能从顶栏下穿过。
                        if (!glassmio) {
                            TopBar(
                                forum = forum,
                                onBack = { navigator.navigateUp() },
                                onForumClick = {
                                    val forumName = forum?.get { name }
                                    if (forumName != null) navigator.navigate(
                                        ForumPageDestination(
                                            forumName
                                        )
                                    )
                                }
                            )
                        }
                    },
                bottomBar = {
                    // GLASMIO 的底栏在 ThreadPageSkin 的 overlay 槽里（能采真实内容）；
                    // 这里只给 LEGACY 保留旧的 Scaffold 底栏。
                    if (!glassmio) {
                        BottomBar(
                            user = user,
                            onClickReply = onReplyClick,
                            onAgree = onAgreeClick,
                            onClickMore = onMoreClick,
                            hasAgreed = hasThreadAgreed,
                            agreeNum = threadAgreeNum,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {}
                                )
                        )
                    }
                },
            ) { paddingValues ->
                ModalBottomSheetLayout(
                    sheetState = bottomSheetState,
                    sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    sheetBackgroundColor = AppTokens.windowBackground,
                    sheetContent = {
                        // LEGACY 仍然用这张 M2 bottom sheet；GLASMIO 走顶栏/底栏的 AppMenu，
                        // sheet 永远保持隐藏（保留状态机只是为了不动旧分支）。
                        if (!glassmio) {
                            threadMenuContent { closeBottomSheet() }
                        }
                    },
                    scrimColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Box(
                        modifier = Modifier
                            .pullRefresh(state = pullRefreshState, enabled = enablePullRefresh)
                    ) {
                        LoadMoreLayout(
                            isLoading = isLoadingMore,
                            onLoadMore = {
                                if (hasMore) {
                                    viewModel.send(
                                        ThreadUiIntent.LoadMore(
                                            threadId = threadId,
                                            page = if (curSortType == ThreadSortType.SORT_TYPE_DESC) totalPage - currentPageMax
                                            else currentPageMax + 1,
                                            forumId = forumId,
                                            postId = nextPagePostId,
                                            seeLz = isSeeLz,
                                            sortType = curSortType,
                                            postIds = data.map { it.post.get { id } }
                                        )
                                    )
                                } else if (data.isNotEmpty() && curSortType != ThreadSortType.SORT_TYPE_DESC) {
                                    viewModel.send(
                                        ThreadUiIntent.LoadLatestPosts(
                                            threadId = threadId,
                                            curLatestPostId = data.last().post.get { id },
                                            forumId = curForumId,
                                            seeLz = isSeeLz,
                                            sortType = curSortType
                                        )
                                    )
                                }
                            },
                            loadEnd = loadMoreEnd,
                            indicator = { isLoading, loadMoreEnd, willLoad ->
                                ThreadLoadMoreIndicator(
                                    isLoading,
                                    loadMoreEnd,
                                    willLoad,
                                    hasMore
                                )
                            },
                            lazyListState = lazyListState,
                            isEmpty = data.isEmpty(),
                            preloadCount = loadMorePreloadCount,
                        ) {
                            MyLazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(
                                    top = if (glassmio) threadTopInset else 0.dp,
                                    // overlay 底栏不再占 Scaffold 高度：把「胶囊 + 边距 + 导航栏」
                                    // 放进 contentPadding，最后一条能滚到胶囊上方，滚动时内容从底下穿过。
                                    bottom = threadBottomInset,
                                ),
                            ) {
                                item(key = "FirstPost") {
                                    if (firstPost != null) {
                                        Container {
                                            Column {
                                                PostCard(
                                                    postHolder = firstPost!!,
                                                    contentRenders = firstPostContentRenders,
                                                    canDelete = { it.author_id == user.get { id } },
                                                    immersiveMode = isImmersiveMode,
                                                    isCollected = {
                                                        it.id == thread?.get { collectMarkPid }
                                                            ?.toLongOrNull()
                                                    },
                                                    showSubPosts = false,
                                                    onUserClick = {
                                                        navigator.navigate(
                                                            UserProfilePageDestination(
                                                                it.id
                                                            )
                                                        )
                                                    },
                                                    onReplyClick = {
                                                        navigator.navigate(
                                                            ReplyPageDestination(
                                                                forumId = curForumId ?: 0,
                                                                forumName = forum?.get { name }
                                                                    .orEmpty(),
                                                                threadId = threadId,
                                                            )
                                                        )
                                                    },
                                                    onMenuCopyClick = {
                                                        navigator.navigate(
                                                            CopyTextDialogPageDestination(it)
                                                        )
                                                    },
                                                    onMenuFavoriteClick = {
                                                        viewModel.send(
                                                            ThreadUiIntent.AddFavorite(
                                                                threadId,
                                                                it.id,
                                                                it.floor
                                                            )
                                                        )
                                                    },
                                                    glassmio = glassmio,
                                                ) {
                                                    deletePost = null
                                                    confirmDeleteDialogState.show()
                                                }

                                                thread?.getNullableImmutable { origin_thread_info }
                                                    .takeIf { thread?.get { is_share_thread } == 1 }
                                                    ?.let {
                                                        OriginThreadCard(
                                                            originThreadInfo = it,
                                                            modifier = Modifier
                                                                .padding(horizontal = 16.dp)
                                                                .padding(bottom = 16.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(AppTokens.floorCard)
                                                                .debounceClickable(onClick = {
                                                                    navigator.navigate(
                                                                        ThreadPageDestination(
                                                                            threadId = it.get { tid.toLong() },
                                                                            forumId = it.get { fid },
                                                                        )
                                                                    )
                                                                })
                                                                .padding(16.dp)
                                                        )
                                                    }

                                                thread?.let { resolvePollInfo(it.get()) }?.let { pollInfo ->
                                                    PollWidget(
                                                        pollInfo,
                                                        { selectedIds ->
                                                            viewModel.send(
                                                                ThreadUiIntent.PollThread(
                                                                    curForumId,
                                                                    threadId,
                                                                    selectedIds.joinToString(separator = ",")
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (glassmio) {
                                    item(key = "ThreadHeader") {
                                        // 原来的「回复数 / 只看楼主 / 正序倒序」整行已移到玻璃底栏。
                                        // 保留一个 0 高度 item，维持 animateScrollToItem(1/2/+n) 的既有 index 语义。
                                    }
                                } else {
                                    stickyHeader(key = "ThreadHeader") {
                                        Container {
                                            Row(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colors.background)
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
    //                                            Text(
    //                                                text = stringResource(
    //                                                    R.string.title_thread_header,
    //                                                    "${thread?.get { replyNum - 1 } ?: 0}"),
    //                                                fontSize = 13.sp,
    //                                                fontWeight = FontWeight.Bold,
    //                                                color = AppTokens.text,
    //                                                modifier = Modifier.padding(horizontal = 8.dp),
    //                                            )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.height(IntrinsicSize.Min)
                                                ) {
                                                    Text(
                                                        text = stringResource(
                                                            R.string.title_thread_header,
                                                            "${thread?.get { replyNum - 1 } ?: 0}"),
                                                        modifier = Modifier
                                                            .padding(horizontal = 8.dp)
                                                            .debounceClickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = null,
                                                                enabled = isSeeLz,
                                                                onClick = {
                                                                    if (isSeeLz) {
                                                                        viewModel.send(
                                                                            ThreadUiIntent.LoadFirstPage(
                                                                                threadId = threadId,
                                                                                forumId = forumId,
                                                                                seeLz = false,
                                                                                sortType = curSortType
                                                                            )
                                                                        )
                                                                    }
                                                                }),
                                                        fontSize = 13.sp,
                                                        fontWeight = if (!isSeeLz) FontWeight.SemiBold else FontWeight.Normal,
                                                        color = if (!isSeeLz) AppTokens.text else AppTokens.textSecondary,
                                                    )
                                                    HorizontalDivider()
                                                    Text(
                                                        text = stringResource(R.string.title_see_lz),
                                                        modifier = Modifier
                                                            .padding(horizontal = 8.dp)
                                                            .debounceClickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                enabled = !isSeeLz,
                                                                indication = null,
                                                                onClick =
                                                                    {
                                                                        if (!isSeeLz) {
                                                                            viewModel.send(
                                                                                ThreadUiIntent.LoadFirstPage(
                                                                                    threadId = threadId,
                                                                                    forumId = forumId,
                                                                                    seeLz = true,
                                                                                    sortType = curSortType
                                                                                )
                                                                            )
                                                                        }
                                                                    }),
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSeeLz) FontWeight.SemiBold else FontWeight.Normal,
                                                        color = if (isSeeLz) AppTokens.text else AppTokens.textSecondary,
                                                    )
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.height(IntrinsicSize.Min)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.title_asc),
                                                        modifier = Modifier
                                                            .padding(horizontal = 8.dp)
                                                            .debounceClickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                enabled = (curSortType == 1),
                                                                indication = null,
                                                                onClick =
                                                                    {
                                                                        if (curSortType == 1) {
                                                                            viewModel.send(
                                                                                ThreadUiIntent.LoadFirstPage(
                                                                                    threadId = threadId,
                                                                                    forumId = forumId,
                                                                                    seeLz = isSeeLz,
                                                                                    sortType = 0
                                                                                )
                                                                            )
                                                                        }
                                                                    }),
                                                        fontSize = 13.sp,
                                                        fontWeight = if (curSortType == 0) FontWeight.SemiBold else FontWeight.Normal,
                                                        color = if (curSortType == 0) AppTokens.text else AppTokens.textSecondary,
                                                    )
                                                    HorizontalDivider()
                                                    Text(
                                                        text = stringResource(R.string.title_desc),
                                                        modifier = Modifier
                                                            .padding(horizontal = 8.dp)
                                                            .debounceClickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                enabled = (curSortType == 0),
                                                                indication = null,
                                                                onClick =
                                                                    {
                                                                        if (curSortType == 0) {
                                                                            viewModel.send(
                                                                                ThreadUiIntent.LoadFirstPage(
                                                                                    threadId = threadId,
                                                                                    forumId = forumId,
                                                                                    seeLz = isSeeLz,
                                                                                    sortType = 1
                                                                                )
                                                                            )
                                                                        }
                                                                    }),
                                                        fontSize = 13.sp,
                                                        fontWeight = if (curSortType == 1) FontWeight.SemiBold else FontWeight.Normal,
                                                        color = if (curSortType == 1) AppTokens.text else AppTokens.textSecondary,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (curSortType == ThreadSortType.SORT_TYPE_DESC) {
                                    latestPosts(true)
                                }
                                item(key = "LoadPreviousBtn") {
                                    if (hasPrevious) {
                                        Container {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .debounceClickable(onClick = {
                                                        viewModel.send(
                                                            ThreadUiIntent.LoadPrevious(
                                                                threadId,
                                                                max(currentPageMax - 1, 1),
                                                                forumId,
                                                                postId = data
                                                                    .first()
                                                                    .post
                                                                    .get { id },
                                                                seeLz = isSeeLz,
                                                                sortType = curSortType,
                                                                postIds = data.map { it.post.get { id } }
                                                            )
                                                        )
                                                    })
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AlignVerticalTop,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    text = stringResource(id = R.string.btn_load_previous),
                                                    color = AppTokens.text,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                                if (!isRefreshing && data.isEmpty()) {
                                    item(key = "EmptyTip") {
                                        Container {
                                            TipScreen(
                                                title = { Text(text = stringResource(id = R.string.title_empty)) },
                                                image = {
                                                    val composition by rememberLottieComposition(
                                                        LottieCompositionSpec.RawRes(R.raw.lottie_empty_box)
                                                    )
                                                    LottieAnimation(
                                                        composition = composition,
                                                        iterations = LottieConstants.IterateForever,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .aspectRatio(2f)
                                                    )
                                                },
                                                actions = {
                                                    if (canReload) {
                                                        Button(onClick = { reload() }) {
                                                            Text(text = stringResource(id = R.string.btn_refresh))
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxSize(),
                                                scrollable = false
                                            )
                                        }
                                    }
                                } else {
                                    items(
                                        items = data,
                                        key = { (item) -> "Post_${item.get { id }}" },
                                        contentType = { "post" }
                                    ) { postItem ->
                                        // 命中屏蔽词的楼层直接不渲染
                                        if (postItem.hidden) return@items
                                        Container {
                                            PostCard(
                                                postItem.post,
                                                postItem.contentRenders,
                                                postItem.subPosts,
                                                postItem.blocked
                                            )
                                        }
                                    }
                                }
                                if (curSortType != ThreadSortType.SORT_TYPE_DESC) {
                                    latestPosts(false)
                                }
                            }
                        }

                        PullRefreshIndicator(
                            refreshing = isRefreshing,
                            state = pullRefreshState,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = if (glassmio) threadTopInset else 0.dp),
                            backgroundColor = AppTokens.pullRefreshIndicator,
                            contentColor = AppTokens.primary,
                        )
                    }
                }
                }
            }
            }
        }
    }
}

@Composable
private fun TopBar(
    forum: ImmutableHolder<SimpleForum>?,
    onBack: () -> Unit,
    onForumClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TitleCentredToolbar(
        title = {
            forum?.let {
                if (forum.get { name }.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 48.dp)
                            .height(IntrinsicSize.Min)
                            .clip(RoundedCornerShape(100))
                            .background(AppTokens.chip)
                            .debounceClickable(onClick = onForumClick)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(
                            data = forum.get { avatar },
                            contentDescription = it.get { name },
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                        )

                        Text(
                            text = stringResource(id = R.string.title_forum, it.get { name }),
                            fontSize = 14.sp,
                            color = AppTokens.text,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        navigationIcon = {
            BackNavigationIcon(onBack)
        },
        modifier = modifier
    )
}

@Composable
private fun BottomBar(
    user: ImmutableHolder<User>,
    onClickReply: () -> Unit,
    onAgree: () -> Unit,
    onClickMore: () -> Unit,
    modifier: Modifier = Modifier,
    hasAgreed: Boolean = false,
    agreeNum: Long = 0,
) {
    Column(
        modifier = Modifier.background(AppTokens.threadBottomBar)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .then(modifier)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (user.get { is_login } == 1 && !LocalContext.current.appPreferences.safeMode) {
                Avatar(
                    data = StringUtil.getAvatarUrl(user.get { portrait }),
                    size = Sizes.Tiny,
                    contentDescription = user.get { name },
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppTokens.bottomBarSurface)
                        .debounceClickable(onClick = onClickReply)
                        .padding(8.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.tip_reply_thread),
                        style = MaterialTheme.typography.caption,
                        color = AppTokens.onBottomBarSurface,
                    )
                }
            } else {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                )
            }

            BottomBarAgreeBtn(
                hasAgreed = hasAgreed,
                agreeNum = agreeNum,
                onClick = onAgree,
                modifier = Modifier.fillMaxHeight()
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .debounceClickable(onClick = onClickMore)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(id = R.string.btn_more),
                    tint = AppTokens.textSecondary,
                )
            }
        }

        Box(
            modifier = Modifier
                .requiredHeightIn(min = if (LocalContext.current.appPreferences.liftUpBottomBar) 16.dp else 0.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
            )
        }
    }
}

@Composable
private fun PostContentRenders(
    contentRenders: ImmutableList<PbContentRender>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        var waterfallImages: MutableList<PicContentRender>? = null
        contentRenders.forEach { render ->
            if (render is PicContentRender) {
                if (waterfallImages == null) waterfallImages = mutableListOf()
                waterfallImages!!.add(render)
            } else {
                waterfallImages?.let { PicWaterfallContentRender(it) }
                waterfallImages = null
                render.Render()
            }
        }
        waterfallImages?.let { PicWaterfallContentRender(it) }
    }
}

@Composable
private fun GlasmioMainPostCard(
    post: Post,
    author: User,
    contentRenders: ImmutableList<PbContentRender>,
    showTitle: Boolean,
    collected: Boolean,
    immersiveMode: Boolean,
    threadAuthorId: Long,
    onUserClick: (User) -> Unit,
) {
    val colors = GlasmioTheme.colors
    val backdrop = remember { emptyBackdrop() }

    GlasmioCard(
        backdrop = backdrop,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 9.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!immersiveMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .debounceClickable(onClick = { onUserClick(author) }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(
                        data = StringUtil.getAvatarUrl(author.portrait),
                        size = 40.dp,
                        contentDescription = stringResource(id = R.string.user_portrait),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ProvideTextStyle(
                            MaterialTheme.typography.body1.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.content,
                            )
                        ) {
                            UserNameText(
                                userName = StringUtil.getUsernameAnnotatedString(
                                    LocalContext.current,
                                    author.name,
                                    author.nameShow,
                                ),
                                userLevel = author.level_id,
                                isLz = author.id == threadAuthorId,
                                bawuType = author.bawuType,
                            )
                        }
                        Text(
                            text = getDescText(post.time.toLong(), 0, author.ip_address),
                            fontSize = 13.sp,
                            color = colors.secondaryContent,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (showTitle) {
                Text(
                    text = post.title,
                    fontSize = 17.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.content,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (collected) {
                Chip(
                    text = stringResource(id = R.string.title_collected_floor),
                    invertColor = true,
                    prefixIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            CompositionLocalProvider(LocalThreadImageRadius provides 12.dp) {
                ProvideTextStyle(
                    MaterialTheme.typography.body1.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = colors.content,
                    )
                ) {
                    PostContentRenders(contentRenders)
                }
            }
        }
    }
}

@Composable
private fun GlasmioFloorRow(
    post: Post,
    author: User,
    contentRenders: ImmutableList<PbContentRender>,
    subPosts: ImmutableList<SubPostItemData>,
    showSubPosts: Boolean,
    collected: Boolean,
    immersiveMode: Boolean,
    threadAuthorId: Long,
    hasAgreed: Boolean,
    agreeNum: Long,
    onUserClick: (User) -> Unit,
    onAgree: () -> Unit,
    onSubPostReplyClick: ((Post, SubPostList) -> Unit)?,
    onOpenSubPosts: (Long) -> Unit,
    onMenuCopyClick: ((String) -> Unit)?,
) {
    val colors = GlasmioTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        Avatar(
            data = StringUtil.getAvatarUrl(author.portrait),
            size = 36.dp,
            contentDescription = stringResource(id = R.string.user_portrait),
            modifier = Modifier.debounceClickable(onClick = { onUserClick(author) }),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (!immersiveMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .debounceClickable(onClick = { onUserClick(author) }),
                ) {
                    ProvideTextStyle(
                        MaterialTheme.typography.body1.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.content,
                        )
                    ) {
                        UserNameText(
                            userName = StringUtil.getUsernameAnnotatedString(
                                LocalContext.current,
                                author.name,
                                author.nameShow,
                            ),
                            userLevel = author.level_id,
                            isLz = author.id == threadAuthorId,
                            bawuType = author.bawuType,
                        )
                    }
                    Text(
                        text = getDescText(post.time.toLong(), post.floor, author.ip_address),
                        fontSize = 13.sp,
                        color = colors.secondaryContent,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            CompositionLocalProvider(LocalThreadImageRadius provides 12.dp) {
                ProvideTextStyle(
                    MaterialTheme.typography.body1.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = colors.content,
                    )
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (collected) {
                            Chip(
                                text = stringResource(id = R.string.title_collected_floor),
                                invertColor = true,
                                prefixIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                        PostContentRenders(contentRenders)
                    }
                }
            }

            if (showSubPosts && post.sub_post_number > 0 && subPosts.isNotEmpty() && !immersiveMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.systemGray5)
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    subPosts.fastForEach { item ->
                        if (item.hidden) return@fastForEach
                        BlockableContent(
                            blocked = item.blocked,
                            blockedTip = {
                                Text(
                                    text = stringResource(id = R.string.tip_blocked_sub_post),
                                    style = MaterialTheme.typography.body2.copy(
                                        color = colors.secondaryContent,
                                        fontSize = 13.sp,
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                )
                            },
                        ) {
                            SubPostItem(
                                subPostList = item.subPost,
                                subPostContent = item.subPostContent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                onReplyClick = {
                                    onSubPostReplyClick?.invoke(post, it)
                                },
                                onOpenSubPosts = onOpenSubPosts,
                                onMenuCopyClick = {
                                    onMenuCopyClick?.invoke(it.content.plainText)
                                },
                            )
                        }
                    }

                    if (post.sub_post_number > subPosts.size) {
                        Text(
                            text = stringResource(
                                id = R.string.open_all_sub_posts,
                                post.sub_post_number,
                            ),
                            style = MaterialTheme.typography.caption,
                            fontSize = 13.sp,
                            color = colors.accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                                .debounceClickable(onClick = { onOpenSubPosts(0) })
                                .padding(horizontal = 12.dp),
                        )
                    }
                }
            }

            if (!immersiveMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    val tint = if (hasAgreed) colors.accent else colors.secondaryContent
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100))
                            .debounceClickable(onClick = onAgree)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (hasAgreed) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = stringResource(id = R.string.title_agree),
                            tint = tint,
                            modifier = Modifier.size(18.dp),
                        )
                        if (agreeNum > 0) {
                            Text(
                                text = agreeNum.getShortNumString(),
                                fontSize = 13.sp,
                                color = tint,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    postHolder: ImmutableHolder<Post>,
    contentRenders: ImmutableList<PbContentRender>,
    subPosts: ImmutableList<SubPostItemData> = persistentListOf(),
    threadAuthorId: Long = 0L,
    blocked: Boolean = false,
    canDelete: (Post) -> Boolean = { false },
    immersiveMode: Boolean = false,
    isCollected: (Post) -> Boolean = { false },
    showSubPosts: Boolean = true,
    onUserClick: (User) -> Unit = {},
    onAgree: () -> Unit = {},
    onReplyClick: (Post) -> Unit = {},
    onSubPostReplyClick: ((Post, SubPostList) -> Unit)? = null,
    onOpenSubPosts: (subPostId: Long) -> Unit = {},
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuFavoriteClick: ((Post) -> Unit)? = null,
    glassmio: Boolean = false,
    // 楼中楼页把「被回复的楼层」也用 GlasmioCard 突出，即使 post.floor > 1。
    forceGlassCard: Boolean = false,
    onMenuDeleteClick: ((Post) -> Unit)? = null,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val account = LocalAccount.current
    val coroutineScope = rememberCoroutineScope()
    val post = remember(postHolder) { postHolder.get() }
    val hasPadding = remember(key1 = postHolder, key2 = immersiveMode) {
        postHolder.get { floor > 1 } && !immersiveMode
    }
    val paddingModifier = Modifier.padding(start = if (hasPadding) Sizes.Small + 8.dp else 0.dp)
    val author = postHolder.get { author!! }
    val showTitle = remember(postHolder) {
        post.title.isNotBlank() && post.floor <= 1 && post.is_ntitle != 1
    }
    val hasAgreed = remember(postHolder) {
        post.agree?.hasAgree == 1
    }
    val agreeNum = remember(postHolder) {
        post.agree?.diffAgreeNum ?: 0L
    }
    val menuState = rememberMenuState()
    BlockableContent(
        blocked = blocked,
        blockedTip = {
            BlockTip {
                Text(
                    text = stringResource(id = R.string.tip_blocked_post, postHolder.get { floor }),
                )
            }
        },
        hideBlockedContent = context.appPreferences.hideBlockedContent || immersiveMode,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (glassmio) Modifier
                else Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
            )
    ) {
        LongClickMenu(
            menuState = menuState,
            indication = null,
            onClick = {
                onReplyClick(post)
            }.takeIf {
                !context.appPreferences.safeMode && account != null
            },
            menuContent = {
                if (onMenuCopyClick != null) {
                    DropdownMenuItem(
                        onClick = {
                            val copyText =
                                if (post.floor == 1) post.title + "\n" + post.content.plainText else post.content.plainText
                            onMenuCopyClick(copyText)
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.menu_copy))
                    }
                }
                if (account != null) {
                    if (!context.appPreferences.safeMode) {
                        DropdownMenuItem(
                            onClick = {
                                onReplyClick(post)
                                menuState.expanded = false
                            }
                        ) {
                            Text(text = stringResource(id = R.string.btn_reply))
                        }
                    }
                    DropdownMenuItem(
                        onClick = {
                            coroutineScope.launch {
                                TiebaUtil.reportPost(context, navigator, post.id.toString())
                            }
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.title_report))
                    }
                    if (onMenuFavoriteClick != null) {
                        DropdownMenuItem(
                            onClick = {
                                onMenuFavoriteClick(post)
                                menuState.expanded = false
                            }
                        ) {
                            if (isCollected(post)) {
                                Text(text = stringResource(id = R.string.title_collect_on))
                            } else {
                                Text(text = stringResource(id = R.string.title_collect_floor))
                            }
                        }
                    }
                    if ((canDelete(post) || threadAuthorId == account.uid.toLong()) && onMenuDeleteClick != null) {
                        DropdownMenuItem(
                            onClick = {
                                onMenuDeleteClick(post)
                                menuState.expanded = false
                            }
                        ) {
                            Text(text = stringResource(id = R.string.title_delete))
                        }
                    }
                }
            }
        ) {
            if (glassmio && (post.floor <= 1 || forceGlassCard)) {
                GlasmioMainPostCard(
                    post = post,
                    author = author,
                    contentRenders = contentRenders,
                    showTitle = showTitle,
                    collected = isCollected(post),
                    immersiveMode = immersiveMode,
                    threadAuthorId = threadAuthorId,
                    onUserClick = onUserClick,
                )
            } else if (glassmio) {
                GlasmioFloorRow(
                    post = post,
                    author = author,
                    contentRenders = contentRenders,
                    subPosts = subPosts,
                    showSubPosts = showSubPosts,
                    collected = isCollected(post),
                    immersiveMode = immersiveMode,
                    threadAuthorId = threadAuthorId,
                    hasAgreed = hasAgreed,
                    agreeNum = agreeNum,
                    onUserClick = onUserClick,
                    onAgree = onAgree,
                    onSubPostReplyClick = onSubPostReplyClick,
                    onOpenSubPosts = onOpenSubPosts,
                    onMenuCopyClick = onMenuCopyClick,
                )
            } else {
                Card(
                    header = {
                    if (!immersiveMode) {
                        UserHeader(
                            avatar = {
                                Avatar(
                                    data = StringUtil.getAvatarUrl(author.portrait),
                                    size = Sizes.Small,
                                    contentDescription = stringResource(id = R.string.user_portrait)
                                )
                            },
                            name = {
                                UserNameText(
                                    userName = StringUtil.getUsernameAnnotatedString(
                                        LocalContext.current,
                                        author.name,
                                        author.nameShow
                                    ),
                                    userLevel = author.level_id,
                                    isLz = author.id == threadAuthorId,
                                    bawuType = author.bawuType,
                                )
                            },
                            desc = {
                                Text(
                                    text = getDescText(
                                        post.time.toLong(),
                                        post.floor,
                                        author.ip_address
                                    )
                                )
                            },
                            onClick = {
                                onUserClick(author)
                            }
                        ) {
                            if (post.floor > 1) {
                                PostAgreeBtn(
                                    hasAgreed = hasAgreed,
                                    agreeNum = agreeNum,
                                    onClick = onAgree
                                )
                            }
                        }
                    }
                },
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = paddingModifier
                            .fillMaxWidth()
                    ) {
                        if (showTitle) {
                            Text(
                                text = post.title,
                                style = MaterialTheme.typography.subtitle1,
                                fontSize = 15.sp
                            )
                        }

                        if (isCollected(post)) {
                            Chip(
                                text = stringResource(id = R.string.title_collected_floor),
                                invertColor = true,
                                prefixIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }

                        var waterfallImages: MutableList<PicContentRender>? = null

                        contentRenders.forEach { render ->
                            if (render is PicContentRender) {
                                if (waterfallImages == null) waterfallImages = mutableListOf()
                                waterfallImages!!.add(render)
                            } else {
                                waterfallImages?.let { PicWaterfallContentRender(it) }
                                waterfallImages = null
                                render.Render()
                            }
                        }
                        waterfallImages?.let { PicWaterfallContentRender(it) }
                    }

                    if (showSubPosts && post.sub_post_number > 0 && subPosts.isNotEmpty() && !immersiveMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(paddingModifier)
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppTokens.floorCard)
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            subPosts.fastForEach { item ->
                                // 命中屏蔽词的楼中楼直接不渲染
                                if (item.hidden) return@fastForEach
                                BlockableContent(
                                    blocked = item.blocked,
                                    blockedTip = {
                                        Text(
                                            text = stringResource(id = R.string.tip_blocked_sub_post),
                                            style = MaterialTheme.typography.body2.copy(
                                                color = AppTokens.textDisabled,
                                                fontSize = 13.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                    },
                                ) {
                                    SubPostItem(
                                        subPostList = item.subPost,
                                        subPostContent = item.subPostContent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp),
                                        onReplyClick = {
                                            onSubPostReplyClick?.invoke(post, it)
                                        },
                                        onOpenSubPosts = onOpenSubPosts,
                                        onMenuCopyClick = {
                                            onMenuCopyClick?.invoke(it.content.plainText)
                                        }
                                    )
                                }
                            }

                            if (post.sub_post_number > subPosts.size) {
                                Text(
                                    text = stringResource(
                                        id = R.string.open_all_sub_posts,
                                        post.sub_post_number
                                    ),
                                    style = MaterialTheme.typography.caption,
                                    fontSize = 13.sp,
                                    color = AppTokens.accent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp)
                                        .debounceClickable(onClick = { onOpenSubPosts(0) })
                                        .padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            )
            }
        }
    }
}

@Composable
private fun SubPostItem(
    subPostList: ImmutableHolder<SubPostList>,
    subPostContent: AnnotatedString,
    modifier: Modifier = Modifier,
    onReplyClick: ((SubPostList) -> Unit)?,
    onOpenSubPosts: (Long) -> Unit,
    onMenuCopyClick: ((SubPostList) -> Unit)?,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val menuState = rememberMenuState()
    LongClickMenu(
        menuState = menuState,
        menuContent = {
            if (onMenuCopyClick != null) {
                DropdownMenuItem(
                    onClick = {
                        onMenuCopyClick(subPostList.get())
                        menuState.expanded = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.menu_copy))
                }
            }
            if (LocalAccount.current != null) {
                if (!context.appPreferences.safeMode) {
                    DropdownMenuItem(
                        onClick = {
                            onReplyClick?.invoke(subPostList.get())
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.title_reply))
                    }
                }
                DropdownMenuItem(
                    onClick = {
                        coroutineScope.launch {
                            TiebaUtil.reportPost(
                                context,
                                navigator,
                                subPostList.get { id }.toString()
                            )
                        }
                        menuState.expanded = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.title_report))
                }
            }
        },
        shape = RoundedCornerShape(0),
        onClick = {
            onOpenSubPosts(subPostList.get { id })
        }
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.body2.copy(color = AppTokens.text)) {
            PbContentText(
                text = subPostContent,
                modifier = modifier,
                fontSize = 13.sp,
                emoticonSize = 0.9f,
                overflow = TextOverflow.Ellipsis,
                maxLines = 4,
                lineSpacing = 0.4.sp,
                inlineContent = mapOf(
                    "Lz" to buildChipInlineContent(
                        stringResource(id = R.string.tip_lz),
                        backgroundColor = AppTokens.textSecondary.copy(alpha = 0.1f),
                        color = AppTokens.textSecondary
                    ),
                )
            )
        }
    }
}

@Composable
fun UserNameText(
    userName: AnnotatedString,
    userLevel: Int,
    modifier: Modifier = Modifier,
    isLz: Boolean = false,
    bawuType: String? = null,
) {
    val text = buildAnnotatedString {
        append(userName)
        append(" ")
        if (userLevel > 0) appendInlineContent("Level", alternateText = "$userLevel")
        if (!bawuType.isNullOrBlank()) {
            append(" ")
            appendInlineContent("Bawu", alternateText = bawuType)
        }
        if (isLz) {
            append(" ")
            appendInlineContent("Lz")
        }
    }
    Text(
        text = text,
        inlineContent = mapOf(
            "Level" to buildChipInlineContent(
                "18",
                color = Color(getIconColorByLevel("$userLevel")),
                backgroundColor = Color(getIconColorByLevel("$userLevel")).copy(alpha = 0.25f)
            ),
            "Bawu" to buildChipInlineContent(
                bawuType ?: "",
                color = AppTokens.primary,
                backgroundColor = AppTokens.primary.copy(alpha = 0.1f)
            ),
            "Lz" to buildChipInlineContent(stringResource(id = R.string.tip_lz)),
        ),
        modifier = modifier
    )
}

@Composable
private fun ThreadMenu(
    isSeeLz: Boolean,
    isCollected: Boolean,
    isImmersiveMode: Boolean,
    isDesc: Boolean,
    canDelete: () -> Boolean,
    onSeeLzClick: () -> Unit,
    onCollectClick: () -> Unit,
    onImmersiveModeClick: () -> Unit,
    onDescClick: () -> Unit,
    onJumpPageClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showHandle: Boolean = true,
) {
    val account = LocalAccount.current
    Column(
        // 不写死高度：让 AppMenu 的 rememberMenuContentHeight 按内容实测
        // （AppMenu 会把 GlasmioContextMenu 的 20dp 内部 padding 补回来）。
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (showHandle) 16.dp else 12.dp)
    ) {
        if (showHandle) {
            Spacer(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(4.dp)
                    .fillMaxWidth(0.25f)
                    .clip(RoundedCornerShape(100))
                    .background(AppTokens.chip)
            )
        }
        VerticalGrid(
            column = 2,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            rowModifier = Modifier.height(IntrinsicSize.Min),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            item {
                ToggleButton(
                    text = {
                        TextWithMinWidth(
                            text = stringResource(id = R.string.title_see_lz),
                            minLength = 4
                        )
                    },
                    checked = isSeeLz,
                    onClick = {
                        onDismiss()
                        onSeeLzClick()
                    },
                    icon = {
                        Icon(
                            imageVector = if (isSeeLz) Icons.Rounded.Face6 else Icons.Rounded.FaceRetouchingOff,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (account != null) {
                item {
                    ToggleButton(
                        text = {
                            TextWithMinWidth(
                                text = stringResource(
                                    id = if (isCollected) R.string.title_collected else R.string.title_uncollected
                                ),
                                minLength = 4
                            )
                        },
                        checked = isCollected,
                        onClick = {
                            onDismiss()
                            onCollectClick()
                        },
                        icon = {
                            Icon(
                                imageVector = if (isCollected) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            item {
                ToggleButton(
                    text = {
                        TextWithMinWidth(
                            text = stringResource(
                                id = R.string.title_pure_read
                            ),
                            minLength = 4
                        )
                    },
                    checked = isImmersiveMode,
                    onClick = {
                        onDismiss()
                        onImmersiveModeClick()
                    },
                    icon = {
                        Icon(
                            imageVector = if (isImmersiveMode) Icons.AutoMirrored.Rounded.ChromeReaderMode else Icons.AutoMirrored.Outlined.ChromeReaderMode,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            item {
                ToggleButton(
                    text = {
                        TextWithMinWidth(
                            text = stringResource(
                                id = R.string.title_sort
                            ),
                            minLength = 4
                        )
                    },
                    checked = isDesc,
                    onClick = {
                        onDismiss()
                        onDescClick()
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Column {
            ListMenuItem(
                icon = Icons.Rounded.RocketLaunch,
                text = stringResource(id = R.string.title_jump_page),
                iconColor = AppTokens.text,
                onClick = {
                    onDismiss()
                    onJumpPageClick()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = Icons.Rounded.Share,
                text = stringResource(id = R.string.title_share),
                iconColor = AppTokens.text,
                onClick = {
                    onDismiss()
                    onShareClick()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            ListMenuItem(
                icon = Icons.Rounded.ContentCopy,
                text = stringResource(id = R.string.title_copy_link),
                iconColor = AppTokens.text,
                onClick = {
                    onDismiss()
                    onCopyLinkClick()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (account != null) {
                ListMenuItem(
                    icon = Icons.Rounded.Report,
                    text = stringResource(id = R.string.title_report),
                    iconColor = AppTokens.destructive,
                    textColor = AppTokens.destructive,
                    onClick = {
                        onDismiss()
                        onReportClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (canDelete()) {
                ListMenuItem(
                    icon = Icons.Rounded.Delete,
                    text = stringResource(id = R.string.title_delete),
                    iconColor = AppTokens.destructive,
                    textColor = AppTokens.destructive,
                    onClick = {
                        onDismiss()
                        onDeleteClick()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * 拉起官方贴吧客户端，定位到当前帖子（楼层 / 主题帖）。
 * 供安全模式提示弹窗「使用官方客户端」使用。
 */
private fun launchOfficialAppForThread(context: Context, threadId: Long, postId: Long?) {
    val uri = if (postId != null && postId != 0L) {
        Uri.parse("com.baidu.tieba://unidispatch/pb?obj_locate=comment_lzl_cut_guide&obj_source=wise&obj_name=index&obj_param2=chrome&has_token=0&qd=scheme&refer=tieba.baidu.com&wise_sample_id=3000232_2&hightlight_anchor_pid=${postId}&is_anchor_to_comment=1&comment_sort_type=0&fr=bpush&tid=${threadId}")
    } else {
        Uri.parse("com.baidu.tieba://unidispatch/pb?obj_locate=pb_reply&obj_source=wise&obj_name=index&obj_param2=chrome&has_token=0&qd=scheme&refer=tieba.baidu.com&wise_sample_id=3000232_2-99999_9&fr=bpush&tid=${threadId}")
    }
    val intent = Intent(Intent.ACTION_VIEW).setData(uri)
    val resolveInfos = context.packageManager.queryIntentActivities(
        intent,
        PackageManager.MATCH_DEFAULT_ONLY
    ).filter { it.activityInfo.packageName != context.packageName }
    try {
        if (resolveInfos.isNotEmpty()) {
            context.startActivity(intent)
        } else {
            context.toastShort(R.string.toast_official_client_not_install)
        }
    } catch (e: ActivityNotFoundException) {
        context.toastShort(R.string.toast_official_client_not_install)
    }
}
