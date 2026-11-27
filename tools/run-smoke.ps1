# NOTE（仓库内快照）：本脚本期望从【工作区根】执行（与 .toolchain/ 同级），
# 不要直接 pwsh tools/<name>.ps1。同步约定见 tools/README.md。
<#
.SYNOPSIS
    TiebaApp 主干冒烟测试——一条命令跑完（构建 test APK → connectedDebugAndroidTest → 汇总 → 失败留现场）。

.DESCRIPTION
    跑 app/src/androidTest/java/com/huanchengfly/tieba/post/smoke/MainSmokeTest.kt 里的 5 条冒烟用例：
      1 coldStartShowsFeed          冷启动 → 首页信息流有可点击的帖子卡片
      2 tapPostOpensThread          点第一个帖子卡片 → 帖子页（底栏 只看楼主/正序 + 顶栏返回）
      3 settingsReachableFromProfile 我的 → 设置 → 账号管理 / 安全模式 / 外观与通用
      4 dockSwitchesAllFourTabs     dock 首页 → 进吧 → 消息 → 我 → 首页
      5 backFromThreadReturnsToFeed  进帖子 → 系统返回 → 回到信息流

    失败时会把以下现场落到 shots/smoke/<时间戳>/ 并打印路径：
      screen.png        设备当前截图
      hierarchy.xml     设备当前语义树（uiautomator dump）
      logcat.txt        最近 logcat
      test-artifacts/   测试自己在失败瞬间写的截图 + 语义树（App 外部私有目录）
      gradle.log        gradle 完整输出
      summary.txt       本次结果汇总

.PARAMETER Serial
    adb 设备序列号，默认 emulator-5554。

.PARAMETER SkipBuild
    跳过 assembleDebug / assembleDebugAndroidTest，直接跑 connectedDebugAndroidTest（改完测试想快速重跑时用）。

.PARAMETER KeepAppState
    不在跑测试前 force-stop App。

.PARAMETER Quick
    快检模式：只跑 2 条（coldStartShowsFeed + settingsReachableFromProfile），
    约 4 分钟（构建增量 + 装机 + 2 条测试），用于日常改动自检。
    走 `adb am instrument` 直跑（不经 UTP），因此也不受 UTP 环境问题影响。

.PARAMETER Only
    只跑指定用例（完整类名或 `#方法名`），逗号分隔。会覆盖 -Quick。

.EXAMPLE
    .\scripts\run-smoke.ps1

.EXAMPLE
    .\scripts\run-smoke.ps1 -Quick

.EXAMPLE
    .\scripts\run-smoke.ps1 -Only com.huanchengfly.tieba.post.smoke.MainSmokeTest#tapPostOpensThread

.EXAMPLE
    .\scripts\run-smoke.ps1 -Serial emulator-5554 -SkipBuild
#>
[CmdletBinding()]
param(
    [string]$Serial = 'emulator-5554',
    [switch]$SkipBuild,
    [switch]$KeepAppState,
    [switch]$Quick,
    [string[]]$Only
)

$ErrorActionPreference = 'Stop'

function Write-Step([string]$Text) {
    Write-Host ""
    Write-Host "=== $Text" -ForegroundColor Cyan
}

