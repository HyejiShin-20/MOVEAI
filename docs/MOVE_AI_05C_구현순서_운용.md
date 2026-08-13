# 05C — 구현 순서와 운용

> **항상 참조한다.** 세션 시작 시 `IMPLEMENTATION_STATUS.md`와 함께 읽는다.
>
> | 문서 | 내용 |
> |---|---|
> | `05A` | DB 스키마(DDL) · 데이터셋 임포트 |
> | `05B` | Spring API 계약 · Python AI 서비스 계약 |
> | **05C** ← 이 문서 | 시스템 구성 · 모듈 구조 · 구현 순서 · 테스트 · 운용 |
> | `04` | 검색·안내 내부 로직 |
>
> 진행 상태 체크리스트는 `IMPLEMENTATION_STATUS.md`에만 있다. 여기에 적지 않는다.

---

# 1. 시스템 구성과 책임 경계

```text
 React (기사 모바일 웹)  React (관리자 검수 웹)
          │                     │
          └────── REST ─────────┘
                    ▼
             Spring Boot  ─── MariaDB
                    │
                    │  REST (내부)
                    ▼
             Python FastAPI
             (STT · 추출 · 임베딩)
```

## 스택 버전 (확정)

```
Java 17          Spring Boot 3.4.x   Gradle
Python 3.12      FastAPI
Node 20+         React 18 + TypeScript + Vite (화면 2개 공통)
MariaDB 11.4+
```

**팀원마다 버전이 다르면 사전에 채워둔 빌드 캐시가 무용지물이 된다.**
본선 전에 전원이 같은 버전을 설치했는지 확인한다. → `PREP_CHECKLIST.md`

## 절대 넘지 않는 경계

| | 담당 | 담당하지 않음 |
|---|---|---|
| **Spring** | 트랜잭션, 상태 전이, 조건 계산, **코사인 유사도 계산**, 응답 조립 | LLM 호출, 음성 처리 |
| **Python** | STT, 지식 추출, 임베딩 벡터 생성 | DB 쓰기, 상태 판단, 순위 결정 |
| **LLM** | 자연어 → 구조화 JSON | 경로 생성, 숫자 계산, 승인, 순위 |

**코사인 유사도를 Python으로 보내지 않는다.** 후보 벡터를 HTTP로 넘기면 단계마다 수백 KB가 오간다. 내적/노름 계산은 Java 20줄이다. Python은 `/embed`로 **질의문 1개**만 벡터화한다.

---

---

# 6. 백엔드 모듈 구조

```text
backend/src/main/java/com/moveai/
├─ common/                         예외, 응답 래퍼, enum
├─ place/                          장소·노드
│  ├─ entity/ ├─ repository/ ├─ service/ ├─ controller/ └─ dto/
├─ route/                          경로·구간·경로 선택
│  ├─ entity/ ├─ repository/ ├─ service/ ├─ controller/ └─ dto/
├─ job/                            배송 건
│  ├─ entity/ ├─ repository/ ├─ service/ ├─ controller/ └─ dto/
├─ report/                         현장 제보·오디오
│  ├─ entity/ ├─ repository/ ├─ service/ ├─ controller/ └─ dto/
├─ knowledge/                      승인 지식
│  ├─ entity/ ├─ repository/ ├─ service/ ├─ controller/ ├─ dto/
│  └─ embedding/                   EmbeddingTextBuilder, KnowledgeEmbedding
├─ moderation/                     Draft·검수·발행
│  ├─ entity/ ├─ repository/ ├─ service/ ├─ controller/ └─ dto/
├─ retrieval/                      후보·조건·코사인·랭킹 (04 §1~6)
├─ guidance/                       단계별 Last 100m 안내
│  ├─ entity/ ├─ repository/ ├─ service/ ├─ controller/ └─ dto/
├─ ai/                             FastAPI HTTP 클라이언트 경계
│  ├─ stt/ ├─ extraction/ └─ embedding/
└─ dataset/                        합성 데이터 검증·임포트
   ├─ controller/ ├─ dto/ ├─ validation/ └─ service/
```

