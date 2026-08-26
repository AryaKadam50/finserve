package com.finserve.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_app_id", columnList = "application_id"),
    @Index(name = "idx_audit_created_at", columnList = "created_at")
})
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "actor_id")
    private Long actorId; // Nullable if system/AI action

    @Column(name = "actor_name")
    private String actorName; // Store name so we don't need strict joins

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public AuditEvent() {}

    public AuditEvent(Long applicationId, String actionType, String description, Long actorId, String actorName) {
        this.applicationId = applicationId;
        this.actionType = actionType;
        this.description = description;
        this.actorId = actorId;
        this.actorName = actorName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
