<div align="center">

# Tiebaliquid

**一个液态玻璃风格（iOS 26/27 观感）的第三方百度贴吧 Android 客户端**

基于 [TiebaLite](https://github.com/HuanCheng65/TiebaLite) 4.0-dev ，
经 [Aur5411/TiebaApp](https://github.com/Aur5411/TiebaApp)（Material 3 重制）二次开发，
本分支把**表现层**重写为基于 [Glasmio](https://github.com/neoruaa/Glasmio) 的液态玻璃视觉。

[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](./LICENSE)
![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg)
![minSdk](https://img.shields.io/badge/minSdk-33-orange.svg)
![targetSdk](https://img.shields.io/badge/targetSdk-36-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF.svg)
![Compose](https://img.shields.io/badge/Compose-BOM%202026.09.00-4285F4.svg)

[下载最新版本](../../releases/latest) · [问题反馈](../../issues)

</div>

---

## 截图

| 首页 | 帖子详情 |
| :---: | :---: |
| ![首页](docs/screenshots/home.png) | ![帖子](docs/screenshots/thread.png) |

| 楼中楼 | 设置 |
| :---: | :---: |
| ![楼中楼](docs/screenshots/subposts.png) | ![设置](docs/screenshots/settings.png) |

| 图标与排版 | 深色（AMOLED） |
| :---: | :---: |
| ![详情](docs/screenshots/settings-detail.png) | ![深色](docs/screenshots/dark.png) |

## 特点

- **液态玻璃材质**：底栏、顶栏、底栏、菜单、弹窗全部走 Glasmio 的 `RuntimeShader` 玻璃（背景采样 → 模糊 → 折射/边缘高光）
- **iOS 式页面语言**：一级页面无标题栏、大标题随滚动收起；二级页大标题 + 悬浮返回圆钮
- **卡片化的信息流**：头像 / 标题 / 正文 / 图片 / 吧名与互动数据，连续大圆角
- **平面化楼层**：主楼为卡片，楼层为无分割线的平面行，楼中楼为圆角预览块
- **统一的字形与图标**：HarmonyOS Sans SC 子集；Phosphor Icons（MIT）替换 Material 图标
- **单一主题令牌**：颜色 / 圆角 / 字号全部来自 `AppTokens`，浅色与 AMOLED 深色两套
- **菜单 source-transfer 形变**：菜单从触发器位置生长而出，而不是从中心缩放

## 与上游的关系

```
百度贴吧（官方，闭源）
   └── TiebaLite（HuanCheng65，GPL-3.0，已归档）
          └── TiebaApp（Aur5411，GPL-3.0，Material 3 重制，包名改为 com.tieba）
                 └── Tiebaliquid（本项目，表现层重写为液态玻璃）
```

- **业务层**（贴吧 API、protobuf、Room、登录、ViewModel）继承自上游，未做修改
- **表现层**（主题、组件、图标、字体、页面骨架）在本分支重写
- 包名 `com.tieba`，可覆盖安装 Aur5411 的 TiebaApp；与 TiebaLite（`com.huanchengfly.tieba.post`）是两个独立 App，数据不互通

## 构建

需要 JDK 21、Android SDK 36。构建环境与依赖版本（重要，组合较新）：

| 项 | 版本 |
| --- | --- |
| Gradle | 8.14.5 |
| AGP | 8.13.2 |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.09.00 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 33 |

```bash
./gradlew :app:assembleDebug
```

Glasmio 的几个模块以本地 Maven 仓库形式随仓库提供（`libs/maven/`），因此构建**不依赖该库在线可用**。

发布构建需要自备签名（`keystore.properties` 不入库）：

```properties
keystore.file=../path/to/your.jks
keystore.password=******
keystore.key.alias=******
keystore.key.password=******
```

## 测试（主干冒烟护栏）

目标不是覆盖率，是**主干被改坏时立刻发现**。5 条 instrumented 冒烟用例跑在真机 / 模拟器上，
全部用 **UiAutomator** 驱动：不注入、不替换 `Application`，Hilt 依赖图保持生产态，
也不需要 `@HiltAndroidTest` / 自定义 runner。

| # | 用例 | 断言点 |
| --- | --- | --- |
| 1 | `coldStartShowsFeed` | 全新启动后 dock 就绪；信息流至少 1 个「宽度 ≥ 60% 屏宽、高度 ≥ 200px、落在顶栏与 dock 之间」的可点击卡片 |
| 2 | `tapPostOpensThread` | 点第一个卡片 → 帖子页底栏出现「只看楼主」/「正序」，且顶栏有返回钮 |
| 3 | `settingsReachableFromProfile` | 「我」→「设置」→ `账号管理`、`安全模式（只读）`、`外观与通用` 三个分组 + 返回钮 |
| 4 | `dockSwitchesAllFourTabs` | 首页→进吧→消息→我→首页，每次都等到该页唯一标志节点；回首页后其它页标题必须消失 |
| 5 | `backFromThreadReturnsToFeed` | 进帖子 → 系统返回 → 信息流卡片回来、帖子页底栏消失、dock 可见 |

用例在 `app/src/androidTest/java/com/huanchengfly/tieba/post/smoke/MainSmokeTest.kt`，
每条都从「清空任务栈重新拉起 Activity」开始，互不依赖顺序；断言只用结构 / 页面级节点，
不依赖网络返回的具体帖子文字；也不需要登录态。

跑法（在**工作区根目录**执行，脚本会自己 source `scripts/env.ps1`）：

```powershell
.\scripts\run-smoke.ps1                      # 构建 test APK → connectedDebugAndroidTest → 汇总
.\scripts\run-smoke.ps1 -SkipBuild           # 只重跑测试
.\scripts\run-smoke.ps1 -Serial emulator-5554
```

等价的纯 gradle 命令（在 `sources/TiebaApp` 下）：

```powershell
Invoke-AppBuild -Tasks @(':app:connectedDebugAndroidTest')
```

失败时脚本把 `screen.png` / `hierarchy.xml` / `logcat.txt` / 测试自己写的现场 / gradle 日志
落到 `shots/smoke/<时间戳>/` 并打印路径；测试内部超时也会先把截图和语义树写进
`/sdcard/Android/data/com.tieba/files/smoke-failures/`。
选型理由（为什么不用 Compose UI Test）与已知不稳定点见工作区的 `SMOKE.md`。

## 已知限制

- **minSdk 33**：液态玻璃依赖 Android 13 的 `RuntimeShader`，Android 12 及以下无法安装
- **部分页面仍为 Material 3 外观**：长尾二级页（屏蔽词、收藏、关注列表等）尚未迁移；`FeedCard` 与 `PostCard` 两套卡片并存
- **部分玻璃为“自采样”**：页面内容层内的悬浮钮采样的是纯色而非真实背景（观感为半透明 + 描边）
- **dock 偶发停帧**：极低概率出现镜头停在中间，未定位（需要真机手势复现）
- **未登录场景未验证**：发帖、编辑资料保存、楼中楼发送等流程需要账号，尚未实测

## 开发方式

这是一个**人机协作**的项目，分工大致如下：

| 角色 | 承担 |
| --- | --- |
| **kingjoungyun**（维护者） | 需求定义、设计与工程取舍、验收与发布、问题定位 |
| **DeepSeek 模型 + DeepSeek Harness 多智能体** | 具体实现：页面改造、组件迁移、测试、构建与验证 |

说明：

- **提交署名统一为维护者**，便于贡献统计清晰；AI 协作的实现细节记录在仓库历史、`docs/` 与设计文档中
- 液态玻璃材质来自 Glasmio，**没有自行发明光学参数**；视觉参照 FlClash 的 prism_ui 与 Apple Liquid Glass
- 界面改动均在真机或模拟器上实际验证后才提交；未验证的部分会写进「已知限制」

如果这种协作方式不合你的预期，请在使用前知悉。

## 致谢与许可

本项目遵循 **GPL-3.0**，与上游保持一致。

- 业务层与页面结构来自 [HuanCheng65/TiebaLite](https://github.com/HuanCheng65/TiebaLite) 与 [Aur5411/TiebaApp](https://github.com/Aur5411/TiebaApp)
- 液态玻璃渲染来自 [Glasmio](https://github.com/neoruaa/Glasmio)（Apache-2.0），其 AAR 与许可随仓库提供于 `libs/maven/`
- 图标来自 [Phosphor Icons](https://phosphoricons.com/)（MIT）
- 字体为 HarmonyOS Sans SC 子集（免费商用）
- 设计语言参考 [FlClash](https://github.com/chen08209/FlClash) 的 prism_ui 与 Apple Liquid Glass

## 免责声明

本项目为个人学习与技术研究用途，与百度公司无关。请遵守当地法律法规与贴吧用户协议，使用风险自负。
