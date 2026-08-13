# MOVEAI Front Checkpoint — Guidance G-08

기준: Figma `Move-AI` 디자인 / React + Vite + TypeScript / plain CSS

## 완료 범위

### 웹 인증
- 웹 로그인
- 웹 회원가입 / 기사 정보 입력

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

## 이번 체크포인트 추가 반영
- G-03 Figma 프레임 기준 지도/참고 사진 스트립/안내 시작 UI 반영
- G-04 지도 컨트롤, 우회전 현재 단계 카드, 안내 종료 UI 반영
- G-06 완료 그래픽, 거리·소요시간, 후문→정차→엘리베이터 요약, 후속 CTA 반영
- G-08 경로 없음 상태, 차량 진입 제약 안내, 차량 조건/장소 정보/종료 액션 반영
- 화면 간 이동: G-03 → G-04 → G-06, 각 종료/후속 액션 라우팅 연결
- 해당 화면에 최신 Figma MCP asset URL 반영

## 다음 작업 순서
- 웹/관리자 화면 전체 프레임 목록 재확인
- 관리자 대시보드부터 Figma 순서대로 구현

## 참고
- Git commit / push 하지 않은 로컬 체크포인트입니다.
- Figma MCP asset URL은 임시 URL입니다. 현재 실행 확인용으로 사용하며 실제 병합 전에 프로젝트 asset/CDN 정책에 맞춰 영구 자산화가 필요합니다.
- 이 실행 환경은 Figma asset URL을 직접 다운로드할 DNS 접근이 없어 체크포인트 내부에 asset 바이너리를 저장하지 못했습니다.

## 검증
- G-03 / G-04 / G-06 / G-08 TSX는 TypeScript `transpileModule` 기준 구문 검사를 통과했습니다.
- App.tsx에 `/guidance/preview`, `/guidance/step`, `/guidance/completed`, `/guidance/unavailable` 라우트가 연결되어 있습니다.
- 전체 `npm run build`는 체크포인트에 `node_modules`가 포함되어 있지 않아 실행하지 않았습니다.
