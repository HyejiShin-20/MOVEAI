package com.moveai.retrieval;

/** 현재 RouteSegment의 구조적 문맥. */
public record SegmentContext(
        long segmentId,
        long fromNodeId,
        long toNodeId,
        String toNodeName,
        String movementMode,
        String traversalMethod,
        String customTraversalMethod,
        String instruction,
        boolean firstSegment) {
}
