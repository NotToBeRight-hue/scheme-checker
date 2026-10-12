$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$portableNode = Join-Path $projectRoot '.tools/node-v24.21.0-win-x64'
if (Test-Path -LiteralPath $portableNode) { $env:Path=$portableNode+';'+$env:Path }
Push-Location (Join-Path $projectRoot 'frontend')
try { & npm.cmd ci; if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }; & npm.cmd run dev -- --host 127.0.0.1; if ($LASTEXITCODE -ne 0) { throw 'Frontend exited with an error' } } finally { Pop-Location }
