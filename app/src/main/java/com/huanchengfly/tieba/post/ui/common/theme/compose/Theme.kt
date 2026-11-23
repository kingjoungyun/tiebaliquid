package com.huanchengfly.tieba.post.ui.common.theme.compose

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.material.Colors
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.compose.darken
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

@SuppressLint("ConflictingOnColor")
fun getColorPalette(darkTheme: Boolean): Colors {
    val primaryColor = AppTokens.accent
    val secondaryColor = AppTokens.primary
    return if (darkTheme) {
        darkColors(
            primary = primaryColor,
            primaryVariant = primaryColor.darken(),
            secondary = secondaryColor,
            secondaryVariant = Color(0xFF3F310A),
            onSecondary = Color(0xFFFFFFFF),
            background = AppTokens.background,
            onBackground = AppTokens.text,
            surface = AppTokens.card,
        )
    } else {
        lightColors(
            primary = primaryColor,
            primaryVariant = primaryColor.darken(),
            secondary = secondaryColor,
            secondaryVariant = Color(0xFF000000),
            onSecondary = Color(0xFFFFFFFF),
            background = AppTokens.background,
            onBackground = AppTokens.text,
            surface = AppTokens.card,
        )
    }
}

@Composable
fun TiebaLiteTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val theme by remember { ThemeUtil.themeState }
    val isDarkColorPalette by remember {
        derivedStateOf {
            ThemeUtil.isNightMode(theme)
                    || (ThemeUtil.isTranslucentTheme(theme) && theme.contains("light"))
        }
    }

    // Stage 6 commit 2：AppTokens 是唯一的取色入口（XML 主题属性 → AppTokens，直接填充）。
    // 动态取色已关闭，这里不再有 dynamic 分支。
    AppTokens.fillFromTheme(context, theme)

    val colors = getColorPalette(isDarkColorPalette)

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
