package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import io.github.neoruaa.glasmio.icons.GlasmioIconResolver

/**
 * dock 图标：把 Glasmio 的符号名解析成**贴吧自己的**图标。
 *
 * 为什么要有这一层：Glasmio 的 `SfIcon` 默认渲染 SF Symbols 字体，本工程没有该字体（无授权），
 * 取不到字形时会回落到 `MaterialGlasmioIconResolver` 的 Material 字形 —— 观感与贴吧原有图标
 * 不是一套（PLAN 第 9 节把「图标」列为保留的自定义之一）。`SfIcon` 的解析顺序是
 * `LocalGlasmioIconResolver` → 内置 Material 兜底，所以注入一个解析器就能「既走 Glasmio 的
 * 组件（镜头/按压形变/收缩），又保留贴吧的图形」。
 *
 * 图形来源：`res/drawable/ic_animated_rounded_*.xml` 里 **base vector** 的 pathData
 * （animated-vector 的 `aapt:attr name="android:drawable"` 部分）。`AnimatedImageVector` 在运行
 * 时拿不到静态 `ImageVector`，所以这里把同一份 pathData 原样搬过来，viewport 保持 960×960，
 * 形状逐点一致。丢掉的只有旧 dock 那套 220ms 的描边/缩放动画 —— 新 dock 的选中态改由
 * 「镜头 + accent 着色」表达（见简报的差异清单）。
 *
 * 键名说明（重要）：`GlasmioTabItem.symbol` 的类型 `SfSymbol` 是**封闭枚举**，没有 `bell`，
 * 也无法注册新名字，所以「消息」这一格的键借用了 `dot.radiowaves.left.and.right`。
 * 解析器是唯一的像素来源，键名只是查找依据；若将来接入 SF 字体，这些键会变成对应的 SF 字形，
 * 届时必须重新评估图标策略。
 */
internal object TiebaDockGlyphs {

    /** 动态 / 首页：`ic_animated_rounded_home`（本体就是实心房子，无描边态）。 */
    val Home: ImageVector = vector(
        name = "tieba.dock.home",
        pathData = "M400,800 L400,560 L560,560 L560,800 L760,800 L760,480 L880,480 L480,120 L80,480 L200,480 L200,800 Z",
        fillType = PathFillType.EvenOdd,
    )

    /** 进吧：`ic_animated_rounded_inventory_2`（抽屉式格子）。 */
    val Explore: ImageVector = vector(
        name = "tieba.dock.explore",
        pathData = "M 200 880 Q 167 880 143.5 856.5 Q 120 833 120 800 L 120 349 Q 102 338 91 320.5 Q 80 303 80 280 L 80 160 Q 80 127 103.5 103.5 Q 127 80 160 80 L 800 80 Q 833 80 856.5 103.5 Q 880 127 880 160 L 880 280 Q 880 303 869 320.5 Q 858 338 840 349 L 840 800 Q 840 833 816.5 856.5 Q 793 880 760 880 L 200 880 Z M 200 360 L 200 800 L 760 800 L 760 360 L 200 360 Z M 160 280 L 800 280 L 800 160 L 160 160 L 160 280 Z M 400 560 L 560 560 Q 577 560 588.5 548.5 Q 600 537 600 520 Q 600 503 588.5 491.5 Q 577 480 560 480 L 400 480 Q 383 480 371.5 491.5 Q 360 503 360 520 Q 360 537 371.5 548.5 Q 383 560 400 560 Z M 480 580 Z",
    )

    /** 消息：`ic_animated_rounded_notifications`（铃铛）。 */
    val Notifications: ImageVector = vector(
        name = "tieba.dock.notifications",
        pathData = "M 200 760 Q 183 760 171.5 748.5 Q 160 737 160 720 Q 160 703 171.5 691.5 Q 183 680 200 680 L 240 680 L 240 400 Q 240 317 290 252.5 Q 340 188 420 168 L 420 140 Q 420 115 437.5 97.5 Q 455 80 480 80 Q 505 80 522.5 97.5 Q 540 115 540 140 L 540 168 Q 620 188 670 252.5 Q 720 317 720 400 L 720 680 L 760 680 Q 777 680 788.5 691.5 Q 800 703 800 720 Q 800 737 788.5 748.5 Q 777 760 760 760 L 200 760 Z M 480 460 Z M 480 880 Q 447 880 423.5 856.5 Q 400 833 400 800 L 560 800 Q 560 833 536.5 856.5 Q 513 880 480 880 Z M 320 680 L 640 680 L 640 400 Q 640 334 593 287 Q 546 240 480 240 Q 414 240 367 287 Q 320 334 320 400 L 320 680 Z",
        fillType = PathFillType.EvenOdd,
    )

    /** 我的：`ic_animated_rounded_person`。 */
    val User: ImageVector = vector(
        name = "tieba.dock.user",
        pathData = "M 480 480 Q 414 480 367 433 Q 320 386 320 320 Q 320 254 367 207 Q 414 160 480 160 Q 546 160 593 207 Q 640 254 640 320 Q 640 386 593 433 Q 546 480 480 480 Z M 160 800 L 160 688 Q 160 654 177.5 625.5 Q 195 597 224 582 Q 286 551 350 535.5 Q 414 520 480 520 Q 546 520 610 535.5 Q 674 551 736 582 Q 765 597 782.5 625.5 Q 800 654 800 688 L 800 800 L 160 800 Z M 240 720 L 720 720 L 720 688 Q 720 677 714.5 668 Q 709 659 700 654 Q 646 627 591 613.5 Q 536 600 480 600 Q 424 600 369 613.5 Q 314 627 260 654 Q 251 659 245.5 668 Q 240 677 240 688 L 240 720 Z M 480 400 Q 513 400 536.5 376.5 Q 560 353 560 320 Q 560 287 536.5 263.5 Q 513 240 480 240 Q 447 240 423.5 263.5 Q 400 287 400 320 Q 400 353 423.5 376.5 Q 447 400 480 400 Z M 480 320 Z M 480 720 Z",
        fillType = PathFillType.EvenOdd,
    )

    private fun vector(
        name: String,
        pathData: String,
        fillType: PathFillType = PathFillType.NonZero,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        // 与源文件一致：960×960 的 viewport，图形留白比例也一并保留。
        viewportWidth = 960f,
        viewportHeight = 960f,
    )
        .addPath(
            pathData = addPathNodes(pathData),
            pathFillType = fillType,
            // 颜色由 `Icon(tint = ...)` 的 ColorFilter 覆盖，这里只需要不透明的形状。
            fill = SolidColor(Color.Black),
        )
        .build()
}

/**
 * 贴吧图标解析器：`SfIcon` 先问它，拿不到（不认识的名字）才回落 Glasmio 内置的 Material 表。
 */
internal val TiebaGlasmioIconResolver = GlasmioIconResolver { systemName ->
    when (systemName) {
        "house", "house.fill" -> TiebaDockGlyphs.Home
        "square.3.layers.3d" -> TiebaDockGlyphs.Explore
        "dot.radiowaves.left.and.right" -> TiebaDockGlyphs.Notifications
        "person", "person.fill" -> TiebaDockGlyphs.User
        else -> null
    }
}

