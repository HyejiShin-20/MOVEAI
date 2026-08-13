# Implementation Status

> **완료 조건 체크리스트는 이 파일에만 있다.** 다른 문서에 복사하지 않는다.
> Phase의 목적·시간 예산·축소 경로는 `MOVE_AI_05C_구현순서_운용.md §7`.
> 하네스는 **세션 시작 시 이 파일을 먼저 읽고, 세션 종료 전 반드시 갱신**한다.

**최종 갱신** — 2026-08-13 (main 병합 충돌 해결 및 배포 환경 점검)
**현재 Phase** — Phase 7 `DONE`. 다음은 Phase 8 (시연 리허설·화면 다듬기)
**경과 시간** — T+0.0h (본선 전 선행 작업)

> **화면 담당에게 알릴 것** — `05B §4-1`·`§4-2` 조회 API가 실제 데이터로 응답한다.
> Mock에서 실제 API로 넘어올 수 있다. base URL `http://localhost:8080`, CORS는 5173/5174 허용.

---

## 다음에 할 정확한 작업

```
Phase 8 — 시연 리허설·화면 다듬기
  실제 녹음(MediaRecorder) → /api/reports 업로드를 연결할지 텍스트 대체 경로로 확정할지 결정한다.
  내 제보 내역의 정적 샘플을 실제 제보 조회 API로 교체한다.
  DRAFT-4(PENDING)를 이용해 승인 → 신규 카드 노출 2막을 사람이 최종 확인한다.
```

## 현재 blocker

```
없음.

주의 두 가지 (환경):
- DB는 호스트 설치 MariaDB 12.3.2가 3307에서 돈다. docker-compose 컨테이너를 같이 띄우면
  같은 포트를 다퉈 어느 쪽에 붙는지 알 수 없게 된다. 둘 중 하나만 쓴다.
- 포트 8000을 다른 앱이 IPv6로 물고 있다. AI_SERVICE_URL은 127.0.0.1로 고정해 두었다.
```

## 마지막으로 검증한 명령

```
python scripts/validate_datasets.py   → 전체 이슈 0건 (본선 전 확인 완료)
python -m compileall scripts          → 성공
python scripts/build_release_zip.py  → 생성 및 제외 규칙 검증 성공
conda moveai: python -m pytest -q (ai-service) → 48 passed
conda moveai: python scripts/smoke_stt.py → gemini-3.6-flash, M4A 1건 전사 성공
conda moveai: python scripts/embed_dataset.py → gemini-embedding-2, 146건 × 1536차원, 67초
backend: .\gradlew.bat build → BUILD SUCCESSFUL (Gradle 8.11.1 / JDK 21 / 17 타깃)
backend: .\gradlew.bat bootRun → Started MoveAiApplication, Tomcat 8080
curl localhost:8080/health → {"status":"ok",
  "database":{"status":"up","product":"12.3.2-MariaDB"},
  "aiService":{"status":"ok","provider":"gemini","model":"gemini-3.6-flash"}}
backend: .\gradlew.bat bootRun --args="--import-datasets" → 2회 실행 모두
  places=4 place_nodes=57 routes=8 route_segments=30 field_reports=41
  knowledge_items=146 knowledge_conditions=146 knowledge_targets=146
  rag_test_queries=20 delivery_jobs=5 users=2
SQL 검증 → 장소별 지식 A36·B37·C40·D33 / 톤수 파생 8건이 05A §3-3 표와 일치
  / target NODE105·SEGMENT8·UNKNOWN33 / 구간 연속성 위반 0건
  / ROUTE_B_01 7단계 · ROUTE_B_02 maxTon·minTon 1.0 상호배타
GET /api/places            → 4건
GET /api/places/2          → 노드 14 + 경로 2 (ROUTE_B_01 maxTon 1.00/maxH 2.30, B_02 minTon 1.00)
GET /api/routes/3          → 7단계 순서대로, fromNodeName/toNodeName 포함
GET /api/routes/4          → 3단계
GET /api/delivery-jobs     → 5건 (status 없이 호출하면 전체)
GET /api/routes/999        → 404 {"error":{"code":"ROUTE_NOT_FOUND", …}}
OPTIONS /api/places        → 200 (Origin http://localhost:5173)
backend: .\gradlew.bat bootRun --args="--import-embeddings"
  → knowledge_embeddings=146, 모델 gemini-embedding-2, 전건 1536차원
  → Spring/Python embedding_text 전건 일치 검증 후 적재
backend: .\gradlew.bat bootRun --args="--evaluate-rag"
  → 정답 질문 20개 Hit@3 20/20(100%), Hit@5 20/20(100%), Top-5 must_not 위반 11건
backend: .\gradlew.bat test → 30 tests, BUILD SUCCESSFUL (Phase 4 단위 테스트 포함)
Phase 4 실제 연동 smoke (MariaDB + Gemini embed + Spring 8082)
  → 1톤 ROUTE_B_01 7단계 / 2.5톤 ROUTE_B_02 3단계
  → K_B_001 seq1 노출, 전 단계 카드 1개 이상, 인접 단계 knowledgeId 중복 0건
  → K_B_014 12:30 seq6 노출 / 15:00 제외
  → 같은 배송 건 재시작 시 이전 ACTIVE 세션 ABANDONED·next 409, complete COMPLETED
mobile: npm run build → TypeScript + Vite production build 성공
브라우저 실제 연동 (React 5173 + Spring 8082 + Gemini 8002)
  → 배송 5건 조회, 1톤 7단계 안내·완료, 2.4m NO_ROUTE_AVAILABLE 확인
  → 텍스트 제보 REP-44 생성·Gemini 추출·DRAFT-4 PENDING 관리자 상세 표시 확인
```

