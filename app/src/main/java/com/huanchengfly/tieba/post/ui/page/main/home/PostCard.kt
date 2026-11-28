package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.VideoInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.models.protos.renders
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.common.TextContentRender
import com.huanchengfly.tieba.post.ui.utils.getPhotoViewData
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.EmoticonText
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideContentColor
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.appPreferences
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.render.backdrops.emptyBackdrop
import io.github.neoruaa.glasmio.ui.GlasmioCard
import kotlinx.collections.immutable.ImmutableList
import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import androidx.compose.material.icons.rounded.Videocam

/**
 * 首页信息流的「推特式」帖子卡（`PostCardUiStyle.TWITTER`）。
 *
 * 版式（所有文字 / 媒体共用同一条左边缘 = 卡内 16dp + 头像列 40dp + 10dp 间距）：
 *
 * ```
 * ┌──────────────────────────────────────────────┐
 * │  ⬤   用户名 · 3 分钟前                    ⋯  │  头像只占左列，文字块整体右移
 * │      标题（17sp / w600 / 2 行）              │
 * │      正文（15sp / 3 行 / 省略号）             │
 * │      ┌────────────────────────────────┐      │  媒体左沿对齐文字，右沿顶到卡内边距
 * │      └────────────────────────────────┘      │
 * │  吧名                ⇄ 36  💬 128  ♡ 1.2万   │  底行：吧名靠左、三个操作靠右
 * └──────────────────────────────────────────────┘
 * ```
 *
 * 结构分三层，尽量只做「排版」不碰材质：
 * 1. **壳**：[GlasmioCard]（26dp 连续圆角、`elevatedBackground` 实色、不是玻璃）。
 *    深色下卡与纯黑底几乎同色，所以再补一层 1dp 的 `subtleStroke` 内缘（不用阴影）。
 * 2. **左列**：40dp 头像（`Avatar`，缺图时是首字占位），只这一列，标题正文媒体都不许回勾。
 * 3. **右列**：作者行（`用户名 · 时间` + 右上 ⋯ 插槽）→ 标题 → 正文 → 媒体 → 底行。
 *
 * 复用的旧部件：`Avatar`（头像）、`NetworkImage`（图片 + 点击进 `PhotoViewActivity`）、
 * `StringUtil.getUsernameAnnotatedString` / `getShortNumString`、`DateTimeUtils.getRelativeTimeString`、
 * `getPhotoViewData`。三个操作按钮**没有**复用 `ThreadShareBtn/ThreadReplyBtn/ThreadAgreeBtn`：
 * 它们的数字是「跟着内容走的 intrinsic 宽度 + 居中」，与「4 位数定宽、右对齐、整簇靠右」冲突，
 * 所以这里按同一套图标 / 颜色 / 格式化重写了一个 [PostAction]（行为等价：点击、点赞变红、计数）。
 *
 * TODO（本轮先用占位，未覆盖）：
 * - 视频帖：只画 16:9 播放占位块，**没接** `VideoPlayer`；
 * - 转发帖：只画「转发自 …」的一行占位，**没接** `OriginThreadCard`；
 * - 精品帖：标题前缀「精品」二字（accent 色），没有角标；
 * - 分类 `tabName`、长图 `isLongPic`、语音、投票、直播等富内容：未渲染；
 * - 「不感兴趣」的 ⋯ 需要 `personalized` 数据，普通帖（无 personalized）本轮不显示 ⋯。
 */