초기 골격은 각 디렉터리의 `package-info.java`만 둔다. 실제 클래스는 해당 Phase에서
`05A`/`05B` 계약을 확인한 뒤 추가하며, 빈 placeholder 엔티티나 DTO는 미리 만들지 않는다.

## AI 클라이언트는 인터페이스 뒤에 둔다

```java
public interface EmbeddingClient { List<float[]> embed(List<String> texts); }

@Profile("!demo") class HttpEmbeddingClient implements EmbeddingClient { … }
@Profile("demo")  class MockEmbeddingClient implements EmbeddingClient { … }
```

외부 호출이 죽어도 화면 흐름은 시연할 수 있어야 한다. 단 **mock 결과를 실제 AI 결과인 것처럼 발표하지 않는다.**

`retrieval` 패키지의 4개 클래스는 전부 **DB 없이 단위 테스트 가능한 순수 로직**으로 만든다. 당일 디버깅 속도가 여기서 갈린다.

---

---

# 7. 구현 순서와 시간 예산

Phase 번호보다 **"세로로 한 줄이 실제로 동작하는가"**가 우선이다. 각 단계는 끝나는 즉시 커밋 가능한 상태여야 한다.

> **완료 조건 체크리스트는 `docs/IMPLEMENTATION_STATUS.md`에만 있다.**
> 이 절은 각 Phase가 **무엇을 왜 하는지**와 **시간 예산**을 정의한다.
> 진행 상태를 여기에 기록하지 않는다.

## 시간표

작업 시작을 `T+0`으로 둔다. **누적 시간**이며 12시간 작업 기준이다.

> ### ⚠ 아래 표는 **백엔드·AI 전용 일정**이다
>
> 12시간이 전부 서버 작업으로 차 있다. **화면 작업은 이 표에 포함되지 않았다.**
> `mobile`(React 4화면)과 `admin-web`(React 2화면)은 **별도 인력이 병렬로** 진행해야 한다.
>
> | 인원 | 성립 여부 |
> |---|---|
> | 4명 | 성립. 백엔드 1 · 모바일 1 · 관리자웹+데이터 1 · 발표 1 |
> | 3명 | 성립. 백엔드 1 · 모바일 1 · (관리자웹 + 데이터 + 발표) 1 |
> | 2명 | **빠듯하다.** 관리자웹을 최소화하고 모바일 `S3`에 집중 |
> | 1명 | **일정이 성립하지 않는다.** 화면을 하나로 줄이거나 시연 범위를 축소해야 한다 |
>
> 화면 트랙의 진행 상태는 `IMPLEMENTATION_STATUS.md`의 별도 절에서 추적한다.

| Phase | 내용 | 소요 | 누적 | 못 지키면 |
|---|---|---:|---:|---|
| 0 | 상황 파악 | 0.5h | 0.5h | — |
| 1 | Server Runtime | 1.0h | 1.5h | 환경 문제. 즉시 서버 담당 투입 |
| **2a** | **DDL + 임포트** | 1.5h | 3.0h | 장소 B만 임포트로 축소 |
| **2b** | **조회 API** | 1.0h | 4.0h | — |
| 3 | 임베딩 + 검색 | 2.0h | 6.0h | **P1·지도 즉시 폐기** |
| 4 | 안내 ★ | 2.0h | 8.0h | **여기서 1막 녹화. 8h 초과 시 5~7 축소 검토** |
| 5 | 음성 제보 | 1.0h | 9.0h | STT를 텍스트 입력으로 대체 |
| 6 | 추출 | 1.0h | 10.0h | — |
| 7 | 검수·발행 ★ | 1.5h | 11.5h | **11.5h에 미완이면 2막 포기, 1막으로 발표** |
| 8 | 다듬기 | 0.5h | 12.0h | — |
| 9 | P1 답장 | — | — | 시간 남을 때만 |

### 컷라인 발동 기준

