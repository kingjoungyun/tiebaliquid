#requires -Version 7
<#
.SYNOPSIS
    采集主题三态截图（浅色 / AMOLED 深色 / 绿色 accent），用于像素回归对比。
.DESCRIPTION
    通过 UI 操作把主题切到目标状态：先到「主题选择」页，按截图自动判断明暗并纠正，
    再按需点 accent 色块，最后进设置页截图。开始前会 force-stop，保证冷启动一致。
    默认采集三态；改颜色/主题时可只采一态（快检，约省 2/3 时间）。
.EXAMPLE
    pwsh -File scripts\capture-theme-states.ps1 -Prefix after
.EXAMPLE
    pwsh -File scripts\capture-theme-states.ps1 -Prefix after -States light
#>
param(
    [Parameter(Mandatory)][string]$Prefix,
    [string]$Device = 'emulator-5554',
    [ValidateSet('all', 'light', 'dark', 'accent')]
    [string]$States = 'all',
    [switch]$SkipRestore
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $root '.toolchain\sdk\platform-tools\adb.exe'
$py = 'C:\Users\39982\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe'


function Sh([string]$cmd) { & $adb -s $Device shell $cmd 2>&1 | Out-Null }
function Grab([string]$path) {
    Sh "screencap -p /sdcard/_state.png"
    & $adb -s $Device pull /sdcard/_state.png $path 2>&1 | Out-Null
}
function IsDark([string]$path) {
    # 取设置页正文区域一点（背景），亮度 < 80 视为深色
    $code = "from PIL import Image; im=Image.open(r'$path').convert('RGB'); p=im.getpixel((30,1500)); print('DARK' if sum(p)/3 < 80 else 'LIGHT')"
    return (& $py -c $code).Trim()
}
function OpenThemePage {
    Sh "am force-stop com.tieba"
    Sh "am start -n com.tieba/com.huanchengfly.tieba.post.MainActivityV2"
    Start-Sleep -Seconds 16
    Sh "input tap 960 2307"       # dock: 我
    Start-Sleep -Seconds 5
    Sh "input tap 540 871"        # 主题选择
    Start-Sleep -Seconds 7
}
function ShotSettings([string]$path) {
    # 用「主题选择」页本身作为对比基准：内容静态（6 个 accent + 深色开关），
    # 且颜色全部来自 AppTokens，没有网络内容干扰，逐像素对比最可靠。
    OpenThemePage
    Grab $path
}

function SetNight([bool]$wantDark) {
    OpenThemePage
    for ($i = 0; $i -lt 3; $i++) {
        Grab (Join-Path $root "shots\_probe.png")
        $isDark = (IsDark (Join-Path $root "shots\_probe.png")) -eq 'DARK'
        if ($isDark -eq $wantDark) { break }
        Sh "input tap 890 946"    # 深色模式开关
        Start-Sleep -Seconds 6
    }
}

function SetAccent([string]$which) {
    OpenThemePage
    switch ($which) {
        'blue'  { Sh "input tap 132 625" }
        'green' { Sh "input tap 469 625" }
    }
    Start-Sleep -Seconds 6
}

$shots = @(
    @{ name = 'light'; dark = $false; accent = 'blue'  },
    @{ name = 'dark';  dark = $true;  accent = 'blue'  },
    @{ name = 'accent'; dark = $false; accent = 'green' }
)
if ($States -ne 'all') {
    $shots = @($shots | Where-Object { $_.name -eq $States })
    if ($shots.Count -eq 0) { throw "未知状态：$States" }
}

foreach ($s in $shots) {
    SetAccent $s.accent
    SetNight $s.dark
    $out = Join-Path $root "shots\_pv_$($Prefix)_$($s.name).png"
    ShotSettings $out
    Write-Host "captured $out"
}
# 恢复默认状态（蓝 + 浅色），方便下次会话
if (-not $SkipRestore) {
    SetAccent 'blue'
    SetNight $false
}
Write-Host "done: $Prefix ($States)"
