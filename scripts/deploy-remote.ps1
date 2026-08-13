$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot

# Docker Desktop의 credential helper는 SSH 비대화형 로그온 세션에서 실패할 수 있다.
# 공개 베이스 이미지만 사용하므로 배포 전용 빈 인증 설정을 별도로 사용한다.
$dockerConfig = Join-Path $projectRoot '.docker-ci'
New-Item -ItemType Directory -Path $dockerConfig -Force | Out-Null

$config = @{
    auths = @{ 'https://index.docker.io/v1/' = @{} }
    credsStore = ''
    cliPluginsExtraDirs = @((Join-Path $env:USERPROFILE '.docker\cli-plugins'))
} | ConvertTo-Json -Depth 4 -Compress

[System.IO.File]::WriteAllText(
    (Join-Path $dockerConfig 'config.json'),
    $config,
    [System.Text.UTF8Encoding]::new($false)
)

$env:DOCKER_CONFIG = $dockerConfig
$env:DOCKER_HOST = 'npipe:////./pipe/dockerDesktopLinuxEngine'

$baseImages = @(
    'python:3.12-slim',
    'gradle:8.10-jdk17',
    'eclipse-temurin:17-jre',
    'mariadb:11.4',
    'caddy:2.10-alpine'
)

foreach ($image in $baseImages) {
    docker pull $image
    if ($LASTEXITCODE -ne 0) {
        throw "Docker image pull failed: $image"
    }
}

docker compose up -d --build
if ($LASTEXITCODE -ne 0) {
    throw 'docker compose up failed'
}

docker compose ps
if ($LASTEXITCODE -ne 0) {
    throw 'docker compose ps failed'
}
