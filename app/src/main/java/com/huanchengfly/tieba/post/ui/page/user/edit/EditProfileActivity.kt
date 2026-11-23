package com.huanchengfly.tieba.post.ui.page.user.edit


import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.panpf.sketch.compose.AsyncImage
import com.eygraber.compose.placeholder.material.placeholder
import com.stoyanvuchev.systemuibarstweaker.SystemBarStyle
import com.stoyanvuchev.systemuibarstweaker.rememberSystemUIBarsTweaker
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BaseTextField
import com.huanchengfly.tieba.post.ui.widgets.compose.CounterTextField
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.ColorUtils
import com.huanchengfly.tieba.post.utils.PermissionUtils
import com.huanchengfly.tieba.post.utils.PickMediasRequest
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.registerPickMediasLauncher
import com.huanchengfly.tieba.post.utils.requestPermission
import com.huanchengfly.tieba.post.utils.shouldUsePhotoPicker
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import java.io.File
import com.huanchengfly.tieba.post.ui.common.icons.PhosphorIcons
import com.huanchengfly.tieba.post.ui.common.theme.glass.AppTokens
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.prefs.GroupHeader
import com.huanchengfly.tieba.post.ui.common.prefs.LocalPrefsUiStyle
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsCard
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsListItem
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsUiStyle
import com.huanchengfly.tieba.post.ui.common.theme.glass.GlassIconButton
import com.huanchengfly.tieba.post.ui.common.theme.glass.HarmonyTextProvider
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassSurface
import com.huanchengfly.tieba.post.ui.common.theme.glass.LiquidGlassTheme
import com.huanchengfly.tieba.post.ui.common.theme.glass.isLiquidGlassSupported
@AndroidEntryPoint
class EditProfileActivity : BaseActivity<Nothing>() {
    private val uCropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.send(EditProfileIntent.UploadPortrait(File(cacheDir, "cropped_portrait")))
            } else if (it.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(it.data!!)
                cropError!!.printStackTrace()
            }
        }

    private val pickMediasLauncher =
        registerPickMediasLauncher { (_, uris) ->
            if (uris.isNotEmpty()) {
                val sourceUri = uris[0]
                Glide.with(this)
                    .asFile()
                    .load(sourceUri)
                    .into(object : CustomTarget<File>() {
                        override fun onLoadCleared(placeholder: Drawable?) {}

                        override fun onResourceReady(
                            resource: File,
                            transition: Transition<in File>?
                        ) {
                            val sourceFileUri = Uri.fromFile(resource)
                            val destUri = Uri.fromFile(File(cacheDir, "cropped_portrait"))
                            val intent = UCrop.of(sourceFileUri, destUri)
                                .withAspectRatio(1f, 1f)
                                .withOptions(UCrop.Options().apply {
                                    setShowCropFrame(true)
                                    setShowCropGrid(true)
                                    setToolbarColor(
                                        ThemeUtils.getColorByAttr(
                                            this@EditProfileActivity,
                                            R.attr.colorPrimary
                                        )
                                    )
                                    setToolbarWidgetColor(
                                        ThemeUtils.getColorByAttr(
                                            this@EditProfileActivity,
                                            R.attr.colorTextOnPrimary
                                        )
                                    )
                                    setActiveControlsWidgetColor(
                                        ThemeUtils.getColorByAttr(
                                            this@EditProfileActivity,
                                            R.attr.colorAccent
                                        )
                                    )
                                    setLogoColor(
                                        ThemeUtils.getColorByAttr(
                                            this@EditProfileActivity,
                                            R.attr.colorPrimary
                                        )
                                    )
                                    setCompressionFormat(Bitmap.CompressFormat.JPEG)
                                }
                                )
                                .getIntent(this@EditProfileActivity)
                            uCropLauncher.launch(intent)
                        }
                    })
            }
            }

    private val viewModel: EditProfileViewModel by viewModels()

    private val intents by lazy {
        merge(
            flowOf(EditProfileIntent.Init(AccountUtil.getUid() ?: "0"))
        )
    }
    private val handler = Handler(Looper.getMainLooper())
    override val isNeedImmersionBar: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            TiebaLiteTheme {
                val systemUIBarsTweaker = rememberSystemUIBarsTweaker()
                SideEffect {
                    val statusBarDarkIcons = ThemeUtil.isStatusBarFontDark()
                    val navigationBarDarkIcons = ThemeUtil.isNavigationBarFontDark()

                    systemUIBarsTweaker.tweakStatusBarStyle(
                        SystemBarStyle(
                            color = Color.Transparent,
                            darkIcons = statusBarDarkIcons
                        )
                    )
                    systemUIBarsTweaker.tweakNavigationBarStyle(
                        SystemBarStyle(
                            color = Color.Transparent,
                            darkIcons = navigationBarDarkIcons
                        )
                    )
                }
                PageEditProfile(viewModel, onBackPressed = { onBackPressed() })
            }
        }
        handler.post {
            intents.onEach(viewModel::send).launchIn(lifecycleScope)
        }
        viewModel.uiEventFlow
            .filterIsInstance<EditProfileEvent>()
            .collectIn(this) { handleEvent(it) }
    }

    private fun handleEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.Init.Fail -> {
                toastShort(event.toast)
            }

            is EditProfileEvent.Submit.Result -> {
                if (event.success) {
                    if (event.changed) toastShort(R.string.toast_success)
                    finish()
                } else {
                    toastShort(event.message)
                }
            }

            EditProfileEvent.UploadPortrait.Pick -> {
                if (shouldUsePhotoPicker()) {
                    pickMediasLauncher.launch(PickMediasRequest(mediaType = PickMediasRequest.ImageOnly))
                } else {
                    requestPermission {
                        permissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            listOf(
                                PermissionUtils.READ_EXTERNAL_STORAGE,
                                PermissionUtils.WRITE_EXTERNAL_STORAGE
                            )
                        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            listOf(
                                PermissionUtils.READ_EXTERNAL_STORAGE
                            )
                        } else {
                            listOf(PermissionUtils.READ_MEDIA_IMAGES)
                        }
                        description = context.getString(R.string.tip_permission_storage)
                        onGranted = {
                            pickMediasLauncher.launch(PickMediasRequest(mediaType = PickMediasRequest.ImageOnly))
                        }
                        onDenied = {
                            toastShort(R.string.toast_no_permission_upload_portrait)
                        }
                    }
                }
            }

            is EditProfileEvent.UploadPortrait.Fail -> toastShort(event.error)
            is EditProfileEvent.UploadPortrait.Success -> toastShort(event.message)
        }
    }
}

