package com.huanchengfly.tieba.post.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionSheetItem
import com.huanchengfly.tieba.post.ui.widgets.compose.AppActionSheet
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioActionSheetContent
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonLayout
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioBottomSheet
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioCard
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.GlasmioListValuePill
import io.github.neoruaa.glasmio.ui.GlasmioMenu
import io.github.neoruaa.glasmio.ui.GlasmioMenuItem
import io.github.neoruaa.glasmio.ui.GlasmioSectionTitle
import io.github.neoruaa.glasmio.ui.GlasmioSlider
import io.github.neoruaa.glasmio.ui.GlasmioStepper
import io.github.neoruaa.glasmio.ui.GlasmioSwitch
import io.github.neoruaa.glasmio.ui.GlasmioTextField

/**
 * P2/C：Glasmio 控件总览（**仅 debug 源集**）。
 *
 * 目的：一屏把 `glasmio-ui` 的主要控件都摆出来、**真实可交互**，用来对截图/验收「动效 + 控件覆盖」。
 *
 * 结构照 Glasmio sample 的规矩：
 * - `sampledContent` = 彩色渐变底（玻璃有东西可采样，能看出折射/晕染）；
 * - `overlayContent` = 控件列表（玻璃控件必须画在录制层之外，否则会自己采样自己）。
 *
 * 启动：
 * ```
 * adb shell am start -n com.tieba/com.huanchengfly.tieba.post.debug.GlassGalleryActivity
 * ```
 */
class GlassGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TiebaLiteTheme {
                GlassGallery()
            }
        }
    }
}

@Composable
private fun GlassGallery() {
    GlasmioTheme(isDark = false) {
        GlasmioBackdropHost(
            modifier = Modifier.fillMaxSize(),
            sampledContent = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF7F5AF0),
                                    Color(0xFF2CB67D),
                                    Color(0xFFFF8906),
                                    Color(0xFF00A8E8),
                                )
                            )
                        )
                )
            },
            overlayContent = {
                GalleryContent()
            },
        )
    }
}

