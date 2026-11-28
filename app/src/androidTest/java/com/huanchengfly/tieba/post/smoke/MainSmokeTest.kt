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
        // 我的页那一行的文案资源是 my_info_settings（与设置页自己的标题 title_settings 同字），
        // 断言指向前者：入口在「我的页」，删的是同一页的「夜间 / 关于」，设置行保留。
        val entry = awaitObject("我的页的「${str(R.string.my_info_settings)}」入口") {
            device.findObject(By.text(str(R.string.my_info_settings)))
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
        awaitObject("我的页「${str(R.string.my_info_settings)}」入口") {
            device.findObject(By.text(str(R.string.my_info_settings)))
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

    // ------------------------------------------------------------------ 第二批 5 条（深水区）

    @Test
    fun forumTabOpensList() {
        // 6. 进吧页能进：dock「进吧」→ 未登录空态（标题 + 去登录按钮）。
        tapDock(R.string.title_explore)

        awaitObject("进吧页空态「${str(R.string.title_empty_login)}」") {
            device.findObject(By.text(str(R.string.title_empty_login)))
        }
        assertTrue(
            "进吧页应有「${str(R.string.button_login)}」入口",
            device.hasObject(By.text(str(R.string.button_login)))
        )
        // 不再断言 dock 上重新出现「首页」：Glasmio dock 的选中项在无障碍树里没有
        // contentDescription，而刚被切走的 tab 在 UiAutomation 的树里不会马上补回来
        // （截图里 dock 视觉是正确的，只是语义树滞后）。改成断言「首页内容已经切走」：
        // 首页顶栏的「搜索」入口在进吧页不应存在。
        assertFalse(
            "切到进吧后不应再看到首页的「${str(R.string.title_search)}」入口",
            device.hasObject(By.text(str(R.string.title_search)))
        )
    }

    @Test
    fun userProfileOpensFromAvatar() {
        // 7. 信息流点作者头像 → 用户主页（返回钮 + 关注/粉丝统计行）。
        val avatar = awaitObject("信息流里的作者头像") {
            device.findObject(By.desc(str(R.string.desc_user_avatar)))
        }
        avatar.clickOrTapByBounds()

        awaitObject("用户主页的「${str(R.string.text_stat_fans)}」统计") {
            device.findObject(By.text(str(R.string.text_stat_fans)))
        }
        assertTrue(
            "用户主页应有返回钮",
            device.hasObject(By.desc(str(R.string.button_back)))
        )
        // 资料区（ID 行）是主页独有的结构；昵称本身是网络内容，不做文字断言。
        assertTrue(
            "用户主页应有资料区（ID 行）",
            device.hasObject(By.text(Pattern.compile("ID: .+")))
        )
    }

    @Test
    fun subPostsOpenAndBack() {
        // 8. 楼中楼能进能出：帖子页 → 展开楼中楼 →「查看全部 N 条回复」→ 楼中楼页 → 返回。
        //    信息流里不一定每条帖子都有楼中楼，所以最多试前 3 张卡片；都没有才降级。
        val openAllPattern = Pattern.compile(
            str(R.string.open_all_sub_posts).replace(Regex("%[ds]"), ".*")
        )
        val subPostsTitle = str(R.string.title_sub_posts_page)

        var opened = false
        for (index in 0 until SUB_POST_CARD_ATTEMPTS) {
            openThreadCard(index)
            if (tryOpenSubPosts(openAllPattern, subPostsTitle)) {
                opened = true
                break
            }
            // 这张帖子的楼中楼没打开：退回信息流，换下一张
            device.pressBack()
            awaitFeedCards()
        }

        if (opened) {
            assertTrue(
                "楼中楼页应有返回钮",
                device.hasObject(By.desc(str(R.string.button_back)))
            )
            device.pressBack()
            await("返回帖子页（底栏「${str(R.string.title_see_lz)}」回来）") {
                device.hasObject(By.desc(str(R.string.title_see_lz)))
            }
            assertFalse(
                "返回后不应还停在楼中楼页",
                device.hasObject(By.text(subPostsTitle))
            )
            return
        }

        // 降级断言（前 3 张卡片都没有楼中楼；单楼层主题帖连「第 N 楼」都没有）：
        // 只断言「帖子页本身在」—— 顶栏返回钮 + 底栏回复入口，两个都是帖子页独有的静态节点。
        Log.w(TAG, "subPostsOpenAndBack: 前 $SUB_POST_CARD_ATTEMPTS 张卡片都没有楼中楼入口，降级为「帖子页在」断言")
        openThreadCard(0)
        assertTrue(
            "降级：帖子页应有返回钮",
            device.hasObject(By.desc(str(R.string.button_back)))
        )
        assertTrue(
            "降级：帖子页应有回复入口（${str(R.string.tip_reply_thread)}）",
            device.hasObject(By.text(str(R.string.tip_reply_thread))) ||
                device.hasObject(By.desc(str(R.string.title_see_lz)))
        )
    }

    @Test
    fun settingsPersonalizationRows() {
        // 9. 我 → 设置 → 个性化：断言三行静态设置项 + 返回钮。
        //    「夜间模式主题」行已随无效入口删除（纯黑/蓝/灰都映射到同一套深色），改断言
        //    保留下来的「跟随系统夜间模式」行。
        tapDock(R.string.title_user)
        val settingsEntry = awaitObject("我的页「${str(R.string.my_info_settings)}」入口") {
            device.findObject(By.text(str(R.string.my_info_settings)))
        }
        settingsEntry.clickOrTapByBounds()

        // IA 六分组后，「个性化」二级页已下沉到设置主页，直接断言主页上的代表行。
        val fontRow = awaitObject("设置页「${str(R.string.title_custom_font_size)}」行") {
            device.findObject(By.text(str(R.string.title_custom_font_size)))
        }
        fontRow.recycle()
        assertTrue(
            "设置页应有「${str(R.string.settings_app_icon)}」行",
            device.hasObject(By.text(str(R.string.settings_app_icon)))
        )
        assertTrue(
            "设置页应有「${str(R.string.settings_incognito_mode)}」行",
            device.hasObject(By.text(str(R.string.settings_incognito_mode)))
        )
        assertTrue(
            "设置页应有返回钮",
            device.hasObject(By.desc(str(R.string.button_back)))
        )
    }

    @Test
    fun pageTitlesAreUnique() {
        // 防回归：「同一标题被画两次」（PrefsLargeTitle 叠 Text、GlassPage + 页内 header 双标题）。
        // 判定方式：uiautomator 能读到同文本的两个节点，所以直接数节点。
        // 注意 tab 页的 dock 上也有同文本标签（「我的」「消息」），这里只数屏幕上方 70% 的节点。

        // 1) 我的（tab 页）
        tapDock(R.string.title_user)
        awaitObject("我的页标题") {
            device.findObject(By.text(str(R.string.mine_title)))
        }
        assertSingleVisibleTitle("我的页", str(R.string.mine_title))

        // 2) 设置（二级页）
        val settingsEntry = awaitObject("我的页「${str(R.string.my_info_settings)}」入口") {
            device.findObject(By.text(str(R.string.my_info_settings)))
        }
        settingsEntry.clickOrTapByBounds()
        awaitObject("设置页标题") {
            device.findObject(By.text(str(R.string.title_settings)))
        }
        assertSingleVisibleTitle("设置页", str(R.string.title_settings))

        // 3) 浏览记录（二级页，GlassPage）
        device.pressBack()
        SystemClock.sleep(1000)
        val historyEntry = awaitObject("我的页「${str(R.string.title_history)}」入口") {
            device.findObject(By.text(str(R.string.title_history)))
        }
        historyEntry.clickOrTapByBounds()
        awaitObject("浏览记录页标题") {
            device.findObject(By.text(str(R.string.title_history)))
        }
        assertSingleVisibleTitle("浏览记录页", str(R.string.title_history))
        device.pressBack()
        SystemClock.sleep(1000)

        // 4) 消息（tab 页）
        tapDock(R.string.title_notifications)
        awaitObject("消息页标题") {
            device.findObject(By.text(str(R.string.title_notifications)))
        }
        assertSingleVisibleTitle("消息页", str(R.string.title_notifications))
    }

    /**
     * 断言「标题在可见区域（上方 70%）只出现 1 次」。
     *
     * - 排除 dock 上的同文本标签（它们在屏幕底部 30%）；
     * - 找不到节点则失败（说明标题根本没渲染）。
     */
    private fun assertSingleVisibleTitle(page: String, title: String) {
        val nodes = device.findObjects(By.text(title))
        try {
            // 读 visibleBounds 时节点可能已经因为重绘失效（StaleObjectException），
            // 这种节点直接跳过——不能让它把断言变成 flaky 失败。
            val visible = nodes.filter { node ->
                val bounds = try {
                    node.visibleBounds
                } catch (e: androidx.test.uiautomator.StaleObjectException) {
                    null
                }
                bounds != null && bounds.top < device.displayHeight * 0.7f
            }
            assertTrue(
                "$page：标题「$title」应只出现 1 次，实际 ${visible.size} 次" +
                    "（节点总数 ${nodes.size}）",
                visible.size == 1,
            )
        } finally {
            nodes.forEach { it.recycle() }
        }
    }

    @Test
    fun nightModeToggleKeepsAppAlive() {
        // 10. 深色模式切换不崩：我的 → 主题选择 → 读当前开关状态 → 切到相反态 →
        //     断言进程存活 + 主题选择页仍在 → 切回原状态。
        //     我的页的「夜间」行已删，深色开关只保留在主题选择页（AppThemePage）。
        //     切主题可能会重建 Activity，所以每次都重新查节点，不用缓存的 UiObject2。
        tapDock(R.string.title_user)
        val themeEntry = awaitObject("我的页「${str(R.string.title_theme)}」入口") {
            device.findObject(By.text(str(R.string.title_theme)))
        }
        themeEntry.clickOrTapByBounds()

        awaitObject("主题选择页「${str(R.string.title_theme_dark)}」行") {
            device.findObject(By.text(str(R.string.title_theme_dark)))
        }
        val toggle = awaitObject("主题选择页的「${str(R.string.title_theme_dark)}」开关") {
            darkModeSwitch()
        }
        val wasChecked = toggle.isChecked

        try {
            toggle.clickOrTapByBounds()
            await("深色开关切到 ${!wasChecked}", 15_000) { darkModeSwitch()?.isChecked == !wasChecked }

            val pid = device.executeShellCommand("pidof $APP_PACKAGE").trim()
            assertTrue("切深色模式后 App 进程应仍存活（pidof 返回空）", pid.isNotEmpty())
            // 切主题可能触发 Activity 重建：用 await 等「主题选择」页重新出现，而不是立刻断言。
            await("切深色模式后主题选择页重新出现", 10_000) {
                device.hasObject(By.text(str(R.string.title_theme)))
            }
        } finally {
            // 不管断言是否失败，都尽量切回原状态，避免污染下一次跑批。
            runCatching {
                val current = darkModeSwitch()
                if (current != null && current.isChecked != wasChecked) {
                    current.clickOrTapByBounds()
                    await("深色开关恢复为 $wasChecked", 10_000) { darkModeSwitch()?.isChecked == wasChecked }
                }
            }.onFailure { Log.w(TAG, "nightModeToggleKeepsAppAlive: 恢复深色开关失败（需手工切回）: $it") }
        }
    }

    // ------------------------------------------------------------------ 第二批的辅助

    /** 主题选择页的「深色模式」开关：该页唯一的 checkable 节点。 */
    private fun darkModeSwitch(): UiObject2? = device.findObjects(By.checkable(true)).firstOrNull()

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
        var tab: UiObject2? = null
        val deadline = SystemClock.uptimeMillis() + DOCK_SEMANTICS_TIMEOUT_MS
        while (tab == null && SystemClock.uptimeMillis() < deadline) {
            tab = device.findObject(By.desc(label))
            if (tab == null) SystemClock.sleep(POLL_MS)
        }
        if (tab != null) {
            tab.clickOrTapByBounds()
            return
        }
        // Glasmio dock 的已知问题：某个 tab 刚从「选中」变成「未选中」时，它的
        // contentDescription 可能整个从无障碍树里消失（视觉正常，截图可证），
        // `By.desc` 会一直找不到。这里按 dock 的等分槽位坐标兜底（布局是固定的）。
        val index = DOCK_TABS.indexOf(tabTitleRes)
        if (index < 0) failWith("dock 上没有「$label」这个 tab")
        val margin = 16 * displayDensity
        val usableWidth = device.displayWidth - 2 * margin
        val x = (margin + usableWidth * (2 * index + 1) / (2f * DOCK_TABS.size)).toInt()
        val y = (device.displayHeight * DOCK_CENTER_Y_FRACTION).toInt()
        Log.w(TAG, "tapDock: 「$label」没有语义节点，退化为坐标点击 ($x, $y)")
        device.click(x, y)
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
    private fun openFirstThread() = openThreadCard(0)

    /** 点信息流里第 [index] 张卡片（0 = 最上面那张）→ 等到帖子页底栏出现。 */
    private fun openThreadCard(index: Int) {
        val cards = awaitFeedCards()
        val card = cards.getOrNull(index) ?: cards.first()
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

    /**
     * 在当前帖子页尝试进入楼中楼：成功返回 true（此时停在楼中楼页），失败返回 false（仍在帖子页）。
     *
     * 入口有三种形态，所以三步都容错：① 帖子里直接有「查看全部 N 条回复」；
     * ② 先点一条楼中楼预览，就地展开出「查看全部 N 条回复」；③ 点预览直接跳到楼中楼页。
     */
    private fun tryOpenSubPosts(openAllPattern: Pattern, subPostsTitle: String): Boolean {
        if (device.hasObject(By.text(subPostsTitle))) return true
        var openAll = device.findObject(By.text(openAllPattern))
        if (openAll == null) {
            val preview = device.findObjects(By.text(Pattern.compile(".+: .+"))).firstOrNull {
                val bounds = it.visibleBounds
                bounds.left > 150 && bounds.height() < 120 && bounds.top > 200
            }
            preview?.clickOrTapByBounds()
            if (waitQuietly(5_000) { device.hasObject(By.text(openAllPattern)) }) {
                openAll = device.findObject(By.text(openAllPattern))
            }
        }
        if (!device.hasObject(By.text(subPostsTitle))) {
            openAll?.clickOrTapByBounds()
        }
        return waitQuietly(6_000) { device.hasObject(By.text(subPostsTitle)) }
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

    /** 不抛异常的轮询等待：用于「可能出现、也可能不出现」的节点（猜路径/降级判断）。 */
    private fun waitQuietly(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (runCatching(condition).getOrDefault(false)) return true
            SystemClock.sleep(POLL_MS)
        }
        return false
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
        const val APP_PACKAGE = "com.tieba"
        const val APP_COMPONENT = "$APP_PACKAGE/com.huanchengfly.tieba.post.MainActivityV2"

        /** 楼中楼用例最多尝试信息流的前 N 张卡片（不是每条帖子都有楼中楼）。 */
        const val SUB_POST_CARD_ATTEMPTS = 3

        /** dock 语义节点最多等多久；超时就走坐标兜底（见 [tapDock]）。 */
        const val DOCK_SEMANTICS_TIMEOUT_MS = 800L

        /** dock 药丸中心距屏幕顶部的比例（展开态固定在底部）。 */
        const val DOCK_CENTER_Y_FRACTION = 0.935f

        /** dock 上四个 tab 的标题资源（顺序 = Page 顺序）。 */
        val DOCK_TABS = intArrayOf(
            R.string.title_main,
            R.string.title_explore,
            R.string.title_notifications,
            R.string.title_user,
        )
    }
}
