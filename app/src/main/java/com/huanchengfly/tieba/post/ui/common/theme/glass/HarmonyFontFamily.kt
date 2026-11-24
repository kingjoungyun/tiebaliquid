package com.huanchengfly.tieba.post.ui.common.theme.glass

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.huanchengfly.tieba.post.R
import io.github.neoruaa.glasmio.core.glasmioTypography

/**
 * 鸿蒙字体（HarmonyOS Sans SC），只用于 Glasmio 那一支 UI。
 *
 * 原始字体每档 ~8MB，直接进 APK 不可接受；这里的三档是 `scripts/subset-fonts.py` 的产物：
 * ASCII + GB2312 全字集 + 常用中文标点，单档 1.7MB 左右（三档合计 ~5.2MB）。
 *
 * ## 任务 0（PLAN §17 第 0 步）：正文整体偏细
 *
 * 用户实测「字体整体偏细」的根因是 **HarmonyOS Sans SC 的 Regular 比 Noto/Roboto 同字重
 * 更轻**，不是字号/行高的问题（34sp 大标题观感正常，只有正文发虚）。
 *
 * 修法有两层，缺一不可：
 *
 * 1. **本字族的 Normal 槽（400）指向 Medium 字重文件**（下面这行）——这是**实际生效**的那层。
 *    业务页里的 `Text` 绝大多数是 **M2** 的 `Text`（信息流正文、楼层正文、设置行…），
 *    它们只吃 [HarmonyTextProvider] 注入的 `fontFamily`，字重来自各自写死/主题继承的
 *    `fontWeight = Normal`，**根本不读 M3 的排版表**；所以只改 [HarmonyGlasmioTypography]
 *    的 body 档位对信息流没有任何效果（原计划就是这么写的，实测无效）。
 * 2. [HarmonyGlasmioTypography] 里 body / label 档位显式设为 `Medium(500)`（见下），
 *    让 M3 那一侧（Glasmio 组件）的排版表语义与这一层一致；因为两档最终指向同一个
 *    Medium 文件，不会出现「双重加粗」。
 *
 * 注意：
 * - 字重仍是**真实字重**（Medium.ttf），不是引擎合成的假粗体 —— 合成加粗才是「发虚」的来源；
 * - `FontWeight.Light(300)` 没有独立文件，会就近落到 Medium 槽（与改造前落到 Regular 一样）；
 * - 因为 Normal 槽改指向 Medium，`harmony_sans_sc_regular.ttf` 目前**没有引用**了
 *   （约 1.7MB 可另行删除，本轮不动资源，避免影响字体子集脚本的可重复性）。
 */
val HarmonyFontFamily: FontFamily = FontFamily(
    Font(R.font.harmony_sans_sc_medium, FontWeight.Normal),
    Font(R.font.harmony_sans_sc_medium, FontWeight.Medium),
    Font(R.font.harmony_sans_sc_semibold, FontWeight.SemiBold),
    Font(R.font.harmony_sans_sc_semibold, FontWeight.Bold),
)

/**
 * Glasmio 的 Material3 排版表 + 鸿蒙字族。
 *
 * `glasmioTypography(fontFamily)` 会把字族写进每个 TextStyle，字号/行高仍由 Glasmio 的
 * Dynamic Type 表决定（LargeTitle 34/41、Body 17/22、Subheadline 15/20 …）。
 *
 * 任务 0 的调整：**正文类档位（body* / label*）由 Glasmio 默认的 Normal 改成 Medium(500)**。
 * 大标题（34sp Semibold）、title* 档位不动 —— 它们本来就没问题。
 *
 * 顶层 val：Typography 是不可变的，但对象创建不便宜，别在每帧/每个玻璃表面里重建。
 */
private val BaseGlasmioTypography: Typography =
    glasmioTypography(fontFamily = HarmonyFontFamily)

val HarmonyGlasmioTypography: Typography = BaseGlasmioTypography.copy(
    bodyLarge = BaseGlasmioTypography.bodyLarge.copy(fontWeight = FontWeight.Medium),
    bodyMedium = BaseGlasmioTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
    bodySmall = BaseGlasmioTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
    labelLarge = BaseGlasmioTypography.labelLarge.copy(fontWeight = FontWeight.Medium),
    labelMedium = BaseGlasmioTypography.labelMedium.copy(fontWeight = FontWeight.Medium),
    labelSmall = BaseGlasmioTypography.labelSmall.copy(fontWeight = FontWeight.Medium),
)
