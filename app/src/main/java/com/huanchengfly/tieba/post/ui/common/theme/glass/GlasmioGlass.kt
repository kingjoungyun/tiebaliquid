package com.huanchengfly.tieba.post.ui.common.theme.glass

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import com.huanchengfly.tieba.post.ui.page.main.TiebaGlasmioIconResolver
import io.github.neoruaa.glasmio.icons.LocalGlasmioIconResolver
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import io.github.neoruaa.glasmio.core.GlassDimensions
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.defaultGlassColors
import io.github.neoruaa.glasmio.core.glasmioColorScheme
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.glass.LocalBlurBackdrop
import io.github.neoruaa.glasmio.glass.LocalGlassBackdrop
import io.github.neoruaa.glasmio.glass.rememberCrossWindowBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.layerBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.glass.trackBackdropPosition
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.GlassSurface as GlasmioGlassSurface
import io.github.neoruaa.glasmio.glass.NavigationGlassMaterial
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

/**
 * 「真·液态玻璃」接入层（Glasmio）。
 *
 * 这一层只做三件事：
 * 1. 把 [GlasmioBackdropHost] 包一层，并在**玻璃层**里提供一个「host 存在」的标记；
 * 2. 提供 [LiquidGlassSurface]：有 host 就用真玻璃，没有就自动退回 P2 的伪玻璃；
 * 3. 把 App 自己的明暗（[AppTokens.isNightMode]）灌给 Glasmio。
 *
 * 为什么不 `try/catch` 试探：[io.github.neoruaa.glasmio.glass.LocalGlassBackdrop] 是
 * `staticCompositionLocalOf { error(...) }`，没有 host 时读它就是抛异常，异常发生在
 * 组合期间，catch 不住也没法优雅降级。所以用我们自己的 [LocalLiquidGlassHosted] 显式判断。
 */
val LocalLiquidGlassHosted = staticCompositionLocalOf { false }

/**
 * 「上面有 host、`LocalGlassBackdrop` / `LocalBlurBackdrop` 可以读」——**两个 slot 都是 true**。
 *
 * 和 [LocalLiquidGlassHosted] 的区别（很重要，踩过坑）：
 * - 有些消费者**画在另一个 Window**（`GlasmioMenu` 的 Popup、`AnyPopDialog` 的 Dialog），
 *   它们在组合树里位于页面内容中，但绘制不在这一层录制里，所以采样环境是安全的；
 *   这些消费者要的是「有没有 backdrop 可读」→ 用本 local。
 * - 画在页面内容里的玻璃控件**不能**读外层 backdrop（会自己采样自己 → HWUI
 *   `prepareTreeImpl` 递归爆栈 SIGSEGV）→ 它们要的是 [LocalGlassSamplingSafe]。
 */
val LocalGlassBackdropAvailable = staticCompositionLocalOf { false }

/**
 * 「**当前这棵子树画在录制层之外**」——即采样环境里那块 backdrop 里没有自己。
 *
 * 为什么需要这个标记：玻璃控件只能采样「自己不在其中」的录制层，否则会自己采样自己
 * （Glasmio 文档里明确警告的 feedback loop）。host 的 `overlayContent` 是安全的，
 * `sampledContent`（页面内容）**不安全**。
 *
 * 于是：
 * - 安全（在 overlay 里 / 页面自己用的是 `sampledColor` 纯色 host）→ 直接用环境 backdrop，
 *   能采到真实内容（主界面 dock / 顶栏就是这条路）；
 * - 不安全（普通页面内容里）→ 组件自己套一个 44dp 的纯色小 host（见 [com.huanchengfly.tieba.post.ui.common.theme.glass.GlassIconButton]），
 *   保证「真 Glasmio 材质 + 不递归」。
 */
val LocalGlassSamplingSafe = staticCompositionLocalOf { false }

/**
 * Glasmio 的材质渲染管线（RuntimeShader / RenderEffect）要求 API 33+；本工程 minSdk 也是 33，
 * 这里仍然显式判断一次，作为「降级路径」的第一个条件。
 */
@ReadOnlyComposable
fun isLiquidGlassSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * 整个「可被玻璃采样的页面」的容器。
 *
 * [sampledContent] 会被录进一个 graphics layer，玻璃从里面取像素；
 * [overlayContent] 画在录制层之上，玻璃控件必须放在这里，否则会自己采样自己（死循环）。
 */
