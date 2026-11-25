package com.huanchengfly.tieba.post.ui.common.components.glass

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * 玻璃风格的 snackbar 宿主（T5 批次 3）。
 *
 * 配合 `GlassPage(snackbarHost = { GlassSnackbarHost(hostState) })` 使用：
 * 骨架把它放在 overlay 层的底部，提示是一条**玻璃胶囊**（`AppTokens.card` 材质 +
 * 连续圆角），而不是 M2 那条深灰直角条。
 *
 * 为什么不直接 `LocalSnackbarHostState`：那个 local 由 App 的 `MyScaffold` 提供，
 * 一旦页面改用 `GlassPage` 就没有提供者，读了会抛 `no scaffold here!`。
 * 所以页面自己 `remember { SnackbarHostState() }`，再用这个宿主渲染。
 */
@Composable
fun GlassSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) { data ->
        GlassComponentSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = ContinuousRoundedRectangle(18.dp),
            tint = AppTokens.card,
            hostFill = AppTokens.groupedBackground,
            innerGlow = true,
        ) {
            Text(
                text = data.message,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontSize = 14.sp,
                color = AppTokens.text,
            )
        }
    }
}
