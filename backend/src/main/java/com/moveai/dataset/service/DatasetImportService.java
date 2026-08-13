package com.moveai.dataset.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveai.dataset.dto.*;
import com.moveai.dataset.validation.DatasetValidator;
import com.moveai.place.entity.*;
import com.moveai.place.repository.*;
import com.moveai.route.entity.*;
import com.moveai.route.repository.*;
import com.moveai.report.entity.FieldReport;
import com.moveai.report.repository.FieldReportRepository;
import com.moveai.knowledge.entity.*;
import com.moveai.knowledge.repository.*;
import com.moveai.common.exception.*;
import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DatasetImportService {
    private final ObjectMapper objectMapper;
    private final DatasetValidator validator;
    private final PlaceRepository placeRepository;
    private final PlaceNodeRepository nodeRepository;
    private final RouteRepository routeRepository;
    private final RouteSegmentRepository segmentRepository;
    private final FieldReportRepository reportRepository;
    private final KnowledgeItemRepository knowledgeRepository;
    private final KnowledgeConditionRepository conditionRepository;
    private final KnowledgeTargetRepository targetRepository;

    public DatasetImportService(ObjectMapper objectMapper, DatasetValidator validator, PlaceRepository placeRepository,
            PlaceNodeRepository nodeRepository, RouteRepository routeRepository, RouteSegmentRepository segmentRepository,
            FieldReportRepository reportRepository, KnowledgeItemRepository knowledgeRepository,
            KnowledgeConditionRepository conditionRepository, KnowledgeTargetRepository targetRepository) {
        this.objectMapper = objectMapper; this.validator = validator; this.placeRepository = placeRepository;
        this.nodeRepository = nodeRepository; this.routeRepository = routeRepository; this.segmentRepository = segmentRepository;
        this.reportRepository = reportRepository; this.knowledgeRepository = knowledgeRepository;
        this.conditionRepository = conditionRepository; this.targetRepository = targetRepository;
    }

    public DatasetImportDto parse(MultipartFile file) {
        try {
            return objectMapper.readValue(file.getInputStream(), DatasetImportDto.class);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.VALIDATION, "Invalid dataset JSON: " + e.getMessage());
        }
    }

    public List<String> validate(MultipartFile file) {
        return validator.validate(parse(file));
    }

    @Transactional
    public void importDataset(MultipartFile file) {
        DatasetImportDto dto = parse(file);
        List<String> errors = validator.validate(dto);
        if (!errors.isEmpty()) throw new BusinessException(ErrorCode.VALIDATION, String.join("; ", errors));

        Place place = placeRepository.findByPlaceCode(dto.place().placeCode())
                .orElseGet(() -> placeRepository.save(new Place(dto.place().placeCode(), dto.place().name())));

        Map<String, Long> nodeIdMap = new HashMap<>();
        Map<String, PlaceNode> nodeMap = new HashMap<>();
        for (NodeImportDto n : dto.nodes()) {
            PlaceNode node = nodeRepository.save(new PlaceNode(place, n.nodeCode()));
            nodeMap.put(n.nodeCode(), node);
            nodeIdMap.put(n.nodeCode(), node.getId());
        }
        for (NodeImportDto n : dto.nodes()) {
            if (n.parentNodeCode() != null) nodeMap.get(n.nodeCode()).setParentNode(nodeMap.get(n.parentNodeCode()));
        }
        nodeRepository.saveAll(nodeMap.values());

        Map<String, Route> routeMap = new HashMap<>();
        for (RouteImportDto r : dto.routes()) {
            Route route = routeRepository.save(new Route(place, r.routeCode(), nodeMap.get(r.startNodeCode()), nodeMap.get(r.destinationNodeCode())));
            routeMap.put(r.routeCode(), route);
            for (RouteSegmentImportDto s : r.segments()) {
                segmentRepository.save(new RouteSegment(route, s.segmentCode(), nodeMap.get(s.fromNodeCode()), nodeMap.get(s.toNodeCode()), s.sequence()));
            }
        }

        Map<String, FieldReport> reportMap = new HashMap<>();
        for (FieldReportImportDto r : dto.reports()) {
            FieldReport report = reportRepository.save(new FieldReport(place, r.reportCode(), r.transcript(), r.audioRecordingCandidate()));
            reportMap.put(r.reportCode(), report);
        }

        for (ExpectedKnowledgeImportDto k : dto.knowledge()) {
            if (!k.resolved() || (k.target() != null && "UNRESOLVED".equals(k.target().targetResolutionStatus()))) continue;
            FieldReport report = reportMap.get(k.reportCode());
            KnowledgeItem item = knowledgeRepository.save(new KnowledgeItem(report, k.knowledgeCode(), k.statement(), "PUBLISHED"));
            if (k.minTonnage() != null || k.maxTonnage() != null || k.maxVehicleHeight() != null) {
                conditionRepository.save(new KnowledgeCondition(item, k.minTonnage(), k.maxTonnage(),
                        Boolean.TRUE.equals(k.minTonnageInclusive()), Boolean.TRUE.equals(k.maxTonnageInclusive()), k.maxVehicleHeight()));
            }
            if (k.target() != null && "NODE".equals(k.target().targetType())) {
                KnowledgeTarget target = new KnowledgeTarget();
                targetRepository.save(target);
            }
        }
    }
}
