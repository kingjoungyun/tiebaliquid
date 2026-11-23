package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

/**
 * **M2 原生 Switch + AppTokens 配色**（掉分叉版 `ui.widgets.compose.Switch` 后的统一入口）。
 *
 * 只服务「还没迁移到 Glasmio 的旧 M2 页面」：玻璃页一律用 `PrefsSwitch`（Glasmio 开关）。
 * 颜色全部来自运行时 token，因此换主题 / 换 accent 时和 Glasmio 组件保持一致。
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = AppTokens.accent,
            uncheckedThumbColor = AppTokens.textSecondary,
            uncheckedTrackColor = AppTokens.unselected,
            uncheckedTrackAlpha = 0.6f,
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.6f),
            disabledCheckedTrackColor = AppTokens.accent.copy(alpha = 0.4f),
            disabledUncheckedThumbColor = AppTokens.textDisabled,
            disabledUncheckedTrackColor = AppTokens.unselected.copy(alpha = 0.3f),
        ),
    )
}