@Composable
fun LiquidGlassBackdropHost(
    modifier: Modifier = Modifier,
    sampledContent: @Composable BoxScope.() -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit,
) {
    // 不用 Glasmio 的 GlasmioBackdropHost：它固定走 layerBackdrop(backdrop)（reuseContent=false），
    // 也就是 **页面被画两遍**——屏幕上正常画一遍，再往录制层里录一遍。
    // 这里拼一个同构的 host，改用 reuseContent=true：只录进录制层，再把这一层上屏，
    // 页面绘制从 2 遍降到 1 遍（实测 p90/p95 各降约 4/9ms，见 perf-opt-report.md）。
    // Glasmio 对该模式的约束是「可见内容与录制内容必须一致、录制内容里不能读这层 backdrop」，
    // 正好是本函数下面那条 LocalGlassSamplingSafe=false 的约定（采样内容里不许放玻璃控件）。
    val backdrop = rememberLayerBackdrop()
    CompositionLocalProvider(
        LocalGlassBackdrop provides backdrop,
        LocalBlurBackdrop provides backdrop,
    ) {
        Box(modifier) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop, reuseContent = true)
                    .trackBackdropPosition(backdrop),
            ) {
                // 「有 backdrop 可读」为真，但**采样不安全**：这段内容正在被录进本层的 LayerBackdrop。
                // 里面的玻璃控件必须走「自带纯色小 host」那一支（见 GlassIconButton），
                // 否则会自己采样自己 → HWUI 渲染树递归爆栈（实测 SIGSEGV）。
                // 显式置 false，盖掉外层 Activity 级纯色 host 给的真值。
                CompositionLocalProvider(
                    LocalGlassBackdropAvailable provides true,
                    LocalGlassSamplingSafe provides false,
                ) {
                    sampledContent()
                }
            }
            CompositionLocalProvider(
                LocalLiquidGlassHosted provides true,
                LocalGlassSamplingSafe provides true,
                LocalGlassBackdropAvailable provides true,
            ) {
                overlayContent()
            }
        }
    }
}

/**
 * **页级** backdrop host：页面内容进 `sampledContent`（被录进 LayerBackdrop），
 * 页面里的玻璃弹层（Popup 菜单、Dialog）就能采到这一页的真实内容。
 *
 * 用在「需要靠背景折射出内容的页面」上（吧页、历史页…），一行接入：
 * ```
 * GlassPageHost(modifier = Modifier.fillMaxSize()) { pageContent() }
 * ```
 *
 * 注意：页面自己画在内容里的玻璃控件（悬浮圆钮等）**不能**采样这层 backdrop，
 * 它们在 `sampledContent` 里，`LocalGlassSamplingSafe = false` 会让
 * [GlassIconButton] / [GlassCircleSurface] 自动改用「自带纯色小 host」那一支。
 *
 * 代价：整页录制（内容变化时每帧多一次离屏渲染 + 一张全屏纹理）。
 * 只在需要的页面上开，见简报里的接入清单。
 */
@Composable
fun GlassPageHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!isLiquidGlassSupported()) {
        content()
        return
    }
    LiquidGlassBackdropHost(
        modifier = modifier,
        sampledContent = { content() },
        overlayContent = {},
    )
}

/**
 * **Activity 级** backdrop host（当前默认关闭，见 `enabled`）。
 *
 * 设计意图：包一层就让所有页面/弹层都有 backdrop，菜单在哪儿都能采到背景。
 *
 * **为什么默认关**：实测在这台模拟器（SwiftShader，无 GPU）上，Activity 级 host +
 * 页面里再套页级 host 的层级，会在 HWUI 的 `RenderNode::prepareTreeImpl` 里递归爆栈
 * （`Fatal signal 11 … Cause: stack pointer is not in a rw map; likely due to stack overflow`，
 * 512 帧同一个函数来回递归），冷启动 ~25s 内必崩；同一个交互脚本在基线 commit 上不崩。
 * 关掉它之后（页级 host 方案）反复切页/开菜单不再复现。
 *
 * 想重新评估（比如真机上）时把 `enabled` 打开，或者直接调 [GlassPageHost]。
 */
@Composable
fun GlassRootHost(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (!enabled || !isLiquidGlassSupported() || isGlassDebugDisabled()) {
        content()
        return
    }
    // 纯色 host：页面内容全部落到 overlay 层，页面里的玻璃控件采样它是安全的。
    val background = glassColors().background
    GlasmioBackdropHost(
        sampledColor = background,
        modifier = modifier.fillMaxSize(),
    ) {
        CompositionLocalProvider(
            LocalLiquidGlassHosted provides true,
            LocalGlassSamplingSafe provides true,
            LocalGlassBackdropAvailable provides true,
        ) {
            content()
        }
    }
}

