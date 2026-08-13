package com.moveai.dataset.validation;

import com.moveai.dataset.dto.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class DatasetValidator {
    public List<String> validate(DatasetImportDto dto) {
        List<String> errors = new ArrayList<>();
        if (dto == null || dto.place() == null) {
            errors.add("place is required");
            return errors;
        }
        requireUnique(dto.nodes().stream().map(NodeImportDto::nodeCode).toList(), "node_code", errors);
        requireUnique(dto.routes().stream().map(RouteImportDto::routeCode).toList(), "route_code", errors);
        requireUnique(dto.routes().stream().flatMap(r -> r.segments().stream()).map(RouteSegmentImportDto::segmentCode).toList(), "segment_code", errors);
        requireUnique(dto.reports().stream().map(FieldReportImportDto::reportCode).toList(), "report_code", errors);
        requireUnique(dto.knowledge().stream().map(ExpectedKnowledgeImportDto::knowledgeCode).toList(), "knowledge_code", errors);

        Set<String> nodes = new HashSet<>(dto.nodes().stream().map(NodeImportDto::nodeCode).toList());
        for (NodeImportDto n : dto.nodes()) {
            if (n.parentNodeCode() != null && !nodes.contains(n.parentNodeCode()))
                errors.add("parent_node_code not found: " + n.parentNodeCode());
        }
        for (RouteImportDto r : dto.routes()) {
            if (!nodes.contains(r.startNodeCode())) errors.add("route start node not found: " + r.startNodeCode());
            if (!nodes.contains(r.destinationNodeCode())) errors.add("route destination node not found: " + r.destinationNodeCode());
            List<RouteSegmentImportDto> s = r.segments().stream().sorted(Comparator.comparing(RouteSegmentImportDto::sequence)).toList();
            if (!s.isEmpty()) {
                if (!s.get(0).fromNodeCode().equals(r.startNodeCode())) errors.add("route first segment.from mismatch: " + r.routeCode());
                if (!s.get(s.size()-1).toNodeCode().equals(r.destinationNodeCode())) errors.add("route last segment.to mismatch: " + r.routeCode());
                for (int i=1; i<s.size(); i++) if (!s.get(i-1).toNodeCode().equals(s.get(i).fromNodeCode()))
                    errors.add("route discontinuity: " + r.routeCode());
            }
            for (RouteSegmentImportDto x : r.segments()) {
                if (!nodes.contains(x.fromNodeCode())) errors.add("segment from node not found: " + x.fromNodeCode());
                if (!nodes.contains(x.toNodeCode())) errors.add("segment to node not found: " + x.toNodeCode());
            }
        }
        Set<String> reports = new HashSet<>(dto.reports().stream().map(FieldReportImportDto::reportCode).toList());
        for (ExpectedKnowledgeImportDto k : dto.knowledge()) {
            if (!reports.contains(k.reportCode())) errors.add("knowledge report not found: " + k.reportCode());
            if (k.sourceExcerpt() != null && k.reportCode() != null) {
                String transcript = dto.reports().stream().filter(r -> r.reportCode().equals(k.reportCode()))
                        .findFirst().map(FieldReportImportDto::transcript).orElse("");
                if (!transcript.contains(k.sourceExcerpt())) errors.add("source_excerpt mismatch: " + k.knowledgeCode());
            }
            var t = k.target();
            if ("UNRESOLVED".equals(k.resolved()) || (t != null && "UNRESOLVED".equals(t.targetResolutionStatus()))) {
                if (t != null && t.targetCode() != null) errors.add("UNRESOLVED target_code must be null: " + k.knowledgeCode());
            }
            if (t != null && "RESOLVED".equals(t.targetResolutionStatus()) && t.targetCode() == null)
                errors.add("RESOLVED target_code must not be null: " + k.knowledgeCode());
            if ("DRIVE".equals(k.traversalMethod())) errors.add("DRIVE is forbidden: " + k.knowledgeCode());
        }
        return errors;
    }

    private void requireUnique(List<String> values, String field, List<String> errors) {
        Set<String> set = new HashSet<>();
        for (String v : values) if (!set.add(v)) errors.add(field + " duplicate: " + v);
    }
}
