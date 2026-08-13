package com.moveai.knowledge.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "knowledge_conditions")
public class KnowledgeCondition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "knowledge_item_id", nullable = false)
    private KnowledgeItem knowledgeItem;

    @Column(name = "min_tonnage")
    private Double minTonnage;
    @Column(name = "max_tonnage")
    private Double maxTonnage;
    @Column(name = "min_tonnage_inclusive", nullable = false)
    private boolean minTonnageInclusive = true;
    @Column(name = "max_tonnage_inclusive", nullable = false)
    private boolean maxTonnageInclusive = true;
    @Column(name = "max_vehicle_height")
    private Double maxVehicleHeight;

    protected KnowledgeCondition() {}

    public KnowledgeCondition(KnowledgeItem item, Double min, Double max, boolean minInc, boolean maxInc, Double maxHeight) {
        this.knowledgeItem = item; this.minTonnage = min; this.maxTonnage = max;
        this.minTonnageInclusive = minInc; this.maxTonnageInclusive = maxInc; this.maxVehicleHeight = maxHeight;
    }
    public Double getMinTonnage() { return minTonnage; }
    public Double getMaxTonnage() { return maxTonnage; }
    public boolean isMinTonnageInclusive() { return minTonnageInclusive; }
    public boolean isMaxTonnageInclusive() { return maxTonnageInclusive; }
    public Double getMaxVehicleHeight() { return maxVehicleHeight; }
}
