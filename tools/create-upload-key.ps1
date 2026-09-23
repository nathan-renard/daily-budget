# Creates the Play Store upload key (upload-keystore.jks) and keystore.properties in the repo root.
# Both are git-ignored. Back them up somewhere safe: every future update must be signed with this key.
# Usage: powershell -ExecutionPolicy Bypass -File tools/create-upload-key.ps1
$ErrorActionPreference = 'Stop'

$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$keystore = Join-Path $root 'upload-keystore.jks'
$props = Join-Path $root 'keystore.properties'
if ((Test-Path $keystore) -or (Test-Path $props)) {
    throw "upload-keystore.jks or keystore.properties already exists; refusing to overwrite."
}

$keytool = 'keytool'
if ($env:JAVA_HOME) { $keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe' }

# Random password (PKCS12 keystores use the same password for store and key).
$bytes = New-Object byte[] 24
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$password = [Convert]::ToBase64String($bytes).Replace('+', 'A').Replace('/', 'B').Replace('=', '')

& $keytool -genkeypair -v -keystore $keystore -storetype PKCS12 -alias upload `
    -keyalg RSA -keysize 2048 -validity 10000 `
    -storepass $password -keypass $password -dname 'CN=Daily Budget'
if ($LASTEXITCODE -ne 0) { throw "keytool failed" }

@"
storeFile=upload-keystore.jks
storePassword=$password
keyAlias=upload
keyPassword=$password
"@ | Set-Content -Encoding ascii $props

Write-Output "Created $keystore and $props. Back up both files."
