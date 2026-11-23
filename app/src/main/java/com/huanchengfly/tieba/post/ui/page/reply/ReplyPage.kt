package com.huanchengfly.tieba.post.ui.page.reply

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.InsertPhoto
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.addTextChangedListener
import com.github.panpf.sketch.compose.AsyncImage
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.database.Draft
import com.huanchengfly.tieba.post.pxToDpFloat
import com.huanchengfly.tieba.post.toMD5
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.PrefsSwitch
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.common.theme.glass.HarmonyTextProvider
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.glassColors
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.glass.GlassSurface
import com.huanchengfly.tieba.post.ui.page.destinations.ReplyPageDestination
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.EMOJI
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.IMAGE
import com.huanchengfly.tieba.post.ui.page.reply.ReplyPanelType.NONE
import com.huanchengfly.tieba.post.ui.utils.imeNestedScroll
import com.huanchengfly.tieba.post.ui.widgets.compose.BaseDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.BaseTextField
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyBackHandler
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.edittext.widget.UndoableEditText
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.DatabaseUtil
import com.huanchengfly.tieba.post.utils.Emoticon
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.PickMediasRequest
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.hideKeyboard
import com.huanchengfly.tieba.post.utils.showKeyboard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyleBottomSheet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens

data class ReplyArgs(
    val forumId: Long,
    val forumName: String,
    val threadId: Long,
    val postId: Long? = null,
    val subPostId: Long? = null,
    val replyUserId: Long? = null,
    val replyUserName: String? = null,
    val replyUserPortrait: String? = null,
    val tbs: String? = null,
)

@Composable
fun ReplyDialog(
    args: ReplyArgs,
    state: DialogState = rememberDialogState(),
) {
    BaseDialog(
        dialogState = state,
        imePadding = false,
    ) {
        ReplyGlassShell(cornerRadius = 26.dp) {
            ReplyPageContent(
                viewModel = pageViewModel(),
                onBack = { dismiss() },
                forumId = args.forumId,
                forumName = args.forumName,
                threadId = args.threadId,
                postId = args.postId,
                subPostId = args.subPostId,
                replyUserId = args.replyUserId,
                replyUserName = args.replyUserName,
                replyUserPortrait = args.replyUserPortrait,
                tbs = args.tbs,
                isDialog = true
            )
        }
    }
}

