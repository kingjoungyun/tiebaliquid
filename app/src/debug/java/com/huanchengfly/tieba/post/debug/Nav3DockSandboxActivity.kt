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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavEntry
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassAppBackground
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassBackdropHost
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.scaffold.navigation.GlasmioNavDisplay
import io.github.neoruaa.glasmio.scaffold.navigation.GlasmioNavHeaderMotion
import io.github.neoruaa.glasmio.scaffold.GlasmioNavigationOperation
import io.github.neoruaa.glasmio.ui.CompactBottomControlSize
import io.github.neoruaa.glasmio.ui.GlasmioNavigationBar
import io.github.neoruaa.glasmio.ui.GlasmioTabItem

/**
 * Stage 1 · Nav3 三屏 spike 的**隔离沙盒**（仅 debug）。
 *
 * 目的：只回答一个问题 —— **底栏挂到 NavDisplay 上之后，快速 0↔3 切换，镜头动画还会不会停帧。**
 * 所以这里刻意排除所有其它变量：
 * - 4 个假页面（纯色 + 大号文字），没有 Hilt ViewModel、没有 compose-destinations、没有深链；
 * - `compactProgress` 恒为 0（不接滚动 → 永远是展开态，排除收起/展开状态机）；
 * - 两个底栏上下叠放，**同一个 backdrop host、同一套 items、同一个 compactProgress**，
 *   唯一差异是 selectedKey 的来源：
 *   · A（上）：Nav3 驱动 —— selectedKey = backStack.last()，onSelected 只做 back stack 替换
 *   · B（下）：手动 state 驱动 —— 生产现在的接法（mutableIntStateOf + 手动改）
 *
 * 启动：
 *   adb shell am start -n com.tieba/com.huanchengfly.tieba.post.debug.Nav3DockSandboxActivity
 */
class Nav3DockSandboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Nav3DockSandbox()
            }
        }
    }
}

private val SandboxLabels = listOf("首页", "进吧", "消息", "我")
private val SandboxSymbols = listOf(
    SfSymbol.House,
    SfSymbol.Layers,
    SfSymbol.Heart,
    SfSymbol.InfoCircle,
)
private val SandboxPageColors = listOf(
    Color(0xFF3B82F6),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFF8B5CF6),
)

@Composable
private fun Nav3DockSandbox() {
    // A：Nav3 的 back stack（单向数据源：导航 → UI）
    val backStack = remember { mutableStateListOf(0) }
    // B：生产同款的手动 state
    var manualIndex by remember { mutableIntStateOf(0) }

    val selectedFromStack = backStack.lastOrNull() ?: 0

    // 两个底栏共用同一份 items / compactProgress（常量 0：永远展开态）
    val items = remember {
        SandboxLabels.mapIndexed { index, label ->
            GlasmioTabItem(
                key = index,
                label = label,
                symbol = SandboxSymbols[index],
                contentDescription = label,
            )
        }
    }
    val compactProgress = remember { mutableStateOf(0f) }

    LiquidGlassBackdropHost(
        modifier = Modifier.fillMaxSize(),
        sampledContent = {
            // 页面区：渐变底 + 色块（给玻璃东西可采），上面盖 NavDisplay 的假页面
            GlassAppBackground(modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    listOf(0xFFEF4444, 0xFF22C55E, 0xFF06B6D4, 0xFFEAB308).forEachIndexed { i, c ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(200.dp)
                                .background(Color(c), CircleShape)
                        )
                    }
                }
                val headerMotion = remember {
                    GlasmioNavHeaderMotion(backStack.last()) { _, _, _ ->
                        GlasmioNavigationOperation.Tab
                    }
                }
                GlasmioNavDisplay(
                    backStack = backStack,
                    motion = headerMotion,
                    modifier = Modifier.fillMaxSize(),
                    onBack = {},
                    entryProvider = { key ->
                        NavEntry(key = key, contentKey = key) {
                            SandboxPage(key)
                        }
                    },
                )
            }
        },
        overlayContent = {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── A：Nav3 驱动 ───────────────────────────────────────────────
                DockLabel("A · Nav3（selectedKey = backStack.last()）", Color(0xFF22C55E))
                LiquidGlassTheme {
                    GlasmioNavigationBar(
                        items = items,
                        selectedKey = selectedFromStack,
                        onSelected = { index ->
                            // 单向：只替换 back stack，不持有第二份状态
                            if (index != selectedFromStack) {
                                backStack.clear()
                                backStack.add(index)
                            }
                        },
                        onExpand = {},
                        compactProgress = compactProgress,
                        compactSize = CompactBottomControlSize,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── B：手动 state 驱动（生产现在的接法）──────────────────────────
                DockLabel("B · 手动 state（生产接法）", Color(0xFFF59E0B))
                LiquidGlassTheme {
                    GlasmioNavigationBar(
                        items = items,
                        selectedKey = manualIndex,
                        onSelected = { index -> manualIndex = index },
                        onExpand = {},
                        compactProgress = compactProgress,
                        compactSize = CompactBottomControlSize,
                    )
                }
            }
        },
    )
}

@Composable
private fun DockLabel(text: String, color: Color) {
    Box(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .background(color.copy(alpha = 0.85f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SandboxPage(index: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 300.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(SandboxPageColors[index].copy(alpha = 0.85f), CircleShape)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = SandboxLabels[index],
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = 0.78f),
            )
            Text(
                text = "route #$index",
                fontSize = 16.sp,
                color = Color.Black.copy(alpha = 0.5f),
            )
        }
    }
}
