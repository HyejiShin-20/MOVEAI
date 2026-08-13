CREATE DATABASE IF NOT EXISTS move_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE move_ai;

CREATE TABLE IF NOT EXISTS places (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  place_code VARCHAR(100) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS place_nodes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  place_id BIGINT NOT NULL,
  node_code VARCHAR(100) NOT NULL UNIQUE,
  parent_node_id BIGINT NULL,
  CONSTRAINT fk_nodes_place FOREIGN KEY (place_id) REFERENCES places(id),
  CONSTRAINT fk_nodes_parent FOREIGN KEY (parent_node_id) REFERENCES place_nodes(id)
);

CREATE TABLE IF NOT EXISTS routes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  place_id BIGINT NOT NULL,
  route_code VARCHAR(100) NOT NULL UNIQUE,
  start_node_id BIGINT NOT NULL,
  destination_node_id BIGINT NOT NULL,
  CONSTRAINT fk_routes_place FOREIGN KEY (place_id) REFERENCES places(id),
  CONSTRAINT fk_routes_start FOREIGN KEY (start_node_id) REFERENCES place_nodes(id),
  CONSTRAINT fk_routes_dest FOREIGN KEY (destination_node_id) REFERENCES place_nodes(id)
);

CREATE TABLE IF NOT EXISTS route_segments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  route_id BIGINT NOT NULL,
  segment_code VARCHAR(100) NOT NULL UNIQUE,
  from_node_id BIGINT NOT NULL,
  to_node_id BIGINT NOT NULL,
  sequence_no INT NOT NULL,
  CONSTRAINT fk_segments_route FOREIGN KEY (route_id) REFERENCES routes(id),
  CONSTRAINT fk_segments_from FOREIGN KEY (from_node_id) REFERENCES place_nodes(id),
  CONSTRAINT fk_segments_to FOREIGN KEY (to_node_id) REFERENCES place_nodes(id)
);

CREATE TABLE IF NOT EXISTS field_reports (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  place_id BIGINT NOT NULL,
  report_code VARCHAR(100) NOT NULL UNIQUE,
  transcript TEXT,
  corrected_stt_text TEXT,
  audio_recording_candidate BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_reports_place FOREIGN KEY (place_id) REFERENCES places(id)
);

CREATE TABLE IF NOT EXISTS knowledge_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source_report_id BIGINT NOT NULL,
  knowledge_code VARCHAR(100) NOT NULL UNIQUE,
  statement TEXT NOT NULL,
  publication_status VARCHAR(30) NOT NULL,
  CONSTRAINT fk_knowledge_report FOREIGN KEY (source_report_id) REFERENCES field_reports(id)
);

CREATE TABLE IF NOT EXISTS knowledge_conditions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  knowledge_item_id BIGINT NOT NULL,
  min_tonnage DOUBLE NULL,
  max_tonnage DOUBLE NULL,
  min_tonnage_inclusive BOOLEAN NOT NULL DEFAULT TRUE,
  max_tonnage_inclusive BOOLEAN NOT NULL DEFAULT TRUE,
  max_vehicle_height DOUBLE NULL,
  CONSTRAINT fk_conditions_knowledge FOREIGN KEY (knowledge_item_id) REFERENCES knowledge_items(id)
);

CREATE TABLE IF NOT EXISTS knowledge_targets (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  knowledge_item_id BIGINT NOT NULL,
  target_type VARCHAR(30) NOT NULL,
  target_node_id BIGINT NULL,
  CONSTRAINT fk_targets_knowledge FOREIGN KEY (knowledge_item_id) REFERENCES knowledge_items(id),
  CONSTRAINT fk_targets_node FOREIGN KEY (target_node_id) REFERENCES place_nodes(id)
);

CREATE INDEX idx_nodes_place ON place_nodes(place_id);
CREATE INDEX idx_routes_place ON routes(place_id);
CREATE INDEX idx_segments_route ON route_segments(route_id);
CREATE INDEX idx_reports_place ON field_reports(place_id);
CREATE INDEX idx_knowledge_status ON knowledge_items(publication_status);