@OptIn(
    ExperimentalLayoutApi::class,
    FlowPreview::class
)
@Composable
internal fun ReplyPageContent(
    viewModel: ReplyViewModel,
    onBack: () -> Unit,
    forumId: Long,
    forumName: String,
    threadId: Long,
    postId: Long? = null,
    subPostId: Long? = null,
    replyUserId: Long? = null,
    replyUserName: String? = null,
    replyUserPortrait: String? = null,
    tbs: String? = null,
    isDialog: Boolean = false,
) {
    val hash = remember(forumId, threadId, postId, subPostId) {
        "${threadId}_${postId}_${subPostId}".toMD5()
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val curTbs = remember(tbs) { tbs ?: AccountUtil.getAccountInfo { this.tbs }.orEmpty() }

    val isUploading by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::isUploading,
        initial = false
    )
    val isSending by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::isSending,
        initial = false
    )
    val isReplying by remember { derivedStateOf { isUploading || isSending } }
    val replySuccess by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::replySuccess,
        initial = false
    )
    val curKeyboardType by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::replyPanelType,
        initial = NONE
    )
    val selectedImageList by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::selectedImageList,
        initial = persistentListOf()
    )
    val isOriginImage by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::isOriginImage,
        initial = false
    )

    val replyType by viewModel.uiState.collectPartialAsState(
        prop1 = ReplyUiState::replyType,
        initial = NONE
    )
    val isTopicThread = replyType == ReplyType.TOPIC_THREAD
    //threadId为0时切换为发主题帖
    if (forumId != 0L && threadId == 0L) viewModel.send(ReplyUiIntent.SwitchReplyType(ReplyType.TOPIC_THREAD))
    val keyboardController = LocalSoftwareKeyboardController.current
    var initialText by remember { mutableStateOf("") }
    var threadTitle by remember { mutableStateOf("") }
    var waitEditTextToSet by remember { mutableStateOf(false) }
    var editTextView by remember { mutableStateOf<UndoableEditText?>(null) }
    fun getText(): String {
        return editTextView?.text?.toString().orEmpty()
    }

    fun setText(text: String) {
        if (editTextView != null) {
            editTextView?.setText(StringUtil.getEmoticonContent(editTextView!!, text))
            editTextView?.setSelection(text.length)
        } else {
            initialText = text
            waitEditTextToSet = true
        }
    }

    fun insertEmoticon(text: String) {
        editTextView?.apply {
            val start = selectionStart
            this.text?.insert(start, text)
            this.setText(StringUtil.getEmoticonContent(this, this.text))
            setSelection(start + text.length)
        }
    }

    val curTextFlow = remember { MutableStateFlow("") }
    val curText by curTextFlow.collectAsState()
    LaunchedEffect(Unit) {
        val draft = DatabaseUtil.getDraft(hash)
        if (draft != null) {
            setText(draft.content)
        }
        curTextFlow
            .sample(500)
            .distinctUntilChanged()
            .collect {
                Log.i("ReplyPage", "collect: $it")
                if (!replySuccess) {
                    DatabaseUtil.saveDraft(hash, it)
                }
            }
    }
    val textLength by remember { derivedStateOf { curText.length } }
    val isTextEmpty by remember { derivedStateOf { curText.isEmpty() } }
    var topTitle = when (replyType) {
        ReplyType.TOPIC_THREAD -> context.getString(R.string.title_thread)
        else -> context.getString(R.string.title_reply)
    }
    LaunchedEffect(replyType) {
        topTitle = when (replyType) {
            ReplyType.TOPIC_THREAD -> context.getString(R.string.title_thread)
            else -> context.getString(R.string.title_reply)
        }
    }
    LaunchedEffect(replyType, editTextView) {
        editTextView?.hint = when {
            replyType == ReplyType.TOPIC_THREAD -> context.getString(R.string.tip_thread_content)
            subPostId != null && subPostId != 0L && replyUserName != null ->
                context.getString(R.string.hint_reply, replyUserName)
            else -> context.getString(R.string.tip_reply)
        }
    }
    viewModel.onEvent<ReplyUiEvent.ReplySuccess> {
        if (it.expInc.isEmpty()) {
            context.toastShort(R.string.toast_add_thread_success_default)
        } else {
            context.toastShort(R.string.toast_reply_success, it.expInc)
        }
        coroutineScope.launch {
            DatabaseUtil.deleteDraft(hash)
            onBack()
        }
    }

    val warningDialogState = rememberDialogState()
    var safeModeDialogOnOpen by remember { mutableStateOf(false) }
    var waitUploadSuccessToSend by remember { mutableStateOf(false) }
    viewModel.onEvent<ReplyUiEvent.UploadSuccess> {
        if (waitUploadSuccessToSend) {
            waitUploadSuccessToSend = false
            val imageContent = it.resultList
                .joinToString("\n") { image ->
                    "#(pic,${image.picId ?: 0},${image.picInfo?.originPic?.width ?: 0},${image.picInfo?.originPic?.height ?: 0})"
                }
            viewModel.send(
                ReplyUiIntent.Send(
                    "${getText()}\n$imageContent",
                    forumId,
                    forumName,
                    threadId,
                    curTbs,
                    title = threadTitle.takeIf { isTopicThread },
                    postId,
                    subPostId,
                    replyUserId,
                )
            )
        }
    }

    var closingPanel by remember { mutableStateOf(false) }
    var startClosingAnimation by remember { mutableStateOf(false) }

    fun showKeyboard() {
        editTextView?.post {
            editTextView?.requestFocus()
            showKeyboard(context, editTextView!!)
        }
        keyboardController?.show()
    }

    fun hideKeyboard() {
        editTextView?.apply {
            hideKeyboard(context, this)
            clearFocus()
        }
        keyboardController?.hide()
    }

    fun switchToPanel(type: ReplyPanelType) {
        if (curKeyboardType == type || type == NONE) {
            if (curKeyboardType != NONE) {
                showKeyboard()
                closingPanel = true
                startClosingAnimation = false
            }
            viewModel.send(ReplyUiIntent.SwitchPanel(NONE))
        } else {
            hideKeyboard()
            viewModel.send(ReplyUiIntent.SwitchPanel(type))
        }
    }


    MyBackHandler(
        enabled = curKeyboardType != NONE,
        currentScreen = ReplyPageDestination.takeUnless { isDialog }
    ) {
        switchToPanel(NONE)
    }

    val density = LocalDensity.current
    val imeInset = WindowInsets.ime
    val imeAnimationTargetInset = WindowInsets.imeAnimationTarget

    val imeCurrentHeight by produceState(initialValue = 0, imeInset, density) {
        snapshotFlow { imeInset.getBottom(density) }
            .distinctUntilChanged()
            .collect { value = it }
    }
    val imeAnimationTargetHeight by produceState(
        initialValue = 0,
        imeAnimationTargetInset,
        density
    ) {
        snapshotFlow { imeAnimationTargetInset.getBottom(density) }
            .distinctUntilChanged()
            .collect { value = it }
    }
    val imeAnimationEnd by remember { derivedStateOf { imeCurrentHeight == imeAnimationTargetHeight } }
    val imeVisibleHeightPx by produceState(
        initialValue = remember { context.appPreferences.imeHeight },
        imeAnimationTargetInset,
        density
    ) {
        snapshotFlow { imeAnimationTargetInset.getBottom(density) }
            .filter { it > 0 }
            .distinctUntilChanged()
            .collect {
                context.appPreferences.imeHeight = it
                value = it
            }
    }

    val panelHeight by remember {
        derivedStateOf { max(imeVisibleHeightPx.pxToDpFloat(), 150f).dp }
    }

    val textMeasurer = rememberTextMeasurer()

    val minResult = textMeasurer.measure(
        AnnotatedString("\n\n"),
        style = LocalTextStyle.current
    ).size.height.pxToDpFloat().dp

    val maxResult = textMeasurer.measure(
        AnnotatedString("\n\n\n\n\n"),
        style = LocalTextStyle.current
    ).size.height.pxToDpFloat().dp

    LaunchedEffect(closingPanel, imeAnimationEnd) {
        if (closingPanel) {
            if (!startClosingAnimation && !imeAnimationEnd) {
                startClosingAnimation = true
            } else if (startClosingAnimation && imeAnimationEnd) {
                closingPanel = false
                startClosingAnimation = false
            }
        }
    }

    val canSend by remember { derivedStateOf { !isTextEmpty || selectedImageList.isNotEmpty() } }

    val textFieldScrollState = rememberScrollState()

    val parentModifier = if (curKeyboardType == NONE && !closingPanel) {
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .consumeWindowInsets(WindowInsets.ime)
    } else {
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .consumeWindowInsets(WindowInsets.ime)
    }

    Column(
        modifier = parentModifier
//            .onFirstVisible() {
//                if (editTextView != null) {
//                    showKeyboard()
//                }
//            }
            .onVisibilityChanged { visibility ->
                if (imeAnimationEnd && (visibility && imeAnimationTargetHeight == 0)) {
                    showKeyboard()
                }
                if (imeAnimationEnd && (!visibility && imeAnimationTargetHeight > 10)) {
                    hideKeyboard()
                }
            }
            .padding(bottom = with(density) {
                imeAnimationTargetHeight.toDp()
            }),
        verticalArrangement = Arrangement.Bottom
    ) {
        // 玻璃标题行：左上角悬浮关闭钮 + 标题 + 字数。
        // 改造前是「一行 M2 纯文字 + 1dp VerticalDivider」，没有任何可点的返回。
        // destination（bottom sheet）会顶到屏幕最上沿，这里补状态栏内边距；
        // Dialog 形态自己带窗口内边距，不重复加。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isDialog) Modifier else Modifier.statusBarsPadding())
                .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                onClick = onBack,
                icon = PhosphorIcons["x"] ?: Icons.Rounded.Close,
                contentDescription = stringResource(id = R.string.btn_close),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = topTitle,
                modifier = Modifier.weight(1f),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTokens.text,
            )
            Text(
                text = "$textLength",
                fontSize = 13.sp,
                color = AppTokens.textSecondary
            )
        }
        if (isTopicThread) {
            // 主题帖标题：和正文同一个玻璃胶囊 + 同一套 iOS 字阶。
            // 字数上限（31）是业务规则，仍在 onValueChange 里。
            ReplyGlassInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                BaseTextField(
                    value = threadTitle,
                    onValueChange = { if (it.length <= 31) threadTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTokens.text,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.hint_thread_title),
                            style = LocalTextStyle.current.copy(fontSize = 16.sp),
                            color = AppTokens.textSecondary,
                        )
                    },
                )
            }
        }
        Box(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .imeNestedScroll(textFieldScrollState),
        ) {
            // 正文输入：玻璃胶囊/卡片（多行），placeholder 由 EditText 的 hint 用次级色
            ReplyGlassInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeightIn(min = minResult, max = maxResult)
                        .verticalScroll(textFieldScrollState)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            (View.inflate(
                                ctx,
                                R.layout.layout_reply_edit_text,
                                null
                            ) as UndoableEditText).apply {
                                editTextView = this
                                hint = when {
                                    isTopicThread -> ctx.getString(R.string.tip_thread_content)
                                    subPostId != null && subPostId != 0L && replyUserName != null ->
                                        ctx.getString(R.string.hint_reply, replyUserName)
                                    else -> ctx.getString(R.string.tip_reply)
                                }
                                // placeholder 用次级色（原 XML 里是 M2 的 hint 色）
                                setHintTextColor(AppTokens.textSecondary.toArgb())
                                setOnFocusChangeListener { _, hasFocus ->
                                    if (hasFocus) {
                                        switchToPanel(NONE)
                                    }
                                }
                                addTextChangedListener(
                                    afterTextChanged = {
                                        coroutineScope.launch {
                                            curTextFlow.emit(it?.toString() ?: "")
                                        }
                                    }
                                )
                                if (waitEditTextToSet) {
                                    waitEditTextToSet = false
                                    this.setText(StringUtil.getEmoticonContent(this, initialText))
                                    this.setSelection(initialText.length)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.Top)
                    )
                }
            }
