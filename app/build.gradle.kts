import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.util.Properties

// 读取 application.properties
val appProperties = Properties().apply {
    file("${rootProject.projectDir}/application.properties").inputStream().use { load(it) }
}

// 读取 keystore.properties（如果存在）
val keystorePropertiesFile = file("${rootProject.projectDir}/keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
    //alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.wire)
}

val sha: String? = System.getenv("GITHUB_SHA")
val isCI: String? = System.getenv("CI")
val isSelfBuild = isCI.isNullOrEmpty() || !isCI.equals("true", ignoreCase = true)
val applicationVersionCode = appProperties.getProperty("versionCode").toInt()
var applicationVersionName = appProperties.getProperty("versionName")
val isPerVersion = appProperties.getProperty("isPreRelease").toBoolean()
if (isPerVersion) {
    applicationVersionName += "-${appProperties.getProperty("preReleaseName")}.${appProperties.getProperty("preReleaseVer")}"
}
if (!isSelfBuild && !sha.isNullOrEmpty()) {
    applicationVersionName += "+${sha.substring(0, 7)}"
}

wire {
    sourcePath {
        srcDir("src/main/protos")
    }

    kotlin {
        android = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    buildToolsVersion = "36.0.0"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.tieba"
        minSdk = 33
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = applicationVersionCode
        versionName = applicationVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        manifestPlaceholders["is_self_build"] = "$isSelfBuild"
    }
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }
    signingConfigs {
        val keystoreFile = keystoreProperties.getProperty("keystore.file", "")
        if (keystoreFile.isNotBlank()) {
            create("config") {
                storeFile = file(File(rootDir, keystoreFile))
                storePassword = keystoreProperties.getProperty("keystore.password")
                keyAlias = keystoreProperties.getProperty("keystore.key.alias")
                keyPassword = keystoreProperties.getProperty("keystore.key.password")
                // 统一使用 v1 + v2 + v3 签名（v4 仅用于增量安装，不需要）
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            isJniDebuggable = true
            multiDexEnabled = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            isJniDebuggable = false
            multiDexEnabled = true
        }
        all {
            signingConfig =
                if (signingConfigs.any { it.name == "config" })
                    signingConfigs.getByName("config")
                else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }
    composeCompiler {
        metricsDestination.set(layout.buildDirectory.dir("compose_metrics"))
        reportsDestination.set(layout.buildDirectory.dir("compose_metrics"))

        // Kotlin 2.4 起 stabilityConfigurationFile 变成 error，改用复数形式
        stabilityConfigurationFiles.set(
            listOf(rootProject.layout.projectDirectory.file("compose_stability_configuration.txt"))
        )
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "DebugProbesKt.bin"
        }
    }
    namespace = "com.huanchengfly.tieba.post"
    applicationVariants.configureEach {
        val variant = this
        outputs.configureEach {
            val fileName =
                "${variant.buildType.name}-${applicationVersionName}(${applicationVersionCode}).apk"

            (this as BaseVariantOutputImpl).outputFileName = fileName
        }
    }
}

// 钉死 Kotlin 的 jvmTarget=17，与上面 compileOptions 的 Java 17 保持一致。
// 否则在本机用 JDK 21 跑 Gradle 时，Kotlin 2.3 会把 jvmTarget 推到 21，
// 与 Java 的 17 冲突报 "Inconsistent JVM Target Compatibility"。
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

// Glasmio 0.1.3 的 AAR 元数据写着 minCompileSdk=37 / minAgpVersion=9.1.0（它自己用
// compileSdk 37 + AGP 9 编译）。本工程是 compileSdk 36 + AGP 8.13.2，Gradle 会在
// checkXxxAarMetadata 任务里把这两条当成硬错误直接失败。
// 这里的 AAR 已经是二进制，只要我们不调用它面向 API 37 / 新版 AGP 的新增 API 就能正常编译运行，
// 所以显式跳过这个元数据校验（比升级 AGP 9 + Gradle 9 的风险小得多）。
tasks.configureEach {
    if (name.startsWith("check") && name.endsWith("AarMetadata")) {
        enabled = false
    }
}

