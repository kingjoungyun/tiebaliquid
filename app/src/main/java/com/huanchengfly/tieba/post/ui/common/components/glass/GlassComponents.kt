package com.huanchengfly.tieba.post.ui.common.components.glass

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.huanchengfly.tieba.post.BuildConfig

/**
 * Glass 组件层的**逐组件回滚开关**（T2）。
 *
 * 每个组件一个键，默认全开；迁移试点页面在渲染前问一次 [useGlassComponent]，
 * 关掉就退回原来的 M2 实现（试点页面 `if (useGlass) GlassXxx(...) else M2Xxx(...)`）。
 *
 * 调试包可以用 `Settings.Global` 覆盖，**同一个 APK 免重装**做 A/B：
 * ```
 * adb shell settings put global tieba_glass_tabrow 0      # 关掉 TabRow → 回 M2 TabRow
 * adb shell settings put global tieba_glass_tabrow -1     # 恢复默认
 * ```
 * release 恒为默认值（true），不会因为别人随手改了个 global 设置就换掉观感。
 */
enum class GlassComponent(val settingsKey: String) {
    TabRow("tieba_glass_tabrow"),
    TextField("tieba_glass_textfield"),
    TimePicker("tieba_glass_timepicker"),
    PromptDialog("tieba_glass_prompt"),
}

/** 当前是否启用某个 Glass 组件（见 [GlassComponent] 的调试覆盖说明）。 */
@Composable
fun useGlassComponent(component: GlassComponent, default: Boolean = true): Boolean {
    if (!BuildConfig.DEBUG) return default
    val context = LocalContext.current
    return remember(context, component, default) {
        runCatching {
            val forced = Settings.Global.getInt(context.contentResolver, component.settingsKey, -1)
            if (forced == 0 || forced == 1) forced == 1 else default
        }.getOrDefault(default)
    }
}
