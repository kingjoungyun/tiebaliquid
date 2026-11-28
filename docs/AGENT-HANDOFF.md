# 智能体交接书（给新的子智能体）

> 你接手的是一个已经运行了很多轮的改造项目。**先读这份，再读 `docs/COMPONENT-CONTRACT.md` 与 `docs/MIGRATION-CHECKLIST.md`，然后按"当前任务队列"往下做。**
> 不要重新调研架构，不要重复已验证的结论。

## 0. 必读（按顺序，10 分钟）

1. `docs/COMPONENT-CONTRACT.md` —— 组件使用硬规则（颜色来源、作用域、玻璃 host、insets）
2. `docs/MIGRATION-CHECKLIST.md` —— 页面迁移 5 步 + 历史坑
3. `docs/INSETS-WHITELIST.md` —— 哪些 `statusBarsPadding()` 是"顶栏自身"，**不许动**
4. `PLAN.md` §17–§27 —— 决策与版本归属
5. `docs/BUGS-v1.9.0.md` —— 已知问题与待验清单

## 1. 项目环境

- 仓库：`sources/TiebaApp`，分支 `glass-ui`，远端 `origin/main` 同名（推它即可）
- 工作区根：`C:\Users\39982\Desktop\SoftwareDevelopment\tieba-glass`
- 构建：`. .\scripts\env.ps1` → `Invoke-AppBuild`（debug，约 3 分钟）；release 用 `Invoke-AppBuild @(':app:assembleRelease')`（约 9 分钟）
- 冒烟：`.\scripts\run-smoke.ps1 -Quick`（2 条，约 20 秒）/ 全量（13 条，约 200 秒）
- 模拟器：`C:\AndroidSdk\emulator\emulator.exe -avd glass -no-window -no-snapshot -no-audio -no-boot-anim -gpu host`
- **推送前确认代理** `127.0.0.1:10808` 在线（v2rayN，机器重启后不自启）

## 2. 必须遵守的操作纪律（都是踩过的坑）

| 纪律 | 原因 |
| --- | --- |
| **跑完冒烟必须 `adb install -r` 重装** | UTP 会卸载 `com.tieba`，之后截图必抓到桌面 |
| **构建与冒烟不许并发** | Gradle 缓存互相破坏 |
| **instrumentation 测试里不许 `force-stop` 被测应用** | 测试就在该进程里，会杀死自己（表现为"0.05s PASS"假通过） |
| **批量改代码用字面 `String.Replace`，不许正则** | `[...]` 会被当字符集，注释会被改坏 |
| **截图落地前必须 `uiautomator dump` 校验内容** | 否则出现"文件名对、内容是桌面/别的页" |
| **替换 `AppScaffold(topBar=…)` 时括号会少一个** | 结构文件按行号删，删完看 diff |
| **manifest 等结构文件按行号删** | 曾一次误删 38 行 |
| **不要用脚本按行切片改 Composable** | 曾三次破坏 `Column` 收尾结构 |

## 3. 关键架构事实（不要重新验证）

- 页面骨架：`GlassPage`（`ui/common/theme/glass/GlassPage.kt`），支持 `title` / `largeTitle` / `actions` / `center` / `bottomBar` / `floating` / `snackbarHost`
- `GlassPage` 是**标题/返回/动作的唯一 owner**；页面 header 只能放 tab 行、搜索框、吧头等"内容性"控件
- 复合 header 迁移前必须给它加 `showTitle` / `showSearchBox` 开关，否则标题会画两次
- `AppScaffold`（`ui/widgets/compose/AppScaffold.kt`）= 自研脚手架（insets + snackbar + FAB），非 M2 派生
- 玻璃控件必须有 `GlasmioBackdropHost` 或 `GlasmioBackdropProvider` 上下文，否则抛 `IllegalStateException`
- **跨 composition / 独立 Window / slot 里不能读 `GlasmioTheme.colors`**（会退回浅色调色板），要用 `AppTokens.*`
- 玻璃表面**不能当容器**：`LiquidGlassSurface` 与 `GlasmioBackdropHost` 都会把外层撑满 → 用 `matchParentSize()` 铺底 + 内容 `wrapContentHeight()`
- insets：页面禁止自己算，用 `GlassPageInsets.contentTopPadding()`；13 处白名单见 `docs/INSETS-WHITELIST.md`

## 4. 测试与验证工具

| 工具 | 用途 |
| --- | --- |
| `PixelRegressionTest#statusBarMatchesPageBackground` | 状态栏 vs 页面背景亮度差（防白条） |
| `PixelRegressionTest#darkModeHasNoLargeWhiteBlocks` | 深色下近白块占比 ≤20% |
| `MainSmokeTest#pageTitlesAreUnique` | 标题节点唯一（防重影） |
| `tools/pixel-diff.py` | 截图基线 diff，支持 ROI（`top_chrome` / `dock` / `bottom_bar`） |
| `tools/measure-ink-gap.py` | 文字墨迹间隙测量（先 dump 取 bounds，再在 bounds 内扫墨迹行） |
| debug 预览入口 | 设置 → 关于组：列表选择 / 文本输入 / 确认弹窗 / 个人主页资料卡（仅 DEBUG） |

## 5. 当前任务队列（按顺序）

### T1. 个人主页资料卡（进行中）
- 头像 80dp 圆形（只改他人主页 `UserProfilePage`，不动其他页）
- **墨迹对齐已收口（2026-11-28，commit 462f4f6）**：行盒压不矮（`lineHeight`/`includeFontPadding`
  都无效），改成 `InkLine` 墨迹盒；三变体 nameTop−avatarTop / avatarBottom−statBottom / Δgap
  全部 ≤1px。实测与选型见 `docs/PROFILE-CARD-INK.md`，别再用公式推 offset。
- 文案：`ID 1234567 · 河北 · 10.1 YEAR`（去掉"IP 属地："与"吧龄…年"）
- 名字字号：22/26/30sp 三变体可切；**推荐 26sp 配 80dp**（差值 1/1/Δ1，间隙 37/36px），待用户确认
- **下一步：关注按钮移到「帖子 / 关注吧」tab 行右侧**：胶囊 32dp 高、16dp 圆角、14dp 水平内边距、
  15sp Medium；三态（未关注 accent 填充 / 已关注描边 / isSelf 编辑资料）
- 验收：`nameTop−avatarTop` 与 `avatarBottom−statBottom` 各 ≤2px；两处 gap 差 ≤2px；
  预览截图含 tab 行与胶囊；胶囊高 ≈88px

### T2. 剩余欠账
- `tools/baseline/profile_*.png` 基线（用 debug 预览拍）
- 连续 3 次全量 13 条冒烟全绿
- `pageTitlesAreUnique` 修 flake（先等标题节点出现，超时失败 + dump）

### T3. 之后按 PLAN §27
统一通知（§20）、空态设计、深色材质配方、菜单形变 —— 这些是 v1.9.3 / 后续。

## 6. 报告风格（用户偏好）

- **诚实优先**：没做到的直接说，不要用"看起来 PASS"糊过去
- 每个结论附**数字或截图**（"看起来差不多"不算）
- 推翻自己上一版结论时，**明确写出"上一版错在哪"**
- 长任务分阶段 commit + push，每阶段编译通过
