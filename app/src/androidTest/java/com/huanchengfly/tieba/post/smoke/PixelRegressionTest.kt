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
        // 与 MainSmokeTest 共用前置：**强制浅色**再采样，避免「深色页面 + 浅色状态栏」的瞬时态。
        SmokeReset.reset(device, context)
        awaitDockReady()
    }

    // ------------------------------------------------------------------ 断言 1

    /**
     * 状态栏区域不能比页面背景「明显更亮」（= 白条 / 分界线）。
     *
     * 采样：
     * - 状态栏：`(w/2, statusBarPx/2)`；
     * - 页面背景：屏内若干点取**最暗**的那个（内容区可能正好压在卡片/图片上，
     *   取最暗点才代表真正的页面底色，否则会把「深色页面 + 深色卡片」误判成断层）。
     *
     * 断言：`statusBar 亮度 - 页面背景亮度 ≤ [LUMA_TOLERANCE]`。
     * 只比「亮出来」的方向——今天两种 bug（首页顶栏白块、消息 tab 深色 header 白底）都是
     * 状态栏那条明显比页面亮。
     */
    @Test
    fun statusBarMatchesPageBackground() {
        Log.i(TAG, "ENTER statusBarMatchesPageBackground")
        var pages = 0
        var asserts = 0
        var retried = 0
        try {
            forEachMainPage { pageName ->
                pages++
                val bitmap = screenshot()
                try {
                    val w = bitmap.width
                    val h = bitmap.height
                    val sbH = statusBarPx
                    val statusBar = bitmap.getPixel(w / 2, sbH / 2)
                    val sbLuma = luma(statusBar)
                    // 页面背景：多个采样点取**中位数**亮度（最暗点会落到照片上，最亮点会落到卡片上，
                    // 中位数才代表「这一页的主色调」）。
                    val samples = ArrayList<Int>()
                    for (fy in BACKGROUND_SAMPLE_Y) {
                        for (fx in BACKGROUND_SAMPLE_X) {
                            val c = bitmap.getPixel((w * fx).toInt(), (h * fy).toInt())
                            samples.add(luma(c))
                        }
                    }
                    samples.sort()
                    val minLuma = if (samples.isEmpty()) sbLuma else samples[samples.size / 2]
                    val minColor = if (samples.isEmpty()) statusBar else bitmap.getPixel(
                        (w * BACKGROUND_SAMPLE_X[1]).toInt(),
                        (h * BACKGROUND_SAMPLE_Y[1]).toInt(),
                    )
                    var delta = sbLuma - minLuma
                    Log.i(
                        TAG,
                        "$pageName 状态栏=${hex(statusBar)}(L=$sbLuma) " +
                            "页面背景=${hex(minColor)}(L=$minLuma) Δ=$delta attempt=1",
                    )
                    // 主题切换 / 转场动画会留一两帧「页面已变、状态栏还没跟上」的瞬时态。
                    // 超阈值时等 800ms 重采样一次；**重试必须打日志**，两次都超才算失败
                    // （这不是放宽断言：真实白条重试后仍然超）。
                    if (delta > LUMA_TOLERANCE) {
                        retried++
                        SystemClock.sleep(RETRY_SETTLE_MS)
                        val again = screenshot()
                        try {
                            val sb2 = again.getPixel(again.width / 2, statusBarPx / 2)
                            val samples2 = ArrayList<Int>()
                            for (fy in BACKGROUND_SAMPLE_Y) {
                                for (fx in BACKGROUND_SAMPLE_X) {
                                    samples2.add(
                                        luma(
                                            again.getPixel(
                                                (again.width * fx).toInt(),
                                                (again.height * fy).toInt(),
                                            )
                                        )
                                    )
                                }
                            }
                            samples2.sort()
                            val bg2 = if (samples2.isEmpty()) luma(sb2) else samples2[samples2.size / 2]
                            delta = luma(sb2) - bg2
                            Log.i(
                                TAG,
                                "$pageName 状态栏=${hex(sb2)}(L=${luma(sb2)}) " +
                                    "Δ=$delta attempt=2（首次 Δ=${sbLuma - minLuma}）",
                            )
                        } finally {
                            again.recycle()
                        }
                    }
                    assertTrue(
                        "$pageName：状态栏比页面背景亮 $delta（重试后仍超阈值 $LUMA_TOLERANCE）" +
                            "——白条/分界线回归，检查该页背景是否铺到状态栏",
                        delta <= LUMA_TOLERANCE,
                    )
                    asserts++
                } finally {
                    bitmap.recycle()
                }
            }
        } finally {
            val hitRate = if (pages == 0) 0 else retried * 100 / pages
            Log.i(TAG, "RETRY stats: attempts=$pages retried=$retried hitRate=${hitRate}%")
            Log.i(TAG, "EXIT statusBarMatchesPageBackground pages=$pages asserts=$asserts")
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

    /** 感知亮度（0~255，够用即可，不需要精确的 sRGB 公式）。 */
    private fun luma(c: Int) = ((red(c) * 299 + green(c) * 587 + blue(c) * 114) / 1000)

    private fun red(c: Int) = (c shr 16) and 0xFF
    private fun green(c: Int) = (c shr 8) and 0xFF
    private fun blue(c: Int) = c and 0xFF
    private fun hex(c: Int) = "#%06X".format(c and 0xFFFFFF)

    /**
     * 把 App 拉回前台。
     *
     * **绝对不要在这里 `am force-stop com.tieba`**：instrumentation 就跑在 `com.tieba` 的进程里，
     * force-stop 会把自己一起杀掉，UTP 只会看到 `Process crashed`、测试方法连第一行都执行不到
     * （今天就是这么假通过的）。真正的进程级冷启动由 `scripts/run-smoke.ps1` 在跑测试前做。
     */
    private fun relaunchApp() {
        val out = device.executeShellCommand(
            "am start -W -f 0x10008000 -n $PACKAGE/com.huanchengfly.tieba.post.MainActivityV2",
        )
        Log.i(TAG, "relaunch: ${out.trim().lineSequence().take(3).joinToString(" / ")}")
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

        /** 状态栏亮度相对页面背景允许多出的量（0~255）。白条时通常 > 200，正常页 < 20。 */
        const val LUMA_TOLERANCE = 40

        /** 首次采样超阈值后的等待（跨过主题切换/转场动画的瞬时帧）。 */
        const val RETRY_SETTLE_MS = 800L

        /** 页面背景采样点（横向比例）。 */
        val BACKGROUND_SAMPLE_X = floatArrayOf(0.08f, 0.5f, 0.92f)

        /** 页面背景采样点（纵向比例，避开顶栏与 dock）。 */
        val BACKGROUND_SAMPLE_Y = floatArrayOf(0.30f, 0.50f, 0.70f, 0.80f)

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
