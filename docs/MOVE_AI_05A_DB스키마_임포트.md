# 05A — DB 스키마와 데이터셋 임포트

> Phase 2a·2b에서 쓴다. 그 외 Phase에서는 열 필요 없다.
>
> | 문서 | 내용 |
> |---|---|
> | **05A** ← 이 문서 | DB 스키마(DDL) · 데이터셋 임포트 |
> | `05B` | Spring API 계약 · Python AI 서비스 계약 |
> | `05C` | 시스템 구성 · 모듈 구조 · 구현 순서 · 테스트 · 운용 |
> | `04` | 검색·안내 내부 로직 |
>
> 충돌 시 우선순위: **사용자 지시 → 01 → 05 → 04 → 02 → 기존 코드**
> 스키마와 계약의 출처는 05뿐이다. `DB_SCHEMA.md`를 따로 만들지 않는다.

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
-- 신규. PRD §11에 없지만 "배송 건 선택 → 경로 결정" 흐름에 필수.
-- 데이터셋에 없으므로 임포트 마지막에 코드로 생성한다. 시드 5건은 §3-5.
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
      → knowledge_targets → delivery_jobs (§3-5, 코드로 생성)
      → (embedding은 별도 단계, 05C §7 Phase 3)
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

## 3-5. ★ 시드 배송 건 (delivery_jobs)

`delivery_jobs`는 데이터셋에 없다. **임포트 마지막 단계에서 아래 5건을 코드로 생성한다.**
이게 없으면 배송 목록 화면이 비고, `POST /api/guidance`를 호출할 대상이 없어 Phase 4가 시작되지 않는다.

**목적지는 Route가 실제로 향하는 노드여야 한다.** 그 외 노드를 넣으면 경로 후보가 0개가 되어
`NO_ROUTE_AVAILABLE`이 난다. 전체 데이터에서 가능한 목적지는 7개뿐이고, 아래 5건은 그중에서 골랐다.

| job_code | place | destination_node | recipient_label | item_summary | status |
|---|---|---|---|---|---|
| `JOB_B_01` | `PLACE_B` | `NODE_B_14` | 12층 입주사 안내데스크 | 일반 / 박스 3 | READY |
| `JOB_B_02` | `PLACE_B` | `NODE_B_14` | 12층 회의실 | 일반 / 박스 1 | READY |
| `JOB_C_01` | `PLACE_C` | `NODE_C_11` | 상온 배송 인계점 | 상온 / 박스 8 | READY |
| `JOB_C_02` | `PLACE_C` | `NODE_C_12` | 냉장 배송 인계점 | 냉장 / 보냉박스 4 | READY |
| `JOB_A_01` | `PLACE_A` | `NODE_A_10` | 101동 1203호 | 일반 / 박스 2 | READY |

`address_text`는 외부 지도 앱으로 넘길 주소 문자열이며 장소명 수준으로 채우면 된다.

### 왜 이 구성인가

- **`JOB_B_01`이 시연 대상이다.** 목록 최상단에 오도록 정렬한다(`job_code` 오름차순이면 자연히 앞에 온다).
- `JOB_B_02` — 같은 장소에 배송 건이 여러 개 있는 것이 자연스럽다. 목록이 비어 보이지 않게 한다.
- `JOB_C_01` / `JOB_C_02` — **목적지로 경로가 갈리는** 사례다(상온/냉장). B는 차량으로 갈린다.
  "다른 장소도 되나요?"라는 질문에 눌러서 보여줄 수 있다.
- **D는 넣지 않는다.** `ROUTE_D_01`의 3단계에 붙는 지식이 0건이라, 시연 중 클릭하면 카드 없는 빈 화면이 나온다.

시간이 부족하면 `JOB_B_01` · `JOB_C_01` · `JOB_A_01` 3건만 만들어도 시연은 성립한다.

## 3-6. 재실행 가능해야 한다

당일 여러 번 돌린다. `--truncate` 옵션으로 전체 삭제 후 재삽입하거나, `place_code` 기준 upsert로 만든다. **부분 실패 상태로 남지 않도록 장소 단위 트랜잭션**으로 감싼다.

---
