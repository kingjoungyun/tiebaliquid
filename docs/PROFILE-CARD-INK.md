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
再在 bounds ±2px 内逐行扫像素找「首墨迹行 / 末墨迹行」，背景色取窗口内出现次数最多的颜色）。
设备：模拟器 1080×2400 / 420dpi（density = 2.625）/ `fontScale = 1`。

## 1. 关键事实：行盒压不矮

**HarmonyOS Sans SC 的 ascent 0.928em + descent 0.244em = 1.172em 就是行盒下限**，
Compose 单行文本按字体自然高排版，`lineHeight` 小于它时被钳住：

| 字号 | `lineHeight` 设 | 实际行盒 |
| --- | --- | --- |
| 22sp | 22sp（1.0×，58px） | 68px = 1.172em |
| 13sp | 13sp（34px） | 40px = 1.172em |
| 15sp | 15sp（39px） | 48px = 1.172em |

`includeFontPadding = false` 只削掉 1px（`tight()` 保留它只为这点收益 + 可预测性）。
行盒压不矮 → `SpaceBetween` 均分的仍是「行盒间隙」，三行留白不同 → 墨迹永远不齐。

### 1.1 也不能靠压高度对齐（会裁字）

试过给每行 `Modifier.height(墨迹高)`：Compose 对 Text 节点**按尺寸裁剪绘制**，
实测名字被切掉 15px 下缘笔画（「特没谱先生」下半截没了），ID 行与数据行同样被切。
截图：`shots/ink_ink_B.png`（错误示范）。

### 1.2 ⚠️ 最容易翻的车：预览和真实页不是同一个字体

真实主页在 `GlassPage` 里 → `HarmonyTextProvider` 注入鸿蒙字族（行盒 1.172em）；
debug 预览弹窗如果**没包** `HarmonyTextProvider`，会退回系统兜底 CJK 字体
（行盒 1.42em、墨迹系数完全不同）。第一版系数就是按兜底字体量的，结果：

- 预览：0px / 1px / Δ1（看起来完美）；
- 真实页：**−5px / +10px**（用同一份代码）。

修法：`DebugDialogPreview` 里给资料卡包上 `HarmonyTextProvider`，两边同字体。
现在预览与真实页的测量结果逐项一致（见 §3）。

## 2. 最终做法：自己造「墨迹盒」

`UserProfileCard.kt` 里的 `InkLine`（三层，每层都有原因）：

```
Box(height = 墨迹高)                       // ① SpaceBetween 看到的槽 = 墨迹
  └ Box(wrapContentHeight(unbounded=true)  // ② 让文字按自然行盒（1.172em）测量，避免被裁
        .offset(y = -墨迹顶留白))           // ③ 上移，使墨迹顶贴住槽顶、墨迹底落在槽底
      └ Row { 文字… }
```

墨迹系数（字号 em 的倍数，`InkMetrics`）由实测反推：

| 行 | 墨迹高 | 顶留白 | 依据（鸿蒙字族） |
| --- | --- | --- | --- |
| 名字 | 0.94em | 0.09em | 22sp：墨迹顶缩进 5px=0.087em、墨迹高 0.935em |
| ID 行 | 0.94em | 0.09em | 13sp：3px=0.088em、0.938em |
| 数据行 | 0.91em | 0.10em | 15sp：4px=0.102em、0.914em |

这样「布局间隙 = 墨迹间隙」，`SpaceBetween` 均分出来的就是视觉间隙；
三行合计墨迹高只有 ~47dp（22sp 时），**任何名字字号都塞得进 80dp 头像**，
不再出现旧版「26sp/76dp、30sp/80dp 时三行行盒合计超过头像高 → Column 压缩子项」的退化。

## 3. 三变体实测（1080×2400，浅色，debug 预览，鸿蒙字族）

修复前（旧样式，`SpaceBetween` + 自然行盒；那轮预览还没补 `HarmonyTextProvider`）：

| 变体 | 名字/头像 | nameTop−avatarTop | avatarBottom−statBottom | gap1 | gap2 | Δgap |
| --- | --- | --- | --- | --- | --- | --- |
| B | 22sp/80dp | +15px | −9px | 32 | 28 | 4 |
| B2 | 26sp/76dp | +19px | −4px | 24 | 18 | 6 |
| B3 | 30sp/80dp | +21px | −1px | 26 | 18 | 8 |

修复后（墨迹盒 + 鸿蒙字族系数）：

| 变体 | 名字/头像 | nameTop−avatarTop | avatarBottom−statBottom | gap1 | gap2 | Δgap | 结论 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| B | 22sp/80dp | **0px** | **1px** | 44 | 44 | **0** | 通过 |
| B2 | 26sp/76dp | **−1px** | **1px** | 35 | 34 | **1** | 通过 |
| B3 | 30sp/80dp | **−1px** | **1px** | 35 | 34 | **1** | 通过 |
| **B2+（推荐）** | **26sp/80dp** | **−1px** | **1px** | **40** | **39** | **1** | 通过 |

**真实主页复核**（`shots/real_h2.xml/png`，用户「我也想多存点分」，CJK 名字）：

| 位置 | nameTop−avatarTop | avatarBottom−statBottom | gap1 | gap2 | Δgap |
| --- | --- | --- | --- | --- | --- |
| 真实主页 22sp/80dp（页面默认） | **0px** | **1px** | 44 | 44 | **0** |

与预览逐项一致 —— 这也验证了 §1.2 的结论（同字体才会一致）。

截图：`shots/profile_b_light.png` / `profile_b2_light.png` / `profile_b3_light.png` /
`profile_b2_80_light.png` / `profile_real_cjk_light.png`（修复前同名图留档在 `shots/profile_before/`）。

## 4. 观感判断（供选型参考）

- **B（22sp）**：三行墨迹合计 ~47dp，要塞满 80dp 头像 → 间隙被撑到 44px（17dp），偏松；
  名字与头像相比显得小，卡片上半部分空。
- **B2（26sp/76dp）**：间隙 35px（13dp），名字有分量又不抢戏。
- **B3（30sp/80dp）**：名字过大 —— 与页面 34sp 大标题接近，长名字更容易被 `Ellipsis` 截断，
  ID 行/数据行在它下面显得碎。

**推荐 26sp 配 80dp 头像**（真实主页用的就是 80dp）：实测间隙 40/39px（≈15dp），
三个差值都是 1px。既有名字的分量，又比 30sp 更耐长名字。

## 5. 已知局限

- **非 CJK 名字**：系数按中文字形（字面高 ≈ 全字身）反推；纯拉丁名的字面顶比中文低约 5px
  （22sp），即 `nameTop−avatarTop` 会变成约 +5px。视觉上仍是「光学顶对齐」，但过不了 2px 断言。
  中文贴吧场景下可接受；真要覆盖拉丁名，得按字符串实测墨迹（`Paint.getTextBounds`）。
- **系统字体放大（`fontScale > 1`）**：墨迹盒按 `LocalDensity` 换算 sp→dp，会与文字同步放大；
  但头像固定 80dp，`fontScale` 到 1.5× 左右三行墨迹合计会超过头像高，`SpaceBetween` 退化为
  0 间隙（不裁字，但顶端会溢出头像顶）。是否随 fontScale 放大头像，留待选型时定。
- 三行墨迹间隙是**被头像高度决定**的（`(头像高 − 墨迹和)/2`），不是可自由调的 8dp 常量。
- 另发现（未修，超出本任务）：`name` 为空的账号，资料卡名字行是空的；页面别处有
  `name.ifEmpty { nameShow }` 的兜底，资料卡可以照抄。
