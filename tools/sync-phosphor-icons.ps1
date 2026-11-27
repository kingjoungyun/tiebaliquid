#requires -Version 7
<#
.SYNOPSIS
    从 @phosphor-icons/core 的 SVG 生成 Compose 的 ImageVector（只取用到的那些）。

.DESCRIPTION
    Phosphor 图标是 MIT 许可，npm 包里同时提供 regular / fill 等 6 种字重的独立 SVG。
    本脚本：
      1. 下载并解压 `@phosphor-icons/core`（缓存到 .toolchain/tmp/phosphor-core）；
      2. 按 [sfName -> phosphor 名字(+fill)] 的映射表，读对应 SVG 的 viewBox 与 <path d>；
      3. 生成 app/src/main/java/.../ui/common/icons/PhosphorIcons.kt —— 一个惰性构建的
         `Map<String, ImageVector>`（key 是 Phosphor 小写名，如 "house" / "star-fill"）。

    路径数据原样交给 Compose 的 `addPathNodes`（它完整支持 M/L/H/V/C/S/Q/T/A/Z，含圆弧），
    所以这里**不需要**自己写贝塞尔近似。

.PARAMETER Version
    @phosphor-icons/core 版本，默认 2.1.1。

.PARAMETER Force
    即使产物已存在也重新生成。

.EXAMPLE
    pwsh -File scripts/sync-phosphor-icons.ps1
