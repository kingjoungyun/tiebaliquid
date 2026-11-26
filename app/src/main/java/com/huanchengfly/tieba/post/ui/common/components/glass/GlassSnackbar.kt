package com.huanchengfly.tieba.post.ui.common.components.glass

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * 当前页面的 snackbar 宿主状态。
 *
 * **可空**：由 `AppScaffold` 或页面自己提供；没有提供者时读到的就是 `null`，
 * 调用方应自行 `remember { SnackbarHostState() }` 兜底。
 *
 * 历史：它原来是**非空** local（默认 `error("no scaffold here!")`），页面一旦脱离
 * `AppScaffold` 就会在运行时崩溃。改成可空后，「删掉 AppScaffold」不再需要一次性
 * 改完所有子页面（PLAN §24 #1）。
 */
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState?> { null }

/**
 * 玻璃风格的 snackbar 宿主（T5 批次 3）。
 *
 * 配合 `GlassPage(snackbarHost = { GlassSnackbarHost(hostState) })` 使用：
 * 骨架把它放在 overlay 层的底部，提示是一条**玻璃胶囊**（`AppTokens.card` 材质 +
 * 连续圆角），而不是 M2 那条深灰直角条。
 *
 * 为什么不直接 `LocalSnackbarHostState`：那个 local 由 App 的 `AppScaffold` 提供，
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
