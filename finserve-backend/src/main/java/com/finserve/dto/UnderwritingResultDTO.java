package com.finserve.dto;

import com.finserve.model.RiskLevel;
import com.finserve.model.UnderwritingRecommendation;
import com.finserve.model.UnderwritingResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UnderwritingResultDTO {

    private Long id;
    private Long applicationId;
    private UnderwritingRecommendation recommendation;
    private RiskLevel riskLevel;
    private Double confidence;
    private List<String> reasons;
    private List<String> verificationIssues;
    private List<Map<String, Object>> policyReferences;
    private Boolean requiresHumanReview;
    private String aiModel;
    private LocalDateTime createdAt;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public UnderwritingResultDTO() {}

    public static UnderwritingResultDTO fromEntity(UnderwritingResult entity) {
        if (entity == null) return null;
        UnderwritingResultDTO dto = new UnderwritingResultDTO();
        dto.setId(entity.getId());
        dto.setApplicationId(entity.getApplicationId());
        dto.setRecommendation(entity.getRecommendation());
        dto.setRiskLevel(entity.getRiskLevel());
        dto.setConfidence(entity.getConfidence());
        dto.setRequiresHumanReview(entity.getRequiresHumanReview());
        dto.setAiModel(entity.getAiModel());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setReasons(parseJsonList(entity.getReasons()));
        dto.setVerificationIssues(parseJsonList(entity.getVerificationIssues()));
        dto.setPolicyReferences(parseJsonListOfMaps(entity.getPolicyReferences()));
        return dto;
    }

    private static List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.singletonList(json);
        }
    }

    private static List<Map<String, Object>> parseJsonListOfMaps(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

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

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }

    public List<String> getVerificationIssues() { return verificationIssues; }
    public void setVerificationIssues(List<String> verificationIssues) { this.verificationIssues = verificationIssues; }

    public List<Map<String, Object>> getPolicyReferences() { return policyReferences; }
    public void setPolicyReferences(List<Map<String, Object>> policyReferences) { this.policyReferences = policyReferences; }

    public Boolean getRequiresHumanReview() { return requiresHumanReview; }
    public void setRequiresHumanReview(Boolean requiresHumanReview) { this.requiresHumanReview = requiresHumanReview; }

    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
