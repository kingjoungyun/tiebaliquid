package com.huanchengfly.tieba.post.utils

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.panpf.sketch.fetch.newFileUri
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DisplayResult
import com.github.panpf.sketch.request.execute
import com.github.panpf.sketch.resize.Scale
import com.google.android.material.appbar.AppBarLayout
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.App.Companion.INSTANCE
import com.huanchengfly.tieba.post.App.Companion.translucentBackground
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.getBoolean
import com.huanchengfly.tieba.post.getInt
import com.huanchengfly.tieba.post.getString
import com.huanchengfly.tieba.post.interfaces.BackgroundTintable
import com.huanchengfly.tieba.post.putBoolean
import com.huanchengfly.tieba.post.putString
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.widgets.theme.TintSwipeRefreshLayout
import kotlinx.coroutines.launch
import java.util.Locale

object ThemeUtil {
    val themeState: MutableState<String> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        mutableStateOf(
            if (App.isInitialized) dataStore.getString(KEY_THEME, THEME_DEFAULT)
            else THEME_DEFAULT
        )
    }

    const val TAG = "ThemeUtil"

    /**
     * 全局 accent（主题色）的唯一状态源。
     *
     * 复用 `custom_primary_color`（旧自定义主题的写法不变），但语义升级为「覆盖所有主题的 accent」：
     * - [App.ThemeDelegate] 解析 `colorPrimary` / `colorNewPrimary` / `colorAccent` 时优先读它；
     * - Compose 侧读它属于 snapshot state 读取，所以写入后 `TiebaLiteTheme` 会自动重组；
     * - 半透明主题与动态取色走各自独立分支，不被覆盖。
     *
     * 初始值在进程启动时从 DataStore 同步读取（`themeState` 也是这么做的）。
     */
    val accentState: MutableState<String?> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        mutableStateOf(
            if (App.isInitialized) dataStore.getString(KEY_CUSTOM_PRIMARY_COLOR) else null
        )
    }

    /**
     * 写入全局 accent。`null` = 清除覆盖，回到各主题 XML 自带的颜色。
     *
     * 写入仍走 `appPreferences.customPrimaryColor`（同一个 DataStore 键），不新造存储；
     * 同时更新 [accentState] 让已组合的界面立即重组（DataStore 写入本身是异步的）。
     */
    fun setCustomAccent(color: String?) {
        if (App.isInitialized) {
            INSTANCE.appPreferences.customPrimaryColor = color
            accentState.value = color
        }
    }

    /** 解析当前全局 accent；未设置或格式非法时返回 null。 */
    fun getCustomAccentColor(): Int? {
        val raw = accentState.value ?: return null
        return try {
            Color.parseColor(raw)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** 当前是否设置了全局 accent 覆盖。 */
    fun hasCustomAccent(): Boolean = getCustomAccentColor() != null

    const val KEY_THEME = "theme"
    const val KEY_DARK_THEME = "dark_theme"
    const val KEY_OLD_THEME = "old_theme"
    const val KEY_USE_DYNAMIC_THEME = "useDynamicColorTheme"
    const val KEY_SWITCH_REASON = "switch_reason"
    const val KEY_TRANSLUCENT_PRIMARY_COLOR = "translucent_primary_color"
    const val KEY_CUSTOM_STATUS_BAR_FONT_DARK = "custom_status_bar_font_dark"
    const val KEY_CUSTOM_TOOLBAR_PRIMARY_COLOR = "custom_toolbar_primary_color"
    const val KEY_TRANSLUCENT_THEME_BACKGROUND_PATH = "translucent_theme_background_path"
    const val KEY_USE_DYNAMIC_COLOR_THEME = "useDynamicColorTheme"

    /** 全局 accent 的存储键；沿用旧「自定义主题」的键，不新造一套存储。 */
    const val KEY_CUSTOM_PRIMARY_COLOR = "custom_primary_color"

    const val THEME_TRANSLUCENT = "translucent"
    const val THEME_TRANSLUCENT_LIGHT = "translucent_light"
    const val THEME_TRANSLUCENT_DARK = "translucent_dark"
    const val THEME_CUSTOM = "custom"
    const val THEME_DEFAULT = "tieba"
    const val THEME_BLACK = "black"
    const val THEME_BLUE = "blue"
    const val THEME_PURPLE = "purple"
    const val THEME_PINK = "pink"
    const val THEME_RED = "red"
    const val THEME_BLUE_DARK = "blue_dark"
    const val THEME_GREY_DARK = "grey_dark"
    const val THEME_AMOLED_DARK = "amoled_dark"

    const val TRANSLUCENT_THEME_LIGHT = 0
    const val TRANSLUCENT_THEME_DARK = 1

    private val context: Context
        get() = INSTANCE

    val dataStore: DataStore<Preferences>
        get() = INSTANCE.dataStore

    fun getTextColor(context: Context?): Int {
        return ThemeUtils.getColorByAttr(context, R.attr.colorText)
    }

    @JvmStatic
    fun getSecondaryTextColor(context: Context?): Int {
        return ThemeUtils.getColorByAttr(context, R.attr.colorTextSecondary)
    }

    fun switchToNightMode(context: Activity) {
        switchToNightMode(context, true)
    }

    private fun refreshUI(activity: Activity?) {
        if (activity is BaseActivity<*>) {
            activity.refreshUIIfNeed()
            return
        }
        ThemeUtils.refreshUI(activity)
    }

    private fun getOldTheme(): String {
        val oldTheme =
            dataStore.getString(KEY_OLD_THEME, THEME_DEFAULT).takeUnless { isNightMode(it) }

        return oldTheme ?: THEME_DEFAULT
    }

    fun switchTheme(newTheme: String, recordOldTheme: Boolean = true) {
        if (recordOldTheme) {
            val oldTheme = getRawTheme()
            if (!isNightMode(oldTheme)) {
                dataStore.putString(KEY_OLD_THEME, oldTheme)
            }
        }
        dataStore.putString(KEY_THEME, newTheme)
        themeState.value = newTheme
    }

    fun switchDynamicTheme() {
        val currentUseDynamicTheme = dataStore.getBoolean(KEY_USE_DYNAMIC_THEME, false)
        dataStore.putBoolean(KEY_USE_DYNAMIC_THEME, !currentUseDynamicTheme)
    }

    fun setUseDynamicTheme(useDynamicTheme: Boolean) {
        dataStore.putBoolean(KEY_USE_DYNAMIC_THEME, useDynamicTheme)
    }

    /**
     * 动态取色（Material You）已关（Stage 6）：主题只剩 2 套 + 6 个预设 accent，
     * 系统的 wallpaper 取色不再参与。方法保留是为了不在调用点大改，但**恒返回 false**，
     * 于是 `getCurrentTheme(checkDynamic = true)` 永远不会拼出 `*_dynamic` 主题名。
     * 设置页里 `useDynamicColorTheme` 的入口也已移除（见 AppThemePage）。
     */
    @Suppress("UNUSED_PARAMETER")
    fun isUsingDynamicTheme(): Boolean = false

    fun switchNightMode() {
        if (isNightMode()) {
            switchTheme(getOldTheme(), recordOldTheme = false)
        } else {
            switchTheme(dataStore.getString(KEY_DARK_THEME, THEME_AMOLED_DARK))
        }
    }

    fun switchToNightMode(context: Activity, recreate: Boolean) {
        switchTheme(dataStore.getString(KEY_DARK_THEME, THEME_AMOLED_DARK))
        if (recreate) {
            refreshUI(context)
        }
    }

    @JvmOverloads
    fun switchFromNightMode(context: Activity, recreate: Boolean = true) {
        switchTheme(getOldTheme(), recordOldTheme = false)
        if (recreate) {
            refreshUI(context)
        }
    }

    @JvmStatic
    fun setChipThemeByLevel(level: String?, parent: View, vararg textViews: TextView) {
        setChipTheme(Util.getIconColorByLevel(level), parent, *textViews)
    }

    fun setChipTheme(@ColorInt color: Int, parent: View, vararg textViews: TextView) {
        parent.backgroundTintList = ColorStateList.valueOf(color)
        for (textView in textViews) {
            textView.setTextColor(ThemeUtils.getColorByAttr(parent.context, R.attr.colorOnAccent))
        }
    }

    @JvmStatic
    fun setThemeForSwipeRefreshLayout(swipeRefreshLayout: SwipeRefreshLayout) {
        if (swipeRefreshLayout is TintSwipeRefreshLayout) {
            swipeRefreshLayout.tint()
            return
        }
        val context = swipeRefreshLayout.context
        val resources = context.resources
        if (resources != null) {
            swipeRefreshLayout.setProgressBackgroundColorSchemeColor(resources.getColor(R.color.color_swipe_refresh_bg))
            swipeRefreshLayout.setColorSchemeColors(
                ThemeUtils.getColorByAttr(
                    context,
                    R.attr.colorAccent
                )
            )
        }
    }

    @JvmStatic
    fun isNightMode(): Boolean {
        return isNightMode(getRawTheme())
    }

    @JvmStatic
    fun isNightMode(theme: String): Boolean {
        return theme.lowercase(Locale.getDefault()).contains("dark") && !theme.contains(
            THEME_TRANSLUCENT,
            ignoreCase = true
        )
    }

    fun isTranslucentTheme(): Boolean {
        return isTranslucentTheme(getRawTheme())
    }

    @JvmStatic
    fun isTranslucentTheme(theme: String): Boolean {
        return theme.equals(
            THEME_TRANSLUCENT,
            ignoreCase = true
        ) || theme.contains(
            THEME_TRANSLUCENT,
            ignoreCase = true
        )
    }

    fun isDynamicTheme(theme: String): Boolean {
        return theme.endsWith("_dynamic")
    }

    fun isStatusBarFontDark(): Boolean {
        val theme = getRawTheme()
        val isToolbarPrimaryColor: Boolean = INSTANCE.appPreferences.toolbarPrimaryColor
        return if (theme == THEME_CUSTOM) {
            INSTANCE.appPreferences.customStatusBarFontDark
        } else if (isTranslucentTheme(theme)) {
            theme.contains("dark", ignoreCase = true)
        } else if (!isToolbarPrimaryColor) {
            !isNightMode(theme)
        } else {
            false
        }
    }

    fun isNavigationBarFontDark(): Boolean {
        return !isNightMode()
    }

    fun setTheme(context: Activity) {
        val nowTheme = getCurrentTheme()
        context.setTheme(getThemeByName(nowTheme))
    }

    @JvmOverloads
    fun getCurrentTheme(
        theme: String = getRawTheme(),
        checkDynamic: Boolean = false,
    ): String {
        var nowTheme = theme
        if (isTranslucentTheme(nowTheme)) {
            val colorTheme =
                dataStore.getInt("translucent_background_theme", TRANSLUCENT_THEME_LIGHT)
            nowTheme = if (colorTheme == TRANSLUCENT_THEME_DARK) {
                THEME_TRANSLUCENT_DARK
            } else {
                THEME_TRANSLUCENT_LIGHT
            }
        } else if (checkDynamic && isUsingDynamicTheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            nowTheme = "${nowTheme}_dynamic"
        }
        return nowTheme
    }

    @JvmStatic
    fun setTranslucentThemeWebViewBackground(webView: WebView?) {
        if (webView == null) {
            return
        }
        if (!isTranslucentTheme()) {
            return
        }
        webView.setBackgroundColor(Color.WHITE)
    }

    private fun setAppBarFitsSystemWindow(view: View?, appBarFitsSystemWindow: Boolean) {
        if (view == null) return
        if (view is AppBarLayout) {
            view.setFitsSystemWindows(appBarFitsSystemWindow)
            view.clipToPadding = !appBarFitsSystemWindow
            return
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setAppBarFitsSystemWindow(view.getChildAt(i), appBarFitsSystemWindow)
            }
        }
    }

    fun setTranslucentBackground(view: View?) {
        if (view == null) {
            return
        }
        if (!isTranslucentTheme()) {
            return
        }
        view.backgroundTintList = null
        view.setBackgroundColor(Color.TRANSPARENT)
    }

    @JvmStatic
    fun setTranslucentDialogBackground(view: View?) {
        if (view == null) {
            return
        }
        if (!isTranslucentTheme()) {
            return
        }
        view.backgroundTintList = null
        view.setBackgroundColor(
            ThemeUtils.getColorById(
                view.context,
                R.color.theme_color_card_grey_dark
            )
        )
    }

    fun setTranslucentThemeBackground(
        activity: BaseActivity<*>,
        view: View?,
        setFitsSystemWindow: Boolean = true,
        useCache: Boolean = false,
    ) {
        if (view == null) {
            return
        }
        if (!isTranslucentTheme()) {
            if (setFitsSystemWindow) {
                setAppBarFitsSystemWindow(view, false)
                view.fitsSystemWindows = false
                (view as ViewGroup).clipToPadding = true
            }
            return
        }
        if (setFitsSystemWindow) {
            if (view is CoordinatorLayout) {
                setAppBarFitsSystemWindow(view, true)
                view.setFitsSystemWindows(false)
                (view as ViewGroup).clipToPadding = true
            } else {
                setAppBarFitsSystemWindow(view, false)
                view.fitsSystemWindows = true
                (view as ViewGroup).clipToPadding = false
            }
        }
        view.backgroundTintList = null
        if (view is BackgroundTintable) {
            (view as BackgroundTintable).setBackgroundTintResId(0)
        }
        val backgroundFilePath = dataStore.getString(KEY_TRANSLUCENT_THEME_BACKGROUND_PATH)
        if (backgroundFilePath == null) {
            view.setBackgroundColor(Color.BLACK)
            return
        }
        if (useCache && translucentBackground != null &&
            (translucentBackground !is BitmapDrawable || !(translucentBackground as BitmapDrawable).bitmap.isRecycled)
        ) {
            view.background = translucentBackground
            return
        }
        activity.launch {
            val result = DisplayRequest(activity, newFileUri(backgroundFilePath)) {
                resizeScale(Scale.CENTER_CROP)
            }.execute()
            if (result is DisplayResult.Success) {
                if (useCache) {
                    translucentBackground = result.drawable
                }
                view.background = result.drawable
            } else {
                view.setBackgroundColor(Color.BLACK)
            }
        }
    }

    @StyleRes
    /**
     * 主题裁到 **2 套**（Stage 6）：
     * - 亮色：`TiebaLite.Tieba`（6 个预设 accent 色在运行时覆盖 colorPrimary，不再各配一套 style）
     * - 暗色：`TiebaLite.Dark.Amoled`（纯黑）
     *
     * 旧的 black/purple/pink/red/blue_dark/grey_dark/translucent/custom 共 10 套 style 已删除；
     * 存量用户机器上如果还存着旧 theme 字符串，这里会安全落到这两套之一（不会因为找不到 style 而崩）。
     */
    private fun getThemeByName(themeName: String): Int {
        return when (themeName.lowercase(Locale.getDefault())) {
            THEME_DEFAULT,
            THEME_BLACK,
            THEME_PURPLE,
            THEME_PINK,
            THEME_RED,
            -> R.style.TiebaLite_Tieba

            THEME_AMOLED_DARK,
            THEME_BLUE_DARK,
            THEME_GREY_DARK,
            -> R.style.TiebaLite_Dark_Amoled

            // 半透明主题入口已删：历史值一律按明暗映射，不再有 translucent style
            THEME_TRANSLUCENT, THEME_TRANSLUCENT_LIGHT -> R.style.TiebaLite_Tieba
            THEME_TRANSLUCENT_DARK -> R.style.TiebaLite_Dark_Amoled
            THEME_CUSTOM -> R.style.TiebaLite_Tieba
            else -> R.style.TiebaLite_Tieba
        }
    }

    @JvmStatic
    fun getRawTheme(): String {
        val theme = themeState.value
        return when (theme.lowercase(Locale.getDefault())) {
            THEME_TRANSLUCENT,
            THEME_TRANSLUCENT_LIGHT,
            THEME_TRANSLUCENT_DARK,
            THEME_CUSTOM,
            THEME_DEFAULT,
            THEME_BLACK,
            THEME_PURPLE,
            THEME_PINK,
            THEME_RED,
            THEME_BLUE_DARK,
            THEME_GREY_DARK,
            THEME_AMOLED_DARK,
            -> theme.lowercase(Locale.getDefault())

            else -> THEME_DEFAULT
        }
    }
}