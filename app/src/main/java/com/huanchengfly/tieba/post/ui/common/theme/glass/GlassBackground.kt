package com.huanchengfly.tieba.post.ui.common.theme.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.fetch.newFileUri
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.utils.ThemeUtil

/**
 * 主页面被采样的背景（垂直渐变 + 可选的「半透明主题」背景图）。
 *
 * 为什么要有这一层：玻璃只能折射**录制层里**的像素。App 的渐变底画在 Activity 根节点上，
 * 在 `GlasmioBackdropHost` 的 sampledContent 外面，录不进去；不把背景放进 sampledContent 的话，
 * 胶囊采到的就是一堆透明像素，看起来像没有折射。
 *
 * 与 `MainActivityV2.TranslucentThemeBackground` 是同一套视觉（同一个 composable），
 * 保证「半透明主题」用户在主页面看到的背景和别处一致。
 */
@Composable
fun GlassAppBackground(modifier: Modifier = Modifier) {
    val glass = glassColors()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(glass.gradientTop, glass.gradientBottom)
                )
            )
    ) {
        if (ThemeUtil.isTranslucentTheme(ExtendedTheme.colors.theme)) {
            val backgroundPath by rememberPreferenceAsMutableState(
                key = stringPreferencesKey("translucent_theme_background_path"),
                defaultValue = ""
            )
            val backgroundUri by remember { derivedStateOf { newFileUri(backgroundPath) } }
            AsyncImage(
                imageUri = backgroundUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
