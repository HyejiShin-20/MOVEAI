# MOVE-AI Harness Engineering Plan

> 목적: 본선 당일 새 Codex/ChatGPT/AI coding harness 세션에 repository와 이 문서를 제공하면, 기획을 다시 설명하지 않고 즉시 구현을 시작할 수 있도록 한다.
>
> 원칙: 사전에는 문서/데이터/환경/작업계획을 준비하고, 대회 규정을 고려하여 핵심 서비스 구현은 본선 당일 진행한다.

---

# 1. Harness Engineering 목표

하네스가 해야 할 일은 "새 설계를 제안하는 것"이 아니라,
이미 결정된 MVP를 repository에 안정적으로 구현하고 검증하는 것이다.

하네스의 기본 루프:

```text
READ
현재 repository / docs / dataset 확인

PLAN
현재 구현 상태를 Phase와 비교

IMPLEMENT
가장 앞의 미완료 Slice 구현

VERIFY
build / test / run / API 실제 확인

RECORD
완료 상태와 다음 작업을 문서에 기록

REPEAT
```

---

# 2. 당일 Source of Truth 우선순위

충돌 시 다음 순서를 따른다.

1. 사용자의 본선 당일 최신 명시 지시
2. `MOVE_AI_01_MVP_PRD.md`
3. `MOVE_AI_02_HARNESS_ENGINEERING_PLAN.md`
4. 팀 최종 데이터셋
5. repository 기존 코드
6. 과거 초안/아이디어 문서

하네스가 임의로 이전 초안의 범위를 복구하면 안 된다.

---

# 3. 권장 Repository 구조

```text
move-ai/
│
├─ README.md
├─ docker-compose.yml
├─ .env.example
│
├─ docs/
│  ├─ MOVE_AI_01_MVP_PRD.md
│  ├─ MOVE_AI_02_HARNESS_ENGINEERING_PLAN.md
│  ├─ IMPLEMENTATION_STATUS.md
│  ├─ API_CONTRACT.md
│  ├─ DB_SCHEMA.md
│  └─ DEMO_SCRIPT.md
│
├─ datasets/
│  ├─ synthetic_dataset_A.json
│  ├─ synthetic_dataset_B.json
│  ├─ synthetic_dataset_C_final_v3.json
│  └─ synthetic_dataset_D.json
│
├─ backend/
│  ├─ build.gradle
│  └─ src/
│
├─ ai-service/
│  ├─ pyproject.toml 또는 requirements.txt
│  ├─ app/
│  └─ tests/
│
├─ frontend/
│  ├─ package.json
│  └─ src/
│
└─ scripts/
   ├─ dev-up.*
   ├─ dev-down.*
   ├─ import-datasets.*
   ├─ embed-seed.*
   └─ smoke-test.*
```

실제 repo가 이미 다른 구조라면 갈아엎지 않는다.
현재 구조에 맞춰 역할만 대응한다.

---

# 4. Harness용 상태 파일

## docs/IMPLEMENTATION_STATUS.md

항상 최신 상태로 유지.

예:

```text
# Implementation Status

## Phase 1 DB
DONE
- MariaDB connection
- base entities

## Phase 2 Dataset Import
PARTIAL
- Place/Node import done
- Route import TODO

## Current blocker
없음

## Next exact task
RouteSegment import + continuity test

## Last verified command
./gradlew test
```

하네스는 세션 종료 전에 반드시 이 파일을 갱신한다.

---

# 5. Contract-First 원칙

프론트/백엔드/AI 서비스가 동시에 작업 가능하도록 계약을 먼저 고정한다.

먼저 확정할 것:
- REST endpoint
- request/response DTO
- Knowledge Extraction JSON schema
- Embedding API
- DB logical fields
- enum

가능하면 첫 구현 Slice에서 `API_CONTRACT.md`를 만든다.

---

# 6. Backend 권장 package

```text
common/
security/

user/

place/
  controller/
  service/
  repository/
  entity/
  dto/

route/

report/

knowledge/
  condition/
  target/
  embedding/

moderation/

retrieval/
  KnowledgeRetrievalService
  ConditionEvaluator
  RankingService

guidance/

ai/
  SttClient
  KnowledgeExtractionClient
  EmbeddingClient

dataset/
  DatasetValidator
  DatasetImportService
```

기존 package가 있으면 이름을 억지로 변경하지 않는다.

---

# 7. AI Service 구조

```text
ai-service/app/
├─ main.py
├─ api/
│  ├─ stt.py
│  ├─ extraction.py
│  └─ embedding.py
├─ schemas/
│  └─ knowledge.py
├─ services/
│  ├─ stt_service.py
│  ├─ extraction_service.py
│  ├─ embedding_service.py
│  └─ similarity_service.py
├─ prompts/
│  └─ knowledge_extraction.txt
└─ tests/
```

필수 endpoint:

```text
GET  /health
POST /stt
POST /extract-knowledge
POST /embed
POST /similarity-search   # 선택. vector 계산을 Python에 모을 경우
```

