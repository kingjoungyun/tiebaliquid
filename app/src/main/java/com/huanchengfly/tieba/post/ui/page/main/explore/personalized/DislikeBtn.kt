package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.personalized.DislikeReason
import com.huanchengfly.tieba.post.api.models.protos.personalized.ThreadPersonalized
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.items
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

/**
 * 「不感兴趣」入口（旧卡片用）：竖直箭头触发。
 *
 * 只保留触发器的差异，菜单内容 / 状态全部在 [DislikeMenu] 里，两种触发器共用同一份实现。
 */
@Composable
fun Dislike(
    personalized: ImmutableHolder<ThreadPersonalized>,
    onDislike: (clickTime: Long, reasons: ImmutableList<ImmutableHolder<DislikeReason>>) -> Unit,
) {
    DislikeMenu(personalized = personalized, onDislike = onDislike) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = PhosphorIcons["caret-down"] ?: Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = AppTokens.textSecondary
            )
        }
    }
}

/**
 * 「不感兴趣」入口（推特式卡片用）：右上角的 `⋯`。
 *
 * 仍是原来的 `DropdownMenu`（`ClickMenu`），本轮不做跨窗口玻璃菜单；只是把触发器从
 * 竖直箭头换成 `⋯` 并交给 `PostCard` 的右上角插槽摆放（尺寸由卡片给，这里铺满即可）。
 */
@Composable
fun DislikeMoreButton(
    personalized: ImmutableHolder<ThreadPersonalized>,
    onDislike: (clickTime: Long, reasons: ImmutableList<ImmutableHolder<DislikeReason>>) -> Unit,
) {
    DislikeMenu(personalized = personalized, onDislike = onDislike) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = PhosphorIcons["dots-three-vertical"] ?: Icons.Rounded.MoreHoriz,
                contentDescription = stringResource(id = R.string.title_more_options),
                tint = AppTokens.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DislikeMenu(
    personalized: ImmutableHolder<ThreadPersonalized>,
    onDislike: (clickTime: Long, reasons: ImmutableList<ImmutableHolder<DislikeReason>>) -> Unit,
    trigger: @Composable () -> Unit,
) {
    var clickTime by remember { mutableStateOf(0L) }
    val selectedReasons = remember { mutableStateListOf<ImmutableHolder<DislikeReason>>() }
    val menuState = rememberMenuState()
    val dislikeResource = personalized.getImmutableList { dislikeResource }
    ClickMenu(
        menuContent = {
            DisposableEffect(personalized) {
                clickTime = System.currentTimeMillis()
                onDispose {
                    selectedReasons.clear()
                }
            }
            ConstraintLayout(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                val (title, grid) = createRefs()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .constrainAs(title) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            top.linkTo(parent.top)
                        }
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.title_dislike),
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(32.dp))
                    Text(
                        text = stringResource(id = R.string.button_submit_dislike),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(color = AppTokens.primary)
                            .clickable {
                                dismiss()
                                onDislike(clickTime, selectedReasons.toImmutableList())
                            }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        color = AppTokens.onAccent,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.subtitle2,
                    )
                }
                VerticalGrid(
                    column = 2,
                    modifier = Modifier
                        .constrainAs(grid) {
                            start.linkTo(title.start)
                            end.linkTo(title.end)
                            top.linkTo(title.bottom, 16.dp)
                            bottom.linkTo(parent.bottom)
                        }
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(
                        items = dislikeResource,
                        span = { if (it.get { dislikeId } == 7) 2 else 1 }
                    ) {
                        val backgroundColor by animateColorAsState(
                            targetValue = if (selectedReasons.contains(it)) AppTokens.primary else AppTokens.chip
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (selectedReasons.contains(it)) AppTokens.onAccent else AppTokens.onChip
                        )
                        Text(
                            text = it.get { dislikeReason },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(color = backgroundColor)
                                .clickable {
                                    if (selectedReasons.contains(it)) {
                                        selectedReasons.remove(it)
                                    } else {
                                        selectedReasons.add(it)
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            color = contentColor,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.subtitle2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        menuState = menuState,
    ) {
        trigger()
    }
}