/**
 * 调试开关（**仅 debug 包生效**）：`adb shell settings put global tieba_no_glass 1` 后重启 App，
 * Activity 级 host 不装 → 所有玻璃弹层退回 M2、悬浮钮退回纯色。
 *
 * 存在的意义：拍「有/无 backdrop」的对照图（证明菜单真的在采样背景），
 * 以及在没有 GPU 的模拟器上临时关掉玻璃排查性能。release 包里这个分支会被常量折叠掉。
 */
@Composable
private fun isGlassDebugDisabled(): Boolean {
    if (!BuildConfig.DEBUG) return false
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getInt(context.contentResolver, "tieba_no_glass", 0) == 1
        }.getOrDefault(false)
    }
}

/**
 * **Dialog 窗口**里的玻璃采样环境。
 *
 * Dialog 是独立的 Window，页面那层 `LayerBackdrop` 的坐标不能直接跨 compose owner 用；
 * Glasmio 的 [rememberCrossWindowBackdrop] 用屏幕坐标做映射，spike 里已经逐像素验证过。
 * 包上之后，对话框内容里的 `GlasmioCard` / `GlassSurface` / `GlasmioMenu` 都能采到页面背景
 * （不包就只能拿到空 backdrop 或者干脆抛异常）。
 *
 * 没有 host / API < 33 时直接透传：对话框保持原来的 M2 外观。
 */
@Composable
fun GlassDialogBackdropScope(content: @Composable () -> Unit) {
    if (!isLiquidGlassSupported() || !LocalGlassBackdropAvailable.current) {
        content()
        return
    }
    val hostBackdrop = LocalBlurBackdrop.current
    val crossWindow = rememberCrossWindowBackdrop(hostBackdrop)
    CompositionLocalProvider(
        LocalGlassBackdrop provides crossWindow,
        LocalBlurBackdrop provides crossWindow,
        LocalLiquidGlassHosted provides true,
        LocalGlassSamplingSafe provides true,
        LocalGlassBackdropAvailable provides true,
    ) {
        content()
    }
}

/**
 * 页面顶栏是否搬进 host 的 `overlayContent`（P2：让顶栏上的悬浮钮采真实内容）。
 *
 * 试点页面：吧页 `ForumPage`、设置页 `PrefsScreen`。默认开；关掉就走各自的旧路径
 * （吧页顶栏留在 Scaffold 的 topBar、设置页用纯色 host）。
 *
 * 真机 A/B 用（同一个 APK，不用重装）：
 * `adb shell settings put global tieba_overlay_topbar 0` 后重启 App 即为旧路径。
 */
/** 顶栏进 overlay 的试点页（真机 A/B 的结论：设置页保留，吧页回退）。 */
enum class GlassOverlayPage { PREFS, FORUM }

/**
 * 页面顶栏是否搬进 host 的 `overlayContent`（P2：让顶栏悬浮钮采真实内容）。
 *
 * **默认值按真机 A/B 结论定**（<device>，设备温度 ~28°C，8 轮 × 每轮一次滑动）：
 * - `PREFS`（设置页）开启：p50 18.5 → 19.5ms（+1.0），jank 6.4% → 5.0%（变好）→ 可以推广
 * - `FORUM`（吧页）**关闭**：p50 13.5 → 44.5ms（+31ms，远超 3ms 判据）→ 不推广，回退旧路径
 *
 * 覆盖开关（同一个 APK 不用重装，改完重启 App 生效）：
 * `adb shell settings put global tieba_overlay_topbar 1|0` → 强制全开 / 全关（仅 debug）。
 */
@Composable
fun useOverlayTopBarHost(page: GlassOverlayPage): Boolean {
    if (!isLiquidGlassSupported()) return false
    val default = when (page) {
        GlassOverlayPage.PREFS -> true
        GlassOverlayPage.FORUM -> false
    }
    if (!BuildConfig.DEBUG) return default
    val context = LocalContext.current
    val forced = remember(context) {
        runCatching {
            Settings.Global.getInt(context.contentResolver, "tieba_overlay_topbar", -1)
        }.getOrDefault(-1)
    }
    return if (forced == 0 || forced == 1) forced == 1 else default
}

