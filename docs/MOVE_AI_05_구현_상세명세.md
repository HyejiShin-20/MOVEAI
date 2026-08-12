# MOVE-AI 구현 상세 명세

> 대상: 본선 당일 구현을 담당하는 개발자 및 AI 코딩 하네스
> 목적: DB 스키마 · API 계약 · 임포트 · 구현 순서를 코드 작성 직전 수준까지 확정한다.
> 전제: 데이터셋 4종은 검증 완료 상태다(정합성 이슈 0건). 아래 스키마는 그 데이터를 그대로 받도록 설계되었다.

---

# 0. 문서 관계와 우선순위

| 문서 | 다루는 것 |
|---|---|
| `MOVE_AI_01_MVP_PRD.md` | 무엇을 만드는가 (제품 정의) |
| `MOVE_AI_02_HARNESS_ENGINEERING_PLAN.md` | 어떤 순서·규칙으로 일하는가 |
| `MOVE_AI_04_RETRIEVAL_GUIDANCE_구현명세.md` | 검색·안내 내부 로직 (가장 어려운 부분) |
| **`MOVE_AI_05_구현_상세명세.md`** ← 이 문서 | **스키마 · API · 임포트 · 구현 순서** |

충돌 시 우선순위:

```text
1. 사용자의 당일 최신 지시
2. 01 PRD
3. 05 이 문서 (구체 계약)
4. 04 검색 명세
5. 02 하네스 플랜
6. repository 기존 코드
```

**04와 05는 겹치지 않는다.** 검색 파이프라인 내부(후보 수집 규칙, 조건 평가, 랭킹 공식)는 04에만 있고 여기서 반복하지 않는다. 이 문서는 그 로직이 들어갈 **그릇**을 정의한다.

---

# 1. 시스템 구성과 책임 경계

```text
        React / TypeScript
                │  REST
                ▼
         Spring Boot  ─── MariaDB
                │
                │  REST (내부)
                ▼
        Python FastAPI
        (STT · 추출 · 임베딩)
```

## 절대 넘지 않는 경계

| | 담당 | 담당하지 않음 |
|---|---|---|
| **Spring** | 트랜잭션, 상태 전이, 조건 계산, **코사인 유사도 계산**, 응답 조립 | LLM 호출, 음성 처리 |
| **Python** | STT, 지식 추출, 임베딩 벡터 생성 | DB 쓰기, 상태 판단, 순위 결정 |
| **LLM** | 자연어 → 구조화 JSON | 경로 생성, 숫자 계산, 승인, 순위 |

**코사인 유사도를 Python으로 보내지 않는다.** 후보 벡터를 HTTP로 넘기면 단계마다 수백 KB가 오간다. 내적/노름 계산은 Java 20줄이다. Python은 `/embed`로 **질의문 1개**만 벡터화한다.

---

# 2. DB 스키마

MariaDB 10.6+ / `utf8mb4_general_ci`. 컬럼 크기는 실제 데이터 최대 길이(괄호 안)를 기준으로 여유를 두었다.

## 2-1. 기준 정보

```sql
CREATE TABLE places (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  place_code         VARCHAR(32)  NOT NULL UNIQUE,      -- PLACE_A
  name               VARCHAR(100) NOT NULL,             -- 실측 12자
  place_type         VARCHAR(30)  NOT NULL,             -- enum §2-6
  custom_place_type  VARCHAR(60)  NULL,
  description        VARCHAR(500) NULL,                 -- 실측 162자
  synthetic          BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE place_nodes (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  place_id           BIGINT       NOT NULL,
  node_code          VARCHAR(32)  NOT NULL UNIQUE,      -- NODE_A_01
  parent_node_id     BIGINT       NULL,
  node_type          VARCHAR(30)  NOT NULL,
  custom_node_type   VARCHAR(60)  NULL,                 -- 실측 12자
  name               VARCHAR(100) NOT NULL,             -- 실측 21자
  floor_label        VARCHAR(10)  NULL,                 -- B3F, 15F
  is_indoor          BOOLEAN      NOT NULL,
  description        VARCHAR(300) NULL,
  CONSTRAINT fk_node_place  FOREIGN KEY (place_id)       REFERENCES places(id),
  CONSTRAINT fk_node_parent FOREIGN KEY (parent_node_id) REFERENCES place_nodes(id),
  INDEX idx_node_place (place_id)
);

CREATE TABLE routes (
  id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
  place_id              BIGINT       NOT NULL,
  route_code            VARCHAR(32)  NOT NULL UNIQUE,
  name                  VARCHAR(120) NOT NULL,
  start_node_id         BIGINT       NOT NULL,
  destination_node_id   BIGINT       NOT NULL,
  vehicle_class         VARCHAR(20)  NULL,
  min_tonnage           DECIMAL(5,2) NULL,
  max_tonnage           DECIMAL(5,2) NULL,
  max_vehicle_height_m  DECIMAL(4,2) NULL,
  max_vehicle_width_m   DECIMAL(4,2) NULL,
  is_default            BOOLEAN      NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_route_place FOREIGN KEY (place_id)            REFERENCES places(id),
  CONSTRAINT fk_route_start FOREIGN KEY (start_node_id)       REFERENCES place_nodes(id),
  CONSTRAINT fk_route_dest  FOREIGN KEY (destination_node_id) REFERENCES place_nodes(id),
  INDEX idx_route_place_dest (place_id, destination_node_id)  -- 경로 선택 1단계
);

CREATE TABLE route_segments (
  id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
  route_id                  BIGINT       NOT NULL,
  segment_code              VARCHAR(32)  NOT NULL UNIQUE,
  sequence_no               INT          NOT NULL,
  from_node_id              BIGINT       NOT NULL,
  to_node_id                BIGINT       NOT NULL,
  movement_mode             VARCHAR(20)  NOT NULL,
  traversal_method          VARCHAR(20)  NOT NULL,
  custom_traversal_method   VARCHAR(60)  NULL,
  instruction               VARCHAR(300) NOT NULL,      -- 실측 45자
  is_indoor                 BOOLEAN      NOT NULL,
  CONSTRAINT fk_seg_route FOREIGN KEY (route_id)     REFERENCES routes(id),
  CONSTRAINT fk_seg_from  FOREIGN KEY (from_node_id) REFERENCES place_nodes(id),
  CONSTRAINT fk_seg_to    FOREIGN KEY (to_node_id)   REFERENCES place_nodes(id),
  UNIQUE KEY uk_route_seq (route_id, sequence_no)
);
```

