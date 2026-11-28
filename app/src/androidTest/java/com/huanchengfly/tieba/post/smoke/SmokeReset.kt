package com.huanchengfly.tieba.post.smoke

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences

/**
 * 冒烟测试的**公共前置**：把 App 恢复到「已知状态」（两个测试类都调它）。
 *
 * 为什么需要：以前两个测试类各自 `@Before` 只做「拉起 App」，**主题和页面栈都是上一条用例
 * 留下的**。实测两次全量冒烟的失败都是这么来的：
 * - `nightModeToggleKeepsAppAlive` 把深色留开了 → 像素断言在「深色页面 + 浅色状态栏」的瞬时态采样；
 * - 前一条失败后 App 停在非首页 → 后续用例点不到帖子卡。
 *
 * 这里只**恢复状态**、不做任何断言（断言仍在各用例里）：
 * 1. 关掉「跟随系统夜间」（否则主题会被 Activity.onResume 打回去）；
 * 2. 强制浅色主题（同进程直接调 `ThemeUtil.switchTheme`，再重启 Activity 生效）；
 * 3. 冷启到首页，并把残留的二级页用 BACK 退掉（最多 5 次，「返回钮是否存在」判定）。
 *
 * ⚠️ 绝对不能 `am force-stop`：instrumentation 就跑在被测进程里，force-stop 会连自己一起杀
 * （历史坑：表现为 `Process crashed` + 「PASS 但耗时 0.05s」的假通过）。
 */
object SmokeReset {

    private const val TAG = "SmokeReset"
    const val PACKAGE = "com.tieba"
    const val MAIN_ACTIVITY = "com.huanchengfly.tieba.post.MainActivityV2"
    private const val LAUNCH_SETTLE_MS = 5500L
    private const val MAX_BACKS = 5

    fun reset(device: UiDevice, context: Context = InstrumentationRegistry.getInstrumentation().targetContext) {
        runCatching { device.wakeUp() }
        runCatching { device.executeShellCommand("wm dismiss-keyguard") }

        // 1) 关跟随系统夜间
        runCatching { context.appPreferences.followSystemNight = false }

        // 2) 强制浅色
        var action = "already-light"
        if (runCatching { ThemeUtil.isNightMode() }.getOrDefault(false)) {
            runCatching { ThemeUtil.switchTheme(ThemeUtil.THEME_DEFAULT) }
            action = "switched-to-light"
        }

        // 3) 冷启到首页
        device.executeShellCommand("am start -W -f 0x10008000 -n $PACKAGE/$MAIN_ACTIVITY")
        SystemClock.sleep(LAUNCH_SETTLE_MS)

        // 4) 退掉残留二级页
        val backDesc = context.getString(R.string.button_back)
        var backs = 0
        while (backs < MAX_BACKS && device.hasObject(By.desc(backDesc))) {
            device.pressBack()
            SystemClock.sleep(350)
            backs++
        }
        SystemClock.sleep(500)

        Log.i(
            TAG,
            "RESET theme=${if (ThemeUtil.isNightMode()) "dark" else "light"}" +
                " action=$action back=$backs",
        )
    }
}
