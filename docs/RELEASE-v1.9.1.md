# v1.9.1 发布说明

**定位：本版无新功能。** 只修 v1.9.0 暴露出来的问题，并把开发端的护栏补齐。

---

## 修复（用户可见）

| 问题 | 说明 |
| --- | --- |
| 深色下「灰阶不统一」 | 深色卡片从蓝调 `#15202B` / `#1A2A39` 统一到 iOS 中性色板 `#1C1C1E` / `#2C2C2E`，主文字改纯白（对比度 14.8:1 → 21:1）；AMOLED 同步 |
| 消息 tab 深色下顶部白块、标题看不见 | 顶栏槽是跨 composition 渲染的，拿不到页面的 `LiquidGlassTheme` → 颜色退回浅色。槽宿主统一补调色板作用域 + backdrop host，header 背景改用 `AppTokens.groupedBackground` |
| 宽窗口（平板/折叠屏展开）启动崩溃 | 顶栏槽里的玻璃控件没有 backdrop host → `IllegalStateException`。三处兜底（仅在无 host 时生效）。**只修崩溃，不做平板布局适配** |
| 视屏帖没有封面 | 信息流接口经常不返回 `video_info.thumbnail_url`，降级链：封面 → 帖子首图 → 「摄像机 + 视频」占位块 |
| 开关后面一块白矩形 | 玻璃采样层是矩形，比胶囊大出的直角会露出；host 裁成胶囊形状 |
| 夜间模式图片蒙版把图弄成全黑 | 放弃 `MaskTransformation`，改在 Compose 层叠半透明黑；夜间压暗仍然生效 |
| 大标题重复渲染 | `PrefsLargeTitle` 里同一段 24sp `Text` 被画了两次 |
| edge-to-edge 收尾 | 浏览记录 / 消息二级页 / 搜索页迁到 `GlassPage`（overlay 顶栏，内容可滚到状态栏下方）；吧页与导航栏经判定属「顶栏自身 padding」，不改 |

## 工程化（开发端，不影响用户）

- **insets 收口**：新增 `GlassPageInsets`（状态栏 / 顶栏高 / 内容顶距的唯一来源），7 个自绘顶栏页面改为调用它；
  `docs/INSETS-WHITELIST.md` 登记全部 13 处 `statusBarsPadding()` 并逐处加注释，**页面根布局违规 0 处**。
- **两条像素级回归断言**（`PixelRegressionTest`）：
  - `statusBarMatchesPageBackground`：状态栏亮度 vs 页面背景中位数亮度，抓「白条/分界线」；
  - `darkModeHasNoLargeWhiteBlocks`：深色下主体区近白像素占比 ≤ 20%，抓「深色灾难」。
  - 两条都做过双向反验证（强制失败 + 注入真实 bug），全量冒烟 13 条。
- **截图基线**（`tools/baseline/` 12 张，6 页 × 浅深）+ `tools/pixel-diff.py` 带 ROI：
  信息流只比顶栏/dock/底栏，纯色页整屏比。
- 文档：`docs/COMPONENT-CONTRACT.md`（颜色来源 / slot 作用域 / insets / 测试纪律）、
  `docs/MIGRATION-CHECKLIST.md`（迁页 5 步 + 历史坑）、`docs/VISUAL-BASELINE.md`。
- 仓库内新增 `tools/`（脚本快照）与 `docs/`（文档快照）。

## 已知问题（未修）

- **未登录场景**：消息、收藏、关注列表为空，相关页面在未登录下无法完整验证。
- **两种「封面/图片」未验证的边界**：视频帖降级链没有用日志确认 `thumbnail_url` 到底是否为空（信息流随机，未遇到视频帖）。
- **平板/折叠屏布局未适配**：宽窗口只保证不崩溃，按手机布局拉伸显示。

## 升级说明

- minSdk 33、版本号 `1.9.1 (40)`。
- 从 1.9.0 升级无数据迁移；深色/浅色偏好保留。