## 2-2. 제보와 초안

```sql
CREATE TABLE field_reports (
  id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
  report_code               VARCHAR(32)  NULL UNIQUE,   -- 시드만. 런타임 생성 건은 NULL
  place_id                  BIGINT       NOT NULL,
  selected_scope_node_id    BIGINT       NULL,          -- 기사가 고른 대표 위치
  source_type               VARCHAR(20)  NOT NULL,      -- SYNTHETIC | VOICE | TEXT
  raw_stt_text              TEXT         NULL,
  corrected_stt_text        TEXT         NOT NULL,      -- 시드는 transcript를 여기에
  status                    VARCHAR(20)  NOT NULL,      -- SUBMITTED | EXTRACTING
                                                        -- | EXTRACTED | EXTRACTION_FAILED
  audio_recording_candidate BOOLEAN      NOT NULL DEFAULT FALSE,
  created_by                BIGINT       NULL,
  created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_report_place FOREIGN KEY (place_id)               REFERENCES places(id),
  CONSTRAINT fk_report_scope FOREIGN KEY (selected_scope_node_id) REFERENCES place_nodes(id),
  INDEX idx_report_place_status (place_id, status)
);

CREATE TABLE report_audio_files (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  report_id    BIGINT       NOT NULL,
  file_path    VARCHAR(500) NOT NULL,        -- 로컬 저장으로 충분
  mime_type    VARCHAR(60)  NULL,
  duration_ms  INT          NULL,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audio_report FOREIGN KEY (report_id) REFERENCES field_reports(id)
);

-- AI 추출 결과. 승인 전에는 절대 검색 대상이 아니다.
CREATE TABLE knowledge_drafts (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  report_id      BIGINT      NOT NULL,
  draft_index    INT         NOT NULL,       -- 한 제보 내 순번
  payload_json   JSON        NOT NULL,       -- 추출 결과 원본 그대로 (§5-2 스키마)
  status         VARCHAR(20) NOT NULL,       -- PENDING | APPROVED | REJECTED
  created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_draft_report FOREIGN KEY (report_id) REFERENCES field_reports(id),
  UNIQUE KEY uk_draft (report_id, draft_index),
  INDEX idx_draft_status (status)
);

CREATE TABLE moderation_reviews (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  draft_id          BIGINT      NOT NULL,
  reviewer_id       BIGINT      NULL,
  decision          VARCHAR(20) NOT NULL,    -- APPROVE | APPROVE_WITH_EDIT | REJECT
  edited_json       JSON        NULL,        -- 수정 승인 시 최종본
  reject_reason     VARCHAR(300) NULL,
  knowledge_item_id BIGINT      NULL,        -- 승인 시 생성된 지식
  created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_review_draft FOREIGN KEY (draft_id) REFERENCES knowledge_drafts(id)
);
```

**`payload_json`을 통째로 보관하는 이유** — AI 출력을 필드별로 쪼개 저장하면 관리자 화면에서 "AI가 원래 뭐라고 했는지"를 복원할 수 없다. 검수의 핵심은 원본 대조다.

## 2-3. 승인된 지식

