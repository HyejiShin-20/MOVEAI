package com.moveai.moderation.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveai.ai.embedding.EmbeddingClient;
import com.moveai.common.ApiException;
import com.moveai.knowledge.embedding.EmbeddingTextBuilder;
import com.moveai.moderation.dto.ModerationDto;
import com.moveai.moderation.entity.KnowledgeDraft;
import com.moveai.moderation.repository.KnowledgeDraftRepository;

/**
 * 검수와 발행 (05B §4-6).
 *
 * <p>승인 HTTP 요청은 동기지만 <b>{@code /embed} 호출은 DB 트랜잭션 밖에서</b> 한다.
 * 느린 외부 호출 중에 트랜잭션을 열어두면 커넥션을 잡은 채 수 초를 기다린다.
 */
@Service
public class ModerationService {

    private static final Logger log = LoggerFactory.getLogger(ModerationService.class);
    private static final int SUMMARY_LENGTH = 60;

    private final KnowledgeDraftRepository draftRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingClient embeddingClient;
    private final EmbeddingTextBuilder embeddingTextBuilder;
    private final KnowledgePublisher knowledgePublisher;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String embeddingModel;

    public ModerationService(
            KnowledgeDraftRepository draftRepository,
            JdbcTemplate jdbcTemplate,
            EmbeddingClient embeddingClient,
            EmbeddingTextBuilder embeddingTextBuilder,
            KnowledgePublisher knowledgePublisher,
            TransactionTemplate transactionTemplate,
            @Value("${moveai.embedding.model}") String embeddingModel) {
        this.draftRepository = draftRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingClient = embeddingClient;
        this.embeddingTextBuilder = embeddingTextBuilder;
        this.knowledgePublisher = knowledgePublisher;
        this.transactionTemplate = transactionTemplate;
        this.embeddingModel = embeddingModel;
    }

    public List<ModerationDto.DraftSummary> findDrafts(String status) {
        String resolved = status == null || status.isBlank() ? "PENDING" : status;
        return jdbcTemplate.query(
                "SELECT d.id, d.report_id, p.name AS place_name, d.created_at, d.payload_json"
                        + " FROM knowledge_drafts d"
                        + " JOIN field_reports r ON r.id = d.report_id"
                        + " JOIN places p ON p.id = r.place_id"
                        + " WHERE d.status = ? ORDER BY d.id ASC",
                (rs, rowNum) -> new ModerationDto.DraftSummary(
                        rs.getLong("id"),
                        rs.getLong("report_id"),
                        rs.getString("place_name"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        summarize(rs.getString("payload_json"))),
                resolved);
    }

    public ModerationDto.DraftDetail findDraft(Long draftId) {
        KnowledgeDraft draft = draft(draftId);
        Map<String, Object> report = jdbcTemplate.queryForMap(
                "SELECT r.id, r.raw_stt_text, r.corrected_stt_text, p.name AS place_name,"
                        + " n.name AS scope_node_name,"
                        + " (SELECT a.file_path FROM report_audio_files a WHERE a.report_id = r.id"
                        + "   ORDER BY a.id DESC LIMIT 1) AS audio_path"
                        + " FROM field_reports r"
                        + " JOIN places p ON p.id = r.place_id"
                        + " LEFT JOIN place_nodes n ON n.id = r.selected_scope_node_id"
                        + " WHERE r.id = ?",
                draft.getReportId());

        JsonNode payload = readPayload(draft.getPayloadJson());
        return new ModerationDto.DraftDetail(
                draft.getId(),
                draft.getStatus(),
                new ModerationDto.Report(
                        ((Number) report.get("id")).longValue(),
                        (String) report.get("audio_path"),
                        (String) report.get("raw_stt_text"),
                        (String) report.get("corrected_stt_text"),
                        (String) report.get("place_name"),
                        (String) report.get("scope_node_name")),
                payload,
                resolvedTargetName(payload));
    }

    /**
     * 1. PENDING 확인 → 2. embedding_text 생성 → 3. /embed (트랜잭션 밖) → 4. 저장 (한 트랜잭션).
     *
     * <p>{@code /embed} 가 실패하면 DB 는 손대지 않고 명시적 오류를 낸다. 화면은 재시도만 하면 된다.
     */
    public ModerationDto.ApproveResult approve(Long draftId, JsonNode editedPayload) {
        KnowledgeDraft draft = draft(draftId);
        if (!draft.isPending()) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "DRAFT_NOT_PENDING", "이미 처리된 초안입니다.");
        }
        boolean edited = editedPayload != null && !editedPayload.isNull() && !editedPayload.isEmpty();
        JsonNode payload = edited ? editedPayload : readPayload(draft.getPayloadJson());

        Context context = loadContext(draft.getReportId(), payload);
        String embeddingText = embeddingTextBuilder.build(context.source());

        double[] vector = embed(embeddingText);

        Long knowledgeId = transactionTemplate.execute(status -> {
            KnowledgeDraft locked = draft(draftId);
            if (!locked.isPending()) {
                // 중복 클릭 방어. 같은 초안이 두 번 발행되면 검색에 쌍둥이 카드가 뜬다.
                throw new ApiException(HttpStatus.CONFLICT, "DRAFT_NOT_PENDING", "이미 처리된 초안입니다.");
            }
            long id = knowledgePublisher.publish(new KnowledgePublisher.PublishCommand(
                    draftId, locked.getReportId(), context.placeId(),
                    context.nodeId(), context.segmentId(),
                    payload, edited, embeddingText, embeddingModel, vector));
            locked.markApproved();
            return id;
        });

