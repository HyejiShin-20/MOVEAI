package com.moveai.dataset.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.moveai.dataset.dto.DatasetPayload;
import com.moveai.dataset.validation.EnumAllowList;
import com.moveai.dataset.validation.TonnageBoundaryRule;

/**
 * datasets/*.json 을 DB로 넣는다 (05A §3).
 *
 * <p>코드 참조를 실제 PK로 바꾸므로 삽입 순서를 지켜야 한다. {@code place_nodes.parent_node_code}
 * 는 자기 참조라 전체 삽입 후 2회차에 UPDATE 한다.
 *
 * <p>여러 번 돌린다는 전제로 <b>전체 삭제 후 재삽입</b>한다 (05A §3-6). 부분 실패 상태로
 * 남지 않도록 전체를 한 트랜잭션으로 감싼다.
 */
@Service
public class DatasetImportService {

    private static final Logger log = LoggerFactory.getLogger(DatasetImportService.class);

    private static final List<String> DATASET_FILES = List.of(
            "synthetic_dataset_A.json",
            "synthetic_dataset_B.json",
            "synthetic_dataset_C.json",
            "synthetic_dataset_D.json");

    /** 자식 → 부모 순. TRUNCATE 는 이 순서로 돈다. */
    private static final List<String> TABLES_CHILD_FIRST = List.of(
            "guidance_sessions", "delivery_jobs", "rag_test_queries",
            "knowledge_embeddings", "knowledge_targets", "knowledge_conditions",
            "moderation_reviews", "knowledge_drafts", "knowledge_items",
            "report_audio_files", "field_reports",
            "route_segments", "routes", "place_nodes", "places", "users");

    /** 시연 시연 대상 배송 건. 목적지는 Route 가 실제로 향하는 노드여야 한다 (05A §3-5). */
    private static final List<SeedJob> SEED_JOBS = List.of(
            new SeedJob("JOB_B_01", "NODE_B_14", "12층 입주사 안내데스크", "일반 / 박스 3"),
            new SeedJob("JOB_B_02", "NODE_B_14", "12층 회의실", "일반 / 박스 1"),
            new SeedJob("JOB_C_01", "NODE_C_11", "상온 배송 인계점", "상온 / 박스 8"),
            new SeedJob("JOB_C_02", "NODE_C_12", "냉장 배송 인계점", "냉장 / 보냉박스 4"),
            new SeedJob("JOB_A_01", "NODE_A_10", "101동 1203호", "일반 / 박스 2"));

    private final JdbcTemplate jdbcTemplate;
    private final Path datasetDir;
    private final ObjectMapper objectMapper;

    public DatasetImportService(
            JdbcTemplate jdbcTemplate,
            @Value("${moveai.dataset.dir}") String datasetDir) {
        this.jdbcTemplate = jdbcTemplate;
        this.datasetDir = Path.of(datasetDir).toAbsolutePath().normalize();
        this.objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Transactional
    public Map<String, Integer> importAll() {
        List<DatasetPayload> datasets = readDatasets();
        truncateAll();

        Context context = new Context();
        // 노드 코드는 파일 간에도 유일하므로 전체를 한 맵에 모은다. 타깃 해석이 파일 경계를 넘는다.
        for (DatasetPayload dataset : datasets) {
            importPlace(dataset, context);
        }
        linkParentNodes(context);
        for (DatasetPayload dataset : datasets) {
            importRoutes(dataset, context);
        }
        for (DatasetPayload dataset : datasets) {
            importReportsAndKnowledge(dataset, context);
        }
        for (DatasetPayload dataset : datasets) {
            importRagQueries(dataset, context);
        }
        insertSeedUsers();
        insertSeedJobs(context);

        Map<String, Integer> counts = countRows();
        log.info("dataset import finished: {}", counts);
        return counts;
    }

    private List<DatasetPayload> readDatasets() {
        List<DatasetPayload> datasets = new ArrayList<>();
        for (String fileName : DATASET_FILES) {
            Path path = datasetDir.resolve(fileName);
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("데이터셋 파일이 없다: " + path);
            }
            try {
                datasets.add(objectMapper.readValue(path.toFile(), DatasetPayload.class));
            } catch (IOException exception) {
                throw new IllegalStateException("데이터셋을 읽지 못했다: " + path, exception);
            }
        }
        return datasets;
    }