```sql
CREATE TABLE knowledge_items (
  id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
  knowledge_code           VARCHAR(32)  NULL UNIQUE,    -- K_B_005 (시드만)
  place_id                 BIGINT       NOT NULL,
  source_report_id         BIGINT       NULL,
  source_draft_id          BIGINT       NULL,

  category                 VARCHAR(30)  NOT NULL,
  custom_category_label    VARCHAR(60)  NULL,           -- 실측 19자
  fact_type                VARCHAR(30)  NOT NULL,
  custom_fact_type_label   VARCHAR(60)  NULL,
  movement_mode            VARCHAR(20)  NOT NULL,
  traversal_method         VARCHAR(20)  NULL,
  custom_traversal_method  VARCHAR(60)  NULL,
  access_state             VARCHAR(20)  NULL,

  statement                VARCHAR(500) NOT NULL,       -- 실측 63자
  action_text              VARCHAR(300) NULL,           -- 실측 32자
  source_excerpt           VARCHAR(500) NOT NULL,       -- 실측 66자
  usage_scope              VARCHAR(30)  NOT NULL,

  status                   VARCHAR(20)  NOT NULL,       -- PUBLISHED | ARCHIVED
  published_at             DATETIME     NULL,           -- 신규 승인 가산점 근거
  created_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_ki_place FOREIGN KEY (place_id) REFERENCES places(id),
  INDEX idx_ki_place_status (place_id, status),         -- 후보 1차 필터
  INDEX idx_ki_published (published_at)
);

-- 1:1. 별도 테이블로 둔 이유는 조건 유무를 JOIN 한 번으로 판별하기 위함.
CREATE TABLE knowledge_conditions (
  id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
  knowledge_id           BIGINT       NOT NULL UNIQUE,
  vehicle_class          VARCHAR(20)  NULL,
  min_tonnage            DECIMAL(5,2) NULL,
  min_tonnage_inclusive  BOOLEAN      NULL,   -- ★ 임포트 시 파생 (§3-3)
  max_tonnage            DECIMAL(5,2) NULL,
  max_tonnage_inclusive  BOOLEAN      NULL,   -- ★
  max_vehicle_height_m   DECIMAL(4,2) NULL,
  max_vehicle_width_m    DECIMAL(4,2) NULL,
  active_time_start      TIME         NULL,
  active_time_end        TIME         NULL,
  active_days            VARCHAR(40)  NULL,   -- "MON,TUE,..." CSV
  extra_condition_text   VARCHAR(200) NULL,   -- ★ 필터 아님. 화면 라벨 전용
  CONSTRAINT fk_cond_ki FOREIGN KEY (knowledge_id) REFERENCES knowledge_items(id)
);

CREATE TABLE knowledge_targets (
  id                       BIGINT      AUTO_INCREMENT PRIMARY KEY,
  knowledge_id             BIGINT      NOT NULL UNIQUE,
  target_type              VARCHAR(20) NOT NULL,   -- PLACE | NODE | SEGMENT | UNKNOWN
  target_node_id           BIGINT      NULL,
  target_segment_id        BIGINT      NULL,
  target_resolution_status VARCHAR(20) NOT NULL,   -- RESOLVED | UNRESOLVED | NEEDS_REVIEW
  target_free_text         VARCHAR(200) NULL,      -- 실측 38자
  CONSTRAINT fk_tgt_ki   FOREIGN KEY (knowledge_id)      REFERENCES knowledge_items(id),
  CONSTRAINT fk_tgt_node FOREIGN KEY (target_node_id)    REFERENCES place_nodes(id),
  CONSTRAINT fk_tgt_seg  FOREIGN KEY (target_segment_id) REFERENCES route_segments(id),
  INDEX idx_tgt_node (target_node_id),        -- 후보 수집 핵심
  INDEX idx_tgt_seg  (target_segment_id),
  INDEX idx_tgt_type (target_type)            -- UNRESOLVED 슬롯 조회
);

CREATE TABLE knowledge_embeddings (
  id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
  knowledge_id        BIGINT       NOT NULL UNIQUE,
  embedding_model     VARCHAR(80)  NOT NULL,
  embedding_dimension INT          NOT NULL,
  embedding_text      TEXT         NOT NULL,   -- 재생성 추적용
  embedding_json      LONGTEXT     NOT NULL,   -- "[0.12,-0.44,...]"
  created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                   ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_emb_ki FOREIGN KEY (knowledge_id) REFERENCES knowledge_items(id)
);
```

## 2-4. 배송과 안내

