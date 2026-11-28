package com.huanchengfly.tieba.post.ui.page.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassDialogTitle
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.fillMaxWidth
import com.huanchengfly.tieba.post.ui.common.components.glass.ProfileAction
import com.huanchengfly.tieba.post.ui.common.components.glass.ProfileActionPill
import com.huanchengfly.tieba.post.ui.common.components.glass.ProfileHeaderTabs
import com.huanchengfly.tieba.post.ui.common.components.glass.UserProfileCard
import com.huanchengfly.tieba.post.ui.common.components.glass.UserProfileCardSpec
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.HarmonyTextProvider
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.sp

/**
 * **仅 debug 包**的弹窗预览（设置 → 关于组末尾的三行入口）。
 *
 * 为什么需要：列表选择 / 文本输入 / 确认这三类弹窗以前只能靠业务入口才能截到图
 * （要登录态、要收藏过帖子、要关注过吧…），UI 验收时几乎截不到。
 * 这里用**固定假数据**搭三个弹窗，入口由 `SettingsPage` 用 `BuildConfig.DEBUG` 包住，
 * release 包里根本不存在。
 *
 * 调用方只需要准备三个 [DialogState] 并在三行入口的 onClick 里 `show()`。
 */
@Composable
fun DebugDialogPreviewDialogs(
    listDialogState: DialogState,
    promptDialogState: DialogState,
    confirmDialogState: DialogState,
    profileCardDialogState: DialogState,
) {
    // ── 列表选择：4 项，默认选中第 2 项（验证 accent 文字 + 勾、无高亮底）
    var selected by remember { mutableStateOf(1) }
    Dialog(
        dialogState = listDialogState,
        title = { GlassDialogTitle { Text("列表选择预览") } },
        buttons = {
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
        },
    ) {
        ListSinglePicker(
            itemTitles = persistentListOf("选项 A", "选项 B（已选中）", "选项 C", "选项 D"),
            itemValues = persistentListOf("a", "b", "c", "d"),
            selectedPosition = selected,
            onItemSelected = { position, _, _, _ -> selected = position },
        )
    }

    // ── 文本输入：标题 + 说明 + 两个文字按钮（输入框截图请用「屏蔽词」真实入口，
    //    真实入口走 GlassPromptDialog，才有 GlassTextField）
    Dialog(
        dialogState = promptDialogState,
        title = { GlassDialogTitle { Text("文本输入预览") } },
        buttons = {
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
            DialogPositiveButton(text = stringResource(id = R.string.button_sure_default))
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("固定假数据：这里是一段说明文字，用来检查正文 15sp / 次级色的观感。")
        }
    }

    // ── 个人主页资料卡（固定假数据，含「关注」按钮态）：不走真实用户导航也能验收布局
    Dialog(
        dialogState = profileCardDialogState,
        title = { GlassDialogTitle { Text("资料卡预览") } },
        buttons = {
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
        },
    ) {
        var avatarSize by remember { mutableStateOf(80) }
        var nameSp by remember { mutableStateOf(22) }
        var action by remember { mutableStateOf(ProfileAction.Follow) }
        var tabIndex by remember { mutableStateOf(0) }
        // 仅 debug：三组对比（B = 22sp/80dp、B2 = 26sp/76dp、B3 = 30sp/80dp）
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 4.dp),
        ) {
            listOf(22, 26, 30).forEach { size ->
                Text(
                    text = "名字 ${size}sp",
                    fontSize = 13.sp,
                    color = if (nameSp == size) AppTokens.primary else AppTokens.textSecondary,
                    modifier = Modifier
                        .clickable { nameSp = size }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 4.dp),
        ) {
            listOf(80, 76, 74).forEach { size ->
                Text(
                    text = "头像 ${size}dp",
                    fontSize = 13.sp,
                    color = if (avatarSize == size) AppTokens.primary else AppTokens.textSecondary,
                    modifier = Modifier
                        .clickable { avatarSize = size }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        // 动作胶囊三态（v1.9.2：胶囊已从卡片挪到 tab 行右侧）
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            listOf(
                ProfileAction.Follow to "动作 未关注",
                ProfileAction.Following to "动作 已关注",
                ProfileAction.EditProfile to "动作 编辑资料",
            ).forEach { (a, label) ->
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = if (action == a) AppTokens.primary else AppTokens.textSecondary,
                    modifier = Modifier
                        .clickable { action = a }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        // 弹窗是独立 Window，拿不到页面的 LiquidGlassTheme：不补这层的话
        // GlasmioTheme.colors 会退回**默认浅色**，深色下就是「白卡 + 白字」。
        LiquidGlassTheme(accent = AppTokens.primary) {
        // 必须补 HarmonyTextProvider：真实主页在 GlassPage 里，M2 Text 用的是鸿蒙字族；
        // 少了这层，预览会用系统兜底 CJK 字体，行盒 1.42em vs 真实页 1.172em —— 量出来的
        // 墨迹位置和真实页完全不是一回事（2026-11-28 踩过）。
        HarmonyTextProvider {
        Column(modifier = Modifier.fillMaxWidth()) {
        UserProfileCard(
            avatarSize = avatarSize.dp,
            nameSp = nameSp.sp,
            spec = UserProfileCardSpec(
                name = "特没谱先生",
                sex = 2,
                id = 1234567L,
                ipAddress = "河北",
                tbAge = "10.1",
                concernNum = 220,
                fansNum = 0,
                agreeNum = 9055,
                intro = "别泥吗点关注我！",
                portraitUrl = "",
                // 预览用本地 drawable，保证能看到真实图片（不是空灰圆）
                portraitRes = com.huanchengfly.tieba.post.R.drawable.ic_launcher_new_round,
            ),
            onCopyIdClick = {},
            onFollowListClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        // tab 行 + 右侧动作胶囊（真实页的玻璃轨道在弹窗里没有 backdrop host，
        // 这里用 M2 样式画同一套布局：滚动区域 + 胶囊位置/尺寸可验）
        ProfileHeaderTabs(
            labels = persistentListOf("贴子 36", "关注吧 69"),
            selectedIndex = tabIndex,
            onSelected = { tabIndex = it },
            glass = false,
            action = {
                ProfileActionPill(action = action, onClick = {})
            },
            modifier = Modifier.padding(top = 8.dp),
        )
        }
        }
        }
    }

    // ── 确认弹窗：标题 + 一句话 + 正负两个文字按钮
    ConfirmDialog(
        dialogState = confirmDialogState,
        onConfirm = {},
        title = { GlassDialogTitle { Text("确认弹窗预览") } },
    ) {
        Text("固定假数据：这是一个确认类弹窗，只有标题、一句话和两个文字按钮。")
    }
}
