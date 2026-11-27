#requires -Version 7
<#
.SYNOPSIS
    T1 迁移前后截图：帖子 / 楼中楼 / 工具箱 / 设置 / 分析 五页。

.DESCRIPTION
    帖子页走深链 `tblite://thread/<ThreadId>`，保证前后是同一篇帖子；其余页面按语义
    文本/描述找节点点击（不写死坐标）。主题由调用方先用 scripts/set-theme.ps1 设定。
    输出：shots/_t1_<page>_<prefix>_<mode>.png

.EXAMPLE
    pwsh -File scripts\capture-t1-pages.ps1 -Prefix before -Mode light
#>
param(
    [Parameter(Mandatory)][string]$Prefix,
    [Parameter(Mandatory)][ValidateSet('light', 'dark')][string]$Mode,
    [string]$Device = 'emulator-5554',
    [string]$ThreadId = '11223600564'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'env.ps1') | Out-Null
$adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'

function Sh([string]$cmd) { & $adb -s $Device shell $cmd 2>&1 | Out-Null }
function Grab([string]$name) {
    $path = Join-Path $root "shots\_t1_$($name)_$($Prefix)_$($Mode).png"
    Sh 'screencap -p /sdcard/_t1.png'
    & $adb -s $Device pull /sdcard/_t1.png $path 2>&1 | Out-Null
    Write-Host "  shot: shots/_t1_$($name)_$($Prefix)_$($Mode).png"
}
function Dump {
    Sh 'uiautomator dump /sdcard/_t1.xml'
    & $adb -s $Device shell cat /sdcard/_t1.xml > (Join-Path $root 'shots\_t1_ui.xml')
    return [xml](Get-Content -LiteralPath (Join-Path $root 'shots\_t1_ui.xml'))
}
function Find-Node([string]$text, [string]$desc, [string]$regex) {
    $x = Dump
    foreach ($n in $x.SelectNodes('//node')) {
        $m = [regex]::Match($n.bounds, '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
        if (-not $m.Success) { continue }
        $hit = ($text -and $n.text -eq $text) -or ($desc -and $n.'content-desc' -eq $desc) -or
               ($regex -and (
                    ($n.text -and [regex]::IsMatch($n.text, $regex)) -or
                    ($n.'content-desc' -and [regex]::IsMatch($n.'content-desc', $regex))))
        if ($hit) {
            return [pscustomobject]@{
                x = ([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2
                y = ([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2
            }
        }
    }
    return $null
}
function Wait-Node([string]$text, [string]$desc, [string]$regex, [int]$timeoutMs = 15000) {
    $deadline = (Get-Date).AddMilliseconds($timeoutMs)
    while ((Get-Date) -lt $deadline) {
        $n = Find-Node -text $text -desc $desc -regex $regex
        if ($n) { return $n }
        Start-Sleep -Milliseconds 500
    }
    $label = if ($text) { "text='$text'" } elseif ($desc) { "desc='$desc'" } else { "regex='$regex'" }
    throw "等不到节点 $label（当前界面见 shots/_t1_ui.xml）"
}
function Tap-Node([string]$text, [string]$desc, [string]$regex) {
    $n = Wait-Node -text $text -desc $desc -regex $regex
    Sh ("input tap {0} {1}" -f [int]$n.x, [int]$n.y)
    Start-Sleep -Milliseconds 1000
}
function LaunchApp {
    Sh 'am force-stop com.tieba'
    Start-Sleep -Milliseconds 600
    Sh 'am start -n com.tieba/com.huanchengfly.tieba.post.MainActivityV2'
    Start-Sleep -Seconds 7
    Wait-Node -desc '进吧' -timeoutMs 20000 | Out-Null
}

Write-Host "capture $Prefix / $Mode"

# ---- 1. 帖子页（固定深链，保证前后同一篇帖子）
Sh 'am force-stop com.tieba'
Start-Sleep -Milliseconds 600
Sh "am start -a android.intent.action.VIEW -d tblite://thread/$ThreadId"
Wait-Node -desc '只看楼主' -timeoutMs 25000 | Out-Null
Start-Sleep -Milliseconds 2500
Grab 'thread'

# ---- 2. 楼中楼（点帖子里的楼中楼预览行，形如 "作者: 内容"）
$preview = Wait-Node -regex '^.+: ' -timeoutMs 10000
Sh ("input tap {0} {1}" -f [int]$preview.x, [int]$preview.y)
Wait-Node -text '评论一番' -timeoutMs 12000 | Out-Null
Start-Sleep -Milliseconds 1500
Grab 'subposts'

# ---- 3. 工具箱 / 设置（我的 tab）
LaunchApp
Tap-Node -desc '我'
Start-Sleep -Milliseconds 800
Tap-Node -text '贴吧工具箱'
Wait-Node -text '设置' -timeoutMs 12000 | Out-Null
Start-Sleep -Milliseconds 1200
Grab 'toolbox'
Sh 'input keyevent KEYCODE_BACK'
Start-Sleep -Milliseconds 1000
Tap-Node -text '设置'
Wait-Node -text '安全模式（只读）' -timeoutMs 12000 | Out-Null
Start-Sleep -Milliseconds 1200
Grab 'settings'

# ---- 4. 分析页（首页 → 头像 → 用户主页 → ⋯ → TA 的发言分析）
Sh 'input keyevent KEYCODE_BACK'
Start-Sleep -Milliseconds 1000
Tap-Node -desc '首页'
Start-Sleep -Milliseconds 800
$avatar = Wait-Node -regex '^用户'
Sh ("input tap {0} {1}" -f [int]$avatar.x, [int]$avatar.y)
Wait-Node -desc '更多选项' -timeoutMs 15000 | Out-Null
Start-Sleep -Milliseconds 800
Tap-Node -desc '更多选项'
Start-Sleep -Milliseconds 700
Tap-Node -text 'TA 的发言分析'
Wait-Node -text '发言分析' -timeoutMs 15000 | Out-Null
# 等统计结果真正出来（「概览」卡片），否则截到的是 loading 态，跟迁移前的对比没有意义。
try { Wait-Node -text '概览' -timeoutMs 25000 | Out-Null } catch { Write-Host '  warn: 等不到「概览」，按当前状态截图' }
Start-Sleep -Milliseconds 1200
Grab 'analysis'

Write-Host "done: $Prefix / $Mode"
