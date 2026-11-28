# 资料卡规范（v1.9.2 定稿）

> 这张卡是**未来所有资料卡的模板**：他人主页（已落地）、我的页、吧页共用同一套结构。
> 本文只写规则，不写实现细节；墨迹对齐的实测过程见 `PROFILE-CARD-INK.md`。

## 1. 用途与范围

| 位置 | 状态 | 动作（tab 行右侧） |
| --- | --- | --- |
| 他人主页 `UserProfilePage` | ✅ 已落地 | 关注 / 已关注 |
| 我的页（自己主页，`isSelf`） | 同一组件 | 编辑资料 |
| 吧页（吧资料） | 待落地（下一轮） | 签到 / 已签到 |

三处**共用**：头像尺寸、三行排版、墨迹对齐系数、tab 行 + 右侧动作的位置与尺寸。
各处不同的只有：字段含义、动作文案、动作点击行为。

## 2. 结构

```
┌─────────────────────────────────────────────┐  卡片：白底 / #1C1C1E(深) + 26dp 圆角 + 1dp subtleStroke
│ ╭──────╮  名字 ♀                             │
│ │ 80dp │  ID 1234567 · 河北 · 10.1 YEAR ⧉    │  ← 三行各自装进「墨迹盒」，SpaceBetween 均分
│ ╰──────╯  关注 220  粉丝 0  获赞 9055         │
│ 个性签名…（一行，超出省略）                    │
└─────────────────────────────────────────────┘
  [ 帖子 36 ｜ 关注吧 69 ]            ( 关注 )     ← tab 行：轨道可横向滚动 + 右侧动作胶囊
```

- 卡片外边距 **16dp**（页面左右），内边距 16dp / 12dp，圆角 **26dp**；
- 卡片边界 = 页面底 `groupedBackground`（浅 `#F2F2F7` / 深 `#000000`）+ 卡片 1dp `subtleStroke`；
  **页面根布局必须给 AppScaffold 显式 `backgroundColor = AppTokens.groupedBackground`**
  （它的默认值 `MaterialTheme.colors.background` 在浅色下是纯白，会把分组页灰底盖掉 —— v1.9.2 踩过）；
- 头像：圆形 **80dp**，固定，不做多档；
- 三行：名字（22/26/30sp 可选，SemiBold）、元信息（13sp，次级色）、数据（15sp，标签次级色 + 数值 SemiBold）；
- 简介：15sp，单行省略，宽度占满（动作已不在卡内）。

## 3. 墨迹对齐（硬规则）

- 每行必须包进 `InkLine`（`UserProfileCard.kt`）：外层 Box 高 = **墨迹高**、内层
  `wrapContentHeight(unbounded = true)` + `offset(y = -墨迹顶留白)`；
- 系数（按鸿蒙字族标定，字号 em 的倍数）：

  | 行 | 墨迹高 | 顶留白 |
  | --- | --- | --- |
  | 名字 | 0.94em | 0.09em |
  | 元信息 | 0.94em | 0.09em |
  | 数据 | 0.91em | 0.10em |

- 验收：`nameTop−avatarTop ≤ 2px`、`avatarBottom−statBottom ≤ 2px`、两处墨迹间隙差 ≤ 2px，
  用 `tools/measure-ink-gap.py` 量；
- **不要**试图用 `lineHeight = 字号` 或 `includeFontPadding = false` 压行盒（无效，见
  `PROFILE-CARD-INK.md` §1）；**不要**用 `Modifier.height(墨迹高)` 直接压文字（会裁字）。

## 4. tab 行 + 动作胶囊

- `ProfileHeaderTabs(labels, selectedIndex, onSelected, action = { … })`：
  `Row { 滚动 tab 轨道(weight 1f) ; 8dp ; 动作 }`，左右各 16dp；
- tab 轨道是横向滚动容器（`GlassScrollableTabRow` 内部 `horizontalScroll`；回滚走 M2
  `ScrollableTabRow`），动作是它的**兄弟节点** → tab 再多只在轨道内滚动，不会把动作挤换行；
- 动作胶囊 `ProfileActionPill`：高 **32dp**、圆角 **16dp**、水平内边距 **14dp**、文字 **15sp Medium**；
  - `Follow`：`AppTokens.accent` 填充 + `onAccent` 字；
  - `Following`：透明 + 1dp 描边（主文字色 24%）+ `AppTokens.text` 字；
  - `EditProfile`：与 `Following` 同款胶囊（次要动作不用强调色）。

## 5. 三处的字段映射

| 卡片位 | 他人主页 | 我的页（isSelf） | 吧页 |
| --- | --- | --- | --- |
| 头像 | 用户头像 80dp | 同 | 吧头像 80dp |
| 名字 | `user.name`，空则 `user.nameShow` | 同 | 吧名 |
| 元信息 | `ID {id} · {ip} · {tb_age} YEAR` | 同 | `{关注数} 关注 · {帖子数} 主题`（拟） |
| 数据 | 关注 / 粉丝 / 获赞 | 同 | 关注数 / 回帖数（拟） |
| 简介 | `user.intro` | 同 | 吧简介（拟） |
| 动作 | 关注 / 已关注 | 编辑资料 | 签到 / 已签到（拟） |

## 6. 组件签名草案（下一轮落地我的页/吧页时按这个收口）

```kotlin
/** 一行统计：标签 + 数值，整项可点。 */
data class ProfileStatSpec(val label: String, val value: String, val onClick: (() -> Unit)? = null)

/**
 * 资料卡数据（与业务解耦：字符串/数字由调用方拍平，便于真实页与 debug 预览复用）。
 */
data class ProfileCardSpec(
    val avatar: ProfileAvatar,        // 远程 URL 或本地 drawable（预览用）
    val title: String,                // 名字（已含 nameShow 兜底）
    val sex: Int = 0,                 // 0 无 / 1 ♂ / 2 ♀
    val meta: String,                 // 元信息一行（调用方拼好）
    val stats: List<ProfileStatSpec>,
    val bio: String,                  // 简介一行（空则显示占位文案）
    val trailingAction: ProfileActionSpec? = null,  // null = 不显示（如未登录看他人主页）
)

data class ProfileActionSpec(val action: ProfileAction, val onClick: () -> Unit, val enabled: Boolean = true)
```

> 现状：`UserProfileCardSpec` + `UserProfileCard` + `ProfileActionPill` 已经是这个形状的雏形
> （头像两个字段、`meta` 还是拆开的 `id/ip/tbAge`）。落吧页时按上面收口，别另起一套。

## 7. 验收清单（每处落地都要过）

1. `tools/measure-ink-gap.py` 三项 ≤2px；
2. 动作胶囊像素高 = 32dp × density（本项目 420dpi → **84px**，不是 88px）；
3. 浅色 / 深色截图各一张，卡片边界一眼可见；
4. 空 `name` → 显示 `nameShow`；长名字 → 省略号且不压住 ♂/♀；
5. 全量 13 条冒烟绿。