/**
 * 「编辑资料」字段区：iOS 分组卡（26dp 连续圆角、内建分隔线）+ 玻璃输入胶囊。
 *
 * 数据与回调与改造前完全一致（头像上传 / 昵称 / 性别 / 签名 / loading 占位），只换渲染层。
 * 必须在 `LocalPrefsUiStyle = GLASSMIO` 的树里调用（[GlassEditScaffold] 负责注入）。
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EditProfileCard(
    portrait: String,
    name: String,
    nickName: String,
    sex: Int,
    intro: String,
    loading: Boolean,
    onNickNameChange: ((String) -> Unit)? = null,
    onIntroChange: ((String) -> Unit)? = null,
    onUploadPortrait: (() -> Unit)? = null,
    onModifySex: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    PrefsCard(header = { GroupHeader(title = "资料") }) {
        // 头像：右侧 52dp 圆，点击换头像（loading 时是占位块）
        PrefsListItem(
            minimalHeight = true,
            text = { Text(text = stringResource(id = R.string.upload_portrait), fontSize = 15.sp) },
            trailing = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !loading) { onUploadPortrait?.invoke() },
                ) {
                    if (loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AppTokens.chip)
                        )
                    } else {
                        AsyncImage(
                            imageUri = StringUtil.getAvatarUrl(portrait),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.32f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PhosphorIcons["camera"] ?: Icons.Rounded.PhotoCamera,
                            contentDescription = stringResource(id = R.string.upload_portrait),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
        )
        // 用户名（账号名只读）
        PrefsListItem(
            text = { Text(text = stringResource(id = R.string.title_username), fontSize = 15.sp) },
            trailingValue = name.ifBlank { "—" },
        )
        // 昵称
        PrefsListItem(
            minimalHeight = true,
            text = { Text(text = stringResource(id = R.string.title_nickname), fontSize = 15.sp) },
            trailing = {
                GlassField(modifier = Modifier.width(176.dp)) {
                    BaseTextField(
                        value = nickName,
                        onValueChange = { onNickNameChange?.invoke(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        maxLines = 1,
                    )
                }
            },
        )
        // 性别（点击弹选择器）
        PrefsListItem(
            text = { Text(text = stringResource(id = R.string.profile_sex), fontSize = 15.sp) },
            trailingValue = stringResource(
                id = when (sex) {
                    1 -> R.string.profile_sex_male
                    2 -> R.string.profile_sex_female
                    else -> R.string.profile_sex_unset
                }
            ),
            showChevron = onModifySex != null,
            onClick = onModifySex?.let { { it() } },
        )
        // 签名（多行，500 字上限仍在 CounterTextField 里）
        PrefsListItem(
            minimalHeight = true,
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.title_intro), fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    GlassField(modifier = Modifier.fillMaxWidth()) {
                        CounterTextField(
                            value = intro,
                            onValueChange = { onIntroChange?.invoke(it) },
                            maxLength = 500,
                            countWhitespace = false,
                            onLengthBeyondRestrict = {
                                context.toastShort(R.string.toast_intro_length_beyond_restrict)
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(id = R.string.tip_no_intro),
                                    color = AppTokens.textSecondary,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
        )
    }
}

/**
 * 玻璃输入框：14dp 圆角。
 *
 * 用项目自己的 [LiquidGlassSurface] 适配层：这页是独立 Activity、没有页级 backdrop host，
 * 它会自动走「伪玻璃」（半透明填充 + 1dp 描边 + 内发光），浅色/深色都与页面底同档；
 * API<33 也由它内部兜底，不需要另写分支。
 */
