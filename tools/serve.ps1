# Minimal static file server for previewing the web UI in a desktop browser.
# Usage: powershell -File tools/serve.ps1 [-Port 8765]
param([int]$Port = 8765)

$root = Resolve-Path (Join-Path $PSScriptRoot '..\app\src\main\assets\www')
$types = @{ '.html' = 'text/html'; '.js' = 'text/javascript'; '.css' = 'text/css'; '.svg' = 'image/svg+xml'; '.png' = 'image/png' }

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://localhost:$Port/")
$listener.Start()
Write-Output "Serving $root at http://localhost:$Port/"

while ($listener.IsListening) {
    $ctx = $listener.GetContext()
    $path = [Uri]::UnescapeDataString($ctx.Request.Url.AbsolutePath.TrimStart('/'))
    if (-not $path) { $path = 'index.html' }
    $file = [IO.Path]::GetFullPath((Join-Path $root $path))
    if ($file.StartsWith($root.Path) -and (Test-Path $file -PathType Leaf)) {
        $bytes = [IO.File]::ReadAllBytes($file)
        $ext = [IO.Path]::GetExtension($file)
        $ctx.Response.ContentType = if ($types[$ext]) { $types[$ext] } else { 'application/octet-stream' }
        $ctx.Response.OutputStream.Write($bytes, 0, $bytes.Length)
    } else {
        $ctx.Response.StatusCode = 404
    }
    $ctx.Response.Close()
    Write-Output "$($ctx.Response.StatusCode) /$path"
}
