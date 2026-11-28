# 组件契约（写新页面 / 改组件前必读）

这份文档记录 v1.9.0 → v1.9.1 期间踩过的坑，收敛成**硬约定**。违反这些约定的代码几乎都出过 bug。

---

## 1. 颜色来源：`AppTokens` vs `GlasmioTheme.colors`

| 场景 | 用什么 | 为什么 |
| --- | --- | --- |
| 页面 / 普通 Compose 内容的文字、背景、分隔线 | **`AppTokens.*`** | 它是全局、随明暗自适应的运行时令牌（深色 `#000000` / 浅色 `#F2F2F7`），不依赖 composition 作用域 |
| **在 `GlassSurface` / `GlassCard` 内部** | **`LocalContentColor.current`**（Glasmio 提供的自适应色）或 `LocalGlassColors.current` | `GlassSurface` 会按采样到的背景亮度自适应前景色；此时再读外层 `GlasmioTheme.colors` → 深色背景下用深色文字 → 看不见 |
| 玻璃页里的普通文字（页面背景之上、玻璃之外） | `GlasmioTheme.colors.content` / `.secondaryContent` 可以，但**必须在 `LiquidGlassTheme` 作用域内** | 见下一条 |

### 硬规则
- **跨 composition 渲染的内容（slot / overlay）里禁止读 `GlasmioTheme.colors`**。
  `MainTabScaffold` 的 `topBar` 槽、`AppScaffold` 的 `topBar` 槽都由别的 composition 渲染，
  拿不到页面自己的 `LiquidGlassTheme`，颜色会退回**默认浅色调色板** ——
  这就是 v1.9.0「消息 tab 深色下顶部白底、白字看不见」的根因。改用 `AppTokens.*`。
- **玻璃控件必须有 backdrop host**：`GlassSurface` / 部分 `GlassIconButton` 在没有
  `GlasmioBackdropHost` / `GlasmioBackdropProvider` 时会抛
  `IllegalStateException: Glass controls must be hosted by …`（宽窗口下崩过一次）。
  兜底写法：

  ```kotlin
  if (LocalLiquidGlassHosted.current) {
      content()   // 已在 host 内：采样真实页面
  } else {
      GlasmioBackdropHost(sampledColor = AppTokens.groupedBackground) { content() }
  }
  ```

## 2. Slot / overlay 作用域

- 顶栏槽的宿主是 `MainTopBarHost`（主界面）与 `AppScaffold`（单页/防御分支）；
- 宿主现在会补两层：`LiquidGlassTheme`（调色板）+ `GlasmioBackdropHost`（host 兜底）；
- 因此**槽内容可以安全使用玻璃控件**，但仍要遵守「不要读外层 `GlasmioTheme.colors`」；
- 槽内容的高度会被 `MainTopBarState` 记录并用于计算内容顶部留白，**不要在槽里做无限高度布局**。

## 3. Insets 规则

- 页面顶部 insets 的**唯一来源**是 `GlassPageInsets`（`ui/common/theme/glass/GlassPageInsets.kt`）：
  `statusBarTop()` / `topBarHeight` / `contentTopPadding()`；
- **页面根布局禁止 `statusBarsPadding()`**；它只允许出现在顶栏、抽屉、导航栏、悬浮控件**自身**上
  （完整白名单见 `docs/INSETS-WHITELIST.md`，代码里每处都有 `// insets：…勿改` 注释）；
- 用 `GlassPage` 的页面**什么都不用算**：骨架会把 content padding 算好传进来；
- 自绘顶栏的页面：列表 `contentPadding.top = GlassPageInsets.contentTopPadding()`，
  这样内容能滚到顶栏下方而不是被裁断。

## 4. 测试纪律

- **`@Before` 里绝对不能 `am force-stop <被测包>`**：instrumentation 就跑在被测包的进程里，
  force-stop 会把自己杀掉，UTP 只报 `Process crashed`，测试方法第一行都执行不到
  （表现为「PASS 但 0.05s」的假通过）。冷启动交给 `run-smoke.ps1`。
- 断言失败必须**传播**：不要用 `runCatching { assertX() }` 包断言。
- 新增断言要做**双向反验证**：① 故意让阈值必然失败 → 必须红；② 注入真实 bug → 必须红。

## 5. 构建 / 运行

- 构建走 `Invoke-AppBuild`（先 dot-source `scripts/env.ps1`）；**构建与冒烟必须串行**，
  并发会让 Gradle 缓存互相踩（`Could not get file mode for …`）；
- **冒烟跑完 `com.tieba` 会被卸载**（UTP 行为），继续截图前要重新 `adb install -r`；
- 截图前用 `dumpsys activity activities | Select-String topResumedActivity` 确认前台是 `com.tieba`。

## 6. 玻璃表面不能当「容器」用（弹窗那次踩的坑）

`LiquidGlassSurface` / `GlassComponentSurface` 如果被当成容器（**内容写在它的 content lambda 里**），
它会被外层约束撑满可用高度 —— 在弹窗里的表现就是「卡片一屏高 + 内容与按钮之间一大片空白」。

正确写法（`Dialog` / `GlassDialogCard` 现在都是这样）：

```kotlin
Box(modifier = Modifier.wrapContentHeight()) {          // 高度由内容决定
    LiquidGlassSurface(Modifier.matchParentSize(), shape = cardShape) {}   // 只铺底，不参与测量
    Box(Modifier.matchParentSize().background(readableTint, cardShape))    // 可读性实底
    Column(Modifier.wrapContentHeight()) { /* 真正的内容 */ }
}
```

规则：
- **玻璃表面 + 可读性实底一律 `matchParentSize()`**；
- 决定高度的那层必须是 `wrapContentHeight()` 的普通 `Column`/`Box`；
- 弹窗里还要给内容补 `GlasmioBackdropHost`（独立 Window 不在页面 host 里）。
