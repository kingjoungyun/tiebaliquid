pluginManagement {
    repositories {
        // 国内镜像优先：google()/mavenCentral() 在墙内会先吃超时
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/jcenter")
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ── 本地 Maven 仓库（必须排最前面）────────────────────────────────
        // Glasmio 的 6 个模块（core/glass/ui/preference/icons/scaffold）0.1.3 的 AAR+POM
        // 已经放进 libs/maven/，换版本用 scripts/sync-glasmio.ps1 重新拉。
        // 放在最前 = 离线也能解析；在线时也不会被远端镜像的旧/新版本盖掉。
        maven { url = uri(rootDir.resolve("libs/maven")) }
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://developer.huawei.com/repo/")
        maven("https://developer.hihonor.com/repo")
    }
}

plugins {
    id("de.fayard.refreshVersions") version "0.60.6"
}

refreshVersions {
    rejectVersionIf {
          candidate.stabilityLevel.isLessStableThan(current.stabilityLevel)
    }
}

rootProject.name = "TiebaLite"
include(":app")