---

# 시간 기준

작업 시작을 `T+0`으로 둔다. 지연되면 아래 기준으로 **즉시 잘라낸다.**

```
T+6.0h  Phase 3 미완  →  P1·지도·복수 장소 폐기. 장소 B 하나로 간다.
T+8.0h  Phase 4 미완  →  Phase 5~7을 수동 시연으로 대체 검토
T+11.5h Phase 7 미완  →  2막 녹화 포기. 1막만으로 발표 구성.
```

---

# Phase 진행 상황

범례 — `TODO` / `PARTIAL` / `DONE`
각 Phase 완료 시 **커밋하고, 사람이 확인한 뒤** 다음으로 넘어간다.

## Phase 0 — 상황 파악  `TODO`  (0.5h / 누적 0.5h)
- [ ] tree · git status 확인
- [ ] 빌드·환경 파일 확인
- [ ] 이 파일의 Phase 표를 실제 상태로 갱신

## Phase 1 — Server Runtime  `DONE`  (1.0h / 누적 1.5h)
- [x] backend `/health` 200
- [x] ai-service `/health` 200
- [x] MariaDB 연결 성공 (호스트 설치 12.3.2 / 3307)
- [x] backend → ai-service `/health` 호출 성공
- [x] **`CLAUDE.md`의 "명령" 절에 실제 빌드·실행 명령 추가**
- [ ] 커밋

## Phase 2a — DDL + 임포트  `DONE`  (1.5h / 누적 3.0h)
- [x] `python scripts/validate_datasets.py` → 이슈 0건
- [x] DDL 적용 (`05A §2`) — `backend/src/main/resources/db/schema.sql`
- [x] 장소 B 임포트 → SQL로 건수 확인 (지식 37)
- [x] A · C · D 임포트 → 장소 4 · 지식 146 확인
- [x] **톤수 포함/배타 파생 컬럼 생성 확인 (`05A §3-3`)** — 8건 표와 SQL 결과 일치
- [x] **시드 배송 건 5건 생성 (`05A §3-5`)** — 5건 모두 실재하는 Route 도착지를 가리킴
- [x] 재실행해도 같은 결과 (idempotent) — 2회 실행 후 건수 동일
- [ ] 커밋

> 막히면 **B 하나만 넣고 2b로 넘어간다.** 시연은 B에서만 한다.

## Phase 2b — 조회 API  `DONE`  (1.0h / 누적 4.0h)
- [x] `GET /api/places` 실제 응답
- [x] `GET /api/places/{id}` 노드·경로 포함 응답
- [x] `GET /api/routes/{id}` 구간 순서대로 응답
- [x] `GET /api/delivery-jobs` · `/{id}` — 화면 M2가 붙으려면 필요해 함께 구현 (`05B §4-2`)
- [x] CORS 허용 (Vite 5173/5174) — 없으면 화면이 Mock에서 못 넘어온다
- [ ] 커밋

