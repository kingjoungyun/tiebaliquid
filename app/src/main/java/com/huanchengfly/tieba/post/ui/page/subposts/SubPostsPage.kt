package com.huanchengfly.tieba.post.ui.page.subposts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.bawuType
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.LocalThreadImageRadius
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.threadBottomBar
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.thread.ThreadBottomBarInset
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassLargeTitle
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPage
import com.huanchengfly.tieba.post.ui.common.theme.glass.rememberGlassTitleCollapse
import com.huanchengfly.tieba.post.ui.page.destinations.CopyTextDialogPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.reply.ReplyArgs
import com.huanchengfly.tieba.post.ui.page.reply.ReplyDialog
import com.huanchengfly.tieba.post.ui.page.thread.PostAgreeBtn
import com.huanchengfly.tieba.post.ui.page.thread.PostCard
import com.huanchengfly.tieba.post.ui.page.thread.UserNameText
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.ui.utils.getPhotoViewData
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyleBottomSheet
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

@Destination
@Composable
fun SubPostsPage(
    navigator: DestinationsNavigator,
    threadId: Long,
    forumId: Long = 0L,
    postId: Long = 0L,
    subPostId: Long = 0L,
    loadFromSubPost: Boolean = false,
    viewModel: SubPostsViewModel = pageViewModel()
) {
    ProvideNavigator(navigator) {
        SubPostsContent(
            viewModel = viewModel,
            forumId = forumId,
            threadId = threadId,
            postId = postId,
            subPostId = subPostId,
            loadFromSubPost = loadFromSubPost,
            onNavigateUp = { navigator.navigateUp() }
        )
    }
}

@Destination(
    style = DestinationStyleBottomSheet::class
)
@Composable
fun SubPostsSheetPage(
    navigator: DestinationsNavigator,
    threadId: Long,
    forumId: Long = 0L,
    postId: Long = 0L,
    subPostId: Long = 0L,
    loadFromSubPost: Boolean = false,
    viewModel: SubPostsViewModel = pageViewModel()
) {
    ProvideNavigator(navigator) {
        SubPostsContent(
            viewModel = viewModel,
            forumId = forumId,
            threadId = threadId,
            postId = postId,
            subPostId = subPostId,
            loadFromSubPost = loadFromSubPost,
            isSheet = true,
            onNavigateUp = { navigator.navigateUp() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
internal fun SubPostsContent(
    viewModel: SubPostsViewModel,
    forumId: Long,
    threadId: Long,
    postId: Long,
    subPostId: Long = 0L,
    loadFromSubPost: Boolean = false,
    isSheet: Boolean = false,
    onNavigateUp: () -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val account = LocalAccount.current

    LazyLoad(key = viewModel, loaded = viewModel.initialized) {
        viewModel.send(
            SubPostsUiIntent.Load(
                forumId,
                threadId,
                postId,
                subPostId.takeIf { loadFromSubPost } ?: 0L
            )
        )
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::isRefreshing,
        initial = false
    )
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::isLoading,
        initial = false
    )
    val anti by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::anti,
        initial = null
    )
    val forum by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::forum,
        initial = null
    )
    val thread by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::thread,
        initial = null
    )
    val post by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::post,
        initial = null
    )
    val postContentRenders by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::postContentRenders,
        initial = persistentListOf()
    )
    val subPosts by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::subPosts,
        initial = persistentListOf()
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::currentPage,
        initial = 1
    )
    val totalCount by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::totalCount,
        initial = 0
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::hasMore,
        initial = true
    )

    val lazyListState = rememberLazyListState()

    viewModel.onEvent<SubPostsUiEvent.ScrollToSubPosts> {
        delay(20)
        lazyListState.scrollToItem(2 + subPosts.indexOfFirst { it.id == subPostId })
    }

    val confirmDeleteDialogState = rememberDialogState()
    var deleteSubPost by remember { mutableStateOf<ImmutableHolder<SubPostList>?>(null) }
    ConfirmDialog(
        dialogState = confirmDeleteDialogState,
        onConfirm = {
            if (deleteSubPost == null) {
                val isSelfPost =
                    if (post?.get { author_id } == 0L) post?.get { author?.id } == account?.uid?.toLongOrNull() else post?.get { author_id } == account?.uid?.toLongOrNull()
                viewModel.send(
                    SubPostsUiIntent.DeletePost(
                        forumId = forum?.get { id } ?: forumId,
                        forumName = forum?.get { name }.orEmpty(),
                        threadId = threadId,
                        postId = post?.get { id } ?: postId,
                        deleteMyPost = isSelfPost,
                        tbs = anti?.get { tbs } ?: account?.tbs,
                    )
                )
            } else {
                val isSelfSubPost =
                    if (deleteSubPost!!.get { author_id } == 0L) deleteSubPost!!.get { author?.id } == account?.uid?.toLongOrNull() else
                        deleteSubPost!!.get { author_id } == account?.uid?.toLongOrNull()
                viewModel.send(
                    SubPostsUiIntent.DeletePost(
                        forumId = forum?.get { id } ?: forumId,
                        forumName = forum?.get { name }.orEmpty(),
                        threadId = threadId,
                        postId = post?.get { id } ?: postId,
                        subPostId = deleteSubPost!!.get { id },
                        deleteMyPost = isSelfSubPost,
                        tbs = anti?.get { tbs } ?: account?.tbs,
                    )
                )
            }
        }
    ) {
        Text(
            text = stringResource(
                id = R.string.message_confirm_delete,
                if (deleteSubPost == null && post != null) stringResource(
                    id = R.string.tip_post_floor,
                    post!!.get { floor })
                else stringResource(id = R.string.this_reply)
            )
        )
    }

    val replyDialogState = rememberDialogState()
    var currentReplyArgs by remember { mutableStateOf<ReplyArgs?>(null) }
    if (currentReplyArgs != null) {
        ReplyDialog(args = currentReplyArgs!!, state = replyDialogState)
    }

