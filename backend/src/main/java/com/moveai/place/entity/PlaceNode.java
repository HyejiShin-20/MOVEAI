package com.moveai.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "place_nodes")
public class PlaceNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "node_code", nullable = false)
    private String nodeCode;

    @Column(name = "parent_node_id")
    private Long parentNodeId;

    @Column(name = "node_type", nullable = false)
    private String nodeType;

    @Column(name = "custom_node_type")
    private String customNodeType;

    @Column(nullable = false)
    private String name;

    @Column(name = "floor_label")
    private String floorLabel;

    @Column(name = "is_indoor", nullable = false)
    private boolean indoor;

    private String description;

    protected PlaceNode() {}

    public Long getId() {
        return id;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public String getNodeCode() {
        return nodeCode;
    }

    public Long getParentNodeId() {
        return parentNodeId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public String getCustomNodeType() {
        return customNodeType;
    }

    public String getName() {
        return name;
    }

    public String getFloorLabel() {
        return floorLabel;
    }

    public boolean isIndoor() {
        return indoor;
    }

    public String getDescription() {
        return description;
    }
}
