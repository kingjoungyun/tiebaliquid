# NOTICE

Tiebaliquid 是 GPL-3.0 许可的开源项目。本文件列出项目中使用或参照的第三方作品及其许可。

## 上游代码

本项目的业务层（贴吧 API、protobuf、Room 数据库、登录流程、ViewModel 逻辑）与部分页面结构来自以下项目，遵循 GPL-3.0：

| 项目 | 作者 | 许可 | 关系 |
| --- | --- | --- | --- |
| [TiebaLite](https://github.com/HuanCheng65/TiebaLite) | HuanCheng65 | GPL-3.0 | 原始第三方贴吧客户端（4.0-dev 分支） |
| [TiebaApp](https://github.com/Aur5411/TiebaApp) | Aur5411 | GPL-3.0 | TiebaLite 的二次开发，Material 3 重制，修改包名为 `com.tieba` |

本项目在上述基础上重写了表现层（主题、组件、图标、字体、页面骨架）。

## 运行时依赖

### Glasmio（液态玻璃渲染）

- 作者：[neoruaa](https://github.com/neoruaa/Glasmio)
- 许可：**Apache-2.0**
- 使用方式：`glasmio-core` / `glass` / `ui` / `preference` / `icons` / `scaffold` 的 AAR 随仓库提供于 `libs/maven/`，许可证全文见 `libs/maven/LICENSE`。

### Phosphor Icons（图标）

- 来源：<https://phosphoricons.com/>
- 许可：**MIT**
- 使用方式：约 175 个图标由脚本（`scripts/sync-phosphor-icons.ps1`）从官方 SVG 转换为 Compose `ImageVector`，生成文件为 `PhosphorIcons.kt`。

### HarmonyOS Sans SC（字体）

- 来源：华为 HarmonyOS 字体（HarmonyOS Sans SC）
- 许可：免费商用
- 使用方式：经子集化（`scripts/subset-fonts.py`）后打包于 `app/src/main/res/font/`。

### 其它开源库

项目同时使用以下常用库，均遵循各自许可（Apache-2.0 / MIT 等）：

AndroidX（Compose、Navigation、Room、Lifecycle、Media3）、Hilt、Kotlin Coroutines、
OkHttp、Retrofit、Wire（protobuf）、Sketch、Coil、Kotlinx Serialization、
compose-destinations、Lottie、uCrop、Matisse 等。完整清单见 `gradle/libs.versions.toml`。

## 设计参照

液态玻璃的交互与动效模型参考：

- [FlClash](https://github.com/chen08209/FlClash) 及其 `prism_ui` 设计语言（未使用其代码）
- Apple Liquid Glass（视觉语言参照，未使用 Apple 素材）
- iOS Human Interface Guidelines（交互范式参照）

Apple 的 SF Symbols 图标**未被使用**——图标全部来自 Phosphor（MIT）。

## 免责声明

本项目为个人学习与技术研究用途，与百度公司及上述项目的作者无关。
"贴吧"为百度公司的商标，本项目仅在描述用途时使用。
