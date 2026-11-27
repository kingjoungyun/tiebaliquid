# 交接说明（2026-11-27 00:35 更新）

> 下次接手先读这份，再读 `PLAN.md`（重点 §17–§24）。

## 当前状态

| 项 | 值 |
| --- | --- |
| 分支 / HEAD | `glass-ui` @ `1669591`（tag `v1.9.0`） |
| 远端 | 与本地一致，tag 已推送 |
| 工作区 | 干净 |
| 冒烟 | 全量 10 条绿 |
| **v1.9.0 已发布** | https://github.com/kingjoungyun/tiebaliquid/releases/tag/v1.9.0 （APK 40.5 MB） |
| 模拟器 | 已关闭 |

## v1.9.0 完成的内容

- 页面骨架统一（`GlassPage` + `largeTitle` + 自动折叠 + snackbar 槽）
- `AppScaffold` / `Toolbar()` 重写为自研实现（M2 依赖清零）
- 信息流卡片统一（视频封面、多图轮播、正文折叠、转发卡、内嵌图提取）
- 设置页六分组、我的页四段结构
- 壁纸链路删除（−983 行）、检查更新 API 化、外部域名依赖清零
- 修复：字体过细、深色背景下底栏图标不可见（内容色未跟随玻璃自适应）

## 下一步：v1.9.1（约 1–1.5 天）

按 PLAN §24.5 / §24.7 的顺序：

1. **`Dialogs.kt` 重写为 `AppDialog`**（432 行 / 62 处调用）
   - 用 Compose `Dialog` + 玻璃壳，不继承 M2
   - 需保证 prompt / time picker / custom 三种形态能力对等
2. **剩余 7 个 `*UiStyle`**：`Home` / `Explore` / `Search` / `Notifications` / `History` / `SearchUser` / `UserProfile`
   - 逐页对比 LEGACY 与玻璃版（§22：缺功能要补，不能连功能一起删）
   - `PrefsUiStyle` / `UserPageUiStyle` / `ToolboxUiStyle` / `AnalysisUiStyle` / `HotTopicListUiStyle` / `ThreadPageUiStyle` / `PostCardUiStyle` / `ForumPageUiStyle` 已删
3. **统一应用内通知（§20）**：`RootNoticeHost` + `AppNoticeController`，替换 37 处 toast/snackbar
4. **收藏页「更新到第 N 楼」角标**（§22.4）：给 `PostCard` 加 `trailingBadge`

## v1.9.2 / v2.0.0

见 PLAN §24.7 全表。

## 环境注意事项

- **代理**：`127.0.0.1:10808`（v2rayN），机器重启后不自启；推送/拉取失败先查端口
- **git 凭据**：Windows 凭据管理器里有 `kingjoungyun` 的 40 位 token（可用于 GitHub API）
- **构建**：`. .\scripts\env.ps1` → `Invoke-AppBuild`；release：`Invoke-AppBuild @(':app:assembleRelease')`（约 9 分钟）
- **冒烟**：`.\scripts\run-smoke.ps1 -Quick`（约 20 秒）/ 全量（约 80 秒）；**不要与构建并发**
- **模拟器**：`C:\AndroidSdk\emulator\emulator.exe -avd glass -no-window -no-snapshot -no-audio -no-boot-anim -gpu host`
- **发布**：见本轮流程（tag → GitHub API 建 release → 上传 APK）；`gh` CLI 未安装，用 REST API

## 已知坑（都验证过）

| 坑 | 教训 |
| --- | --- |
| 用正则批量改 Kotlin 文本 | `[...]` 被当字符类 → 注释被改坏。**一律用字面 `String.Replace`** |
| 替换 `AppScaffold(topBar=…)` | 括号数会少一个，必须在文件末尾补 `}` |
| 构建与冒烟并发 | Gradle 缓存互相破坏，必须串行 |
| 「节点存在」= 测试通过 | 深色背景下图标不可见时 uiautomator 仍能读到，**自动化抓不到**，需要人眼看截图 |
| `GlassSurface` 内部取外层颜色 | 会覆盖玻璃的自适应内容色 → 深色背景上不可见。用 `LocalContentColor.current` |

## 2026-11-27 下午 修 BUG 停点（HEAD `42a6ffb`）

v1.9.0 发布后收到真实设备反馈，已修 4 项并全部推送：

| BUG | 根因 | commit |
| --- | --- | --- |
| 1 大标题重影 | `PrefsLargeTitle` 同一个标题画了两次（34sp + 24sp） | `a2baa5c` |
| 2 状态栏白条 | pager 吃了顶部 inset + tab 页根布局 `statusBarsPadding()` | `3b187f0`、`01373ea` |
| 5 深色配图全黑 | Sketch `MaskTransformation` 静默失败（返回 Success 但涂黑）→ 改 Compose 层遮罩 | `baef50a` |
| 4 白矩形 | `PrefsSwitch` 玻璃采样层是矩形，直角在深色下露白 → 裁胶囊 | `42a6ffb` |

**BUG 3「内容穿透状态栏」经用户澄清是理想状态，不是 bug，未改代码。**

### 晚上继续的清单（详见 `docs/BUGS-v1.9.0.md` 末尾）

1. **edge-to-edge 剩余 5 处**：`NotificationsPage:193`、`SearchPage:267`、`HistoryPage:98`、`ForumGlasmio:324`、`NavigationComponents:161,315`
2. **深色灰阶 token 对照表**：`groupedBackground`/`card`/`elevatedBackground`/`content`/`secondaryContent`/dock/分段控件/搜索框，判据「页面 < 卡片 < 浮层」+ 对比度 ≥ 4.5:1
3. **深色自查**：帖子页底栏、设置页六分组卡片、我的页账户卡
4. 截图欠账：`bug4_settings_dark`、`e2e_notifications`、`e2e_search`、`e2e_history`、`e2e_forum`

### 修完后的发布决定（待定）
- 方案 A：把修复打成 **v1.9.1**（推荐，v1.9.0 已知有严重深色 bug，用户已装）
- 方案 B：删除 v1.9.0 release，重发同 tag（不推荐，历史混乱）

### 环境备注
- 本轮 App 被卸载过一次 → 偏好重置；**验证深色前先重新打开深色开关**
- 系统权限弹窗（"Remember photo locations?"）会把 App 顶到后台；uiautomator 抓不到节点时先 BACK 再拉起
