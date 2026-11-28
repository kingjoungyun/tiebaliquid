package com.huanchengfly.tieba.post.ui.common.components.glass

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * 资料页右上角的**动作胶囊**（未关注 / 已关注 / 编辑资料）。
 *
 * 规范（v1.9.2 用户定，见 `docs/PROFILE-CARD-SPEC.md`）：
 * 高 32dp、圆角 16dp、水平内边距 14dp、文字 15sp Medium。
 *
 * 三态由 [filled] 决定：
 * - 填充态：`AppTokens.accent` 底 + `AppTokens.onAccent` 字（未关注）；
 * - 描边态：透明底 + 1dp 描边 + `AppTokens.text` 字（已关注 / 编辑资料）。
 *
 * 为什么不放在资料卡里：用户要求它和「帖子 / 关注吧」tab 同一行、右对齐，卡片里不再有动作。
 */
enum class ProfileAction(@StringRes val labelRes: Int, val filled: Boolean) {
    /** 未关注：accent 填充 + 白字。 */
    Follow(R.string.button_follow, filled = true),

    /** 已关注：透明 + 1dp 描边 + 主文字色（点一下 = 取消关注，行为没变）。 */
    Following(R.string.button_followed, filled = false),

    /** 本人：编辑资料，与已关注同款胶囊。 */
    EditProfile(R.string.menu_edit_info, filled = false),
}

/** 胶囊高度（也是圆角半径的两倍：32dp 高 / 16dp 圆角）。 */
val ProfileActionPillHeight = 32.dp

/** 胶囊水平内边距。 */
val ProfileActionPillHorizontalPadding = 14.dp

/**
 * 画一个动作胶囊。
 *
 * @param label 文字；默认取 [ProfileAction.labelRes]，吧页「签到」这类复用场景可直接传文案
 */
@Composable
fun ProfileActionPill(
    action: ProfileAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(action.labelRes),
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(ProfileActionPillHeight / 2)
    val background = if (action.filled) AppTokens.accent else Color.Transparent
    val contentColor = if (action.filled) AppTokens.onAccent else AppTokens.text
    // 描边用主文字色的 24%：浅色下看得见但不抢眼，深色下同样成立（不依赖 XML divider 的深浅值）
    val strokeColor = if (action.filled) Color.Transparent else AppTokens.text.copy(alpha = 0.24f)

    Row(
        modifier = modifier
            .height(ProfileActionPillHeight)
            .clip(shape)
            .background(background)
            .border(width = 1.dp, color = strokeColor, shape = shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = ProfileActionPillHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.4f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/**
 * 资料页的**tab 行 + 右侧动作**。
 *
 * 结构：`Row { 可横向滚动的 tab 轨道（weight 1f）; 8dp; 动作胶囊 }`，整体左右各 16dp。
 * tab 多了只会在自己的轨道里滚动，**不会把胶囊挤换行**。
 *
 * @param glass 是否用玻璃分段控件（真实页 true）；预览弹窗里没有 backdrop host，
 *              传 false 走 M2 的 [ScrollableTabRow]，布局与滚动行为一致
 */
@Composable
fun ProfileHeaderTabs(
    labels: ImmutableList<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    glass: Boolean = useGlassComponent(GlassComponent.TabRow),
    action: (@Composable () -> Unit)? = null,
) {
    if (labels.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (glass) {
                GlassScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    onTabSelected = onSelected,
                    tabCount = labels.size,
                    modifier = Modifier.wrapContentWidth(align = Alignment.Start),
                ) { index, selected ->
                    GlassTabLabel(text = labels[index], selected = selected)
                }
            } else {
                ScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    divider = {},
                    backgroundColor = Color.Transparent,
                    contentColor = AppTokens.primary,
                    edgePadding = 0.dp,
                    modifier = Modifier.wrapContentWidth(align = Alignment.Start),
                ) {
                    labels.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedIndex == index,
                            onClick = { onSelected(index) },
                            selectedContentColor = AppTokens.primary,
                            unselectedContentColor = AppTokens.textSecondary,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .height(48.dp)
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.button.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.sp,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
        if (action != null) {
            Spacer(modifier = Modifier.width(8.dp))
            action()
        }
    }
}

/** 空列表便捷值（预览/占位用）。 */
val EmptyProfileTabs: ImmutableList<String> = persistentListOf()
