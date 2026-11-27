#requires -Version 7
<#
真机性能采集（HONOR 80 / ANN-AN00，序列号 A2PD023426001874）。
每次迭代：gfxinfo reset -> 交互 -> 抓整份 gfxinfo + framestats，落盘到 perf-raw/。
因为该 ROM（Android 15 / MagicOS）的 framestats 只保留约 10 帧，
所以用「每轮 reset + 每轮抓一次」的方式做多次采样，再在 perf_parse.py 里合并直方图。

用法：
  .\scripts\perf-collect.ps1 -Phase A -Iterations 10      # 信息流滚动（基线）
  .\scripts\perf-collect.ps1 -Phase Aopt -Iterations 10   # 信息流滚动（录制优化后）
  .\scripts\perf-collect.ps1 -Phase Aoff -Iterations 10   # 信息流滚动（整页录制关，需先开 tieba_no_page_record=1）
  .\scripts\perf-collect.ps1 -Phase B -Iterations 5       # 帖子转场（混合）
  .\scripts\perf-collect.ps1 -Phase Bcold -Iterations 5   # 转场：每次换没打开过的帖子
  .\scripts\perf-collect.ps1 -Phase Bwarm -Iterations 5   # 转场：反复打开同一个帖子（缓存热）
  .\scripts\perf-collect.ps1 -Phase Bpush -Iterations 5   # 只测「点进」那一下
  .\scripts\perf-collect.ps1 -Phase Bpop -Iterations 5    # 只测「返回」那一下
  .\scripts\perf-collect.ps1 -Phase Btab -Iterations 5    # 对照：只切 tab（无数据加载）
  .\scripts\perf-collect.ps1 -Phase C1 -Iterations 3      # 慢速拖 dock
  .\scripts\perf-collect.ps1 -Phase C2 -Iterations 3      # dock 收缩/展开
  .\scripts\perf-collect.ps1 -Phase D -Iterations 3       # 首页静止 5s
