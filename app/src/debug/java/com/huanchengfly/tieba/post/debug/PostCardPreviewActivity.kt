package com.huanchengfly.tieba.post.debug

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.huanchengfly.tieba.post.api.models.protos.Agree
import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.personalized.DislikeReason
import com.huanchengfly.tieba.post.api.models.protos.personalized.ThreadPersonalized
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.HarmonyFontFamily
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.DislikeMoreButton
import com.huanchengfly.tieba.post.ui.common.theme.glass.HarmonyTextProvider
import com.huanchengfly.tieba.post.ui.page.main.home.PostCard
import com.huanchengfly.tieba.post.utils.ThemeUtil
import io.github.neoruaa.glasmio.core.GlasmioTheme
import java.io.File
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

/**
 * **只读预览页**：用生产组件 [PostCard] 渲染一批静态假帖子，用来验收「推特式」卡片的版式。
 *
 * 这个页面不碰业务数据、不写 DataStore（除深色开关走 `ThemeUtil` 与设置页同一条链路），
 * 存在的意义是：App 未登录 / 首页空列表时，仍然能在真机上看清
 * 「头像左列 / 时间在用户名右边 / 标题正文左对齐 / 媒体顶到右边界 / 底行吧名左 + 操作右」。
 * 生产路径在 `PersonalizedPage` 的 `TwitterFeedList`，用的是同一个 `PostCard`。
 *
 * 启动（模拟器已装好 debug 包）：
 * ```
 * adb shell am start -n com.tieba/com.huanchengfly.tieba.post.debug.PostCardPreviewActivity
 * adb shell am start -n com.tieba/...PostCardPreviewActivity --ez dark true
 * adb shell am start -n com.tieba/...PostCardPreviewActivity --ez dark true --ei scroll_dp 260
 * ```
 * 点赞可点：卡片右下角的 ♡，点一下变红并且数字 +1（本地状态，仅预览用）。
 */
class PostCardPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 与 GlassFeedPreviewActivity 同一条链路：写生产 DataStore，截出来的深色是 App 的真深色。
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

        val scrollDp = intent.getIntExtra(EXTRA_SCROLL_DP, 0)
        val likedFirst = intent.getBooleanExtra(EXTRA_LIKED, false)
        val fontAb = intent.getBooleanExtra(EXTRA_FONT_AB, false)

        setContent {
            TiebaLiteTheme {
                if (fontAb) {
                    FontAbPage(window = window)
                } else {
                    PostCardPreviewPage(
                        window = window,
                        initialScrollDp = scrollDp,
                        likedFirst = likedFirst,
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_DARK = "dark"
        const val EXTRA_SCROLL_DP = "scroll_dp"
        const val EXTRA_LIKED = "liked"

        /** `--ez fontab true`：只渲染字重/字族对照页（验收鸿蒙 600 不是合成加粗）。 */
        const val EXTRA_FONT_AB = "fontab"
    }
}

@Composable
private fun PostCardPreviewPage(
    window: android.view.Window,
    initialScrollDp: Int,
    likedFirst: Boolean,
) {
    val isDark = AppTokens.isNightMode
    SideEffect {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    LiquidGlassTheme(accent = AppTokens.primary) {
        HarmonyTextProvider {
        val colors = GlasmioTheme.colors
        val context = LocalContext.current
        val photos = remember { PreviewPhotos(context) }
        var posts by remember { mutableStateOf(previewPosts(photos, likedFirst)) }
        val listState = rememberLazyListState()
        val density = LocalDensity.current

        LaunchedEffect(Unit) {
            if (initialScrollDp > 0) {
                listState.scrollBy(with(density) { initialScrollDp.dp.toPx() })
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.groupedBackground),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 48.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(posts, key = { it.id }) { post ->
                    val holder = remember(post) { wrapImmutable(post) }
                    val personalized = remember(post.threadId) { previewPersonalized(post.threadId) }
                    PostCard(
                        item = holder,
                        onClick = {},
                        onClickReply = {},
                        onClickForum = {},
                        onClickUser = {},
                        onAgree = { agreed ->
                            // 与生产链路一致：乐观更新（先变色 + 数字 ±1）。
                            posts = posts.map {
                                if (it.threadId == agreed.threadId) it.toggledAgree() else it
                            }
                        },
                        menuAction = {
                            DislikeMoreButton(
                                personalized = personalized,
                                onDislike = { _, _ -> },
                            )
                        },
                    )
                }
            }
        }
        }
    }
}

/**
 * 字体/字重对照页（`--ez fontab true`）。
 *
 * 同一句话、同一个字号，三种渲染并排：
 * 1. **系统字体（Roboto）+ w600**：Roboto 没有 600 档，走合成加粗 —— 笔画会糊、边缘发虚；
 * 2. **鸿蒙 + w600**：`HarmonyFontFamily` 声明了 600 档（Semibold ttf），是真字重；
 * 3. **鸿蒙 + w400**：Regular，用来对比 600 确实更粗而不是「同一档变模糊」。
 *
 * 三行都用同一个 `Text`（M2）与同样的字号/行高，只有 `fontFamily` / `fontWeight` 不同。
 */
@Composable
private fun FontAbPage(window: android.view.Window) {
    val isDark = AppTokens.isNightMode
    SideEffect {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }
    val sample = "折腾了一下午，终于把龟背竹救活了"
    LiquidGlassTheme(accent = AppTokens.primary) {
        HarmonyTextProvider {
        val colors = GlasmioTheme.colors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.groupedBackground)
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                FontAbRow(
                    label = "系统字族（FontFamily.Default）· w600",
                    text = sample,
                    family = FontFamily.Default,
                    weight = FontWeight(600),
                )
                FontAbRow(
                    label = "鸿蒙（显式 fontFamily）· w600",
                    text = sample,
                    family = HarmonyFontFamily,
                    weight = FontWeight(600),
                )
                FontAbRow(
                    label = "鸿蒙（靠 HarmonyTextProvider 注入，不写 fontFamily）· w600",
                    text = sample,
                    family = null,
                    weight = FontWeight(600),
                )
                FontAbRow(
                    label = "鸿蒙（显式 fontFamily）· w400",
                    text = sample,
                    family = HarmonyFontFamily,
                    weight = FontWeight(400),
                )
            }
        }
        }
    }
}

@Composable
private fun FontAbRow(
    label: String,
    text: String,
    family: FontFamily?,
    weight: FontWeight,
) {
    val colors = GlasmioTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.secondaryContent,
            fontFamily = HarmonyFontFamily,
        )
        Text(
            text = text,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            fontWeight = weight,
            color = colors.content,
            // family == null：不写 fontFamily，只沿用 M2 LocalTextStyle —— 也就是 HarmonyTextProvider 注入的鸿蒙。
            style = if (family == null) LocalTextStyle.current else LocalTextStyle.current.copy(fontFamily = family),
        )
    }
}

private fun previewPersonalized(threadId: Long): ImmutableHolder<ThreadPersonalized> = wrapImmutable(
    ThreadPersonalized(
        tid = threadId,
        dislikeResource = listOf(
            DislikeReason(dislikeReason = "内容不感兴趣", dislikeId = 1),
            DislikeReason(dislikeReason = "重复推荐", dislikeId = 2),
            DislikeReason(dislikeReason = "质量差", dislikeId = 3),
            DislikeReason(dislikeReason = "不想看这个吧", dislikeId = 7),
        ),
    )
)

private fun ThreadInfo.toggledAgree(): ThreadInfo {
    val current = agree ?: Agree(agreeNum = agreeNum.toLong(), hasAgree = 0)
    return if (current.hasAgree == 1) {
        copy(
            agreeNum = (agreeNum - 1).coerceAtLeast(0),
            agree = current.copy(agreeNum = current.agreeNum - 1, hasAgree = 0),
        )
    } else {
        copy(
            agreeNum = agreeNum + 1,
            agree = current.copy(agreeNum = current.agreeNum + 1, hasAgree = 1),
        )
    }
}

