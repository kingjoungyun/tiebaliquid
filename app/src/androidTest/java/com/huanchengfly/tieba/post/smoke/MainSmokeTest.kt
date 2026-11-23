package com.huanchengfly.tieba.post.smoke

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import com.huanchengfly.tieba.post.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

/**
 * 主干冒烟测试（UiAutomator 版本）。
 *
 * 为什么是 UiAutomator 而不是 Compose UI Test：[MainSmokeTest] 只操作「已经跑起来的真 App」，
 * 不注入、不替换 Application、不需要 `@HiltAndroidTest` / 自定义 runner（Hilt 图保持生产态）；
 * 也不依赖 Compose 的 idling resource —— 本工程有液态玻璃 backdrop、Glasmio 镜头弹簧动画、
 * 列表 Lottie 等，`waitForIdle` 很容易被判成"永远不空闲"，而 UiAutomator 的
 * `device.wait(Until...)` 是轮询式等待，动画再多也不会卡住。详见 SMOKE.md。
 *
 * 设计约定：
 * - 每条测试都从 [freshLaunch] 开始：清空任务栈后重建 MainActivityV2，互不依赖执行顺序；
 * - 只用「结构」做断言（可点击卡片数量、页面级 contentDescription），不依赖网络返回的具体文字；
 * - 所有等待 10s 起步，失败时把截图 + 语义树 + 当前可见文本写进 App 外部目录，并由
 *   `scripts/run-smoke.ps1` 拉回本地。
 *
 * 注意：本工程「首页」= dock 第 1 个 tab（`R.string.title_main`，ExplorePage 的个性化信息流），
 * 「进吧」= dock 第 2 个 tab（`R.string.title_explore`）。
 */
@RunWith(AndroidJUnit4::class)
class MainSmokeTest {

    @get:Rule
    val testName = TestName()

    private lateinit var device: UiDevice

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun str(resId: Int): String = context.getString(resId)

    /** 屏幕密度（dp → px）。UiDevice 2.3.0 没有 displayDensity，只能从 Context 的 DisplayMetrics 拿。 */
    private val displayDensity: Float
        get() = context.resources.displayMetrics.density

