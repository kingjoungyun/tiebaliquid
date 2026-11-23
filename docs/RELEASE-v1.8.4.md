# Tiebaliquid v1.8.4（首个公开版本）

> 一个把液态玻璃（iOS 26/27 观感）带到 Android 的第三方贴吧客户端。
> 基于 TiebaLite → Aur5411/TiebaApp 的业务层，重写了整个表现层。

## 亮点

- **液态玻璃材质**：底栏、顶栏、菜单、弹窗使用 `RuntimeShader` 做背景采样 + 模糊 + 折射，边缘带高光
- **iOS 式页面语言**：一级页无标题栏、大标题随滚动收起；二级页大标题 + 悬浮返回圆钮
- **信息流卡片化**：头像 / 标题 / 正文 / 图片 / 吧名与互动数据，连续大圆角
- **平面楼层**：主楼为卡片，楼层无分割线，楼中楼为圆角预览块
- **统一字形与图标**：HarmonyOS Sans SC 子集 + Phosphor Icons
- **单一主题令牌**：颜色 / 圆角 / 字号全部来自 `AppTokens`，浅色与 AMOLED 纯黑两套

## 安装

| 文件 | 适用 |
| --- | --- |
| `Tiebaliquid-1.8.4-release.apk` | Android 13（API 33）及以上 |

```bash
adb install -r Tiebaliquid-1.8.4-release.apk
```

- 包名 `com.tieba`，**可覆盖安装** Aur5411 的 TiebaApp
- 与 TiebaLite（`com.huanchengfly.tieba.post`）是两个独立 App，数据不互通
- 首次使用需自行登录贴吧账号

## 已知限制

- **仅支持 Android 13+**：液态玻璃依赖 `RuntimeShader`（API 33）
- **部分长尾页面仍是 Material 3 外观**：屏蔽词管理、收藏、关注列表等
- **部分悬浮钮为"自采样"玻璃**：采样的是纯色而非真实背景，观感为半透明 + 描边
- **dock 偶发停帧**：极低概率镜头停在中间，未定位
- **未登录相关流程验证不足**：发帖、楼中楼发送、编辑资料保存等需要账号的操作未实测

## 校准说明

- 这是**个人学习性质**的项目，非百度官方客户端
- 使用 GPL-3.0，源码可自由查看与修改；请遵守贴吧用户协议与当地法律法规
- 反馈问题请附：机型、Android 版本、复现步骤、截图

## 致谢

TiebaLite（HuanCheng65）、TiebaApp（Aur5411）、Glasmio（neoruaa）、Phosphor Icons、HarmonyOS Sans、FlClash 的 prism_ui（设计参照）。
详见仓库内 `NOTICE.md`。
