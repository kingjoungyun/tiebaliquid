package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * 同时设置 `LocalContentColor` / `LocalContentAlpha` 的小工具。
 *
 * 原来挂在 `TextFields.kt`（M2 输入框分叉）的末尾；T3 删掉那个文件时它还有 13 个调用点
 * （菜单、Chip、弹窗、卡片…），所以单独留在这里 —— 它跟输入框无关，是 M2 内容色通道的
 * 通用补充（M2 的 `Text` 同时读这两个 local，只设 `LocalContentColor` 时 alpha 会沿用上层的）。
 */
@Composable
fun ProvideContentColor(
    color: Color,
    alpha: Float = color.alpha,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides color,
        LocalContentAlpha provides alpha,
    ) {
        content()
    }
}
