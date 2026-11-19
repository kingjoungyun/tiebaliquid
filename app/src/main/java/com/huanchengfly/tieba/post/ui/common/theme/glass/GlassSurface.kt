package com.huanchengfly.tieba.post.ui.common.theme.glass

import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 伪玻璃表面（P2 第一版）。
 *
 * 组成：半透明填充 + 1dp 高光描边 + 柔和阴影。
 * 真正的玻璃材质（AGSL / 折射 / 模糊）属于下一阶段，这里刻意保持纯 Compose 绘制。
 *
 * 注意：
 * - 阴影**不用** M2 的 `Surface(elevation=)`，那会画成实色；
 * - `Modifier.shadow` 不支持向下偏移，所以阴影用 `drawBehind` + `Paint.setShadowLayer` 自己画
 *   （minSdk 33，硬件加速下 `setShadowLayer` 对 path 有效）；
 * - 阴影在 `clip` 之前绘制，避免被自身裁剪。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = SquircleShape(GlassTokens.Radii.xxl),
    fill: Color = glassColors().glassFill,
    strokeColor: Color = glassColors().glassStroke,
    shadowColor: Color = glassColors().shadow,
    shadowElevation: Dp = GlassTokens.Glass.shadowElevation,
    shadowOffsetY: Dp = GlassTokens.Glass.shadowOffsetY,
    strokeWidth: Dp = GlassTokens.Glass.strokeWidth,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .glassShadow(
                shape = shape,
                color = shadowColor,
                blur = shadowElevation,
                offsetY = shadowOffsetY,
            )
            .clip(shape)
            .background(fill)
            .border(width = strokeWidth, color = strokeColor, shape = shape),
        content = content,
    )
}

/**
 * 柔和阴影：等价于「黑色 x% + blur + y 偏移」，但完全由我们自己绘制。
 *
 * 因为要用路径阴影，这里把 [Shape] 的 outline 转成原生 `Path` 再画；
 * 三种 [Outline] 都要覆盖（`SquircleShape`/`CircleShape` 一般是 Generic）。
 */
private fun Modifier.glassShadow(
    shape: Shape,
    color: Color,
    blur: Dp,
    offsetY: Dp,
): Modifier {
    if (blur <= 0.dp && offsetY == 0.dp && color.alpha == 0f) return this
    return this.drawBehind {
        val blurPx = blur.toPx()
        if (blurPx <= 0f) return@drawBehind
        val nativePath = shape.createOutline(size, layoutDirection, this).toNativePath()
        drawIntoCanvas { canvas ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                // 完全透明：只画阴影，不画本体
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(blurPx, 0f, offsetY.toPx(), color.toArgb())
            }
            canvas.nativeCanvas.drawPath(nativePath, paint)
        }
    }
}

private fun Outline.toNativePath(): android.graphics.Path = when (this) {
    is Outline.Generic -> path.asAndroidPath()
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }.asAndroidPath()
    is Outline.Rectangle -> Path().apply { addRect(rect) }.asAndroidPath()
}
