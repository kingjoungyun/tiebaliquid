package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.systemGray5
import io.github.neoruaa.glasmio.glass.render.backdrops.emptyBackdrop
import io.github.neoruaa.glasmio.ui.GlasmioCard
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

/**
 * 帖子详情首屏骨架。
 *
 * 结构刻意贴近真实内容（主楼卡 + 3 条楼层），数据到达后直接换成真实列表，
 * 不做闪烁动画，避免二次跳动。
 */
@Composable
fun ThreadSkeleton(
    glassmio: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = if (glassmio) {
        GlasmioTheme.colors.groupedBackground
    } else {
        AppTokens.background
    }
    val blockColor = if (glassmio) {
        GlasmioTheme.colors.systemGray5
    } else {
        AppTokens.chip
    }
    val cardColor = if (glassmio) {
        GlasmioTheme.colors.elevatedBackground
    } else {
        AppTokens.background
    }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topInset = statusBarTop + if (glassmio) ThreadTopBarHeight else 0.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(top = topInset)
            .padding(horizontal = 16.dp),
    ) {
        if (glassmio) {
            GlasmioCard(
                backdrop = remember { emptyBackdrop() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SkeletonMainPost(blockColor = blockColor)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(cardColor)
                    .padding(16.dp),
            ) {
                SkeletonMainPost(blockColor = blockColor)
            }
        }

        repeat(3) {
            SkeletonFloor(blockColor = blockColor)
        }
    }
}

@Composable
private fun SkeletonMainPost(blockColor: Color) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        SkeletonBlock(
            width = 40.dp,
            height = 40.dp,
            color = blockColor,
            shape = CircleShape,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SkeletonBlock(width = 120.dp, height = 14.dp, color = blockColor)
            SkeletonBlock(width = 80.dp, height = 11.dp, color = blockColor)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    SkeletonBlock(width = 180.dp, height = 18.dp, color = blockColor)
    Spacer(modifier = Modifier.height(10.dp))
    SkeletonBlock(height = 13.dp, color = blockColor)
    Spacer(modifier = Modifier.height(6.dp))
    SkeletonBlock(width = 220.dp, height = 13.dp, color = blockColor)
    Spacer(modifier = Modifier.height(14.dp))
    SkeletonBlock(
        height = 180.dp,
        color = blockColor,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun SkeletonFloor(blockColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
    ) {
        SkeletonBlock(
            width = 36.dp,
            height = 36.dp,
            color = blockColor,
            shape = CircleShape,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SkeletonBlock(width = 100.dp, height = 13.dp, color = blockColor)
            SkeletonBlock(width = 70.dp, height = 10.dp, color = blockColor)
            Spacer(modifier = Modifier.height(2.dp))
            SkeletonBlock(height = 12.dp, color = blockColor)
        }
    }
}

@Composable
private fun SkeletonBlock(
    width: Dp? = null,
    height: Dp,
    color: Color,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    val widthModifier = if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()
    Box(
        modifier = widthModifier
            .height(height)
            .clip(shape)
            .background(color)
    )
}