```sql
-- 신규. PRD §11에 없지만 "배송 건 선택 → 경로 결정" 흐름에 필수. 시드 4~6건이면 충분.
CREATE TABLE delivery_jobs (
  id                   BIGINT       AUTO_INCREMENT PRIMARY KEY,
  job_code             VARCHAR(32)  NOT NULL UNIQUE,   -- JOB_B_01
  place_id             BIGINT       NOT NULL,
  destination_node_id  BIGINT       NOT NULL,          -- 경로 선택 1단계 입력
  recipient_label      VARCHAR(100) NULL,              -- "12층 입주사"
  address_text         VARCHAR(255) NULL,              -- 외부 지도 앱 목적지
  item_summary         VARCHAR(100) NULL,              -- "상온 / 박스 3"
  status               VARCHAR(20)  NOT NULL,          -- READY | IN_PROGRESS | DONE
  CONSTRAINT fk_job_place FOREIGN KEY (place_id)            REFERENCES places(id),
  CONSTRAINT fk_job_dest  FOREIGN KEY (destination_node_id) REFERENCES place_nodes(id)
);

CREATE TABLE guidance_sessions (
  id                    BIGINT       AUTO_INCREMENT PRIMARY KEY,
  delivery_job_id       BIGINT       NULL,
  place_id              BIGINT       NOT NULL,
  route_id              BIGINT       NOT NULL,        -- 생성 시 확정, 변경 금지
  current_sequence_no   INT          NOT NULL DEFAULT 1,
  vehicle_class         VARCHAR(20)  NULL,
  vehicle_tonnage       DECIMAL(5,2) NULL,
  vehicle_height_m      DECIMAL(4,2) NULL,
  vehicle_width_m       DECIMAL(4,2) NULL,
  context_time          DATETIME     NOT NULL,        -- ★ 시연 시 지정 가능
  status                VARCHAR(20)  NOT NULL,        -- ACTIVE | COMPLETED | ABANDONED
  started_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at          DATETIME     NULL,
  CONSTRAINT fk_gs_route FOREIGN KEY (route_id) REFERENCES routes(id),
  CONSTRAINT fk_gs_job   FOREIGN KEY (delivery_job_id) REFERENCES delivery_jobs(id)
);

CREATE TABLE users (
  id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
  login_id      VARCHAR(50)  NOT NULL UNIQUE,
  display_name  VARCHAR(50)  NOT NULL,
  role          VARCHAR(20)  NOT NULL,     -- DRIVER | ADMIN
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**인증은 구현하지 않는다.** 시드 유저 2명(기사/관리자)을 만들고 헤더나 화면 토글로 전환한다. 로그인 화면에 시간을 쓰지 않는다.

## 2-5. 검색 평가용 (선택)

```sql
CREATE TABLE rag_test_queries (
  id                     BIGINT      AUTO_INCREMENT PRIMARY KEY,
  query_code             VARCHAR(32) NOT NULL UNIQUE,
  place_id               BIGINT      NOT NULL,
  question               VARCHAR(300) NOT NULL,
  context_json           JSON        NOT NULL,
  expected_codes         VARCHAR(300) NOT NULL,   -- CSV
  must_not_return_codes  VARCHAR(300) NULL,
  reason                 VARCHAR(500) NULL
);
```

DB에 넣지 않고 JSON을 테스트 코드에서 직접 읽어도 된다. **평가 스크립트를 돌릴 수 있으면 형태는 무관하다.**

## 2-6. Enum 값 (데이터셋 실측 기준, 이 목록이 전부다)

```text
place_type        APARTMENT · OFFICE · LOGISTICS_CENTER · COMPLEX_FACILITY · OTHER
node_type         SITE · ENTRANCE · SECURITY_GATE · PARKING_POINT · LOADING_POINT
                  BUILDING · BUILDING_ENTRANCE · ELEVATOR · STAIRS · CORRIDOR
                  DELIVERY_POINT · EXIT_POINT · OTHER
movement_mode     VEHICLE · PEDESTRIAN · GENERAL
traversal_method  DRIVE · WALK · STAIRS · ELEVATOR · ESCALATOR · CART · OTHER
access_state      ALLOWED · CONDITIONAL · PROHIBITED · UNKNOWN · (null)
category          ACCESS · PARKING_STOPPING · LOADING · BUILDING_ENTRANCE
                  INTERNAL_ROUTE · ELEVATOR_STAIRS · CONGESTION_WAIT
                  DELIVERY_POINT · OTHER
fact_type         RESTRICTION · ALLOWANCE · LOCATION · INSTRUCTION
                  WARNING · CONDITION · OTHER
usage_scope       WARNING_ONLY · ACTION_GUIDANCE · ROUTE_GUIDANCE · REFERENCE_ONLY
target_type       PLACE · NODE · SEGMENT · UNKNOWN
vehicle_class     TRUCK (데이터셋 전량)
```

**허용목록에 없는 값이 들어오면 임포트를 실패시킨다.** 조용히 통과시키면 안 된다.

---

# 3. 데이터셋 임포트

## 3-1. 대상 파일

```text
datasets/synthetic_dataset_A.json    지식 36
datasets/synthetic_dataset_B.json    지식 37   ← 시연 장소
datasets/synthetic_dataset_C.json    지식 40
datasets/synthetic_dataset_D.json    지식 33
                              합계   146
