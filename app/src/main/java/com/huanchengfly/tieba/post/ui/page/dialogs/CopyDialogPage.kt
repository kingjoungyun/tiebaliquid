package com.huanchengfly.tieba.post.ui.page.dialogs

import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassComponentSurface
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassTextField
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.GlasmioTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

object CopyTextDialogStyle : DestinationStyle.Dialog {
    override val properties: DialogProperties
        get() = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
}

/**
 * 复制文本弹窗（长按楼层/帖子 → 复制）。
 *
 * T5：去掉 M2 `TitleCentredToolbar`，改成：
 * - 一张玻璃卡（同圆角/内边距），标题 + 关闭按钮在一行，副标题在下；
 * - 正文用 `GlassTextField` 的**只读无壳**形态 —— 它内部是 `BasicTextField(readOnly = true)`，
 *   自带文本选择与「全选/复制」系统菜单，比原来的 `SelectionContainer` + `Text` 更可靠；
 * - 底部两个按钮（复制全部 / 关闭）保持不变。
 */
@Destination(
    style = CopyTextDialogStyle::class
)
@Composable
fun CopyTextDialogPage(
    text: String,
    navigator: DestinationsNavigator,
) {
    val context = LocalContext.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppTokens.windowBackground)
    ) {
        CopyTextPageContent(
            text = text,
            onCopy = {
                TiebaUtil.copyText(context, it)
            },
            onCancel = {
                navigator.navigateUp()
            }
        )
    }
}

@Composable
private fun CopyTextPageContent(
    text: String,
    onCopy: (String) -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassComponentSurface(
            modifier = Modifier
                .fillMaxWidth()
                // 对话框底和卡片在浅色下都是近白：补一圈 1dp 内缘，否则卡片边界看不见
                .border(
                    width = 1.dp,
                    color = GlasmioTheme.colors.subtleStroke,
                    shape = ContinuousRoundedRectangle(24.dp),
                ),
            shape = ContinuousRoundedRectangle(24.dp),
            tint = AppTokens.card,
            hostFill = AppTokens.windowBackground,
            innerGlow = true,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ContinuousRoundedRectangle(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ---- 标题行：标题 + 关闭 ----
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.title_copy),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTokens.text,
                        )
                        Text(
                            text = stringResource(id = R.string.tip_copy_text),
                            fontSize = 12.sp,
                            color = AppTokens.textSecondary,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = PhosphorIcons["x"] ?: Icons.Rounded.Close,
                            contentDescription = stringResource(id = R.string.btn_close),
                            tint = AppTokens.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // ---- 正文：只读、可选择、可滚动 ----
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    GlassTextField(
                        value = text,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        maxLines = Int.MAX_VALUE,
                        showCounter = false,
                        chrome = false,
                        contentPadding = PaddingValues(0.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = AppTokens.text,
                        ),
                    )
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onCopy(text)
                        onCancel()
                    }
                ) {
                    Text(text = stringResource(id = R.string.btn_copy_all))
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onCancel()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppTokens.text.copy(alpha = 0.1f),
                        contentColor = AppTokens.text
                    )
                ) {
                    Text(text = stringResource(id = R.string.btn_close))
                }
            }
        }
    }
}
