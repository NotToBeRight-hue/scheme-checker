$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
Push-Location (Join-Path $projectRoot 'backend')
try { & ./mvnw.cmd spring-boot:run; if ($LASTEXITCODE -ne 0) { throw "Backend exited with $LASTEXITCODE" } } finally { Pop-Location }
