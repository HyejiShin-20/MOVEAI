# MOVE-AI 배포 작업 이관

작성일: 2026-08-13  
작업 경로: `E:\MOVE-AI`  
브랜치: `main`

## 현재 상태

- 프론트 GitHub Pages 배포는 유지한다.
  - URL: `https://hyejishin-20.github.io/MOVEAI/`
  - 기존 워크플로: `.github/workflows/deploy.yml`
- 백엔드는 PDF의 Windows SSH 방식으로 배포하도록 별도 구성했다.
  - 신규 워크플로: `.github/workflows/deploy-backend.yml`
  - 원격 팀원 PC 작업 경로: `C:\MOVEAI`
  - 공개 API 예정 URL: `https://121-166-129-218.sslip.io`
- Docker Compose 서비스:
  - `database`: MariaDB 11.4
  - `ai-service`: FastAPI + Gemini
  - `backend`: Spring Boot
  - `caddy`: HTTPS 및 Spring reverse proxy
- 최초 데이터 적재용 `dataset-import`, `embedding-build`, `embedding-import` 프로필도 준비했다.
- GitHub Repository secrets에 아래 이름이 등록되어 있다.
  - `SERVER_HOST`, `SERVER_USER`, `SERVER_PASSWORD`, `SERVER_PORT`
  - `API_DOMAIN`, `CORS_ALLOWED_ORIGINS`, `VITE_API_BASE_URL`
  - 기존 DB·Gemini·STT·Kakao 관련 secrets
- `docker compose config --quiet` 통과.
- FastAPI와 Spring 운영 Docker 이미지 실제 빌드 성공.
- 커밋과 push는 하지 않았다.
- 원격 SSH `home@121.166.129.218:22` 접속과 비밀번호 인증에 성공했다.
- 원격 `C:\MOVEAI`에 GitHub main(`5081570`)을 clone했다.
- 원격 Git 2.55.0, Docker Compose 5.3.1 설치를 확인했다.
- 원격 Docker Desktop Linux 엔진 29.6.2 실행을 확인했다.
- SSH 비대화형 세션에서 Docker Desktop credential helper가 실패하는 문제를 확인했고,
  배포 전용 빈 인증 설정으로 공개 이미지 pull과 FastAPI·Spring 실제 빌드에 성공했다.

## 현재 차단 사항

배포 대상은 현재 로컬 PC가 아니라 PDF에 적힌 다른 팀원의 원격 Windows PC다. 공인 IP는 `121.166.129.218`, SSH 22번 포트는 개방 완료로 전달받았다. 원격 PC에서 TCP 80·443 Windows 방화벽 허용과 공유기 포트포워딩을 추가로 완료해야 Caddy가 HTTPS 인증서를 발급할 수 있다.

SSH 연결과 원격 Docker Desktop Linux 엔진까지 실제로 확인했다. 푸시 전 임시 파일로 Compose 문법 검사와 FastAPI·Spring 이미지 빌드도 통과했다. 남은 검증은 push 후 GitHub Actions 실행과 원격 80·443 HTTPS 확인이다.

또한 PDF에 있던 서버 사용자·비밀번호가 실제 이 PC의 Windows 로그인 정보인지 반드시 확인해야 한다. 현재 Windows 사용자는 `shyej`다. PDF 값이 예시라면 `SERVER_USER`, `SERVER_PASSWORD`를 실제 SSH 계정 값으로 교체한다.

## 다음 세션 실행 순서

> 로컬에서는 Compose 빌드·초기 적재·API·CORS smoke test까지 통과했으며 테스트용 컨테이너는 제거했다. 실제 원격 DB 최초 적재는 push 후 원격 PC에서 아래 5단계를 실행한다.

### 1. OpenSSH 활성화

관리자 PowerShell에서 실행한다. 외부 원격 로그인을 여는 보안 변경이므로 사용자의 명시적 승인 후 진행한다.

```powershell
Start-Service sshd
Set-Service sshd -StartupType Automatic
Get-Service sshd
Get-NetTCPConnection -State Listen -LocalPort 22
```

Windows 방화벽 규칙도 확인한다.

```powershell
Get-NetFirewallRule -DisplayName '*OpenSSH*' | Format-Table DisplayName, Enabled, Direction, Action
```

필요할 때만 관리자 PowerShell에서 inbound 규칙을 만든다.

