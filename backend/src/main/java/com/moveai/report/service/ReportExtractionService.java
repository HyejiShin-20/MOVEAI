package com.moveai.report.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.moveai.ai.extraction.ExtractionClient;
import com.moveai.common.ApiException;
import com.moveai.place.entity.Place;
import com.moveai.place.entity.PlaceNode;
import com.moveai.place.repository.PlaceNodeRepository;
import com.moveai.place.repository.PlaceRepository;
import com.moveai.report.dto.ExtractResponse;
import com.moveai.report.entity.FieldReport;
import com.moveai.report.repository.FieldReportRepository;
import com.moveai.moderation.entity.KnowledgeDraft;
import com.moveai.moderation.repository.KnowledgeDraftRepository;
import com.moveai.route.entity.Route;
import com.moveai.route.entity.RouteSegment;
import com.moveai.route.repository.RouteRepository;
import com.moveai.route.repository.RouteSegmentRepository;

/**
 * {@code corrected_stt_text} → Draft 생성 (05B §4-5).
 *
 * <p>LLM 호출은 DB 트랜잭션 밖에서 끝내고, 저장만 트랜잭션으로 묶는다.
 */
@Service
public class ReportExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ReportExtractionService.class);

    private final FieldReportRepository fieldReportRepository;
    private final KnowledgeDraftRepository knowledgeDraftRepository;
    private final PlaceRepository placeRepository;
    private final PlaceNodeRepository placeNodeRepository;
    private final RouteRepository routeRepository;
    private final RouteSegmentRepository routeSegmentRepository;
    private final ExtractionClient extractionClient;
    private final TransactionTemplate transactionTemplate;

    public ReportExtractionService(
            FieldReportRepository fieldReportRepository,
            KnowledgeDraftRepository knowledgeDraftRepository,
            PlaceRepository placeRepository,
            PlaceNodeRepository placeNodeRepository,
            RouteRepository routeRepository,
            RouteSegmentRepository routeSegmentRepository,
            ExtractionClient extractionClient,
            TransactionTemplate transactionTemplate) {
        this.fieldReportRepository = fieldReportRepository;
        this.knowledgeDraftRepository = knowledgeDraftRepository;
        this.placeRepository = placeRepository;
        this.placeNodeRepository = placeNodeRepository;
        this.routeRepository = routeRepository;
        this.routeSegmentRepository = routeSegmentRepository;
        this.extractionClient = extractionClient;
        this.transactionTemplate = transactionTemplate;
    }

    public ExtractResponse extract(Long reportId) {
        FieldReport report = fieldReportRepository.findById(reportId)
                .orElseThrow(() -> ApiException.notFound("REPORT_NOT_FOUND", "제보를 찾을 수 없습니다."));
        Place place = placeRepository.findById(report.getPlaceId())
                .orElseThrow(() -> ApiException.notFound("PLACE_NOT_FOUND", "장소를 찾을 수 없습니다."));

        List<PlaceNode> nodes = placeNodeRepository.findByPlaceIdOrderByNodeCodeAsc(place.getId());
        List<ExtractionClient.KnownLocation> knownNodes = nodes.stream()
                .map(node -> new ExtractionClient.KnownLocation(node.getNodeCode(), node.getName()))
                .toList();
        List<ExtractionClient.KnownLocation> knownSegments = knownSegments(place.getId());
        String scopeNodeName = nodes.stream()
                .filter(node -> node.getId().equals(report.getSelectedScopeNodeId()))
                .map(PlaceNode::getName)
                .findFirst()
                .orElse(null);

        List<JsonNode> items;
        try {
            items = extractionClient.extract(
                    place.getName(), report.getCorrectedSttText(), scopeNodeName,
                    knownNodes, knownSegments);
        } catch (ApiException exception) {
            // 실패를 조용히 삼키지 않는다. 상태로 남겨 사람이 본다.
            transactionTemplate.executeWithoutResult(status -> markStatus(reportId, "EXTRACTION_FAILED"));
            return ExtractResponse.failed(reportId, exception.getMessage());
        }

        return transactionTemplate.execute(status -> save(reportId, items));
    }

    private ExtractResponse save(Long reportId, List<JsonNode> items) {
        // 재추출하면 이전 초안은 버린다. 같은 제보에 초안이 겹겹이 쌓이면 검수 화면이 뒤엉킨다.
        knowledgeDraftRepository.deleteByReportId(reportId);

        List<ExtractResponse.Draft> drafts = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            JsonNode item = items.get(index);
            KnowledgeDraft draft =
                    knowledgeDraftRepository.save(new KnowledgeDraft(reportId, index, item.toString()));
            drafts.add(new ExtractResponse.Draft(draft.getId(), draft.getDraftIndex(), item));
        }
        markStatus(reportId, "EXTRACTED");
        log.info("extraction saved: reportId={} drafts={}", reportId, drafts.size());
        return ExtractResponse.extracted(reportId, drafts);
    }

    private void markStatus(Long reportId, String status) {
        fieldReportRepository.findById(reportId).ifPresent(report -> report.markStatus(status));
    }

    /** 구간에는 이름이 없으므로 "출발 → 도착"으로 만든다 (05B §5-2 의 knownSegments 형식). */
    private List<ExtractionClient.KnownLocation> knownSegments(Long placeId) {
        List<ExtractionClient.KnownLocation> segments = new ArrayList<>();
        for (Route route : routeRepository.findByPlaceIdOrderByRouteCodeAsc(placeId)) {
            for (RouteSegment segment : routeSegmentRepository.findByRouteIdOrderBySequence(route.getId())) {
                segments.add(new ExtractionClient.KnownLocation(
                        segment.getSegmentCode(),
                        segment.getFromNode().getName() + " → " + segment.getToNode().getName()));
            }
        }
        return segments;
    }
}