function Write-Rel([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return $Path }
    $full = (Resolve-Path -LiteralPath $Path).Path
    if ($full.StartsWith($root + '\')) { return $full.Substring($root.Length + 1) }
    return $full
}

# --------------------------------------------------------------------------- 环境
. (Join-Path $PSScriptRoot 'env.ps1')

$repo = Join-Path $root 'sources\TiebaApp'
$adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'

if (-not (Test-Path -LiteralPath $adb)) { throw "找不到 adb：$adb" }
if (-not (Test-Path -LiteralPath (Join-Path $repo 'gradlew.bat'))) { throw "找不到工程：$repo" }

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$runDir = Join-Path $root "shots\smoke\$stamp"
New-Item -ItemType Directory -Force -Path $runDir | Out-Null
$gradleLog = Join-Path $runDir 'gradle.log'
$summaryLog = Join-Path $runDir 'summary.txt'
$instrumentLog = Join-Path $runDir 'instrument.log'

$testClass = 'com.huanchengfly.tieba.post.smoke.MainSmokeTest'
$testClassBase = 'com.huanchengfly.tieba.post.smoke.'
$quickMethods = @('coldStartShowsFeed', 'settingsReachableFromProfile')
$testFilter = ''
if ($Only -and $Only.Count -gt 0) {
    # 允许传短类名：`-Only MainSmokeTest` / `-Only PixelRegressionTest#statusBarMatchesPageBackground`
    # 会自动补 `com.huanchengfly.tieba.post.smoke.` 前缀；已经是全限定名的原样使用。
    $testFilter = (($Only | ForEach-Object {
        if ($_ -match '^[A-Za-z_][\w]*(\.[A-Za-z_][\w]*)+') { $_ } else { "$testClassBase$_" }
    }) -join ',')
    Write-Host "[smoke] 过滤: $testFilter" -ForegroundColor DarkGray
} elseif ($Quick) {
    $testFilter = (($quickMethods | ForEach-Object { "$testClass#$_" }) -join ',')
}

function Install-Apk([string]$ApkPath, [string]$Package) {
    $out = (& $adb -s $Serial install -r -t $ApkPath 2>&1) -join "`n"
    if ($out -match 'UPDATE_INCOMPATIBLE|signatures do not match') {
        # 装过别的签名（换 keystore / 手工装过旧包）：卸掉重装
        Write-Host "[smoke] $Package 签名不一致，先卸载再装" -ForegroundColor Yellow
        & $adb -s $Serial uninstall $Package 2>&1 | Out-Null
        $out = (& $adb -s $Serial install -r -t $ApkPath 2>&1) -join "`n"
    }
    if ($out -notmatch 'Success') { throw "安装失败：$ApkPath`n$out" }
}

$state = (& $adb -s $Serial get-state 2>$null | Select-Object -First 1)
if ($state -ne 'device') {
    throw "设备 '$Serial' 不在线（adb get-state=$state）。先启动模拟器：emulator -avd <name>"
}

$model = (@(& $adb -s $Serial shell getprop ro.product.model) -join '').Trim()
$sdk = (@(& $adb -s $Serial shell getprop ro.build.version.sdk) -join '').Trim()
Write-Host "设备: $Serial ($model, API $sdk)" -ForegroundColor Green
Write-Host "现场目录: $(Write-Rel $runDir)"

# 跑测试期间别让屏幕睡过去 / 停在锁屏
& $adb -s $Serial shell svc power stayon true | Out-Null
& $adb -s $Serial shell input keyevent KEYCODE_WAKEUP | Out-Null
& $adb -s $Serial shell wm dismiss-keyguard | Out-Null

if (-not $KeepAppState) {
    # 真正的进程级冷启动：测试进程与被测 App 同进程，测试代码里没法 force-stop 自己，
    # 所以在进测试前从 shell 侧杀掉，保证第一条活动启动是冷启动。
    & $adb -s $Serial shell am force-stop com.tieba | Out-Null
    Write-Host "已 force-stop com.tieba（下次启动 = 冷启动）"
}

Set-Location $repo

# --------------------------------------------------------------------------- 1. 构建
if (-not $SkipBuild) {
    Write-Step '构建 debug APK + androidTest APK'
    Invoke-AppBuild -Tasks @(':app:assembleDebug', ':app:assembleDebugAndroidTest') 2>&1 |
        Tee-Object -FilePath $gradleLog
    $buildExit = $LASTEXITCODE
    if ($buildExit -ne 0) {
        Write-Host ""
        Write-Host "[smoke] 构建失败（exit $buildExit），测试没有跑起来。" -ForegroundColor Red
        Write-Host "[smoke] gradle 日志: $(Write-Rel $gradleLog)"
        Write-Host "[smoke] 末尾 60 行：" -ForegroundColor Yellow
        Get-Content -LiteralPath $gradleLog -Tail 60 | ForEach-Object { Write-Host "    $_" }
        exit 2
    }
}

# --------------------------------------------------------------------------- 2. 跑测试
# 首选 connectedDebugAndroidTest（AGP 的 UTP）。本机上 UTP 会报
# "Failed to receive the UTP test results" 且不产出任何 TEST-*.xml（环境问题，改用例无用），
# 所以一旦没拿到结果就自动降级为 `adb shell am instrument -w -r` —— 同一个 test APK、同一批用例，
# 只是绕开 UTP 的收集层。降级不需要人介入。
Write-Step "在 $Serial 上运行 connectedDebugAndroidTest"
$instrumentPreRun = $false
if ($testFilter) {
    # 快检 / 指定用例：直接 am instrument（不经 UTP），先装 APK
    Write-Step "快检模式：只跑 $testFilter"
    $appApk = (Get-ChildItem (Join-Path $repo 'app\build\outputs\apk\debug') -Filter '*.apk' -ErrorAction SilentlyContinue | Select-Object -First 1)
    $testApk = (Get-ChildItem (Join-Path $repo 'app\build\outputs\apk\androidTest\debug') -Filter '*.apk' -ErrorAction SilentlyContinue | Select-Object -First 1)
    if (-not $appApk -or -not $testApk) { throw "找不到 APK（先跑一次构建，或去掉 -SkipBuild）：$($appApk.FullName) / $($testApk.FullName)" }
    Install-Apk $appApk.FullName 'com.tieba'
    Install-Apk $testApk.FullName 'com.tieba.test'
    & $adb -s $Serial shell am instrument -w -r -e class $testFilter com.tieba.test/androidx.test.runner.AndroidJUnitRunner 2>&1 |
        Tee-Object -FilePath $instrumentLog | Out-Null
    $testExit = $LASTEXITCODE
    $instrumentPreRun = $true
} else {
    Invoke-AppBuild -Tasks @(':app:connectedDebugAndroidTest') 2>&1 |
        Tee-Object -FilePath $gradleLog -Append
    $testExit = $LASTEXITCODE
}

# --------------------------------------------------------------------------- 3. 汇总
Write-Step '汇总测试结果'

$runnerMode = 'utp'

$resultRoots = @(
    (Join-Path $repo 'app\build\outputs\androidTest-results\connected\debug'),
    (Join-Path $repo 'app\build\outputs\androidTest-results\connected')
) | Where-Object { Test-Path -LiteralPath $_ }

$xmlFiles = @()
foreach ($d in $resultRoots) {
    $found = @(Get-ChildItem -LiteralPath $d -Recurse -Filter 'TEST-*.xml' -File -ErrorAction SilentlyContinue)
    if ($found.Count -gt 0) { $xmlFiles += $found }
}
$xmlFiles = @($xmlFiles | Where-Object { $null -ne $_ } | Sort-Object FullName -Unique)

# 快检 / 指定用例走的是 am instrument：不要读上一轮 connected 留下的旧 XML，
# 否则会把历史结果当成本次结果。
if ($instrumentPreRun) { $xmlFiles = @() }

$cases = New-Object System.Collections.Generic.List[object]
$totalTests = 0; $totalFailures = 0; $totalErrors = 0; $totalSkipped = 0; $totalSeconds = 0.0

if ($xmlFiles.Count -eq 0) {
    # ---------------- 降级：adb am instrument ----------------
    $runnerMode = 'instrument'
    if (-not $instrumentPreRun) {
        Write-Host "[smoke] connectedDebugAndroidTest 没拿到 TEST-*.xml（UTP 环境问题），降级为 adb am instrument 全量。" -ForegroundColor Yellow

        $appApk = (Get-ChildItem (Join-Path $repo 'app\build\outputs\apk\debug') -Filter '*.apk' -ErrorAction SilentlyContinue | Select-Object -First 1)
        $testApkDir = Join-Path $repo 'app\build\outputs\apk\androidTest\debug'
        $testApk = (Get-ChildItem $testApkDir -Filter '*.apk' -ErrorAction SilentlyContinue | Select-Object -First 1)
        if (-not $appApk -or -not $testApk) { throw "找不到 APK（先跑一次构建）：$($appApk.FullName) / $($testApk.FullName)" }

        Install-Apk $appApk.FullName 'com.tieba'
        Install-Apk $testApk.FullName 'com.tieba.test'

        & $adb -s $Serial shell am instrument -w -r com.tieba.test/androidx.test.runner.AndroidJUnitRunner 2>&1 |
            Tee-Object -FilePath $instrumentLog | Out-Null
    }

    # 解析 -r 原始协议：test=/class= 是当前用例，CODE 1=开始，0=通过，-2=失败
    $curTest = ''; $curClass = ''; $curStack = ''; $startedAt = $null; $streamTail = @()
    foreach ($raw in (Get-Content -LiteralPath $instrumentLog -ErrorAction SilentlyContinue)) {
        $line = "$raw".TrimEnd()
        if ($line -match '^INSTRUMENTATION_STATUS: test=(.+)$') { $curTest = $Matches[1]; continue }
        if ($line -match '^INSTRUMENTATION_STATUS: class=(.+)$') { $curClass = $Matches[1]; continue }
        if ($line -match '^INSTRUMENTATION_STATUS: stack=(.*)$') { $curStack = $Matches[1]; continue }
        if ($line -match '^INSTRUMENTATION_RESULT: stream=(.*)$') { $streamTail += $Matches[1]; continue }
        if ($line -match '^INSTRUMENTATION_STATUS_CODE: 1$') { $startedAt = Get-Date; continue }
        if ($line -match '^INSTRUMENTATION_STATUS_CODE: (0|-2)$') {
            $code = $Matches[1]
            $failed = ($code -eq '-2')
            $secs = if ($startedAt) { [math]::Round(((Get-Date) - $startedAt).TotalSeconds, 1) } else { 0.0 }
            $cases.Add([pscustomobject]@{
                    Name    = $curTest
                    Class   = $curClass
                    Time    = $secs
                    Failed  = $failed
                    Message = if ($failed) { ($curStack -split "`n" | Select-Object -First 1) } else { '' }
                })
            $totalTests++
            if ($failed) { $totalFailures++ }
            $totalSeconds += $secs
            $curStack = ''
            $startedAt = $null
        }
    }

    # 原始协议没解析出用例时的兜底：只看汇总行
    if ($cases.Count -eq 0) {
        $flat = (Get-Content -LiteralPath $instrumentLog -Raw -ErrorAction SilentlyContinue)
        if ($flat -match 'OK \((\d+) tests?\)') {
            $totalTests = [int]$Matches[1]
            for ($i = 1; $i -le $totalTests; $i++) {
                $cases.Add([pscustomobject]@{ Name = "test #$i"; Class = ''; Time = 0; Failed = $false; Message = '' })
            }
        } elseif ($flat -match 'Tests run:\s*(\d+),\s*Failures:\s*(\d+)') {
            $totalTests = [int]$Matches[1]
            $totalFailures = [int]$Matches[2]
            for ($i = 1; $i -le $totalTests; $i++) {
                $cases.Add([pscustomobject]@{ Name = "test #$i"; Class = ''; Time = 0; Failed = ($i -le $totalFailures); Message = '' })
            }
        }
        if ($streamTail.Count -gt 0 -and $cases.Count -eq 0) {
            Write-Host "[smoke] am instrument 原始输出没有用例信息，末尾：" -ForegroundColor Yellow
            $streamTail | Select-Object -Last 5 | ForEach-Object { Write-Host "    $_" }
        }
    }
}

foreach ($file in $xmlFiles) {
    try { [xml]$doc = Get-Content -LiteralPath $file.FullName -Raw } catch { continue }
    $suites = @()
    if ($doc.testsuite) { $suites = @($doc.testsuite) }
    elseif ($doc.testsuites.testsuite) { $suites = @($doc.testsuites.testsuite) }

    foreach ($suite in $suites) {
        $totalTests += [int]$suite.tests
        $totalFailures += [int]$suite.failures
        $totalErrors += [int]$suite.errors
        $totalSkipped += [int]$suite.skipped
        if ($suite.time) { $totalSeconds += [double]$suite.time }

        foreach ($case in @($suite.testcase)) {
            if ($null -eq $case) { continue }
            $failed = $false
            $message = ''
            if ($case.failure) {
                $failed = $true
                $message = (@($case.failure)[0].message -split "`n" | Select-Object -First 1)
            } elseif ($case.error) {
                $failed = $true
                $message = (@($case.error)[0].message -split "`n" | Select-Object -First 1)
            }
            $cases.Add([pscustomobject]@{
                    Name    = $case.name
                    Class   = $case.classname
                    Time    = $case.time
                    Failed  = $failed
                    Message = $message
                })
        }
    }
}

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("run: $stamp  device: $Serial ($model, API $sdk)  runner: $runnerMode")
$lines.Add("tests=$totalTests failures=$totalFailures errors=$totalErrors skipped=$totalSkipped")

Write-Host "[smoke] runner: $runnerMode" -ForegroundColor DarkGray

if ($cases.Count -eq 0) {
    Write-Host "[smoke] 没有解析到任何测试结果（gradle exit=$testExit）。测试没跑起来，先看 gradle 日志。" -ForegroundColor Red
} else {
    $color = if (($totalFailures + $totalErrors) -eq 0) { 'Green' } else { 'Yellow' }
    Write-Host ("[smoke] {0} tests, {1} failures, {2} errors, {3} skipped  ({4:N1}s)" -f `
            $totalTests, $totalFailures, $totalErrors, $totalSkipped, $totalSeconds) -ForegroundColor $color
    foreach ($case in $cases) {
        if ($case.Failed) {
            Write-Host ("  FAIL  {0} [{1}s]" -f $case.Name, $case.Time) -ForegroundColor Red
            if ($case.Message) { Write-Host "        $($case.Message)" -ForegroundColor DarkRed }
            $lines.Add("FAIL $($case.Class)#$($case.Name) $($case.Time)s :: $($case.Message)")
        } else {
            Write-Host ("  PASS  {0} [{1}s]" -f $case.Name, $case.Time) -ForegroundColor Green
            $lines.Add("PASS $($case.Class)#$($case.Name) $($case.Time)s")
        }
    }
}

$failedCount = $totalFailures + $totalErrors
if ($runnerMode -eq 'utp') {
    $success = ($testExit -eq 0) -and ($cases.Count -gt 0) -and ($failedCount -eq 0)
} else {
    $success = ($cases.Count -gt 0) -and ($failedCount -eq 0)
}

# --------------------------------------------------------------------------- 4. 失败现场
if (-not $success) {
    Write-Step '收集失败现场'

    # 设备当前画面
    & $adb -s $Serial shell screencap -p /sdcard/smoke-fail.png 2>$null | Out-Null
    & $adb -s $Serial pull /sdcard/smoke-fail.png (Join-Path $runDir 'screen.png') 2>$null | Out-Null

    # 设备当前语义树
    & $adb -s $Serial shell uiautomator dump /sdcard/smoke-fail.xml 2>$null | Out-Null
    & $adb -s $Serial pull /sdcard/smoke-fail.xml (Join-Path $runDir 'hierarchy.xml') 2>$null | Out-Null

    # logcat
    & $adb -s $Serial logcat -d -t 4000 2>$null | Out-File -FilePath (Join-Path $runDir 'logcat.txt') -Encoding utf8

    # 测试自己在失败瞬间写的现场（截图 + 语义树）
    $remoteArtifacts = '/sdcard/Android/data/com.tieba/files/smoke-failures'
    & $adb -s $Serial pull $remoteArtifacts $runDir 2>$null | Out-Null

    $reportIndex = Join-Path $repo 'app\build\reports\androidTests\connected\debug\index.html'

    Write-Host ""
    Write-Host "[smoke] 失败现场（按这个顺序看）：" -ForegroundColor Yellow
    foreach ($p in @(
            (Join-Path $runDir 'screen.png'),
            (Join-Path $runDir 'hierarchy.xml'),
            (Join-Path $runDir 'logcat.txt'),
            (Join-Path $runDir 'instrument.log'),
            (Join-Path $runDir 'smoke-failures'),
            $reportIndex,
            $gradleLog
        )) {
        if (Test-Path -LiteralPath $p) {
            Write-Host "    $(Write-Rel $p)"
        }
    }
    $lines.Add('')
    $lines.Add('failure artifacts:')
    foreach ($p in @('screen.png', 'hierarchy.xml', 'logcat.txt', 'instrument.log', 'smoke-failures', 'gradle.log')) {
        $full = Join-Path $runDir $p
        if (Test-Path -LiteralPath $full) { $lines.Add("  $(Write-Rel $full)") }
    }
} else {
    Write-Host ""
    Write-Host "[smoke] 全绿 ✅" -ForegroundColor Green
}

$lines | Out-File -FilePath $summaryLog -Encoding utf8
Write-Host "[smoke] 汇总: $(Write-Rel $summaryLog)"
Write-Host "[smoke] gradle 日志: $(Write-Rel $gradleLog)"

if (-not $success) { exit 1 }
exit 0
