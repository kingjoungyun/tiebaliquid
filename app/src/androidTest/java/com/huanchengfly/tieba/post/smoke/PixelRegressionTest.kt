package com.huanchengfly.tieba.post.smoke

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import com.huanchengfly.tieba.post.R
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

/**
 * 像素级回归断言（D 线）。
 *
 * 为什么单独一个类：这两个断言检查的是**渲染结果**（状态栏区域颜色、深色主体是否被白块糊住），
 * 而不是「结构上有没有那个控件」，所以用 `device.takeScreenshot()` + `Bitmap.getPixel`，
 * 与 [MainSmokeTest] 的结构断言互补。
 *
 * 这两条断言对应 v1.9.0 修过的两类真实 bug：
 * 1. 状态栏出现白条 / 分界线（首页顶栏白块、消息 tab 深色 header 白底）；
 * 2. 深色下大片纯白（玻璃控件没拿到深色调色板 / 材质退回白实底）。
 */
@RunWith(AndroidJUnit4::class)
class PixelRegressionTest {

    @get:Rule
    val testName = TestName()

    private lateinit var device: UiDevice

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun str(resId: Int): String = context.getString(resId)

    private val displayDensity: Float
        get() = context.resources.displayMetrics.density

    /** 状态栏高度（px）。系统 dimen 拿不到时退回「屏高 5%」这个经验值。 */
    private val statusBarPx: Int
        get() {
            val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
            val fromRes = if (id > 0) context.resources.getDimensionPixelSize(id) else 0
            return if (fromRes > 0) fromRes else (device.displayHeight * 0.053f).toInt()
        }

