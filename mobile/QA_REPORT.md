# MOVEAI Front QA Report

검수 대상: `MOVEAI_front_checkpoint_integration` 기반 전체 프론트 화면

## 점검 범위

- 웹 인증: 로그인, 회원가입/기사 정보 입력
- 모바일 인증: 로그인, 약관동의, 회원정보 입력
- 모바일 현장 팁: M-01, M-02, M-03, M-04, M-06/07, M-08, M-10, M-11
- Last 100m 안내: G-03, G-04, G-06, G-08
- 관리자: A-01, A-02, A-03, A-07, A-08, A-09, A-10

## 수정한 주요 QA 이슈

1. 320px 모바일에서 하단 네비게이션 고정 폭 합계가 viewport를 초과할 가능성
   - 4열 grid 기반으로 변경
   - 320/390px에서 document horizontal overflow 없음 확인

2. M-08 사진 3개 영역의 96px 고정 폭
   - 반응형 3열 grid + `aspect-ratio: 1`로 변경
   - 320px에서도 카드 내부 overflow 없음 확인

3. G-06 경로 요약의 83px 고정 연결선
   - 연결선만 남는 공간을 flex로 사용하도록 변경
   - 노드 아이콘/라벨 너비는 유지

4. G-03/G-04의 845px 고정 viewport 의존
   - `100dvh` 기반으로 보강
   - 390×845 Figma 기준 배치는 유지
   - 320×667 / 390×667에서도 안내 카드, 종료 버튼, 하단 CTA가 겹치지 않음 확인

5. 최근 장소/내 제보의 긴 문자열
   - ellipsis와 `min-width: 0` 보강

6. 모바일 fixed action의 safe-area 미대응
   - bottom nav, confirm action, draft footer에 `env(safe-area-inset-bottom)` 반영

7. 1024~1279px 관리자 화면의 고정 sidebar/grid
   - sidebar 220px fallback
   - KPI 2열, filter 2열, 상세 grid 재배치
   - A-02 테이블은 card 내부 horizontal scroll로 제한

8. A-08~A-10 route admin의 1280px 최소 폭 강제
   - 1024~1279px용 sidebar/main/grid fallback 추가
   - 900px 이하에서는 sidebar를 숨기고 content 전체 폭 사용
   - validation control 및 결과 grid를 단계적으로 2열/1열 재배치

9. 관리자 페이지 pagination button의 `type` 누락
   - 6개 button에 `type="button"` 추가

## 자동 검증 결과

- TS/TSX 파일: 35개
- TypeScript `transpileModule` syntax diagnostics: 0
- PostCSS parse: 성공
- 상대 import 누락: 0
- 로컬 `/public/assets` 누락/빈 파일: 0
- JSX `<img>` `alt` 누락: 0
- JSX `<button>` `type` 누락: 0

### Viewport CSS fixture 검사

Playwright + 시스템 Chromium의 `page.set_content()`로 핵심 레이아웃 fixture를 렌더링했다.

- Mobile: 320, 390, 768, 900, 1024, 1280px
- Admin: 320, 390, 768, 900, 1024, 1280px
- Route Admin: 320, 390, 768, 900, 1024, 1280px

모든 테스트 viewport에서 `documentElement.scrollWidth === clientWidth` 확인.
A-02 테이블은 의도적으로 table card 내부에서만 scrollWidth가 증가하도록 유지했다.

추가로 다음 height 조합에서 절대/고정 배치 bounding box를 확인했다.

- 320×667
- 390×667
- 390×845
- 430×932

검사 대상: G-03 사진 strip/시작 CTA, G-04 단계 카드/종료 CTA, 모바일 회원가입 약관 card/다음 CTA.
겹침 없음.

## 실행 환경 때문에 미완료인 검증

`npm ping`이 `registry.npmjs.org` DNS `EAI_AGAIN`으로 실패하여 dependency 설치가 불가능했다. 따라서 실제 `npm run build` 및 React 앱 전체 E2E 렌더링은 이 환경에서는 실행하지 못했다.

## 배포 전 반드시 처리할 항목

현재 소스에는 Figma MCP 임시 asset URL이 다수 남아 있다. 이 URL은 영구 배포용이 아니므로 실제 배포 전에 `public/assets` 또는 프로젝트에서 사용하는 CDN/스토리지로 이전해야 한다.