---

# 8. Knowledge Extraction Contract

LLM 자유 출력 금지.
Pydantic 또는 JSON Schema로 강제한다.

필수 의미 필드:
```text
target
category
custom_category_label
fact_type
custom_fact_type_label
movement_mode
traversal_method
custom_traversal_method
access_state
statement
action_text
source_excerpt
conditions
usage_scope
```

Validation 후 실패 처리:
```text
1차 출력
→ schema fail
→ repair/retry 1회
→ 여전히 fail
→ extraction failure 상태 반환
```

잘못된 응답을 조용히 필드 삭제해서 저장하지 않는다.

---

# 9. MariaDB Vector Store 구조

MVP에서 별도 Vector DB 없음.

```text
knowledge_embeddings
- id
- knowledge_id UNIQUE
- embedding_model
- embedding_dimension
- embedding_text
- embedding_json
- created_at
- updated_at
```

생성:
```text
PUBLISHED Knowledge
→ embedding_text builder
→ Python /embed
→ vector
→ DB 저장
```

업데이트:
Knowledge 내용/Target/조건의 검색 의미가 바뀌면 embedding 재생성.

---

# 10. Retrieval 상세 Pipeline

```text
Guidance Context
      |
      v
[1] SQL Candidate Filter
      |
      v
[2] ConditionEvaluator
      |
      v
[3] Query Text Builder
      |
      v
[4] Query Embedding
      |
      v
[5] Cosine Similarity
      |
      v
[6] Metadata Bonus / Ranking
      |
      v
Top-K Knowledge
```

Hard Filter 최소:
- same place
- PUBLISHED

ConditionEvaluator:
- vehicle_class
- tonnage
- height
- width
- time
- day

Soft/Ranking:
- current target
- current segment
- movement
- traversal
- semantic similarity

Category는 기본적으로 hard filter로 사용하지 않는다.

---

# 11. 본선 구현 Phase

중요:
Phase 숫자보다 "vertical slice가 실제로 동작하는가"를 우선한다.

## Phase 0 — 20~30분 Repository Audit

하네스 첫 작업:
```text
pwd / tree
git status
README
build files
env files
existing endpoints
existing DB
existing frontend routes
existing AI code
```

출력:
`IMPLEMENTATION_STATUS.md`

절대 바로 대규모 refactor부터 하지 않는다.

---

## Phase 1 — Runtime Skeleton

목표:
Frontend / Backend / AI / DB가 모두 뜬다.

검증:
```text
frontend page open
backend health 200
ai /health 200
MariaDB connection success
```

이 단계에서는 핵심 로직이 없어도 된다.

---

## Phase 2 — Dataset Import Vertical Slice

목표:
A/B/C/D 중 최소 1개 완전한 Place dataset이 DB에 들어간다.

우선 C 최종본처럼 검수 완료된 데이터를 먼저 사용.

DatasetValidator:
- code uniqueness
- references
- route continuity
- resolved target validity
- unresolved free text
- source_excerpt substring
- RAG code refs

Import:
```text
Place
→ Node
→ Route
→ Segment
→ Report
→ Knowledge
→ Condition
→ Target
```

완료 후 API:
```text
GET /api/places
GET /api/routes/{id}
```

---

## Phase 3 — Seed Embedding + RAG Slice

목표:
DB Knowledge 검색이 실제로 된다.

```text
PUBLISHED Knowledge
→ embedding
→ MariaDB
→ query embedding
→ cosine similarity
→ Top-K
```

처음에는 단일 Place 대상으로 구현해도 된다.

Gold `rag_test_queries`를 즉시 smoke test에 사용.

---

## Phase 4 — Guidance Slice

목표:
기사 화면에서 고정 Route + Knowledge가 보인다.

최소 API:
```text
POST /api/guidance
GET  /api/guidance/{id}
POST /api/guidance/{id}/next
POST /api/guidance/{id}/complete
```

완료 조건:
- Segment sequence 정상
- current step 변경
- step에 Top-K Knowledge 표시
- complete 버튼 동작

이 시점에 첫 번째 "발표 가능한 기능"이 완성된다.

---

## Phase 5 — Field Report + STT Slice

목표:
실제 음성으로 제보 생성.

```text
record
→ upload
→ STT
→ raw text
→ user correction
→ corrected text save
```

오디오 파일은 로컬/dev storage여도 무방.

---

## Phase 6 — Extraction + Draft Slice

목표:
corrected transcript가 Knowledge Draft가 된다.

```text
Report
→ ExtractionClient
→ Pydantic validated JSON
→ Draft rows
```

Dataset transcript를 사용해 추출 품질 smoke test.

---

## Phase 7 — Moderation + Publish Slice

목표:
새 지식이 실제 RAG로 들어간다.

```text
Draft
→ 관리자 승인
→ PUBLISHED Knowledge
→ Embedding
→ RAG 검색 가능
```

이 Phase가 MOVE-AI의 핵심 end-to-end 완성 지점이다.

