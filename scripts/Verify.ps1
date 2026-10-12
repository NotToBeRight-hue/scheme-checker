param([switch]$Database,[switch]$Browser,[switch]$Containers)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Load-Env.ps1')
$env:RUN_DB_TESTS=if($Database -or $Containers){'true'}else{'false'}
$env:USE_TESTCONTAINERS=if($Containers){'true'}else{'false'}
$applicationDbUrl=$env:DB_URL
if($Database -and !$Containers){if(!$env:TEST_DB_URL){throw 'Set TEST_DB_URL to an isolated MySQL test database.'};$env:DB_URL=$env:TEST_DB_URL}
Push-Location (Join-Path $projectRoot 'backend')
try { $ErrorActionPreference='Continue'; if($Browser){& ./mvnw.cmd test}else{& ./mvnw.cmd verify}; $nativeExit=$LASTEXITCODE; $ErrorActionPreference='Stop'; if ($nativeExit -ne 0) { throw 'Backend verification failed' } } finally { Pop-Location; $env:DB_URL=$applicationDbUrl }
$portableNode=Join-Path $projectRoot '.tools/node-v24.21.0-win-x64'
if(Test-Path -LiteralPath $portableNode){$env:Path=$portableNode+';'+$env:Path}
Push-Location (Join-Path $projectRoot 'frontend')
try { foreach($command in $(if($Browser){@('test','run build')}else{@('ci','test','run build')})) { if($command -eq 'run build'){& npm.cmd run build}else{& npm.cmd $command};if($LASTEXITCODE -ne 0){throw "Frontend $command failed"} }; if($Browser){& npm.cmd run test:e2e;if($LASTEXITCODE -ne 0){throw 'Browser tests failed'}} } finally { Pop-Location }
