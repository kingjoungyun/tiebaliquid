# Tiebaliquid v1.9.0

> 一个把液态玻璃（iOS 26/27 观感）带到 Android 的第三方贴吧客户端。
> 骨架与组件层为自研实现（`GlassPage` / `GlassScaffold` / `GlassTabRow` / `GlassTextField` /
> `GlassSnackbar` / `GlassListCard` 等），玻璃材质来自 Glasmio（`RuntimeShader` 采样 + 模糊 + 折射）。

## 亮点

### 信息流

- **卡片统一**：首页 / 吧页 / 收藏 / 话题榜共用同一个 `PostCard`
  - 视频帖显示封面 + 播放键（点击进楼）
  - 多图帖为横向轮播，右上角 `1/N` 计数
  - 富文本正文（加粗/表情/链接）统一渲染，超过 6 行折叠 + 「展开」
  - 引用帖（转发）显示原帖标题 / 作者 / 摘要 / 缩略图

### 导航与结构

- **设置页六分组**（§21.3）：账号 / 安全与隐私 / 外观 / 阅读 / 通用 / 关于
  - 原「个性化」「使用习惯」「更多」三个二级页的条目全部下沉到设置主页，偏好 key 未改（旧设置继续生效）
  - 删除 5 个失效开关（顶栏颜色跟随主题、隐藏首页「动态」、跟随系统夜间、应用图标动态取色、夜间模式主题）
- **「我的」页重排**（§21.2）：账户卡 → 我的内容（帖子 / 回帖 / 收藏 / 关注的吧 / 浏览记录）→ 工具 → 应用
  - 补齐「我的帖子」「我的回帖」「关注的吧」三个直达入口（旧版只能从工具箱进）
- **检查更新 API 化**：直接读 GitHub Releases 的 `tag_name` 与版本号比较，有新版弹提示并可跳转下载页；
  失败时给出短提示，不再打开网页

### 删除 M2 分叉（表现层收口）

- 删除自研的 M2 **Switch / Tabs / TextField / Toolbar / Scaffold** 分叉，全部改用官方组件 + 玻璃包装：
  - `GlassPage` 承担页面骨架（大标题、悬浮返回、顶栏胶囊、snackbar 槽）
  - `AppScaffold`（原 `MyScaffold`）→ 逐页迁移到 `Column` / `GlassPage`（进行中）
  - `TitleCentredToolbar` 及 `Toolbar.kt` 中的旧工具栏定义**已删除**；剩余的图标小件迁到 `GlassIcons.kt`
- 11 个页面级 `*UiStyle` 回滚开关已删 4 个（Personalized / Toolbox / Forum / Analysis / HotTopic / UserPage / Prefs），
  其余 7 个推迟到 v1.9.1

### 字形与视觉修正

- 统一 HarmonyOS Sans SC 子集，修正若干处行高与字重（大标题 34/41 w700、列表行 17sp w590）
- 浅色与 AMOLED 纯黑两套令牌（`AppTokens` / `GlasmioTheme.colors`）

### 清理

- **删除壁纸链路**：`TranslucentThemeActivity` / `WallpaperAdapter` / `TranslucentThemeColorAdapter` /
  `LiteApi` + `LiteApiInterface` + 布局文件 + manifest 注册，彻底移除对
  `huancheng65.github.io/TiebaLite/wallpapers.json` 的外部依赖（半透明主题已下线）
  - 旧主题字符串（`translucent_*`）的解析保留在 `ThemeUtil.isTranslucentTheme(...)`，老用户的主题值不会崩

## 安装

| 文件 | 适用 |
| --- | --- |
| `Tiebaliquid-1.9.0-release.apk` | Android 13（API 33）及以上 |

```bash
adb install -r Tiebaliquid-1.9.0-release.apk
```

- 包名 `com.tieba`，**可覆盖安装** Aur5411 的 TiebaApp
- 与 TiebaLite（`com.huanchengfly.tieba.post`）是两个独立 App，数据不互通
- 首次使用需自行登录贴吧账号

## 已知限制

- **仅支持 Android 13+**：液态玻璃依赖 `RuntimeShader`（API 33）
- **`Dialogs.kt` 仍是 Material 2 基类**：弹窗尚未换成玻璃外观，计划 v1.9.1 处理
- **7 个页面仍有 LEGACY 分支**：Home / Explore / Search / Notifications / History / SearchUser / UserProfile
  的 `*UiStyle` 回滚开关仍在，计划 v1.9.1 清理
- **部分长尾页面仍是 Material 3 外观**：屏蔽词管理、关注列表、账号管理等
- **部分悬浮钮为「自采样」玻璃**：采样纯色而非真实背景，观感为半透明 + 描边
- **未登录相关流程验证不足**：发帖、楼中楼发送、编辑资料保存等需要账号的操作未实测

## 校准说明

- 这是**个人学习性质**的项目，非百度官方客户端
- 使用 GPL-3.0，源码可自由查看与修改；请遵守贴吧用户协议与当地法律法规
- 反馈问题请附：机型、Android 版本、复现步骤、截图

## 致谢

TiebaLite（HuanCheng65）、TiebaApp（Aur5411）、Glasmio（neoruaa）、Phosphor Icons、HarmonyOS Sans、FlClash 的 prism_ui（设计参照）。
