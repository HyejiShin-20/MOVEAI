package com.moveai.knowledge.embedding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Python이 생성한 146건 산출물을 검증한 뒤 MariaDB에 원자적으로 적재한다. */
@Service
public class KnowledgeEmbeddingImportService {

    private static final String SOURCE_SQL = """
            SELECT ki.knowledge_code, kt.target_type, kt.target_free_text,
                   p.name AS place_name, n.name AS node_name,
                   fn.name AS from_node_name, tn.name AS to_node_name,
                   ki.movement_mode, ki.traversal_method, ki.custom_traversal_method,
                   ki.statement, ki.action_text
              FROM knowledge_items ki
              JOIN places p ON p.id = ki.place_id
              JOIN knowledge_targets kt ON kt.knowledge_id = ki.id
              LEFT JOIN place_nodes n ON n.id = kt.target_node_id
              LEFT JOIN route_segments rs ON rs.id = kt.target_segment_id
              LEFT JOIN place_nodes fn ON fn.id = rs.from_node_id
              LEFT JOIN place_nodes tn ON tn.id = rs.to_node_id
             WHERE ki.status = 'PUBLISHED'
             ORDER BY ki.knowledge_code
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final EmbeddingTextBuilder textBuilder;
    private final Path artifactPath;
    private final String expectedModel;
    private final int expectedDimension;

    public KnowledgeEmbeddingImportService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            EmbeddingTextBuilder textBuilder,
            @Value("${moveai.embedding.artifact:${EMBEDDING_ARTIFACT:../data/embeddings/knowledge_embeddings.json}}")
                    String artifactPath,
            @Value("${moveai.embedding.model:${EMBEDDING_MODEL:gemini-embedding-2}}")
                    String expectedModel,
            @Value("${moveai.embedding.dimension:${EMBEDDING_DIMENSION:1536}}") int expectedDimension) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.textBuilder = textBuilder;
        this.artifactPath = Path.of(artifactPath).toAbsolutePath().normalize();
        this.expectedModel = expectedModel;
        this.expectedDimension = expectedDimension;
    }

    @Transactional
    public int importAll() {
        Artifact artifact = readArtifact();
        Map<String, EmbeddingTextBuilder.Source> sources = loadSources();
        validateArtifact(artifact, sources);

        jdbcTemplate.update("DELETE FROM knowledge_embeddings");
        for (ArtifactItem item : artifact.items()) {
            jdbcTemplate.update(
                    """
                    INSERT INTO knowledge_embeddings
                      (knowledge_id, embedding_model, embedding_dimension, embedding_text, embedding_json)
                    SELECT id, ?, ?, ?, ? FROM knowledge_items
                     WHERE knowledge_code = ? AND status = 'PUBLISHED'
                    """,
                    item.embeddingModel(), item.embeddingDimension(), item.embeddingText(),
                    item.embeddingJson(), item.knowledgeCode());
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM knowledge_embeddings", Integer.class);
        if (count == null || count != sources.size()) {
            throw new IllegalStateException("임베딩 적재 건수가 PUBLISHED 지식 건수와 다릅니다.");
        }
        return count;
    }

    private Artifact readArtifact() {
        if (!Files.isRegularFile(artifactPath)) {
            throw new IllegalStateException("임베딩 산출물 파일이 없습니다: " + artifactPath);
        }
        try {
            return objectMapper.readValue(artifactPath.toFile(), Artifact.class);
        } catch (IOException exception) {
            throw new IllegalStateException("임베딩 산출물을 읽지 못했습니다: " + artifactPath, exception);
        }
    }

    private Map<String, EmbeddingTextBuilder.Source> loadSources() {
        Map<String, EmbeddingTextBuilder.Source> sources = new LinkedHashMap<>();
        jdbcTemplate.query(SOURCE_SQL, resultSet -> {
            EmbeddingTextBuilder.Source source = new EmbeddingTextBuilder.Source(
                    resultSet.getString("knowledge_code"), resultSet.getString("target_type"),
                    resultSet.getString("target_free_text"), resultSet.getString("place_name"),
                    resultSet.getString("node_name"), resultSet.getString("from_node_name"),
                    resultSet.getString("to_node_name"), resultSet.getString("movement_mode"),
                    resultSet.getString("traversal_method"),
                    resultSet.getString("custom_traversal_method"), resultSet.getString("statement"),
                    resultSet.getString("action_text"));
            sources.put(source.knowledgeCode(), source);
        });
        return sources;
    }

    private void validateArtifact(
            Artifact artifact, Map<String, EmbeddingTextBuilder.Source> sources) {
        if (!expectedModel.equals(artifact.embeddingModel())
                || artifact.embeddingDimension() != expectedDimension) {
            throw new IllegalStateException("임베딩 산출물의 모델 또는 차원이 환경설정과 다릅니다.");
        }
        if (artifact.count() != artifact.items().size() || artifact.items().size() != sources.size()) {
            throw new IllegalStateException("임베딩 산출물 건수가 PUBLISHED 지식 건수와 다릅니다.");
        }

        Set<String> seenCodes = new HashSet<>();
        Map<String, String> failures = new HashMap<>();
        for (ArtifactItem item : artifact.items()) {
            if (!seenCodes.add(item.knowledgeCode())) {
                failures.put(item.knowledgeCode(), "중복 코드");
                continue;
            }
            EmbeddingTextBuilder.Source source = sources.get(item.knowledgeCode());
            if (source == null) {
                failures.put(item.knowledgeCode(), "PUBLISHED 지식 없음");
                continue;
            }
            if (!expectedModel.equals(item.embeddingModel())
                    || item.embeddingDimension() != expectedDimension) {
                failures.put(item.knowledgeCode(), "모델 또는 차원 불일치");
                continue;
            }
            String expectedText = textBuilder.build(source);
            if (!expectedText.equals(item.embeddingText())) {
                failures.put(item.knowledgeCode(), "Spring/Python embedding_text 불일치");
                continue;
            }
            try {
                double[] vector = objectMapper.readValue(item.embeddingJson(), double[].class);
                if (vector.length != expectedDimension) {
                    failures.put(item.knowledgeCode(), "벡터 차원 불일치: " + vector.length);
                }
            } catch (IOException exception) {
                failures.put(item.knowledgeCode(), "embeddingJson 파싱 실패");
            }
        }
        if (!seenCodes.equals(sources.keySet())) {
            Set<String> missing = new HashSet<>(sources.keySet());
            missing.removeAll(seenCodes);
            failures.put("missing", missing.toString());
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("임베딩 산출물 검증 실패: " + failures);
        }
    }

    public record Artifact(
            String generatedAt,
            String embeddingModel,
            int embeddingDimension,
            int count,
            List<ArtifactItem> items) {
    }

    public record ArtifactItem(
            String knowledgeCode,
            String placeCode,
            String embeddingModel,
            int embeddingDimension,
            String embeddingText,
            String embeddingJson) {
    }
}