```

**`docs/dataset/` 의 JSON은 임포트하지 않는다.** 그쪽은 원본과 정정본이 함께 있는
작성 이력이며, 원본을 임포트하면 검증에 실패한다. 임포트 대상은 `/datasets` 뿐이다.

임포트 전 반드시 실행한다.

```bash
python scripts/validate_datasets.py    # 기대: 전체 이슈 0건
```

장소 4 · 경로 8 · 구간 30 · 제보 41 · 검색 정답 질문 20.

## 3-2. 순서

코드 참조를 실제 PK로 바꾸므로 순서를 지켜야 한다.

```text
places → place_nodes (parent는 2-pass) → routes → route_segments
      → field_reports → knowledge_items → knowledge_conditions
      → knowledge_targets → (embedding은 별도 단계 §7 Phase 3)
```

`place_nodes.parent_node_code`는 자기 참조라 **전체 삽입 후 2회차에 UPDATE**한다.

시드 지식은 이미 검수된 것으로 간주해 `status='PUBLISHED'`, `published_at`은 임포트 시각보다 과거로 넣는다. 그래야 당일 승인하는 신규 지식만 "최근 승인" 가산점을 받는다.

```java
seed.setPublishedAt(LocalDateTime.now().minusDays(30));
```

## 3-3. ★ 톤수 경계 파생 컬럼

데이터셋 작성자마다 "초과"와 "이상"을 같은 필드에 넣었다. 값만으로는 판별이 불가능하다.

```text
K_B_005  min_tonnage=1.0  "1톤을 초과하는 차량은 …"   → 배타(>)
K_C_005  min_tonnage=5.0  "5톤 이상 트럭은 …"        → 포함(≥)
```

정답 질문이 정확히 이 경계를 찌른다. `≥`로 통일하면 `QUERY_B_02`가 깨지고 `>`로 통일하면 `QUERY_C_01`이 깨진다.

**임포트 시점에 statement 문구로 판정한다.**

```java
boolean inclusive = !(statement.contains("초과") || statement.contains("넘"));
cond.setMinTonnageInclusive(inclusive);
```

대상은 전량 8건뿐이다. 아래 표를 **단위 테스트로 고정**한다.

| code | 필드 | statement 표현 | 판정 |
|---|---|---|---|
| K_A_001 | min 1.5 | (표현 없음) | 포함 |
| K_A_002 | min 1.5 | (표현 없음) | 포함 |
| K_A_016 | min 1.5 | (표현 없음) | 포함 |
| K_B_005 | min 1.0 | 초과 | **배타** |
| K_B_006 | min 1.0 | 초과 | **배타** |
| K_C_005 | min 5.0 | 이상 | 포함 |
| K_D_023 | min 5.0 | 이상 | 포함 |
| K_D_025 | min 5.0 | 이상 | 포함 |

## 3-4. 임포트 검증 (실패 시 중단)

```text
[ ] 코드 유일성 (place/node/route/segment/knowledge/report/query)
[ ] 참조 무결성 (없는 코드 참조 금지)
[ ] route_segments.sequence_no 가 1부터 연속
[ ] 이전 구간 to_node == 다음 구간 from_node
[ ] route.start == 1번 구간 from,  route.destination == 마지막 구간 to
[ ] target_type=NODE/SEGMENT 면 해당 코드 존재
[ ] target_type=UNKNOWN 이면 target_code 없고 free_text 있음
[ ] source_excerpt 가 transcript의 부분 문자열 (공백 정규화 후)
[ ] 모든 enum이 §2-6 허용목록 안
[ ] category=OTHER 면 custom_category_label 존재
[ ] usage_scope=WARNING_ONLY 면 action_text 없음
[ ] usage_scope=ACTION/ROUTE_GUIDANCE 면 action_text 존재
[ ] rag_test_queries가 참조하는 knowledge_code 존재
```

현재 4개 파일은 이 검증을 **전부 통과**한다. 통과하지 못하면 파일이 바뀐 것이니 임포트하지 말고 원인을 찾는다.

## 3-5. 재실행 가능해야 한다

당일 여러 번 돌린다. `--truncate` 옵션으로 전체 삭제 후 재삽입하거나, `place_code` 기준 upsert로 만든다. **부분 실패 상태로 남지 않도록 장소 단위 트랜잭션**으로 감싼다.

---

# 4. Spring API 계약

공통: `Content-Type: application/json`, 시각은 ISO-8601, 오류는 아래 형식.

```json
{ "error": { "code": "NO_ROUTE_AVAILABLE", "message": "이 차량으로 진입 가능한 경로가 없습니다." } }
```

## 4-1. 기준 정보

```text
GET /api/places
    → [{ id, placeCode, name, placeType, description }]

GET /api/places/{id}
    → { id, name, placeType, description,
        nodes:  [{ id, nodeCode, nodeType, name, floorLabel, isIndoor }],
        routes: [{ id, routeCode, name, isDefault, destinationNodeId,
                   constraints:{ vehicleClass, minTonnage, maxTonnage,
                                 maxVehicleHeightM, maxVehicleWidthM } }] }

GET /api/routes/{id}
    → { id, name, totalSteps,
        segments:[{ id, sequenceNo, fromNodeName, toNodeName,
                    movementMode, traversalMethod, instruction, isIndoor }] }
