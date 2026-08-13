package com.moveai.moderation.service;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.moveai.dataset.validation.TonnageBoundaryRule;

/**
 * 승인된 초안을 PUBLISHED 지식으로 굳힌다.
 *
 * <p>지식·조건·타깃·임베딩·검수 이력·초안 상태를 <b>한 트랜잭션</b>으로 저장한다. 하나라도
 * 실패하면 전부 롤백한다 — 벡터 없는 지식이 검색에 걸리면 그게 더 나쁘다 (04 §7).
 *
 * <p>임베딩 생성(외부 호출)은 이 클래스에 들어오기 전에 끝나 있어야 한다.
 */
@Component
public class KnowledgePublisher {

    private final JdbcTemplate jdbcTemplate;

    public KnowledgePublisher(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long publish(PublishCommand command) {
        JsonNode payload = command.payload();
        long knowledgeId = insertKnowledge(command, payload);
        insertConditions(knowledgeId, payload);
        insertTarget(knowledgeId, payload, command);
        insertEmbedding(knowledgeId, command);
        insertReview(knowledgeId, command);
        return knowledgeId;
    }

    private long insertKnowledge(PublishCommand command, JsonNode payload) {
        return insert(
                "INSERT INTO knowledge_items (place_id, source_report_id, source_draft_id, category,"
                        + " custom_category_label, fact_type, custom_fact_type_label, movement_mode,"
                        + " traversal_method, custom_traversal_method, access_state, statement,"
                        + " action_text, source_excerpt, usage_scope, status, published_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PUBLISHED', ?)",
                command.placeId(), command.reportId(), command.draftId(),
                text(payload, "category"), text(payload, "custom_category_label"),
                text(payload, "fact_type"), text(payload, "custom_fact_type_label"),
                text(payload, "movement_mode"), text(payload, "traversal_method"),
                text(payload, "custom_traversal_method"), text(payload, "access_state"),
                text(payload, "statement"), text(payload, "action_text"),
                text(payload, "source_excerpt"), text(payload, "usage_scope"),
                // published_at 이 지금이어야 04 §6-3 의 "최근 승인" 가산점과 배지가 붙는다.
                LocalDateTime.now());
    }

    private void insertConditions(long knowledgeId, JsonNode payload) {
        JsonNode conditions = payload.get("conditions");
        if (conditions == null || conditions.isNull()) {
            return;
        }
        BigDecimal minTonnage = decimal(conditions, "min_tonnage");
        BigDecimal maxTonnage = decimal(conditions, "max_tonnage");
        String statement = text(payload, "statement");

        jdbcTemplate.update(
                "INSERT INTO knowledge_conditions (knowledge_id, vehicle_class, min_tonnage,"
                        + " min_tonnage_inclusive, max_tonnage, max_tonnage_inclusive,"
                        + " max_vehicle_height_m, max_vehicle_width_m, active_time_start,"
                        + " active_time_end, active_days, extra_condition_text)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                knowledgeId, text(conditions, "vehicle_class"),
                minTonnage, TonnageBoundaryRule.inclusive(minTonnage, statement),
                maxTonnage, TonnageBoundaryRule.inclusive(maxTonnage, statement),
                decimal(conditions, "max_vehicle_height_m"), decimal(conditions, "max_vehicle_width_m"),
                time(conditions, "active_time_start"), time(conditions, "active_time_end"),
                csv(conditions.get("active_days")), text(conditions, "extra_condition_text"));
    }

    private void insertTarget(long knowledgeId, JsonNode payload, PublishCommand command) {
        JsonNode target = payload.get("target");
        String targetType = text(target, "target_type");
        // UNKNOWN 은 비슷한 노드에 억지로 붙이지 않는다 (절대 규칙 8).
        jdbcTemplate.update(
                "INSERT INTO knowledge_targets (knowledge_id, target_type, target_node_id,"
                        + " target_segment_id, target_resolution_status, target_free_text)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                knowledgeId, targetType,
                "NODE".equals(targetType) ? command.targetNodeId() : null,
                "SEGMENT".equals(targetType) ? command.targetSegmentId() : null,
                text(target, "target_resolution_status"), text(target, "target_free_text"));
    }

    private void insertEmbedding(long knowledgeId, PublishCommand command) {
        jdbcTemplate.update(
                "INSERT INTO knowledge_embeddings (knowledge_id, embedding_model, embedding_dimension,"
                        + " embedding_text, embedding_json) VALUES (?, ?, ?, ?, ?)",
                knowledgeId, command.embeddingModel(), command.vector().length,
                command.embeddingText(), toJsonArray(command.vector()));
    }

    private void insertReview(long knowledgeId, PublishCommand command) {
        jdbcTemplate.update(
                "INSERT INTO moderation_reviews (draft_id, decision, edited_json, knowledge_item_id)"
                        + " VALUES (?, ?, ?, ?)",
                command.draftId(), command.edited() ? "APPROVE_WITH_EDIT" : "APPROVE",
                command.edited() ? command.payload().toString() : null, knowledgeId);
    }

    private long insert(String sql, Object... params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < params.length; index++) {
                statement.setObject(index + 1, params[index]);
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("생성된 키를 받지 못했다.");
        }
        return key.longValue();
    }

    private static String toJsonArray(double[] vector) {
        List<String> values = new ArrayList<>(vector.length);
        for (double value : vector) {
            values.add(Double.toString(value));
        }
        return "[" + String.join(",", values) + "]";
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.decimalValue();
    }

    private static Time time(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? null : Time.valueOf(LocalTime.parse(value));
    }

    private static String csv(JsonNode array) {
        if (array == null || !array.isArray() || array.isEmpty()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        array.forEach(item -> values.add(item.asText()));
        return String.join(",", values);
    }

    /** 승인 시점에 확정된 값 묶음. 외부 호출 결과(vector)까지 이미 들어 있다. */
    public record PublishCommand(
            Long draftId,
            Long reportId,
            Long placeId,
            Long targetNodeId,
            Long targetSegmentId,
            JsonNode payload,
            boolean edited,
            String embeddingText,
            String embeddingModel,
            double[] vector) {}
}