//            BaseTextField(
//                value = text,
//                onValueChange = { text = it },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//                    .requiredHeightIn(min = minResult, max = maxResult)
//                    .focusRequester(focusRequester)
//                    .verticalScroll(textFieldScrollState)
//                    .onFocusChanged {
//                        Log.i("ReplyPage", "onFocusChanged: $it")
//                        if (it.hasFocus) {
//                            switchToKeyboard(NONE)
//                        }
//                    },
//                placeholder = { Text(text = stringResource(id = R.string.tip_reply)) },
//            )
        }
//            BaseTextField(
//                value = text,
//                onValueChange = { text = it },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//                    .requiredHeightIn(min = minResult, max = maxResult)
//                    .focusRequester(focusRequester)
//                    .verticalScroll(textFieldScrollState)
//                    .onFocusChanged {
//                        Log.i("ReplyPage", "onFocusChanged: $it")
//                        if (it.hasFocus) {
//                            switchToKeyboard(NONE)
//                        }
//                    },
//                placeholder = { Text(text = stringResource(id = R.string.tip_reply)) },
//            )
        // 底部工具条：玻璃条 + 圆钮；发送是 accent 实心胶囊
        ReplyGlassBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            GlassIconButton(
                onClick = { switchToPanel(EMOJI) },
                icon = PhosphorIcons["smiley"] ?: Icons.Outlined.EmojiEmotions,
                contentDescription = stringResource(id = R.string.insert_emotions),
            )
            if (postId == null || postId == 0L) {
                Box {
                    GlassIconButton(
                        onClick = { switchToPanel(IMAGE) },
                        icon = PhosphorIcons["image"] ?: Icons.Outlined.InsertPhoto,
                        contentDescription = stringResource(id = R.string.insert_photo),
                    )
                    if (selectedImageList.isNotEmpty()) {
                        // 已选图片数量角标（信息与改造前一致）
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(AppTokens.accent)
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = "${selectedImageList.size}",
                                fontSize = 10.sp,
                                color = AppTokens.onAccent,
                            )
                        }
                    }
                }
            }
