package com.finserve.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores the AI underwriting recommendation for audit purposes.
 * This is advisory only — it does NOT change LoanApplication.status directly.
 */
@Entity
@Table(name = "underwriting_results", indexes = {
    @Index(name = "idx_uw_app_id", columnList = "application_id"),
    @Index(name = "idx_uw_created_at", columnList = "created_at")
})
public class UnderwritingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnderwritingRecommendation recommendation;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private Double confidence;

    // Stored as JSON string to avoid schema churn — parsed at read time
    @Column(columnDefinition = "TEXT")
    private String reasons;

    @Column(name = "verification_issues", columnDefinition = "TEXT")
    private String verificationIssues;

    @Column(name = "requires_human_review")
    private Boolean requiresHumanReview;

    @Column(name = "policy_references", columnDefinition = "TEXT")
    private String policyReferences;

    @Column(name = "ai_model")
    private String aiModel;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UnderwritingResult() {}

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public UnderwritingRecommendation getRecommendation() { return recommendation; }
    public void setRecommendation(UnderwritingRecommendation recommendation) { this.recommendation = recommendation; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getReasons() { return reasons; }
    public void setReasons(String reasons) { this.reasons = reasons; }

    public String getVerificationIssues() { return verificationIssues; }
    public void setVerificationIssues(String verificationIssues) { this.verificationIssues = verificationIssues; }

    public Boolean getRequiresHumanReview() { return requiresHumanReview; }
    public void setRequiresHumanReview(Boolean requiresHumanReview) { this.requiresHumanReview = requiresHumanReview; }

    public String getPolicyReferences() { return policyReferences; }
    public void setPolicyReferences(String policyReferences) { this.policyReferences = policyReferences; }

    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
