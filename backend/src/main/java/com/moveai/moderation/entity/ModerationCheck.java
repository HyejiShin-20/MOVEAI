package com.moveai.moderation.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "moderation_checks")
public class ModerationCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private boolean safe = true;

    @Column(columnDefinition = "TEXT")
    private String reason;

    protected ModerationCheck() {}

    public ModerationCheck(String targetType, Long targetId, boolean safe, String reason) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.safe = safe;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public boolean isSafe() { return safe; }
    public void setSafe(boolean safe) { this.safe = safe; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