/**
 * 预览用的配图：运行时在 cache 目录画几张渐变 PNG，用 `file://` URI 喂给 Sketch。
 *
 * 这么做的原因：仓库里只有一张真图（`user_header.jpg`），多图帖会把同一张图排三遍，
 * 截图里分不清「三张独立图片」还是「一张被裁成三块」。
 */
private class PreviewPhotos(context: Context) {
    val wide: String
    val squares: List<String>

    init {
        val dir = File(context.cacheDir, "post-card-preview").apply { mkdirs() }
        wide = gradient(File(dir, "wide.png"), 1200, 675, 0xFF3A6073.toInt(), 0xFF0F2027.toInt())
        squares = listOf(
            gradient(File(dir, "sq1.png"), 720, 720, 0xFF6A82FB.toInt(), 0xFF2B3A8F.toInt()),
            gradient(File(dir, "sq2.png"), 720, 720, 0xFFF7971E.toInt(), 0xFFB34700.toInt()),
            gradient(File(dir, "sq3.png"), 720, 720, 0xFF11998E.toInt(), 0xFF0B5D57.toInt()),
            gradient(File(dir, "sq4.png"), 720, 720, 0xFFE96443.toInt(), 0xFF6A2C70.toInt()),
        )
    }

    private fun gradient(file: File, width: Int, height: Int, from: Int, to: Int): String {
        if (!file.exists()) {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawRect(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, width.toFloat(), height.toFloat(), from, to, Shader.TileMode.CLAMP
                    )
                },
            )
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 92, it) }
            bitmap.recycle()
        }
        return "file://${file.absolutePath}"
    }
}

