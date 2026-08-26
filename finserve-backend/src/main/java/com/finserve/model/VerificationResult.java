package com.finserve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_results")
public class VerificationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    @JsonIgnoreProperties("verificationResults")
    private Document document;

    @NotNull
    private String field;

    @Column(name = "declared_value")
    private String declaredValue;

    @Column(name = "extracted_value")
    private String extractedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status")
    private MatchStatus matchStatus;

    private Double confidence;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public VerificationResult() {}

    public VerificationResult(Long id, Document document, String field, String declaredValue, String extractedValue, MatchStatus matchStatus, Double confidence, LocalDateTime createdAt) {
        this.id = id;
        this.document = document;
        this.field = field;
        this.declaredValue = declaredValue;
        this.extractedValue = extractedValue;
        this.matchStatus = matchStatus;
        this.confidence = confidence;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

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
