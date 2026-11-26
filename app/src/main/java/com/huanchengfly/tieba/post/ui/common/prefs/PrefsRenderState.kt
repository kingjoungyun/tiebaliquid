package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 设置页渲染层用到的 CompositionLocal 与状态容器。
 *
 * 历史上这里还有一个「玻璃版 / 旧版」双实现开关，已随
 * 「组件只有一套」的收口删除（PLAN §24）。现在 pref 组件固定走玻璃渲染，
 * 只剩下面这些与渲染风格无关、纯粹用于布局协作的状态。
 */

/**
 * 行在所属分组里的下标（0 = 组内第一行）。
 *
 * Glasmio 的分组列表由**行自己**画上边界分隔线（`glasmioGroupedListSeparator`），
 * 并且会自动吞掉「组的第一行」那条线，所以行必须知道自己的下标。
 */
internal val LocalPrefsRowIndex = staticCompositionLocalOf { 0 }

/**
 * 当前分组的「组尾 footnote」收集器。
 *
 * Apple 的设置页把解释性长文案放在组尾的小灰字里，行内只留「标题 + 开关/箭头」。
 * 但 DSL 的 `prefsItem { ... }` 不是 composable 上下文，分组这一层拿不到行里的
 * `summary` 字符串，所以这里反过来：**行在组合时把自己「装不下一行」的解释登记进来**，
 * 组尾再把它们渲染成 footnote。
 *
 * 没有收集器（例如行被单独复用）时为 null，行会退回行内副标题。
 */
internal val LocalPrefsFootnotes = staticCompositionLocalOf<PrefsFootnoteState?> { null }

/**
 * 一组 footnote 的收集器：`行下标 -> 文案`，按下标排序输出，保证 footnote 顺序与行顺序一致。
 *
 * 写入发生在行的 `SideEffect` 里（不是组合期间），读方是组卡片（[PrefsCard]），
 * 因此第一帧没有 footnote、第二帧出现 —— 这个延迟只有一帧，且不会死循环
 * （[report] / [clear] 都做了等值判断，值没变就不写 state）。
 */
@Stable
internal class PrefsFootnoteState {
    private val notes = mutableStateMapOf<Int, String>()

    /** 按行顺序返回已登记的 footnote 文案。 */
    fun ordered(): List<String> = notes.toSortedMap().values.toList()

    /** 登记 / 撤销某一行的 footnote；`text == null` 表示这一行不需要 footnote。 */
    fun report(key: Int, text: String?) {
        if (text == null) {
            clear(key)
        } else if (notes[key] != text) {
            notes[key] = text
        }
    }

    fun clear(key: Int) {
        if (notes.containsKey(key)) notes.remove(key)
    }
}