    @Before
    fun freshLaunch() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // 模拟器可能休眠 / 锁屏：先把屏幕唤醒并解掉 keyguard，否则点击会落在锁屏上。
        runCatching { device.wakeUp() }
        runCatching { device.executeShellCommand("wm dismiss-keyguard") }
        relaunchApp()
        awaitDockReady()
    }

    // ------------------------------------------------------------------ 5 条冒烟

    @Test
    fun coldStartShowsFeed() {
        // 1. 冷启动 → 首页有内容：dock 就绪 + 信息流至少渲染出 1 个可点击的帖子卡片。
        val cards = awaitFeedCards()
        assertTrue(
            "首页 dock 应至少有「进吧」「消息」「我」中的 2 个 tab",
            dockTabCount() >= 2
        )
        assertTrue("首页信息流应至少有 1 个可点击的帖子卡片，实际 ${cards.size}", cards.isNotEmpty())
        Log.i(TAG, "coldStartShowsFeed: ${cards.size} card(s), first=${cards.first().visibleBounds}")
    }

    @Test
    fun tapPostOpensThread() {
        // 2. 点帖子卡片 → 帖子页打开：底栏（只看楼主/正序）+ 顶栏返回钮。
        openFirstThread()
        assertTrue(
            "帖子页顶栏应出现「${str(R.string.button_back)}」",
            device.hasObject(By.desc(str(R.string.button_back)))
        )
    }

    @Test
    fun settingsReachableFromProfile() {
        // 3. 设置可达：「我」→「设置」→ 账号/安全分组都在。
        tapDock(R.string.title_user)
        val entry = awaitObject("我的页的「${str(R.string.title_settings)}」入口") {
            device.findObject(By.text(str(R.string.title_settings)))
        }
        entry.clickOrTapByBounds()

        awaitObject("设置页「${str(R.string.title_account_manage)}」") {
            device.findObject(By.text(str(R.string.title_account_manage)))
        }
        assertTrue(
            "设置页应出现「${str(R.string.title_safe_mode)}」",
            device.hasObject(By.text(str(R.string.title_safe_mode)))
        )
        assertTrue(
            "设置页应出现「${str(R.string.settings_group_appearance)}」分组",
            device.hasObject(By.text(str(R.string.settings_group_appearance)))
        )
        assertTrue(
            "设置页顶栏应有返回钮",
            device.hasObject(By.desc(str(R.string.button_back)))
        )
    }

    @Test
    fun dockSwitchesAllFourTabs() {
        // 4. dock 四 tab 依次切换：首页 → 进吧 → 消息 → 我 → 首页，每次都等到该页的标志节点。
        tapDock(R.string.title_explore)
        awaitObject("进吧页空态「${str(R.string.title_empty_login)}」") {
            device.findObject(By.text(str(R.string.title_empty_login)))
        }

        tapDock(R.string.title_notifications)
        awaitObject("消息页「${str(R.string.title_reply_me)}」tab") {
            device.findObject(By.text(str(R.string.title_reply_me)))
        }

        tapDock(R.string.title_user)
        awaitObject("我的页「${str(R.string.title_settings)}」入口") {
            device.findObject(By.text(str(R.string.title_settings)))
        }

        tapDock(R.string.title_main)
        // 回到首页：信息流卡片回来，且其它 tab 的标题已经不在树上（确认真的切走了）。
        awaitFeedCards()
        assertFalse(
            "切回首页后不应再看到「${str(R.string.mine_title)}」页标题",
            device.hasObject(By.text(str(R.string.mine_title)))
        )
        assertFalse(
            "切回首页后不应再看到「${str(R.string.title_reply_me)}」",
            device.hasObject(By.text(str(R.string.title_reply_me)))
        )
    }

    @Test
    fun backFromThreadReturnsToFeed() {
        // 5. 返回能退栈：进帖子 → 系统返回 → 回到信息流。
        openFirstThread()
        device.pressBack()

        val cards = awaitFeedCards()
        assertTrue("返回后应回到信息流（卡片数 ${cards.size}）", cards.isNotEmpty())
        assertFalse(
            "返回后不应还停在帖子页（底栏「${str(R.string.title_see_lz)}」应消失）",
            device.hasObject(By.desc(str(R.string.title_see_lz)))
        )
        assertTrue(
            "返回后首页 dock 应可见",
            device.hasObject(By.desc(str(R.string.title_explore)))
        )
    }

    // ------------------------------------------------------------------ 启动 / 导航

    /**
     * 清掉任务栈后重新拉起 [APP_COMPONENT]。
     *
     * 测试进程就是被测 App 的进程（instrumentation 的 targetPackage = com.tieba），所以测试里
     * 不能 force-stop 自己。`FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK` 会销毁旧 Activity
     * 并新建 MainActivityV2：新的 Compose 树、导航回栈归零，效果等同一次干净启动。
     * 真正的「进程级冷启动」由 `scripts/run-smoke.ps1` 在跑测试前 force-stop 保证。
     */
    private fun relaunchApp() {
        val out = device.executeShellCommand("am start -W -f 0x10008000 -n $APP_COMPONENT")
        Log.i(TAG, "relaunch: ${out.trim().lineSequence().take(3).joinToString(" / ")}")
        SystemClock.sleep(400)
    }

    /** dock 的「当前选中 tab」没有语义节点（Glasmio 选中态只画图标），所以用其余 tab 判断就绪。 */
    private fun dockTabCount(): Int = DOCK_TABS.count { device.hasObject(By.desc(str(it))) }

    private fun awaitDockReady() {
        await("主界面的 dock 就绪（三个未选中 tab 至少可见 2 个）") { dockTabCount() >= 2 }
    }

    private fun tapDock(tabTitleRes: Int) {
        val label = str(tabTitleRes)
        val tab = awaitObject("dock 上的「$label」tab") { device.findObject(By.desc(label)) }
        tab.clickOrTapByBounds()
    }

    // ------------------------------------------------------------------ 信息流结构

    /**
     * 信息流里的「帖子卡片」= 满足这些条件的可点击节点：
     * 宽度 ≥ 屏宽 60%、高度 ≥ 200px、完全落在顶栏以下 / dock 以上的内容区。
     * 这样不依赖帖子文字/图片/数量，只依赖「有一块大的可点击卡片」这个结构。
     */
    private fun findFeedCards(): List<UiObject2> {
        val screenWidth = device.displayWidth
        val screenHeight = device.displayHeight
        val topLimit = (screenHeight * 0.08f).toInt()
        val bottomLimit = screenHeight - (screenHeight * 0.10f).toInt()
        return device.findObjects(By.clickable(true)).filter { obj ->
            val bounds = runCatching { obj.visibleBounds }.getOrNull() ?: return@filter false
            bounds.width() >= screenWidth * 0.6f &&
                bounds.height() >= 200 &&
                bounds.top >= topLimit &&
                bounds.bottom <= bottomLimit
        }
    }

    private fun awaitFeedCards(): List<UiObject2> {
        var cards: List<UiObject2> = emptyList()
        await("信息流出现至少 1 个可点击的帖子卡片") {
            cards = findFeedCards()
            cards.isNotEmpty()
        }
        return cards
    }

    /** 点第一个帖子卡片 → 等到帖子页底栏出现。 */
    private fun openFirstThread() {
        val card = awaitFeedCards().first()
        val bounds = card.visibleBounds
        // 点卡片顶部「作者名」那一行：图片/头像/分享/评论都是嵌套的可点击区域，
        // 点它们会进看图页 / 个人页；点卡片本体才会进帖子页。
        val authorRowY = bounds.top + minOf((24 * displayDensity).toInt(), bounds.height() / 5)
        device.click(bounds.centerX(), authorRowY)
        await("帖子页打开（底栏出现「${str(R.string.title_see_lz)}」或「${str(R.string.title_asc)}」）") {
            device.hasObject(By.desc(str(R.string.title_see_lz))) ||
                device.hasObject(By.desc(str(R.string.title_asc)))
        }
    }

    // ------------------------------------------------------------------ 通用等待 / 失败现场

    /** 轮询等待；超时就落现场（截图 + 语义树 + 可见文本）并让用例失败。 */
    private fun await(what: String, timeoutMs: Long = TIMEOUT_MS, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var lastError: Throwable? = null
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                if (condition()) return
            } catch (t: Throwable) {
                // 节点在等待期间被回收（StaleObjectException）属于正常抖动，继续轮询。
                lastError = t
            }
            SystemClock.sleep(POLL_MS)
        }
        failWith(
            "等待超时（${timeoutMs}ms）：$what" +
                (lastError?.let { "\n  最近一次异常: $it" } ?: "")
        )
    }

    private fun awaitObject(
        what: String,
        timeoutMs: Long = TIMEOUT_MS,
        find: () -> UiObject2?,
    ): UiObject2 {
        var found: UiObject2? = null
        await(what, timeoutMs) {
            found = find()
            found != null
        }
        return found ?: failWith(cause = "等待超时：$what")
    }

    /**
     * 点节点中心；UiObject2.click() 是 void（失败抛异常），所以用 try/catch 兜底，
     * 兜底路径用节点 bounds 的中心做坐标点击。
     */
    private fun UiObject2.clickOrTapByBounds() {
        try {
            click()
            return
        } catch (t: Throwable) {
            Log.w(TAG, "UiObject2.click() 失败，改用坐标点击: $t")
        }
        val bounds = visibleBounds
        device.click(bounds.centerX(), bounds.centerY())
    }

    /**
     * 失败现场：截图 + 完整语义树 + 当前可见文字，写到 App 外部文件目录
     * （`/sdcard/Android/data/com.tieba/files/smoke-failures/`），run-smoke.ps1 会拉回 `shots/smoke/`。
     */
    private fun failWith(cause: String): Nothing {
        val dir = File(context.getExternalFilesDir(null), "smoke-failures").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
        val base = "${testName.methodName}-$stamp"
        val screenshot = File(dir, "$base.png")
        val hierarchy = File(dir, "$base.xml")
        runCatching { device.takeScreenshot(screenshot) }
        runCatching { device.dumpWindowHierarchy(hierarchy) }
        val summary = buildString {
            append(cause)
            append("\n  screenshot: ").append(screenshot.absolutePath)
            append("\n  hierarchy : ").append(hierarchy.absolutePath)
            append("\n  visible   : ").append(visibleSummary().joinToString(" | "))
        }
        Log.e(TAG, summary)
        throw AssertionError(summary)
    }

    private fun visibleSummary(): List<String> {
        val texts = LinkedHashSet<String>()
        runCatching {
            device.findObjects(By.text(Pattern.compile(".+"))).forEach { obj ->
                runCatching { obj.text }.getOrNull()?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { texts.add(it.take(48)) }
            }
        }
        runCatching {
            device.findObjects(By.desc(Pattern.compile(".+"))).forEach { obj ->
                runCatching { obj.contentDescription }.getOrNull()?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { texts.add("[desc]$it") }
            }
        }
        return texts.take(40).toList()
    }

    private companion object {
        const val TAG = "Smoke"
        const val TIMEOUT_MS = 10_000L
        const val POLL_MS = 200L
        const val APP_COMPONENT = "com.tieba/com.huanchengfly.tieba.post.MainActivityV2"

        /** dock 上四个 tab 的标题资源（顺序 = Page 顺序）。 */
        val DOCK_TABS = intArrayOf(
            R.string.title_main,
            R.string.title_explore,
            R.string.title_notifications,
            R.string.title_user,
        )
    }
}
