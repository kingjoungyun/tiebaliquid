# v1.9.0 BUG 修复记录

日期：2026-11-27
分支 `glass-ui`，起点 `1669591`(tag v1.9.0)，**当前 HEAD `01373ea`**

| # | 状态 | commit |
| --- | --- | --- |
| BUG 1 大标题画两次 | ✅ 已修 | `a2baa5c` |
| BUG 2 状态栏白色实条 / 分界线 | ✅ 白条已修（4 个 tab 背景）；我的页内容已 edge-to-edge | `3b187f0` + `01373ea` |
| BUG 3 帖子页内容透到状态栏 | ✅ **不是 bug**（用户澄清：这是正确范例） | — |
| BUG 4 深色灰阶混乱 / 开关后白矩形 | ⏳ 未定位 | — |
| BUG 5 首页配图不显示（用户 Android 15/16） | ⏳ 未定位 | — |

---

## BUG 1：`PrefsLargeTitle` 画了两次标题 ✅

**现象**：我的 / 设置 / 屏蔽设置 / 一键签到 页的大标题叠两层；uiautomator 读到两个同 origin 节点
（24sp 的 `[42,288][224,396]`、34sp 的 `[42,288][168,379]`）。

**根因**：删除 `PrefsUiStyle` 时只删了分支条件，旧实现里的第二个 `Text`（24sp `pageTitle`）留在了同一个 `Box` 里。

**修法**：删掉多余 `Text`，只保留 `IosTypography.largeTitle`（34sp）。

---

## BUG 2：状态栏是一条更白的实色条，且与内容有分界线 ✅（背景已修）

**根因（两层）**
1. `MainPage` 把 `AppScaffold` 的 `paddingValues`（top = 状态栏高）整个交给 pager →
   各 tab 页从状态栏**下方**开始画，状态栏那条露出 `MainPage` 的 `GlassAppBackground`（更白）；
2. tab 页根布局再用 `.statusBarsPadding()` 把内容裁在状态栏下方（与帖子页的做法相反）。

**修法**
- `3b187f0`：`MainPage` 只把**底部** inset 交给 pager（`PaddingValues(bottom = …)`）→ 页面从 y=0 绘制，
  自己的 `groupedBackground` 铺到状态栏；
- `01373ea`：`UserPageGlasmio`（我的）照 `GlassPage` 的做法，列表去掉 `statusBarsPadding()`，
  改用 `contentPadding.top = statusBarTop + 8.dp` → 内容可滚到状态栏下方，首项仍从状态栏之下开始。

**验证**：`shots/fix_statusbar_light.png`（状态栏 #F2F2F7，与正文一致，无白条/分界线）、
`shots/fix_statusbar_dark.png`（纯黑贯通）。全量 10 条冒烟绿。

**剩余**：进吧 / 消息 / 搜索 / 历史 / 吧页 等页仍用根布局 `statusBarsPadding()`（只修了背景，内容还不会滚到
状态栏下方）。逐页照 `UserPageGlasmio` 改即可，清单见下：
`NotificationsPage.kt:193`、`SearchPage.kt:267`、`HistoryPage.kt:98`、`ForumGlasmio.kt:324`、
`NavigationComponents.kt:161,315`。

---

## BUG 3：帖子页内容透到状态栏

**结论：不是 bug。** 用户澄清该截图是**正确范例**（edge-to-edge + 状态栏自适应）。不需要改动。

---

## BUG 4：深色灰阶混乱 / 开关后有白矩形 ⏳

**现象**（用户截图）：消息页深色下大标题几乎看不清；卡片/分段控件/dock 灰阶不统一；
主题选择页开关后面有一块白色矩形（玻璃材质没被裁剪）。

**根因假设**
1. **白矩形**：`GlassSurface` 在 Compose 里的圆角是 `shape` 裁剪；若某处用 `Modifier.background(color)`
   而没 `.clip(shape)`，或 `Surface` 的 elevation 阴影层不受 shape 约束，就会出现直角白块。
   重点查 `PrefsSwitch` / `AppThemePage` 的开关行是否在 `LiquidGlassTheme` 之外被绘制。
2. **标题对比度**：深色页大标题用 `GlasmioTheme.colors.content`，而消息页可能用的是外层
   `AppTokens.text`（深色下是灰白），压在纯黑上对比不足——与 v1.9.0 底栏那个 bug 同源：
   **玻璃/深色表面内部的内容色必须取表面提供的自适应色**。
3. **灰阶不统一**：`groupedBackground` / `systemGray5` / `elevatedBackground` 三套灰在深色模式下是否都映射到
   纯黑 + 深灰两档，需要逐页核对（消息页 header 用自己的背景色）。

**下一步**：先在模拟器构造深色复现（系统 `cmd uimode night yes` + App 内深色开关），逐页截图量色值，
再决定改哪个 token。

