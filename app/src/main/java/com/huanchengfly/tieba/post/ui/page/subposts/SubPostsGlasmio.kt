package com.huanchengfly.tieba.post.ui.page.subposts

import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.thread.ThreadTopBarHeight
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.glass.GlassSurface
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPageInsets

/**
 * 楼中楼页的悬浮顶栏：返回 + 可选打开原帖 + 滚动后的紧凑标题。
 *
 * 大标题本身在列表第一项里（随列表滚走），这里只在滚动后显示一个默认材质的玻璃胶囊。
 */
@Composable
fun SubPostsGlassTopBar(
    title: String,
    collapsed: Boolean,
    scrimAlpha: Float,
    onBack: () -> Unit,
    onOpenOrigin: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val statusBarTop = GlassPageInsets.statusBarTop()
    val scrimHeight = statusBarTop + 88.dp
    val scrimBrush = Brush.verticalGradient(
        0f to colors.groupedBackground.copy(alpha = 0.82f),
        0.58f to colors.groupedBackground.copy(alpha = 0.48f),
        1f to Color.Transparent,
    )

    Box(modifier = modifier.fillMaxWidth()) {
        if (scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scrimHeight)
                    .background(scrimBrush)
                    .alpha(scrimAlpha)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // insets：这里属于【顶栏/悬浮控件自身】，勿改（见 docs/INSETS-WHITELIST.md）
                .statusBarsPadding()
                .height(ThreadTopBarHeight)
                .padding(horizontal = 8.dp)
        ) {
            GlasmioIconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(id = R.string.button_back),
                    tint = colors.content,
                    modifier = Modifier.size(20.dp),
                )
            }

            AnimatedVisibility(
                visible = collapsed,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(120)),
            ) {
                GlassSurface(
                    modifier = Modifier
                        .height(36.dp)
                        .widthIn(max = 240.dp)
                        .wrapContentWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(
                        text = title,
                        style = IosTypography.subheadline,
                        color = colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                }
            }

            if (onOpenOrigin != null) {
                GlasmioIconButton(
                    onClick = onOpenOrigin,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Icon(
                        imageVector = PhosphorIcons["arrow-square-out"] ?: Icons.Rounded.OpenInBrowser,
                        contentDescription = stringResource(id = R.string.btn_open_origin_thread),
                        tint = colors.content,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * 列表首项里的大标题（34sp w700），随列表滚走；
 * 高度通过 onSizeChanged 回传给页面，用来判断何时切到紧凑标题。
 */
@Composable
fun SubPostsLargeTitle(
    title: String,
    smallTitle: String?,
    onHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { onHeightChanged(it.height) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (!smallTitle.isNullOrBlank()) {
            Text(
                text = smallTitle,
                fontSize = 13.sp,
                color = colors.secondaryContent,
            )
        }
        Text(
            text = title,
            style = LocalTextStyle.current.copy(
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = colors.content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 底栏输入胶囊：和帖子页同款默认 GlassSurface。
 */
@Composable
fun SubPostsGlassBottomBar(
    onClickReply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            onClick = onClickReply,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(id = R.string.tip_reply_thread),
                    style = IosTypography.subheadline,
                    color = colors.secondaryContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
