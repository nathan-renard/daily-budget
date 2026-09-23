# Builds the signed Play Store bundle and copies it to dist/DailyBudget-<version>.aab.
# Usage: powershell -ExecutionPolicy Bypass -File tools/bundle.ps1
$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path $PSScriptRoot '..')
if (-not (Test-Path (Join-Path $root 'keystore.properties'))) {
    throw "keystore.properties not found. Run tools/create-upload-key.ps1 first."
}

Push-Location $root
try {
    & .\gradlew.bat bundleRelease
    if ($LASTEXITCODE -ne 0) { throw "bundleRelease failed" }
} finally {
    Pop-Location
}

$gradle = Get-Content (Join-Path $root 'app\build.gradle') -Raw
$code = [regex]::Match($gradle, 'versionCode\s+(\d+)').Groups[1].Value
$name = [regex]::Match($gradle, "versionName\s+'([^']+)'").Groups[1].Value

$dist = Join-Path $root 'dist'
New-Item -ItemType Directory -Force $dist | Out-Null
$out = Join-Path $dist "DailyBudget-$name-$code.aab"
Copy-Item (Join-Path $root 'app\build\outputs\bundle\release\app-release.aab') $out -Force
Write-Output "Bundle: $out"
