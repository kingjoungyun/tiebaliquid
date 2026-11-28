package com.huanchengfly.tieba.post.ui.common.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import io.github.neoruaa.glasmio.core.GlasmioTheme

/**
 * 个人主页「资料卡」的数据：把 `User` 拍平成基础类型，便于**复用**（真实页面 + debug 预览）。
 *
 * 为什么单独抽出来：预览入口要能显示完整资料区（含关注按钮状态），但造一个假 `User`
 * 需要填几十个接口字段。拍平成 [UserProfileCardSpec] 后，预览只要给几个字符串。
 */
data class UserProfileCardSpec(
    val name: String,
    val sex: Int,
    val id: Long,
    val ipAddress: String,
    val tbAge: String,
    val concernNum: Int,
    val fansNum: Int,
    val agreeNum: Int,
    val intro: String,
    val portraitUrl: String,
    val showBtn: Boolean,
    val isSelf: Boolean,
    val hasConcerned: Int,
    /** 吧务等级 / 神认证之类的一行徽标文字（没有就 null）。 */
    val badgeText: String? = null,
)

/**
 * 个人主页资料卡（v1.9.2 改版）。
 *
 * 布局（名字顶 = 头像顶、数据底 = 头像底）：
 * ```
 * ╭──────────────────────────────────────────╮
 * │ ╭──────╮ 名字 ♀                            │
 * │ │ 80dp │ ID 12345 · 河北 · 10.1 YEAR  ⧉   │
 * │ ╰──────╯ 关注 220  粉丝 0  获赞 9055       │
 * │ 个性签名…                                  │
 * ╰──────────────────────────────────────────╯
 * ```
 * 排版：名字 22/26、ID 13/18、数据 15/20；布局间距 3.5dp（名字→ID）与 3dp（ID→数据），
 * 行盒自带约 4.5dp 空隙，所以**视觉间隙都是 8dp**。
 */
@Composable
fun UserProfileCard(
    spec: UserProfileCardSpec,
    onBtnClick: () -> Unit,
    onCopyIdClick: () -> Unit,
    onFollowListClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val sexEmoji = when (spec.sex) {
        1 -> "♂"
        2 -> "♀"
        else -> null
    }
    // 「ID xxx · 河北 · 10.1 YEAR」：精简过的三段，空项自动跳过
    val metaLine = buildList {
        add("ID " + spec.id)
        if (spec.ipAddress.isNotEmpty()) add(spec.ipAddress)
        if (spec.tbAge.isNotEmpty()) add(spec.tbAge + " YEAR")
    }.joinToString(" · ")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(colors.elevatedBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 头像：主页这一处 80dp 圆形（其它页面仍按原规则）
            Avatar(
                data = spec.portraitUrl,
                size = 80.dp,
                contentDescription = stringResource(id = R.string.user_portrait),
                modifier = Modifier
                    .clip(CircleShape),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = spec.name,
                        style = LocalTextStyle.current.copy(
                            fontSize = 22.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (sexEmoji != null) {
                        Text(
                            text = sexEmoji,
                            fontSize = 13.sp,
                            color = AppTokens.textSecondary,
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = metaLine,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = AppTokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        imageVector = PhosphorIcons["copy"] ?: Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(
                            id = R.string.text_profile_user_id,
                            spec.id.toString(),
                        ),
                        tint = AppTokens.textSecondary,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable(onClick = onCopyIdClick),
                    )
                }
            }

        }

        // 数据行铺满卡片宽度：手机宽度只有 411dp，塞进「头像右侧那一列」会被挤成竖排。
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 3.dp),
        ) {
            ProfileStat(
                label = stringResource(id = R.string.text_stat_follow),
                value = spec.concernNum.getShortNumString(),
                onClick = onFollowListClick,
            )
            ProfileStat(
                label = stringResource(id = R.string.text_stat_fans),
                value = spec.fansNum.getShortNumString(),
            )
            ProfileStat(
                label = stringResource(id = R.string.text_stat_agrees),
                value = spec.agreeNum.getShortNumString(),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = spec.intro.takeIf { it.isNotEmpty() }
                    ?: stringResource(id = R.string.tip_no_intro),
                style = LocalTextStyle.current.copy(fontSize = 15.sp, lineHeight = 20.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // 关注 / 已关注 / 编辑资料：放在简介行右侧，避免和数据行抢宽度
            if (spec.showBtn) {
                FollowButton(spec = spec, onBtnClick = onBtnClick)
            }
        }

        if (spec.badgeText != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = PhosphorIcons["seal-check"] ?: Icons.Rounded.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AppTokens.primary,
                )
                Text(
                    text = spec.badgeText!!,
                    style = LocalTextStyle.current.copy(fontSize = 15.sp),
                    color = AppTokens.primary,
                )
            }
        }
    }
}

/** 资料卡里的一项数据：标签（次级色 15/20）+ 数字（主文字色 SemiBold），整项可点。 */
@Composable
private fun ProfileStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = AppTokens.textSecondary,
        )
        Text(
            text = value,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTokens.text,
        )
    }
}

/** 关注 / 已关注 / 编辑资料 三种形态的按钮。 */
@Composable
private fun FollowButton(spec: UserProfileCardSpec, onBtnClick: () -> Unit) {
    Button(
        onClick = onBtnClick,
        colors = if (spec.hasConcerned == 0 || spec.isSelf) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.outlinedButtonColors()
        },
        border = if (spec.hasConcerned == 0 || spec.isSelf) {
            null
        } else {
            ButtonDefaults.outlinedBorder
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (spec.isSelf) {
                Icon(
                    imageVector = PhosphorIcons["pencil-simple"] ?: Icons.Rounded.Edit,
                    contentDescription = null,
                )
                Text(text = stringResource(id = R.string.menu_edit_info))
            } else if (spec.hasConcerned == 0) {
                Icon(
                    imageVector = PhosphorIcons["plus"] ?: Icons.Rounded.Add,
                    contentDescription = null,
                )
                Text(text = stringResource(id = R.string.button_follow))
            } else {
                Text(text = stringResource(id = R.string.button_unfollow))
            }
        }
    }
}