## Phase 3 — 임베딩 + 검색  `DONE`  (2.0h / 누적 6.0h)
- [x] `EmbeddingTextBuilder` (`04 §5-1`) — Python 기준 문자열과 전건 비교
- [x] PUBLISHED 전건 일괄 임베딩 → DB 저장 — 146건 × 1536차원
- [x] **톤수 경계 8건 단위 테스트 통과 (`05A §3-3`)**
- [x] 높이 부호 분기 테스트 (`K_B_002` 초과 적용 / `K_C_006` 이하 적용)
- [x] `CandidateCollector` · `ConditionEvaluator` · `QueryTextBuilder`
- [x] `CosineCalculator` · `RankingService` · 구조 4 + UNRESOLVED 1 하이브리드 슬롯
- [x] 정답 질문 20개 평가 동작 — Hit@3 100%, Hit@5 100%, Top-5 must_not 11건
- [ ] 커밋

> **T+6.0h 체크포인트.** 여기 못 왔으면 P1·지도·복수 장소를 지금 버린다.

## Phase 4 — 안내  `DONE`  (2.0h / 누적 8.0h)  ★ 첫 발표 가능 지점
- [x] 1톤 → `ROUTE_B_01` (7단계) 선택
- [x] 2.5톤 → `ROUTE_B_02` (3단계) 선택
- [x] 경로 후보 0개면 `404 NO_ROUTE_AVAILABLE` (기본 경로로 대체하지 않음)
- [x] **같은 배송 건을 차량만 바꿔 다시 시작할 수 있다 (`05B §4-3`)**
- [x] 같은 지식이 연속 두 단계에 중복 노출되지 않음
- [x] Demo fixture B의 시연 단계마다 예상 Knowledge가 1개 이상 노출
- [x] 일반 Guidance는 relevant Knowledge가 없으면 카드 0개 허용
- [x] `contextTime=12:30` → `K_B_014` 노출 / `15:00` → 사라짐
- [x] `complete` 동작
- [ ] 커밋
- [x] **★ 1막 시연 동작 확인** — 실제 React 화면에서 7단계 진행·완료·경로 없음까지 확인

> **T+8.0h 체크포인트.** 여기 못 왔으면 Phase 5~7 축소를 결정한다.

## Phase 5 — 음성 제보  `DONE`  (1.0h / 누적 9.0h)
- [x] AI 서비스 `POST /stt` 구현 (Gemini, 업로드 검증·오류 매핑 포함)
- [x] `datasets/voice` M4A 샘플 1건 Gemini 실호출 성공
- [x] 녹음 → 업로드 → STT — `POST /api/reports` 실제 m4a로 201, 전사문 반환, 원본 파일 저장
- [x] 기사 텍스트 수정 → 저장 — `PATCH /api/reports/{id}/transcript`
- [x] 텍스트 직접 입력 축소 경로 `POST /api/reports/text`
- [ ] 커밋

> **남은 결함** — 깨진 JSON 본문이 오면 `500 INTERNAL_ERROR`가 나간다.
> `HttpMessageNotReadableException` 핸들러를 `ApiExceptionHandler`에 추가해 400으로 바꿀 것.
> **오디오 저장 위치는 `.env`의 `AUDIO_STORAGE_PATH=./data/audio`가 이긴다** → `backend/data/audio/<날짜>/`.

> STT가 막히면 **텍스트 직접 입력으로 대체**하고 진행한다.

## Phase 6 — 추출  `DONE`  (1.0h / 누적 10.0h)
- [x] `corrected_stt_text` → Draft 생성 — `POST /api/reports/{id}/extract`
- [x] 스키마 검증 + 1회 재시도 + `EXTRACTION_FAILED` 처리 (검증은 ai-service, Spring은 상태 저장)
- [x] `source_excerpt`가 원문 부분 문자열인지 검증 — ai-service `extraction.py`
- [x] 실제 STT 전사문으로 품질 확인 — 3건 추출, 타깃·근거 구절 모두 정확
- [ ] 커밋

> `knownNodes`/`knownSegments`는 그 장소의 노드 전체와 구간 전체를 넘긴다.
> 구간은 이름이 없어 `"출발 → 도착"`으로 만든다. 이게 없으면 LLM이 타깃 코드를 못 맞춘다.
> 재추출하면 그 제보의 이전 초안은 지운다. 겹겹이 쌓이면 검수 화면이 뒤엉킨다.