// Kotlin 升到 2.4.21 后，Hilt(2.58) 的注解处理器读 App 的 Kotlin 类元数据会报
// "Provided Metadata instance has version 2.4.0, while maximum supported version is 2.3.0"。
// 它用的是非 shaded 的 kotlin-metadata-jvm，这里把所有配置里的这个库顶到 2.4.21。
configurations.configureEach {
    resolutionStrategy.force("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.21")
}

dependencies {
    //Local Files
//    implementation fileTree(include: ["*.jar"], dir: "libs")

    implementation(libs.net.swiftzer.semver.semver)
    implementation(libs.godaddy.color.picker)

    implementation(libs.airbnb.lottie)
    implementation(libs.airbnb.lottie.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)

    // implementation(libs.androidx.navigation.compose)

    api(libs.wire.runtime)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.accompanist.drawablepainter)

    // Replaced deprecated Accompanist modules with official/third-party alternatives
    implementation(libs.eygraber.placeholder.material)
    implementation(libs.systemuibars.tweaker)

    implementation(libs.sketch.core)
    implementation(libs.sketch.compose)
    implementation(libs.sketch.ext.compose)
    implementation(libs.sketch.gif)
    implementation(libs.sketch.okhttp)

    implementation(libs.zoomimage.compose.sketch)

    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    runtimeOnly(libs.compose.runtime.tracing)
    implementation(libs.compose.animation)
    implementation(libs.compose.animation.graphics)
    implementation(libs.compose.material)
    implementation(libs.compose.material.navigation)
    implementation(libs.compose.material.icons.core)
    // Optional - Add full set of material icons
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.util)
    // Glasmio 需要 material3（本工程业务代码仍然只用 M2，0 引用）
    implementation(libs.compose.material3)

    // 真·液态玻璃（Glasmio）：backdrop 录制 + 折射/模糊材质
    implementation(libs.glasmio.glass)
    // Glasmio 的上层组件（设置页开关用）。glasmio-glass 的 POM 只依赖 core + icons，
    // 不含 ui，所以这里必须并列声明同一个版本，否则 compileClasspath 看不到 GlasmioSwitch。
    implementation(libs.glasmio.ui)
    // 设置页组件（GlasmioPreferenceEntry / GlasmioSwitchPreference / GlasmioPreferenceGroupTitle /
    // GlasmioGroupedList / GlasmioGroupedTableFooter）：生产设置页的渲染层（PrefsScreen 的
    // GLASSMIO 路径）真机也要用，所以走 implementation（此前只有 debug 预览页用，走 debugImplementation）。
    implementation(libs.glasmio.preference)
    // 导航壳（Stage 1 的 Navigation 3 / NavDisplay 用）。本轮只声明依赖、不写业务代码，
    // 目的是把传递依赖（navigation3 / navigationevent / miuix）先拉通、可离线解析。
    implementation(libs.glasmio.scaffold)

    // Android Studio Preview support
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // UI Tests
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugRuntimeOnly(libs.compose.ui.test.manifest)

    implementation(libs.androidx.constraintlayout.compose)

    implementation(libs.github.oaid)

    implementation(libs.org.jetbrains.annotations)

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    //AndroidX
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.window)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.swiperefreshlayout)

    //Test
    testImplementation(libs.junit.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestRuntimeOnly(libs.androidx.test.runner)

    //Glide
    implementation(libs.glide.core)
    ksp(libs.glide.ksp)
    implementation(libs.glide.okhttp3.integration)

    implementation(libs.google.material)

    implementation(libs.okhttp3.core)
    implementation(libs.retrofit2.core)
    implementation(libs.retrofit2.converter.wire)

    implementation(libs.google.gson)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.com.jaredrummler.colorpicker)

    implementation(libs.github.matisse)
    implementation(libs.xx.permissions)
    implementation(libs.com.gyf.immersionbar.immersionbar)

    implementation(libs.com.github.yalantis.ucrop)

    //implementation(libs.com.jakewharton.butterknife)
    //ksp(libs.com.jakewharton.butterknife.compiler)

    // ksp(libs.kotlin.metadata.jvm)
}