---

## BUG 5：首页配图不显示（用户 Android 15/16）⏳

**现象**：用户设备上头像正常显示、帖子配图不显示；模拟器（API 34 release）正常。

**已排除**：release 的 R8/资源压缩（同包在模拟器图片正常）。

**根因假设（按可能性）**
1. `PostCard` 图片容器在 **Android 15/16 的 edge-to-edge + 新尺寸测量**下拿到 0 高度（
   `AspectRatio`/`fillMaxWidth` 组合在极小屏或字体放大时塌陷）→ 图片不加载也不显示；
2. `darkenWhenNightMode` 处理在深色下把 alpha 乘成 0（用户可能开着深色）；
3. Sketch 图片加载器在 Android 15 的 `ImageDecoder` 路径上失败（头像走另一条路径/不同 decoder），
   失败后没有占位图 → 显示空白；
4. 用户网络/图床问题（但头像能显示，说明网络通）。

**下一步**：向用户要 ①设备型号 + Android 版本 ②是否深色模式 ③该帖链接；同时在模拟器上用
API 35 镜像复现（`sdkmanager "system-images;android-35;google_apis;x86_64"`），
若复现不了则需要 logcat 里的 `Sketch` 错误行。

---

## 验证汇总

| 项 | 结果 |
| --- | --- |
| 编译 | ✅ |
| 全量 10 条冒烟 | ✅ 10 tests, 0 failures（`shots/smoke/20261127-124938/`） |
| 截图 | `fix_statusbar_light.png` / `fix_statusbar_dark.png` |
| 推送 | ✅ `1669591..01373ea` |

---

## 待续（2026-11-27 下午停点，HEAD `42a6ffb`）

- [x] **BUG 1** 大标题画两次 — `a2baa5c`
- [x] **BUG 2** 状态栏白条 — `3b187f0`（pager 不吃顶部 inset）+ `01373ea`（我的页 edge-to-edge）
- [x] **BUG 5** 深色配图全黑 — `baef50a`（MaskTransformation → Compose 遮罩）
- [x] **BUG 4（部分）** 主题选择页开关白矩形 — `42a6ffb`（`PrefsSwitch` 的玻璃采样层裁成胶囊）
- [ ] **edge-to-edge 剩余 5 处**（做法照 `UserPageGlasmio`：根布局去掉 `statusBarsPadding()`，
      列表 `contentPadding.top = statusBarTop + 顶栏高`，背景铺满整屏）
  - [ ] `ui/page/main/notifications/NotificationsPage.kt:193`（`withStatusBarPadding` 参数）
  - [ ] `ui/page/search/SearchPage.kt:267`
  - [ ] `ui/page/history/HistoryPage.kt:98`
  - [ ] `ui/page/forum/ForumGlasmio.kt:324`
  - [ ] `ui/page/main/NavigationComponents.kt:161` 与 `:315`
- [ ] **深色灰阶 token 对照表**（未做）：抓 `groupedBackground` / `card` / `elevatedBackground` /
      `content` / `secondaryContent` / dock / 分段控件 / 搜索框 的实际色值，判断层级是否满足
      页面 < 卡片 < 浮层，文字对比度 ≥ 4.5:1
- [ ] **BUG 4（深色灰阶不统一）**：消息页大标题对比度、卡片/分段控件/dock 灰阶
- [ ] **深色自查（用户未提）**：帖子页底栏（深色下再确认一次）、设置页六分组卡片对比、我的页账户卡
- [ ] 截图欠账：`bug4_settings_dark.png`、`e2e_notifications.png`、`e2e_search.png`、
      `e2e_history.png`、`e2e_forum.png`

### 环境备注
- 本轮中途 **App 被卸载过**（`pm list packages com.tieba` 为空，需 `adb install -r` 重装），
  重装后 App 内偏好（深色开关等）回到默认值——晚上验证深色问题前记得先重新打开深色。
- 模拟器上系统权限弹窗（"Remember photo locations?"）曾把 App 顶到后台，遇到 uiautomator 抓不到节点时
  先 `input keyevent KEYCODE_BACK` 再重新拉起 App。

---

## 更新（2026-11-27 晚，HEAD `ca58bfd`）

- [x] **edge-to-edge 1/5**：`HistoryPage` 迁 `GlassPage`（`ca58bfd`），截图 `shots/e2e_history.png`
- [x] **深色灰阶 token 对照表**：见 `docs/dark-tokens-table.md`（含 hex 与 WCAG 对比度）
  - 结论：深色下**三套卡色并存**（玻璃 `#1C1C1E` 中性 / M2 `#15202B` 蓝调 / AMOLED `#101010`），
    这是「灰阶不统一」的根源；层级数值有序，主要问题是**色相不一致**