---

## Phase 8 — UI Polish / Demo

기능 추가보다:
- Loading
- Error
- Empty state
- 카드 우선순위
- 데모 데이터 선택
- 시연 동선 고정

에 집중.

---

## Phase 9 — P1 Reply Assist

P0가 안정된 경우만 구현.

---

# 12. 병렬 작업 가능한 경계

Contract가 잡힌 후:

### Track A — Frontend
Mock DTO로 화면 개발.

### Track B — Backend
DB / import / guidance / moderation.

### Track C — AI
STT / extraction / embedding / eval.

병합 포인트:
- API DTO
- enum
- dataset mapping
- embedding endpoint

하네스는 자신이 수정하지 않는 영역의 계약을 임의 변경하지 않는다.

---

# 13. Test Pyramid

## Unit
- ConditionEvaluator
- embedding text builder
- ranking calculation
- dataset reference validator

## Contract
- Extraction Pydantic
- API request/response JSON

## Integration
- Dataset import
- approve → embedding
- retrieval with DB

## Smoke
본선 데모 시나리오 1개를 스크립트로 고정.

---

# 14. Gold 평가

## Extraction
Dataset transcript → AI output.

자동 확인 가능한 것:
- item count
- enum validity
- target code
- resolution
- conditions
- source excerpt containment

의미 평가:
- statement가 원문을 확대하지 않았는가

## RAG
추천:
```text
Hit@3
Hit@5
Must-not violation
```

본선 당일 모델/가중치 튜닝은 이 Gold로 한다.

---

# 15. Demo-Critical Error Handling

데모에서 AI 외부 호출이 실패할 수 있다.

반드시:
- timeout
- clear error
- retry
- mock/fallback interface

를 분리한다.

단, fallback을 실제 AI 결과처럼 속이지 않는다.

예:
```text
AiClient interface
├─ RealKnowledgeExtractionClient
└─ DemoMockKnowledgeExtractionClient
```

환경 변수로 전환.

---

# 16. Environment Variables

예시:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD

AI_SERVICE_URL

LLM_API_KEY
LLM_MODEL

EMBEDDING_MODEL

AUDIO_STORAGE_PATH

SPRING_PROFILES_ACTIVE
```

`.env.example`만 저장하고 실제 secret commit 금지.

---

# 17. Git / 작업 규칙

본선 당일:
- 큰 refactor 금지
- 작은 vertical slice commit
- 동작 단위 commit
- merge 전에 smoke test

추천 commit 단위:
```text
feat(dataset): import place and nodes
feat(rag): store knowledge embeddings
feat(guidance): return current segment tips
feat(report): add stt upload flow
feat(moderation): publish approved knowledge
```

---

# 18. Definition of Done

기능은 다음을 만족해야 DONE이다.

```text
코드 작성
+
build 성공
+
최소 test 성공
+
실행 환경에서 실제 호출 성공
+
문서 상태 업데이트
```

"코드를 작성했다"만으로 완료 처리하지 않는다.

---

# 19. 하네스 세션 종료 규칙

세션을 끝내기 전에 반드시 남긴다.

```text
1. 무엇을 변경했는지
2. 실제 검증한 명령
3. 통과/실패 결과
4. 남은 blocker
5. 다음 exact task
```

`IMPLEMENTATION_STATUS.md`에 기록.

이 방식이면 새 세션이 chain-of-thought 없이도 이어서 작업 가능하다.

---

# 20. 본선 직전 준비 가능 항목

대회 규정을 고려해 핵심 기능을 미리 완성하지 않고 다음 준비만 한다.

- 기획 문서
- API 계약 초안
- DB 스키마 초안
- dataset 정리/검수
- 필요한 소프트웨어 설치
- Java / Python / Node 버전 확인
- Docker / MariaDB 실행 확인
- API key 발급 및 secret 보관
- 모델 다운로드 가능 여부 확인
- 하네스 프롬프트 준비
- 데모 시나리오 정의

핵심 기능 구현 시작 시점은 본선 운영 규정을 따른다.

---

# 21. P0 Cut Line

시간이 부족할 때 버리는 순서:

```text
1. Reply Assist 제거
2. 지도 시각화 최소화
3. 복수 Place 데모 제거 → 대표 Place 1개
4. UI 장식 제거
5. 관리자 고급 기능 제거
```

절대 버리지 않을 것:

```text
Report
→ Extraction
→ Moderation
→ Publish
→ Embedding
→ Retrieval
→ Guidance
```

이 연결이 서비스 정체성이다.

---

# 22. 최종 당일 목표

다음 화면 흐름 하나가 매끄럽게 되면 성공이다.

```text
기사: 새 음성 팁 등록
↓
STT 확인
↓
AI가 팁 2~4개로 분해
↓
관리자: 원문과 비교하여 승인
↓
시스템: embedding 저장
↓
다른 기사: 같은 장소 안내 시작
↓
Route의 해당 단계에서 새 팁 등장
↓
배송 완료
```
