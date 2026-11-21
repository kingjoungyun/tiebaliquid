package com.huanchengfly.tieba.post.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations

/**
 * 页面转场：用 compose-destinations 的 `RootNavGraphDefaultAnimations` 复刻 iOS 的 push/pop。
 *
 * **为什么不直接用 Glasmio 的导航动效**：Glasmio 的 `IosNavigationMotion` 绑在 Navigation 3 的
 * `NavDisplay` 上（`glasmio-scaffold/.../navigation/`），本工程是 compose-destinations（基于
 * Navigation Compose，40+ destination、深链、回退栈）。迁移到 NavDisplay = 重写导航层，
 * 成本远超动效本身的收益，所以这里只**复刻规格**（曲线/时长参考 `IosNavigationMotion.kt`：
 * `IosNavigationDurationMillis = 350`，临界阻尼弹簧）。
 *
 * iOS push 的观感（本实现的近似）：
 * - 新页从右侧整屏滑入（`initialOffset = { it }`）；
 * - 旧页向左只走 1/3（`targetOffset = { -it / 3 }`，视差），同时淡出 → 读起来就是「变暗」；
 * - pop 完全反向：新页从左侧 1/3 处滑回并淡入，旧页整屏向右滑出。
 *
 * 用 `tween(350, CubicBezier(0.32, 0.72, 0, 1))` 近似 Glasmio 的临界阻尼弹簧：
 * 弹簧在 350ms 处收尾、没有回弹，这条曲线的形态最接近，且可被 Compose 的 seek/中断安全处理
 * （弹簧在拖动/快速连点时更容易出现「追不上」的观感）。
 *
 * **回滚开关**：[EnableIosPush] 置 false 就退回改造前的 spring 滑入滑出，一行回滚。
 *
 * 注意：动效挂在**根 NavGraph 的默认值**上，所以对所有 destination 一致生效；
 * 逐 destination 定制需要 `@Destination(style = ...)` 或在生成的 NavGraph 里传参数，
 * 本工程由 KSP 生成，暂不做（TODO）。独立 Activity 的页面（如全屏看图 `PhotoViewActivity`）
 * 不在这个 NavHost 里，天然不受影响。
 */
object TiebaNavTransitions {

    /** 一行回滚：false = 改造前的 spring 转场。 */
    const val EnableIosPush: Boolean = true

    /** 350ms，临界阻尼弹簧的近似曲线（快出、缓收）。 */
    private val IosEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    private val IosOffsetSpec: FiniteAnimationSpec<IntOffset> = tween(350, easing = IosEasing)
    private val IosFadeSpec: FiniteAnimationSpec<Float> = tween(350, easing = IosEasing)

    /** 改造前用的 spring（保留作回滚路径）。 */
    private val LegacySpec = spring<IntOffset>(
        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )

    fun current(): RootNavGraphDefaultAnimations = if (EnableIosPush) ios() else legacy()

    /** iOS push / pop。 */
    private fun ios(): RootNavGraphDefaultAnimations = RootNavGraphDefaultAnimations(
        // push：新页整屏从右滑入
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = IosOffsetSpec,
                initialOffset = { it },
            )
        },
        // push：旧页只左移 1/3（视差）并淡出 → 视觉上「变暗/退后」
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = IosOffsetSpec,
                targetOffset = { -it / 3 },
            ) + fadeOut(IosFadeSpec)
        },
        // pop：被返回的页面从左侧 1/3 处滑回并淡入（从「变暗」恢复）
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = IosOffsetSpec,
                initialOffset = { -it / 3 },
            ) + fadeIn(IosFadeSpec)
        },
        // pop：当前页整屏向右滑出
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = IosOffsetSpec,
                targetOffset = { it },
            )
        },
    )

    /** 改造前的转场（spring 滑入滑出），用于一行回滚。 */
    private fun legacy(): RootNavGraphDefaultAnimations = RootNavGraphDefaultAnimations(
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = LegacySpec,
                initialOffset = { it },
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = LegacySpec,
                targetOffset = { -it },
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = LegacySpec,
                initialOffset = { -it },
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = LegacySpec,
                targetOffset = { it },
            )
        },
    )
}
