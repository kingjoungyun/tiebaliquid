package com.huanchengfly.tieba.post.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlassSurface as GlasmioGlassSurface
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.glass.NavigationGlassMaterial

/** 只用来在装机后截一张「玻璃是否真的折射了背后内容」的图。 */
class GlassPlaygroundActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TiebaLiteTheme {
                GlassPlayground()
            }
        }
    }
}

/**
 * 最小验证 Demo：
 * - sampledContent：四条高饱和色带 + 大字号文字（折射/模糊会把它们扭曲、糊开，一眼能看出来）
 * - overlayContent：GlassSurface(RegularSmall, CircleShape)
 */
@androidx.compose.runtime.Composable
fun GlassPlayground() {
    GlasmioTheme(isDark = false) {
        GlasmioBackdropHost(
            modifier = Modifier.fillMaxSize(),
            sampledContent = {
                Column(Modifier.fillMaxSize()) {
                    listOf(
                        Color(0xFFFF3B30),
                        Color(0xFFFFCC00),
                        Color(0xFF34C759),
                        Color(0xFF0A84FF),
                        Color(0xFFAF52DE),
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(color),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "GLASS 0123456789",
                                color = Color.White,
                                fontSize = 26.sp,
                            )
                        }
                    }
                }
            },
            overlayContent = {
                Box(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        GlasmioGlassSurface(
                            modifier = Modifier
                                .size(280.dp, 96.dp),
                            shape = CircleShape,
                            material = NavigationGlassMaterial.RegularSmall,
                        ) {
                            Text("RegularSmall", color = Color.Black, fontSize = 20.sp)
                        }
                        GlasmioGlassSurface(
                            modifier = Modifier
                                .size(280.dp, 96.dp),
                            shape = CircleShape,
                            material = NavigationGlassMaterial.RegularLarge,
                        ) {
                            Text("RegularLarge", color = Color.Black, fontSize = 20.sp)
                        }
                        GlasmioGlassSurface(
                            modifier = Modifier
                                .size(280.dp, 96.dp),
                            shape = CircleShape,
                            material = NavigationGlassMaterial.Clear,
                        ) {
                            Text("Clear", color = Color.Black, fontSize = 20.sp)
                        }
                    }
                }
            },
        )
    }
}
