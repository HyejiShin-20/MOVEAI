package com.moveai.guidance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.moveai.route.service.VehicleContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 생성 시 확정한 routeId를 끝까지 유지하는 Last 100m 안내 세션. */
@Entity
@Table(name = "guidance_sessions")
public class GuidanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_job_id")
    private Long deliveryJobId;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "route_id", nullable = false)
    private Long routeId;

    @Column(name = "current_sequence_no", nullable = false)
    private int currentSequenceNo;

    @Column(name = "vehicle_class")
    private String vehicleClass;

    @Column(name = "vehicle_tonnage")
    private BigDecimal vehicleTonnage;

    @Column(name = "vehicle_height_m")
    private BigDecimal vehicleHeightM;

    @Column(name = "vehicle_width_m")
    private BigDecimal vehicleWidthM;

    @Column(name = "context_time", nullable = false)
    private LocalDateTime contextTime;

    @Column(nullable = false)
    private String status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected GuidanceSession() {
    }

    public static GuidanceSession start(
            long jobId, long placeId, long routeId, VehicleContext vehicle, LocalDateTime contextTime) {
        GuidanceSession session = new GuidanceSession();
        session.deliveryJobId = jobId;
        session.placeId = placeId;
        session.routeId = routeId;
        session.currentSequenceNo = 1;
        session.vehicleClass = vehicle.vehicleClass();
        session.vehicleTonnage = BigDecimal.valueOf(vehicle.tonnage());
        session.vehicleHeightM = BigDecimal.valueOf(vehicle.heightM());
        session.vehicleWidthM = vehicle.widthM() == null ? null : BigDecimal.valueOf(vehicle.widthM());
        session.contextTime = contextTime;
        session.status = "ACTIVE";
        session.startedAt = LocalDateTime.now();
        return session;
    }

    public void advance(int totalSteps) {
        if (currentSequenceNo < totalSteps) {
            currentSequenceNo++;
        }
    }

    public void complete(LocalDateTime completedAt) {
        status = "COMPLETED";
        this.completedAt = completedAt;
    }

    public Long getId() { return id; }
    public Long getDeliveryJobId() { return deliveryJobId; }
    public Long getPlaceId() { return placeId; }
    public Long getRouteId() { return routeId; }
    public int getCurrentSequenceNo() { return currentSequenceNo; }
    public String getVehicleClass() { return vehicleClass; }
    public BigDecimal getVehicleTonnage() { return vehicleTonnage; }
    public BigDecimal getVehicleHeightM() { return vehicleHeightM; }
    public BigDecimal getVehicleWidthM() { return vehicleWidthM; }
    public LocalDateTime getContextTime() { return contextTime; }
    public String getStatus() { return status; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
