# tools/（仓库内快照）

这里的脚本是**工作区 `scripts/` 的只读快照**，随仓库一起提交，方便换机器时不用重新找。

## 重要：执行位置

脚本里假定自己与 **`.toolchain/`（JDK / Android SDK / Gradle / TMP）同级**，也就是**工作区根目录**：

```
<tieba-glass>/                 ← 在这里执行脚本
├── .toolchain/                ← JDK21 + Android SDK + Gradle + TMP（不进仓库）
├── scripts/                   ← 活脚本（本地改动优先改这里）
├── docs/                      ← 活文档
├── shots/                     ← 截图产物
└── sources/TiebaApp/          ← git 仓库（本文件所在处）
    ├── app/
    ├── docs/                  ← 文档快照（随仓库提交）
    └── tools/                 ← 本目录（脚本快照，随仓库提交）
```

也就是说：**仓库内这份只是快照，不能直接 `pwsh tools/run-smoke.ps1`**（`$PSScriptRoot` 的上一级会变成仓库根，
找不到 `.toolchain/`）。要跑请回到工作区根执行 `pwsh scripts/run-smoke.ps1`。

## 同步约定

工作区 `scripts/` 与 `docs/` 是**活文档**；仓库 `tools/`、`docs/` 是**快照**。
改完活版本后手动同步过来（复制覆盖 + 提交），WORKSPACE.md 里有同样的提醒。

## 主要脚本

| 脚本 | 作用 |
| --- | --- |
| `run-smoke.ps1` | 主干冒烟：编译 + 安装 + 跑 `MainSmokeTest` / `PixelRegressionTest`，失败时把截图/语义树/logcat 拉回 `shots/smoke/<时间戳>/`。支持 `-Only <短类名或全限定名>`、`-Quick`、`-SkipBuild` |
| `env.ps1` | 设置 `JAVA_HOME` / `ANDROID_HOME` / `GRADLE_USER_HOME` / `TMP` 等环境变量（被其它脚本 dot-source） |
| `pixel-diff.py` | 截图基线对比（见 `docs/VISUAL-BASELINE.md`） |
| `set-theme.ps1` / `capture-theme-states.ps1` | 切浅色/深色并批量截图 |
| `burst-screencap.ps1` / `one-screencap.ps1` | 连续截图（动画诊断）/ 单张截图 |
| `perf-collect.ps1` / `perf_parse.py` / `perf_transitions.py` | 滚动性能采集与分析 |
| `sync-glasmio.ps1` / `sync-phosphor-icons.ps1` | 同步 Glasmio 库 / Phosphor 图标 |
| `subset-fonts.py` / `gen-brand-assets.py` | 字体子集化 / 品牌图生成 |

`*.py` 需要 Python 3；工作区用 `.dsh` 自带的 Python（`scripts/env.ps1` 不设置它，直接 `python` 即可）。
