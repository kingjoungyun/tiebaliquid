package com.huanchengfly.tieba.post.ui.page.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.BuildConfig
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.launchUrl
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsBackButtonOverlay
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsGlasmioDefaults
import com.huanchengfly.tieba.post.ui.common.theme.glass.HarmonyFontFamily
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

@Destination
@Composable
fun AboutPage(
    navigator: DestinationsNavigator,
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var clickCount by remember { mutableIntStateOf(0) }

    // 二级页范式：不再有 M2 顶栏，页面从状态栏下方起排，返回是左上角悬浮的玻璃圆钮
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 悬浮返回钮留位 + 大标题（与其它设置页同一套二级页范式）
            Spacer(modifier = Modifier.height(PrefsGlasmioDefaults.BackButtonTopSpace))
            Text(
                text = stringResource(id = R.string.title_about),
                fontSize = 34.sp,
                lineHeight = 41.sp,
                fontWeight = FontWeight(700),
                fontFamily = HarmonyFontFamily,
                color = AppTokens.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.align(Alignment.Center)) {
                    Image(
                        painter = rememberDrawablePainter(
                            drawable = LocalContext.current.getDrawable(
                                R.mipmap.ic_launcher_new
                            )
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(100.dp)
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                val context = LocalContext.current
                Text(
                    text = stringResource(id = R.string.title_thanks),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = AppTokens.text,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(id = R.string.tip_thanks),
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Center,
                    color = AppTokens.textSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                ThanksLink(text = stringResource(id = R.string.thanks_tieba_lite)) {
                    launchUrl(context, navigator, "https://github.com/zzc10086/TiebaLite")
                }
                Spacer(modifier = Modifier.height(8.dp))
                ThanksLink(text = stringResource(id = R.string.thanks_flux_tie)) {
                    launchUrl(context, navigator, "https://github.com/makise2060/FluxTie")
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 字体声明：HarmonyOS Sans 允许免费商用，但要求在使用处署名。
                Text(
                    text = stringResource(id = R.string.thanks_harmony_sans),
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Center,
                    color = AppTokens.textSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.tip_about, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.caption,
                    color = AppTokens.textSecondary,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastClickTime < 500) {
                                clickCount++
                            } else {
                                clickCount = 1
                            }
                            lastClickTime = currentTime
                            if (clickCount >= 7) {
                                clickCount = 0
                                context.appPreferences.showExperimentalFeatures =
                                    !context.appPreferences.showExperimentalFeatures
                                if (context.appPreferences.showExperimentalFeatures) {
                                    context.toastShort(R.string.toast_experimental_features_enabled)
                                } else {
                                    context.toastShort(R.string.toast_experimental_features_disabled)
                                }
                            }
                        }
                    )
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
        PrefsBackButtonOverlay(onBack = { navigator.navigateUp() })
    }
}

@Composable
private fun ThanksLink(
    text: String,
    onClick: () -> Unit,
) {
    TextButton(
        shape = RoundedCornerShape(100),
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = AppTokens.text.copy(
                alpha = 0.1f
            ),
            contentColor = AppTokens.text
        ),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}
