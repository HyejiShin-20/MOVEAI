package com.moveai.route.entity;

import com.moveai.place.entity.Place;
import jakarta.persistence.*;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 관리자가 미리 등록한 고정 경로. <b>AI가 만들지 않는다</b> (절대 규칙 1).
 *
 * <p>차량 제약(min/max)의 경계 규칙은 04 §11-3에 있다. maxTonnage 는 이하 허용(≤),
 * minTonnage 는 초과만 허용(&gt;)이다. 그래야 정확히 1.0톤에서 두 경로가 동시에 살아남지 않는다.
 */
@Entity
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;
    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "route_code", nullable = false)
    private String routeCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_node_id", nullable = false)
    private Long startNodeId;
    @Column(name = "destination_node_id", nullable = false)
    private Long destinationNodeId;

    @Column(name = "vehicle_class")
    private String vehicleClass;

    @Column(name = "min_tonnage")
    private BigDecimal minTonnage;

    @Column(name = "max_tonnage")
    private BigDecimal maxTonnage;

    @Column(name = "max_vehicle_height_m")
    private BigDecimal maxVehicleHeightM;

    @Column(name = "max_vehicle_width_m")
    private BigDecimal maxVehicleWidthM;

    @Column(name = "is_default", nullable = false)
    private boolean defaultRoute;

    protected Route() {}

    public Long getId() {
        return id;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public String getRouteCode() {
        return routeCode;
    }

    public String getName() {
        return name;
    }

    public Long getStartNodeId() {
        return startNodeId;
    }

    public Long getDestinationNodeId() {
        return destinationNodeId;
    }

    public String getVehicleClass() {
        return vehicleClass;
    }

    public BigDecimal getMinTonnage() {
        return minTonnage;
    }

    public BigDecimal getMaxTonnage() {
        return maxTonnage;
    }

    public BigDecimal getMaxVehicleHeightM() {
        return maxVehicleHeightM;
    }

    public BigDecimal getMaxVehicleWidthM() {
        return maxVehicleWidthM;
    }

    public boolean isDefaultRoute() {
        return defaultRoute;
    }
}
