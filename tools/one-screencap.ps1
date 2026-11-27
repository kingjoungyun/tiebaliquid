param(
    [string]$Device = "A2PD023426001874",
    [string]$Out = "shots/_p1.png",
    [int]$DelayMs = 0
)

if ($DelayMs -gt 0) { Start-Sleep -Milliseconds $DelayMs }
$adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
& $adb -s $Device exec-out screencap -p > $Out
