<#
.SYNOPSIS
    直接把 App 的 DataStore 改成指定主题（并可选改 accent 覆盖色）。

.DESCRIPTION
    旧版（.toolchain/tmp/set-theme.ps1）坏在两个地方：
      1. 用 `adb pull /data/data/com.tieba/...` 读私有文件 —— shell 用户没有权限，必然失败；
      2. patch 脚本假定 theme 条目一定存在、且长度是 1 字节 —— 主题没写过时 key 根本不存在。

    新版改走 `run-as`（debug 包可调试），并且 patch 脚本能：
      - 解析真正的 varint 长度（不再假定 1 字节）；
      - theme 不存在时追加条目，而不是直接失败；
      - 顺带把 `custom_primary_color` 写成指定颜色。

.EXAMPLE
    pwsh -File scripts/set-theme.ps1 -Mode light -Accent "#FF0A84FF"
    pwsh -File scripts/set-theme.ps1 -Mode amoled
#>
param(
    [ValidateSet("light", "dark", "amoled")]
    [string]$Mode = "light",
    [string]$Accent = "",
    [string]$Device = "emulator-5554"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'env.ps1') | Out-Null

$py = "C:\Users\39982\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"
if (-not (Test-Path $py)) { throw "python not found: $py" }

$adb = (Get-Command adb).Source
$pb = Join-Path $env:TMP 'theme-prefs.pb'
$remote = "/data/local/tmp/tieba_theme_prefs.pb"

& $adb -s $Device shell am force-stop com.tieba | Out-Null
Remove-Item -LiteralPath $pb -ErrorAction SilentlyContinue

# 私有目录：shell 读不了，用 run-as（debug 包）把文件流到本地。
$proc = Start-Process -FilePath $adb `
    -ArgumentList @('-s', $Device, 'exec-out', 'run-as', 'com.tieba', 'cat',
        'files/datastore/app_preferences.preferences_pb') `
    -RedirectStandardOutput $pb -NoNewWindow -Wait -PassThru
if ($proc.ExitCode -ne 0 -or -not (Test-Path $pb) -or (Get-Item $pb).Length -eq 0) {
    throw "pull failed (exit $($proc.ExitCode))"
}

$patchArgs = @((Join-Path $PSScriptRoot 'patch-theme.py'), $pb, $Mode)
if ($Accent) { $patchArgs += $Accent }
& $py @patchArgs
if ($LASTEXITCODE -ne 0) { throw "patch failed" }

# 写回：push 到 /data/local/tmp（shell 可写），再 run-as cp 到 app 私有目录。
& $adb -s $Device push $pb $remote 2>$null | Out-Null
& $adb -s $Device shell run-as com.tieba cp $remote files/datastore/app_preferences.preferences_pb
& $adb -s $Device shell rm -f $remote | Out-Null

& $adb -s $Device shell monkey -p com.tieba -c android.intent.category.LAUNCHER 1 2>$null | Out-Null
Write-Host "theme -> $Mode$(if ($Accent) { " / accent $Accent" })  (device $Device)"
