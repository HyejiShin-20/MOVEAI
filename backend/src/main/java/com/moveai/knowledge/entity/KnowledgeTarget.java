package com.moveai.knowledge.entity;

import com.moveai.place.entity.PlaceNode;
import jakarta.persistence.*;

@Entity
@Table(name = "knowledge_targets")
public class KnowledgeTarget {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "knowledge_item_id", nullable = false)
    private KnowledgeItem knowledgeItem;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_node_id")
    private PlaceNode targetNode;

    protected KnowledgeTarget() {}
}
