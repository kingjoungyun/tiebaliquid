param(
    [string]$OutDir = 'tools/baseline',
    [switch]$DarkOnly,
    [switch]$LightOnly
)
# NOTE（仓库内快照）：期望从【工作区根】执行；仓库内副本仅参考。见 tools/README.md。
$ErrorActionPreference = 'Stop'
$adb = Join-Path 'C:\Users\39982\Desktop\SoftwareDevelopment\tieba-glass\.toolchain\sdk' 'platform-tools\adb.exe'
$root = 'C:\Users\39982\Desktop\SoftwareDevelopment\tieba-glass'
if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Force -Path $OutDir | Out-Null }

function Sh([string]$c) { & $adb -s emulator-5554 shell $c 2>&1 | Out-Null }
function Dump {
    Sh 'uiautomator dump /sdcard/_u.xml'
    & $adb -s emulator-5554 shell cat /sdcard/_u.xml > (Join-Path $root 'shots/_u.xml')
    return [xml](Get-Content -LiteralPath (Join-Path $root 'shots/_u.xml'))
}
function Fg() { (& $adb -s emulator-5554 shell dumpsys activity activities 2>&1 | Select-String 'topResumedActivity' | Select-Object -First 1) -match 'com.tieba' }
function TapNode($node) {
    $m = [regex]::Match($node.bounds, '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
    if ($m.Success) {
        Sh ("input tap {0} {1}" -f [int](([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2), [int](([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2))
        return $true
    }
    return $false
}
function Tap-Dock([string]$desc) {
    $x = Dump
    foreach ($n in $x.SelectNodes('//node')) { if ($n.'content-desc' -eq $desc) { return (TapNode $n) } }
    return $false
}
function Tap-Text([string]$text) {
    $x = Dump
    foreach ($n in $x.SelectNodes('//node')) { if ($n.text -eq $text) { return (TapNode $n) } }
    return $false
}
function Shot([string]$name, [string]$theme) {
    $path = Join-Path $OutDir "$name`_$theme.png"
    & $adb -s emulator-5554 exec-out screencap -p > $path
    $size = (Get-Item $path).Length
    if (-not (Fg)) { Write-Warning "$name`_$theme：前台不是 com.tieba，截图可能无效" }
    Write-Host ("  shot {0,-28} {1,8:N0} B" -f "$name`_$theme.png", $size)
}
function Set-Theme([bool]$dark) {
    $x = Dump
    foreach ($n in $x.SelectNodes('//node')) {
        if ($n.text -eq '主题选择') { TapNode $n | Out-Null; break }
    }
    Start-Sleep -Seconds 3
    $cur = $null
    $sw = $null
    $x = Dump
    foreach ($n in $x.SelectNodes('//node')) { if ($n.checkable -eq 'true') { $sw = $n; $cur = $n.checked; break } }
    if (-not $sw) { Write-Warning '找不到深色开关'; return }
    if ($cur -ne "$dark".ToLower()) {
        TapNode $sw | Out-Null
        Start-Sleep -Seconds 3
        $x = Dump
        foreach ($n in $x.SelectNodes('//node')) { if ($n.checkable -eq 'true') { Write-Host "  theme checked=$($n.checked)"; break } }
    } else { Write-Host "  theme already checked=$cur" }
    Sh 'input keyevent KEYCODE_BACK'
    Start-Sleep -Seconds 2
}

function Capture-Set([string]$theme) {
    Write-Host "=== $theme 组 ==="
    Sh 'am start -W -f 0x10008000 -n com.tieba/com.huanchengfly.tieba.post.MainActivityV2'
    Start-Sleep -Seconds 10
    Shot 'home' $theme

    Tap-Dock '消息' | Out-Null; Start-Sleep -Seconds 4; Shot 'notifications' $theme
    Tap-Dock '我' | Out-Null;   Start-Sleep -Seconds 4; Shot 'mine' $theme
    Tap-Text '设置' | Out-Null; Start-Sleep -Seconds 4; Shot 'settings' $theme
    Sh 'input keyevent KEYCODE_BACK'; Start-Sleep -Seconds 2

    Tap-Dock '首页' | Out-Null; Start-Sleep -Seconds 3
    # 搜索页：点首页顶部的搜索框（文字「搜索」）
    Tap-Text '搜索' | Out-Null; Start-Sleep -Seconds 4; Shot 'search' $theme
    Sh 'input keyevent KEYCODE_BACK'; Start-Sleep -Seconds 3

    # 帖子页：点信息流里第一张卡片（屏宽中间、约 40% 高度处）
    Sh 'input tap 540 900'; Start-Sleep -Seconds 6; Shot 'thread' $theme
    Sh 'input keyevent KEYCODE_BACK'; Start-Sleep -Seconds 3
}

if (-not $DarkOnly) { Capture-Set 'light' }
if (-not $LightOnly) {
    Tap-Dock '我' | Out-Null; Start-Sleep -Seconds 3
    Set-Theme $true
    Capture-Set 'dark'
}
Write-Host ''
Write-Host "基线目录：$OutDir"
Get-ChildItem $OutDir -Filter '*.png' | Sort-Object Name | ForEach-Object { "  $($_.FullName)" }
