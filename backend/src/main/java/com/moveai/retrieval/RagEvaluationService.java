package com.moveai.retrieval;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moveai.ai.embedding.EmbeddingClient;

/** 데이터셋의 정답 질문 20개로 Hit@3/Hit@5와 must-not 위반을 계산한다. */
@Service
public class RagEvaluationService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final KnowledgeCandidateRepository candidateRepository;
    private final EmbeddingClient embeddingClient;
    private final QueryTextBuilder queryTextBuilder;
    private final ConditionEvaluator conditionEvaluator;
    private final RankingService rankingService;

    public RagEvaluationService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            KnowledgeCandidateRepository candidateRepository,
            EmbeddingClient embeddingClient,
            QueryTextBuilder queryTextBuilder,
            ConditionEvaluator conditionEvaluator,
            RankingService rankingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.candidateRepository = candidateRepository;
        this.embeddingClient = embeddingClient;
        this.queryTextBuilder = queryTextBuilder;
        this.conditionEvaluator = conditionEvaluator;
        this.rankingService = rankingService;
    }

    public EvaluationReport evaluate() {
        List<GoldQuery> queries = loadQueries();
        List<String> texts = queries.stream()
                .map(query -> queryTextBuilder.buildEvaluationQuestion(
                        query.question(), stringValue(query.context(), "movement_mode")))
                .toList();
        List<double[]> queryVectors = embeddingClient.embed(texts);

        int hitAt3 = 0;
        int hitAt5 = 0;
        int mustNotViolations = 0;
        List<QueryResult> results = new java.util.ArrayList<>();
        for (int index = 0; index < queries.size(); index++) {
            GoldQuery query = queries.get(index);
            SearchContext context = toSearchContext(query.context());
            List<KnowledgeCandidate> filtered = candidateRepository.findPublishedByPlaceId(query.placeId())
                    .stream()
                    .filter(candidate -> conditionEvaluator.matches(candidate, context))
                    .toList();
            List<String> rankedCodes = rankingService.rankByCosine(filtered, queryVectors.get(index))
                    .stream().map(result -> result.candidate().knowledgeCode()).toList();
            List<String> top3 = rankedCodes.stream().limit(3).toList();
            List<String> top5 = rankedCodes.stream().limit(5).toList();
            boolean queryHit3 = intersects(top3, query.expectedCodes());
            boolean queryHit5 = intersects(top5, query.expectedCodes());
            Set<String> violations = intersection(top5, query.mustNotCodes());
            hitAt3 += queryHit3 ? 1 : 0;
            hitAt5 += queryHit5 ? 1 : 0;
            mustNotViolations += violations.size();
            results.add(new QueryResult(
                    query.queryCode(), queryHit3, queryHit5, top5,
                    query.expectedCodes(), List.copyOf(violations)));
        }
        return new EvaluationReport(queries.size(), hitAt3, hitAt5, mustNotViolations, results);
    }

    private List<GoldQuery> loadQueries() {
        return jdbcTemplate.query(
                "SELECT query_code, place_id, question, context_json, expected_codes,"
                        + " must_not_return_codes FROM rag_test_queries ORDER BY query_code",
                this::mapQuery);
    }

    private GoldQuery mapQuery(ResultSet rs, int rowNumber) throws SQLException {
        try {
            return new GoldQuery(
                    rs.getString("query_code"), rs.getLong("place_id"), rs.getString("question"),
                    objectMapper.readValue(rs.getString("context_json"), MAP_TYPE),
                    csv(rs.getString("expected_codes")), csv(rs.getString("must_not_return_codes")));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("RAG 평가 context_json 파싱 실패", exception);
        }
    }

    private SearchContext toSearchContext(Map<String, Object> context) {
        String time = stringValue(context, "current_time");
        String day = stringValue(context, "current_day");
        return new SearchContext(
                stringValue(context, "vehicle_class"),
                doubleValue(context, "vehicle_tonnage"),
                doubleValue(context, "vehicle_height_m"),
                doubleValue(context, "vehicle_width_m"),
                stringValue(context, "movement_mode"),
                time == null ? null : LocalTime.parse(time),
                day == null ? null : DayCode.parse(day));
    }

    private static String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }

    private static Double doubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private static List<String> csv(String value) {
        return value == null || value.isBlank()
                ? Collections.emptyList()
                : Arrays.stream(value.split(",")).map(String::strip).toList();
    }

    private static boolean intersects(List<String> actual, List<String> expected) {
        return actual.stream().anyMatch(expected::contains);
    }

    private static Set<String> intersection(List<String> actual, List<String> prohibited) {
        Set<String> result = new LinkedHashSet<>(actual);
        result.retainAll(prohibited);
        return result;
    }

    private record GoldQuery(
            String queryCode,
            long placeId,
            String question,
            Map<String, Object> context,
            List<String> expectedCodes,
            List<String> mustNotCodes) {
    }

    public record QueryResult(
            String queryCode,
            boolean hitAt3,
            boolean hitAt5,
            List<String> top5,
            List<String> expectedCodes,
            List<String> mustNotViolations) {
    }

    public record EvaluationReport(
            int queryCount,
            int hitAt3Count,
            int hitAt5Count,
            int mustNotViolationCount,
            List<QueryResult> results) {

        public double hitAt3() {
            return queryCount == 0 ? 0 : (double) hitAt3Count / queryCount;
        }

        public double hitAt5() {
            return queryCount == 0 ? 0 : (double) hitAt5Count / queryCount;
        }
    }
}
