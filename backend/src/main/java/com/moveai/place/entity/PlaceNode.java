package com.moveai.place.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "place_nodes", uniqueConstraints = @UniqueConstraint(name = "uk_nodes_code", columnNames = "node_code"))
public class PlaceNode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "node_code", nullable = false, length = 100)
    private String nodeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_node_id")
    private PlaceNode parentNode;

    protected PlaceNode() {}
    public PlaceNode(Place place, String nodeCode) { this.place = place; this.nodeCode = nodeCode; }
    public Long getId() { return id; }
    public String getNodeCode() { return nodeCode; }
    public Place getPlace() { return place; }
    public void setParentNode(PlaceNode parentNode) { this.parentNode = parentNode; }
}