## Phase 7 — 검수 + 발행  `DONE`  (1.5h / 누적 11.5h)  ★ 핵심 완성 지점
- [x] 검수 API — 목록 / 상세(원문·AI 결과·근거 구절·resolvedTargetName) / 승인 / 반려
- [x] 검수 화면 — `mobile/`의 React 관리자 목록·상세를 실제 API에 연결
- [x] 승인 요청 중 `/embed` 동기 호출은 DB 트랜잭션 밖에서 수행
- [x] 임베딩 성공 후 PUBLISHED·Embedding·Review·Draft 상태를 한 DB 트랜잭션으로 저장
- [x] 실패 시 롤백 + 명시적 오류 (`EMBEDDING_FAILED`, DB 무변경)
- [x] 중복 클릭 방어 — 트랜잭션 안에서 PENDING 재확인, 아니면 `409 DRAFT_NOT_PENDING`
- [x] 새 제보 승인 후 같은 경로 재시작 시 새 카드 노출 — `id=148`이 seq1 최상단
- [x] `isRecentlyAdded = true` 배지
- [ ] 커밋
- [x] **★ 2막 서버 동작 확인** — 승인 → 재조회에서 새 카드가 실제로 뜬다

> 승인한 지식이 조건에 걸리면 안 뜨는 게 **정상**이다. 실제로 draft 1(높이 2.3m 초과 제한)은
> 2.0m 차량 세션에서 제외됐고, 조건 없는 draft 2는 seq1 최상단에 떴다.
> 2막 리허설 때 이 차이를 모르면 "버그"로 오해하기 쉽다.

> **T+11.5h 체크포인트.** 미완이면 2막을 포기하고 1막으로 발표를 구성한다.
> 없는 기능을 있는 것처럼 말하지 않는다.

## Phase 8 — 다듬기  `TODO`  (0.5h / 누적 12.0h)
- [ ] 로딩 / 오류 / 빈 상태
- [ ] 카드 우선순위
- [ ] 시연 동선 고정
- [ ] 커밋

## Phase 9 — Reply Assist (P1)  `TODO`
- [ ] P0 안정 확인 후에만 착수

---

# 화면 트랙  (서버 Phase와 병렬 진행)