    private void truncateAll() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        try {
            TABLES_CHILD_FIRST.forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE " + table));
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private void importPlace(DatasetPayload dataset, Context context) {
        DatasetPayload.Place place = dataset.place();
        String code = place.placeCode();
        EnumAllowList.require(place.placeType(), EnumAllowList.PLACE_TYPE, "place_type", code);

        long placeId = insert(
                "INSERT INTO places (place_code, name, place_type, custom_place_type, description, synthetic)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                code, place.name(), place.placeType(), place.customPlaceType(),
                place.description(), Boolean.TRUE.equals(place.synthetic()));
        context.placeIds.put(code, placeId);

        for (DatasetPayload.Node node : dataset.nodes()) {
            EnumAllowList.require(node.nodeType(), EnumAllowList.NODE_TYPE, "node_type", node.nodeCode());
            long nodeId = insert(
                    "INSERT INTO place_nodes (place_id, node_code, node_type, custom_node_type,"
                            + " name, floor_label, is_indoor, description)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    placeId, node.nodeCode(), node.nodeType(), node.customNodeType(),
                    node.name(), node.floorLabel(), Boolean.TRUE.equals(node.isIndoor()),
                    node.description());
            context.nodeIds.put(node.nodeCode(), nodeId);
            if (node.parentNodeCode() != null) {
                context.nodeParents.put(node.nodeCode(), node.parentNodeCode());
            }
        }
    }

    /** 자기 참조라 전체 삽입 후 2회차에 채운다 (05A §3-2). */
    private void linkParentNodes(Context context) {
        context.nodeParents.forEach((childCode, parentCode) -> jdbcTemplate.update(
                "UPDATE place_nodes SET parent_node_id = ? WHERE node_code = ?",
                context.nodeId(parentCode), childCode));
    }

    private void importRoutes(DatasetPayload dataset, Context context) {
        long placeId = context.placeIds.get(dataset.place().placeCode());
        for (DatasetPayload.Route route : dataset.routes()) {
            EnumAllowList.require(
                    route.vehicleClass(), EnumAllowList.VEHICLE_CLASS, "vehicle_class", route.routeCode());
            long routeId = insert(
                    "INSERT INTO routes (place_id, route_code, name, start_node_id, destination_node_id,"
                            + " vehicle_class, min_tonnage, max_tonnage, max_vehicle_height_m,"
                            + " max_vehicle_width_m, is_default)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    placeId, route.routeCode(), route.name(),
                    context.nodeId(route.startNodeCode()), context.nodeId(route.destinationNodeCode()),
                    route.vehicleClass(), route.minTonnage(), route.maxTonnage(),
                    route.maxVehicleHeightM(), route.maxVehicleWidthM(),
                    Boolean.TRUE.equals(route.isDefault()));
            context.routeIds.put(route.routeCode(), routeId);
        }
        for (DatasetPayload.Segment segment : dataset.routeSegments()) {
            String code = segment.segmentCode();
            EnumAllowList.require(segment.movementMode(), EnumAllowList.MOVEMENT_MODE, "movement_mode", code);
            EnumAllowList.require(
                    segment.traversalMethod(), EnumAllowList.TRAVERSAL_METHOD, "traversal_method", code);
            long segmentId = insert(
                    "INSERT INTO route_segments (route_id, segment_code, sequence_no, from_node_id,"
                            + " to_node_id, movement_mode, traversal_method, custom_traversal_method,"
                            + " instruction, is_indoor)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    context.routeIds.get(segment.routeCode()), code, segment.sequenceNo(),
                    context.nodeId(segment.fromNodeCode()), context.nodeId(segment.toNodeCode()),
                    segment.movementMode(), segment.traversalMethod(), segment.customTraversalMethod(),
                    segment.instruction(), Boolean.TRUE.equals(segment.isIndoor()));
            context.segmentIds.put(code, segmentId);
        }
    }

    private void importReportsAndKnowledge(DatasetPayload dataset, Context context) {
        long placeId = context.placeIds.get(dataset.place().placeCode());
        // 시드 지식은 검수된 것으로 보고 과거 시각에 발행한다. 그래야 당일 승인 건만
        // "최근 승인" 가산점(04 §6-3)을 받는다.
        LocalDateTime publishedAt = LocalDateTime.now().minusDays(30);

        for (DatasetPayload.Report report : dataset.fieldReports()) {
            EnumAllowList.require(
                    report.sourceType(), EnumAllowList.SOURCE_TYPE, "source_type", report.reportCode());
            long reportId = insert(
                    "INSERT INTO field_reports (report_code, place_id, selected_scope_node_id, source_type,"
                            + " raw_stt_text, corrected_stt_text, status, audio_recording_candidate)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    report.reportCode(), placeId,
                    report.selectedScopeNodeCode() == null
                            ? null : context.nodeId(report.selectedScopeNodeCode()),
                    report.sourceType(), null, report.transcript(), "EXTRACTED",
                    Boolean.TRUE.equals(report.audioRecordingCandidate()));

            for (DatasetPayload.Knowledge knowledge : report.expectedKnowledgeItems()) {
                insertKnowledge(knowledge, placeId, reportId, publishedAt, context);
            }
        }
    }

    private void insertKnowledge(
            DatasetPayload.Knowledge knowledge,
            long placeId,
            long reportId,
            LocalDateTime publishedAt,
            Context context) {
        String code = knowledge.knowledgeCode();
        EnumAllowList.require(knowledge.category(), EnumAllowList.CATEGORY, "category", code);
        EnumAllowList.require(knowledge.factType(), EnumAllowList.FACT_TYPE, "fact_type", code);
        EnumAllowList.require(knowledge.movementMode(), EnumAllowList.MOVEMENT_MODE, "movement_mode", code);
        EnumAllowList.require(
                knowledge.traversalMethod(), EnumAllowList.TRAVERSAL_METHOD, "traversal_method", code);
        EnumAllowList.require(knowledge.accessState(), EnumAllowList.ACCESS_STATE, "access_state", code);
        EnumAllowList.require(knowledge.usageScope(), EnumAllowList.USAGE_SCOPE, "usage_scope", code);

        long knowledgeId = insert(
                "INSERT INTO knowledge_items (knowledge_code, place_id, source_report_id, category,"
                        + " custom_category_label, fact_type, custom_fact_type_label, movement_mode,"
                        + " traversal_method, custom_traversal_method, access_state, statement,"
                        + " action_text, source_excerpt, usage_scope, status, published_at)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                code, placeId, reportId, knowledge.category(), knowledge.customCategoryLabel(),
                knowledge.factType(), knowledge.customFactTypeLabel(), knowledge.movementMode(),
                knowledge.traversalMethod(), knowledge.customTraversalMethod(), knowledge.accessState(),
                knowledge.statement(), knowledge.actionText(), knowledge.sourceExcerpt(),
                knowledge.usageScope(), "PUBLISHED", publishedAt);

        insertConditions(knowledge, knowledgeId);
        insertTarget(knowledge, knowledgeId, context);
    }

    private void insertConditions(DatasetPayload.Knowledge knowledge, long knowledgeId) {
        DatasetPayload.Conditions conditions = knowledge.conditions();
        if (conditions == null) {
            return;
        }
        EnumAllowList.require(
                conditions.vehicleClass(), EnumAllowList.VEHICLE_CLASS, "conditions.vehicle_class",
                knowledge.knowledgeCode());

        jdbcTemplate.update(
                "INSERT INTO knowledge_conditions (knowledge_id, vehicle_class, min_tonnage,"
                        + " min_tonnage_inclusive, max_tonnage, max_tonnage_inclusive,"
                        + " max_vehicle_height_m, max_vehicle_width_m, active_time_start,"
                        + " active_time_end, active_days, extra_condition_text)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                knowledgeId, conditions.vehicleClass(),
                conditions.minTonnage(),
                TonnageBoundaryRule.inclusive(conditions.minTonnage(), knowledge.statement()),
                conditions.maxTonnage(),
                TonnageBoundaryRule.inclusive(conditions.maxTonnage(), knowledge.statement()),
                conditions.maxVehicleHeightM(), conditions.maxVehicleWidthM(),
                toTime(conditions.activeTimeStart()), toTime(conditions.activeTimeEnd()),
                toCsv(conditions.activeDays()), conditions.extraConditionText());
    }

    private void insertTarget(DatasetPayload.Knowledge knowledge, long knowledgeId, Context context) {
        DatasetPayload.Target target = knowledge.target();
        String code = knowledge.knowledgeCode();
        EnumAllowList.require(target.targetType(), EnumAllowList.TARGET_TYPE, "target_type", code);
        EnumAllowList.require(
                target.targetResolutionStatus(), EnumAllowList.TARGET_RESOLUTION_STATUS,
                "target_resolution_status", code);

        Long nodeId = null;
        Long segmentId = null;
        if ("NODE".equals(target.targetType())) {
            nodeId = context.nodeId(target.targetCode());
        } else if ("SEGMENT".equals(target.targetType())) {
            segmentId = context.segmentIds.get(target.targetCode());
            if (segmentId == null) {
                throw new IllegalStateException(code + ": 없는 구간 코드 " + target.targetCode());
            }
        }
        // UNKNOWN 은 비슷한 노드에 억지로 붙이지 않고 원문 표현을 그대로 보존한다 (절대 규칙 8).
        jdbcTemplate.update(
                "INSERT INTO knowledge_targets (knowledge_id, target_type, target_node_id,"
                        + " target_segment_id, target_resolution_status, target_free_text)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                knowledgeId, target.targetType(), nodeId, segmentId,
                target.targetResolutionStatus(), target.targetFreeText());
    }

    private void importRagQueries(DatasetPayload dataset, Context context) {
        long placeId = context.placeIds.get(dataset.place().placeCode());
        for (DatasetPayload.RagQuery query : dataset.ragTestQueries()) {
            jdbcTemplate.update(
                    "INSERT INTO rag_test_queries (query_code, place_id, question, context_json,"
                            + " expected_codes, must_not_return_codes, reason)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                    query.queryCode(), placeId, query.question(), toJson(query.context()),
                    toCsv(query.expectedKnowledgeCodes()), toCsv(query.mustNotReturnCodes()),
                    query.reason());
        }
    }

    /** 인증은 구현하지 않는다. 화면 토글용 시드 2명만 둔다 (05A §2-4). */
    private void insertSeedUsers() {
        jdbcTemplate.update(
                "INSERT INTO users (login_id, display_name, role) VALUES (?, ?, ?)",
                "driver01", "김기사", "DRIVER");
        jdbcTemplate.update(
                "INSERT INTO users (login_id, display_name, role) VALUES (?, ?, ?)",
                "admin01", "관리자", "ADMIN");
    }

    private void insertSeedJobs(Context context) {
        for (SeedJob job : SEED_JOBS) {
            Long destinationNodeId = context.nodeIds.get(job.destinationNodeCode());
            if (destinationNodeId == null) {
                // 목적지가 없으면 경로 후보가 0개가 되어 NO_ROUTE_AVAILABLE 이 난다. 조용히 넘기지 않는다.
                throw new IllegalStateException(
                        job.jobCode() + ": 목적지 노드 " + job.destinationNodeCode() + " 가 없다.");
            }
            Long placeId = jdbcTemplate.queryForObject(
                    "SELECT place_id FROM place_nodes WHERE id = ?", Long.class, destinationNodeId);
            jdbcTemplate.update(
                    "INSERT INTO delivery_jobs (job_code, place_id, destination_node_id,"
                            + " recipient_label, address_text, item_summary, status)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                    job.jobCode(), placeId, destinationNodeId, job.recipientLabel(),
                    placeName(placeId), job.itemSummary(), "READY");
        }
    }

    private String placeName(Long placeId) {
        return jdbcTemplate.queryForObject("SELECT name FROM places WHERE id = ?", String.class, placeId);
    }

    public Map<String, Integer> countRows() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        List.of("places", "place_nodes", "routes", "route_segments", "field_reports",
                        "knowledge_items", "knowledge_conditions", "knowledge_targets",
                        "rag_test_queries", "delivery_jobs", "users")
                .forEach(table -> counts.put(
                        table, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)));
        return counts;
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
            throw new IllegalStateException("생성된 키를 받지 못했다: " + sql);
        }
        return key.longValue();
    }

    private static Time toTime(String value) {
        return value == null || value.isBlank() ? null : Time.valueOf(LocalTime.parse(value));
    }

    private static String toCsv(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (IOException exception) {
            throw new IllegalStateException("JSON 직렬화 실패", exception);
        }
    }

    private static final class Context {
        private final Map<String, Long> placeIds = new LinkedHashMap<>();
        private final Map<String, Long> nodeIds = new LinkedHashMap<>();
        private final Map<String, String> nodeParents = new LinkedHashMap<>();
        private final Map<String, Long> routeIds = new LinkedHashMap<>();
        private final Map<String, Long> segmentIds = new LinkedHashMap<>();

        private long nodeId(String code) {
            Long id = nodeIds.get(code);
            if (id == null) {
                throw new IllegalStateException("없는 노드 코드: " + code);
            }
            return id;
        }
    }

    private record SeedJob(
            String jobCode, String destinationNodeCode, String recipientLabel, String itemSummary) {}
}
