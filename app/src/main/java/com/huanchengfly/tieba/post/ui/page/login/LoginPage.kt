package com.huanchengfly.tieba.post.ui.page.login

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassPageHost
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.ui.page.webview.MyWebChromeClient
import com.huanchengfly.tieba.post.ui.page.webview.MyWebViewClient
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadingState
import com.huanchengfly.tieba.post.ui.widgets.compose.WebView
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSaveableWebViewState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberWebViewNavigator
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AccountUtil.parseCookie
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.common.components.glass.GlassSnackbarHost

const val LOGIN_URL =
    "https://wappass.baidu.com/passport?login&u=https%3A%2F%2Ftieba.baidu.com%2Findex%2Ftbwise%2Fmine"

@SuppressLint("SetJavaScriptEnabled")
@Destination
@Composable
fun LoginPage(
    navigator: DestinationsNavigator,
) {
        // P1：页级 backdrop host —— 本页的菜单/动作面板要能采到这一页的真实内容。
        // 页面内容进 sampledContent（活 backdrop，整页录制）；页内玻璃控件会自动走自带纯色 host。
        GlassPageHost(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val webViewState = rememberSaveableWebViewState()
            val webViewNavigator = rememberWebViewNavigator()
            var loaded by rememberSaveable {
                mutableStateOf(false)
            }

            LazyLoad(loaded = loaded) {
                webViewNavigator.loadUrl(LOGIN_URL)
                loaded = true
            }
    
            val isLoading by remember {
                derivedStateOf {
                    webViewState.loadingState is LoadingState.Loading
                }
            }
    
            val progress by remember {
                derivedStateOf {
                    webViewState.loadingState.let {
                        if (it is LoadingState.Loading) {
                            it.progress
                        } else {
                            0f
                        }
                    }
                }
            }
    
            val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    
            // T5/B4a：外壳从 MyScaffold 换成普通 Box（原来它只提供 insets + snackbar host）。
            // 登录页自己画顶栏（返回/刷新 + 34sp 大标题），snackbar 用 GlassSnackbarHost 挂底部。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTokens.background)
            ) {
                val snackbarHostState = remember { SnackbarHostState() }
                Column(modifier = Modifier.fillMaxSize()) {
                    // 大标题 + 悬浮返回（登录页原来是 M2 Toolbar，这里统一成二级页的大标题范式）
                    Column(modifier = Modifier.statusBarsPadding()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(horizontal = 8.dp)
                        ) {
                            GlassIconButton(
                                onClick = { navigator.navigateUp() },
                                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(id = R.string.button_back),
                                modifier = Modifier.align(Alignment.CenterStart),
                            )
                            GlassIconButton(
                                onClick = { webViewNavigator.reload() },
                                icon = PhosphorIcons["arrow-clockwise"] ?: Icons.Rounded.Refresh,
                                contentDescription = stringResource(id = R.string.title_refresh),
                                modifier = Modifier.align(Alignment.CenterEnd),
                            )
                        }
                        Text(
                            text = "登录",
                            fontSize = 34.sp,
                            lineHeight = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTokens.text,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color.White)
                    ) {
                        WebView(
                            state = webViewState,
                            modifier = Modifier.fillMaxSize(),
                            navigator = webViewNavigator,
                            onCreated = {
                                it.settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                }
                            },
                            client = remember(navigator) {
                                LoginWebViewClient(
                                    navigator,
                                    coroutineScope,
                                    snackbarHostState
                                )
                            },
                            chromeClient = remember { MyWebChromeClient(context, coroutineScope) }
                        )

                        if (isLoading) {
                            LinearProgressIndicator(
                                progress = animatedProgress,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                GlassSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
}

class LoginWebViewClient(
    nativeNavigator: DestinationsNavigator? = null,
    val coroutineScope: CoroutineScope,
    val snackbarHostState: SnackbarHostState,
) : MyWebViewClient(nativeNavigator) {
    private var isLoadingAccount = false

    override fun injectCookies(url: String) {}

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        if (url == null) {
            return
        }
        if (isLoadingAccount) {
            return
        }
        val cookieStr = CookieManager.getInstance().getCookie(url) ?: return
        val cookies = parseCookie(cookieStr).mapKeys { it.key.uppercase() }
        val bduss = cookies["BDUSS"]
        val sToken = cookies["STOKEN"]
        val baiduId = cookies["BAIDUID"]
        if (url.startsWith("https://tieba.baidu.com/index/tbwise/") || url.startsWith("https://tiebac.baidu.com/index/tbwise/")) {
            if (bduss == null || sToken == null) {
                return
            }
            if (!baiduId.isNullOrEmpty() && ClientUtils.baiduId.isNullOrEmpty()) {
                coroutineScope.launch {
                    ClientUtils.saveBaiduId(context, baiduId)
                }
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.text_please_wait),
                    duration = SnackbarDuration.Indefinite
                )
            }
            coroutineScope.launch {
                AccountUtil.fetchAccountFlow(bduss, sToken, cookieStr)
                    .catch {
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                context.getString(
                                    R.string.text_login_failed,
                                    it.getErrorMessage()
                                ), duration = SnackbarDuration.Short
                            )
                        }
                        navigator.loadUrl(LOGIN_URL)
                        isLoadingAccount = false
                    }
                    .flowOn(Dispatchers.Main)
                    .collect { account ->
                        isLoadingAccount = false
                        AccountUtil.newAccount(account.uid, account) {
                            if (it >= 0) {
                                AccountUtil.switchAccount(context, it.toInt())
                                coroutineScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.text_login_success),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                coroutineScope.launch {
                                    delay(1500L)
                                    nativeNavigator?.navigateUp()
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.text_database_failed_default),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                view.loadUrl(LOGIN_URL)
                            }
                        }
                    }
            }
        }
    }
}