```powershell
New-NetFirewallRule -Name 'OpenSSH-Server-In-TCP' -DisplayName 'OpenSSH Server (sshd)' -Enabled True -Direction Inbound -Protocol TCP -Action Allow -LocalPort 22
```

공유기에서 외부 TCP 22를 이 PC의 내부 IP TCP 22로 포트포워딩한다. 설정 후 외부 연결을 다시 확인한다.

```powershell
Test-NetConnection 121.166.129.218 -Port 22
```

### 2. GitHub Secrets 재확인

GitHub의 `Settings → Secrets and variables → Actions`에서 확인한다.

- `SERVER_HOST`: 실제 서버 공인 IP
- `SERVER_USER`: 실제 Windows SSH 사용자
- `SERVER_PASSWORD`: 위 사용자의 실제 비밀번호
- `SERVER_PORT`: 외부 SSH 포트
- `API_DOMAIN`: `121-166-129-218.sslip.io`
- `CORS_ALLOWED_ORIGINS`: `https://hyejishin-20.github.io`
- `VITE_API_BASE_URL`: `https://121-166-129-218.sslip.io`

Kakao Developers에도 배포 도메인 `https://hyejishin-20.github.io`를 Web 플랫폼 허용 도메인으로 등록한다.

### 3. 변경 검증

Git Bash에서 실행한다.

```bash
cd /e/MOVE-AI
API_DOMAIN=121-166-129-218.sslip.io docker compose config --quiet
docker compose build ai-service backend
git diff --check
git status --short
```

### 4. 커밋과 push

사용자가 직접 실행한다.

```bash
cd /e/MOVE-AI
git add .env.example .dockerignore Caddyfile docker-compose.yml \
  ai-service/Dockerfile ai-service/.dockerignore \
  backend/.dockerignore \
  .github/workflows/deploy-backend.yml \
  scripts/deploy-remote.ps1 scripts/enable-deployment-firewall.ps1 \
  docs/DEPLOYMENT.md docs/DEPLOYMENT_HANDOFF.md docs/IMPLEMENTATION_STATUS.md
git commit -m "백엔드 운영 배포 환경 구성"
git push origin main
```

### 5. GitHub Actions 확인

`Actions → Deploy backend to Windows server`에서 실행 결과를 확인한다. main push의 변경 경로가 조건과 맞으면 자동 실행된다. 필요하면 `Run workflow`로 수동 실행한다.

첫 배포 후 서버에서 아래 순서로 최초 데이터만 적재한다. `dataset-import`는 런타임 데이터를 초기화하므로 최초 1회만 실행한다.

```powershell
cd C:\MOVEAI
docker compose up -d --build database ai-service
docker compose --profile init run --rm dataset-import
docker compose --profile init run --rm embedding-build
docker compose --profile init run --rm embedding-import
docker compose up -d --build
docker compose ps
```

### 6. 최종 확인

```powershell
Invoke-RestMethod https://121-166-129-218.sslip.io/health
docker compose logs --tail 100 backend ai-service caddy
```

브라우저에서 다음을 확인한다.

1. `https://hyejishin-20.github.io/MOVEAI/` 접속
2. 로그인 화면 표시
3. 배송 목록 조회
4. 안내 세션 생성 및 단계 이동
5. 음성 제보/STT 호출
6. 관리자 Draft 승인과 신규 지식 반영

## 이번 작업에서 변경한 파일

- `.env.example`
- `.dockerignore`
- `Caddyfile`
- `docker-compose.yml`
- `ai-service/Dockerfile`
- `ai-service/.dockerignore`
- `backend/.dockerignore`
- `.github/workflows/deploy-backend.yml`
- `scripts/enable-deployment-firewall.ps1`
- `scripts/deploy-remote.ps1`
- `docs/DEPLOYMENT.md`
- `docs/DEPLOYMENT_HANDOFF.md`
- `docs/IMPLEMENTATION_STATUS.md`

## 주의

- 기존 `.github/workflows/deploy.yml`은 GitHub Pages 전용이며 이번 작업에서 변경하지 않았다.
- `.env`와 비밀값은 커밋하지 않는다.
- Actions가 쓰는 원격 서버 경로는 `C:\MOVEAI`다.
- 서버의 80·443 포트가 외부에 열려 있어야 Caddy가 인증서를 발급할 수 있다.
- `VITE_API_BASE_URL` secret 변경은 프론트 Pages를 다시 빌드해야 반영된다.
