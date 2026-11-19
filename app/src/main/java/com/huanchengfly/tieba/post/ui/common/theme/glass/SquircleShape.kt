package com.huanchengfly.tieba.post.ui.common.theme.glass

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * iOS 风格的「连续圆角」（squircle / 超椭圆，n≈4）。
 *
 * 与 [androidx.compose.foundation.shape.RoundedCornerShape]（正圆弧）的区别：
 * 角部的过渡更长、切线更「贴边」，观感比正圆弧更方一点，即 iOS 的连续圆角。
 *
 * 实现：每个角用**一段三次贝塞尔**近似超椭圆的一个象限，
 * 两个控制点都落在相邻的两条边上，距角点 [CONTROL_OFFSET] × radius
 * （正圆弧的等价偏移是 0.4477r，这里取 0.35r 让角更饱满；已离线渲染对比过 circle / 0.25r / 0.35r）。
 * 控制点落在边上也保证了角与直边在切点处 G1 连续（切线方向一致）。
 *
 * 退化规则：
 * - `radius <= 0`              -> 矩形
 * - `radius >= min(w, h) / 2`  -> 圆形
 */
class SquircleShape(val radius: Dp = GlassTokens.Radii.lg) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusPx = with(density) { radius.toPx() }
        val minSide = size.minDimension
        return when {
            // 退化：矩形
            radiusPx <= 0f || minSide <= 0f ->
                Outline.Rectangle(Rect(Offset.Zero, size))

            // 退化：圆形（半径吃掉短边一半后没有直边了）
            radiusPx >= minSide / 2f ->
                Outline.Generic(Path().apply { addOval(Rect(Offset.Zero, size)) })

            else -> Outline.Generic(squirclePath(size, radiusPx))
        }
    }

    override fun toString(): String = "SquircleShape(radius=$radius)"

    private companion object {
        /**
         * 控制点沿边、距角点的偏移比例（相对 radius）。
         * 0.4477 ≈ 正圆弧；这里取 0.35 让角更接近超椭圆的「方」。
         */
        const val CONTROL_OFFSET = 0.35f
    }

    private fun squirclePath(size: Size, r: Float): Path {
        val w = size.width
        val h = size.height
        val o = r * CONTROL_OFFSET
        val path = Path()

        path.moveTo(r, 0f)
        // 上边 -> 右上角
        path.lineTo(w - r, 0f)
        path.squircleCorner(
            start = Offset(w - r, 0f),
            control1 = Offset(w - o, 0f),
            control2 = Offset(w, o),
            end = Offset(w, r),
        )
        // 右边 -> 右下角
        path.lineTo(w, h - r)
        path.squircleCorner(
            start = Offset(w, h - r),
            control1 = Offset(w, h - o),
            control2 = Offset(w - o, h),
            end = Offset(w - r, h),
        )
        // 下边 -> 左下角
        path.lineTo(r, h)
        path.squircleCorner(
            start = Offset(r, h),
            control1 = Offset(o, h),
            control2 = Offset(0f, h - o),
            end = Offset(0f, h - r),
        )
        // 左边 -> 左上角
        path.lineTo(0f, r)
        path.squircleCorner(
            start = Offset(0f, r),
            control1 = Offset(0f, o),
            control2 = Offset(o, 0f),
            end = Offset(r, 0f),
        )

        path.close()
        return path
    }

    private fun Path.squircleCorner(
        start: Offset,
        control1: Offset,
        control2: Offset,
        end: Offset,
    ) {
        cubicTo(control1.x, control1.y, control2.x, control2.y, end.x, end.y)
    }
}

/** 便捷构造。 */
fun squircle(radius: Dp = GlassTokens.Radii.lg): Shape = SquircleShape(radius)

/** 圆形（胶囊）语义别名，等价于 [CircleShape]。 */
val CapsuleShape: Shape get() = CircleShape
