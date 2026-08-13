# 02 — 하네스 엔지니어링 운용 규칙

> 현재 적용되는 하네스 규칙만 담는다. 폐기된 초안은 `archive/`에 있으며
> 구현 판단에 사용하지 않는다. 진행 상태와 완료 체크는 `IMPLEMENTATION_STATUS.md`만 갱신한다.

## 문서 우선순위

```text
사용자 지시 → 01(MVP PRD) → 05A/05B/05C → 04 → 02 → 기존 코드
```

- `05A`: DB 스키마와 데이터 임포트의 유일한 출처
- `05B`: Spring/Python API 계약의 유일한 출처
- `05C`: 구현 순서, 책임 경계, 테스트와 컷라인
- `04`: 검색, 조건 평가, 랭킹, Guidance 내부 로직

## 한 명의 하네스, 분리된 사람 트랙

```text
R1 Harness  → backend/ + ai-service/
R2 Human    → mobile/ (React 기사용 모바일 웹)
R3 Human    → admin-web/ (React 관리자 웹)
R4 Human    → 발표·문서
```

하네스 세션을 Backend/AI Track으로 나누지 않는다. Spring과 FastAPI의 계약을 한 세션에서
유지하고, 화면 담당자는 `05B` 계약을 기준으로 Mock부터 만든다.

## 작업 루프

1. `CLAUDE.md`와 `IMPLEMENTATION_STATUS.md`를 읽는다.
2. 가장 앞선 미완료 Phase에서 시연으로 이어지는 세로 슬라이스 하나를 고른다.
3. 기존 계약과 코드를 확인하고 필요한 범위만 구현한다.
4. build, test, 실제 호출로 검증한다.
5. 결과·blocker·다음 작업을 `IMPLEMENTATION_STATUS.md`에 남긴다.
6. Phase 완료 시 사람이 확인한 뒤 커밋한다.

## 고정 결정

- 절대 흐름: `제보 → 추출 → 검수 → 발행 → 임베딩 → 검색 → 안내`
- Route는 고정 데이터이며 AI가 생성하거나 재배열하지 않는다.
- Python은 STT, 추출, 임베딩만 담당한다. cosine/상태/조건/랭킹은 Spring이 담당한다.
- Draft와 Published를 분리하고 PUBLISHED만 임베딩·검색한다.
- 별도 Vector DB 없이 MariaDB JSON vector를 사용한다.
- UNKNOWN/UNRESOLVED를 보존하고 원문에 없는 정보를 만들지 않는다.
- 숫자·시간·요일 조건은 코드로 계산한다.
- 시연 장소는 B다. 일반 검색 결과가 관련 없으면 카드 0개를 허용한다.

## 완료와 컷라인

```text
완료 = 빌드 성공 + 최소 테스트 통과 + 실행 환경에서 실제 호출 + 상태 문서 갱신

T+6.0h  Phase 3 미완 → P1·지도·복수 장소 폐기, B만 유지
T+8.0h  Phase 4 미완 → Phase 5~7 수동 시연 대체 검토
T+11.5h Phase 7 미완 → 2막 포기, 동작하는 1막으로 발표
```

큰 리팩터링, 계약의 독단적 변경, 검증되지 않은 완료 표시는 하지 않는다.