```text
T+6.0h 에 Phase 3 미완  →  P1·지도·복수 장소 폐기. B 하나로 간다.
T+8.0h 에 Phase 4 미완  →  Phase 5~7을 "수동 시연"으로 대체 검토
                            (제보를 DB에 직접 넣고 승인 화면만 구현)
T+11.5h 에 Phase 7 미완 →  2막 녹화 포기. 1막만으로 발표 구성.
                            없는 기능을 있는 것처럼 말하지 않는다.
```

**Phase 4가 분기점이다.** 여기까지 되면 발표가 성립하고, 안 되면 발표 자체가 흔들린다.

---

## Phase 0 — 상황 파악  `0.5h`

repository tree, git status, 빌드 파일, 기존 코드 확인.
`IMPLEMENTATION_STATUS.md`의 Phase 표를 실제 상태와 대조해 갱신한다.
**바로 대규모 리팩터링 하지 않는다.**

## Phase 1 — Server Runtime  `1.0h`

`backend /health`, `ai-service /health`, MariaDB 연결, backend → ai-service 호출이 되는 상태.
핵심 로직은 없어도 된다. `mobile`과 `admin-web` 실행은 화면 트랙의 M1/W1에서 확인하며
서버 Phase 1의 완료를 막지 않는다.
**끝나면 `CLAUDE.md`의 "명령" 절에 실제 빌드·실행 명령을 추가한다.**

## Phase 2a — DDL + 임포트  `1.5h`

```text
DDL 적용 → 장소 B 임포트 → 검증 → 나머지 3개
```

API 없이 **SQL로 건수를 직접 확인**한다. 여기서 API까지 만들려 하면
실패 지점이 흐려진다. 검증은 `scripts/validate_datasets.py`를 먼저 돌린 뒤 시작한다.

**축소 경로** — 임포트가 막히면 **B 하나(지식 37건)만 넣고 진행한다.**
시연은 B에서만 하므로 A·C·D 없이도 전 과정이 성립한다.
"장소가 4개다"는 발표에서 말할 수 없게 되지만, 멈추는 것보다 낫다.

## Phase 2b — 조회 API  `1.0h`

엔티티·리포지토리·컨트롤러를 얹어 `GET /api/places`, `GET /api/routes/{id}`가
실제로 응답하게 만든다. 프론트가 여기서부터 붙을 수 있다.

## Phase 3 — 임베딩 + 검색  `2.0h`

```text
PUBLISHED 지식 → embedding_text → /embed 일괄 → DB 저장
CosineCalculator · ConditionEvaluator 단위 테스트
정답 질문 20개 평가 스크립트
```

**05A §3-3 톤수 경계 8건 테스트를 반드시 통과시킨다.** 여기서 틀리면 Phase 4의
경로 선택이 같은 이유로 틀린다.

## Phase 4 — 안내  `2.0h`  ★ 첫 발표 가능 지점

`RouteSelector` → `GuidanceSession` → 단계별 카드 조립.

**완료 즉시 시연 영상 1막(컷 1~3)을 녹화한다.** 뒤로 미루면 리허설 없이 마감에 몰린다.
→ `docs/DEMO_SCRIPT.md`

## Phase 5 — 음성 제보  `1.0h`

녹음 → 업로드 → STT → 기사 수정 → 저장. 오디오는 로컬 디렉터리로 충분.

**축소 경로** — STT가 막히면 텍스트 직접 입력으로 대체한다.
"음성으로 남긴다"는 시연에서 약해지지만 이후 파이프라인은 그대로 살아난다.

## Phase 6 — 추출  `1.0h`

`corrected_stt_text` → 검증 → `knowledge_drafts`.
데이터셋 transcript 41건이 그대로 품질 확인용 입력이다.

## Phase 7 — 검수와 발행  `1.5h`  ★ 핵심 완성 지점

Draft → 승인 → PUBLISHED → 임베딩 → 검색 반영.
**여기까지가 서비스 정체성이다.** 완료 후 2막(컷 4~9)을 녹화한다.

## Phase 8 — 다듬기  `0.5h`

로딩·오류·빈 상태, 카드 우선순위, 시연 동선 고정. **기능 추가 아님.**