```

## 4-2. 배송 건

```text
GET /api/delivery-jobs?status=READY
    → [{ id, jobCode, placeId, placeName, recipientLabel, addressText,
         itemSummary, status }]

GET /api/delivery-jobs/{id}
    → { id, jobCode, place:{id,name}, destinationNode:{id,name},
        recipientLabel, addressText, itemSummary, status }
```

## 4-3. 안내 (핵심)

```text
POST /api/guidance
{
  "deliveryJobId": 12,
  "vehicle": { "vehicleClass":"TRUCK", "tonnage":2.5,
               "heightM":2.7, "widthM":null },
  "contextTime": "2026-08-20T12:20:00"        // 생략 시 서버 시각
}
→ 201
{
  "sessionId": 88,
  "route": { "id":3, "name":"정문-지상 로비 하차 배송 경로", "totalSteps":3 },
  "currentStep": { …§4-4 형식… }
}
```

경로 선택은 04 §11-3. 후보가 0개면 **`404 NO_ROUTE_AVAILABLE`**. 기본 경로로 조용히 대체하지 않는다 — 2.5톤을 높이 2.3m 램프로 보내면 그게 사고다.

```text
GET  /api/guidance/{id}          현재 단계 재조회 (새로고침 대응)
POST /api/guidance/{id}/next     다음 단계
POST /api/guidance/{id}/complete 배송 완료 → { sessionId, status:"COMPLETED", completedAt }
```

**세션 생성 시 확정한 `route_id`는 끝까지 바꾸지 않는다.** 중간에 바뀌면 `sequence_no`가 가리키는 대상이 흔들려 상태가 깨진다.

## 4-4. 단계 응답 형식

```json
{
  "sequenceNo": 4,
  "totalSteps": 7,
  "fromNodeName": "지하 2층 하역장",
  "toNodeName": "하역장 방화문",
  "movementMode": "PEDESTRIAN",
  "traversalMethod": "CART",
  "instruction": "카트에 물품을 적재하여 방화문까지 이동한다.",
  "isLastStep": false,
  "cards": [
    {
      "knowledgeId": 213,
      "kind": "WARNING",                      // WARNING | ACTION | REFERENCE
      "statement": "하역장에서 연결 통로로 넘어가는 방화문이 매우 무겁다.",
      "actionText": null,
      "conditionLabel": null,                 // extra_condition_text 그대로
      "isRecentlyAdded": false,
      "targetName": "하역장 방화문",
      "isUnresolvedTarget": false
    }
  ]
}
```

### 카드 종류 매핑

```text
usage_scope = WARNING_ONLY                     → kind = WARNING
              ACTION_GUIDANCE / ROUTE_GUIDANCE → kind = ACTION
              REFERENCE_ONLY                   → kind = REFERENCE  (기본 접힘)
```

`actionText`가 비었는데 kind가 ACTION인 경우 `statement`로 대체한다(데이터에 4건 존재).

`isRecentlyAdded`는 `published_at`이 24시간 이내일 때 true. 화면의 **"새로 추가된 팁"** 배지 근거이며 시연의 핵심 장치다.

## 4-5. 제보

```text
POST /api/reports                    multipart
     placeId, selectedScopeNodeId?, audio(file)
     → { reportId, rawSttText }              // 내부에서 Python /stt 호출

PATCH /api/reports/{id}/transcript
     { "correctedText": "..." }
     → { reportId, correctedSttText }

POST /api/reports/{id}/extract
     → { reportId, status:"EXTRACTED",
         drafts:[{ draftId, draftIndex, payload:{…§5-2…} }] }
     실패 시 → { reportId, status:"EXTRACTION_FAILED", reason }
```

## 4-6. 검수

```text
GET  /api/moderation/drafts?status=PENDING
     → [{ draftId, reportId, placeName, createdAt, summary }]

GET  /api/moderation/drafts/{id}
     → { draftId,
         report:{ id, audioUrl, rawSttText, correctedSttText,
                  placeName, scopeNodeName },
         payload:{ …추출 결과 원본… },
         resolvedTargetName }              // UNKNOWN이면 free_text

POST /api/moderation/drafts/{id}/approve
     { "editedPayload": { … } }            // 무수정 승인이면 생략
     → { draftId, knowledgeId, embeddingCreated:true }

POST /api/moderation/drafts/{id}/reject
     { "reason": "..." }
     → { draftId, status:"REJECTED" }
```

**승인은 하나의 트랜잭션이다.**

```text
PUBLISHED 전환 → embedding_text 생성 → Python /embed 호출(동기)
              → knowledge_embeddings 저장 → 커밋
```

비동기로 빼면 "승인 직후 재조회"라는 시연의 결정적 순간에 경합이 생긴다. 관리자 한 명이 버튼 한 번 누르는 상황이므로 동기가 맞다. 실패하면 **롤백하고 명시적 오류**를 띄운다. 조용히 넘어가면 검색되지 않는 지식이 생긴다.

---

# 5. Python AI 서비스 계약

```text
GET  /health              → { "status":"ok", "model":"..." }
POST /stt
POST /extract-knowledge
POST /embed
```

`/similarity-search`는 **만들지 않는다**(§1).

## 5-1. STT / 임베딩

```text
POST /stt          multipart: audio
     → { "text":"...", "durationMs":8200 }