- [ ] edge-to-edge 剩余 4 处（需各自把内联顶栏迁到 overlay，做法同 `HistoryPage` → `GlassPage`）：
  - [ ] `NotificationsPage.kt:193`（**二级页版本**；tab 版本已走 MainTabScaffold overlay，无需改）
  - [ ] `SearchPage.kt:267`
  - [ ] `ForumGlasmio.kt:324`
  - [ ] `NavigationComponents.kt:161` / `:315`
- [ ] 按 `dark-tokens-table.md` 的「问题清单」改卡色（**等设计确认后再动**）
- [ ] 深色自查截图：帖子页底栏 / 设置页六分组 / 我的页账户卡

---

## 更新（2026-11-27 晚 第 2 段，HEAD `eace09d`）

- [x] **深色 token 统一**（BUG 4 主体）：`e37827f` + `eace09d`，详见 `docs/dark-token-change.md`
  - `card` → `#1C1C1E`、`floorCard` → `#2C2C2E`、`text` → `#FFFFFF`；AMOLED 同步；浅色未动
  - 主文字对比度 14.8:1 → **21:1**
- [x] **edge-to-edge 1/5**：`HistoryPage` → `GlassPage`（`ca58bfd`）
- [ ] edge-to-edge 剩余 4 处（每页需把内联顶栏迁成 overlay，做法同 `HistoryPage`）：
  - [ ] `SearchPage.kt:267`
  - [ ] `ForumGlasmio.kt:324`
  - [ ] `NavigationComponents.kt:161` / `:315`
  - [ ] `NotificationsPage` 的**二级页版本**（tab 版已达标）
- [ ] 浅色回归截图补拍（`shots/light_after.png` 那次抓到的是桌面）
- [ ] 深色自查截图：帖子页底栏 / 设置页六分组 / 我的页账户卡
- [ ] `shots/dark_before.png` 没抓到（App 被系统弹窗顶到后台），只有 `settings_after.png`；
      对照可参考 `docs/dark-tokens-table.md` 里的改前 hex 值

---

## 更新（2026-11-27 晚 第 3 段，HEAD `eace09d`）

**本轮没有新 commit**（SearchPage 迁移试了一版、发现结构比 HistoryPage 复杂，回滚了；详见下方结构分析）

### 已补的截图（欠账）
- 浅色回归：`shots/light_home.png` / `light_mine.png` / `light_settings.png` —— 白卡、浅灰底，
  与 token 改动前一致（浅色分支未改，符合预期）
- 深色自查：`shots/dark_thread.png`（帖子页：底栏双胶囊 + 图标数字可见、配图正常显示、主楼卡中性灰）、
  `shots/dark_mine.png`（我的页账户卡）

### SearchPage 的结构分析（为什么不能照 HistoryPage 直接迁）
| 组件 | HistoryPage（已迁） | SearchPage（未迁） |
| --- | --- | --- |
| 顶栏第一行 | 返回 + 删除（可进 `actions`） | 返回（可进 `onBack`） |
| 大标题 | 34sp `Text`（→ `largeTitle`） | 34sp `Text`（→ `largeTitle`） |
| **额外固定区** | 无 | **搜索框（44dp，`SearchTopBar`）+ 条件显示的 `SearchTabRow`** |
| 内容 | `HorizontalPager` | `LazyLoadHorizontalPager` + 无关键字时的建议/历史列表 |

差异点：SearchPage 的搜索框和 tab 行必须在**内容区顶部固定**（不能随列表滚走，否则输入时会被顶掉），
所以迁移后结构应为：

```kotlin
GlassPage(title, onBack, largeTitle) { _ ->
    Column(Modifier.fillMaxSize()) {
        SearchTopBar(...)          // 固定
        if (!isKeywordEmpty) SearchTabRow(...)   // 固定
        Box(Modifier.fillMaxWidth().weight(1f)) { pager / 建议列表 }
    }
}
```

我这一版按此改了，但**括号平衡出错**（原文件里 header 的内层 `Column` 收尾 `}` 与新结构不匹配，
导致 `Box(weight)` 落到 `ColumnScope` 之外、`GlassPage` 也未真正 import），编译失败后已 `git checkout` 回滚。
下次建议：**直接在编辑器里按上面 6 行结构整段替换**（不要用脚本按行号切片），改完立即编译。

### 其它 3 页的结构预判
- `ForumGlasmio.kt:324`：吧页顶栏是**玻璃胶囊 + 吧头卡片**（吧名/关注数/签到按钮），比 SearchPage 更复杂，
  建议先阅读 `ForumGlasmio` 的 header 组合再决定是否迁 `GlassPage`，或只把 `statusBarsPadding()` 换成
  `contentPadding.top`（保留现结构、让列表能滚到状态栏下）。
