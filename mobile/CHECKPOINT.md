# MOVEAI Front Checkpoint — Mobile Auth + Admin Complete

기준: Figma `Move-AI` 디자인 / React + Vite + TypeScript / plain CSS

## 완료 범위

### 인증 화면
- 데스크톱 로그인
- 데스크톱 회원가입 / 기사 정보 입력
- 모바일 로그인 (`118:9520`)
- 모바일 회원가입 약관동의 (`118:9469`)
- 모바일 회원정보 입력 (`118:9568`)
- 반응형 인증 라우팅: `/login`, `/signup`, `/signup/info`

### 모바일 현장 팁 흐름
- M-01 메인 화면
- M-02 음성 녹음
- M-03 음성 업로드
- M-04 STT 결과 확인
- M-06/07 장소 지정
- M-08 최종 제보 확인
- M-10 작성 중인 제보 목록
- M-11 내 제보 내역

### 모바일 Last 100m 안내 흐름
- G-03 경로 미리보기
- G-04 단계별 길안내
- G-06 배달 및 안내 완료
- G-08 경로 정보 없음

### 관리자 화면
- A-01 관리자 대시보드
- A-02 검수 대기 목록
- A-03 제보 검수 상세
- A-07 장소 내부 지점 관리
- A-08 경로 생성 및 편집
- A-09 경로별 지식 연결
- A-10 경로 미리보기 및 검증

## Figma 전체 프레임 대조 결과
- PC 페이지의 관리자 프레임은 A-01/A-02/A-03/A-07/A-08/A-09/A-10으로 구성되어 있음.
- 현재 Figma PC 페이지에 A-04/A-05/A-06 프레임은 존재하지 않음.
- MO 페이지의 서비스 화면과 모바일 인증 3종까지 코드에 반영함.

## 참고
- Git commit / push 하지 않은 로컬 체크포인트입니다.
- Figma MCP asset URL은 임시 URL입니다. 실제 병합 전 프로젝트 asset/CDN 정책에 맞춰 영구 자산화가 필요합니다.

## 이번 체크포인트 검증
- 전체 `src` TS/TSX 31개 파일을 TypeScript `transpileModule`로 구문 검증 완료.
- `src/styles/global.css` 중괄호 구조 검증 완료.
- `npm install --no-audit --no-fund`를 시도했으나 실행 환경의 패키지 다운로드가 120초 내 완료되지 않아 전체 `npm run build`는 실행하지 못함.