@Composable
fun PostCard(
    item: ImmutableHolder<ThreadInfo>,
    onClick: (ThreadInfo) -> Unit,
    onAgree: (ThreadInfo) -> Unit,
    modifier: Modifier = Modifier,
    onClickReply: (ThreadInfo) -> Unit = {},
    onClickUser: (User) -> Unit = {},
    onClickForum: (SimpleForum) -> Unit = {},
    menuAction: @Composable () -> Unit = {},
    /** 吧内帖子列表用：接口返回的 `ThreadInfo.forumName` 常常是空串，用当前吧名兜底。 */
    forumNameFallback: String? = null,
) {
    val colors = GlasmioTheme.colors
    val context = LocalContext.current

    val author = remember(item) { item.get { author } }
    val isGood = remember(item) { item.get { isGood } == 1 }
    val hasTitle = remember(item) {
        item.get { isNoTitle != 1 && title.isNotBlank() }
    }
    val title = remember(item) { item.get { title } }
    val body = remember(item) { item.get { abstractText } }
    val medias = remember(item) { item.getImmutableList { media } }
    val videoInfo = remember(item) { item.getNullableImmutable { videoInfo } }
    // 转发帖：title / 作者名直接读 proto 的可空字段，避免再套一层 ImmutableHolder 造成 lambda 里名字遮蔽。
    val originThread = remember(item) {
        if (item.get { is_share_thread } == 1) item.get { origin_thread_info } else null
    }
    val rawForumName = remember(item, forumNameFallback) {
        item.get { forumName }.takeUnless { it.isBlank() } ?: forumNameFallback.orEmpty()
    }
    val forumLabel = remember(rawForumName) {
        if (rawForumName.isBlank()) "" else context.getString(R.string.title_forum_name, rawForumName)
    }
    val time = remember(item) {
        DateTimeUtils.getRelativeTimeString(
            context,
            item.get { lastTimeInt }.toString()
        )
    }
    val nameText = remember(author, colors) {
        author?.let {
            StringUtil.getUsernameAnnotatedString(
                context = context,
                username = it.name,
                nickname = it.nameShow,
                color = colors.secondaryContent,
            )
        }
    }
    val headerText = remember(nameText, time, colors) {
        buildAnnotatedString {
            val hasName = nameText != null && nameText.length > 0
            if (nameText != null) append(nameText)
            withStyle(
                SpanStyle(
                    color = colors.secondaryContent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                )
            ) {
                if (hasName) append("  ·  ")
                append(time)
            }
        }
    }
    val titleText = remember(title, isGood, colors) {
        buildAnnotatedString {
            if (isGood) {
                withStyle(SpanStyle(color = colors.accent)) { append("精品 ") }
            }
            append(title)
        }
    }

    // 正文富文本（T3.5）：优先用第一楼完整内容，其次用摘要；图片节点单独提出来进轮播，
    // 正文只渲染文字，避免图文重复占位。
    val richRenders = remember(item) {
        val first = item.get { firstPostContent }
        if (first.isNotEmpty()) first.renders else item.get { richAbstract }.renders
    }
    val inlineImages = remember(richRenders) {
        richRenders.filterIsInstance<PicContentRender>()
    }

    val hasMedia = medias.isNotEmpty() || videoInfo != null || inlineImages.isNotEmpty()
    val hideMedia = context.appPreferences.hideMedia
    // GlasmioCard 的 backdrop 默认值会去读 `LocalGlassBackdrop.current`，而这个 local 在没有
    // `GlasmioBackdropHost` 祖先时**直接抛异常**（宽屏 / 无 dock / 玻璃不可用的降级路径都会没有 host）。
    // 卡片是实色的、根本不需要采样背景，所以显式给一个空 backdrop：
    // 既跳过那次 local 读取，也让卡片在任何页面上都能安全使用。
    val cardBackdrop = remember { emptyBackdrop() }

    GlasmioCard(
        modifier = modifier.fillMaxWidth(),
        backdrop = cardBackdrop,
        onClick = { onClick(item.get()) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 深色下 #1C1C1E 卡贴纯黑底时边界太弱：补 1dp 内缘（PLAN 第 10 节第 7 条），不用阴影。
                .border(
                    width = 1.dp,
                    color = colors.subtleStroke,
                    shape = ContinuousRoundedRectangle(PostCardTokens.CardRadius),
                )
                .padding(
                    start = PostCardTokens.CardPaddingH,
                    end = PostCardTokens.CardPaddingH,
                    top = PostCardTokens.CardPaddingTop,
                    bottom = PostCardTokens.CardPaddingBottom,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                // ---- 左列：只有头像。文字块整体右移一列，标题/正文/媒体共用同一条左边缘 ----
                AvatarColumn(
                    author = author,
                    onClick = { author?.let { onClickUser(it) } },
                )

                Spacer(modifier = Modifier.width(PostCardTokens.AvatarGutter))

                // ---- 右列：文字块 ----
                Column(modifier = Modifier.weight(1f)) {

                    // 作者行：`用户名 · 3 分钟前`（同一行、13sp 次级色的时间）+ 右上 ⋯
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PostCardTokens.HeaderRowHeight),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = headerText,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.content,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Box(modifier = Modifier.size(PostCardTokens.MenuButtonSize)) {
                            menuAction()
                        }
                    }

                    if (hasTitle) {
                        Spacer(modifier = Modifier.height(PostCardTokens.TitleTopGap))
                        Text(
                            text = titleText,
                            fontSize = 17.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight(600),
                            color = colors.content,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    PostBody(
                        renders = richRenders,
                        fallbackText = body,
                        topGap = if (hasTitle) PostCardTokens.BodyTopGap else PostCardTokens.TitleTopGap,
                    )

                    if (originThread != null) {
                        Spacer(modifier = Modifier.height(PostCardTokens.BodyTopGap))
                        OriginThreadBlock(originThread = originThread)
                    }

                    if (hasMedia) {
                        Spacer(modifier = Modifier.height(PostCardTokens.MediaTopGap))
                        PostMedia(
                            medias = medias,
                            inlineImages = inlineImages,
                            videoInfo = videoInfo,
                            forumId = item.get { forumId },
                            forumName = item.get { forumName },
                            threadId = item.get { threadId },
                            hideMedia = hideMedia,
                            onOpenThread = { onClick(item.get()) },
                        )
                    }

                    Spacer(modifier = Modifier.height(PostCardTokens.FooterTopGap))

                    // ---- 底行：吧名靠左（与文字左边缘平齐），三个操作整体靠右 ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (forumLabel.isNotBlank()) {
                            Text(
                                text = forumLabel,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = colors.secondaryContent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .debounceClickable(
                                        onClick = {
                                            onClickForum(
                                                item.get { forumInfo ?: SimpleForum(name = rawForumName) }
                                            )
                                        }
                                    )
                                    .padding(vertical = 4.dp),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        PostAction(
                            text = numText(item.get { shareNum }),
                            tint = colors.secondaryContent,
                            onClick = { /* 旧版分享按钮也是空实现（ThreadShareBtn(onClick = {})），保持一致 */ },
                        ) { iconModifier ->
                            Icon(
                                imageVector = (PhosphorIcons["repeat"] ?: Icons.Rounded.SwapCalls),
                                contentDescription = stringResource(id = R.string.desc_share),
                                modifier = iconModifier,
                            )
                        }

                        Spacer(modifier = Modifier.width(PostCardTokens.ActionGap))

                        PostAction(
                            text = numText(item.get { replyNum }.toLong()),
                            tint = colors.secondaryContent,
                            onClick = { onClickReply(item.get()) },
                        ) { iconModifier ->
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_comment_new),
                                contentDescription = stringResource(id = R.string.desc_comment),
                                modifier = iconModifier,
                            )
                        }

                        Spacer(modifier = Modifier.width(PostCardTokens.ActionGap))

                        val hasAgree = item.get { agree?.hasAgree == 1 }
                        PostAction(
                            text = numText(item.get { agreeNum }.toLong()),
                            tint = if (hasAgree) PostCardTokens.likeColor else colors.secondaryContent,
                            onClick = { onAgree(item.get()) },
                        ) { iconModifier ->
                            Icon(
                                imageVector = if (hasAgree) (PhosphorIcons["heart-fill"] ?: Icons.Rounded.Favorite) else (PhosphorIcons["heart"] ?: (PhosphorIcons["heart"] ?: Icons.Rounded.FavoriteBorder)),
                                contentDescription = stringResource(id = R.string.desc_like),
                                modifier = iconModifier,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 卡内的一列：40dp 头像 + 底下留白。
 *
 * 宽高都锁死，保证右列（标题 / 正文 / 媒体）的左边缘严格等于 `40 + 10`，
 * 不会因为作者名长短、头像加载状态而漂移。
 */
@Composable
private fun AvatarColumn(
    author: User?,
    onClick: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val portrait = author?.portrait.orEmpty()
    Box(
        modifier = Modifier
            .width(PostCardTokens.AvatarSize)
            .height(PostCardTokens.AvatarSize)
            .clip(CircleShape)
            .debounceClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (portrait.isNotBlank()) {
            Avatar(
                data = StringUtil.getAvatarUrl(portrait),
                size = PostCardTokens.AvatarSize,
                contentDescription = stringResource(id = R.string.desc_user_avatar),
            )
        } else {
            // 头像缺失（未登录 / 数据不全）时画个中性占位，不挤掉布局也不塌成一个字母。
            Box(
                modifier = Modifier
                    .size(PostCardTokens.AvatarSize)
                    .clip(CircleShape)
                    .background(colors.secondaryContent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = author?.name.orEmpty().trim().take(1).ifEmpty { "吧" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.secondaryContent,
                )
            }
        }
    }
}

/**
 * 媒体区（T3.5 重写）：视频封面 / 单图 / 多图轮播。左沿 = 文字块左沿，右沿 = 卡内边距。
 *
 * - **视频**：`videoInfo.thumbnailUrl` 封面 + 中央播放键。这里**不内嵌播放器**（信息流里同时
 *   存活的播放器实例数不可控、且会和「点整卡进详情」抢点击），点击进帖子详情再看；
 * - **单图**：按宽高比（夹到 1.2~1.9），14dp 圆角，点开相册；
 * - **多图**：横向轮播（可左右滑），右下角 `1/N` 药丸，点开相册并**定位到当前页**；
 * - **正文内嵌图**（`medias` 为空但 `PbContent` 里有图）：用提取出来的图走同样的单图/轮播；
 * - `hideMedia` 打开时只画一行占位。
 */
@Composable
private fun PostMedia(
    medias: ImmutableList<ImmutableHolder<Media>>,
    inlineImages: List<PicContentRender>,
    videoInfo: ImmutableHolder<VideoInfo>?,
    forumId: Long,
    forumName: String,
    threadId: Long,
    hideMedia: Boolean,
    onOpenThread: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val mediaShape = ContinuousRoundedRectangle(PostCardTokens.MediaRadius)

    // 轮播素材：优先接口给的 media，为空时用正文里抽出来的图。
    val urls: List<String> = remember(medias, inlineImages) {
        if (medias.isNotEmpty()) medias.map { it.mediaUrl() } else inlineImages.map { it.picUrl }
    }
    val aspects: List<Float> = remember(medias, inlineImages) {
        if (medias.isNotEmpty()) medias.map { it.singleAspectRatio() } else inlineImages.map { it.inlineAspectRatio() }
    }
    val photoViewDataList: List<PhotoViewData> = remember(medias, inlineImages, forumId, forumName, threadId) {
        if (medias.isNotEmpty()) {
            val plain = medias.map { it.get() }
            medias.indices.map { index ->
                getPhotoViewData(
                    medias = plain,
                    forumId = forumId,
                    forumName = forumName,
                    threadId = threadId,
                    index = index,
                )
            }
        } else {
            inlineImages.map { pic ->
                getPhotoViewData(
                    forumId = forumId,
                    forumName = forumName,
                    threadId = threadId,
                    postId = 0L,
                    picId = pic.picId,
                    picUrl = pic.picUrl,
                    originUrl = pic.originUrl,
                    showOriginBtn = pic.showOriginBtn,
                    originSize = pic.originSize,
                )
            }
        }
    }

    if (hideMedia) {
        val videoOnly = videoInfo != null && urls.isEmpty()
        val label = when {
            videoOnly -> stringResource(id = R.string.desc_video)
            else -> stringResource(id = R.string.btn_open_photos, urls.size)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(mediaShape)
                .background(colors.container)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (videoOnly) PhosphorIcons["play"] ?: Icons.Rounded.PlayArrow else PhosphorIcons["image"] ?: Icons.Rounded.Photo,
                contentDescription = null,
                tint = colors.secondaryContent,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = colors.secondaryContent,
                maxLines = 1,
            )
        }
        return
    }

    if (videoInfo != null) {
        val thumbnailUrl = remember(videoInfo) { videoInfo.get { thumbnailUrl } }
        // 信息流接口常常**不返回** `video_info.thumbnail_url`（只有视频帖详情页才有），
        // 于是「播放按钮 + 空容器」= 用户报的「视频没封面」。降级：用帖子第一张图当封面。
        val coverUrl = remember(thumbnailUrl, urls) {
            thumbnailUrl?.takeIf { it.isNotBlank() } ?: urls.firstOrNull().orEmpty()
        }
        val videoAspect = remember(videoInfo) {
            val w = videoInfo.get { thumbnailWidth }.toFloat()
            val h = videoInfo.get { thumbnailHeight }.toFloat()
            if (w > 0f && h > 0f) (w / h).coerceIn(1.2f, 16f / 9f) else 16f / 9f
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(videoAspect)
                .clip(mediaShape)
                .background(colors.container)
                .debounceClickable(onClick = onOpenThread),
        ) {
            if (coverUrl.isNotBlank()) {
                NetworkImage(
                    imageUri = coverUrl,
                    contentDescription = stringResource(id = R.string.desc_video),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    enablePreview = false,
                )
            } else {
                // 连配图都没有：给一个明确的「视频」占位，而不是空容器。
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = PhosphorIcons["video-camera"] ?: Icons.Rounded.Videocam,
                        contentDescription = null,
                        tint = colors.secondaryContent,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.desc_video),
                        fontSize = 13.sp,
                        color = colors.secondaryContent,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(PostCardTokens.PlayBadgeSize)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PhosphorIcons["play-fill"] ?: Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(id = R.string.desc_video),
                    tint = Color.White,
                    modifier = Modifier.size(PostCardTokens.PlayIconSize),
                )
            }
        }
        return
    }

    if (urls.isEmpty()) return

    if (urls.size == 1) {
        NetworkImage(
            imageUri = urls.first(),
            contentDescription = stringResource(id = R.string.desc_photo),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspects.first())
                .clip(mediaShape),
            photoViewData = photoViewDataList.first(),
            contentScale = ContentScale.Crop,
            enablePreview = true,
        )
        return
    }

    // 多图：横向轮播 + 1/N 药丸
    val pagerState = rememberPagerState(pageCount = { urls.size })
    Box(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                // 统一按 4:3 出图（单图才按原比例），保证左右翻页时卡片高度不跳。
                .aspectRatio(PostCardTokens.CarouselAspect)
                .clip(mediaShape),
        ) { page ->
            NetworkImage(
                imageUri = urls[page],
                contentDescription = stringResource(id = R.string.desc_photo),
                modifier = Modifier.fillMaxSize(),
                photoViewData = photoViewDataList[page],
                contentScale = ContentScale.Crop,
                enablePreview = true,
            )
        }
        Text(
            text = "${pagerState.currentPage + 1}/${urls.size}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(100))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/**
 * 正文（T3.5）：富文本渲染 + 6 行折叠。
 *
 * - 文字来自 `PbContentRender` 的文本节点（图片节点已被 [PostCard] 抽出去做轮播），
 *   所以加粗 / 链接 / @ / 表情样式都还在；
 * - 超过 6 行时在卡内给「展开」，展开后给「收起」（不跳转）；
 * - 没有富文本时退回 `abstract` 纯文本，行为一致。
 */
@Composable
private fun PostBody(
    renders: ImmutableList<PbContentRender>,
    fallbackText: String,
    topGap: androidx.compose.ui.unit.Dp,
) {
    val colors = GlasmioTheme.colors
    val text: AnnotatedString? = remember(renders, fallbackText) {
        val fromRenders = buildAnnotatedString {
            renders.forEach { render ->
                if (render is TextContentRender && render.text.isNotBlank()) {
                    if (length > 0) append('\n')
                    append(render.text)
                }
            }
        }
        when {
            fromRenders.isNotEmpty() -> fromRenders
            fallbackText.isNotBlank() -> AnnotatedString(fallbackText)
            else -> null
        }
    }

    if (text == null) return

    var expanded by remember(text) { mutableStateOf(false) }
    var overflowed by remember(text) { mutableStateOf(false) }

    Spacer(modifier = Modifier.height(topGap))
    EmoticonText(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        fontSize = 15.sp,
        lineSpacing = 0.8.sp,
        color = colors.secondaryContent,
        style = LocalTextStyle.current,
        maxLines = if (expanded) Int.MAX_VALUE else PostCardTokens.BodyCollapsedLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout ->
            if (!expanded) overflowed = layout.hasVisualOverflow
        },
    )
    if (overflowed) {
        Text(
            text = if (expanded) "收起" else "展开",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.accent,
            modifier = Modifier
                .padding(top = 3.dp, bottom = 1.dp)
                .clip(RoundedCornerShape(6.dp))
                .debounceClickable(onClick = { expanded = !expanded })
                .padding(horizontal = 2.dp, vertical = 2.dp),
        )
    }
}

/**
 * 转发帖的「原帖卡片」：标题 + 作者（+ 吧名）+ 摘要，有配图时右侧给一张 64dp 缩略图。
 *
 * 原帖卡片**不单独可点**：整卡点击仍然是进当前转发帖的详情，避免在同一张卡里出现两个跳转目标。
 */
@Composable
private fun OriginThreadBlock(originThread: OriginThreadInfo) {
    val colors = GlasmioTheme.colors
    val title = originThread.title
    val authorName = remember(originThread) {
        val author = originThread.author
        author?.nameShow?.takeIf { it.isNotBlank() } ?: author?.name.orEmpty()
    }
    val forumName = originThread.fname
    val summary = remember(originThread) { originThread._abstract.abstractText }
    val cover = remember(originThread) {
        originThread.media.firstOrNull()?.let { media ->
            ImageUtil.getUrl(App.INSTANCE, true, media.originPic, media.bigPic, media.dynamicPic, media.srcPic)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(PostCardTokens.MediaRadius))
            .background(colors.container)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight(600),
                    color = colors.content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (authorName.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (forumName.isNotBlank()) "@$authorName · $forumName" else "@$authorName",
                    fontSize = 12.sp,
                    color = colors.secondaryContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = summary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = colors.secondaryContent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!cover.isNullOrBlank()) {
            NetworkImage(
                imageUri = cover,
                contentDescription = stringResource(id = R.string.desc_photo),
                modifier = Modifier
                    .size(PostCardTokens.OriginCoverSize)
                    .clip(ContinuousRoundedRectangle(PostCardTokens.OriginCoverRadius)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** 正文内嵌图的单图比例（夹到 1.2~1.9，跟 [singleAspectRatio] 一致）。 */
private fun PicContentRender.inlineAspectRatio(): Float {
    if (width <= 0 || height <= 0) return 4f / 3f
    return (width.toFloat() / height.toFloat()).coerceIn(1.2f, 1.9f)
}

/**
 * 一个操作 = 16dp 图标 + 定宽数字。
 *
 * 数字固定占 4 个字符宽（`34dp`，右对齐、`softWrap=false`），所以「36」→「128」→「1.2万」
 * 切换时后面的图标不会左右跳动；三个操作之间用固定 [PostCardTokens.ActionGap] 分隔。
 */
@Composable
private fun PostAction(
    text: String,
    tint: Color,
    onClick: () -> Unit,
    icon: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .debounceClickable(onClick = onClick)
            .heightIn(min = PostCardTokens.ActionTouchHeight)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 图标不给 tint 时默认吃 LocalContentColor（M2 里是黑色）；这里统一由 tint 驱动，
        // 保证「图标 + 数字」同色、点赞时一起变红。
        ProvideContentColor(color = tint) {
            icon(Modifier.size(PostCardTokens.ActionIconSize))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = tint,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.End,
            modifier = Modifier.width(PostCardTokens.ActionNumWidth),
        )
    }
}

/** 数字统一走短格式（0 → "0"，10000+ → "1.2W"）；定宽按「1.2万」这种更宽的形状预留。 */
private fun numText(value: Long): String =
    if (value <= 0L) "0" else value.getShortNumString()

private fun ImmutableHolder<Media>.mediaUrl(): String = ImageUtil.getUrl(
    App.INSTANCE,
    true,
    get { originPic },
    get { bigPic },
    get { dynamicPic },
    get { srcPic },
)

/** 单图比例：夹到 1.2（5:6 竖图）~ 1.9（接近 16:9），避免超长竖图把卡片撑破。 */
private fun ImmutableHolder<Media>.singleAspectRatio(): Float {
    val width = get { width }.toFloat()
    val height = get { height }.toFloat()
    if (width <= 0f || height <= 0f) return 4f / 3f
    return (width / height).coerceIn(1.2f, 1.9f)
}

/** 卡片内部的所有尺寸 / 间距，集中一处，方便和设计稿逐条对。 */
internal object PostCardTokens {
    /** 列表里相邻卡片的间距（首页 / 吧页 / 主页统一）。 */
    val CardGap = 10.dp
    val CardRadius = 26.dp
    val AvatarSize = 40.dp
    val AvatarGutter = 10.dp
    val CardPaddingH = 16.dp
    val CardPaddingTop = 14.dp
    val CardPaddingBottom = 8.dp
    val HeaderRowHeight = 22.dp
    val MenuButtonSize = 22.dp
    val TitleTopGap = 4.dp
    val BodyTopGap = 3.dp
    val MediaTopGap = 8.dp
    val FooterTopGap = 6.dp
    val MediaRadius = 14.dp
    val ActionIconSize = 16.dp
    val ActionNumWidth = 34.dp
    val ActionGap = 10.dp

    /** 操作按钮的触控区高度：图标只有 16dp，但点击区域补到 28dp。 */
    val ActionTouchHeight = 28.dp

    /** 正文折叠行数（T3.5：完整渲染 + 6 行折叠）。 */
    const val BodyCollapsedLines = 6

    /** 多图轮播的页高比（统一 4:3，翻页时高度不跳）。 */
    const val CarouselAspect = 4f / 3f

    /** 视频封面上的播放键。 */
    val PlayBadgeSize = 48.dp
    val PlayIconSize = 26.dp

    /** 转发帖原帖配图。 */
    val OriginCoverSize = 64.dp
    val OriginCoverRadius = 10.dp

    /** 点赞高亮色：iOS 红，浅色 / 深色各一档（不跟主题 accent 走，见简报「遗留」）。 */
    val likeColor: Color
        @Composable get() = if (AppTokens.isNightMode) Color(0xFFFF453A) else Color(0xFFFF3B30)
}

/**
 * 帖子卡骨架屏（T3.5 从 `FeedCard.kt` 的 `FeedCardPlaceholder` 搬来）。
 *
 * 原实现用 M2 `placeholder()` + `Card`/`UserHeader` 占位；卡片统一后这里只保留「同尺寸的
 * 中性色块」——骨架屏出现的时间很短，用色块已经足够，也不用再依赖那套 M2 组件。
 */
@Composable
fun PostCardPlaceholder(modifier: Modifier = Modifier) {
    val colors = GlasmioTheme.colors
    GlasmioCard(
        modifier = modifier.fillMaxWidth(),
        backdrop = remember { emptyBackdrop() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = PostCardTokens.CardPaddingH,
                    end = PostCardTokens.CardPaddingH,
                    top = PostCardTokens.CardPaddingTop,
                    bottom = PostCardTokens.CardPaddingBottom,
                ),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(PostCardTokens.AvatarSize)
                    .clip(CircleShape)
                    .background(colors.secondaryContent.copy(alpha = 0.12f))
            )
            Spacer(modifier = Modifier.width(PostCardTokens.AvatarGutter))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.secondaryContent.copy(alpha = 0.12f))
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.secondaryContent.copy(alpha = 0.10f))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.secondaryContent.copy(alpha = 0.07f))
                )
            }
        }
    }
}