- `NavigationComponents.kt:161/:315`：这两处是**通用顶栏组件**（被多个页复用），改它们会影响所有使用者，
  需要单独评估。
- `NotificationsPage` 二级页版：结构同 HistoryPage（header = 返回 + 大标题 + `NotificationsHeader` 的 tab 行），
  是最容易的一页，建议下一轮从它开始。

### 环境问题（本轮耗时主因）
- 模拟器上 **App 又被卸载了两次**（`pm list packages com.tieba` 为空），每轮都要 `adb install -r` 重装，
  重装后偏好（深色开关）重置。
- 我的截图脚本有一次把桌面上的 "Gallery" 文字当成帖子标题点了，导致进错 App；
  已改为按 `dumpsys activity` 校验前台进程。

---

## 更新（2026-11-27 晚 第 4 段）：NotificationsPage 迁移受阻，已回滚

**结论：这页不能直接套 HistoryPage 的模板**，原因是 `NotificationsHeader`
（`NotificationsPage.kt:178`）**自己就画了三样东西**：

| 行号 | 内容 |
| --- | --- |
| `:202` | `if (showBack)` 返回钮 |
| `:211` | 搜索钮（`onClick = onSearch`） |
| `:217-219` | **34sp 大标题**（`title_notifications`） |
| 其余 | `GlassTabRow` 分段控件 |

所以用 `GlassPage(title, onBack, largeTitle, actions = 搜索钮)` 包它时：
- 标题出现两次（`GlassPage` 的 `largeTitle` + header 自己的 34sp 标题）
- 搜索钮出现两次（`GlassPage.actions` + header 自己的搜索钮）
实机截图已确认（`shots/e2e_notifications_dark.png` 是失败版本，仅留证据）。
代码已 `git checkout` 回滚，当前 HEAD `eace09d` 干净、编译通过。

### 正确做法（下一轮按这个改，1 页闭环）

给 `NotificationsHeader` 加两个参数，让它只画「该谁画的部分」：

```kotlin
private fun NotificationsHeader(
    ...,
    showTitle: Boolean = true,      // 新增
    showSearch: Boolean = true,     // 新增
)
```
- **二级页**（destination 版）：`showTitle = false, showSearch = false`，
  由 `GlassPage(title, onBack, largeTitle, actions = { 搜索钮 })` 负责标题/返回/搜索，
  header 只留 `GlassTabRow`；
- **tab 版**（`MainTabScaffold`）：保持默认 `true/true`，一行不用改。

这样两个版本共用同一个 header，不会出现双标题/双搜索钮。

### 其它页面的同类风险（先记下来，别再踩）
- `SearchPage`：header 里**也有**搜索框 + 34sp 标题，等同问题 → 迁 `GlassPage` 时同样要加 `showTitle/showSearchBox` 开关；
- `ForumGlasmio`：吧页 header 由「玻璃胶囊 + 吧头卡片」组成，标题不是简单 `Text`，需先看组合；
- `NavigationComponents:161/:315`：通用顶栏组件，被多页复用，改前要统计使用点。

---

## 更新（2026-11-27 晚 第 5 段，HEAD `a71ba3e`）：消息页二级版已闭环 ✅

**做法**（通用规律的第一例）：
1. `NotificationsHeader` 加 `showTitle: Boolean = true` / `showSearch: Boolean = true`，
   并把「返回 + 搜索」那行包进 `if (showBack || showSearch)`；
2. **二级版**传 `showTitle = false, showSearch = false`，标题/返回/搜索交给
   `GlassPage(title, onBack, largeTitle, actions)`；header 只剩 `GlassTabRow`；
3. **tab 版一行未改**（默认值兼容）。

验证：
- 浅色二级页 `shots/notif_secondary_light.png`：单标题、单搜索钮、无白条 ✅
- 浅色 tab 版 `shots/notif_tab_light.png`：无重复标题/搜索钮 ✅
- 编译 ✅ / 全量冒烟 ✅（`shots/smoke/20261127-185519/`）

### ⚠️ 本轮新发现的问题（未修，需单独确认）

**消息 tab 深色下顶部是一块白底**（`shots/notif_tab_dark.png`）：header 区域白底 + 白字「消息」
几乎不可见，而下方列表区是纯黑。特征：
- 只有**深色**出现，浅色正常；
- 白底范围正好是 `MainTopBarHost` 的 overlay 区（状态栏 + 52dp 顶栏 + 标题 + 分段控件那一块）；
- 怀疑 `MainTopBarHost` 的 scrim/底色用了未随深色自适应的颜色（很可能是
  `AppTokens.topBar` 之外的某个 `Color.White.copy(alpha=…)`，或 `MainTopBar` 里写了固定白底）。

