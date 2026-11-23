package com.huanchengfly.tieba.post.debug

import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumInfoChip
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadAgreeBtn
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadReplyBtn
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadShareBtn
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.glass.LocalGlassBackdrop
import io.github.neoruaa.glasmio.glass.rememberCrossWindowBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.emptyBackdrop
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

/**
 * 技术验证（spike）：信息流帖子卡 + 跨窗口菜单采背景。**只读预览，不接业务数据。**
 *
 * 验证 A（帖子卡）：10 条静态假数据放进 `LazyColumn`，每条用 [GlasmioCard] 做外壳
 * （26dp 连续圆角、`elevatedBackground` 实色），内部按 iOS 信息流密度排版：
 * 36dp 头像 + 名字/时间、17sp 标题、15sp 正文最多 3 行、圆角配图、吧名 chip +
 * 三个操作按钮（`ThreadShareBtn` / `ThreadReplyBtn` / `ThreadAgreeBtn`，直接复用生产组件，
 * 用来判断既有按钮能否塞进 Glasmio 卡片而不改）。
 *
 * 验证 B（跨窗口菜单）：页面由 `GlasmioBackdropHost` 托管，`sampledContent` 是信息流
 * （录制进 graphics layer），`overlayContent` 是页头 + 右下角玻璃「更多」按钮 + [GlasmioMenu]。
 * 菜单本身是 `Popup`（独立 Window），`backdrop` 默认取 `LocalGlassBackdrop`（即主窗口录制层），
 * Glasmio 内部用 `rememberSettledBackdrop` → [rememberCrossWindowBackdrop] 把采样坐标映射到
 * 菜单所在的 Window，于是菜单能折射/模糊主窗口的背景。
 *
 * 两个可切换的传参方式，用来回答「需要哪些前置条件」：
 * - `menu_mode=0`（默认）：把 host 的 LayerBackdrop 直接交给菜单，跨窗口包装交给 Glasmio；
 * - `menu_mode=1`：调用方自己先 `rememberCrossWindowBackdrop` 再传进去（显式路径）；
 * - `menu_mode=2`：对照组，传 `emptyBackdrop()`（不采任何背景）。与 0/1 在同一位置对比，
 *   如果 2 是平的白/灰、0/1 带上了背景色和模糊，就证明「采到了」，而不是材质本身的底色。
 *
 * 启动：
 * ```
 * # 浅色信息流（验收图 1）：
 * adb shell am start -n com.tieba/...GlassFeedPreviewActivity
 * # 深色纯黑（验收图 3）：
 * adb shell am start -n com.tieba/...GlassFeedPreviewActivity --ez dark true
 * # 菜单打开、且先把配图滚到菜单下面（验收图 4；也可手动点右下角玻璃按钮）：
 * adb shell am start -n com.tieba/...GlassFeedPreviewActivity --ez menu true --ei menu_mode 0 --ei scroll_dp 312
 * # 性能对照：--ei card_mode 0|1|2（GlasmioCard / 普通 clip / 无 clip 背景）
 * ```
 */
class GlassFeedPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 与 GlassSettingsPreviewActivity 同一条链路：写生产 DataStore，截出来的深色是 App 的真深色。
        if (intent.hasExtra(EXTRA_DARK)) {
            val wantDark = intent.getBooleanExtra(EXTRA_DARK, false)
            if (wantDark != ThemeUtil.isNightMode()) {
                ThemeUtil.switchNightMode()
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = AndroidColor.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = AndroidColor.TRANSPARENT

        val autoOpenMenu = intent.getBooleanExtra(EXTRA_MENU, false)
        val menuMode = intent.getIntExtra(EXTRA_MENU_MODE, 0)
        val scrollDp = intent.getIntExtra(EXTRA_SCROLL_DP, 0)
        // 性能对照（同机、同内容、同滚动，比较 gfxinfo）：
        //   0 = GlasmioCard（clip(ContinuousRoundedRectangle) → graphicsLayer(clip=true)）；
        //   1 = 普通 Box + clip(RoundedCornerShape)（同样有 layer，只是形状不同）；
        //   2 = 只画圆角背景、不 clip（无 graphicsLayer，理论最省路径）。
        val cardMode = intent.getIntExtra(EXTRA_CARD_MODE, 0)

        setContent {
            TiebaLiteTheme {
                FeedSpikePage(
                    window = window,
                    autoOpenMenu = autoOpenMenu,
                    menuMode = menuMode,
                    initialScrollDp = scrollDp,
                    cardMode = cardMode,
                    onBack = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_DARK = "dark"
        const val EXTRA_MENU = "menu"
        const val EXTRA_MENU_MODE = "menu_mode"
        const val EXTRA_SCROLL_DP = "scroll_dp"
        const val EXTRA_CARD_MODE = "card_mode"
    }
}

@Composable
private fun FeedSpikePage(
    window: Window,
    autoOpenMenu: Boolean,
    menuMode: Int,
    initialScrollDp: Int,
    cardMode: Int,
    onBack: () -> Unit,
) {
    val isDark = AppTokens.isNightMode
    SideEffect {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    LiquidGlassTheme(accent = AppTokens.primary) {
        val colors = GlasmioTheme.colors
        val context = LocalContext.current
        // 只解码一次：10 张卡共用同一张本地图（1080²），避免把「每卡各解码一张」算进滚动性能。
        val photo: ImageBitmap = remember {
            BitmapFactory.decodeResource(context.resources, R.drawable.user_header).asImageBitmap()
        }
        val posts = remember { spikePosts() }
        var menuExpanded by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()
        val density = LocalDensity.current
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        // 截图/复现钩子：先滚到指定位置（把配图送到菜单下面），再开菜单。
        LaunchedEffect(Unit) {
            if (initialScrollDp > 0) {
                listState.scrollBy(with(density) { initialScrollDp.dp.toPx() })
            }
            if (autoOpenMenu) {
                withFrameNanos { }
                menuExpanded = true
            }
        }

        GlasmioBackdropHost(
            modifier = Modifier.fillMaxSize(),
            // 被录进 graphics layer 的内容：整页信息流。玻璃浮层绝不能放进来（会自己采样自己）。
            sampledContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.groupedBackground),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = statusBarTop + 10.dp,
                            bottom = 120.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item(key = "title") {
                            // 大标题留在列表里（随内容滚走），左上角只留浮动的玻璃返回钮：
                            // 这样滚动后标题不会压在配图上、也不会和按钮重叠。
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .padding(start = 48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "信息流",
                                        fontSize = 28.sp,
                                        lineHeight = 34.sp,
                                        fontWeight = FontWeight(700),
                                        color = colors.content,
                                    )
                                    Text(
                                        text = if (isDark) "spike · 深色纯黑" else "spike · 浅色 · GlasmioCard",
                                        fontSize = 12.sp,
                                        color = colors.secondaryContent,
                                    )
                                }
                            }
                        }
                        items(posts, key = { it.id }) { post ->
                            SpikePostCard(post = post, photo = photo, cardMode = cardMode)
                        }
                        item(key = "footnote") {
                            Text(
                                text = "spike · 静态假数据 · GlasmioCard 26dp 实色壳",
                                fontSize = 12.sp,
                                color = colors.tertiaryContent,
                                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
                            )
                        }
                    }
                }
            },
            // 玻璃层：左上角返回钮 + 右下角「更多」按钮 + 菜单。全部画在录制层之上。
            overlayContent = {
                Box(modifier = Modifier.fillMaxSize()) {
                    GlasmioIconButton(
                        onClick = onBack,
                        size = 40.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(start = 16.dp, top = 10.dp),
                    ) {
                        SfIcon(
                            symbol = SfSymbol.ChevronBack,
                            contentDescription = "返回",
                            size = 18.dp,
                            mirrored = true,
                            tint = colors.content,
                        )
                    }

                    // 菜单的采样源：host 在主窗口录制的 LayerBackdrop。
                    val hostBackdrop = LocalGlassBackdrop.current
                    // 显式跨窗口包装：先按屏幕坐标把「主窗口图层」映射到 Popup 窗口。
                    // GlasmioMenu 内部还会再包一次 rememberSettledBackdrop，非 LayerBackdrop
                    // 会原样透传，所以两种传法都不会重复变换。
                    val crossWindowBackdrop = rememberCrossWindowBackdrop(hostBackdrop)

                    GlasmioMenu(
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it },
                        itemCount = 4,
                        // 关闭「从锚点形变」，把这次验证收敛到「背景采样」一件事上。
                        morphFromAnchor = false,
                        backdrop = when (menuMode) {
                            1 -> crossWindowBackdrop
                            2 -> emptyBackdrop()
                            else -> hostBackdrop
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(16.dp),
                        anchor = { onClick ->
                            GlasmioIconButton(onClick = onClick) {
                                SfIcon(
                                    systemName = "ellipsis",
                                    contentDescription = "更多操作",
                                    size = 20.dp,
                                    tint = colors.content,
                                )
                            }
                        },
                    ) { backdrop, close ->
                        GlasmioMenuItem(
                            title = "收藏",
                            onClick = { close() },
                            systemName = "bookmark",
                            backdrop = backdrop,
                        )
                        GlasmioMenuItem(
                            title = "复制链接",
                            onClick = { close() },
                            systemName = "doc.on.doc",
                            backdrop = backdrop,
                        )
                        GlasmioMenuItem(
                            title = "分享",
                            onClick = { close() },
                            systemName = "square.and.arrow.up",
                            backdrop = backdrop,
                        )
                        GlasmioMenuItem(
                            title = "不感兴趣",
                            onClick = { close() },
                            systemName = "eye.slash",
                            backdrop = backdrop,
                        )
                    }
                }
            },
        )
    }
}