    @Before
    fun freshLaunch() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        runCatching { device.wakeUp() }
        runCatching { device.executeShellCommand("wm dismiss-keyguard") }
        relaunchApp()
        awaitDockReady()
    }

    // ------------------------------------------------------------------ 断言 1

    /**
     * 状态栏区域不能和页面内容区「颜色断层」。
     *
     * 采样点：
     * - 状态栏中心 `(w/2, statusBarPx/2)`
     * - 内容区 `(w/2, statusBarPx + 200px)`
     *
     * 断言：每个通道差 ≤ [CHANNEL_TOLERANCE]。
     *
     * 为什么这样能抓到「白条」：整页背景铺满时两点同色；一旦状态栏那条被别的容器
     * （白底顶栏 / 未自适应的 scrim）盖住，就会出现 200+ 的通道差。
     */
    @Test
    fun statusBarMatchesPageBackground() {
        forEachMainPage { pageName ->
            val bitmap = screenshot()
            try {
                val w = bitmap.width
                val sbH = statusBarPx
                val statusBar = bitmap.getPixel(w / 2, sbH / 2)
                val content = bitmap.getPixel(w / 2, sbH + 200)
                val dr = kotlin.math.abs(red(statusBar) - red(content))
                val dg = kotlin.math.abs(green(statusBar) - green(content))
                val db = kotlin.math.abs(blue(statusBar) - blue(content))
                Log.i(
                    TAG,
                    "$pageName 状态栏=${hex(statusBar)} 内容区=${hex(content)} Δ=($dr,$dg,$db)",
                )
                assertTrue(
                    "$pageName：状态栏区域与内容区颜色断层过大 Δ=($dr,$dg,$db) " +
                        "（状态栏=${hex(statusBar)} 内容区=${hex(content)}）——" +
                        "检查该页背景是否铺到状态栏、顶栏是不是白底",
                    dr <= CHANNEL_TOLERANCE && dg <= CHANNEL_TOLERANCE &&
                        db <= CHANNEL_TOLERANCE,
                )
            } finally {
                bitmap.recycle()
            }
        }
    }

    // ------------------------------------------------------------------ 断言 2

    /**
     * 深色下页面主体不能出现「大片纯白」。
     *
     * 做法：切到深色 → 逐页截图 → 统计主体区（顶栏以下、dock 以上）里
     * 「接近 #FFFFFF」的像素占比，超过 [WHITE_BLOCK_TOLERANCE] 判失败。
     *
     * 这条能抓到：玻璃控件在深色下退回白实底、header 拿到浅色调色板（今天 Messages tab 的白块）。
     */
    @Test
    fun darkModeHasNoLargeWhiteBlocks() {
        setDarkMode(enabled = true)
        try {
            forEachMainPage { pageName ->
                val bitmap = screenshot()
                try {
                    val ratio = whiteRatio(bitmap)
                    Log.i(TAG, "$pageName 深色主体近白占比=${"%.3f".format(ratio)}")
                    assertTrue(
                        "$pageName：深色下主体区近白像素占比 ${"%.1f".format(ratio * 100)}% " +
                            "> ${"%.0f".format(WHITE_BLOCK_TOLERANCE * 100)}%——" +
                            "该页有玻璃控件/文字没拿到深色调色板",
                        ratio <= WHITE_BLOCK_TOLERANCE,
                    )
                } finally {
                    bitmap.recycle()
                }
            }
        } finally {
            setDarkMode(enabled = false)
        }
    }

    // ------------------------------------------------------------------ 页面遍历

    /** 依次打开主界面的四个 tab + 设置页，回调里传入可读的页面名。 */
    private fun forEachMainPage(block: (String) -> Unit) {
        val tabs = intArrayOf(
            R.string.title_main,
            R.string.title_explore,
            R.string.title_notifications,
            R.string.title_user,
        )
        tabs.forEach { resId ->
            tapDock(resId)
            SystemClock.sleep(PAGE_SETTLE_MS)
            block(str(resId))
        }
        // 设置（二级页）：我的 → 设置
        tapDock(R.string.title_user)
        SystemClock.sleep(PAGE_SETTLE_MS)
        val entry = device.findObject(By.text(str(R.string.my_info_settings)))
        if (entry == null) {
            Log.w(TAG, "找不到「${str(R.string.my_info_settings)}」入口，跳过设置页")
            return
        }
        entry.clickOrTapByBounds()
        SystemClock.sleep(1500)
        block(str(R.string.title_settings))
        device.pressBack()
        SystemClock.sleep(1000)
    }

    // ------------------------------------------------------------------ 深色开关

    /**
     * 打开/关闭深色：我的 → 主题选择 → 点可勾选的那个开关，并**回读 `checked` 确认生效**。
     * （今天吃过亏：盲点坐标，开关没动，截图全是浅色还以为是 bug。）
     */
    private fun setDarkMode(enabled: Boolean) {
        tapDock(R.string.title_user)
        SystemClock.sleep(PAGE_SETTLE_MS)
        val themeEntry = awaitObject("主题选择入口") {
            device.findObject(By.text(str(R.string.title_theme)))
        }
        themeEntry.clickOrTapByBounds()
        SystemClock.sleep(1500)

        val switchNode = awaitObject("深色开关") {
            device.findObjects(By.checkable(true)).firstOrNull()
        }
        if (switchNode.isChecked == enabled) {
            Log.i(TAG, "深色开关已是 $enabled，不需要切换")
        } else {
            switchNode.clickOrTapByBounds()
            SystemClock.sleep(2500)
            val after = device.findObjects(By.checkable(true)).firstOrNull()
            val actual = after?.isChecked
            Log.i(TAG, "深色开关切换后 checked=$actual（期望 $enabled）")
            assertTrue("深色开关没有切到 $enabled（实际 $actual）", actual == enabled)
        }
        device.pressBack()
        SystemClock.sleep(1500)
    }

    // ------------------------------------------------------------------ 工具

    /** 全屏截图。用 `uiAutomation.takeScreenshot()`（直接拿 Bitmap），不写文件。 */
    private fun screenshot(): Bitmap =
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            ?: throw AssertionError("截图失败：uiAutomation.takeScreenshot() 返回 null")

    /** 主体区「接近纯白」的像素占比：顶栏以下、dock 以上，且 RGB 全部 ≥ 250。 */
    private fun whiteRatio(bitmap: Bitmap): Float {
        val w = bitmap.width
        val top = statusBarPx + (120 * displayDensity).toInt()
        val bottom = (bitmap.height * 0.86f).toInt()
        if (bottom <= top) return 0f
        var white = 0
        var total = 0
        var y = top
        while (y < bottom) {
            var x = w / 10
            while (x < w - w / 10) {
                val c = bitmap.getPixel(x, y)
                if (red(c) >= 250 && green(c) >= 250 && blue(c) >= 250) white++
                total++
                x += WHITE_SAMPLE_STEP
            }
            y += WHITE_SAMPLE_STEP
        }
        return if (total == 0) 0f else white.toFloat() / total
    }

    private fun red(c: Int) = (c shr 16) and 0xFF
    private fun green(c: Int) = (c shr 8) and 0xFF
    private fun blue(c: Int) = c and 0xFF
    private fun hex(c: Int) = "#%06X".format(c and 0xFFFFFF)

    private fun relaunchApp() {
        device.executeShellCommand("am force-stop $PACKAGE")
        SystemClock.sleep(600)
        device.executeShellCommand(
            "am start -W -f 0x10008000 -n $PACKAGE/com.huanchengfly.tieba.post.MainActivityV2",
        )
        SystemClock.sleep(LAUNCH_SETTLE_MS)
    }

    private fun dockTabCount(): Int = DOCK_TABS.count { device.hasObject(By.desc(str(it))) }

    private fun awaitDockReady() {
        await("主界面 dock 就绪") { dockTabCount() >= 2 }
    }

    private fun tapDock(tabTitleRes: Int) {
        val label = str(tabTitleRes)
        val tab = device.findObject(By.desc(label))
        if (tab != null) {
            tab.clickOrTapByBounds()
            return
        }
        val index = DOCK_TABS.indexOf(tabTitleRes)
        if (index < 0) return
        val margin = 16 * displayDensity
        val usableWidth = device.displayWidth - 2 * margin
        val x = (margin + usableWidth * (2 * index + 1) / (2f * DOCK_TABS.size)).toInt()
        val y = (device.displayHeight * DOCK_CENTER_Y_FRACTION).toInt()
        device.click(x, y)
    }

    private fun awaitObject(what: String, timeoutMs: Long = 10000, block: () -> UiObject2?): UiObject2 {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            block()?.let { return it }
            SystemClock.sleep(200)
        }
        throw AssertionError("等待超时（${timeoutMs}ms）：$what")
    }

    private fun await(what: String, timeoutMs: Long = 10000, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(200)
        }
        throw AssertionError("等待超时（${timeoutMs}ms）：$what")
    }

    private fun UiObject2.clickOrTapByBounds() {
        val b = visibleBounds ?: return
        device.click(b.centerX(), b.centerY())
        SystemClock.sleep(400)
    }

    private companion object {
        const val TAG = "PixelRegression"
        const val PACKAGE = "com.tieba"
        const val LAUNCH_SETTLE_MS = 6000L
        const val PAGE_SETTLE_MS = 2500L

        /** 每通道允许的差值（状态栏 vs 内容区）。先宽后紧：3 能抓今天的白条，又不至于被渐变底误伤。 */
        const val CHANNEL_TOLERANCE = 12

        /** 深色下主体区允许的近白像素占比。 */
        const val WHITE_BLOCK_TOLERANCE = 0.20f

        /** 统计近白占比时的采样步长（px），控制耗时。 */
        const val WHITE_SAMPLE_STEP = 24

        const val DOCK_CENTER_Y_FRACTION = 0.93f

        val DOCK_TABS = intArrayOf(
            R.string.title_main,
            R.string.title_explore,
            R.string.title_notifications,
            R.string.title_user,
        )
    }
}
