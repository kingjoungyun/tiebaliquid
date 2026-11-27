# 截图基线（最小可用版）

6 个页面 × 浅色/深色 = **12 张**基线，用来在改样式后快速发现「整块变色 / 白条 / 深色灾难」这类回归。

## 文件

```
tools/baseline/
  home_light.png          home_dark.png           首页
  thread_light.png        thread_dark.png         帖子页
  settings_light.png      settings_dark.png       设置页
  mine_light.png          mine_dark.png           我的
  search_light.png        search_dark.png         搜索页
  notifications_light.png notifications_dark.png  消息（tab 版）
```

## 生成 / 重拍

```powershell
# 从【工作区根】执行（脚本依赖 .toolchain/）
pwsh tools/capture-baseline.ps1                     # 12 张全部重拍（浅色一轮 + 深色一轮）
pwsh tools/capture-baseline.ps1 -LightOnly          # 只重拍浅色 6 张
pwsh tools/capture-baseline.ps1 -DarkOnly           # 只重拍深色 6 张

# 帖子页单独重拍（首页信息流随机，「点第一张卡片」不可靠，
# 这个脚本会用 uiautomator 找一条正文节点再点进去，并校验「评论一番」出现）
pwsh tools/capture-thread-baseline.ps1 -theme light
pwsh tools/capture-thread-baseline.ps1 -theme dark
```

⚠️ 跑之前确认 App 在前台且**已登录状态无关**（这些页面都不需要登录）；
⚠️ 冒烟测试跑完 `com.tieba` 会被卸载，先 `adb install -r` 再拍。

## 对比

```powershell
python tools/pixel-diff.py --baseline tools/baseline --current shots/current
```

- 判定：**每通道平均绝对差**（0~255），默认阈值 **8**；
- 超过阈值的图会写差异图到 `<current>/__diff/`（一致像素压暗、差异像素标红）；
- 同时打印「差异最大的 3 个网格区域」（8×8 网格，归一化坐标）；
- 退出码 0 = 全绿、1 = 有差异、2 = 用法/文件问题。

## 阈值怎么定

| 场景 | 建议阈值 |
| --- | --- |
| 纯色页面（设置 / 我的 / 搜索） | 4~6 |
| 含图片的信息流（首页 / 帖子页） | 10~14 |

**为什么信息流要宽**：帖子内容是随机的，头像/配图/文字每次不同，基线只能覆盖「布局与配色」，
不能覆盖内容。要稳定的话应该给这些页面配**固定数据源**（未做）。

## 已知局限

1. **内容随机**：首页/帖子页的图片与文字每次不同 → 这两页的基线只对「大面积配色异常」敏感；
2. **状态栏时钟**：截图里含时间，理论上会带来极小差异（实测 < 0.1，可忽略）；
3. **帖子页抓取**：靠「点开一条正文」实现，若信息流没加载出来会失败（脚本会 `exit 1`）；
4. 基线**尚未提交**（等维护者抽查确认后再进仓库）。

## 反验证（已验证工具本身有效）

- 自比对：12 张全部 `0.00` ✔
- 注入「深色卡色偏蓝」：把 `settings_dark.png` 每个像素 B 通道 +40 → 平均差 **13.15 > 8** → 判 DIFF，
  差异图正常产出 ✔（改回去即恢复全绿）