## Phase 9 — 고객 메시지 답장 (P1)

P0가 완전히 안정된 경우에만.

## 버리는 순서

```text
1. 고객 메시지 답장       2. 지도 시각화
3. 복수 장소 → B 하나만   4. UI 장식
5. 관리자 고급 기능
```

절대 버리지 않는 것:

```text
제보 → 추출 → 검수 → 발행 → 임베딩 → 검색 → 안내
```

---

# 8. 테스트

## 단위 (DB 없이)

```text
ConditionEvaluator
  - 톤수 경계 8건 (05A §3-3 표)
  - 높이 부호: K_B_002는 초과일 때 적용, K_C_006은 이하일 때 적용
  - 시간/요일 경계
CosineCalculator      정규화, 영벡터 방어
EmbeddingTextBuilder  action_text 없으면 줄 생략
RouteSelector         1.0톤 → B_01만, 2.5톤 → B_02만, 후보 0이면 예외
CandidateCollector    첫 구간만 from_node 포함, 인접 중복 없음
```

## 통합

```text
임포트 → 146건 · 검증 통과
승인 → embedding 생성 → 검색 노출
안내 → 단계 이동 → 완료
```

## 평가 스크립트

```text
정답 질문 20개 → Hit@3 / Hit@5 / must_not 위반 수
```

가중치 조정은 **눈으로 보고 고치지 말고 이 숫자로** 한다.

## 시연 스모크

Phase 4·7 완료 조건을 그대로 스크립트로 만들어 둔다. 당일 코드가 바뀔 때마다 돌린다.

---

# 9. 환경변수

```text
DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD
AI_SERVICE_URL          http://localhost:8000
GEMINI_API_KEY
LLM_MODEL              gemini-3.5-flash-lite
LLM_THINKING_LEVEL     minimal
EMBEDDING_MODEL        gemini-embedding-2
EMBEDDING_DIMENSION    1536
EMBEDDING_BATCH_SIZE   50            API 상한 100 / 무료 등급은 분당 100건
STT_MODEL              gemini-3.6-flash
STT_MAX_FILE_BYTES     10485760
AUDIO_STORAGE_PATH      ./data/audio
SPRING_PROFILES_ACTIVE  local | demo
```

`.env.example`만 커밋한다. **실제 키를 커밋하지 않는다.** 시스템에
`GOOGLE_API_KEY`가 함께 있어도 애플리케이션은 `GEMINI_API_KEY`를 우선한다.

---

# 10. 당일 운용 규칙

```text
큰 리팩터링 금지
세로 한 줄 단위로 커밋
DTO 계약을 임의로 바꾸지 않는다 (프론트가 멈춘다)
오류가 나면 범위를 넓히지 말고 좁힌다
"코드 작성함"은 완료가 아니다 — 빌드 + 테스트 + 실제 호출까지가 완료
```

## 완료 정의

```text
코드 + 빌드 성공 + 최소 테스트 통과 + 실행 환경에서 실제 호출 성공 + 문서 갱신
```

## 세션 종료 전 남길 것

`docs/IMPLEMENTATION_STATUS.md`에 기록한다.

```text
1. 무엇을 변경했는지
2. 실제 검증한 명령
3. 통과/실패 결과
4. 남은 blocker
5. 다음에 할 정확한 작업 하나
```

이게 있으면 새 세션이 앞 대화 없이도 이어받는다. **이 파일이 하네스 운용의 핵심이다.**

---

# 11. 시연 대비

**녹화 전 확인 목록은 `docs/DEMO_SCRIPT.md §4`에 있다.** 여기에 옮겨 적지 않는다.

구현 쪽에서만 챙길 것 두 가지:

- **`ROUTE_D_01` 3단계(중앙 엘리베이터 → 15층)에는 붙는 지식이 없다.**
  D를 시연에 쓰면 빈 단계가 보인다. B를 쓰면 문제없다.
- **임포트는 재실행 가능해야 한다.** 시연 중 초기화가 필요할 수 있다.