#>
param(
    [string]$Version = '2.1.1',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$cache = Join-Path $root '.toolchain\tmp\phosphor-core'
$tgz = Join-Path $cache "phosphor-core-$Version.tgz"
$assets = Join-Path $cache 'package\assets'
$outFile = Join-Path $root 'sources\TiebaApp\app\src\main\java\com\huanchengfly\tieba\post\ui\common\icons\PhosphorIcons.kt'

# ── SF Symbols 名 -> Phosphor 名（$true = 取 fill 字重）──────────────────────────
# 语义修正过的几个（旧 Material 兜底画错过）：
#   eye.slash                        -> eye-slash                 （以前画成睁眼）
#   rectangle.topthird.inset.filled  -> square-split-horizontal   （以前画成空方块）
#   rectangle.3.group                -> columns                   （以前画成胶片条）
$map = [ordered]@{
    'arrow.clockwise'                      = @('arrow-clockwise', $false)
    'arrow.up.arrow.down'                  = @('arrows-down-up', $false)
    'arrow.up.left.and.arrow.down.right'   = @('arrows-out-simple', $false)
    'bell.fill'                            = @('bell', $true)
    'dot.radiowaves.left.and.right'        = @('bell', $false)
    'calendar.badge.clock'                 = @('calendar-check', $false)
    'character'                            = @('text-aa', $false)
    'checkmark.circle'                     = @('check-circle', $false)
    'checkmark.seal'                       = @('seal-check', $false)
    'chevron.down.circle'                  = @('caret-circle-down', $false)
    'chevron.left'                         = @('caret-left', $false)
    'chevron.right'                        = @('caret-right', $false)
    'clock'                                = @('clock', $false)
    'clock.arrow.circlepath'               = @('clock-counter-clockwise', $false)
    'doc.on.doc'                           = @('copy', $false)
    'doc.text'                             = @('file-text', $false)
    'ellipsis'                             = @('dots-three', $false)
    'exclamationmark.triangle'             = @('warning', $false)
    'eye.slash'                            = @('eye-slash', $false)
    'gearshape'                            = @('gear', $false)
    'house'                                = @('house', $false)
    'house.fill'                           = @('house', $true)
    'info.circle'                          = @('info', $false)
    'list.bullet'                          = @('list-bullets', $false)
    'lock.fill'                            = @('lock-simple', $true)
    'moon'                                 = @('moon', $false)
    'moon.fill'                            = @('moon', $true)
    'paintpalette'                         = @('palette', $false)
    'paintpalette.fill'                    = @('palette', $true)
    'pencil'                               = @('pencil-simple', $false)
    'person'                               = @('user', $false)
    'person.fill'                          = @('user', $true)
    'person.2'                             = @('users', $false)
    'person.2.fill'                        = @('users', $true)
    'person.badge.plus'                    = @('user-plus', $false)
    'photo'                                = @('image', $false)
    'photo.on.rectangle'                   = @('images', $false)
    'play.slash'                           = @('video-camera-slash', $false)
    'plus.circle'                          = @('plus-circle', $false)
    'questionmark.circle'                  = @('question', $false)
    'rectangle.3.group'                    = @('columns', $false)
    'rectangle.portrait.and.arrow.right'   = @('sign-out', $false)
    'rectangle.topthird.inset.filled'      = @('square-split-horizontal', $false)
    'safari'                               = @('compass', $false)
    'slider.horizontal.3'                  = @('sliders-horizontal', $false)
    'square.and.arrow.up'                  = @('export', $false)
    'square.3.layers.3d'                   = @('archive', $false)
    'star'                                 = @('star', $false)
    'star.fill'                            = @('star', $true)
    'sun.max'                              = @('sun', $false)
    'text.badge.xmark'                     = @('text-strikethrough', $false)
    'text.bubble'                          = @('chat-centered-text', $false)
    'textformat'                           = @('text-t', $false)
    'textformat.size'                      = @('text-aa', $false)
    'trash'                                = @('trash', $false)
    'xmark.circle'                         = @('x-circle', $false)
    'xmark.octagon'                         = @('prohibit', $false)
    'wrench.and.screwdriver'                = @('toolbox', $false)
}

Write-Host "== Phosphor $Version -> $outFile"

if ($Force -or -not (Test-Path $assets)) {
    New-Item -ItemType Directory -Force -Path $cache | Out-Null
    $url = "https://registry.npmjs.org/@phosphor-icons/core/-/core-$Version.tgz"
    Write-Host "   downloading $url"
    Invoke-WebRequest -Uri $url -OutFile $tgz -UseBasicParsing -TimeoutSec 300
    if (Test-Path (Join-Path $cache 'package')) { Remove-Item -Recurse -Force (Join-Path $cache 'package') }
    tar -xzf $tgz -C $cache
} else {
    Write-Host "   using cached assets"
}

# 直接按 Phosphor 名生成、不经 SF 别名的图标（信息流/帖子页的 Material 图标要用）
$extraIcons = @('heart', 'heart-fill', 'dots-three-vertical', 'repeat', 'x', 'arrow-square-out', 'play', 'chat-circle', 'share-network', 'thumbs-up', 'bookmark-simple', 'download-simple', 'magnifying-glass', 'smiley', 'smiley-sad', 'paper-plane-tilt', 'plus', 'check', 'caret-down', 'caret-up', 'caret-right', 'caret-left', 'dots-three', 'star', 'star-fill', 'trash', 'flag', 'rocket-launch', 'arrow-line-up', 'user-minus', 'user-circle', 'user-list', 'seal-check', 'pencil-simple', 'pause', 'rewind', 'fast-forward', 'arrow-counter-clockwise', 'image', 'images', 'image-square', 'film-strip', 'video-camera', 'camera', 'arrows-down-up', 'arrows-out', 'arrows-in', 'export', 'copy', 'link', 'chats-circle', 'file-text', 'arrow-bend-up-left', 'gear', 'warning-circle', 'prohibit', 'check-circle', 'clock-counter-clockwise', 'users-three', 'house', 'bell')

# 需要的 Phosphor 名字 -> 字重（regular / fill）；同一名字可能被多个 SF 名引用
$wanted = @{}
foreach ($sf in $map.Keys) {
    $name, $isFill = $map[$sf]
    $weight = if ($isFill) { 'fill' } else { 'regular' }
    $fileName = if ($isFill) { "$name-fill" } else { $name }
    $path = Join-Path $assets "$weight\$fileName.svg"
    if (-not (Test-Path $path)) {
        throw "缺少 SVG：$weight/$fileName.svg（来自 $sf）"
    }
    $wanted["$weight/$name"] = @($path, $weight, $fileName)
}

foreach ($name in $extraIcons) {
    foreach ($weight in @('regular', 'fill')) {
        $fileName = if ($weight -eq 'fill') { "$name-fill" } else { $name }
        $path = Join-Path $assets "$weight\$fileName.svg"
        if (Test-Path $path) { $wanted["$weight/$name-$weight"] = @($path, $weight, $fileName) }
    }
}

$entries = @()
foreach ($key in ($wanted.Keys | Sort-Object)) {
    $path, $weight, $fileName = $wanted[$key]
    $svg = Get-Content $path -Raw
    $vb = [regex]::Match($svg, 'viewBox="([^"]+)"').Groups[1].Value
    $w, $h = ($vb -split '\s+')[2], ($vb -split '\s+')[3]
    $ds = [regex]::Matches($svg, '<path[^>]*\sd="([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
    if ($ds.Count -eq 0) { throw "没有 <path>：$path" }
    $iconName = if ($weight -eq 'fill') { "$fileName" } else { $fileName }
    $paths = ($ds | ForEach-Object { '"' + ($_ -replace '\\', '\\' -replace '"', '\"') + '"' }) -join "`n            "
    $entries += @{
        Name = $iconName
        Width = $w
        Height = $h
        Paths = $ds
    }
}

$sb = [System.Text.StringBuilder]::new()
[void]$sb.AppendLine('package com.huanchengfly.tieba.post.ui.common.icons')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('import androidx.compose.ui.graphics.Color')
[void]$sb.AppendLine('import androidx.compose.ui.graphics.PathFillType')
[void]$sb.AppendLine('import androidx.compose.ui.graphics.SolidColor')
[void]$sb.AppendLine('import androidx.compose.ui.graphics.vector.ImageVector')
[void]$sb.AppendLine('import androidx.compose.ui.graphics.vector.addPathNodes')
[void]$sb.AppendLine('import androidx.compose.ui.unit.dp')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('/**')
[void]$sb.AppendLine(' * Phosphor Icons 的 `ImageVector` 子集（MIT，https://phosphoricons.com）。')
[void]$sb.AppendLine(' *')
[void]$sb.AppendLine(' * **自动生成，不要手改**：由 `scripts/sync-phosphor-icons.ps1` 从')
[void]$sb.AppendLine(" * ``@phosphor-icons/core@$Version`` 的 SVG 里抽取 viewBox 与 path 数据生成。")
[void]$sb.AppendLine(' * 路径数据原样交给 `addPathNodes`（Compose 的 SVG 解析器支持 A 圆弧，无需近似）。')
[void]$sb.AppendLine(' *')
[void]$sb.AppendLine(' * 只包含本项目实际用到的图标（约 ' + $entries.Count + ' 个），构建时惰性生成 `ImageVector`，')
[void]$sb.AppendLine(' * 没被用到的不占内存。key 是 Phosphor 的小写名（`"house"`、`"star-fill"`）。')
[void]$sb.AppendLine(' */')
[void]$sb.AppendLine('object PhosphorIcons {')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('    private val cache = HashMap<String, ImageVector>(64)')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('    /** Phosphor 名 + 原始 path 数据（`viewBox` 全部是 0 0 256 256）。 */')
[void]$sb.AppendLine('    private val paths: Map<String, List<String>> = mapOf(')
foreach ($e in $entries) {
    $joined = ($e.Paths | ForEach-Object { '            "' + ($_ -replace '\\', '\\' -replace '"', '\"') + '"' }) -join ",`n"
    [void]$sb.AppendLine("        `"$($e.Name)`" to listOf(")
    [void]$sb.AppendLine($joined + ',')
    [void]$sb.AppendLine('        ),')
}
[void]$sb.AppendLine('    )')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('    /** SF Symbols 名 -> Phosphor key（由脚本的映射表生成，业务代码只写 SF 名）。 */')
[void]$sb.AppendLine('    private val sfAliases: Map<String, String> = mapOf(')
foreach ($sf in $map.Keys) {
    $name, $isFill = $map[$sf]
    $key = if ($isFill) { "$name-fill" } else { $name }
    [void]$sb.AppendLine("        `"$sf`" to `"$key`",")
}
[void]$sb.AppendLine('    )')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('    /** 按 SF Symbols 名取图标（业务侧只传 SF 名，映射表在生成文件里）。 */')
[void]$sb.AppendLine('    fun resolve(systemName: String): ImageVector? =')
[void]$sb.AppendLine('        sfAliases[systemName]?.let { get(it) }')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('    /** 取一个 Phosphor 图标；名字不存在返回 null（调用方回退到别的图标集）。 */')
[void]$sb.AppendLine('    operator fun get(name: String): ImageVector? {')
[void]$sb.AppendLine('        cache[name]?.let { return it }')
[void]$sb.AppendLine('        val data = paths[name] ?: return null')
[void]$sb.AppendLine('        return build(name, data).also { cache[name] = it }')
[void]$sb.AppendLine('    }')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('    fun contains(name: String): Boolean = paths.containsKey(name)')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('    private fun build(name: String, data: List<String>): ImageVector {')
[void]$sb.AppendLine('        var builder = ImageVector.Builder(')
[void]$sb.AppendLine('            name = "phosphor.$name",')
[void]$sb.AppendLine('            defaultWidth = 24.dp,')
[void]$sb.AppendLine('            defaultHeight = 24.dp,')
[void]$sb.AppendLine('            viewportWidth = 256f,')
[void]$sb.AppendLine('            viewportHeight = 256f,')
[void]$sb.AppendLine('        )')
[void]$sb.AppendLine('        data.forEach { d ->')
[void]$sb.AppendLine('            builder = builder.addPath(')
[void]$sb.AppendLine('                pathData = addPathNodes(d),')
[void]$sb.AppendLine('                fill = SolidColor(Color.Black),')
[void]$sb.AppendLine('                fillAlpha = 1f,')
[void]$sb.AppendLine('                stroke = null,')
[void]$sb.AppendLine('                strokeAlpha = 1f,')
[void]$sb.AppendLine('                // Phosphor 的 fill 变体靠 evenOdd 挖洞（例如眼罩的瞳孔），')
[void]$sb.AppendLine('                // 用 NonZero 会把洞填死。')
[void]$sb.AppendLine('                pathFillType = PathFillType.EvenOdd,')
[void]$sb.AppendLine('            )')
[void]$sb.AppendLine('        }')
[void]$sb.AppendLine('        return builder.build()')
[void]$sb.AppendLine('    }')
[void]$sb.AppendLine('}')

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $outFile) | Out-Null
[System.IO.File]::WriteAllText($outFile, $sb.ToString(), [System.Text.UTF8Encoding]::new($false))

Write-Host "   生成 $($entries.Count) 个图标 -> $outFile"