/** 一条假帖子：作者、时间、标题、正文、吧名、三个数字、是否有图。 */
private data class SpikePost(
    val id: Int,
    val author: String,
    val time: String,
    val title: String,
    val body: String,
    val forum: String,
    val reply: String,
    val agree: String,
    val share: String,
    val hasImage: Boolean,
)

private fun spikePosts(): List<SpikePost> = listOf(
    SpikePost(
        id = 1,
        author = "青柠不酸",
        time = "3 分钟前",
        title = "折腾了一下午，终于把阳台上的龟背竹救活了",
        body = "之前一直烂根，换了疏松的土、减少浇水频率，新叶子这两天总算抽出来了。配土是泥炭 5 + 珍珠岩 3 + 松树皮 2，盆底垫了三厘米陶粒。",
        forum = "植物",
        reply = "128",
        agree = "1.2万",
        share = "36",
        hasImage = true,
    ),
    SpikePost(
        id = 2,
        author = "晚风过境",
        time = "12 分钟前",
        title = "有没有人跟我一样，一打开贴吧就先看吧务后台",
        body = "每天醒来第一件事不是看消息，是点开后台看看有没有被删帖、有没有人申诉。这种习惯是不是有点病态了……",
        forum = "吧务",
        reply = "64",
        agree = "2381",
        share = "8",
        hasImage = false,
    ),
    SpikePost(
        id = 3,
        author = "老张的相机",
        time = "27 分钟前",
        title = "【扫街】雨后的老城区，随手拍了二十张",
        body = "机身 A7C2 + 35mm，后期只做了简单曲线和去雾。雨天的高光特别好看，湿漉漉的石板路反光很出片。",
        forum = "摄影",
        reply = "302",
        agree = "6842",
        share = "91",
        hasImage = true,
    ),
    SpikePost(
        id = 4,
        author = "半夜煮面",
        time = "41 分钟前",
        title = "深夜食堂：一碗葱油拌面",
        body = "葱油小火熬二十分钟，熬到葱段焦黄，加两勺生抽半勺老抽一点糖，拌面的时候再淋一勺猪油，香得睡不着。",
        forum = "美食",
        reply = "57",
        agree = "1120",
        share = "12",
        hasImage = false,
    ),
    SpikePost(
        id = 5,
        author = "海边的卡夫卡",
        time = "1 小时前",
        title = "求助：显示器 4K 60Hz 偶尔黑屏一秒，是线的问题吗",
        body = "DP 线是买显示器送的，最近打游戏偶尔黑屏一秒又恢复。换了接口还是这样，电源和显卡都排查过了。",
        forum = "显卡",
        reply = "89",
        agree = "76",
        share = "3",
        hasImage = false,
    ),
    SpikePost(
        id = 6,
        author = "旧书摊主",
        time = "2 小时前",
        title = "在旧书摊淘到一本 1983 年的《围城》",
        body = "扉页上有前主人的签名和购书日期，纸张泛黄但没有霉味。老板说这批书是从一个老教授家里收来的。",
        forum = "读书",
        reply = "146",
        agree = "3920",
        share = "45",
        hasImage = true,
    ),
    SpikePost(
        id = 7,
        author = "跑步的鱼",
        time = "3 小时前",
        title = "连续跑步第 100 天，配速终于进 6 了",
        body = "从最开始跑三公里就喘，到现在十公里轻轻松松。膝盖没出问题，秘诀大概就是跑前热身和跑后拉伸。",
        forum = "跑步",
        reply = "73",
        agree = "856",
        share = "6",
        hasImage = false,
    ),
    SpikePost(
        id = 8,
        author = "键盘侠本侠",
        time = "5 小时前",
        title = "客制化键盘入坑一年，说点实话",
        body = "轴体换了七八种，最后留在桌上的是最便宜的红轴。键帽比轴体影响手感更大，别一上来就买贵的。",
        forum = "机械键盘",
        reply = "211",
        agree = "4302",
        share = "67",
        hasImage = true,
    ),
    SpikePost(
        id = 9,
        author = "南山南",
        time = "7 小时前",
        title = "周末去了趟海边，风大得站不稳",
        body = "本来想看日落，结果云层太厚什么都没看到。不过海风很舒服，坐在礁石上发了两个小时呆。",
        forum = "旅行",
        reply = "38",
        agree = "623",
        share = "4",
        hasImage = false,
    ),
    SpikePost(
        id = 10,
        author = "一只咸鱼",
        time = "昨天 23:41",
        title = "问一下大家，贴吧的夜间模式你们用纯黑还是深灰",
        body = "纯黑在 OLED 上确实省电，但卡片边界会糊在一起；深灰层次清楚一点，又觉得不够沉浸。",
        forum = "客户端",
        reply = "95",
        agree = "1477",
        share = "10",
        hasImage = false,
    ),
)

