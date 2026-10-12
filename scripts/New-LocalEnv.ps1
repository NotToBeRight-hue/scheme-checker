$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $projectRoot '.env'
if (Test-Path -LiteralPath $envFile) { throw '.env already exists. It was left unchanged.' }
function New-Secret { $bytes=New-Object byte[] 48; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes); return [Convert]::ToBase64String($bytes) }
$dbSecret=New-Secret
$rootSecret=New-Secret
$jwtSecret=New-Secret
$adminSecret=New-Secret
@"
MYSQL_DATABASE=schemes
MYSQL_USER=schemes_app
MYSQL_PASSWORD=$dbSecret
MYSQL_ROOT_PASSWORD=$rootSecret
DB_URL=jdbc:mysql://localhost:3306/schemes
TEST_DB_URL=jdbc:mysql://localhost:3306/schemes_test
DB_USER=schemes_app
DB_PASSWORD=$dbSecret
JWT_SECRET=$jwtSecret
SPRING_PROFILES_ACTIVE=dev
ADMIN_EMAIL=admin@example.test
ADMIN_PASSWORD=$adminSecret
DEBUG=false
"@ | Set-Content -LiteralPath $envFile -Encoding ASCII
Write-Output 'Created private .env with random credentials. Read the local file to sign in as administrator. Remove ADMIN_PASSWORD after first provisioning.'
