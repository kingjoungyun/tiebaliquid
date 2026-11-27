# BUG 5 修复：深色模式配图全黑

日期：2026-11-27
分支 `glass-ui`，**HEAD `c19a081`**（已推送）
commit：`baef50a`（修复）、`c19a081`（清 import）

---

## 现象

深色模式下首页/帖子页的**配图区域是纯黑空块**（角标 `1/2`、`1/4` 正常），头像正常显示。
浅色模式完全正常。用户设备 Android 15/16 上同样复现（用户描述「头像正常、帖子配图不显示」）。

## 定位过程（实测日志）

在 `NetworkImage` 里加临时日志（tag `NetworkImageDiag`，已删除），深色首页滚动抓取：

```
I NetworkImageDiag: uri=https://tiebapic.baidu.com/... size=781x651
   shouldLoad=true nightMode=true darkenMask=true result=null
I NetworkImageDiag: ... result=Success 1206x2622       ← 加载成功！
```

**结论：图片**加载与解码都是成功的**（`DisplayResult.Success`），只是画出来是黑的** → 问题在图片变换（transformation）而非网络/Sketch/资源。

## 根因

`ui/widgets/compose/Images.kt`：

```kotlin
val colorMask =
    if (AppTokens.isNightMode && context.appPreferences.imageDarkenWhenNightMode) {
        MaskTransformation(Color.parseColor("#99000000"))    // ← 罪魁
    } else null
...
if (colorMask != null) { transformations(colorMask) }
```

`MaskTransformation` 在这个 Sketch 版本上把整张图**涂成不透明黑**（颜色里的 alpha `0x99` 没有按叠加处理）。
因为结果仍是 `Success`，所以抓图上「图像存在但看不见」。

`imageDarkenWhenNightMode` 默认 `true`（`AppPreferencesUtils.kt:141`），**所有深色模式用户都中招**；头像不走
这条路径（没 mask），所以头像正常——与用户描述完全吻合。

## 修法

不再用 Sketch 的 transformation，改成 Compose 层叠一层半透明黑：

```kotlin
val darkenOverlay: Color? =
    if (AppTokens.isNightMode && context.appPreferences.imageDarkenWhenNightMode) Color(0x99000000) else null
...
AsyncImage(...)
if (darkenOverlay != null) {
    Box(Modifier.matchParentSize().background(darkenOverlay))
}
```

- 与图片解码链路完全解耦，零风险；
- 浅色模式不加遮罩，行为与之前一致；
- 临时诊断日志已删除（`git grep NetworkImageDiag` = 0），`MaskTransformation` 的 import 也清掉了。

## 验证

| 项 | 结果 |
| --- | --- |
| 深色首页配图 | ✅ 正常显示并轻微压暗（`shots/dk_home_fixed.png`） |
| 浅色首页配图 | ✅ 不变（无遮罩） |
| 全量 10 条冒烟 | ✅ 10 tests, 0 failures（`shots/smoke/20261127-132750/`） |
| 推送 | ✅ `01373ea..c19a081` |

## 给以后的提醒

> Sketch 的 **transformation 失败是静默的**：`DisplayResult` 依然返回 `Success`，
> 只是产出的 bitmap 是错的。凡是「图片加载成功但显示不对」的问题，先怀疑 transformation（mask / blur / resize），
> 优先用 Compose 层（`drawWithContent`、叠 `Box` + `background`）实现视觉处理，避免碰解码链路。
