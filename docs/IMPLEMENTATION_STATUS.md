# Implementation Status

> 하네스는 **세션 시작 시 이 파일을 먼저 읽고, 세션 종료 전 반드시 갱신**한다.
> Phase 완료 조건은 `MOVE_AI_05_구현_상세명세.md §7`에 있다.

**최종 갱신** — (미시작)
**현재 Phase** — Phase 0 (미착수)

---

## 다음에 할 정확한 작업

```
Phase 0 — repository audit
  tree / git status / 빌드 파일 / 기존 코드 확인 후 이 파일을 갱신한다.
  바로 대규모 리팩터링을 시작하지 않는다.
```

## 현재 blocker

```
없음
```

## 마지막으로 검증한 명령

```
python scripts/validate_datasets.py   → 전체 이슈 0건 (본선 전 확인 완료)
```

---

# Phase 진행 상황

범례 — `TODO` / `PARTIAL` / `DONE`

## Phase 0 — Repository Audit  `TODO`
- [ ] tree / git status 확인
- [ ] 빌드·환경 파일 확인
- [ ] 이 파일 갱신

## Phase 1 — Runtime Skeleton  `TODO`
- [ ] frontend 페이지 열림
- [ ] backend `/health` 200
- [ ] ai-service `/health` 200
- [ ] MariaDB 연결 성공

## Phase 2 — Dataset Import  `TODO`
- [ ] DDL 적용 (`05 §2`)
- [ ] B 임포트 + 검증 통과
- [ ] A / C / D 임포트
- [ ] `GET /api/places` 실제 응답
- [ ] `GET /api/routes/{id}` 실제 응답
- [ ] 장소 4 · 지식 146건 확인

## Phase 3 — Embedding + Retrieval  `TODO`
- [ ] embedding_text 빌더
- [ ] PUBLISHED 146건 일괄 임베딩 → DB 저장
- [ ] `ConditionEvaluator` 단위 테스트
- [ ] **톤수 경계 8건 테스트 통과 (`05 §3-3`)**
- [ ] `CosineCalculator` 단위 테스트
- [ ] 정답 질문 20개 평가 스크립트 동작, Hit@3 수치 확보

## Phase 4 — Guidance  `TODO`  ★ 첫 발표 가능 지점
- [ ] 1톤 → `ROUTE_B_01` (7단계) 선택
- [ ] 2.5톤 → `ROUTE_B_02` (3단계) 선택
- [ ] 같은 지식이 연속 두 단계에 중복 노출되지 않음
- [ ] 모든 단계에 카드 최소 1장
- [ ] `contextTime=12:30` → `K_B_014` 노출 / `15:00` → 사라짐
- [ ] `complete` 동작

## Phase 5 — Field Report + STT  `TODO`
- [ ] 녹음 → 업로드 → STT
- [ ] 기사 텍스트 수정 → 저장

## Phase 6 — Extraction  `TODO`
- [ ] `corrected_stt_text` → Draft 생성
- [ ] 스키마 검증 + 1회 재시도 + 실패 처리
- [ ] 데이터셋 transcript로 품질 확인

## Phase 7 — Moderation + Publish  `TODO`  ★ 핵심 완성 지점
- [ ] 검수 화면 (원문 / AI 결과 / 근거 구절 나란히)
- [ ] 승인 → PUBLISHED → 임베딩 (동기, 한 트랜잭션)
- [ ] 새 제보 승인 후 같은 경로 재시작 시 새 카드 노출
- [ ] `isRecentlyAdded = true` 배지

## Phase 8 — Polish  `TODO`
- [ ] 로딩 / 오류 / 빈 상태
- [ ] 카드 우선순위
- [ ] 시연 동선 고정

## Phase 9 — Reply Assist (P1)  `TODO`
- [ ] P0 안정 확인 후에만 착수

---

# 변경 이력

세션마다 아래 형식으로 **위에** 추가한다.

```
## YYYY-MM-DD HH:MM  Phase N
변경   …
검증   실행한 명령 / 결과
남은것 …
다음   …
```

<!-- 여기부터 기록 -->