**下一步**：查 `MainTopBar.kt` 的 `MainTopBarHost` / scrim 绘制，把颜色换成
`GlasmioTheme.colors.groupedBackground`（或 `AppTokens.background`）后再截一张深色 tab 图。

### 未能验证的验收项
「内容滚到状态栏区域」：消息列表在**未登录**时是空态（只有一个 toast「Not logged in!」），
没有可滚动内容，无法验证。登录后可补；或改用有内容的页面（如浏览记录）作为滚动验证样本
（`HistoryPage` 已经用同样结构验证过滚动）。

### 剩余 edge-to-edge
- [ ] `SearchPage`（同样需要给它的 header 加 `showTitle` / `showSearchBox` 开关）
- [ ] `ForumGlasmio:324`（吧页 header 是胶囊 + 吧头卡片）
- [ ] `NavigationComponents:161/:315`（通用顶栏组件，多页复用）

---

## 更新（2026-11-27 晚 第 6 段，HEAD `027c114`）：深色白底已修 ✅

**根因（像素采样确认）**：tab 版 header 是通过 `MainTabScaffold` 放进 **MainPage 的 overlay 槽**渲染的，
那个 composition **不在本页的 `LiquidGlassTheme` 作用域内** → `GlasmioTheme.colors` 退回**默认浅色调色板**
→ `groupedBackground` = **#F2F2F7**（实测像素 `(242,242,247)`），而标题色 `AppTokens.text` 在深色下是纯白
→ 白底白字。

**修法**：`NotificationsHeader` 的背景改用 **`AppTokens.groupedBackground`**（全局、随明暗自适应：
深色 #000000 / 浅色 #F2F2F7）。浅色值与原来等价，观感不变。

**验证**：深色 tab 像素采样 status/header/body 全部 `(0,0,0)`，标题「消息」白色可见
（`shots/notif_tab_dark_fixed.png`）；全量冒烟绿（`shots/smoke/20261127-190349/`）。

### 通用教训（重要）
> 任何通过 **slot / overlay 跨 composition** 渲染的内容（`MainTabScaffold` 的 topBar 槽、
> `AppScaffold` 的 topBar 槽等）都**拿不到页面自己的 `LiquidGlassTheme`**。
> 这类内容里**不要读 `GlasmioTheme.colors`**（会退回浅色默认值），要用 `AppTokens.*`
> 或显式把 `LiquidGlassTheme` 包在 overlay 层里。

后续可考虑的系统性修法：在 `MainTopBarHost` 里给 slot 包一层 `LiquidGlassTheme`，
这样所有 tab header 的玻璃组件都能拿到正确调色板（本轮先用一行的定点修复）。

---

## 更新（2026-11-27 晚 第 7 段，HEAD `c8fd925`）：系统性修法已落地，**推送失败待补**

**代码已提交（本地 `c8fd925`），但 `git push` 连续 8 次失败**（`Failed to connect to github.com:443` /
`Connection was reset`），`origin/main` 仍停在 `027c114`。网络恢复后需要补推。

### 改了什么
`MainTopBarHost` 在调用 slot 前包一层 `LiquidGlassTheme(accent = AppTokens.primary)`：

```kotlin
CompositionLocalProvider(LocalMainTopBarCollapse provides collapse) {
    LiquidGlassTheme(accent = AppTokens.primary) {
        slot.invoke()
    }
}
```

上一轮的 `AppTokens.groupedBackground` 定点修复**保留**（双保险）。

### 验证
- 深色 tab：status/header/body 像素全部 `(0,0,0)`，标题白色可见，分段控件/搜索钮材质正常
  → `shots/notif_tab_dark_fixed2.png`
- 浅色 tab：`shots/notif_tab_light_fixed2.png`，与修复前一致（无白块、无材质异常）
- 全量冒烟 ✅（`shots/smoke/20261127-191041/`）

### 未做
- `SearchPage` 的 `showTitle` / `showSearchBox` 迁移：本轮未开始（时间用于系统性修 + 验证 + 反复推送）。
- 下一轮建议顺序：① 补推 `c8fd925`；② SearchPage 迁移（同消息页套路）；③ 标题唯一性冒烟断言（lead 安排）。

---

## 更新（2026-11-27 晚 第 8 段，HEAD `30080b4`）

### ✅ 标题唯一性冒烟断言（`3550f64`）
新增 `MainSmokeTest#pageTitlesAreUnique`：开页面 → `device.findObjects(By.text(title))` → 断言
可见区（屏幕上方 70%，排除 dock 同文本标签）里只有 1 个节点。覆盖：我的 / 设置 / 浏览记录 / 消息 tab。
首次跑挂过一次：把「我的页标题」写成了 `title_user`（=「我」，dock 标签），已改为 `mine_title`（=「我的」）。

