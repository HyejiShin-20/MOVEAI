# MOVE-AI 배포

프론트는 GitHub Pages, Spring·FastAPI·MariaDB는 PDF의 Windows SSH 서버 방식으로 배포한다.

## 1. 서버 최초 1회 준비

1. 원격 팀원 PC의 `C:\MOVEAI`에 저장소를 둔다.
2. 저장소 루트의 `.env.example`을 `.env`로 복사하고 운영 값을 입력한다.
3. 공유기·Windows 방화벽에서 TCP 22, 80, 443을 허용한다.
4. `API_DOMAIN`은 공인 IP 기반 `121-166-129-218.sslip.io`를 사용한다. GitHub Pages가 HTTPS이므로 API도 HTTPS여야 한다.

Windows 방화벽 80·443 규칙은 관리자 PowerShell에서 다음 스크립트로 설정할 수 있다.

```powershell
Set-Location C:\MOVEAI
.\scripts\enable-deployment-firewall.ps1
```

처음 데이터베이스를 만들 때만 아래 명령을 순서대로 실행한다. `dataset-import`는 런타임 데이터를 초기화하므로 재배포 때 반복하지 않는다.

```powershell
cd C:\MOVEAI
docker compose up -d --build database ai-service
docker compose --profile init run --rm dataset-import
docker compose --profile init run --rm embedding-build
docker compose --profile init run --rm embedding-import
docker compose up -d --build
```

## 2. GitHub Actions Secrets

`SERVER_HOST`, `SERVER_USER`, `SERVER_PASSWORD`, `SERVER_PORT`와 운영 환경변수를 Repository secrets에 등록한다. 이후 main의 배포 관련 파일이 바뀌면 `.github/workflows/deploy-backend.yml`이 원격 PC의 `C:\MOVEAI`에서 `.env` 생성·pull·build·restart를 수행한다.

프론트 빌드용 `VITE_API_BASE_URL`에는 `https://121-166-129-218.sslip.io`를 넣는다. Spring CORS의 `CORS_ALLOWED_ORIGINS`는 `https://hyejishin-20.github.io`로 둔다.

## 3. 확인

```powershell
docker compose ps
docker compose logs --tail 100 backend ai-service caddy
```

- API: `https://121-166-129-218.sslip.io/health`
- 프론트: `https://hyejishin-20.github.io/MOVEAI/`
