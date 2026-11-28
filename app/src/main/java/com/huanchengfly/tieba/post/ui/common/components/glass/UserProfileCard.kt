package com.huanchengfly.tieba.post.ui.common.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import io.github.neoruaa.glasmio.core.GlasmioTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/** 头像尺寸（主页这一处 80dp 圆形）。 */
val AvatarSize = 80.dp

/**
 * 一行文字的**墨迹盒**几何（两个都是字号的倍数，即 em）。
 *
 * - [inkHeight]：这一行墨迹的高度；
 * - [inkTop]：字形墨迹在行盒内的顶部留白。
 *
 * 为什么需要它（2026-11-28 实测，1080×2400 / 420dpi / **HarmonyOS Sans SC**）：
 *
 * | 字号 | `lineHeight` 设 | 实际行盒 |
 * | --- | --- | --- |
 * | 22sp | 22sp（1.0×，58px） | 68px = 1.172em |
 * | 13sp | 13sp（34px） | 40px = 1.172em |
 * | 15sp | 15sp（39px） | 48px = 1.172em |
 *
 * 也就是说：**`lineHeight = 字号` 压不矮行盒**（鸿蒙字族 ascent 0.928 + descent 0.244
 * = 1.172em 就是下限；Compose 单行文本按字体自然高排版），`includeFontPadding = false`
 * 也只削掉 1px。行盒压不矮 → `SpaceBetween` 均分的仍是「行盒间隙」，三行留白不同 → 墨迹永远不齐。
 *
 * 所以这里反过来做：**自己造一个高度 = 墨迹高的盒子**，把字形整体上移 [inkTop] 塞进去，
 * 盒子的上下边就恰好是墨迹的上下边。这样 `SpaceBetween` 均分的才是真正的墨迹间隙。
 *
 * ⚠️ 系数是**按鸿蒙字族**量的。预览入口（`DebugDialogPreview`）必须包
 * `HarmonyTextProvider`，否则会退回系统兜底 CJK 字体（行盒 1.42em、墨迹系数完全不同），
 * 量出来的数字和真实页对不上 —— 2026-11-28 就踩过这个坑。
 */
private data class InkMetrics(val inkHeight: Float, val inkTop: Float)

/** 名字：22sp 时墨迹顶缩进 5px（0.087em）、墨迹高 0.935em。 */
private val NameInk = InkMetrics(inkHeight = 0.94f, inkTop = 0.09f)

/** ID 行：13sp 时墨迹顶缩进 3px（0.088em）、墨迹高 0.938em。 */
private val MetaInk = InkMetrics(inkHeight = 0.94f, inkTop = 0.09f)

/** 数据行：15sp 时墨迹顶缩进 4px（0.102em）、墨迹高 0.914em。 */
private val StatInk = InkMetrics(inkHeight = 0.91f, inkTop = 0.10f)

/**
 * 把一行内容装进「墨迹盒」：盒子 = 墨迹大小，内容上移 [InkMetrics.inkTop]。
 *
 * 三层结构，缺一层都会错（都实测过）：
 * 1. 外层 Box = 墨迹盒尺寸 → `SpaceBetween` 看到的就是墨迹，均分的才是墨迹间隙；
 * 2. 中层 `wrapContentHeight(unbounded = true)` → 让文字按**自然行盒**（1.44em）测量。
 *    少了这层，文字会被父约束压到墨迹盒高度，**字形底部直接被裁掉**（实测名字被切 15px，
 *    「特没谱先生」的下缘笔画全丢）；
 * 3. 内层 offset 上移 [InkMetrics.inkTop] → 墨迹顶贴住墨迹盒顶，墨迹底正好落在盒底。
 *    只给外层加 offset 会把墨迹盒一起挪走，末行就会浮起来 —— 也踩过。
 */
@Composable
private fun InkLine(
    size: TextUnit,
    metrics: InkMetrics,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // sp → dp 走 LocalDensity，所以系统字体放大时盒子跟着一起放大（与文字同步）
    val em = with(LocalDensity.current) { size.toDp().value }
    Box(modifier.height((em * metrics.inkHeight).dp)) {
        Box(
            Modifier
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .offset(y = -(em * metrics.inkTop).dp)
        ) { content() }
    }
}

/**
 * 三行文字的基础样式：`includeFontPadding = false` + 行高 = 字号（1.0 倍）。
 *
 * **它本身解决不了墨迹对齐**（见 [InkMetrics] 的实测表：行盒仍被钳在 1.172em），
 * 保留它只为了两件事：去掉 Android 的字形上下留白（实测 1px）、行高不再比字号更松，
 * 让 [InkMetrics] 的 em 系数稳定、可预测。真正的对齐交给 [InkLine] 的墨迹盒。
 *
 * 详细实测过程见 `docs/PROFILE-CARD-INK.md`。
 */
