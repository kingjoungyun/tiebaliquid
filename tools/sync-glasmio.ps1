#requires -Version 7
<#
.SYNOPSIS
    把指定版本的 Glasmio 6 个模块（AAR + POM）同步到工程内的本地 Maven 仓库。

.DESCRIPTION
    本地仓库路径：sources/TiebaApp/libs/maven（标准 Maven 布局）。
    settings.gradle.kts 的 dependencyResolutionManagement 里把这个目录排在**最前**，
    所以离线也能解析；换版本时跑一次本脚本，再改 gradle/libs.versions.toml 里的 version。

.PARAMETER Version
    Glasmio 版本，默认 0.1.3。

.PARAMETER Repo
    远端仓库基址，默认 aliyun central 镜像（墙内最快）。官方源是
    https://repo1.maven.org/maven2。

.PARAMETER Force
    目标文件已存在时也重新下载（默认跳过，方便增量）。

.EXAMPLE
    pwsh -File scripts/sync-glasmio.ps1
    pwsh -File scripts/sync-glasmio.ps1 -Version 0.1.4 -Force
#>
param(
    [string]$Version = '0.1.3',
    [string]$Repo = 'https://maven.aliyun.com/repository/central',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$repoRoot = Join-Path $root 'sources\TiebaApp\libs\maven'
$groupPath = 'io/github/neoruaa/glasmio'
$modules = @(
    'glasmio-core',
    'glasmio-glass',
    'glasmio-ui',
    'glasmio-preference',
    'glasmio-icons',
    'glasmio-scaffold'
)

Write-Host "== Glasmio $Version -> $repoRoot"
Write-Host "   source: $Repo"

$failed = @()
foreach ($m in $modules) {
    $dir = Join-Path $repoRoot (Join-Path $groupPath "$m\$Version")
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    foreach ($ext in @('pom', 'aar')) {
        $name = "$m-$Version.$ext"
        $dest = Join-Path $dir $name
        if ((Test-Path $dest) -and -not $Force) {
            Write-Host "   skip  $name"
            continue
        }
        $url = "$Repo/$groupPath/$m/$Version/$name"
        try {
            Invoke-WebRequest -Uri $url -OutFile $dest -UseBasicParsing -TimeoutSec 300
            $kb = [math]::Round((Get-Item $dest).Length / 1KB)
            Write-Host "   ok    $name ($kb KB)"
        }
        catch {
            Write-Warning "   FAIL  $name : $($_.Exception.Message)"
            if (Test-Path $dest) { Remove-Item $dest -Force }
            $failed += $name
        }
    }
}

# 本地仓库元数据（可选，但让文件浏览器/工具识别成仓库）
$md = Join-Path $repoRoot 'io\github\neoruaa\glasmio\maven-metadata-local.xml'
@"
<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>io.github.neoruaa.glasmio</groupId>
  <metadataVersion>1</metadataVersion>
  <versioning>
    <latest>$Version</latest>
    <release>$Version</release>
    <versions>
      <version>$Version</version>
    </versions>
    <lastUpdated>$(Get-Date -Format 'yyyyMMddHHmmss')</lastUpdated>
  </versioning>
</metadata>
"@ | Set-Content -Path $md -Encoding utf8

if ($failed.Count -gt 0) {
    Write-Host ""
    Write-Warning ("同步失败 {0} 个文件：{1}" -f $failed.Count, ($failed -join ', '))
    exit 1
}

Write-Host ""
Write-Host "完成。别忘了：gradle/libs.versions.toml 里的 glasmio-* version = $Version"
