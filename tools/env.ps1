# NOTE（仓库内快照）：本脚本期望从【工作区根】执行（与 .toolchain/ 同级），
# 不要直接 pwsh tools/<name>.ps1。同步约定见 tools/README.md。
# 用法：在仓库根目录执行  . .\scripts\env.ps1  （点号加载，变量留在当前会话）
$root = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME         = Join-Path $root '.toolchain\jdk21'
$env:ANDROID_HOME      = Join-Path $root '.toolchain\sdk'
$env:ANDROID_SDK_ROOT  = $env:ANDROID_HOME
$env:ANDROID_USER_HOME = Join-Path $root '.toolchain\android-home'   # 避免写 C:\Users\*\.android
$env:GRADLE_USER_HOME  = Join-Path $root '.toolchain\gradle-home'
$env:GRADLE_BIN        = Join-Path $root '.toolchain\gradle-8.14.5\bin\gradle.bat'

# 沙箱/系统 %TEMP% 不一定可写：Room 的 KSP verifier 会往临时目录解压 sqlite-jdbc，
# adb 也要在 TEMP 里写 adb.log，统一改到工作区。
$env:TMP  = Join-Path $root '.toolchain\tmp'
$env:TEMP = $env:TMP
New-Item -ItemType Directory -Force -Path $env:TMP | Out-Null

$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\build-tools\36.0.0;$env:PATH"

# 构建封装：gradle 的 JVM 也要显式指到可写临时目录
function Invoke-AppBuild {
    param([string[]]$Tasks = @(':app:assembleDebug'))
    & $env:GRADLE_BIN --no-daemon "-Djava.io.tmpdir=$env:TMP" @Tasks --console=plain --no-watch-fs
}

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "ANDROID_HOME=$env:ANDROID_HOME"
Write-Host "GRADLE_USER_HOME=$env:GRADLE_USER_HOME"
Write-Host "TMP=$env:TMP"
Write-Host "提示：构建用 Invoke-AppBuild，例如  Invoke-AppBuild  (默认 assembleDebug)"