/**
 * 验证 A 的核心：**一张帖子卡 = [GlasmioCard] 外壳 + iOS 信息流排版**。
 *
 * 卡片左右/顶部 16dp，底部留给三个生产按钮自带的 16dp vertical padding；
 * 卡内元素间距 10dp、6dp、8dp，标题 17sp/23sp，正文 15sp/21sp 最多 3 行——全部走 Glasmio
 * 的语义色（`GlasmioTheme.colors.content / secondaryContent / tertiaryContent`），不引入 M2 色。
 */
@Composable
private fun SpikePostCard(
    post: SpikePost,
    photo: ImageBitmap,
    cardMode: Int,
) {
    val colors = GlasmioTheme.colors
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        ) {
            // ---- 作者行：36dp 头像 + 名字（17sp w590）+ 时间（12sp）----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.author,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight(590),
                        color = colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = post.time,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = colors.secondaryContent,
                        maxLines = 1,
                    )
                }
                SfIcon(
                    systemName = "ellipsis",
                    contentDescription = null,
                    size = 18.dp,
                    tint = colors.tertiaryContent,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ---- 标题 17sp / 正文 15sp 最多 3 行 ----
            Text(
                text = post.title,
                fontSize = 17.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight(600),
                color = colors.content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = post.body,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = colors.secondaryContent,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            // ---- 一张圆角配图（本地图，16:9 裁切；圆角跟随 Glasmio 的连续圆角语言）----
            if (post.hasImage) {
                Spacer(modifier = Modifier.height(10.dp))
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                        .clip(ContinuousRoundedRectangle(14.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ---- 吧名 chip：直接复用生产信息流的 ForumInfoChip，验证既有组件能否嵌进 Glasmio 卡片 ----
            ForumInfoChip(
                imageUriProvider = { null },
                nameProvider = { post.forum },
                onClick = {},
            )

            // ---- 三个操作按钮：直接复用生产组件（自带 16dp vertical padding，即卡片底边距）----
            Row(modifier = Modifier.fillMaxWidth()) {
                ThreadShareBtn(
                    shareNum = post.share,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                ThreadReplyBtn(
                    replyNum = post.reply,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                ThreadAgreeBtn(
                    hasAgree = false,
                    agreeNum = post.agree,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    when (cardMode) {
        // 1 = 同样的内容 + 26dp 圆角实色底，但用普通 RoundedCornerShape（仍然 clip → 仍有 layer）。
        1 -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(colors.elevatedBackground),
        ) {
            body()
        }
        // 2 = 连 clip 都不做，只画圆角背景（无 graphicsLayer，是理论上的最省路径）。
        2 -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.elevatedBackground, RoundedCornerShape(26.dp)),
        ) {
            body()
        }
        else -> GlasmioCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        ) {
            body()
        }
    }
}
