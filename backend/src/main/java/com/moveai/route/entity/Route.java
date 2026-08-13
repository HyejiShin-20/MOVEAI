package com.moveai.route.entity;

import com.moveai.place.entity.Place;
import com.moveai.place.entity.PlaceNode;
import jakarta.persistence.*;

@Entity
@Table(name = "routes", uniqueConstraints = @UniqueConstraint(name = "uk_routes_code", columnNames = "route_code"))
public class Route {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "route_code", nullable = false, length = 100)
    private String routeCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "start_node_id", nullable = false)
    private PlaceNode startNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_node_id", nullable = false)
    private PlaceNode destinationNode;

    protected Route() {}
    public Route(Place place, String routeCode, PlaceNode startNode, PlaceNode destinationNode) {
        this.place = place; this.routeCode = routeCode; this.startNode = startNode; this.destinationNode = destinationNode;
    }
    public Long getId() { return id; }
    public String getRouteCode() { return routeCode; }
    public PlaceNode getStartNode() { return startNode; }
    public PlaceNode getDestinationNode() { return destinationNode; }
}