> **위 Phase 표는 백엔드·AI 전용이다.** 화면은 여기서 따로 추적한다.
> 담당이 다르므로 서로의 진행을 막지 않는다.
> API 계약(`05B`)이 이미 확정돼 있어 **서버가 안 끝나도 Mock으로 만들 수 있다.**
> 디자인 시안 — [Figma](https://www.figma.com/design/4girj3oH3g2JxyIM5erfUC/Untitled?node-id=0-1)

## M — 기사 모바일 웹 (React)  `PARTIAL`

```
합류 지점   T+1.5h  Phase 1 완료 후 Mock 으로 시작
           T+4.0h  Phase 2b 완료 후 실제 API 연결
```

### M1 — 프로젝트 뼈대  `DONE`
- [x] React 18 + TypeScript + Vite 프로젝트 생성 및 실행 확인
- [x] API 클라이언트 · 모델 클래스 (`05B` 계약 그대로)
- [x] React Router 화면 전환 동작

### M2 — 배송 목록 · 상세 (S1·S2)  `DONE`
- [x] 배송 목록 표시
- [x] 배송 상세 → 차량 입력 (톤수·높이·너비)
- [x] 실제 API 연결

### M3 — Last 100m 안내 (S3)  `DONE`  ★ 시연의 중심
- [x] 단계 표시 (`4 / 7` 형태)
- [x] 다음 이동 문구
- [x] 카드 3종 시각 구분 (WARNING / ACTION / REFERENCE)
- [x] `conditionLabel` 표시
- [x] **`isRecentlyAdded` 배지**
- [x] [다음] / [배송 완료] 버튼
- [x] 실제 API 연결

### M4 — 현장 팁 등록 (S4)  `PARTIAL`
- [x] 실제 API 장소·대표 위치 선택
- [ ] 녹음 + 마이크 권한
- [x] 텍스트 대체 경로 결과 표시 · 수정 · 제출 → 실제 추출·Draft 생성

> 녹음이 막히면 **텍스트 직접 입력**으로 대체한다. 이후 파이프라인은 그대로 살아난다.

## W — 관리자 검수 (React)  `DONE`

```
합류 지점   T+1.5h  Mock 으로 시작
           Phase 7 완료 후 실제 승인 연결
```

### W1 — Admin Runtime · 검수 대기 목록  `DONE`
- [x] React 18 + TypeScript + Vite 프로젝트 실행 확인 (`mobile/`에 통합)
- [x] 실제 API 초안 목록·필터·빈 상태·오류 표시

### W2 — 검수 상세  `DONE`  ★ 신뢰성의 근거
- [x] **좌우 분할** — 왼쪽 원문 / 오른쪽 AI 결과
- [x] AI 결과를 카드로 표시 (JSON 노출 없음)
- [x] `source_excerpt` 표시
- [x] UNKNOWN 타깃은 "확인 필요"로 표시
- [x] 승인 / 수정 승인 / 반려
- [x] **승인 중 처리 상태** — 중복 클릭 방지
- [x] 실패 시 명시적 오류 + 재시도

## 화면 트랙 컷라인

```
T+8.0h   M3 미완  →  M4·W1 을 버리고 M3 에 전원 투입
                    S3 없이는 시연 자체가 불가능하다
T+10.0h  W2 미완  →  검수를 API 직접 호출로 대체하고 화면은 포기
                    (2막에서 검수 장면이 빠지지만 흐름은 성립)
```

**버리는 순서** — `S4` → `W1` → `S1` → `S2`. **`S3`와 `W2`는 마지막까지 지킨다.**

---

## 개발 종료 후 — 시연 녹화  (사람이 진행)

하네스 작업이 아니다. **개발을 멈추고 사람이 직접 진행한다.**
점검 항목과 촬영 순서는 `docs/DEMO_SCRIPT.md §7`.

- [ ] 사전 점검 12항목 통과
- [ ] 1막 촬영 (약 40초)
- [ ] 2막 촬영 (약 90초)
- [ ] 영상 확인 — 화면 글자가 읽히는지, 핵심 장면이 잡혔는지

---

# 변경 이력

세션마다 아래 형식으로 **위에** 추가한다.

```
## T+N.Nh  Phase N
변경   …
검증   실행한 명령 / 결과
남은것 …
다음   …
```

<!-- 여기부터 기록 -->

## 본선 전 선행 — 2026-08-13  main 병합·배포 점검
변경   main의 로그인·회원가입·관리자 셸·관리 화면·라우팅을 기준으로 충돌을 해결하고,
       실제 배송 목록·안내 세션·장소 선택·제보 추출·관리자 검수 API 연결을 다시 결합했다.
검증   충돌 표식 0건, `npm run build` 성공(66 modules), `gradlew test` BUILD SUCCESSFUL.
       브라우저에서 루트 URL 로그인 화면, `/home` 실제 배송 5건, `/admin/reviews` DRAFT-4를 확인했다.
배포   프론트 정적 산출물은 생성 가능. 전체 서비스 배포 전 공개 API URL, CORS 허용 도메인,
       SPA rewrite, Spring·AI·MariaDB 런타임 구성이 배포 플랫폼에 필요하다.
다음   배포 플랫폼을 정한 뒤 플랫폼 설정 파일과 운영 환경변수를 추가한다.

## 본선 전 선행 — 2026-08-13  React 실제 API 통합
변경   배송 목록·차량 조건·안내 세션·단계 이동·완료·경로 없음 화면을 Phase 4 API에 연결하고,
       장소·내부 노드 선택·텍스트 제보·Gemini 추출을 Phase 6 API에 연결했다.
       관리자 대기 목록·상세·승인·수정 승인·반려를 Phase 7 API에 연결했다.
검증   TypeScript/Vite production build 성공. 실제 브라우저에서 1톤 B01 7단계 완주,
       2.4m 차량의 NO_ROUTE_AVAILABLE, 반응형 모바일 레이아웃을 확인했다.
       REP-44를 실제 생성해 Gemini가 DRAFT-4 한 건으로 구조화했고 관리자 상세에서 원문·조건·
       타깃·근거 구절을 확인했다. DRAFT-4는 사용자 확인용으로 PENDING 상태를 유지한다.
결정   실제 음성 녹음이 아직 없으므로 M4는 텍스트 직접 입력 축소 경로까지만 완료로 본다.
남은것 MediaRecorder/STT 업로드 연결, 내 제보 내역의 정적 샘플을 실제 조회로 교체.
다음   Phase 8 — DRAFT-4 승인 시연과 신규 카드 노출 재확인, 시연 동선 고정

## 본선 전 선행 — 2026-08-13  Phase 4 안내
변경   RouteSelector·GuidanceSession과 `POST /api/guidance`, `GET /api/guidance/{id}`,
       `POST /next`, `POST /complete`를 구현하고 Phase 3 검색 결과를 단계별 카드로 조립
       카드에 usageScope·statement·actionText·targetName을 연결하고 ACTION fallback,
       조건 라벨·24시간 신규 배지·미해결 타깃 표시를 적용
검증   `./gradlew test` → 30 tests, BUILD SUCCESSFUL
       실제 MariaDB·Gemini 임베딩 연동 smoke → B01 7단계·B02 3단계, 인접 중복 0건,
       K_B_001 출발점 예외와 K_B_014 시간 조건, ACTIVE 재시작·complete 상태 전환 통과
결정   Route는 배송 목적지를 먼저 고정한 뒤 차량 제약으로만 선택한다. 후보가 없으면 기본
       Route로 우회하지 않고 404를 반환한다. 같은 배송 건 재시작 시 이전 ACTIVE는 ABANDONED다.
남은것 React 화면에서 실제 버튼으로 이어지는 1막 리허설은 프론트 합류 뒤 사람이 확인한다.
다음   Phase 5·6 — 기존 Python STT·구조화 API를 Spring 제보 저장·Draft 생성 흐름에 연결

## 본선 전 선행 — 2026-08-13  Phase 3 임베딩 + 검색
변경   Python 산출물 검증·적재 서비스와 `--import-embeddings`, `/embed` Spring 클라이언트,
       CandidateCollector·ConditionEvaluator·QueryTextBuilder·CosineCalculator·RankingService,
       HybridSearchService(구조 4 + UNRESOLVED 1), `--evaluate-rag` 구현
검증   `knowledge_embeddings` 146건 × 1536차원 실제 MariaDB 적재
       Spring/Python embedding_text 146건 전부 동일
       `--evaluate-rag` → Hit@3 20/20, Hit@5 20/20, Top-5 must_not 11건
       `./gradlew test` → 25 tests, BUILD SUCCESSFUL
결정   톤수·높이·시간·요일·movement는 임베딩 전에 hard filter하고,
       코사인과 구조 가산점 계산은 Spring에서 수행한다. 별도 Vector DB와 Python 검색 API는 없다.
남은것 Gold 질문에는 현재 세그먼트가 없어 Top-5 must_not 11건은 위치 구조로 제거할 수 없다.
       실제 Guidance에서는 세그먼트·to_node 후보 제한이 먼저 적용된다.
다음   Phase 4 — RouteSelector · GuidanceSession · 단계별 카드 조립

## 본선 전 선행 — 2026-08-13  Phase 2b 조회 API
변경   JPA 엔티티 5개(Place · PlaceNode · Route · RouteSegment · DeliveryJob)와 리포지토리,
       PlaceService/RouteService/DeliveryJobService, 컨트롤러 3개,
       `05B` 오류 형식용 `ApiException`·`ApiExceptionHandler`, CORS 설정
검증   `.\gradlew.bat build` → BUILD SUCCESSFUL
       places 4 / places/2 노드 14·경로 2 / routes/3 7단계 / routes/4 3단계 /
       delivery-jobs 5 / 404 오류 형식 / CORS preflight 200
결정   `GET /api/delivery-jobs` 를 여기서 함께 만들었다. 화면 M2(배송 목록)가 T+4.0h에
       실제 API로 넘어오려면 필요하고, 이미 임포트된 데이터를 읽기만 하므로 범위가 커지지 않는다.
       필터 없이 호출하면 전체를 반환한다 — 시연 중 목록에서 건이 사라지면 안 된다 (05B §4-3).
남은것 임베딩 벡터가 아직 DB에 없다. retrieval 로직 전무.
다음   Phase 3 — 벡터 적재 + CandidateCollector·ConditionEvaluator·CosineCalculator·RankingService

## 본선 전 선행 — 2026-08-13  Phase 2a DDL + 임포트
변경   `db/schema.sql`(05A §2 전량 16테이블), `SchemaInitializer`, `DatasetImportService`(JdbcTemplate),
       `DatasetImportRunner`(`--import-datasets`), `DatasetPayload` DTO,
       `EnumAllowList`(05A §2-6), `TonnageBoundaryRule`(§3-3) + 단위 테스트 8건
검증   `.\gradlew.bat build` → BUILD SUCCESSFUL (톤수 8건 테스트 포함)
       `--import-datasets` 2회 실행 → 건수 동일 (idempotent)
       SQL 직접 확인 → 지식 146(A36·B37·C40·D33), 톤수 파생이 §3-3 표와 일치,
       구간 연속성 위반 0건, 시드 배송 5건 전부 실재 Route 도착지, 시간·요일 17건 파싱 정상
결정   Phase 2a는 JdbcTemplate으로 넣는다. JPA 엔티티는 조회가 필요한 Phase 2b에서 만든다.
       FK 순서와 2-pass parent 갱신을 직접 통제하는 편이 임포트에서 더 안전하다.
       임포트는 전체 TRUNCATE 후 재삽입이다. 런타임 데이터(초안·세션)도 함께 지워진다.
남은것 조회 API 없음. 임베딩 벡터도 아직 DB에 없다(파일 산출물만).
다음   Phase 2b — GET /api/places · /api/places/{id} · /api/routes/{id}

## 본선 전 선행 — 2026-08-13  Phase 1 Server Runtime
변경   Gradle wrapper(8.11.1) 부트스트랩, `build.gradle`·`settings.gradle`,
       `MoveAiApplication`, `HealthController`(서버·DB·ai-service 3경계), `AiServiceClient`,
       `application.yml`(ddl-auto=none), `bootRun`이 루트 `.env`를 읽도록 구성,
       `.gitignore`의 wrapper jar 예외 경로 수정, `CLAUDE.md` 명령 절 작성
검증   `.\gradlew.bat build` → BUILD SUCCESSFUL
       `curl localhost:8080/health` → database up(12.3.2-MariaDB) · aiService ok(gemini)
환경   DB는 **호스트 설치 MariaDB 12.3.2 / 3307**을 쓴다. compose 컨테이너는 같은 포트를
       다투므로 띄우지 않는다. 3306은 다른 프로젝트 mysqld가 점유 중이라 건드리지 않는다.
       포트 8000은 다른 앱이 IPv6로 물고 있어 `AI_SERVICE_URL`을 127.0.0.1로 고정했다.
       JDK 17이 없어 JDK 21로 빌드하되 산출물은 17 타깃으로 맞췄다.
남은것 DB 테이블 0개. Phase 2a DDL·임포트가 다음이다.
다음   05A §2 DDL 적용 → datasets 4개 임포트 → 톤수 파생 컬럼 8건 테스트 → 시드 배송 5건

## 본선 전 선행 — 2026-08-13  ai-service 임베딩
변경   `POST /embed` 구현(05B §5-1 계약 그대로), `embedding_text` 조립 규칙 확정(04 §5-1),
       datasets 146건 → 벡터 산출물 생성 스크립트, `EMBEDDING_BATCH_SIZE` 환경변수 추가
검증   `pytest -q` → 48 passed (임베딩 관련 22건 신규)
       `python scripts/embed_dataset.py` → gemini-embedding-2 / 1536차원 / 146건 / 67초
       질의 스모크: ROUTE_B_01 seq6 질의문 → Top1 `K_B_027`(그 구간의 배수구 턱 경고) 0.862,
       Top3에 UNRESOLVED 지식 `K_B_015` 등장 (04 §4-2가 임베딩에 기대한 동작)
남은것 backend Spring 부재로 DB 적재 미완. 산출물은 `data/embeddings/`(gitignore)에만 있다.
       Spring `EmbeddingTextBuilder`가 04 §5-1 표와 같은 문자열을 만들어야 한다.
다음   Phase 1 backend 런타임 → Phase 2a DDL·임포트 → Phase 3 벡터 적재

## 본선 전 정비 — 2026-08-13
변경   폐기 문서 격리, React 프론트 통일, Phase 책임 경계·승인 트랜잭션·카드 규칙 정정,
       AI 모델 확정, validator 경로/Route 연속성 보강, 안전한 전달 ZIP 스크립트 추가
검증   `python scripts/validate_datasets.py` → 4개 데이터셋 전체 이슈 0건
       `python -m compileall scripts` → 성공
       `python scripts/build_release_zip.py` → 금지 파일 제외 검증 성공
남은것 LLM API 키 발급 및 세 모델 실제 호출 확인
다음   본선 당일 Phase 0 repository audit부터 시작
