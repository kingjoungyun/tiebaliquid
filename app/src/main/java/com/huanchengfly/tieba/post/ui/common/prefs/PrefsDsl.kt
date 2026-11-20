package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.collectPreferenceAsState

/**
 * Receiver scope which is used by [PrefsScreen].
 *
 * 分组的语义完全由这里决定：
 * - [prefsGroup] 开一张新卡片（可选一个标题），组内所有 [prefsItem] 落在同一张卡片里；
 * - 组与组之间自动断卡，页面上表现为「组间距 8dp」；
 * - 两次 [prefsGroup] 之间直接写的 [prefsItem] 会归入一张**隐式卡片**（没有标题），
 *   所以老页面（只写 `prefsItem { ... }`）不需要改写法也能拿到卡片化渲染。
 */
interface PrefsScope {
    /**
     * Adds a single Pref
     *
     * @param content the content of the item
     */
    fun prefsItem(content: @Composable PrefsScope.() -> Unit)

    /**
     * Adds a group of Prefs with a title.
     *
     * @param title Group header text. Will be shown above the list of Prefs
     * @param footer 组尾 footnote（灰小字，渲染在卡片下方、组内最后）。Apple 的设置页把
     * 解释性长文案放在这里，行内只留「标题 + 控件」。`null` = 没有 footnote。
     * @param items All the prefs in this group
     */
    fun prefsGroup(title: String, footer: String? = null, items: PrefsScope.() -> Unit)

    /**
     * Adds a group of Prefs. This overload is intended for passing in a [GroupHeader] if you want more control over the header.
     *
     * @param header Group header. Will be shown above the list of Prefs
     * @param footer 组尾 footnote，语义同上一个重载
     * @param items All the prefs in this group
     */
    fun prefsGroup(
        header: @Composable PrefsScope.() -> Unit,
        footer: String? = null,
        items: PrefsScope.() -> Unit
    )
}

/**
 * 一张分组卡片的数据：可选的分组标题 + 组内各行的下标。
 *
 * 下标指 [PrefsScopeImpl.prefsItems]，渲染时按需取下标的 composable 调用，
 * 这样卡片内部的懒加载粒度和以前一样（每行仍在 LazyColumn 的 item 里……见 [PrefsScreen] 说明）。
 */
internal class PrefsSection {
    /** 分组标题在 `prefsItems` 里的下标；隐式卡片为 null。 */
    var headerIndex: Int? = null

    /** 组尾 footnote（显式传入的那条）；隐式卡片为 null。 */
    var footer: String? = null

    /** 组内各行的下标，按顺序。 */
    val itemIndexes: MutableList<Int> = mutableListOf()
}

internal class PrefsScopeImpl : PrefsScope {

    private val _prefsItems: MutableList<PrefsItem> = mutableListOf()
    val prefsItems: List<PrefsItem> get() = _prefsItems

    private val _sections: MutableList<PrefsSection> = mutableListOf()
    val sections: List<PrefsSection> get() = _sections

    /** 当前「开着的」卡片；`prefsGroup` 结束后重置，于是组后的裸 item 会另起一张隐式卡片。 */
    private var openSection: PrefsSection? = null

    override fun prefsItem(content: @Composable PrefsScope.() -> Unit) {
        _prefsItems.add(
            PrefsItem(
                content = { @Composable { content() } }
            )
        )
        val section = openSection ?: PrefsSection().also {
            _sections.add(it)
            openSection = it
        }
        section.itemIndexes.add(_prefsItems.lastIndex)
    }

    override fun prefsGroup(title: String, footer: String?, items: PrefsScope.() -> Unit) {
        prefsGroup(header = { GroupHeader(title) }, footer = footer, items = items)
    }

    override fun prefsGroup(
        header: @Composable PrefsScope.() -> Unit,
        footer: String?,
        items: PrefsScope.() -> Unit
    ) {
        val section = PrefsSection().also { _sections.add(it) }
        section.footer = footer
        openSection = section

        _prefsItems.add(PrefsItem(content = { @Composable { header() } }))
        section.headerIndex = _prefsItems.lastIndex

        this.apply(items)

        // 组结束：之后的裸 item 另起一张隐式卡片（不再续写进组外那张卡）
        openSection = null
    }

    fun getPrefsItem(index: Int): @Composable () -> Unit {
        val prefsItem = prefsItems[index]
        return prefsItem.content.invoke(this, index)
    }
}

internal class PrefsItem(
    val content: PrefsScope.(index: Int) -> @Composable () -> Unit
)

@Composable
fun depend(key: String): Boolean {
    return LocalPrefsDataStore.current.collectPreferenceAsState(
        key = booleanPreferencesKey(key),
        defaultValue = true
    ).value
}

@Composable
fun dependNot(key: String): Boolean {
    return !LocalPrefsDataStore.current.collectPreferenceAsState(
        key = booleanPreferencesKey(key),
        defaultValue = false
    ).value
}
