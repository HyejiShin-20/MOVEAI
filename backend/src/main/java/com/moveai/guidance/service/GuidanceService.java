package com.moveai.guidance.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.moveai.ai.embedding.EmbeddingClient;
import com.moveai.common.ApiException;
import com.moveai.guidance.dto.GuidanceCompleteResponse;
import com.moveai.guidance.dto.GuidanceCreateRequest;
import com.moveai.guidance.dto.GuidanceSessionResponse;
import com.moveai.guidance.dto.GuidanceSessionResponse.GuidanceCardResponse;
import com.moveai.guidance.dto.GuidanceSessionResponse.GuidanceStepResponse;
import com.moveai.guidance.dto.GuidanceSessionResponse.RouteSummary;
import com.moveai.guidance.entity.GuidanceSession;
import com.moveai.guidance.repository.GuidanceSessionRepository;
import com.moveai.job.entity.DeliveryJob;
import com.moveai.job.repository.DeliveryJobRepository;
import com.moveai.retrieval.HybridSearchService;
import com.moveai.retrieval.KnowledgeCandidate;
import com.moveai.retrieval.KnowledgeCandidateRepository;
import com.moveai.retrieval.QueryTextBuilder;
import com.moveai.retrieval.RankingService;
import com.moveai.retrieval.SearchContext;
import com.moveai.retrieval.SegmentContext;
import com.moveai.route.entity.Route;
import com.moveai.route.entity.RouteSegment;
import com.moveai.route.repository.RouteRepository;
import com.moveai.route.repository.RouteSegmentRepository;
import com.moveai.route.service.RouteSelector;
import com.moveai.route.service.RouteSelector.RouteOption;
import com.moveai.route.service.VehicleContext;

/** Route 선택부터 단계별 카드 조립까지의 Phase 4 애플리케이션 서비스. */
@Service
public class GuidanceService {

    private final DeliveryJobRepository jobRepository;
    private final RouteRepository routeRepository;
    private final RouteSegmentRepository segmentRepository;
    private final GuidanceSessionRepository sessionRepository;
    private final KnowledgeCandidateRepository candidateRepository;
    private final RouteSelector routeSelector;
    private final QueryTextBuilder queryTextBuilder;
    private final EmbeddingClient embeddingClient;
    private final HybridSearchService hybridSearchService;
    private final GuidanceCardAssembler cardAssembler;
    private final TransactionTemplate transactionTemplate;

    public GuidanceService(
            DeliveryJobRepository jobRepository,
            RouteRepository routeRepository,
            RouteSegmentRepository segmentRepository,
            GuidanceSessionRepository sessionRepository,
            KnowledgeCandidateRepository candidateRepository,
            RouteSelector routeSelector,
            QueryTextBuilder queryTextBuilder,
            EmbeddingClient embeddingClient,
            HybridSearchService hybridSearchService,
            GuidanceCardAssembler cardAssembler,
            TransactionTemplate transactionTemplate) {
        this.jobRepository = jobRepository;
        this.routeRepository = routeRepository;
        this.segmentRepository = segmentRepository;
        this.sessionRepository = sessionRepository;
        this.candidateRepository = candidateRepository;
        this.routeSelector = routeSelector;
        this.queryTextBuilder = queryTextBuilder;
        this.embeddingClient = embeddingClient;
        this.hybridSearchService = hybridSearchService;
        this.cardAssembler = cardAssembler;
        this.transactionTemplate = transactionTemplate;
    }

    public GuidanceSessionResponse create(GuidanceCreateRequest request) {
        VehicleContext vehicle = new VehicleContext(
                request.vehicle().vehicleClass(), request.vehicle().tonnage(),
                request.vehicle().heightM(), request.vehicle().widthM());
        Long sessionId = transactionTemplate.execute(status -> {
            DeliveryJob job = findJob(request.deliveryJobId());
            List<Route> routes = routeRepository.findByPlaceIdAndDestinationNodeIdOrderByRouteCodeAsc(
                    job.getPlace().getId(), job.getDestinationNode().getId());
            RouteOption selected = routeSelector.select(routes.stream().map(this::toOption).toList(), vehicle)
                    .orElseThrow(() -> ApiException.notFound(
                            "NO_ROUTE_AVAILABLE", "이 차량으로 진입 가능한 등록된 경로가 없습니다."));

            sessionRepository.abandonActiveByJobId(job.getId());
            GuidanceSession session = GuidanceSession.start(
                    job.getId(), job.getPlace().getId(), selected.id(), vehicle,
                    request.contextTime() == null ? LocalDateTime.now() : request.contextTime());
            job.markInProgress();
            jobRepository.save(job);
            return sessionRepository.save(session).getId();
        });
        return get(requireTransactionResult(sessionId));
    }

    public GuidanceSessionResponse get(long sessionId) {
        return buildResponse(findSession(sessionId));
    }

    public GuidanceSessionResponse next(long sessionId) {
        Long updatedId = transactionTemplate.execute(status -> {
            GuidanceSession session = findSession(sessionId);
            requireActive(session);
            int totalSteps = segmentRepository.countByRouteId(session.getRouteId());
            session.advance(totalSteps);
            return sessionRepository.save(session).getId();
        });
        return get(requireTransactionResult(updatedId));
    }