@Composable
private fun GlassField(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    LiquidGlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        // 卡片本身是白的，玻璃默认 fill 也是白的 → 边界不可见。
        // 用 chip 色给一个更深的档，输入框才「看得出是个框」。
        navigationSurfaceColor = AppTokens.chip,
        innerGlow = true,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            content()
        }
    }
}

/** 保存：accent 实心胶囊（原来是 Toolbar 右上角的 M2 图标按钮）。 */
@Composable
private fun SaveCapsule(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(AppTokens.accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = PhosphorIcons["check"] ?: Icons.Rounded.Check,
            contentDescription = null,
            tint = AppTokens.onAccent,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(id = R.string.button_save_profile),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTokens.onAccent,
            maxLines = 1,
        )
    }
}

/**
 * 页面骨架：分组底色 + 悬浮返回 + 34sp 大标题 + 保存胶囊。
 * 字段区由调用方传入（[EditProfileCard]）。
 */
@Composable
private fun GlassEditScaffold(
    onBack: () -> Unit,
    onSave: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTokens.groupedBackground)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(52.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(id = R.string.button_back),
                )
                Spacer(modifier = Modifier.weight(1f))
                SaveCapsule(onClick = onSave)
            }
            Text(
                text = stringResource(id = R.string.title_activity_edit_profile),
                fontSize = 34.sp,
                lineHeight = 41.sp,
                fontWeight = FontWeight.Bold,
                color = AppTokens.text,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            LiquidGlassTheme(accent = AppTokens.primary) {
                HarmonyTextProvider {
                    CompositionLocalProvider(LocalPrefsUiStyle provides PrefsUiStyle.GLASSMIO) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            content()
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PageEditProfile(
    viewModel: EditProfileViewModel,
    onBackPressed: () -> Unit,
) {
    val isLoading by viewModel.uiState.collectPartialAsState(
        EditProfileState::isLoading,
        initial = false
    )
    if (!isLoading) {
        // 加载完成后才读一次完整状态：`remember` 的初值捕获的是「已到达的字段」，
        // 提前 collect 会把空值固化、后续输入覆盖不到（与改造前语义一致）。
        val uiState by viewModel.uiState.collectAsState()
        var sex by remember { mutableIntStateOf(uiState.sex) }
        val birthdayTime by remember { mutableLongStateOf(uiState.birthdayTime) }
        val birthdayShowStatus by remember { mutableStateOf(uiState.birthdayShowStatus) }
        var intro by remember { mutableStateOf(uiState.intro) }
        var nickName by remember { mutableStateOf(uiState.nickName) }
        val dialogState = rememberDialogState()

        // 保存逻辑与改造前逐字一致：没改 → SubmitWithoutChange；改了但昵称为空 → 不发
        val onSave: () -> Unit = {
            if (sex != uiState.sex ||
                birthdayTime != uiState.birthdayTime ||
                birthdayShowStatus != uiState.birthdayShowStatus ||
                intro != uiState.intro ||
                nickName != uiState.nickName
            ) {
                if (nickName.isNotEmpty()) {
                    viewModel.send(
                        EditProfileIntent.Submit(
                            sex,
                            birthdayTime,
                            birthdayShowStatus,
                            intro ?: "",
                            nickName
                        )
                    )
                }
            } else {
                viewModel.send(EditProfileIntent.SubmitWithoutChange)
            }
        }

        GlassEditScaffold(onBack = onBackPressed, onSave = onSave) {
            EditProfileCard(
                portrait = uiState.portrait,
                name = uiState.name,
                nickName = nickName,
                sex = sex,
                intro = intro ?: "",
                loading = false,
                onNickNameChange = { nickName = it },
                onIntroChange = { intro = it },
                onUploadPortrait = { viewModel.send(EditProfileIntent.UploadPortraitStart) },
                onModifySex = { dialogState.show = true }
            )
        }

        // 性别选择器：仍走全站统一弹窗外壳（AnyPopDialog + 玻璃壳）
        Dialog(
            dialogState = dialogState,
            title = { Text(text = stringResource(id = R.string.title_modify_sex)) },
            buttons = {
                DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
            }
        ) {
            ListSinglePicker(
                itemTitles = persistentListOf(
                    stringResource(id = R.string.profile_sex_male),
                    stringResource(id = R.string.profile_sex_female)
                ),
                itemValues = persistentListOf(1, 2),
                selectedPosition = if (sex == 1) 0 else if (sex == 2) 1 else -1,
                onItemSelected = { _, _, value, _ ->
                    sex = value
                    dismiss()
                }
            )
        }
    } else {
        GlassEditScaffold(onBack = onBackPressed, onSave = {}) {
            EditProfileCard(
                portrait = "",
                name = "",
                nickName = "",
                sex = 0,
                intro = "",
                loading = true
            )
        }
    }
}
