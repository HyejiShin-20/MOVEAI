-- MOVE-AI 스키마. 출처는 docs/MOVE_AI_05A_DB스키마_임포트.md §2 하나뿐이다.
-- Hibernate ddl-auto=none 이므로 테이블은 이 파일로만 만든다.
-- IF NOT EXISTS 를 붙여 임포트를 여러 번 돌려도 안전하게 한다.

-- ── 기준 정보 ────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS places (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  place_code         VARCHAR(32)  NOT NULL UNIQUE,
  name               VARCHAR(100) NOT NULL,
  place_type         VARCHAR(30)  NOT NULL,
  custom_place_type  VARCHAR(60)  NULL,
  description        VARCHAR(500) NULL,
  synthetic          BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS place_nodes (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  place_id           BIGINT       NOT NULL,
  node_code          VARCHAR(32)  NOT NULL UNIQUE,
  parent_node_id     BIGINT       NULL,
  node_type          VARCHAR(30)  NOT NULL,
  custom_node_type   VARCHAR(60)  NULL,
  name               VARCHAR(100) NOT NULL,
  floor_label        VARCHAR(10)  NULL,
  is_indoor          BOOLEAN      NOT NULL,
  description        VARCHAR(300) NULL,
  CONSTRAINT fk_node_place  FOREIGN KEY (place_id)       REFERENCES places(id),
  CONSTRAINT fk_node_parent FOREIGN KEY (parent_node_id) REFERENCES place_nodes(id),
  INDEX idx_node_place (place_id)
);

CREATE TABLE IF NOT EXISTS routes (
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
  INDEX idx_route_place_dest (place_id, destination_node_id)
);

CREATE TABLE IF NOT EXISTS route_segments (
  id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
  route_id                  BIGINT       NOT NULL,
  segment_code              VARCHAR(32)  NOT NULL UNIQUE,
  sequence_no               INT          NOT NULL,
  from_node_id              BIGINT       NOT NULL,
  to_node_id                BIGINT       NOT NULL,
  movement_mode             VARCHAR(20)  NOT NULL,
  traversal_method          VARCHAR(20)  NOT NULL,
  custom_traversal_method   VARCHAR(60)  NULL,
  instruction               VARCHAR(300) NOT NULL,
  is_indoor                 BOOLEAN      NOT NULL,
  CONSTRAINT fk_seg_route FOREIGN KEY (route_id)     REFERENCES routes(id),
  CONSTRAINT fk_seg_from  FOREIGN KEY (from_node_id) REFERENCES place_nodes(id),
  CONSTRAINT fk_seg_to    FOREIGN KEY (to_node_id)   REFERENCES place_nodes(id),
  UNIQUE KEY uk_route_seq (route_id, sequence_no)
);

-- ── 제보와 초안 ──────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS field_reports (
  id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
  report_code               VARCHAR(32)  NULL UNIQUE,
  place_id                  BIGINT       NOT NULL,
  selected_scope_node_id    BIGINT       NULL,
  source_type               VARCHAR(20)  NOT NULL,
  raw_stt_text              TEXT         NULL,
  corrected_stt_text        TEXT         NOT NULL,
  status                    VARCHAR(20)  NOT NULL,
  audio_recording_candidate BOOLEAN      NOT NULL DEFAULT FALSE,
  created_by                BIGINT       NULL,
  created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_report_place FOREIGN KEY (place_id)               REFERENCES places(id),
  CONSTRAINT fk_report_scope FOREIGN KEY (selected_scope_node_id) REFERENCES place_nodes(id),
  INDEX idx_report_place_status (place_id, status)
);

CREATE TABLE IF NOT EXISTS report_audio_files (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  report_id    BIGINT       NOT NULL,
  file_path    VARCHAR(500) NOT NULL,
  mime_type    VARCHAR(60)  NULL,
  duration_ms  INT          NULL,
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audio_report FOREIGN KEY (report_id) REFERENCES field_reports(id)
);

CREATE TABLE IF NOT EXISTS knowledge_drafts (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  report_id      BIGINT      NOT NULL,
  draft_index    INT         NOT NULL,
  payload_json   JSON        NOT NULL,
  status         VARCHAR(20) NOT NULL,
  created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_draft_report FOREIGN KEY (report_id) REFERENCES field_reports(id),
  UNIQUE KEY uk_draft (report_id, draft_index),
  INDEX idx_draft_status (status)
);

CREATE TABLE IF NOT EXISTS moderation_reviews (
  id                BIGINT      AUTO_INCREMENT PRIMARY KEY,
  draft_id          BIGINT      NOT NULL,
  reviewer_id       BIGINT      NULL,
  decision          VARCHAR(20) NOT NULL,
  edited_json       JSON        NULL,
  reject_reason     VARCHAR(300) NULL,
  knowledge_item_id BIGINT      NULL,
  created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_review_draft FOREIGN KEY (draft_id) REFERENCES knowledge_drafts(id)
);

-- ── 승인된 지식 ──────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS knowledge_items (
  id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
  knowledge_code           VARCHAR(32)  NULL UNIQUE,
  place_id                 BIGINT       NOT NULL,
  source_report_id         BIGINT       NULL,
  source_draft_id          BIGINT       NULL,
  category                 VARCHAR(30)  NOT NULL,
  custom_category_label    VARCHAR(60)  NULL,
  fact_type                VARCHAR(30)  NOT NULL,
  custom_fact_type_label   VARCHAR(60)  NULL,
  movement_mode            VARCHAR(20)  NOT NULL,
  traversal_method         VARCHAR(20)  NULL,
  custom_traversal_method  VARCHAR(60)  NULL,
  access_state             VARCHAR(20)  NULL,
  statement                VARCHAR(500) NOT NULL,
  action_text              VARCHAR(300) NULL,
  source_excerpt           VARCHAR(500) NOT NULL,
  usage_scope              VARCHAR(30)  NOT NULL,
  status                   VARCHAR(20)  NOT NULL,
  published_at             DATETIME     NULL,
  created_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_ki_place FOREIGN KEY (place_id) REFERENCES places(id),
  INDEX idx_ki_place_status (place_id, status),
  INDEX idx_ki_published (published_at)
);

CREATE TABLE IF NOT EXISTS knowledge_conditions (
  id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
  knowledge_id           BIGINT       NOT NULL UNIQUE,
  vehicle_class          VARCHAR(20)  NULL,
  min_tonnage            DECIMAL(5,2) NULL,
  min_tonnage_inclusive  BOOLEAN      NULL,
  max_tonnage            DECIMAL(5,2) NULL,
  max_tonnage_inclusive  BOOLEAN      NULL,
  max_vehicle_height_m   DECIMAL(4,2) NULL,
  max_vehicle_width_m    DECIMAL(4,2) NULL,
  active_time_start      TIME         NULL,
  active_time_end        TIME         NULL,
  active_days            VARCHAR(40)  NULL,
  extra_condition_text   VARCHAR(200) NULL,
  CONSTRAINT fk_cond_ki FOREIGN KEY (knowledge_id) REFERENCES knowledge_items(id)
);

CREATE TABLE IF NOT EXISTS knowledge_targets (
  id                       BIGINT      AUTO_INCREMENT PRIMARY KEY,
  knowledge_id             BIGINT      NOT NULL UNIQUE,
  target_type              VARCHAR(20) NOT NULL,
  target_node_id           BIGINT      NULL,
  target_segment_id        BIGINT      NULL,
  target_resolution_status VARCHAR(20) NOT NULL,
  target_free_text         VARCHAR(200) NULL,
  CONSTRAINT fk_tgt_ki   FOREIGN KEY (knowledge_id)      REFERENCES knowledge_items(id),
  CONSTRAINT fk_tgt_node FOREIGN KEY (target_node_id)    REFERENCES place_nodes(id),
  CONSTRAINT fk_tgt_seg  FOREIGN KEY (target_segment_id) REFERENCES route_segments(id),
  INDEX idx_tgt_node (target_node_id),
  INDEX idx_tgt_seg  (target_segment_id),
  INDEX idx_tgt_type (target_type)
);

CREATE TABLE IF NOT EXISTS knowledge_embeddings (
  id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
  knowledge_id        BIGINT       NOT NULL UNIQUE,
  embedding_model     VARCHAR(80)  NOT NULL,
  embedding_dimension INT          NOT NULL,
  embedding_text      TEXT         NOT NULL,
  embedding_json      LONGTEXT     NOT NULL,
  created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                   ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_emb_ki FOREIGN KEY (knowledge_id) REFERENCES knowledge_items(id)
);

-- ── 배송과 안내 ──────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS delivery_jobs (
  id                   BIGINT       AUTO_INCREMENT PRIMARY KEY,
  job_code             VARCHAR(32)  NOT NULL UNIQUE,
  place_id             BIGINT       NOT NULL,
  destination_node_id  BIGINT       NOT NULL,
  recipient_label      VARCHAR(100) NULL,
  address_text         VARCHAR(255) NULL,
  item_summary         VARCHAR(100) NULL,
  status               VARCHAR(20)  NOT NULL,
  CONSTRAINT fk_job_place FOREIGN KEY (place_id)            REFERENCES places(id),
  CONSTRAINT fk_job_dest  FOREIGN KEY (destination_node_id) REFERENCES place_nodes(id)
);

CREATE TABLE IF NOT EXISTS guidance_sessions (
  id                    BIGINT       AUTO_INCREMENT PRIMARY KEY,
  delivery_job_id       BIGINT       NULL,
  place_id              BIGINT       NOT NULL,
  route_id              BIGINT       NOT NULL,
  current_sequence_no   INT          NOT NULL DEFAULT 1,
  vehicle_class         VARCHAR(20)  NULL,
  vehicle_tonnage       DECIMAL(5,2) NULL,
  vehicle_height_m      DECIMAL(4,2) NULL,
  vehicle_width_m       DECIMAL(4,2) NULL,
  context_time          DATETIME     NOT NULL,
  status                VARCHAR(20)  NOT NULL,
  started_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at          DATETIME     NULL,
  CONSTRAINT fk_gs_route FOREIGN KEY (route_id)        REFERENCES routes(id),
  CONSTRAINT fk_gs_job   FOREIGN KEY (delivery_job_id) REFERENCES delivery_jobs(id)
);

CREATE TABLE IF NOT EXISTS users (
  id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
  login_id      VARCHAR(50)  NOT NULL UNIQUE,
  display_name  VARCHAR(50)  NOT NULL,
  role          VARCHAR(20)  NOT NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── 검색 평가용 ──────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS rag_test_queries (
  id                     BIGINT      AUTO_INCREMENT PRIMARY KEY,
  query_code             VARCHAR(32) NOT NULL UNIQUE,
  place_id               BIGINT      NOT NULL,
  question               VARCHAR(300) NOT NULL,
  context_json           JSON        NOT NULL,
  expected_codes         VARCHAR(300) NOT NULL,
  must_not_return_codes  VARCHAR(300) NULL,
  reason                 VARCHAR(500) NULL,
  CONSTRAINT fk_rag_place FOREIGN KEY (place_id) REFERENCES places(id)
);
