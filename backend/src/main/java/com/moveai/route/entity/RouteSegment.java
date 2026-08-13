package com.moveai.route.entity;

import com.moveai.place.entity.PlaceNode;
import jakarta.persistence.*;

@Entity
@Table(name = "route_segments", uniqueConstraints = @UniqueConstraint(name = "uk_segments_code", columnNames = "segment_code"))
public class RouteSegment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "segment_code", nullable = false, length = 100)
    private String segmentCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_node_id", nullable = false)
    private PlaceNode fromNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_node_id", nullable = false)
    private PlaceNode toNode;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    protected RouteSegment() {}
    public RouteSegment(Route route, String segmentCode, PlaceNode fromNode, PlaceNode toNode, Integer sequenceNo) {
        this.route = route; this.segmentCode = segmentCode; this.fromNode = fromNode; this.toNode = toNode; this.sequenceNo = sequenceNo;
    }
    public Long getId() { return id; }
    public Integer getSequenceNo() { return sequenceNo; }
}