//            IconButton(
//                onClick = { switchToPanel(VOICE) },
//                modifier = Modifier.size(24.dp)
//            ) {
//                Icon(
//                    imageVector = Icons.Outlined.KeyboardVoice,
//                    contentDescription = stringResource(id = R.string.insert_voice),
//                    modifier = Modifier.size(24.dp)
//                )
//            }
            Spacer(modifier = Modifier.weight(1f))
            if (isReplying) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(24.dp),
                    strokeWidth = 2.dp,
                    color = AppTokens.primary
                )
            } else {
                // 发送：accent 实心胶囊（不可发送时降为 chip 色，仍然保留点击禁用语义）
                val sendContentColor =
                    if (canSend) AppTokens.onAccent else AppTokens.textSecondary
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(if (canSend) AppTokens.accent else AppTokens.chip)
                        .clickable(enabled = canSend) {
                            if (context.appPreferences.safeMode) {
                                safeModeDialogOnOpen = false
                                warningDialogState.show()
                                return@clickable
                            }
                            val replyContent = if (subPostId == null || subPostId == 0L) {
                                getText()
                            } else {
                                "回复 #(reply, ${replyUserPortrait}, ${replyUserName}) :${getText()}"
                            }
                            if (selectedImageList.isEmpty()) {
                                viewModel.send(
                                    ReplyUiIntent.Send(
                                        content = replyContent,
                                        forumId = forumId,
                                        forumName = forumName,
                                        threadId = threadId,
                                        tbs = curTbs,
                                        title = threadTitle.takeIf { isTopicThread },
                                        postId = postId,
                                        subPostId = subPostId,
                                        replyUserId = replyUserId
                                    )
                                )
                            } else {
                                waitUploadSuccessToSend = true
                                viewModel.send(
                                    ReplyUiIntent.UploadImages(
                                        forumName,
                                        selectedImageList,
                                        isOriginImage
                                    )
                                )
                            }
                        }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = PhosphorIcons["paper-plane-tilt"]
                            ?: Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(id = R.string.send_reply),
                        tint = sendContentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.send_reply),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = sendContentColor,
                        maxLines = 1,
                    )
                }
            }
        }
