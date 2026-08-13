# Implementation Status

> **완료 조건 체크리스트는 이 파일에만 있다.** 다른 문서에 복사하지 않는다.
> Phase의 목적·시간 예산·축소 경로는 `MOVE_AI_05C_구현순서_운용.md §7`.
> 하네스는 **세션 시작 시 이 파일을 먼저 읽고, 세션 종료 전 반드시 갱신**한다.

**최종 갱신** — 2026-08-13 (Phase 3 완료 — 벡터 적재 + 하이브리드 검색)
**현재 Phase** — Phase 3 `DONE`. 다음은 Phase 4 (안내)
**경과 시간** — T+0.0h (본선 전 선행 작업)

> **화면 담당에게 알릴 것** — `05B §4-1`·`§4-2` 조회 API가 실제 데이터로 응답한다.
> Mock에서 실제 API로 넘어올 수 있다. base URL `http://localhost:8080`, CORS는 5173/5174 허용.

---

## 다음에 할 정확한 작업

```
Phase 4 — 안내
  RouteSelector로 배송 목적지와 차량 조건에 맞는 고정 Route를 선택한다.
  GuidanceSession을 생성하고 Phase 3 HybridSearchService를 단계별 카드 조립에 연결한다.
  1톤 ROUTE_B_01(7단계) / 2.5톤 ROUTE_B_02(3단계), 시간 필터와 중복 제거를 검증한다.
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
backend: .\gradlew.bat test → 25 tests, BUILD SUCCESSFUL
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

## Phase 4 — 안내  `TODO`  (2.0h / 누적 8.0h)  ★ 첫 발표 가능 지점
- [ ] 1톤 → `ROUTE_B_01` (7단계) 선택
- [ ] 2.5톤 → `ROUTE_B_02` (3단계) 선택
- [ ] 경로 후보 0개면 `404 NO_ROUTE_AVAILABLE` (기본 경로로 대체하지 않음)
- [ ] **같은 배송 건을 차량만 바꿔 다시 시작할 수 있다 (`05B §4-3`)**
- [ ] 같은 지식이 연속 두 단계에 중복 노출되지 않음
- [ ] Demo fixture B의 시연 단계마다 예상 Knowledge가 1개 이상 노출
- [ ] 일반 Guidance는 relevant Knowledge가 없으면 카드 0개 허용
- [ ] `contextTime=12:30` → `K_B_014` 노출 / `15:00` → 사라짐
- [ ] `complete` 동작
- [ ] 커밋
- [ ] **★ 1막 시연 동작 확인** — 실제로 눌러보며 화면이 나오는지 (녹화는 개발 종료 후)

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

## Phase 6 — 추출  `TODO`  (1.0h / 누적 10.0h)
- [ ] `corrected_stt_text` → Draft 생성
- [ ] 스키마 검증 + 1회 재시도 + `EXTRACTION_FAILED` 처리
- [ ] `source_excerpt`가 원문 부분 문자열인지 검증
- [ ] 데이터셋 transcript로 품질 확인
- [ ] 커밋

## Phase 7 — 검수 + 발행  `TODO`  (1.5h / 누적 11.5h)  ★ 핵심 완성 지점
- [ ] 검수 화면 (원문 / AI 결과 / 근거 구절 나란히)
- [ ] 승인 요청 중 `/embed` 동기 호출은 DB 트랜잭션 밖에서 수행
- [ ] 임베딩 성공 후 PUBLISHED·Embedding·Review·Draft 상태를 한 DB 트랜잭션으로 저장
- [ ] 실패 시 롤백 + 명시적 오류
- [ ] 새 제보 승인 후 같은 경로 재시작 시 새 카드 노출
- [ ] `isRecentlyAdded = true` 배지
- [ ] 커밋
- [ ] **★ 2막 시연 동작 확인** — 승인 → 재조회에서 새 카드가 실제로 뜨는지

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

## M — 기사 모바일 웹 (React)  `TODO`

```
합류 지점   T+1.5h  Phase 1 완료 후 Mock 으로 시작
           T+4.0h  Phase 2b 완료 후 실제 API 연결
```

### M1 — 프로젝트 뼈대  `TODO`
- [ ] React 18 + TypeScript + Vite 프로젝트 생성 및 실행 확인
- [ ] API 클라이언트 · 모델 클래스 (`05B` 계약 그대로)
- [ ] Mock 응답으로 화면 전환 동작

### M2 — 배송 목록 · 상세 (S1·S2)  `TODO`
- [ ] 배송 목록 표시
- [ ] 배송 상세 → [현장 도착] → 차량 입력 (톤수·높이)
- [ ] 실제 API 연결

### M3 — Last 100m 안내 (S3)  `TODO`  ★ 시연의 중심
- [ ] 단계 표시 (`4 / 7` 형태)
- [ ] 다음 이동 문구
- [ ] 카드 3종 시각 구분 (WARNING / ACTION / REFERENCE)
- [ ] `conditionLabel` 표시
- [ ] **`isRecentlyAdded` 배지** — 없으면 2막이 약해진다
- [ ] [다음] / [배송 완료] 버튼
- [ ] 실제 API 연결

### M4 — 현장 팁 등록 (S4)  `TODO`
- [ ] 장소·대표 위치 선택
- [ ] 녹음 + 마이크 권한
- [ ] STT 결과 표시 · 수정 · 제출

> 녹음이 막히면 **텍스트 직접 입력**으로 대체한다. 이후 파이프라인은 그대로 살아난다.

## W — 관리자 검수 (React)  `TODO`

```
합류 지점   T+1.5h  Mock 으로 시작
           Phase 7 완료 후 실제 승인 연결
```

### W1 — Admin Runtime · 검수 대기 목록  `TODO`
- [ ] React 18 + TypeScript + Vite 프로젝트 생성 및 실행 확인
- [ ] 초안 목록 표시

### W2 — 검수 상세  `TODO`  ★ 신뢰성의 근거
- [ ] **좌우 분할** — 왼쪽 원문 / 오른쪽 AI 결과
- [ ] AI 결과를 카드로 표시 (JSON 노출 금지)
- [ ] `source_excerpt` 표시 (원문 하이라이트면 더 좋음)
- [ ] UNKNOWN 타깃은 "확인 필요"로 표시
- [ ] 승인 / 수정 승인 / 반려
- [ ] **승인 중 스피너** — 임베딩까지 동기 처리라 수 초 걸린다
- [ ] 실패 시 명시적 오류 + 재시도

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
