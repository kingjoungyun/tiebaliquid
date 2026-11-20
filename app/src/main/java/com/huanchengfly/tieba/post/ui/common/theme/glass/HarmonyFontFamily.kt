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
 * 字重映射：400 → Regular、500 → Medium、600/700 → Semibold。
 * Glasmio 的排版表会请求 400（常规）/ 590（强调）/ 700（大标题），Compose 按「就近且不轻于
 * 请求值」匹配：590 落到 600，700 直接命中 700 的 Semibold 槽。
 */
val HarmonyFontFamily: FontFamily = FontFamily(
    Font(R.font.harmony_sans_sc_regular, FontWeight.Normal),
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
 * 顶层 val：Typography 是不可变的，但对象创建不便宜，别在每帧/每个玻璃表面里重建。
 */
val HarmonyGlasmioTypography: Typography = glasmioTypography(fontFamily = HarmonyFontFamily)
