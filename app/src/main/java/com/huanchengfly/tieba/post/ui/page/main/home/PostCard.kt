package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.VideoInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.utils.getPhotoViewData
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
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
    val originTitle = remember(originThread) { originThread?.title.orEmpty() }
    val originAuthorName = remember(originThread) { originThread?.author?.name.orEmpty() }
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

    val hasMedia = medias.isNotEmpty() || videoInfo != null
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

                    if (body.isNotBlank()) {
                        Spacer(
                            modifier = Modifier.height(
                                if (hasTitle) PostCardTokens.BodyTopGap else PostCardTokens.TitleTopGap
                            )
                        )
                        Text(
                            text = body,
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            color = colors.secondaryContent,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (originThread != null) {
                        Spacer(modifier = Modifier.height(PostCardTokens.BodyTopGap))
                        ShareThreadPlaceholder(
                            title = originTitle,
                            authorName = originAuthorName,
                        )
                    }

                    if (hasMedia) {
                        Spacer(modifier = Modifier.height(PostCardTokens.MediaTopGap))
                        PostMedia(
                            medias = medias,
                            videoInfo = videoInfo,
                            forumId = item.get { forumId },
                            forumName = item.get { forumName },
                            threadId = item.get { threadId },
                            hideMedia = hideMedia,
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
 * 媒体区：左沿 = 文字块左沿，右沿 = 卡内边距。
 *
 * - 单图：按 `width/height` 算比例（夹到 1.2~1.9，避免超长竖图把卡片撑成一张图），14dp 圆角；
 * - 多图：最多 3 张并排，每张 1:1、间距 4dp，>3 张时右下角挂 `+N`；
 * - 视频：16:9 占位块（TODO：接 `VideoPlayer`）；
 * - `hideMedia` 打开时只画一行「图片 N 张」占位。
 */
@Composable
private fun PostMedia(
    medias: ImmutableList<ImmutableHolder<Media>>,
    videoInfo: ImmutableHolder<VideoInfo>?,
    forumId: Long,
    forumName: String,
    threadId: Long,
    hideMedia: Boolean,
) {
    val colors = GlasmioTheme.colors
    val mediaShape = ContinuousRoundedRectangle(PostCardTokens.MediaRadius)

    if (hideMedia) {
        val label = when {
            videoInfo != null -> stringResource(id = R.string.desc_video)
            else -> stringResource(id = R.string.btn_open_photos, medias.size)
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
                imageVector = if (videoInfo != null) PhosphorIcons["play"] ?: Icons.Rounded.PlayArrow else PhosphorIcons["image"] ?: Icons.Rounded.Photo,
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
        // TODO(视频): 目前只是占位，点击走整卡进详情；后续接 FeedCard 里的 VideoPlayer。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(mediaShape)
                .background(colors.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PhosphorIcons["play"] ?: Icons.Rounded.PlayArrow,
                contentDescription = stringResource(id = R.string.desc_video),
                tint = colors.secondaryContent,
                modifier = Modifier.size(36.dp),
            )
        }
        return
    }

    if (medias.isEmpty()) return

    if (medias.size == 1) {
        val media = medias.first()
        val aspect = remember(media) { media.singleAspectRatio() }
        val photoViewData = remember(medias, forumId, forumName, threadId) {
            getPhotoViewData(
                medias = medias.map { it.get() },
                forumId = forumId,
                forumName = forumName,
                threadId = threadId,
                index = 0,
            )
        }
        NetworkImage(
            imageUri = remember(media) { media.mediaUrl() },
            contentDescription = stringResource(id = R.string.desc_photo),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(mediaShape),
            photoViewData = photoViewData,
            contentScale = ContentScale.Crop,
            enablePreview = true,
        )
        return
    }

    val shown = remember(medias) { medias.take(3) }
    val moreCount = remember(medias) { medias.size - shown.size }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 2 张就各占一半、3 张各占 1/3（不做「固定 3 等分 + 补空位」，否则 2 张只铺满 2/3）。
            shown.forEachIndexed { index, media ->
                val photoViewData = remember(medias, index, forumId, forumName, threadId) {
                    getPhotoViewData(
                        medias = medias.map { it.get() },
                        forumId = forumId,
                        forumName = forumName,
                        threadId = threadId,
                        index = index,
                    )
                }
                NetworkImage(
                    imageUri = remember(media) { media.mediaUrl() },
                    contentDescription = stringResource(id = R.string.desc_photo),
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(mediaShape),
                    photoViewData = photoViewData,
                    contentScale = ContentScale.Crop,
                    enablePreview = true,
                )
            }
        }
        if (moreCount > 0) {
            Text(
                text = "+$moreCount",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(100))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/**
 * 转发帖占位（TODO：接 `OriginThreadCard` 的完整原帖内容 / 原帖配图）。
 */
@Composable
private fun ShareThreadPlaceholder(
    title: String,
    authorName: String,
) {
    val colors = GlasmioTheme.colors
    Text(
        text = if (authorName.isNotBlank()) "转发自 $authorName：$title" else "转发：$title",
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = colors.secondaryContent,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(PostCardTokens.MediaRadius))
            .background(colors.container)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
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
private object PostCardTokens {
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

    /** 点赞高亮色：iOS 红，浅色 / 深色各一档（不跟主题 accent 走，见简报「遗留」）。 */
    val likeColor: Color
        @Composable get() = if (AppTokens.isNightMode) Color(0xFFFF453A) else Color(0xFFFF3B30)
}