    public GuidanceCompleteResponse complete(long sessionId) {
        GuidanceCompleteResponse response = transactionTemplate.execute(status -> {
            GuidanceSession session = findSession(sessionId);
            if ("ABANDONED".equals(session.getStatus())) {
                throw new ApiException(
                        HttpStatus.CONFLICT, "GUIDANCE_SESSION_NOT_ACTIVE", "중단된 안내 세션입니다.");
            }
            if (!"COMPLETED".equals(session.getStatus())) {
                LocalDateTime completedAt = LocalDateTime.now();
                session.complete(completedAt);
                DeliveryJob job = findJob(session.getDeliveryJobId());
                job.markDone();
                jobRepository.save(job);
                sessionRepository.save(session);
            }
            return new GuidanceCompleteResponse(
                    session.getId(), session.getStatus(), session.getCompletedAt());
        });
        return requireTransactionResult(response);
    }

    private GuidanceSessionResponse buildResponse(GuidanceSession session) {
        Route route = routeRepository.findById(session.getRouteId())
                .orElseThrow(() -> new IllegalStateException("세션의 Route가 없습니다."));
        int totalSteps = segmentRepository.countByRouteId(route.getId());
        RouteSegment current = findStep(route.getId(), session.getCurrentSequenceNo());
        RouteSegment previous = session.getCurrentSequenceNo() <= 1 ? null
                : findStep(route.getId(), session.getCurrentSequenceNo() - 1);

        SegmentContext currentContext = toSegmentContext(current);
        List<SegmentContext> queryContexts = new ArrayList<>();
        if (previous != null) {
            queryContexts.add(toSegmentContext(previous));
        }
        queryContexts.add(currentContext);
        List<double[]> vectors = embeddingClient.embed(
                queryContexts.stream().map(queryTextBuilder::build).toList());

        List<KnowledgeCandidate> placeCandidates = candidateRepository.findPublishedByPlaceId(
                session.getPlaceId());
        Set<Long> previousKnowledgeIds = new HashSet<>();
        if (previous != null) {
            SegmentContext previousContext = queryContexts.get(0);
            hybridSearchService.search(
                            placeCandidates, previousContext,
                            searchContext(session, previousContext), vectors.get(0), LocalDateTime.now())
                    .forEach(result -> previousKnowledgeIds.add(result.candidate().id()));
        }
        int currentVectorIndex = previous == null ? 0 : 1;
        List<GuidanceCardResponse> cards = hybridSearchService.search(
                        placeCandidates, currentContext, searchContext(session, currentContext),
                        vectors.get(currentVectorIndex), LocalDateTime.now())
                .stream()
                .filter(result -> !previousKnowledgeIds.contains(result.candidate().id()))
                .map(result -> cardAssembler.assemble(result, LocalDateTime.now()))
                .toList();

        GuidanceStepResponse step = new GuidanceStepResponse(
                current.getSequenceNo(), totalSteps,
                current.getFromNode().getName(), current.getToNode().getName(),
                current.getMovementMode(), traversalLabel(current), current.getInstruction(),
                current.getSequenceNo() == totalSteps, cards);
        return new GuidanceSessionResponse(
                session.getId(), new RouteSummary(route.getId(), route.getName(), totalSteps), step);
    }

    private SearchContext searchContext(GuidanceSession session, SegmentContext segment) {
        LocalDateTime contextTime = session.getContextTime();
        return new SearchContext(
                session.getVehicleClass(), session.getVehicleTonnage().doubleValue(),
                session.getVehicleHeightM().doubleValue(),
                session.getVehicleWidthM() == null ? null : session.getVehicleWidthM().doubleValue(),
                segment.movementMode(), contextTime.toLocalTime(), contextTime.getDayOfWeek());
    }

    private SegmentContext toSegmentContext(RouteSegment segment) {
        return new SegmentContext(
                segment.getId(), segment.getFromNode().getId(), segment.getToNode().getId(),
                segment.getToNode().getName(), segment.getMovementMode(), segment.getTraversalMethod(),
                segment.getCustomTraversalMethod(), segment.getInstruction(), segment.getSequenceNo() == 1);
    }

    private String traversalLabel(RouteSegment segment) {
        return "OTHER".equals(segment.getTraversalMethod())
                && segment.getCustomTraversalMethod() != null
                ? segment.getCustomTraversalMethod() : segment.getTraversalMethod();
    }

    private RouteOption toOption(Route route) {
        return new RouteOption(
                route.getId(), route.getRouteCode(), route.getName(), route.getVehicleClass(),
                route.getMinTonnage(), route.getMaxTonnage(), route.getMaxVehicleHeightM(),
                route.getMaxVehicleWidthM(), route.isDefaultRoute());
    }

    private DeliveryJob findJob(long id) {
        return jobRepository.findDetailedById(id)
                .orElseThrow(() -> ApiException.notFound("DELIVERY_JOB_NOT_FOUND", "배송 건을 찾을 수 없습니다."));
    }

    private GuidanceSession findSession(long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(
                        "GUIDANCE_SESSION_NOT_FOUND", "안내 세션을 찾을 수 없습니다."));
    }

    private RouteSegment findStep(long routeId, int sequenceNo) {
        return segmentRepository.findStep(routeId, sequenceNo)
                .orElseThrow(() -> new IllegalStateException("안내 단계가 없습니다."));
    }

    private void requireActive(GuidanceSession session) {
        if (!"ACTIVE".equals(session.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "GUIDANCE_SESSION_NOT_ACTIVE", "진행 중인 안내 세션이 아닙니다.");
        }
    }

    private <T> T requireTransactionResult(T value) {
        if (value == null) {
            throw new IllegalStateException("트랜잭션 결과가 없습니다.");
        }
        return value;
    }
}
