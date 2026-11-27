# Insets 白名单（`statusBarsPadding()` 允许出现的位置）

> 规则：**页面根布局禁止**用 `statusBarsPadding()`；它只允许出现在「顶栏 / 抽屉 / 导航栏 / 悬浮控件」**自身**上。
> 页面需要给顶栏让位时，用 `GlassPageInsets.contentTopPadding()`（或直接用 `GlassPage`，骨架会代算）。
>
> 代码里每个合法位置上方都有一行注释：`// insets：这里属于【顶栏/悬浮控件自身】，勿改`。
> 新增用法若不在下表内，视为不合规，需要先在本文件登记理由。

## 白名单

| 文件:行 | 所属 | 判定 | 理由 |
| --- | --- | --- | --- |
| `GlassPage.kt:292` | `GlassPage` 的 overlay 顶栏 | 保留 ✔ | 骨架顶栏自身；页面无需再算（B 线的目标状态） |
| `ForumGlasmio.kt:324` | `ForumTopBar`（透明顶栏 + 3 个悬浮玻璃钮） | 保留 ✔ | 顶栏自身；列表用 `contentTopPadding` 让位 |
| `LoginPage.kt:121` | 登录页「大标题 + 悬浮返回」头部 | 保留 ✔ | 头部容器，不是页面根布局 |
| `NavigationComponents.kt:161` | `NavigationDrawerContent` 头部 | 保留 ✔ | 常驻抽屉自身（仅宽窗口） |
| `NavigationComponents.kt:315` | `NavigationRail` | 保留 ✔ | 导航栏自身（仅宽窗口） |
| `NotificationsPage.kt:214` | `NotificationsHeader`（`withStatusBarPadding` 开关） | 保留 ✔ | tab 版 header 自身；二级版传 `false`，由 `GlassPage` 负责 |
| `UserPageGlasmio.kt:329` | `PullRefreshIndicator`（`align(TopCenter)`） | 保留 ✔ | 悬浮指示器自身，不能钻到状态栏里 |
| `PhotoViewActivity.kt:274` | 全屏看图的悬浮返回钮 | 保留 ✔ | 悬浮控件自身 |
| `ReplyPage.kt:499` | 回复页头部 Row（非 dialog 形态） | 保留 ✔ | bottom sheet 会顶到屏幕最上沿，头部自身需要内边距 |
| `SearchUserPage.kt:163` | 查人页头部 Column（背景 + 顶部内边距） | 保留 ✔ | 头部容器 |
| `SubPostsGlasmio.kt:87` | 子帖页顶栏 Box（48dp） | 保留 ✔ | 顶栏自身；列表已用 `contentTopPadding` |
| `UserProfilePage.kt:581` | 用户主页 `topBar` 槽 | 保留 ✔ | 顶栏槽自身 |
| `EditProfileActivity.kt:506` | 编辑资料页头部 Row（52dp） | 保留 ✔ | 头部容器 |

**当前「页面根布局」违规：0 处。**

## 已收口（历史记录，供回溯）

| 曾经的位置 | 处理 |
| --- | --- |
| `HistoryPage` 根布局 | 迁 `GlassPage`，删除根 `statusBarsPadding()` |
| `NotificationsPage`（二级版）根布局 | 迁 `GlassPage` + `NotificationsHeader(withStatusBarPadding=false)` |
| `SearchPage` 根布局 | 迁 `GlassPage` + `SearchHeader(showBack=false, showTitle=false)` |
| `HistoryListPage` / `HotTopicListPage` / `UserPageGlasmio` / `SubPostsGlasmio` / `ThreadPage` / `ThreadSkeleton` / `ForumPage` 的状态栏计算 | 统一走 `GlassPageInsets.statusBarTop()` / `contentTopPadding()` |

## 自查命令

```powershell
# 列出所有 statusBarsPadding 使用点（应与上表一一对应）
git grep -n "statusBarsPadding" -- app/src/main/java | Select-String -NotMatch "^.*import "

# 页面层是否还有直接读 WindowInsets.statusBars（除白名单外应为空）
git grep -n "WindowInsets.statusBars" -- app/src/main/java
```
