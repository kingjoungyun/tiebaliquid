param(
    [string]$Device = "A2PD023426001874",
    [string]$Prefix = "shots/_burst",
    [int]$Count = 9
)

$adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
for ($i = 1; $i -le $Count; $i++) {
    & $adb -s $Device exec-out screencap -p > "$Prefix$i.png"
}
Write-Host "burst done: $Count frames"
