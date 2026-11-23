package com.huanchengfly.tieba.post.ui.page.main

import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons
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
 * 图形直接来自 Phosphor Icons（`PhosphorIcons`）。键名说明（重要）：`GlasmioTabItem.symbol`
 * 的类型 `SfSymbol` 是**封闭枚举**，没有 `bell`，也无法注册新名字，所以「消息」这一格的键
 * 借用了 `dot.radiowaves.left.and.right`。解析器是唯一的像素来源，键名只是查找依据；
 * 若将来接入 SF 字体，这些键会变成对应的 SF 字形，届时必须重新评估图标策略。
 */
internal val TiebaGlasmioIconResolver = GlasmioIconResolver { systemName ->
    when (systemName) {
        // dock 四格：用 Phosphor 的 **regular** 字重。
        // 试过 fill（house-fill / archive-fill / bell-fill / user-fill）：24dp 下实心块太重，
        // 未选中项一片黑，和页面里的线性图标反差过大（对比图见 stage5_dock_icons_fill.png）。
        // 选中态本来就由「镜头 + accent 着色」表达（见 TiebaDockIcons 顶部注释），不需要 fill 再加一层。
        "house", "house.fill" -> PhosphorIcons["house"]
        "square.3.layers.3d" -> PhosphorIcons["archive"]
        "dot.radiowaves.left.and.right" -> PhosphorIcons["bell"]
        "person", "person.fill" -> PhosphorIcons["user"]
        // 其余（设置行 / 顶栏 / 列表箭头）：按生成脚本里的别名表走（fill 变体保留）。
        else -> PhosphorIcons.resolve(systemName)
    }
}
