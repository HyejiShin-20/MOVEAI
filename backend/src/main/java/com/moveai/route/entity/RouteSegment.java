package com.moveai.route.entity;

import com.moveai.place.entity.PlaceNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "route_segments")
public class RouteSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "segment_code", nullable = false)
    private String segmentCode;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    // 단계 응답에 노드 "이름"이 필요하다 (05B §4-4). 단계는 경로당 최대 7개라 지연 로딩으로 충분하다.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_node_id")
    private PlaceNode fromNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_node_id")
    private PlaceNode toNode;

    @Column(name = "movement_mode", nullable = false)
    private String movementMode;

    @Column(name = "traversal_method", nullable = false)
    private String traversalMethod;

    @Column(name = "custom_traversal_method")
    private String customTraversalMethod;

    @Column(nullable = false)
    private String instruction;

    @Column(name = "is_indoor", nullable = false)
    private boolean indoor;

    protected RouteSegment() {}

    public Long getId() {
        return id;
    }

    public Long getRouteId() {
        return routeId;
    }

    public String getSegmentCode() {
        return segmentCode;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public PlaceNode getFromNode() {
        return fromNode;
    }

    public PlaceNode getToNode() {
        return toNode;
    }

    public String getMovementMode() {
        return movementMode;
    }

    public String getTraversalMethod() {
        return traversalMethod;
    }

    public String getCustomTraversalMethod() {
        return customTraversalMethod;
    }

    public String getInstruction() {
        return instruction;
    }

    public boolean isIndoor() {
        return indoor;
    }
}