//    onGlobalEvent<GlobalEvent.ReplySuccess>(
//        filter = { it.threadId == threadId && it.postId == postId }
//    ) { event ->
//        viewModel.send(
//            SubPostsUiIntent.Load(
//                forumId,
//                threadId,
//                postId,
//                subPostId.takeIf { loadFromSubPost } ?: 0L
//            )
//        )
//    }

    fun showReplyDialog(args: ReplyArgs) {
        currentReplyArgs = args
        replyDialogState.show()
    }

    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = ThreadBottomBarInset + navigationBarBottom
    var largeTitleHeightPx by remember { mutableIntStateOf(0) }
    val isLargeTitleCollapsed = rememberGlassTitleCollapse(lazyListState, largeTitleHeightPx)
    val topScrimAlpha by animateFloatAsState(
        targetValue = if (isLargeTitleCollapsed) 1f else 0f,
        animationSpec = tween(180),
        label = "subPostsTopScrim",
    )
    val largeTitle = stringResource(id = R.string.title_sub_posts_page)
    val smallTitle = post?.let {
        stringResource(id = R.string.title_sub_posts, it.get { floor })
    }

    // T1：外壳交给统一的 GlassPage（overlay 双层 host + 悬浮返回钮 + 紧凑胶囊标题 + scrim）。
    GlassPage(
        title = largeTitle,
        onBack = onNavigateUp,
        enabled = true,
        collapsed = isLargeTitleCollapsed,
        scrimAlpha = topScrimAlpha,
        actions = {
            if (!isSheet) {
                //「打开原帖」仍在顶栏右侧：沿用原来的 Glasmio 圆钮（材质与迁移前一致）。
                GlasmioIconButton(
                    onClick = {
                        navigator.navigate(
                            ThreadPageDestination(
                                forumId = forum?.get { id } ?: forumId,
                                threadId = threadId,
                                postId = post?.get { id } ?: postId
                            )
                        )
                    },
                ) {
                    Icon(
                        imageVector = PhosphorIcons["arrow-square-out"] ?: Icons.Rounded.OpenInBrowser,
                        contentDescription = stringResource(id = R.string.btn_open_origin_thread),
                        tint = GlasmioTheme.colors.content,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        bottomBar = {
            SubPostsGlassBottomBar(
                onClickReply = {
                    val fid = forum?.get { id } ?: forumId
                    val forumName = forum?.get { name }
                    if (fid != 0L) {
                        showReplyDialog(
                            ReplyArgs(
                                forumId = fid,
                                forumName = forumName.toString(),
                                threadId = threadId,
                                postId = post?.get { id } ?: postId,
                            )
                        )
                    }
                }
            )
        },
    ) { padding ->
    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = subPosts.isEmpty(),
        isError = false,
        isLoading = isRefreshing
    ) {
        MyScaffold(
            modifier = Modifier.fillMaxSize(),
            backgroundColor = Color.Transparent,
            topBar = {
            },
            bottomBar = {
            }
        ) { paddingValues ->
            LoadMoreLayout(
                modifier = Modifier.padding(paddingValues),
                isLoading = isLoading,
                onLoadMore = {
                    viewModel.send(
                        SubPostsUiIntent.LoadMore(
                            forumId,
                            threadId,
                            postId,
//                            subPostId,
                            page = currentPage + 1,
                        )
                    )
                },
                loadEnd = !hasMore,
                lazyListState = lazyListState,
                isEmpty = post == null && subPosts.isEmpty(),
            ) {
                MyLazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = bottomInset,
                    ),
                ) {
                    item(key = "Post$postId") {
                        post?.let {
                            Column {
                                GlassLargeTitle(
                                    title = largeTitle,
                                    subtitle = smallTitle,
                                    onHeightChanged = { largeTitleHeightPx = it },
                                )
                                PostCard(
                                    postHolder = it,
                                    contentRenders = postContentRenders,
                                    threadAuthorId = thread?.get { author?.id } ?: 0L,
                                    canDelete = { it.author_id == account?.uid?.toLongOrNull() || it.author?.id == account?.uid?.toLongOrNull() || thread?.get { author?.id } ==  account?.uid?.toLongOrNull() },
                                    showSubPosts = false,
                                    glassmio = true,
                                    forceGlassCard = true,
                                    onUserClick = {
                                        navigator.navigate(UserProfilePageDestination(it.id))
                                    },
                                    onAgree = {
                                        val hasAgreed = it.get { agree?.hasAgree != 0 }
                                        viewModel.send(
                                            SubPostsUiIntent.Agree(
                                                forumId,
                                                threadId,
                                                post?.get { id } ?: postId,
                                                agree = !hasAgreed
                                            )
                                        )
                                    },
                                    onReplyClick = {
                                        val fid = forum?.get { id } ?: forumId
                                        if (fid != 0L) {
                                            showReplyDialog(
                                                ReplyArgs(
                                                    forumId = fid,
                                                    forumName = forum?.get { name } ?: "",
                                                    threadId = threadId,
                                                    postId =  post?.get { id } ?: postId,
                                                    replyUserId = it.author?.id ?: it.author_id,
                                                    replyUserName = it.author?.nameShow.takeIf { name -> !name.isNullOrEmpty() }
                                                        ?: it.author?.name,
                                                    replyUserPortrait = it.author?.portrait,
                                                )
                                            )
                                        }
                                    },
                                    onMenuCopyClick = {
                                        navigator.navigate(
                                            CopyTextDialogPageDestination(it)
                                        )
                                    },
                                ) {
                                    deleteSubPost = null
                                    confirmDeleteDialogState.show()
                                }
                            }
                        }
                    }
                    // §22：LEGACY 的「共 N 条回复」计数补回玻璃版（原来这里是 0 高度占位，
                    // 计数被吞了）。仍用 item（不做 sticky），保持 scrollToItem(2 + index) 的 index 语义。
                    item(key = "SubPostsHeader") {
                        Text(
                            text = stringResource(
                                id = R.string.title_sub_posts_header,
                                totalCount
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 13.sp,
                            color = AppTokens.textSecondary,
                        )
                    }
                    itemsIndexed(
                        items = subPosts,
                        key = { _, subPost -> subPost.id }
                    ) { _, item ->
                        // 命中屏蔽词的楼中楼直接不渲染
                        if (item.hidden) return@itemsIndexed
                        SubPostItem(
                            item = item,
                            glassmio = true,
                            forumId = forum?.get { id } ?: forumId,
                            forumName = forum?.get { name } ?: "",
                            threadId = threadId,
                            canDelete = { it.author_id == account?.uid?.toLongOrNull() || it.author?.id == account?.uid?.toLongOrNull() || thread?.get { author?.id } ==  account?.uid?.toLongOrNull() },
                            threadAuthorId = thread?.get { author?.id },
                            onUserClick = {
                                navigator.navigate(UserProfilePageDestination(it.id))
                            },
                            onAgree = {
                                val hasAgreed = it.agree?.hasAgree != 0
                                viewModel.send(
                                    SubPostsUiIntent.Agree(
                                        forumId,
                                        threadId,
                                        post?.get { id } ?: postId,
                                        subPostId = it.id,
                                        agree = !hasAgreed
                                    )
                                )
                            },
                            onReplyClick = {
                                val fid = forum?.get { id } ?: forumId
                                if (fid != 0L) {
                                    showReplyDialog(
                                        ReplyArgs(
                                            forumId = fid,
                                            forumName = forum?.get { name } ?: "",
                                            threadId = threadId,
                                            postId = post?.get { id } ?: postId,
                                            subPostId = it.id,
                                            replyUserId = it.author?.id ?: it.author_id,
                                            replyUserName = it.author?.nameShow.takeIf { name -> !name.isNullOrEmpty() }
                                                ?: it.author?.name,
                                            replyUserPortrait = it.author?.portrait,
                                        )
                                    )
                                }
                            },
                            onMenuCopyClick = {
                                navigator.navigate(
                                    CopyTextDialogPageDestination(it)
                                )
//                                TiebaUtil.copyText(context, it)
                            },
                            onMenuDeleteClick = {
                                deleteSubPost = it.wrapImmutable()
                                confirmDeleteDialogState.show()
                            },
                        )
                    }
                }
            }
        }
    }
}
}

private fun getDescText(
    time: Long?,
    floor: Int = 0,
    ipAddress: String?
): String {
    val texts = listOfNotNull(
        time?.let { DateTimeUtils.getRelativeTimeString(App.INSTANCE, it) },
        if (floor > 0) App.INSTANCE.getString(R.string.tip_post_floor, floor) else null,
        ipAddress?.let { App.INSTANCE.getString(R.string.text_ip_location, it) }
    )
    if (texts.isEmpty()) return ""
    return texts.joinToString(" · ")
}

@Composable
private fun SubPostItem(
    item: SubPostItemData,
    glassmio: Boolean = false,
    forumId: Long = 0L,
    forumName: String = "",
    threadId: Long = 0L,
    threadAuthorId: Long? = null,
    canDelete: (SubPostList) -> Boolean = { false },
    onUserClick: (User) -> Unit = {},
    onAgree: (SubPostList) -> Unit = {},
    onReplyClick: (SubPostList) -> Unit = {},
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuDeleteClick: ((SubPostList) -> Unit)? = null,
) {
    val (subPost, contentRenders, blocked) = item
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val account = LocalAccount.current
    val coroutineScope = rememberCoroutineScope()
    // 楼中楼的 renders 是裸的 subPost.content.renders，不带 photoViewData，
    // 导致图片点不动（点击会被外层卡片的「回复」吃掉）。这里补上，点击即可进看图页。
    val renders = remember(contentRenders, forumId, forumName, threadId, subPost) {
        if (forumId == 0L) {
            contentRenders
        } else {
            contentRenders.map { render ->
                if (render is PicContentRender && render.photoViewData == null) {
                    render.copy(
                        photoViewData = getPhotoViewData(
                            forumId = forumId,
                            forumName = forumName,
                            threadId = threadId,
                            postId = subPost.get { id },
                            picId = render.picId,
                            picUrl = render.picUrl,
                            originUrl = render.originUrl,
                            showOriginBtn = render.showOriginBtn,
                            originSize = render.originSize,
                        )
                    )
                } else {
                    render
                }
            }
        }
    }
    val author = remember(subPost) { subPost.get { author }?.wrapImmutable() }
    val hasAgreed = remember(subPost) {
        subPost.get { agree?.hasAgree == 1 }
    }
    val agreeNum = remember(subPost) {
        subPost.get { agree?.diffAgreeNum ?: 0L }
    }
    val menuState = rememberMenuState()
    BlockableContent(
        blocked = blocked,
        blockedTip = { BlockTip(text = { Text(text = stringResource(id = R.string.tip_blocked_sub_post)) }) },
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (glassmio) Modifier
                else Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ),
    ) {
        LongClickMenu(
            menuState = menuState,
            indication = null,
            menuContent = {
                if (onMenuCopyClick != null) {
                    DropdownMenuItem(
                        onClick = {
                            onMenuCopyClick(contentRenders.joinToString("\n") { it.toString() })
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
                                onReplyClick(subPost.get())
                                menuState.expanded = false
                            }
                        ) {
                            Text(text = stringResource(id = R.string.btn_reply))
                        }
                    }
                    DropdownMenuItem(
                        onClick = {
                            coroutineScope.launch {
                                TiebaUtil.reportPost(
                                    context,
                                    navigator,
                                    subPost.get { id }.toString()
                                )
                            }
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.title_report))
                    }
                }
                if (canDelete(subPost.get()) && onMenuDeleteClick != null) {
                    DropdownMenuItem(
                        onClick = {
                            onMenuDeleteClick(subPost.get())
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.title_delete))
                    }
                }
            },
            onClick = { onReplyClick(subPost.get()) }.takeUnless {
                context.appPreferences.safeMode || account == null
            }
        ) {
            if (glassmio) {
                val colors = GlasmioTheme.colors
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                ) {
                    if (author != null) {
                        Avatar(
                            data = StringUtil.getAvatarUrl(author.get { portrait }),
                            size = 36.dp,
                            contentDescription = null,
                            modifier = Modifier.debounceClickable(
                                onClick = { onUserClick(author.get()) }
                            ),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (author != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .debounceClickable(onClick = { onUserClick(author.get()) }),
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
                                            author.get { name },
                                            author.get { nameShow },
                                        ),
                                        userLevel = author.get { level_id },
                                        isLz = author.get { id } == threadAuthorId,
                                        bawuType = author.get { bawuType },
                                    )
                                }
                                Text(
                                    text = getDescText(
                                        time = subPost.get { time }.toLong(),
                                        floor = subPost.get { floor },
                                        ipAddress = author.get { ip_address },
                                    ),
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
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    renders.fastForEach { it.Render() }
                                }
                            }
                        }

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
                                    .debounceClickable(onClick = { onAgree(subPost.get()) })
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = if (hasAgreed) (PhosphorIcons["heart-fill"] ?: Icons.Rounded.Favorite) else (PhosphorIcons["heart"] ?: (PhosphorIcons["heart"] ?: Icons.Rounded.FavoriteBorder)),
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
            } else {
            Card(
                header = {
                    if (author != null) {
                        UserHeader(
                            avatar = {
                                Avatar(
                                    data = StringUtil.getAvatarUrl(author.get { portrait }),
                                    size = Sizes.Small,
                                    contentDescription = null
                                )
                            },
                            name = {
                                UserNameText(
                                    userName = StringUtil.getUsernameAnnotatedString(
                                        LocalContext.current,
                                        author.get { name },
                                        author.get { nameShow }
                                    ),
                                    userLevel = author.get { level_id },
                                    isLz = author.get { id } == threadAuthorId,
                                    bawuType = author.get { bawuType },
                                )
                            },
                            desc = {
                                Text(
                                    text = getDescText(
                                        time = subPost.get { time }.toLong(),
                                        ipAddress = author.get { ip_address },
                                    )
                                )
                            },
                            onClick = {
                                onUserClick(author.get())
                            }
                        ) {
                            PostAgreeBtn(
                                hasAgreed = hasAgreed,
                                agreeNum = agreeNum,
                                onClick = { onAgree(subPost.get()) }
                            )
                        }
                    }
                },
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .padding(start = Sizes.Small + 8.dp)
                            .fillMaxWidth()
                    ) {
                        renders.fastForEach { it.Render() }
                    }
                }
            )
            }
        }
    }
}