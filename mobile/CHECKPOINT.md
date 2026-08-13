# MOVEAI Front Checkpoint — Navigation / Draft Flow Integration

기준: Figma `Move-AI` 디자인 / React + Vite + TypeScript / plain CSS

## 기존 구현 완료 범위

### 인증
- 데스크톱 로그인
- 데스크톱 회원가입 / 기사 정보 입력
- 모바일 로그인
- 모바일 회원가입 약관동의
- 모바일 회원정보 입력

### 모바일 현장 팁
- M-01 메인
- M-02 음성 녹음
- M-03 음성 업로드
- M-04 STT 결과 확인
- M-06/07 장소 지정
- M-08 최종 제보 확인
- M-10 작성 중인 제보 목록
- M-11 내 제보 내역

### 모바일 Last 100m 안내
- G-03 경로 미리보기
- G-04 단계별 길안내
- G-06 배달 및 안내 완료
- G-08 경로 정보 없음

### 관리자
- A-01 관리자 대시보드
- A-02 검수 대기 목록
- A-03 제보 검수 상세
- A-07 장소 내부 지점 관리
- A-08 경로 생성 및 편집
- A-09 경로별 지식 연결
- A-10 경로 미리보기 및 검증

## 이번 작업 단위

### 1. 공통 네비게이션 정리
- `src/routes.ts` 추가: 주요 프론트 라우트 상수화
- 공통 `BottomNav` 실제 라우팅 연결
  - 배송 → `/home`
  - 현장 팁 → `/reports/record`
  - 경로안내 → `/guidance/preview`
  - MY → `/reports/mine`
- 현재 화면에 맞는 bottom nav active 상태 처리
- 메인 상단 메뉴 → 작성 중인 제보
- 메인 프로필 → 내 제보 내역
- 최근 검색 장소 → 경로 미리보기
- STT 화면 하단 Navigation / Profile 버튼 연결
- 내 제보 내역 하단 메뉴 상태 및 이동 수정

### 2. 제보 작성 상태 연속성
- `src/state/reportDraft.ts` 추가
- localStorage 기반 임시 작성 상태 저장
- 음성 녹음 → 업로드 → STT → 장소 지정 → 최종 확인 단계 상태 유지
- STT 수정 텍스트 저장
- 선택 장소/세부 구역 저장
- 최종 확인 화면이 저장된 장소/텍스트를 표시하도록 연결
- `나중에 이어서 등록` 후 M-10에서 현재 작성 건을 다시 이어갈 수 있도록 연결
- 등록 완료 시 현재 draft 제거

### 3. API 연결 경계 정리
- 기존 `src/api/client.ts`의 배송/길안내 HTTP client 유지
- `src/api/reportRepository.ts` 추가
- 제보 UI가 향후 특정 HTTP endpoint 구현에 직접 결합되지 않도록 `ReportRepository` 인터페이스 정의
- 현재 checkpoint는 browser/mock repository를 사용하고, 백엔드 제보 API 계약 확정 후 구현체만 교체할 수 있도록 구성

### 4. 인증 동작 보완
- 모바일 회원가입 1단계에서 필수 약관(이용약관/정보처리방침) 동의 전 `다음` 버튼 비활성화
- 선택 마케팅 동의는 다음 단계 진입 필수 조건에서 제외

## 검증
- `src` TS/TSX 34개 파일 TypeScript `transpileModule` 구문 검사 통과
- `src/styles/global.css` brace balance 통과 (980 / 980)
- `public/assets` 로컬 에셋 파일 존재/빈 파일 여부 검사 통과
- `npm install --no-audit --no-fund --prefer-offline` 재시도했으나 실행 환경에서 180초 timeout
- 따라서 실제 `npm run build`는 이 환경에서 미실행

## 참고
- Git commit / push 하지 않은 로컬 체크포인트입니다.
- Figma MCP asset URL은 임시 URL이므로 실제 통합 전 영구 asset/CDN 정책으로 이전이 필요합니다.
- 비밀번호 찾기, 주소 검색, 사진 추가, 관리자 설정/지원처럼 별도 Figma 화면이 없는 액션은 새 화면을 임의 생성하지 않았습니다.

## QA / 반응형 점검 추가

### 모바일
- 320~389px에서 하단 네비게이션 4개 항목이 가로로 넘치지 않도록 flex 고정폭 구조를 4열 grid로 보강
- M-08 사진 3칸 영역을 고정 96px 대신 반응형 3열 grid로 변경해 320px 화면에서도 카드 밖으로 넘치지 않도록 수정
- G-06 경로 요약 연결선이 남는 공간을 유동적으로 사용하도록 변경
- 최근 장소명/주소, 내 제보 날짜 영역에 ellipsis 처리 추가
- G-03/G-04는 845px 고정 캔버스 대신 `100dvh` 기준으로 동작하도록 보강하고 Figma 845px 기준 위치는 유지
- 짧은 화면에서 업로드 상태/경로 없음 액션의 과도한 상단 여백을 `clamp()`로 완화
- iOS 홈 인디케이터 영역을 고려해 fixed bottom nav/action에 `safe-area-inset-bottom` 적용
- 모바일 인증/녹음/업로드/STT 화면은 667px 이상 또는 현재 동적 viewport 높이를 기준으로 최소 높이 처리

### 웹/관리자
- 1280px Figma 원본 레이아웃은 유지
- 1024~1279px에서 관리자 sidebar를 220px로 축소하고 dashboard metric/filter/detail grid가 재배치되도록 보강
- A-02 고정 폭 테이블은 화면 전체가 깨지는 대신 테이블 카드 내부 가로 스크롤로 처리
- A-07 장소 관리 상세 grid를 2열/1열로 단계적 축소
- A-08~A-10 route admin의 sidebar/topbar/main margin을 viewport에 맞춰 조정
- route validation simulation control과 validation grid가 좁은 화면에서 2열/1열로 재배치되도록 처리
- 900px 이하에서는 관리자 sidebar를 숨기고 content가 전체 폭을 사용하도록 fallback 적용
- 좁은 관리자 topbar의 title/breadcrumb는 ellipsis 처리

### QA 검증
- TypeScript `transpileModule` 기준 TS/TSX 35개 구문 오류 0건
- PostCSS parser 기준 `src/styles/global.css` 파싱 성공
- 상대 import 누락 0건
- `/public/assets` 로컬 참조 누락/빈 파일 0건
- `npm ping` 결과 실행 환경 DNS에서 `registry.npmjs.org`가 `EAI_AGAIN`으로 해석되지 않아 dependency 설치 및 실제 Vite build는 실행 불가
- Playwright + 시스템 Chromium으로 CSS fixture viewport QA 수행: 320 / 390 / 768 / 900 / 1024 / 1280px에서 document-level 가로 overflow 0 확인
- 320×667 / 390×667 / 390×845 / 430×932에서 G-03, G-04, 모바일 회원가입 주요 고정/절대 배치가 서로 겹치지 않는지 bounding-box 검증 완료
- A-02 고정 폭 테이블은 1024px 이하에서 페이지 전체 overflow가 아니라 table card 내부 overflow로 한정되는지 확인

### 남은 외부 의존성 리스크
- 소스에 Figma MCP 임시 asset URL이 다수 남아 있음. 이 URL은 영구 자산이 아니므로 실제 배포 전 로컬/public asset 또는 프로젝트 CDN으로 이전 필요