private fun previewPosts(photos: PreviewPhotos, likedFirst: Boolean): List<ThreadInfo> {
    val now = (System.currentTimeMillis() / 1000).toInt()

    fun author(id: Long, name: String) = User(id = id, name = name, nameShow = name, portrait = "")

    fun media(uri: String, width: Int, height: Int) = Media(
        type = 3,
        bigPic = uri,
        srcPic = uri,
        dynamicPic = uri,
        originPic = uri,
        width = width,
        height = height,
        originSize = 240_000,
        postId = 1L,
    )

    return listOf(
        // 纯文字帖：标题 1 行 + 正文 3 行（验证正文最多 3 行省略），点赞数 5 位（"1.2W"）
        ThreadInfo(
            id = 1L,
            threadId = 9001L,
            title = "折腾了一下午，终于把阳台上的龟背竹救活了",
            forumId = 101L,
            forumName = "植物",
            forumInfo = SimpleForum(id = 101L, name = "植物", avatar = ""),
            author = author(11L, "青柠不酸"),
            lastTimeInt = now - 3 * 60,
            replyNum = 128,
            shareNum = 36,
            agreeNum = 12000,
            agree = Agree(agreeNum = 12000, hasAgree = if (likedFirst) 1 else 0),
            richAbstract = listOf(
                PbContent(
                    type = 0,
                    text = "之前一直烂根，换了疏松的土、减少浇水频率，新叶子这两天总算抽出来了。" +
                        "配土是泥炭 5 + 珍珠岩 3 + 松树皮 2，盆底垫了三厘米陶粒。"
                )
            ),
        ),
        // 纯文字帖：短标题 + 两行正文
        ThreadInfo(
            id = 2L,
            threadId = 9002L,
            title = "有没有人跟我一样，一打开贴吧就先看吧务后台",
            forumId = 102L,
            forumName = "吧务",
            forumInfo = SimpleForum(id = 102L, name = "吧务", avatar = ""),
            author = author(12L, "晚风过境"),
            lastTimeInt = now - 12 * 60,
            replyNum = 64,
            shareNum = 8,
            agreeNum = 2381,
            agree = Agree(agreeNum = 2381, hasAgree = 0),
            richAbstract = listOf(
                PbContent(type = 0, text = "每天醒来第一件事不是看消息，是点开后台看看有没有被删帖、有没有人申诉。")
            ),
        ),
        // 单图帖：16:9 配图，右沿顶到卡内边距
        ThreadInfo(
            id = 3L,
            threadId = 9003L,
            title = "雨后的老城区，随手拍了几张",
            forumId = 103L,
            forumName = "摄影",
            forumInfo = SimpleForum(id = 103L, name = "摄影", avatar = ""),
            author = author(13L, "老张的相机"),
            lastTimeInt = now - 27 * 60,
            replyNum = 302,
            shareNum = 91,
            agreeNum = 6842,
            agree = Agree(agreeNum = 6842, hasAgree = 0),
            richAbstract = listOf(
                PbContent(type = 0, text = "机身 A7C2 + 35mm，后期只做了简单曲线和去雾。雨天的高光特别好看。")
            ),
            media = listOf(media(photos.wide, 1200, 675)),
        ),
        // 纯文字帖：一条短帖，用来让一屏里能放下 4~6 条
        ThreadInfo(
            id = 4L,
            threadId = 9004L,
            title = "深夜食堂：一碗葱油拌面",
            forumId = 104L,
            forumName = "美食",
            forumInfo = SimpleForum(id = 104L, name = "美食", avatar = ""),
            author = author(14L, "半夜煮面"),
            lastTimeInt = now - 41 * 60,
            replyNum = 57,
            shareNum = 12,
            agreeNum = 1120,
            agree = Agree(agreeNum = 1120, hasAgree = 0),
            richAbstract = listOf(
                PbContent(type = 0, text = "葱油小火熬二十分钟，熬到葱段焦黄，加两勺生抽半勺老抽一点糖。")
            ),
        ),
        // 多图帖：4 张 → 并排 3 张 + 右下角 +1
        ThreadInfo(
            id = 5L,
            threadId = 9005L,
            title = "【扫街】老城区的二十张照片",
            forumId = 103L,
            forumName = "摄影",
            forumInfo = SimpleForum(id = 103L, name = "摄影", avatar = ""),
            author = author(13L, "老张的相机"),
            lastTimeInt = now - 55 * 60,
            replyNum = 41,
            shareNum = 17,
            agreeNum = 936,
            agree = Agree(agreeNum = 936, hasAgree = 0),
            richAbstract = listOf(
                PbContent(type = 0, text = "挑了几张自己最喜欢的，原图都在手机里，慢慢挑。")
            ),
            media = listOf(
                media(photos.squares[0], 720, 720),
                media(photos.squares[1], 720, 720),
                media(photos.squares[2], 720, 720),
                media(photos.squares[3], 720, 720),
            ),
        ),
        // 纯文字帖：短标题 + 短正文
        ThreadInfo(
            id = 6L,
            threadId = 9006L,
            title = "连续跑步第 100 天，配速终于进 6 了",
            forumId = 105L,
            forumName = "跑步",
            forumInfo = SimpleForum(id = 105L, name = "跑步", avatar = ""),
            author = author(15L, "跑步的鱼"),
            lastTimeInt = now - 3 * 3600,
            replyNum = 73,
            shareNum = 6,
            agreeNum = 856,
            agree = Agree(agreeNum = 856, hasAgree = 0),
            richAbstract = listOf(PbContent(type = 0, text = "从最开始跑三公里就喘，到现在十公里轻轻松松。")),
        ),
        // 精品帖（标题前缀「精品」占位）
        ThreadInfo(
            id = 7L,
            threadId = 9007L,
            title = "客制化键盘入坑一年，说点实话",
            forumId = 106L,
            forumName = "机械键盘",
            forumInfo = SimpleForum(id = 106L, name = "机械键盘", avatar = ""),
            author = author(16L, "键盘侠本侠"),
            lastTimeInt = now - 5 * 3600,
            isGood = 1,
            replyNum = 211,
            shareNum = 67,
            agreeNum = 4302,
            agree = Agree(agreeNum = 4302, hasAgree = 0),
            richAbstract = listOf(
                PbContent(type = 0, text = "轴体换了七八种，最后留在桌上的是最便宜的红轴。")
            ),
        ),
    )
}