POST /embed        { "texts": ["...", "..."] }
     → { "model":"...", "dimension":1536,
         "vectors":[[0.12,-0.44,...], [...]] }
```

`/embed`는 **배열을 받는다.** 시드 146건을 한 번에 처리해야 한다(Phase 3).

## 5-2. 지식 추출

```text
POST /extract-knowledge
{
  "placeName": "가온스퀘어 오피스타워",
  "transcript": "...",
  "scopeNodeName": "후문 차량 출입구",
  "knownNodes":    [{ "code":"NODE_B_03", "name":"후문 차량 출입구" }, …],
  "knownSegments": [{ "code":"SEG_B_01",  "name":"후문 → 지하주차장 진입 램프" }, …]
}
```

응답은 **Pydantic으로 강제**한다.

```json
{
  "items": [
    {
      "target": {
        "target_type": "NODE",
        "target_code": "NODE_B_04",
        "target_resolution_status": "RESOLVED",
        "target_free_text": null
      },
      "category": "ACCESS",
      "custom_category_label": null,
      "fact_type": "RESTRICTION",
      "custom_fact_type_label": null,
      "movement_mode": "VEHICLE",
      "traversal_method": "DRIVE",
      "custom_traversal_method": null,
      "access_state": "PROHIBITED",
      "statement": "1톤을 초과하는 차량은 지하주차장으로 내려갈 수 없다.",
      "action_text": null,
      "source_excerpt": "1톤 넘는 차는 지하 못 내려가니까",
      "conditions": {
        "vehicle_class": "TRUCK",
        "min_tonnage": 1.0,
        "max_tonnage": null,
        "max_vehicle_height_m": null,
        "max_vehicle_width_m": null,
        "active_time_start": null,
        "active_time_end": null,
        "active_days": null,
        "extra_condition_text": null
      },
      "usage_scope": "WARNING_ONLY"
    }
  ]
}
```

### 서버측 검증 (LLM 응답 신뢰 금지)

```text
[ ] 모든 enum이 §2-6 허용목록 안
[ ] source_excerpt 가 transcript의 부분 문자열   ← 가장 중요
[ ] target_code 가 knownNodes/knownSegments 안에 존재
[ ] UNKNOWN 이면 code 없고 free_text 있음
[ ] conditions의 숫자가 transcript에 문자열로 등장
[ ] WARNING_ONLY 면 action_text 없음
```

실패 처리:

```text
1차 출력 → 검증 실패 → 오류 내용을 붙여 1회 재요청
        → 또 실패 → EXTRACTION_FAILED 반환
```

**필드를 조용히 지우고 저장하지 않는다.** 잘못된 추출은 실패로 남기고 사람이 본다.

### 프롬프트 핵심 규칙

`ai-service/app/prompts/knowledge_extraction.txt`에 둔다.

```text
1. 하나의 항목에는 하나의 사실만 담는다.
2. 차량 이동 사실과 보행/카트 사실을 한 항목에 섞지 않는다.
3. 원문에 없는 숫자·조건·행동·대체 경로를 만들지 않는다.
4. "빡빡하다/힘들다/좁다"를 "금지"로 바꾸지 않는다. → CONDITIONAL
5. 명시적으로 "못 간다/금지"일 때만 PROHIBITED.
6. source_excerpt는 원문에 실제로 있는 연속된 구절이어야 한다.
7. 대안이 원문에 없으면 action_text는 null, usage_scope는 WARNING_ONLY.
8. 알려진 위치로 설명되지 않으면 UNKNOWN + UNRESOLVED + target_free_text.
9. 허용목록에 없는 값을 만들지 않는다. 애매하면 OTHER + custom label.
```

**추출 품질 확인은 데이터셋으로 한다.** `field_reports.transcript`를 입력하고 `expected_knowledge_items`와 비교하면 41건의 정답 세트가 이미 있다.

---

# 6. 백엔드 모듈 구조

```text
com.moveai
├─ common/          예외, 응답 래퍼, enum
├─ place/           Place, PlaceNode
├─ route/           Route, RouteSegment, RouteSelector      ← 04 §11-3
├─ job/             DeliveryJob
├─ report/          FieldReport, ReportAudioFile
├─ knowledge/
│   ├─ KnowledgeItem, KnowledgeCondition, KnowledgeTarget
│   └─ embedding/   EmbeddingTextBuilder, KnowledgeEmbedding
├─ moderation/      KnowledgeDraft, ModerationReview, PublishService
├─ retrieval/
│   ├─ CandidateCollector      04 §1
│   ├─ ConditionEvaluator      04 §3
│   ├─ QueryTextBuilder        04 §5-2
│   ├─ CosineCalculator        순수 함수, 단위 테스트 쉬움
│   └─ RankingService          04 §6
├─ guidance/        GuidanceSession, GuidanceService, StepAssembler
├─ ai/              SttClient, ExtractionClient, EmbeddingClient (interface)
└─ dataset/         DatasetValidator, DatasetImportService
```

## AI 클라이언트는 인터페이스 뒤에 둔다

```java
public interface EmbeddingClient { List<float[]> embed(List<String> texts); }