//        Spacer(
//            modifier = Modifier
//                .fillMaxWidth()
//                .windowInsetsPadding(WindowInsets.ime)
//        )
        if (curKeyboardType != NONE) {
            Column(modifier = Modifier.height(panelHeight)) {
                when (curKeyboardType) {
                    EMOJI -> {
                        EmoticonPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            onEmoticonClick = { emoticon ->
                                insertEmoticon("#(${emoticon.name})")
                            }
                        )
                    }

                    IMAGE -> {
                        ImagePanel(
                            selectedImages = selectedImageList,
                            onNewImageSelected = { uris ->
                                viewModel.send(ReplyUiIntent.AddImage(uris.map { it.toString() }))
                            },
                            onRemoveImage = {
                                viewModel.send(ReplyUiIntent.RemoveImage(it))
                            },
                            isOriginImage = isOriginImage,
                            onIsOriginImageChange = {
                                viewModel.send(ReplyUiIntent.ToggleIsOriginImage(it))
                            },
                            modifier = Modifier
                                .padding(16.dp, 8.dp)
                                .fillMaxSize(),
                        )
                    }

                    else -> {}
                }
            }
        } else if (closingPanel) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(panelHeight)
            )
        }
    }

//    DisposableEffect(Unit) {
//        if (editTextView != null) {
//            showKeyboard()
//        }
//
//        onDispose {
//            if (editTextView != null) {
//                hideKeyboard()
//            }
//        }
//    }

    fun getDispatchUri(): Uri {
        return if (postId != null) {
            Uri.parse("com.baidu.tieba://unidispatch/pb?obj_locate=comment_lzl_cut_guide&obj_source=wise&obj_name=index&obj_param2=chrome&has_token=0&qd=scheme&refer=tieba.baidu.com&wise_sample_id=3000232_2&hightlight_anchor_pid=${postId}&is_anchor_to_comment=1&comment_sort_type=0&fr=bpush&tid=${threadId}")
        } else {
            Uri.parse("com.baidu.tieba://unidispatch/pb?obj_locate=pb_reply&obj_source=wise&obj_name=index&obj_param2=chrome&has_token=0&qd=scheme&refer=tieba.baidu.com&wise_sample_id=3000232_2-99999_9&fr=bpush&tid=${threadId}")
        }
    }

    fun launchOfficialApp() {
        val intent = Intent(Intent.ACTION_VIEW).setData(getDispatchUri())
        val resolveInfos =
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            ).filter { it.activityInfo.packageName != context.packageName }
        try {
            if (resolveInfos.isNotEmpty()) {
                context.startActivity(intent)
            } else {
                context.toastShort(R.string.toast_official_client_not_install)
            }
        } catch (e: ActivityNotFoundException) {
            context.toastShort(R.string.toast_official_client_not_install)
        }
    }

    Dialog(
        dialogState = warningDialogState,
        title = { Text(text = stringResource(id = R.string.title_dialog_safe_mode)) },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.btn_use_official_client),
                onClick = { launchOfficialApp() }
            )
            DialogNegativeButton(
                text = stringResource(id = R.string.btn_turn_off_safe_mode),
                onClick = {
                    context.appPreferences.safeMode = false
                    // 关闭安全模式后重置「不再提示」，下次重新开启时仍会提醒一次
                    context.appPreferences.safeModeDialogDontShow = false
                    context.toastShort(R.string.toast_safe_mode_disabled)
                }
            )
            DialogNegativeButton(
                text = stringResource(id = R.string.btn_cancel_safe_mode),
                onClick = {
                    if (safeModeDialogOnOpen) {
                        onBack()
                    }
                }
            )
            DialogNegativeButton(
                text = stringResource(id = R.string.btn_safe_mode_dont_show_again),
                onClick = {
                    context.appPreferences.safeModeDialogDontShow = true
                    context.toastShort(R.string.toast_safe_mode_dont_show_again)
                    if (safeModeDialogOnOpen) {
                        onBack()
                    }
                }
            )
        },
    ) {
        Text(
            text = stringResource(id = R.string.message_dialog_safe_mode),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }

    // 安全模式：进入发贴/回贴页时弹出提示；发送操作同样会被拦截
    // 用户点过「不再提示」后不再弹出（safeModeDialogDontShow == true）
    LaunchedEffect(Unit) {
        if (context.appPreferences.safeMode &&
            !context.appPreferences.safeModeDialogDontShow
        ) {
            safeModeDialogOnOpen = true
            warningDialogState.show()
        }
    }
}