### ⚠️ `SearchPage` 迁移：**第二次失败，已回滚**
失败现象与上次一致：`Unresolved reference 'GlassPage'` + 一串连带错误（`SearchTopBar` unresolved、
`Assignment type mismatch: Unit vs String`）。这说明**替换后文件结构被破坏**（不只是 import 问题）：
`SearchPage` 的 header 是**内联在函数体内**的三段（返回 Row + 34sp 标题 + 搜索框 Box）+
条件 tab 行，整个函数体在 `LiquidGlassTheme { HarmonyTextProvider { ... } }` 里且缩进不规则；
按块替换时很容易把 `Column` 的收尾和内容 `Box` 的归属弄错。

**下一轮建议**（这次不要再试脚本/整块替换）：
1. 先把 header 抽成独立的 `@Composable private fun SearchHeader(showTitle, showSearchBox, ...)`（照
   `NotificationsHeader` 的样子，**先只做抽取、不改布局**）→ 编译通过、看截图确认无变化；
2. 再按消息页套路：`GlassPage` + `SearchHeader(showTitle=false, showSearchBox=false)` → 编译、截图。

### ✅ 视频帖没封面（`30080b4`）
`PostCard` 之前只读 `video_info.thumbnail_url`，信息流接口经常不返回该字段 → 只有播放键、没有封面。
改为降级链：`thumbnail_url` → 帖子首图（`medias`/正文图）→ 「摄像机 + 视频」占位块。
**未用日志确认字段实况**（本轮信息流滚动 4 屏没碰到视频帖），下次遇到视频帖时补日志确认。

### 验收状态
- 全量冒烟 11 条全绿（`shots/smoke/20261127-194909/`，含新标题断言）
- 推送：`c8fd925..30080b4`（走代理 `127.0.0.1:10808` 成功）

---

## 更新（2026-11-27 晚 第 9 段，HEAD `177e295`）：SearchPage 第 1 步完成 ✅

按批准的方案，只做**纯重构**：把内联的「返回 Row + 34sp 标题 + 搜索框」抽成

```kotlin
@Composable
private fun SearchHeader(
    title: String,
    onBack: () -> Unit,
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onKeywordSubmit: (String) -> Unit,
    glassmio: Boolean,
    showBack: Boolean = true,
    showTitle: Boolean = true,
    showSearchBox: Boolean = true,
)
```

- 三个 `show*` 默认全 `true` → **页面布局一行未变**；
- tab 行仍留在页面里（依赖 `pagerState`），本步不动；
- 编译通过；`search_before.png` / `search_after.png` 视觉一致
  （像素 diff 的差异只在状态栏时钟，属正常）；
- 全量冒烟 **11 条全绿**（`shots/smoke/20261127-201015/`）；
- 已推送 `30080b4..177e295`（走代理）。

### 环境发现（重要，以后不再踩）
`scripts/run-smoke.ps1`（UTP 跑完）**会把 `com.tieba` 一起卸载**——这就是这几次「App 突然消失、
`pm list packages` 为空、截图抓到桌面」的原因。**每次跑完冒烟都要重新 `adb install -r`**，
否则后续 UI 自动化全会失败。

### 下一步（等确认）
第 2 步：`SearchPage` 迁 `GlassPage`（`SearchHeader(showBack = false, showTitle = false)`，
根布局去 `statusBarsPadding()`，验收单标题/单搜索框/滚动穿透）。

---

## 更新（2026-11-27 晚 第 10 段，HEAD `f5393dd`）

### ✅ 第 2 步：`SearchPage` 迁 `GlassPage` 完成（`f5393dd`，edge-to-edge 3/5）
- 外层 `Column` → `GlassPage(title, onBack, largeTitle)`；背景铺满整屏；
- `SearchHeader(showBack = false, showTitle = false)`：只留搜索框，无「双标题/双返回」；
- 根布局去掉 `statusBarsPadding()`（import 一并删除）；
- 截图：`shots/search_page_light.png`（浅色）、`shots/search_page_dark.png`（深色），
  单返回 / 单标题 / 单搜索框、状态栏区域为页面底色；
- 全量冒烟 11 条全绿（`shots/smoke/20261127-202222/`）。

**踩坑根因（前两次「Unresolved reference 'GlassPage'」的真凶）**：
用「字符串包含」判断 import 是否存在 —— `import ...glass.GlassPage` 是
`import ...glass.GlassPageHost` 的**子串**，判断永远为真 → import 从未插入 → 一连串
`Cannot infer type` / `@Composable invocations can only happen…` 的连带错误看起来像括号问题。
**以后加 import 必须整行相等比较。**

### ✅ 第 3 步：`ForumGlasmio:324` 结论 —— **不需要改（假阳性）**
- `:324` 的 `statusBarsPadding()` 挂在 **`ForumTopBar` 顶栏自己身上**，而这个顶栏是
  「透明底 + 三个悬浮玻璃钮（返回/搜索/⋯）、无标题」的 overlay 式顶栏（`ForumGlasmio.kt:304-313` 注释写明）；