@Profile("!demo") class HttpEmbeddingClient implements EmbeddingClient { … }
@Profile("demo")  class MockEmbeddingClient implements EmbeddingClient { … }
```

외부 호출이 죽어도 화면 흐름은 시연할 수 있어야 한다. 단 **mock 결과를 실제 AI 결과인 것처럼 발표하지 않는다.**

`retrieval` 패키지의 4개 클래스는 전부 **DB 없이 단위 테스트 가능한 순수 로직**으로 만든다. 당일 디버깅 속도가 여기서 갈린다.

---

# 7. 구현 순서

Phase 번호보다 **"세로로 한 줄이 실제로 동작하는가"**가 우선이다. 각 단계는 끝나는 즉시 커밋 가능한 상태여야 한다.

## Phase 0 — 상황 파악 (20~30분)

repository tree, git status, 빌드 파일, 기존 코드 확인 → `docs/IMPLEMENTATION_STATUS.md` 생성.
**바로 대규모 리팩터링 하지 않는다.**

## Phase 1 — 뼈대

```text
완료 조건: 프론트 페이지 열림 · 백엔드 /health 200
          · AI /health 200 · MariaDB 연결 성공
```

## Phase 2 — 임포트

```text
DDL 적용 → B_fixed 임포트 → 검증 통과 → 나머지 3개
완료 조건: GET /api/places, GET /api/routes/{id} 실제 응답
          장소 4 · 지식 146건 확인
```

## Phase 3 — 임베딩 + 검색

```text
PUBLISHED 146건 → embedding_text 생성 → /embed 일괄 → DB 저장
CosineCalculator + ConditionEvaluator 단위 테스트
완료 조건: 정답 질문 20개로 평가 스크립트가 돌고 Hit@3 숫자가 나옴
```

**여기서 §3-3 톤수 경계 테스트 8건을 반드시 통과시킨다.**

## Phase 4 — 안내 ★ 첫 발표 가능 지점

```text
RouteSelector → GuidanceSession → 단계별 카드 조립
완료 조건:
  [ ] 1톤으로 시작 → ROUTE_B_01 (7단계) 선택됨
  [ ] 2.5톤으로 시작 → ROUTE_B_02 (3단계) 선택됨
  [ ] 같은 지식이 연속 두 단계에 중복 노출되지 않음
  [ ] 모든 단계에 카드가 최소 1장
  [ ] contextTime=12:30 이면 K_B_014 노출, 15:00 이면 사라짐
  [ ] complete 동작
```

**이 시점에 시연 영상 1막을 찍을 수 있다.**

## Phase 5 — 음성 제보

```text
녹음 → 업로드 → STT → 기사 수정 → 저장
오디오는 로컬 디렉터리로 충분
```

## Phase 6 — 추출

```text
corrected_stt_text → ExtractionClient → 검증 → knowledge_drafts
데이터셋 transcript로 품질 확인
```

## Phase 7 — 검수와 발행 ★ 핵심 완성 지점

```text
Draft → 승인 → PUBLISHED → embedding → 검색 반영
완료 조건:
  [ ] 새 제보 승인 후 같은 경로 재시작 시 새 카드가 노출
  [ ] isRecentlyAdded = true
```

**여기까지가 서비스 정체성이다.** 시간이 부족하면 이 다음은 전부 버린다.

## Phase 8 — 다듬기

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
  - 톤수 경계 8건 (§3-3 표)
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
LLM_API_KEY / LLM_MODEL
EMBEDDING_MODEL
AUDIO_STORAGE_PATH      ./data/audio
SPRING_PROFILES_ACTIVE  local | demo
```

`.env.example`만 커밋한다. **실제 키를 커밋하지 않는다.**

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

# 11. 시연 대비 확인 목록

```text
[ ] B 장소 1톤 / 2.5톤 경로 분기 동작
[ ] contextTime 지정으로 시간 조건 지식 노출 제어 가능
[ ] 시연용 신규 지식이 기존 146건과 겹치지 않음
[ ] 승인 → 재조회에서 "새로 추가된 팁" 배지 노출
[ ] AI 호출 실패 시 화면이 죽지 않고 오류 표시
[ ] 임포트 재실행 가능 (데모 중 초기화 필요할 수 있음)
```

**ROUTE_D_01 3단계(중앙 엘리베이터 → 15층)에는 붙는 지식이 없다.** D를 시연에 쓸 경우 빈 단계가 보인다. B를 쓰면 문제없다.
