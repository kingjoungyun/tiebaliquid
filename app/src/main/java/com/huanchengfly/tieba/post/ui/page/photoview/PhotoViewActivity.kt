package com.huanchengfly.tieba.post.ui.page.photoview

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.panpf.sketch.compose.rememberAsyncImageState
import com.github.panpf.sketch.request.LoadState
import com.github.panpf.zoomimage.SketchZoomAsyncImage
import com.stoyanvuchev.systemuibarstweaker.SystemUIBarsTweaker
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivityWithParcelable
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.download
import kotlin.math.roundToInt
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.glass.GlassSurface

@Composable
private fun ViewPhoto(
    imageUri: String,
    modifier: Modifier = Modifier,
    onTap: (offset: Offset) -> Unit = {},
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val state = rememberAsyncImageState()
        val progress by remember {
            derivedStateOf {
                (state.progress?.completedLength?.toFloat() ?: 0f) / (state.progress?.totalLength
                    ?: 1)
            }
        }
        val showProgress by remember {
            derivedStateOf {
                state.loadState is LoadState.Started
            }
        }
        SketchZoomAsyncImage(
            imageUri = imageUri,
            contentDescription = stringResource(id = R.string.desc_image),
            modifier = Modifier.fillMaxSize(),
            onTap = onTap,
            imageState = state,
        )
        if (showProgress) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    color = AppTokens.accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    color = AppTokens.accent,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 底部悬浮玻璃操作条。
 *
 * 这是独立 Activity，**不做整页 backdrop 录制**：这里只给操作条本身自带一个 56dp 的
 * 纯色 host（`GlasmioBackdropHost(sampledColor = AppTokens.card)`），玻璃采的是这层纯色；
 * 操作条画在 host 的 overlay 层，不在录制层里，因此不会自己采样自己。
 * 其余参数（材质 / 明暗 / 折射）全部用 Glasmio 默认值。
 */
@Composable
private fun PhotoViewActionBar(
    modifier: Modifier = Modifier,
    indexText: String?,
    onShare: () -> Unit,
    onSave: () -> Unit,
) {
    GlasmioBackdropHost(
        sampledColor = AppTokens.card,
        modifier = modifier.height(56.dp),
    ) {
        LiquidGlassTheme(accent = AppTokens.primary) {
            val colors = GlasmioTheme.colors
            GlassSurface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(28.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = indexText.orEmpty(),
                        fontSize = 14.sp,
                        color = colors.secondaryContent,
                        modifier = Modifier.weight(1f),
                    )
                    PhotoViewAction(
                        icon = PhosphorIcons["export"] ?: Icons.Rounded.Share,
                        contentDescription = stringResource(id = R.string.title_share_pic),
                        tint = colors.content,
                        onClick = onShare,
                    )
                    PhotoViewAction(
                        icon = PhosphorIcons["download-simple"] ?: Icons.Rounded.Download,
                        contentDescription = stringResource(id = R.string.desc_download_pic),
                        tint = colors.content,
                        onClick = onSave,
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoViewAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

class PhotoViewActivity : BaseComposeActivityWithParcelable<PhotoViewData>() {
    private val viewModel: PhotoViewViewModel by viewModels()

    override val dataExtraKey: String = EXTRA_PHOTO_VIEW_DATA

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content(data: PhotoViewData) {
        LazyLoad(loaded = viewModel.initialized) {
            viewModel.send(PhotoViewUiIntent.Init(data))
            viewModel.initialized = true
        }
        val uiState by viewModel.uiState.collectAsState()
        val items by remember { derivedStateOf { uiState.data } }
        val initialIndex by remember { derivedStateOf { uiState.initialIndex } }
        val totalAmount by remember { derivedStateOf { uiState.totalAmount } }
        val hasPrev by remember { derivedStateOf { uiState.hasPrev } }
        val hasNext by remember { derivedStateOf { uiState.hasNext } }
        val loadPicPageData by remember { derivedStateOf { uiState.loadPicPageData } }
        val loaded by remember { derivedStateOf { uiState.data.isNotEmpty() } }

        Surface(color = Color.Black) {
            if (loaded) {
                val pageCount by remember { derivedStateOf { items.size } }

                val pagerState = rememberPagerState(initialPage = initialIndex) { pageCount }

                LaunchedEffect(initialIndex) {
                    if (pagerState.currentPage != initialIndex) pagerState.scrollToPage(initialIndex)
                }

                LaunchedEffect(pagerState.currentPage, pageCount, loadPicPageData) {
                    loadPicPageData?.let {
                        val item = items[pagerState.currentPage]
                        if (pagerState.currentPage == 0 && hasPrev) {
                            viewModel.send(
                                PhotoViewUiIntent.LoadPrev(
                                    item.picId,
                                    item.overallIndex,
                                    it
                                )
                            )
                        } else if (pagerState.currentPage == pageCount - 1 && hasNext) {
                            viewModel.send(
                                PhotoViewUiIntent.LoadMore(
                                    item.picId,
                                    item.overallIndex,
                                    it
                                )
                            )
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        key = { "${items[it].picId}_${items[it].postId}" }
                    ) {
                        val item = items[it]
                        ViewPhoto(
                            imageUri = item.originUrl,
                            modifier = Modifier.fillMaxSize(),
                            onTap = {
                                finish()
                            },
                        )
                    }

                    // 悬浮返回圆钮：独立 Activity 没有页级 backdrop，GlassIconButton 自带纯色小 host。
                    // 全屏看图恒为黑底，所以给自带的 host 一个纯黑 fill：host 的方形采样层与背景融在一起，
                    // 圆钮本身仍是 Glasmio 玻璃；图标固定白色，避免浅色主题下变成「白方块 + 深色箭头」。
                    GlassIconButton(
                        onClick = { finish() },
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(id = R.string.button_back),
                        tint = Color.White,
                        navigationSurfaceColor = Color.Black,
                        // 全屏看图恒为黑底：采样底也要给黑，否则圆钮底下是一圈浅色盘。
                        hostFill = Color.Black,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            // insets：这里属于【顶栏/悬浮控件自身】，勿改（见 docs/INSETS-WHITELIST.md）
                            .statusBarsPadding()
                            .padding(start = 12.dp, top = 6.dp),
                    )

                    val index = pagerState.currentPage
                    PhotoViewActionBar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        indexText = if (totalAmount > 1) {
                            "${items[index].overallIndex} / $totalAmount"
                        } else {
                            null
                        },
                        onShare = {
                            toastShort(R.string.toast_preparing_share_pic)
                            ImageUtil.download(
                                this@PhotoViewActivity,
                                items[index].originUrl,
                                true
                            ) { uri: Uri ->
                                val chooser = Intent(Intent.ACTION_SEND).apply {
                                    type = Intent.normalizeMimeType("image/jpeg")
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    } else {
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                    }
                                }.let {
                                    Intent.createChooser(
                                        it,
                                        getString(R.string.title_share_pic)
                                    )
                                }
                                runCatching {
                                    startActivity(chooser)
                                }
                            }
                        },
                        onSave = {
                            ImageUtil.download(
                                this@PhotoViewActivity,
                                items[index].originUrl
                            )
                        },
                    )
                }
            }
        }
    }

    override fun onCreateContent(systemUIBarsTweaker: SystemUIBarsTweaker) {
        systemUIBarsTweaker.tweakStatusBarVisibility(false)
        systemUIBarsTweaker.tweakNavigationBarVisibility(false)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return try {
            super.dispatchTouchEvent(ev)
        } catch (e: RuntimeException) {
            e.printStackTrace()
            true
        }
    }

    companion object {
        const val EXTRA_PHOTO_VIEW_DATA = "photo_view_data"
    }
}