private fun TextStyle.tight(size: TextUnit): TextStyle = copy(
    fontSize = size,
    lineHeight = size,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

/**
 * 个人主页「资料卡」的数据：把 `User` 拍平成基础类型，便于**复用**（真实页面 + debug 预览）。
 *
 * 为什么单独抽出来：预览入口要能显示完整资料区，但造一个假 `User`
 * 需要填几十个接口字段。拍平成 [UserProfileCardSpec] 后，预览只要给几个字符串。
 *
 * 注意：**动作（关注 / 已关注 / 编辑资料）不在卡片里**，它在 tab 行右侧的
 * [ProfileActionPill]；卡片只负责「头像 + 名字 / 元信息 / 数据 + 简介」。
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
    /** 吧务等级 / 神认证之类的一行徽标文字（没有就 null）。 */
    val badgeText: String? = null,
    /** 仅 debug 预览用：本地 drawable 资源 id（非 null 时优先于 [portraitUrl]）。 */
    val portraitRes: Int? = null,
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
 * 排版：名字 22/26/30、ID 13、数据 15；三行各自装进「墨迹盒」（见 [InkMetrics] / [InkLine]），
 * 由 `SpaceBetween` 均分剩余空间。墨迹间隙不是写死的常数：它等于
 * （头像高 − 三行墨迹高之和）/ 2，所以 22sp 时约 15.8dp、26/30sp 时约 11.8dp。
 */
@Composable
fun UserProfileCard(
    spec: UserProfileCardSpec,
    avatarSize: Dp = AvatarSize,
    nameSp: TextUnit = 22.sp,
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
            // 1dp 细描边：浅色下白卡叠在 #F2F2F7 页底上本来就有对比，但深色下
            // 卡片(#1C1C1E)与页底(纯黑)只差一点，加一道 8%/10% 的 hairline 才「一眼看出边界」。
            .border(width = 1.dp, color = colors.subtleStroke, shape = RoundedCornerShape(26.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 头像：主页这一处 80dp 圆形（其它页面仍按原规则）
            if (spec.portraitRes != null) {
                // 预览用本地资源：ndroid.resource:// 在 Coil 里不稳，直接 painterResource
                Image(
                    painter = painterResource(id = spec.portraitRes),
                    contentDescription = stringResource(id = R.string.user_portrait),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape),
                )
            } else {
                Avatar(
                    data = spec.portraitUrl,
                    size = avatarSize,
                    contentDescription = stringResource(id = R.string.user_portrait),
                    modifier = Modifier
                        .clip(CircleShape),
                )
            }

            // 文字列高度钉死成头像高度（80dp），内部 SpaceBetween：三行各装进自己的「墨迹盒」
            // （见 [InkLine]），于是 SpaceBetween 均分的是**墨迹间隙** ——
            // 名字墨迹顶 = 头像顶、数据墨迹底 = 头像底、两处墨迹间隙相等，都不需要额外 offset。
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(avatarSize),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                InkLine(size = nameSp, metrics = NameInk) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = spec.name,
                            style = LocalTextStyle.current.tight(nameSp).copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (sexEmoji != null) {
                            Text(
                                text = sexEmoji,
                                style = LocalTextStyle.current.tight(13.sp),
                                color = AppTokens.textSecondary,
                            )
                        }
                    }
                }
                InkLine(size = 13.sp, metrics = MetaInk) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = metaLine,
                            style = LocalTextStyle.current.tight(13.sp),
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
                            // requiredSize 而不是 size：ID 行的墨迹盒只有 0.94×13sp ≈ 12.2dp，
                            // size 会被父约束压小；这里保住 15dp 的点击区域，溢出部分画在盒外。
                            modifier = Modifier
                                .requiredSize(15.dp)
                                .clickable(onClick = onCopyIdClick),
                        )
                    }
                }
                InkLine(size = 15.sp, metrics = StatInk) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                }
            }
        }

        // 简介行：动作（关注 / 已关注 / 编辑资料）已经移到 tab 行右侧的胶囊里，
        // 卡片里只剩简介，宽度全给文字。
        Text(
            text = spec.intro.takeIf { it.isNotEmpty() }
                ?: stringResource(id = R.string.tip_no_intro),
            style = LocalTextStyle.current.copy(fontSize = 15.sp, lineHeight = 20.sp),
            color = AppTokens.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

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
            style = LocalTextStyle.current.tight(15.sp),
            color = AppTokens.textSecondary,
        )
        Text(
            text = value,
            style = LocalTextStyle.current.tight(15.sp).copy(fontWeight = FontWeight.SemiBold),
            color = AppTokens.text,
        )
    }
}

/** 资料卡里的一项数据：标签（次级色 15/20）+ 数字（主文字色 SemiBold），整项可点。 */