#>
param(
    [Parameter(Mandatory)][ValidateSet('A', 'Aopt', 'Aopt2', 'Aopt3', 'Aopt4', 'Aoff', 'B', 'Bcold', 'Bwarm', 'Bpush', 'Bpop', 'Btab', 'C1', 'C2', 'D', 'Doff', 'Fov', 'Foff', 'Pov', 'Poff')][string]$Phase,
    [int]$Iterations = 0,
    [string]$Serial = 'A2PD023426001874',
    [string]$Package = 'com.tieba'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$env:TMP = Join-Path $root '.toolchain\tmp'
$env:TEMP = $env:TMP
New-Item -ItemType Directory -Force -Path $env:TMP | Out-Null
$adb = Join-Path $root '.toolchain\sdk\platform-tools\adb.exe'
if (-not (Test-Path $adb)) { throw "adb not found: $adb" }
$rawDir = Join-Path $root 'perf-raw'
New-Item -ItemType Directory -Force -Path $rawDir | Out-Null

if ($Iterations -le 0) {
    $Iterations = switch ($Phase) { 'B' { 5 } 'Bcold' { 5 } 'Bwarm' { 5 } 'Bpush' { 5 } 'Bpop' { 5 } 'Btab' { 5 } 'C1' { 3 } 'C2' { 3 } 'D' { 3 } 'Doff' { 3 } 'Fov' { 8 } 'Foff' { 8 } 'Pov' { 8 } 'Poff' { 8 } default { 10 } }
}

function Sh([string]$cmd) {
    & $adb -s $Serial shell $cmd 2>&1 | Out-Null
}
function Get-AppLayer {
    $out = & $adb -s $Serial shell "dumpsys SurfaceFlinger --list | grep -E '^RequestedLayerState\{com\.tieba/.*MainActivityV2#[0-9]+ '"
    $line = ($out | Select-Object -First 1) -join ''
    if (-not $line) { return $null }
    return ($line -replace '^RequestedLayerState\{', '' -replace ' parentId=.*$', '')
}
function Grab([int]$i, [string]$layer) {
    $base = Join-Path $rawDir ('{0}_{1:D2}' -f $Phase, $i)
    $a = & $adb -s $Serial shell "dumpsys gfxinfo $Package"
    $a | Set-Content -Path "$base`_stats.txt" -Encoding utf8
    $b = & $adb -s $Serial shell "dumpsys gfxinfo $Package framestats"
    $b | Set-Content -Path "$base`_fs.txt" -Encoding utf8
    if ($layer) {
        & $adb -s $Serial shell "dumpsys SurfaceFlinger --latency '$layer'" |
            Set-Content -Path "$base`_sf.txt" -Encoding utf8
    }
    $tot = ($a | Select-String -Pattern '^Total frames rendered:').Line
    Write-Host ("  [{0}] {1}" -f $i, $tot)
}

# 保证前台是我们的 App
$focus = (& $adb -s $Serial shell "dumpsys window | grep mCurrentFocus") -join ''
if ($focus -notmatch [regex]::Escape($Package)) {
    Sh "am start -n $Package/com.huanchengfly.tieba.post.MainActivityV2"
    Start-Sleep -Seconds 3
}

Write-Host "== Phase $Phase, iterations=$Iterations, device=$Serial =="
$layer = Get-AppLayer
Write-Host "surface layer: $layer"
# Bwarm 先预热一次「打开/返回」，让同一个帖子进内存缓存；这段不计入统计。
if ($Phase -eq 'Bwarm') {
    Sh "input tap 540 520"
    Start-Sleep -Milliseconds 1500
    Sh "input keyevent KEYCODE_BACK"
    Start-Sleep -Milliseconds 1200
}
for ($i = 1; $i -le $Iterations; $i++) {
    # C2 需要从「滚到顶部、dock 展开」的同一状态起跑；这段准备不计入统计。
    if ($Phase -eq 'C2') {
        Sh "for k in 1 2 3; do input swipe 540 500 540 1800 250; sleep 0.3; done"
        Start-Sleep -Milliseconds 1200
    }
    # Bcold 每次先滚到没打开过的帖子；这段准备不计入统计。
    if ($Phase -eq 'Bcold') {
        Sh "for k in 1 2; do input swipe 540 1600 540 500 250; sleep 0.35; done"
        Start-Sleep -Milliseconds 900
    }
    # Bpop 每轮前先进入详情页（准备不计入统计），再单独测返回。
    if ($Phase -eq 'Bpop') {
        Sh "input tap 540 520"
        Start-Sleep -Milliseconds 1400
    }
    # F/P 试点：每轮前回到目标页面（准备不计入统计）
    if ($Phase -in @('Fov','Foff')) {
        Sh "am start -a android.intent.action.VIEW -d 'tblite://forum/植物' -n $Package/com.huanchengfly.tieba.post.MainActivityV2"
        Start-Sleep -Milliseconds 1500
    }
    if ($Phase -in @('Pov','Poff')) {
        Sh "am start -n $Package/com.huanchengfly.tieba.post.MainActivityV2"
        Start-Sleep -Milliseconds 800
        Sh "input tap 960 2307"
        Start-Sleep -Milliseconds 900
        Sh "input tap 540 1264"
        Start-Sleep -Milliseconds 1200
    }
    Sh "dumpsys gfxinfo $Package reset"
    if ($layer) { Sh "dumpsys SurfaceFlinger --latency-clear '$layer'" }
    switch ($Phase) {
        'A' {
            Sh "input swipe 540 1600 540 500 300"
            Start-Sleep -Milliseconds 500
        }
        'Aopt' {
            Sh "input swipe 540 1600 540 500 300"
            Start-Sleep -Milliseconds 500
        }
        'Aopt4' {
            Sh "input swipe 540 1600 540 500 300"
            Start-Sleep -Milliseconds 500
        }
        'Aopt3' {
            Sh "input swipe 540 1600 540 500 300"
            Start-Sleep -Milliseconds 500
        }
        'Aopt2' {
            Sh "input swipe 540 1600 540 500 300"
            Start-Sleep -Milliseconds 500
        }
        'Aoff' {
            Sh "input swipe 540 1600 540 500 300"
            Start-Sleep -Milliseconds 500
        }
        'B' {
            # 点第一条帖子 -> 详情，等网络/首帧，再返回
            Sh "input tap 540 520"
            Start-Sleep -Milliseconds 1300
            Sh "input keyevent KEYCODE_BACK"
            Start-Sleep -Milliseconds 1000
        }
        'Bcold' {
            Sh "input tap 540 520"
            Start-Sleep -Milliseconds 1300
            Sh "input keyevent KEYCODE_BACK"
            Start-Sleep -Milliseconds 1000
        }
        'Bwarm' {
            Sh "input tap 540 520"
            Start-Sleep -Milliseconds 1300
            Sh "input keyevent KEYCODE_BACK"
            Start-Sleep -Milliseconds 1000
        }
        'Bpush' {
            Sh "input tap 540 520"
            Start-Sleep -Milliseconds 1400
        }
        'Bpop' {
            Sh "input keyevent KEYCODE_BACK"
            Start-Sleep -Milliseconds 1000
        }
        'Btab' {
            # 纯动画对照：推荐 <-> 查人（页面已预热，不产生新网络请求）
            Sh "input tap 420 195"
            Start-Sleep -Milliseconds 700
            Sh "input tap 650 195"
            Start-Sleep -Milliseconds 700
        }
        'C1' {
            # 在 dock 上慢速横拖 2s
            Sh "input swipe 850 2280 250 2280 2000"
            Start-Sleep -Milliseconds 800
        }
        'C2' {
            # 上滑让 dock 收缩成圆球，再点圆球展开（prep 已在循环外）
            Sh "input swipe 540 1900 540 1000 400"
            Start-Sleep -Milliseconds 1000
            Sh "input tap 561 2244"
            Start-Sleep -Milliseconds 1000
        }
        'D' {
            Start-Sleep -Seconds 5
        }
        'Doff' {
            Start-Sleep -Seconds 5
        }
        'Fov' { Sh "input swipe 540 1500 540 600 300"; Start-Sleep -Milliseconds 600 }
        'Foff' { Sh "input swipe 540 1500 540 600 300"; Start-Sleep -Milliseconds 600 }
        'Pov' { Sh "input swipe 540 1400 540 700 300"; Start-Sleep -Milliseconds 600 }
        'Poff' { Sh "input swipe 540 1400 540 700 300"; Start-Sleep -Milliseconds 600 }
    }
    Grab $i $layer
    if ($Phase -eq 'Bpush') {
        Sh "input keyevent KEYCODE_BACK"
        Start-Sleep -Milliseconds 900
    }
}

# 记录一次设备热状态，便于判断是否降频
$thermal = (& $adb -s $Serial shell "dumpsys battery | grep -E 'temperature|level'") -join '; '
Write-Host "battery: $thermal"
Write-Host "raw dumps -> $rawDir"




