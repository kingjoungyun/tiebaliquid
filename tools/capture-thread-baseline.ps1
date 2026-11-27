param([string]$theme = 'light')
# 只重拍帖子页基线：用 uiautomator 找一个「正文较长的可点节点」再点进去
$adb = Join-Path 'C:\Users\39982\Desktop\SoftwareDevelopment\tieba-glass\.toolchain\sdk' 'platform-tools\adb.exe'
$root = 'C:\Users\39982\Desktop\SoftwareDevelopment\tieba-glass'
$out = Join-Path $root 'sources\TiebaApp\tools\baseline'
function Sh([string]$c) { & $adb -s emulator-5554 shell $c 2>&1 | Out-Null }
function Dump {
    Sh 'uiautomator dump /sdcard/_u.xml'
    & $adb -s emulator-5554 shell cat /sdcard/_u.xml > (Join-Path $root 'shots/_u.xml')
    return [xml](Get-Content -LiteralPath (Join-Path $root 'shots/_u.xml'))
}
Sh 'am force-stop com.tieba'
Start-Sleep -Milliseconds 600
Sh 'am start -W -f 0x10008000 -n com.tieba/com.huanchengfly.tieba.post.MainActivityV2'
Start-Sleep -Seconds 12
$x = Dump
$target = $null
foreach ($n in $x.SelectNodes('//node')) {
    $txt = $n.text
    if (-not $txt) { continue }
    if ($txt.Length -lt 8) { continue }
    if ($txt -match '吧$|分钟前|今天|首页|进吧|消息|搜索') { continue }
    $m = [regex]::Match($n.bounds, '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
    if (-not $m.Success) { continue }
    $top = [int]$m.Groups[2].Value
    $bottom = [int]$m.Groups[4].Value
    if ($top -lt 500 -or $bottom -gt 2100) { continue }
    $target = $n
    break
}
if (-not $target) { Write-Warning '没找到可点击的帖子正文节点'; exit 1 }
$mm = [regex]::Match($target.bounds, '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
$cx = [int](([int]$mm.Groups[1].Value + [int]$mm.Groups[3].Value) / 2)
$cy = [int](([int]$mm.Groups[2].Value + [int]$mm.Groups[4].Value) / 2)
Write-Host "点开帖子：$($target.text.Substring(0, [Math]::Min(24, $target.text.Length))) @ ($cx,$cy)"
Sh ("input tap $cx $cy")
Start-Sleep -Seconds 9
$fg = (& $adb -s emulator-5554 shell dumpsys activity activities 2>&1 | Select-String 'topResumedActivity' | Select-Object -First 1) -match 'com.tieba'
Write-Host "foreground=$fg"
$hasReply = (Dump).SelectNodes('//node') | Where-Object { $_.text -eq '评论一番' -or $_.'content-desc' -eq '回复' } | Select-Object -First 1
Write-Host "帖子页特征节点（评论一番/回复）：$([bool]$hasReply)"
$path = Join-Path $out "thread_$theme.png"
& $adb -s emulator-5554 exec-out screencap -p > $path
Write-Host "saved: $path  $((Get-Item $path).Length) B"