- 页面侧 `ForumPage.kt:684` 已经算了 `forumTopBarInset = 状态栏 + 48dp`，注释写着「内容从顶栏下穿过」；
- 实测截图 `shots/forum_scrolled_light.png`：滚动后列表内容从顶栏下方穿过，三个玻璃钮悬浮其上，无白条。

**结论**：这一条是「顶栏自身的 statusBarsPadding」被误当成「页面根布局的 padding」，A（迁 GlassPage）
会凭空多出一个本设计不需要的大标题，B（换内容 padding）也无必要。**不改**，避免制造回归。

### 🧪 环境事实确认（回答 lead 的问题）
`run-smoke.ps1` 跑完后，`adb shell pm list packages com.tieba` **返回空** —— 已复现 4 次，
即 **`connectedAndroidTest`（UTP）跑完会连 `com.tieba` 一起卸载**。请写进 `WORKSPACE.md`：
**每次冒烟后必须 `adb install -r` 重装再截图/自动化**。

### 未做
- 第 4 步 `NavigationComponents:161/:315`（通用顶栏，需先统计调用点）。

---

## 更新（2026-11-27 晚 第 11 段，HEAD `f5393dd`，无新 commit）

### 第 4 步：`NavigationComponents:161 / :315` 判定 —— **假阳性，无需改动**

| 位置 | 所属函数 | 性质 |
| --- | --- | --- |
| `:161` | `NavigationDrawerContent`（`layoutId(HEADER)` 的 `Column` 上） | 常驻抽屉**自身**的头部内边距 |
| `:315` | `NavigationRail`（传给 M2 `NavigationRail(modifier=…)`） | 导航栏**自身**的内边距 |

调用点只有一处：`MainPage.kt:86 / :95` 的 `NavigationWrapper`，两者都在
`AnimatedVisibility(navigationType == PERMANENT_NAVIGATION_DRAWER / NAVIGATION_RAIL)` 里，
即**只在宽窗口（平板/折叠屏）出现**的导航容器，不是页面根布局。
账号图标不钻到状态栏下面正是这两处 padding 的作用 → **与吧页同类，是正确用法**。
**无代码变更。**

### 🚨 顺带发现一个 P0（宽窗口直接崩）—— 不是本轮改动引起

为了截宽窗口的图，用 `adb shell wm size 1800x2400` 把窗口撑到 762dp 宽，App **启动即崩**：

```
java.lang.IllegalStateException: Glass controls must be hosted by GlasmioBackdropHost or GlasmioBackdropProvider
  at io.github.neoruaa.glasmio.glass.GlassBackdropKt.LocalGlassBackdrop$lambda$0(GlassBackdrop.kt:50)
  at io.github.neoruaa.glasmio.glass.GlassSurfaceKt.GlassSurface-HwGDOj4(GlassSurface.kt:1385)
  at com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePageKt.ExplorePage$lambda$0(ExplorePage.kt:133)
```

- `ExplorePage.kt:133` 是一个 `GlassSurface`（探索页顶部的搜索框），它被放进
  `MainTabScaffold(topBar = { … })` 的**顶栏槽**；
- 顶栏槽在没有 `LocalMainTopBarState` 时会走 `MainTabScaffold` 的防御分支
  （`MainTopBar.kt:202-217`：`AppScaffold(topBar = …)`），那条路径**没有 backdrop host** → 崩；
- **与本轮改动无关**（本轮只碰 SearchPage / NotificationsPage / PostCard / AppTokens 颜色 /
  MainTopBarHost 的调色板作用域，都不创建或移除 backdrop host）；`ExplorePage` 最近改动是
  `d28bf41`（TabRow 迁 GlassTabRow）。属于**宽窗口路径一直没被测试**留下的坑。

**修复建议（下一轮/B 线一起做）**：在 `MainTabScaffold` 的顶栏槽渲染处包一层
`GlasmioBackdropHost(sampledColor = AppTokens.groupedBackground)`
（与 BUG 4 里 `PrefsSwitch` 的修法同款），这样槽内任何玻璃控件都有 host，宽窗口不再崩。

证据：`shots/nav_wide.png`（宽窗口下的崩溃弹窗）。设备已 `wm size reset` 恢复竖屏，
App 在竖屏正常启动（`topResumedActivity=com.tieba/...MainActivityV2`）。

---

## 更新（2026-11-27 晚 第 12 段，HEAD `59706dc`）

