# Renders the Play Store listing graphics into store/listing/ with headless Microsoft Edge.
# Screenshots show the real app UI (app/src/main/assets/www) loaded with sample data.
# Usage: powershell -ExecutionPolicy Bypass -File store/render.ps1
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$out = Join-Path $PSScriptRoot 'listing'
$port = 8766
$base = "http://localhost:$port/store/src"

$edge = @(
    "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $edge) { throw "Microsoft Edge not found." }

$profile = Join-Path $env:TEMP "dailybudget-render-$PID"
New-Item -ItemType Directory -Force $out | Out-Null

# Serves the repo root so the store pages and the app share an origin (and localStorage).
$server = Start-Process powershell -PassThru -WindowStyle Hidden -ArgumentList @(
    '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $root 'tools\serve.ps1'), '-Port', $port, '-Root', $root)

function Render($url, $file, $width, $height, $scale, [switch]$Alpha) {
    $png = Join-Path $out $file
    $tmp = "$png.tmp.png"
    Start-Process $edge -Wait -WindowStyle Hidden -ArgumentList @(
        '--headless=new', '--disable-gpu', '--hide-scrollbars', '--no-first-run',
        "--user-data-dir=$profile", "--window-size=$width,$height",
        "--force-device-scale-factor=$scale", '--virtual-time-budget=5000',
        "--screenshot=$tmp", "`"$url`"")
    if (-not (Test-Path $tmp)) { throw "Edge did not write $file" }

    # Play wants a 32-bit icon and 24-bit (no alpha) feature graphic / screenshots.
    $src = [Drawing.Image]::FromFile($tmp)
    $format = if ($Alpha) { [Drawing.Imaging.PixelFormat]::Format32bppArgb } else { [Drawing.Imaging.PixelFormat]::Format24bppRgb }
    $bmp = New-Object Drawing.Bitmap $src.Width, $src.Height, $format
    $g = [Drawing.Graphics]::FromImage($bmp)
    $g.DrawImage($src, 0, 0, $src.Width, $src.Height)
    $g.Dispose(); $src.Dispose()
    $bmp.Save($png, [Drawing.Imaging.ImageFormat]::Png)
    Write-Output "$file  $($bmp.Width)x$($bmp.Height)"
    $bmp.Dispose()
    Remove-Item $tmp
}

function Q($s) { [Uri]::EscapeDataString($s) }

try {
    for ($i = 0; $i -lt 50; $i++) {
        try { Invoke-WebRequest "$base/icon.html" -UseBasicParsing -TimeoutSec 2 | Out-Null; break }
        catch { Start-Sleep -Milliseconds 200 }
    }

    Render "$base/icon.html" 'icon-512.png' 512 512 1 -Alpha
    Render "$base/feature.html" 'feature-graphic-1024x500.png' 1024 500 1

    $shots = @(
        @('budget',   'dark',  'Know what you can spend today', 'Your monthly budget, turned into a daily allowance'),
        @('expenses', 'dark',  'Log expenses in seconds',        'Every purchase this month, grouped by day'),
        @('calendar', 'dark',  'See how your month is going',    'Green days stayed on budget, red days went over'),
        @('budget',   'light', 'Light or dark, your choice',     'Plus a morning reminder with today''s budget')
    )
    $n = 1
    foreach ($s in $shots) {
        $url = "$base/screenshot.html?tab=$($s[0])&theme=$($s[1])&title=$(Q $s[2])&sub=$(Q $s[3])"
        Render $url "screenshot-$n-$($s[0])-$($s[1]).png" 540 960 2
        $n++
    }
} finally {
    Stop-Process -Id $server.Id -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force $profile -ErrorAction SilentlyContinue
}
