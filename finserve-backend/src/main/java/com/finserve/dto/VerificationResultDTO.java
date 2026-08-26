package com.finserve.dto;

import com.finserve.model.MatchStatus;
import com.finserve.model.VerificationResult;
import java.time.LocalDateTime;

public class VerificationResultDTO {
    private Long id;
    private String field;
    private String declaredValue;
    private String extractedValue;
    private MatchStatus matchStatus;
    private Double confidence;
    private LocalDateTime createdAt;

    public VerificationResultDTO() {}

    public VerificationResultDTO(Long id, String field, String declaredValue, String extractedValue, MatchStatus matchStatus, Double confidence, LocalDateTime createdAt) {
        this.id = id;
        this.field = field;
        this.declaredValue = declaredValue;
        this.extractedValue = extractedValue;
        this.matchStatus = matchStatus;
        this.confidence = confidence;
        this.createdAt = createdAt;
    }

    public static VerificationResultDTO fromEntity(VerificationResult entity) {
        if (entity == null) return null;
        return new VerificationResultDTO(
            entity.getId(),
            entity.getField(),
            entity.getDeclaredValue(),
            entity.getExtractedValue(),
            entity.getMatchStatus(),
            entity.getConfidence(),
            entity.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getDeclaredValue() { return declaredValue; }
    public void setDeclaredValue(String declaredValue) { this.declaredValue = declaredValue; }

    public String getExtractedValue() { return extractedValue; }
    public void setExtractedValue(String extractedValue) { this.extractedValue = extractedValue; }

    public MatchStatus getMatchStatus() { return matchStatus; }
    public void setMatchStatus(MatchStatus matchStatus) { this.matchStatus = matchStatus; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