/**
 * 玻璃表面：真玻璃可用时用 Glasmio 的 [io.github.neoruaa.glasmio.glass.GlassSurface]，
 * 否则退回 P2 的伪玻璃（[GlassSurface]：半透明 + 1dp 描边 + 阴影）。
 *
 * 降级条件（任一成立就走伪玻璃）：
 * - [LocalLiquidGlassHosted] 为 false —— 当前这棵子树不在 [LiquidGlassBackdropHost] 里
 *   （例如宽屏走 NavigationRail 分支、或者开关关掉了玻璃层）。这一条也保证了「不会白屏、
 *   不会因为读不到 backdrop 而崩」。
 * - [isLiquidGlassSupported] 为 false —— API < 33，Glasmio 的 shader 起不来。
 *
 * 注意：降级是**整块表面**级的（形状/内容完全一致，只换 background 实现），所以布局不会跳。
 *
 * @param navigationSurfaceColor 覆盖 Glasmio 材质的 tint（FlClash 的 regular tint）。null 走 Glasmio 默认；
 * @param navigationSurfaceAlphaMultiplier Glasmio 的 tint alpha 乘数；我们传的 tint 自带 alpha，用 1f 保持原值；
 * @param emphasis Regular / Prominent，决定材质走 regular 还是 prominent；
 * @param innerGlow 自绘 1px 上缘高光 + 微弱内发光（深色下把壳从纯黑背景里拉出来）。
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    material: NavigationGlassMaterial = NavigationGlassMaterial.RegularSmall,
    navigationSurfaceColor: Color? = null,
    navigationSurfaceAlphaMultiplier: Float = 1f,
    emphasis: GlassEmphasis = GlassEmphasis.Regular,
    innerGlow: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val hosted = LocalLiquidGlassHosted.current
    if (hosted && isLiquidGlassSupported()) {
        LiquidGlassTheme(accent = AppTokens.primary) {
            GlasmioGlassSurface(
                modifier = modifier,
                shape = shape,
                material = material,
                navigationSurfaceColor = navigationSurfaceColor,
                navigationSurfaceAlphaMultiplier = navigationSurfaceAlphaMultiplier,
                emphasis = emphasis,
            ) {
                content()
                if (innerGlow) GlassInnerGlow(shape)
            }
        }
    } else {
        val colors = glassColors()
        // 降级路径：P2 的伪玻璃，刻意保留
        GlassSurface(
            modifier = modifier,
            shape = shape,
            fill = navigationSurfaceColor ?: colors.glassFill,
            strokeColor = if (navigationSurfaceColor != null && colors.isDark) {
                Color.White.copy(alpha = 0.18f)
            } else {
                colors.glassStroke
            },
            content = {
                content()
                if (innerGlow) GlassInnerGlow(shape)
            },
        )
    }
}

/**
 * 玻璃内缘的高光/内发光：1px 渐变描边（上缘最亮）+ 一条更宽的弱白色内发光。
 *
 * 画在 surface 的 content 层之上、并 clip 到形状里，所以不会盖住 content 的命中区（纯绘制）。
 * 深色下白边更亮，浅色下压低，避免亮玻璃上再出现一道明显的白线。
 */
@Composable
internal fun BoxScope.GlassInnerGlow(shape: Shape) {
    val dark = glassColors().isDark
    val rimAlpha = if (dark) 0.72f else 0.46f
    val glowAlpha = if (dark) 0.10f else 0.05f
    Spacer(
        modifier = Modifier
            .matchParentSize()
            .clip(shape)
            .drawWithContent {
                drawContent()
                val stroke = 1.dp.toPx()
                val glowStroke = 5.dp.toPx()
                val rimBrush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = rimAlpha),
                    0.32f to Color.White.copy(alpha = rimAlpha * 0.22f),
                    1f to Color.White.copy(alpha = 0.02f),
                )
                val glowBrush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = glowAlpha),
                    0.55f to Color.White.copy(alpha = glowAlpha * 0.35f),
                    1f to Color.Transparent,
                )
                val inset = stroke / 2f
                val insetSize = Size(
                    width = (size.width - stroke).coerceAtLeast(0f),
                    height = (size.height - stroke).coerceAtLeast(0f),
                )
                val outline = shape.createOutline(insetSize, layoutDirection, this)
                // 弱内发光：宽一点、很淡，只为了在纯黑背景里勾出壳的轮廓
                translate(inset, inset) {
                    drawOutline(
                        outline = outline,
                        brush = glowBrush,
                        style = Stroke(width = glowStroke, cap = StrokeCap.Round),
                    )
                }
                // 1px 上缘高光
                translate(inset, inset) {
                    drawOutline(outline = outline, brush = rimBrush, style = Stroke(stroke))
                }
            }
    )
}

