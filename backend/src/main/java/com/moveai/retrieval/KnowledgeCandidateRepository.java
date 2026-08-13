package com.moveai.retrieval;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** PUBLISHED + embedding이 모두 있는 지식만 검색 경계로 올린다. */
@Repository
public class KnowledgeCandidateRepository {

    private static final String SELECT_BY_PLACE = """
            SELECT ki.id, ki.knowledge_code, ki.place_id, kt.target_type,
                   kt.target_node_id, kt.target_segment_id,
                   ki.movement_mode, ki.traversal_method, ki.fact_type, ki.access_state,
                   kc.vehicle_class, kc.min_tonnage, kc.min_tonnage_inclusive,
                   kc.max_tonnage, kc.max_tonnage_inclusive,
                   kc.max_vehicle_height_m, kc.max_vehicle_width_m,
                   kc.active_time_start, kc.active_time_end, kc.active_days,
                   kc.extra_condition_text, ki.published_at, ke.embedding_json
              FROM knowledge_items ki
              JOIN knowledge_targets kt ON kt.knowledge_id = ki.id
              JOIN knowledge_embeddings ke ON ke.knowledge_id = ki.id
              LEFT JOIN knowledge_conditions kc ON kc.knowledge_id = ki.id
             WHERE ki.place_id = ? AND ki.status = 'PUBLISHED'
             ORDER BY ki.knowledge_code
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public KnowledgeCandidateRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<KnowledgeCandidate> findPublishedByPlaceId(long placeId) {
        return jdbcTemplate.query(SELECT_BY_PLACE, this::mapCandidate, placeId);
    }

    private KnowledgeCandidate mapCandidate(ResultSet rs, int rowNumber) throws SQLException {
        KnowledgeCandidate.Conditions conditions = new KnowledgeCandidate.Conditions(
                rs.getString("vehicle_class"),
                nullableDouble(rs, "min_tonnage"),
                nullableBoolean(rs, "min_tonnage_inclusive"),
                nullableDouble(rs, "max_tonnage"),
                nullableBoolean(rs, "max_tonnage_inclusive"),
                nullableDouble(rs, "max_vehicle_height_m"),
                nullableDouble(rs, "max_vehicle_width_m"),
                rs.getTime("active_time_start") == null ? null : rs.getTime("active_time_start").toLocalTime(),
                rs.getTime("active_time_end") == null ? null : rs.getTime("active_time_end").toLocalTime(),
                parseDays(rs.getString("active_days")),
                rs.getString("extra_condition_text"));
        return new KnowledgeCandidate(
                rs.getLong("id"), rs.getString("knowledge_code"), rs.getLong("place_id"),
                rs.getString("target_type"), nullableLong(rs, "target_node_id"),
                nullableLong(rs, "target_segment_id"), rs.getString("movement_mode"),
                rs.getString("traversal_method"), rs.getString("fact_type"),
                rs.getString("access_state"), conditions,
                rs.getTimestamp("published_at") == null ? null
                        : rs.getTimestamp("published_at").toLocalDateTime(),
                parseVector(rs.getString("embedding_json")));
    }

    private double[] parseVector(String json) {
        try {
            return objectMapper.readValue(json, double[].class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 embedding_json을 읽지 못했습니다.", exception);
        }
    }

    private Set<DayOfWeek> parseDays(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(csv.split(","))
                .map(String::strip)
                .map(DayCode::parse)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Boolean nullableBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }
}
