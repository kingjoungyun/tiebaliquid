# 深色灰阶 token 对照表（BUG 4 · 第 1 步：先给数据）

日期：2026-11-27
代码位置：`ui/common/theme/glass/AppTokens.kt`（旧 M2 令牌）、
Glasmio `glasmio-core/.../GlassTokens.kt`（玻璃令牌）、`res/values/styles.xml`（XML attr）

---

## 一、两套深色调色板（这就是「灰阶不统一」的根源）

深色下 App 里**同时存在两套卡色**：玻璃页用 Glasmio 的中性灰，非玻璃组件用旧 M2 的蓝调灰。

| 用途 | Glasmio 令牌（玻璃页） | 旧 AppTokens / XML（M2 组件） | 是否一致 |
| --- | --- | --- | --- |
| 页面底 | `groupedBackground` = **#000000** | `groupedBackground` = **#000000** ／ `background` = **#000000**（深色强制） | ✅ 一致 |
| 卡片 | `elevatedBackground` = **#1C1C1E**（中性灰） | `colorCard` = **#15202B**（蓝调） | ❌ **不一致** |
| 楼层卡 | —（用 elevatedBackground） | `colorFloorCard` = **#1A2A39**（更蓝） | ❌ 不一致 |
| 容器（玻璃填充） | `container` = **#1C1C1E @ 54%**（叠加在黑上≈#0F0F10） | `chip` = `@color/theme_color_window_background_dark` | ❌ 两套 |
| 主文字 | `content` = **#FFFFFF** | `text` = textColor **@87%**（≈#DEDEDE） | ⚠️ 明度不同（21:1 vs 14.8:1） |
| 次文字 | `secondaryContent` = **#B2EBEBF5**（70% 白） | `textSecondary` = textColor **@60%** | ⚠️ 明度不同 |
| 三级文字 | `tertiaryContent` = **#4DEBEBF5**（30% 白） | `textDisabled` = `@color/color_text_disabled` | ⚠️ 两套 |
| 分隔线 | `separator` = **#2BFFFFFF**（17% 白） | `divider` = `@color/colorDivider` | ⚠️ 两套 |
| 描边 | `subtleStroke` = **#2BFFFFFF** | — | — |

AMOLED 深色（`styles.xml:278`）还有第三套：`colorCard` = **#FF101010**、`colorFloorCard` = **#FF151515**。
即：**同一个 App 深色下存在 3 套卡色**（#1C1C1E 中性 / #15202B 蓝调 / #101010 AMOLED）。

---

## 二、对比度计算（WCAG 相对亮度）

| 前景 / 背景 | 对比度 | 判定（正文需 ≥ 4.5:1） |
| --- | --- | --- |
| `content` #FFFFFF / `groupedBackground` #000000 | **21.0 : 1** | ✅ |
| `content` #FFFFFF / `elevatedBackground` #1C1C1E | **18.6 : 1** | ✅ |
| `secondaryContent` #B2EBEBF5(70%白→≈#B3B3B3) / #000000 | **10.1 : 1** | ✅ |
| `secondaryContent` / #1C1C1E | **9.0 : 1** | ✅ |
| `tertiaryContent` 30%白(≈#4D4D4D) / #000000 | **2.4 : 1** | ⚠️ 仅装饰性文字可用 |
| `AppTokens.text` 87%白(≈#DEDEDE) / #000000 | **14.8 : 1** | ✅ |
| `AppTokens.card` #15202B / #000000 | **1.28 : 1** | 卡面与页面底几乎同亮度（层级靠色相区分 → 失败） |
| `elevatedBackground` #1C1C1E / #000000 | **1.13 : 1** | 同上，层级很弱但可接受（iOS 风格） |

**层级判定**：页面 `#000000` < 卡片 `#1C1C1E`(1.13) / `#15202B`(1.28) < 浮层 `#2C2C2E`(≈1.35)。
数值层级是「有序」的，**问题在色相**：`#15202B`、`#1A2A39` 是蓝调，和 `#1C1C1E` 中性灰并排时肉眼可见「一块偏蓝、一块偏灰」。

---

## 三、问题清单（待确认后修改）

1. **卡色三套并存** → 建议：深色下 `AppTokens.card`/`floorCard` 统一到 Glasmio 的
   `#1C1C1E` / `#2C2C2E`（AMOLED 保持 `#101010`/`#151515`）。改 `AppTokens.fillFromTheme`
   深色分支即可，不动 XML attr。
2. **主/次文字明度两套**（87%/60% vs 100%/70%）→ 建议玻璃页统一用 Glasmio 的（已是），
   非玻璃页（M2 Scaffold 残留、WebView 顶部条等）可保持 87%，但**同一屏内不要混用**。
3. **消息页大标题对比度不足**（用户截图）：该页大标题可能取了 `AppTokens.text`@87% 压在纯黑上
   本应是 14.8:1 —— 若视觉上偏灰，说明被额外 alpha 叠加（`LocalContentColor` 二次透明度），
   需要单独确认该处用的是哪个颜色来源。
4. **分段控件 / dock / 搜索框**：都走玻璃材质（`GlassSurface`），深色下由采样背景决定亮度，
   不属于「灰阶 token」问题；若观感跳变，应统一 `frostRadius`/`emphasis`，不是改色值。

---

## 四、本轮已做

- `ca58bfd`：浏览记录页迁 `GlassPage`（edge-to-edge 第 1/5 页），状态栏区域与页面同色、无白条。
- 其余 4 页未动（见 `BUGS-v1.9.0.md` 待续清单），其中 `NotificationsPage` 的 tab 版已经走
  `MainTabScaffold` overlay（等价于已达标），需要改的是它的**二级页版本**。
