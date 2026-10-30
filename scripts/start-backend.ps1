
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $project '.env'))) {
  throw 'Missing .env in project root. Run: Copy-Item .env.example .env and configure DB + admin credentials.'
}
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Apache Maven (mvn) is not installed or not in PATH.' }
Set-Location (Join-Path $project 'backend')
mvn spring-boot:run