        log.info("draft approved: draftId={} knowledgeId={} edited={}", draftId, knowledgeId, edited);
        return new ModerationDto.ApproveResult(draftId, knowledgeId, true);
    }

    @Transactional
    public ModerationDto.RejectResult reject(Long draftId, String reason) {
        KnowledgeDraft draft = draft(draftId);
        if (!draft.isPending()) {
            throw new ApiException(HttpStatus.CONFLICT, "DRAFT_NOT_PENDING", "이미 처리된 초안입니다.");
        }
        draft.markRejected();
        jdbcTemplate.update(
                "INSERT INTO moderation_reviews (draft_id, decision, reject_reason) VALUES (?, 'REJECT', ?)",
                draftId, reason);
        return new ModerationDto.RejectResult(draftId, "REJECTED");
    }

    private double[] embed(String embeddingText) {
        try {
            List<double[]> vectors = embeddingClient.embed(List.of(embeddingText));
            if (vectors.isEmpty() || vectors.get(0).length == 0) {
                throw new IllegalStateException("빈 벡터");
            }
            return vectors.get(0);
        } catch (Exception exception) {
            log.warn("embedding failed on approve: error_type={}", exception.getClass().getSimpleName());
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY, "EMBEDDING_FAILED",
                    "임베딩 생성에 실패했습니다. 저장하지 않았습니다. 다시 시도해 주세요.");
        }
    }

    /** 지식 텍스트에 들어갈 이름들을 DB에서 확정한다. 여기서 못 찾으면 발행하지 않는다. */
    private Context loadContext(Long reportId, JsonNode payload) {
        Map<String, Object> report = jdbcTemplate.queryForMap(
                "SELECT r.place_id, p.name AS place_name FROM field_reports r"
                        + " JOIN places p ON p.id = r.place_id WHERE r.id = ?",
                reportId);
        long placeId = ((Number) report.get("place_id")).longValue();
        String placeName = (String) report.get("place_name");

        JsonNode target = payload.get("target");
        String targetType = target.get("target_type").asText();
        String targetCode = target.hasNonNull("target_code") ? target.get("target_code").asText() : null;

        Long nodeId = null;
        Long segmentId = null;
        String nodeName = null;
        String fromNodeName = null;
        String toNodeName = null;

        if ("NODE".equals(targetType)) {
            Map<String, Object> node = queryOne(
                    "SELECT id, name FROM place_nodes WHERE node_code = ?", targetCode,
                    "TARGET_NOT_FOUND", "노드 " + targetCode + " 를 찾을 수 없습니다.");
            nodeId = ((Number) node.get("id")).longValue();
            nodeName = (String) node.get("name");
        } else if ("SEGMENT".equals(targetType)) {
            Map<String, Object> segment = queryOne(
                    "SELECT s.id, f.name AS from_name, t.name AS to_name FROM route_segments s"
                            + " JOIN place_nodes f ON f.id = s.from_node_id"
                            + " JOIN place_nodes t ON t.id = s.to_node_id"
                            + " WHERE s.segment_code = ?",
                    targetCode, "TARGET_NOT_FOUND", "구간 " + targetCode + " 를 찾을 수 없습니다.");
            segmentId = ((Number) segment.get("id")).longValue();
            fromNodeName = (String) segment.get("from_name");
            toNodeName = (String) segment.get("to_name");
        }

        EmbeddingTextBuilder.Source source = new EmbeddingTextBuilder.Source(
                null, targetType,
                target.hasNonNull("target_free_text") ? target.get("target_free_text").asText() : null,
                placeName, nodeName, fromNodeName, toNodeName,
                payload.get("movement_mode").asText(),
                asTextOrNull(payload, "traversal_method"),
                asTextOrNull(payload, "custom_traversal_method"),
                payload.get("statement").asText(),
                asTextOrNull(payload, "action_text"));

        return new Context(placeId, nodeId, segmentId, source);
    }

    private Map<String, Object> queryOne(String sql, Object param, String code, String message) {
        try {
            return jdbcTemplate.queryForMap(sql, param);
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
        }
    }

    private KnowledgeDraft draft(Long draftId) {
        return draftRepository.findById(draftId)
                .orElseThrow(() -> ApiException.notFound("DRAFT_NOT_FOUND", "초안을 찾을 수 없습니다."));
    }

    private JsonNode readPayload(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PAYLOAD", "초안 내용을 읽을 수 없습니다.");
        }
    }

    private String resolvedTargetName(JsonNode payload) {
        JsonNode target = payload.get("target");
        if (target == null) {
            return null;
        }
        String targetCode = target.hasNonNull("target_code") ? target.get("target_code").asText() : null;
        if (targetCode == null) {
            // UNKNOWN 은 "확인 필요"로 보이도록 기사가 말한 표현을 그대로 넘긴다.
            return target.hasNonNull("target_free_text") ? target.get("target_free_text").asText() : null;
        }
        List<String> names = jdbcTemplate.queryForList(
                "SELECT name FROM place_nodes WHERE node_code = ?", String.class, targetCode);
        return names.isEmpty() ? targetCode : names.get(0);
    }

    private static String asTextOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private static String summarize(String payloadJson) {
        try {
            JsonNode statement = new ObjectMapper().readTree(payloadJson).get("statement");
            String text = statement == null ? "" : statement.asText();
            return text.length() <= SUMMARY_LENGTH ? text : text.substring(0, SUMMARY_LENGTH) + "…";
        } catch (JsonProcessingException exception) {
            return "";
        }
    }

    private record Context(
            Long placeId, Long nodeId, Long segmentId, EmbeddingTextBuilder.Source source) {}
}
