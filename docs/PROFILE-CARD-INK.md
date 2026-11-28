# 资料卡「墨迹对齐」实测记录（v1.9.2 任务 #2）

> 目标：个人主页资料卡里三行文字（名字 / ID 行 / 数据行）与 80dp 头像做到
> **名字墨迹顶 = 头像顶、数据墨迹底 = 头像底、两处墨迹间隙相等（各 ≤2px）**。
> 本文记录「为什么按原来的办法做不到」以及最终做法与实测数字，避免后人重走一遍。

## 0. 验收口径

```
nameTop − avatarTop          ≤ 2px
avatarBottom − statBottom    ≤ 2px
|gap(name→id) − gap(id→stat)| ≤ 2px
```

测量工具：`tools/measure-ink-gap.py`（先 `uiautomator dump` 取三处文本节点的 bounds，
再在 bounds 内逐行扫像素找「首墨迹行 / 末墨迹行」，背景色取窗口内出现次数最多的颜色）。
设备：模拟器 1080×2400 / 420dpi（density = 2.625）/ `fontScale = 1`。

## 1. 关键事实：行盒压不矮（这决定了解法）

HarmonyOS Sans SC 的 `ascent + descent ≈ 1.44em`，而 **Compose 单行文本的行盒不会小于字体自然高**。
把 `lineHeight` 设成字号（1.0×）或打开 `PlatformTextStyle(includeFontPadding = false)` 都没用：

| 行 | 字号 | `lineHeight` 设 | 实测行盒 | 折合 |
| --- | --- | --- | --- | --- |
| 名字 | 22sp | 26sp（1.18×，旧样式） | 83px | 1.437em |
| 名字 | 22sp | 22sp（1.0×）+ 去字形留白 | 82px | 1.420em |
| ID 行 | 13sp | 18sp（旧样式） | 49px | 1.436em |
| ID 行 | 13sp | 13sp（1.0×）+ 去字形留白 | 49px | 1.436em |
| 数据行 | 15sp | 20sp（旧样式） | 57px | 1.448em |
| 数据行 | 15sp | 15sp（1.0×）+ 去字形留白 | 56px | 1.422em |

结论：`includeFontPadding = false` 只削掉 1px，`lineHeight = 字号` 完全无效（被钳到字体自然高）。
所以「三行行盒统一成 1.0×字号、SpaceBetween 就自动对齐墨迹」这条路**走不通** —— 行盒之间的
留白差（22sp 的行盒顶留白 15px、13sp 的只有 10px）依然存在，墨迹间隙因此差 4–8px。

### 1.1 也不能靠压高度对齐（会裁字）

试过给每行 `Modifier.height(墨迹高)`：Compose 对 Text 节点**按尺寸裁剪绘制**，
实测名字被切掉 15px 下缘笔画（「特没谱先生」下半截没了）、ID 行与数据行同样被切。
截图：`shots/ink_ink_B.png`（错误示范）。

## 2. 最终做法：自己造「墨迹盒」

`UserProfileCard.kt` 里的 `InkLine`（三层，每层都有原因）：

```
Box(height = 墨迹高)                       // ① SpaceBetween 看到的槽 = 墨迹
  └ Box(wrapContentHeight(unbounded=true)  // ② 让文字按自然行盒（1.44em）测量，避免被裁
        .offset(y = -墨迹顶留白))           // ③ 上移，使墨迹顶贴住槽顶、墨迹底落在槽底
      └ Row { 文字… }
```

墨迹系数（字号 em 的倍数，`InkMetrics`）由下表实测反推：

| 行 | 墨迹高 | 顶留白 | 依据（22sp / 13sp / 15sp） |
| --- | --- | --- | --- |
| 名字 | 0.99em | 0.26em | 墨迹 57px / 行盒 57.75px，顶留白 15px |
| ID 行 | 0.94em | 0.29em | 墨迹 32px / 34.125px，顶留白 10px |
| 数据行 | 0.96em | 0.28em | 墨迹 38px / 39.375px，顶留白 11px |

这样「布局间隙 = 墨迹间隙」，`SpaceBetween` 均分出来的就是视觉间隙；
同时三行合计高度变小（22sp 时约 48dp），**任何名字字号都能塞进 80dp 头像**，
不再出现旧版「26sp/76dp、30sp/80dp 时三行行盒合计超过头像高 → Column 压缩子项、字形被裁」的退化。

## 3. 三变体实测（1080×2400，浅色，debug 预览）

修复前（旧样式，`Arrangement.SpaceBetween` + 自然行盒）：

| 变体 | 名字/头像 | nameTop−avatarTop | avatarBottom−statBottom | gap1 | gap2 | Δgap |
| --- | --- | --- | --- | --- | --- | --- |
| B | 22sp/80dp | +15px | −9px | 32 | 28 | 4 |
| B2 | 26sp/76dp | +19px | −4px | 24 | 18 | 6 |
| B3 | 30sp/80dp | +21px | −1px | 26 | 18 | 8 |

修复后（墨迹盒）：

| 变体 | 名字/头像 | nameTop−avatarTop | avatarBottom−statBottom | gap1 | gap2 | Δgap | 结论 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| B | 22sp/80dp | **0px** | **1px** | 42 | 41 | **1** | 通过 |
| B2 | 26sp/76dp | **1px** | **1px** | 32 | 31 | **1** | 通过 |
| B3 | 30sp/80dp | **1px** | **1px** | 32 | 31 | **1** | 通过 |
| **B2+（实际主页尺寸）** | **26sp/80dp** | **1px** | **1px** | **37** | **36** | **1** | 通过 |

截图：`shots/profile_b_light.png` / `profile_b2_light.png` / `profile_b3_light.png`
（修复前同名图留档在 `shots/profile_before/`）；
26sp/80dp（推荐组合）实测图：`shots/profile_b2_80_light.png`。

## 4. 观感判断（供选型参考）

- **B（22sp）**：三行合计墨迹才 48dp，要塞满 80dp 头像 → 间隙被撑到 42px（16dp），偏松；
  名字与头像相比显得小，卡片上半部分空。
- **B2（26sp/76dp）**：间隙 32px（12dp），名字有分量又不抢戏，与 13sp/15sp 的信息层级差明显。
- **B3（30sp/80dp）**：名字过大 —— 与页面 34sp 大标题接近，长名字更容易被 `Ellipsis` 截断，
  ID 行/数据行在它下面显得碎。

**推荐 B2 的字号（26sp）配 80dp 头像**（真实主页用的就是 80dp）：实测间隙 37/36px（≈14dp），
三个差值都是 1px。既有名字的分量，又比 30sp 更耐长名字。

## 5. 已知局限

- **非 CJK 名字**：墨迹顶留白是按中文字形（字面高 ≈ 全字身）反推的，纯拉丁名（大写顶 = cap height）
  会比头像顶低约 2px（22sp）—— 视觉上仍是「光学顶对齐」，可接受。
- **系统字体放大（`fontScale > 1`）**：墨迹盒按 `LocalDensity` 换算 sp→dp，会与文字同步放大；
  但头像固定 80dp，字体放到 1.3× 以上时三行墨迹合计会超过头像高，`SpaceBetween` 退化为 0 间隙
  （不会裁字，但顶端会溢出头像顶）。是否需要随 fontScale 放大头像，留待选型时定。
- 三行墨迹间隙是**被头像高度决定**的（`(头像高 − 墨迹和)/2`），不是可自由调的 8dp 常量。