// TODO: 将软键盘状态相关逻辑抽离出来
@Destination(style = DestinationStyleBottomSheet::class)
@Composable
fun ReplyPage(
    navigator: DestinationsNavigator,
    forumId: Long,
    forumName: String,
    threadId: Long,
    postId: Long? = null,
    subPostId: Long? = null,
    replyUserId: Long? = null,
    replyUserName: String? = null,
    replyUserPortrait: String? = null,
    tbs: String? = null,
    viewModel: ReplyViewModel = pageViewModel(),
) {
    // destination 是 M2 的 ModalBottomSheet（16dp 顶圆角），玻璃壳跟着这个圆角走，
    // 避免两种圆角叠出白边。
    ReplyGlassShell(cornerRadius = 16.dp) {
        ReplyPageContent(
            viewModel = viewModel,
            onBack = { navigator.navigateUp() },
            forumId = forumId,
            forumName = forumName,
            threadId = threadId,
            postId = postId,
            subPostId = subPostId,
            replyUserId = replyUserId,
            replyUserName = replyUserName,
            replyUserPortrait = replyUserPortrait,
            tbs = tbs
        )
    }
}

@Composable
private fun EmoticonPanel(
    modifier: Modifier = Modifier,
    onEmoticonClick: (Emoticon) -> Unit,
) {
    val emoticons = remember {
        EmoticonManager.getAllEmoticon()
            .filter { it.name.isNotEmpty() }
    }

    // 面板容器：和输入区同一块玻璃（原来是裸的 M2 网格贴在页面底）。
    // 内部仍是 LazyVerticalGrid + EmoticonManager，插入逻辑（"#(name)"）不变。
    ReplyGlassInput(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(44.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(emoticons) { emoticon ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEmoticonClick(emoticon) },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = rememberDrawablePainter(
                            drawable = EmoticonManager.getEmoticonDrawable(
                                LocalContext.current,
                                emoticon.id
                            )
                        ),
                        contentDescription = stringResource(
                            id = R.string.emoticon,
                            emoticon.name
                        ),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagePanel(
    selectedImages: ImmutableList<String>,
    onNewImageSelected: (List<Uri>) -> Unit,
    onRemoveImage: (Int) -> Unit,
    isOriginImage: Boolean,
    onIsOriginImageChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val id = remember { UUID.randomUUID().toString() }
    val coroutineScope = rememberCoroutineScope()
    onGlobalEvent<GlobalEvent.SelectedImages>(
        coroutineScope,
        filter = { it.id == id }
    ) {
        onNewImageSelected(it.images)
    }

    // 图片面板：整块玻璃容器 + 圆角缩略图。选图/删除/原图开关的行为全部不变
    // （GlobalEvent.StartSelectImages 的 id 过滤、9 张上限、动图判定都照旧）。
    ReplyGlassInput(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(selectedImages) { index, imageUri ->
                    Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
                        AsyncImage(
                            imageUri = imageUri,
                            contentDescription = stringResource(id = R.string.desc_image),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                        )
                        // 删除角标：深色半透明圆底 + 白叉，保证在任何图片上都看得见
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { onRemoveImage(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PhosphorIcons["x"] ?: Icons.Rounded.Close,
                                contentDescription = stringResource(id = R.string.desc_remove_image),
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
                if (selectedImages.size < 9) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppTokens.chip)
                                .clickable {
                                    coroutineScope.emitGlobalEvent(
                                        GlobalEvent.StartSelectImages(
                                            id,
                                            9 - selectedImages.size,
                                            PickMediasRequest.ImageOnly
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = PhosphorIcons["plus"] ?: Icons.Rounded.Add,
                                contentDescription = stringResource(id = R.string.desc_add_image),
                                tint = AppTokens.textSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            // 「原图」开关：Glasmio 开关（与设置页同一套 PrefsSwitch，含 API<33 兜底）
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onIsOriginImageChange(!isOriginImage)
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrefsSwitch(checked = isOriginImage, onCheckedChange = onIsOriginImageChange)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.origin_image),
                    fontSize = 14.sp,
                    color = AppTokens.text,
                )
            }
        }
    }
}
/* ────────────────────────── P3：回复页玻璃外壳 ────────────────────────── */

/**
 * 回复页 / 回复弹窗的**玻璃卡片**外壳。
 *
 * - 圆角：弹窗 26dp、bottom-sheet 目的地 16dp（跟 M2 `ModalBottomSheetLayout` 的圆角对齐，
 *   避免两种圆角叠出一圈白边）；
 * - backdrop：回复页是独立 Window / 独立 sheet，读不到页面那层 `LayerBackdrop`，
 *   所以按全 App 的做法**自带一个纯色 host**（和 `AppActionSheet` 同一个模式），
 *   里面的玻璃控件采样这块纯色底即可；
 * - 降级：API < 33（`isLiquidGlassSupported()` 为 false）时退回原来的不透明 `Surface`
 *   + `windowBackground`，外观与改造前一致，不会崩、不会白屏。
 */
@Composable
private fun ReplyGlassShell(
    cornerRadius: Dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
    if (!isLiquidGlassSupported()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = AppTokens.windowBackground,
            elevation = 0.dp,
        ) {
            content()
        }
        return
    }
    LiquidGlassTheme(accent = AppTokens.primary) {
        HarmonyTextProvider {
            GlasmioBackdropHost(
                sampledColor = glassColors().background,
                modifier = Modifier.fillMaxWidth(),
            ) {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                ) {
                    content()
                }
            }
        }
    }
}

/** 内层玻璃：输入胶囊。没有玻璃能力时退化成 chip 底色的圆角块。 */
@Composable
private fun ReplyGlassInput(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    if (!isLiquidGlassSupported()) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(AppTokens.chip)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            content = content,
        )
        return
    }
    GlassSurface(
        modifier = modifier,
        shape = shape,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            content = content,
        )
    }
}

/** 内层玻璃：底部工具条（胶囊）。 */
@Composable
private fun ReplyGlassBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    if (!isLiquidGlassSupported()) {
        Row(
            modifier = modifier
                .clip(shape)
                .background(AppTokens.chip)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
        return
    }
    GlassSurface(
        modifier = modifier,
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}