@Composable
private fun GalleryContent() {
    val colors = GlasmioTheme.colors
    var switch by remember { mutableStateOf(true) }
    var slider by remember { mutableFloatStateOf(0.45f) }
    var stepper by remember { mutableIntStateOf(3) }
    var text by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var sheetOpen by remember { mutableStateOf(false) }
    var actionSheetOpen by remember { mutableStateOf(false) }
    var alertOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(48.dp))
            Text("Glasmio 控件总览", color = colors.content, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }

        // ── Buttons ─────────────────────────────────────────────
        item {
            Label("GlasmioButton · Regular / Prominent / Destructive / Large")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlasmioButton(onClick = {}) { Text("Regular") }
                GlasmioButton(onClick = {}, emphasis = GlassEmphasis.Prominent) { Text("Prominent") }
                GlasmioButton(onClick = {}, destructive = true) { Text("Delete") }
            }
            Spacer(Modifier.height(8.dp))
            GlasmioButton(onClick = {}, size = GlasmioButtonSize.Large) { Text("Large button") }
        }
        item {
            Label("GlasmioIconButton")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlasmioIconButton(onClick = {}) { SfIcon("heart", null, tint = GlasmioTheme.colors.content) }
                GlasmioIconButton(onClick = {}, size = 44.dp) {
                    SfIcon("square.and.arrow.up", null, tint = GlasmioTheme.colors.content)
                }
            }
        }

        // ── Controls ────────────────────────────────────────────
        item {
            Label("GlasmioSwitch / GlasmioSlider / GlasmioStepper")
            GlasmioGroupedList {
                GlasmioListRow(
                    title = "GlasmioSwitch",
                    showTopSeparator = false,
                    trailing = { GlasmioSwitch(checked = switch, onCheckedChange = { switch = it }) },
                )
                GlasmioListRow(
                    title = "GlasmioSlider",
                    showTopSeparator = true,
                    trailing = {
                        GlasmioSlider(
                            value = slider,
                            onValueChange = { slider = it },
                            modifier = Modifier.size(width = 140.dp, height = 32.dp),
                        )
                    },
                )
                GlasmioListRow(
                    title = "GlasmioStepper",
                    showTopSeparator = true,
                    trailing = { GlasmioStepper(value = stepper, onValueChange = { stepper = it }, range = 0..10) },
                )
                GlasmioListRow(
                    title = "GlasmioListValuePill",
                    showTopSeparator = true,
                    trailing = { GlasmioListValuePill(text = "$stepper", onClick = {}) },
                )
            }
        }
        item {
            Label("GlasmioTextField")
            GlasmioTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "输入点什么…",
            )
        }
        item {
            Label("GlasmioSectionTitle / GlasmioListRow / GlasmioGroupedList")
            GlasmioSectionTitle("分组标题")
            GlasmioGroupedList {
                GlasmioListRow("带图标的一行", systemName = "gear", showTopSeparator = false, onClick = {})
                GlasmioListRow("带副标题", subtitle = "subtitle", onClick = {})
                GlasmioListRow("带详情", detail = "详情", onClick = {})
            }
        }

        // ── Card ────────────────────────────────────────────────
        item {
            Label("GlasmioCard")
            GlasmioCard {
                Column(Modifier.padding(16.dp)) {
                    Text("GlasmioCard", color = GlasmioTheme.colors.content, fontSize = 17.sp)
                    Text("26dp 连续圆角的实色卡片", color = GlasmioTheme.colors.secondaryContent, fontSize = 13.sp)
                }
            }
        }

        // ── Overlays ────────────────────────────────────────────
        item {
            Label("GlasmioMenu / GlasmioAlertDialog / GlasmioBottomSheet / ActionSheet")
            GlasmioMenu(
                expanded = menuOpen,
                onExpandedChange = { menuOpen = it },
                itemCount = 3,
                backdrop = io.github.neoruaa.glasmio.glass.LocalGlassBackdrop.current,
                anchor = { onClick ->
                    GlasmioButton(onClick = onClick) { Text("打开 GlasmioMenu") }
                },
            ) { backdrop, close ->
                GlasmioMenuItem("收藏", onClick = { close() }, systemName = "bookmark", backdrop = backdrop)
                GlasmioMenuItem("复制链接", onClick = { close() }, systemName = "doc.on.doc", backdrop = backdrop)
                GlasmioMenuItem("不感兴趣", onClick = { close() }, systemName = "eye.slash", backdrop = backdrop)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlasmioButton(onClick = { alertOpen = true }) { Text("AlertDialog") }
                GlasmioButton(onClick = { sheetOpen = true }) { Text("BottomSheet") }
                GlasmioButton(onClick = { actionSheetOpen = true }) { Text("ActionSheet") }
            }
        }

        item {
            Text(
                "❌ 本工程未依赖 glasmio-scaffold：GlasmioSegmentedControl / GlasmioTabBar / GlasmioNavigationBar 不在 glasmio-ui 里",
                color = colors.secondaryContent,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }

    if (alertOpen) {
        GlasmioAlertDialog(
            onDismissRequest = { alertOpen = false },
            title = "GlasmioAlertDialog",
            message = "iOS 风格的玻璃告警，按钮可并排/堆叠。",
            buttonLayout = GlasmioAlertButtonLayout.SideBySide,
            buttons = listOf(
                GlasmioAlertButtonSpec(label = "取消", role = GlasmioAlertButtonRole.Cancel, onClick = { alertOpen = false }),
                GlasmioAlertButtonSpec(label = "确定", onClick = { alertOpen = false }),
            ),
        )
    }
    if (sheetOpen) {
        GlasmioBottomSheet(
            visible = true,
            onDismissRequest = { sheetOpen = false },
            title = "GlasmioBottomSheet",
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("全屏 modal sheet：可下拉关闭", color = GlasmioTheme.colors.content)
                GlasmioButton(onClick = { sheetOpen = false }) { Text("关闭") }
            }
        }
    }
    // AppActionSheet（B 的产出）也放一个入口，直接对比 GlasmioActionSheetContent 的组合
    AppActionSheet(
        visible = actionSheetOpen,
        onDismiss = { actionSheetOpen = false },
        title = "AppActionSheet",
        message = "底部升起 · 拖拽关闭 · 玻璃",
        items = listOf(
            ActionSheetItem("分享", systemName = "square.and.arrow.up", onClick = {}),
            ActionSheetItem("删除", systemName = "trash", destructive = true, onClick = {}),
        ),
    )
    // 没有 host 时的对照：GlasmioActionSheetContent 直接嵌在页面上
    if (false) {
        GlasmioActionSheetContent(title = "unused") {}
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
    )
}
