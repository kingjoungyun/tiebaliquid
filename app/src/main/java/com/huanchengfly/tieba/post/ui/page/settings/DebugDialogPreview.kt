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

    // ── 确认弹窗：标题 + 一句话 + 正负两个文字按钮
    ConfirmDialog(
        dialogState = confirmDialogState,
        onConfirm = {},
        title = { GlassDialogTitle { Text("确认弹窗预览") } },
    ) {
        Text("固定假数据：这是一个确认类弹窗，只有标题、一句话和两个文字按钮。")
    }
}