### ✅ P0 宽窗口崩溃已修（`1f85d25`）
`IllegalStateException: Glass controls must be hosted by …` 在宽窗口（762dp）下由顶栏槽里的玻璃控件触发，
实际有两个崩溃点：`ExplorePage.kt:133`（探索页玻璃搜索框）与 `HomePage.kt:832`（首页顶栏玻璃控件）。
三处兜底（都只在 `LocalLiquidGlassHosted == false` 时生效）：
1. `MainTopBarHost` 渲染 slot 时包 `GlasmioBackdropHost(sampledColor = AppTokens.groupedBackground)`；
2. `MainTabScaffold` 防御分支同理；
3. `ExplorePage` 顶栏搜索框自身也兜底。

验证：`wm size 1800x2400` 下启动不崩、四个 tab 点击后 crash 计数保持 0
（`shots/wide_no_crash.png`）；`wm size reset` 后竖屏正常（`shots/portrait_after_wide_fix.png`）。
**只修崩溃，不做平板/折叠屏布局适配**。

### ✅ B 线第一步：insets 收口（`59706dc`）
新增 `GlassPageInsets`（状态栏 / 顶栏高 / 内容顶距的唯一来源），迁移 7 个文件：
`HistoryListPage` / `HotTopicListPage` / `UserPageGlasmio` / `SubPostsGlasmio` /
`ThreadPage` / `ThreadSkeleton` / `ForumPage`（原 `LocalDensity + getTop + 48.dp` → `contentTopPadding(48.dp)`）。
`SearchPage`/`HistoryPage`/`NotificationsPage` 已由 `GlassPage` 代算，无需改动——这正是目标状态。
验证：编译通过、`shots/mine_after_insets.png` 布局与改前一致、全量冒烟 11 条全绿。

### 宽窗口回归步骤（可复现，写在这里备查）
```
adb shell wm size 1800x2400     # 762dp 宽 = 平板/折叠屏展开
adb shell am start -W -f 0x10008000 -n com.tieba/com.huanchengfly.tieba.post.MainActivityV2
# 期望：能进首页、四 tab 点击不崩（logcat -b crash 无 FATAL EXCEPTION）
adb shell wm size reset         # 必须恢复，否则后续截图全是宽屏
```
**未适配平板布局**：宽窗口下按手机布局拉伸显示，仅保证不崩溃；平板适配不在路线图内。

### 剩余（v1.9.1 候选）
- `NotificationsPage` / `SearchUserPage` / `UserProfilePage` / `LoginPage` / `ReplyPage` 等页面仍有
  `statusBarsPadding()`，属「顶栏自身」用法多为正确，需逐个判定再决定是否收口；
- `GlassPage` 内部还可以把 `contentTopPadding` 暴露给页面自定义顶栏（本轮未做）。

---

## 更新（2026-11-27 晚 第 13 段，HEAD `7cb2647`）：像素断言跑通并完成双向反验证

### 假通过的根因（一句话）
`PixelRegressionTest.relaunchApp()` 里 `am force-stop com.tieba` —— **instrumentation 就在 com.tieba 进程里**，
force-stop 把自己杀了 → UTP 只看到 `Process crashed`，测试方法第一行都没跑到（所以出现「PASS 但 0.054s」）。
`MainSmokeTest` 从不 force-stop（冷启动由 `run-smoke.ps1` 负责），已对齐并写进注释。

### 断言量纲修正
原「状态栏 vs 状态栏下方 200px」在深色下会采到卡片（#1C1C1E）→ 假阳性。
改为「状态栏亮度 vs 页面背景**中位数**亮度」（12 个采样点取中位数，避开照片与卡片），只判「状态栏更亮」方向。

### 双向反验证（均已完成）
| 反验证 | 手法 | 结果 |
| --- | --- | --- |
| 断言有效性 | 阈值改 `LUMA_TOLERANCE=-300` / `WHITE_BLOCK_TOLERANCE=-0.01` | **2 条全红**，消息含页面名与数值 |
| 能抓真实 bug | 临时把深色 `groupedBackground` 改 `Color.White` | `darkModeHasNoLargeWhiteBlocks` **红**：我的页近白 63% > 20% |
| 恢复后回归 | 还原阈值与代码 | 两条绿 |

### 真实耗时（证明在真跑）
- `statusBarMatchesPageBackground` **27.7s**（5 页采样，日志 `首页 状态栏=#F2F2F7(L=242) 页面背景=#E6E6EA(L=242) Δ=0`）
- `darkModeHasNoLargeWhiteBlocks` **49.2s**（切深色并回读 checked，5 页近白 0.000~0.014）
- **全量 13 条全绿**（`shots/smoke/20261127-212216/`）

### 顺带
`scripts/run-smoke.ps1` 的 `-Only` 现在支持短类名（自动补 `com.huanchengfly.tieba.post.smoke.` 前缀）。
注意：`scripts/` 与 `docs/` 都在仓库外，不随 git 提交。