/**
 * Glasmio 的主题跟随：用 App 自己的 `isNightMode`（不是系统的 `isSystemInDarkTheme()`，
 * 这个 App 默认不跟随系统），Glasmio 据此解析 light/dark 材质配方与语义前景色。
 *
 * **accent 注入（唯一来源）**：调用方传 App 的 `AppTokens.primary`，这里把它同时写进
 * - Glasmio 的 M3 `ColorScheme`（primary / secondary / onPrimary），Glasmio 控件读
 *   `MaterialTheme.colorScheme.primary` 或 `GlasmioTheme.colors.accent` 都会拿到它；
 * - Glasmio 的 `GlassColors.accent`（图标、列表勾选、开关默认 tint 的来源）。
 *
 * 没有传/传了 `Color.Unspecified`（预览兜底）时退回 Glasmio 默认蓝，避免出现「透明 accent」。
 *
 * 深色背景固定纯黑、卡片固定 `#1C1C1E`：A2 的「深色统一纯黑」在**适配层**落地，
 * 这样不依赖 Glasmio 内部默认值（升级 AAR 也不会漂移）。
 *
 * 只包玻璃这一小块：Glasmio 内部会装一个 M3 `MaterialTheme`，包太大有污染现有 M2 页面的风险；
 * 玻璃控件之外的内容保持原样。
 */
@Composable
fun LiquidGlassTheme(
    accent: Color = AppTokens.primary,
    content: @Composable () -> Unit,
) {
    val isDark = AppTokens.isNightMode
    val resolvedAccent = if (accent == Color.Unspecified) {
        if (isDark) Color(0xFF0091FF) else Color(0xFF0088FF)
    } else {
        accent
    }
    val onAccent = if (resolvedAccent.luminance() > 0.6f) Color.Black else Color.White

    // Stage 5 · C：主题**全部由 AppTokens 显式构造**，不再读 Glasmio 的默认值。
    val tokens = glassTokenSet(isDark, resolvedAccent)
    val colorScheme = glasmioColorScheme(isDark, tokens.accent).copy(
        primary = tokens.accent,
        onPrimary = onAccent,
        secondary = tokens.accent,
        onSecondary = onAccent,
        surfaceTint = tokens.accent,
        background = tokens.background,
        onBackground = tokens.textPrimary,
        surface = tokens.background,
        onSurface = tokens.textPrimary,
        surfaceVariant = tokens.card,
        onSurfaceVariant = tokens.textSecondary,
        surfaceContainer = tokens.card,
        outline = tokens.separator,
        outlineVariant = tokens.stroke,
        error = tokens.destructive,
    )
    // GlassColors 的每一个字段都来自 token；深色背景就是纯黑 #000000（token 定义），
    // 不再是 Glasmio 的 groupedBackground 灰。
    // 每个字段都显式来自 token（copy 覆盖默认值）；深色背景 = 纯黑 #000000。
    val glassColors = defaultGlassColors(isDark, tokens.accent).copy(
        accent = tokens.accent,
        content = tokens.textPrimary,
        secondaryContent = tokens.textSecondary,
        container = tokens.card,
        prominentContainer = tokens.cardProminent,
        subtleStroke = tokens.stroke,
        groupedBackground = tokens.background,
        elevatedBackground = tokens.card,
        separator = tokens.separator,
        tertiaryContent = tokens.textTertiary,
        destructive = tokens.destructive,
    )
    val glassDimensions = GlassDimensions(
        compactCornerRadius = tokens.radiusCompact,
        regularCornerRadius = tokens.radiusCard,
        sheetCornerRadius = tokens.radiusSheet,
        controlHeight = tokens.controlHeight,
        iconButtonSize = tokens.iconButtonSize,
        bottomBarHeight = tokens.bottomBarHeight,
    )

    // 图标解析器在这里注入一次：设置页、顶栏、列表箭头都会走 TiebaGlasmioIconResolver
    // （dock 图形 + Phosphor），不再落到 Glasmio 内置的 Material 兜底表。
    CompositionLocalProvider(
        LocalGlasmioIconResolver provides TiebaGlasmioIconResolver,
        LocalGlassTokens provides tokens,
    ) {
        GlasmioTheme(
            isDark = isDark,
            colorScheme = colorScheme,
            colors = glassColors,
            dimensions = glassDimensions,
            // Glasmio 的语义排版表（iOS Dynamic Type 的 12 档）默认用系统字族；
            // 这里整表换成鸿蒙，字号/行高/字重仍由 Glasmio 决定。
            typography = HarmonyGlasmioTypography,
            content = content,
        )
    }
}