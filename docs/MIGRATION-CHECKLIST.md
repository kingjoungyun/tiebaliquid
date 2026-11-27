# 迁页 checklist（把一页迁到玻璃 / edge-to-edge）

按顺序做，**一页一 commit**，每步编译。历史上几次翻车都发生在「跳步」或「脚本批量改结构」时。

---

## 0. 先读结构，别急着改

- [ ] 这一页的 header 是**纯装饰层**，还是**自带标题/动作的复合组件**？
      （`NotificationsHeader` 自带返回钮 + 搜索钮 + 34sp 标题，直接套 `GlassPage` 会出现双标题/双按钮）
- [ ] header 里有哪些**固定区**（搜索框、分段控件）？它们应该留在内容区顶部，还是交给骨架？
- [ ] 页面有没有 `LazyColumn` / `Pager`？内容区拿到的 `PaddingValues` 要传给谁？

## 1. 抽 header（纯重构，先不改布局）

- [ ] 把内联的「返回 Row + 大标题 + 搜索框/tab」抽成 `private fun XxxHeader(...)`
- [ ] 加开关参数：`showBack = true`、`showTitle = true`、`showSearch = true`（默认值保证**调用方不用改**）
- [ ] 编译 + 截图对比：**布局必须与改前一模一样**（像素 diff 只允许状态栏时钟差异）
- [ ] 单独 commit

## 2. 迁 `GlassPage`

```kotlin
GlassPage(
    title = …,                       // 顶栏胶囊标题（收起时显示）
    onBack = { navigator.navigateUp() },
    largeTitle = …,                  // 34sp 大标题（展开时显示）——两个都传，别只传一个
    actions = { /* 右侧动作钮 */ },
    snackbarHost = { GlassSnackbarHost(hostState) },
) { _ ->
    Column(Modifier.fillMaxSize()) {
        XxxHeader(showBack = false, showTitle = false)   // 只留固定区
        Box(Modifier.fillMaxWidth().weight(1f)) { /* pager / 列表 */ }
    }
}
```

- [ ] `title` 与 `largeTitle` **都要传**（只传 `title` 时大标题不显示、只传 `largeTitle` 时胶囊标题空着）
- [ ] 页内 header 传 `showBack = false, showTitle = false`
- [ ] 删掉页面根布局的 `statusBarsPadding()`（import 一并删）
- [ ] 背景用 `AppTokens.groupedBackground`（跨 composition 也正确）
- [ ] 编译 + 浅色/深色各一张截图

## 3. 验收（缺一不可）

- [ ] **单标题 / 单返回 / 单搜索框**：截图确认（或跑 `MainSmokeTest#pageTitlesAreUnique`）
- [ ] **状态栏区域无白条**：跑 `PixelRegressionTest#statusBarMatchesPageBackground`
- [ ] **深色无白块**：跑 `PixelRegressionTest#darkModeHasNoLargeWhiteBlocks`
- [ ] 内容能滚到状态栏/顶栏下方（有内容时向下滚一点截图）
- [ ] 全量冒烟绿

## 4. 常见坑

| 症状 | 原因 | 处理 |
| --- | --- | --- |
| `Unresolved reference 'GlassPage'` + 一堆 `Cannot infer type` | import 没真加进去（用字符串包含判断时 `GlassPageHost` 的子串会误判） | 判断 import 用**整行相等** |
| 双标题 / 双搜索钮 | header 自带这些又用 `GlassPage` 的 | header 传 `show* = false` |
| 深色下 header 白底白字 | header 读了 `GlasmioTheme.colors`（跨 composition 退回浅色） | 改 `AppTokens.*` |
| 内容被裁在状态栏下 | 根布局还留着 `statusBarsPadding()` | 删掉，用 `GlassPage` 的 padding |
| 括号不平衡 | 用脚本按行号切片替换整块 | **整段文本替换**，别切行号 |

## 5. 收尾

- [ ] 把这一页在 `docs/INSETS-WHITELIST.md` 里登记（如果是新增 `statusBarsPadding()` 白名单）
- [ ] commit message 写清「edge-to-edge N/5」进度
- [ ] 同步仓库快照（`tools/`、`docs/`）
