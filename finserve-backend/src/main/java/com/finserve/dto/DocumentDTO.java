package com.finserve.dto;

import com.finserve.model.Document;
import com.finserve.model.DocumentType;
import com.finserve.model.VerificationStatus;
import java.time.LocalDateTime;
import java.util.List;

public class DocumentDTO {
    private Long id;
    private Long loanApplicationId;
    private DocumentType documentType;
    private String originalFileName;
    private LocalDateTime uploadedAt;
    private VerificationStatus verificationStatus;
    private List<VerificationResultDTO> verificationResults;

    public DocumentDTO() {}

    public DocumentDTO(Long id, Long loanApplicationId, DocumentType documentType, String originalFileName, LocalDateTime uploadedAt, VerificationStatus verificationStatus, List<VerificationResultDTO> verificationResults) {
        this.id = id;
        this.loanApplicationId = loanApplicationId;
        this.documentType = documentType;
        this.originalFileName = originalFileName;
        this.uploadedAt = uploadedAt;
        this.verificationStatus = verificationStatus;
        this.verificationResults = verificationResults;
    }

    public static DocumentDTO fromEntity(Document entity) {
        if (entity == null) return null;
        return new DocumentDTO(
            entity.getId(),
            entity.getLoanApplication() != null ? entity.getLoanApplication().getId() : null,
            entity.getDocumentType(),
            entity.getOriginalFileName(),
            entity.getUploadedAt(),
            entity.getVerificationStatus(),
            null // Populated conditionally to avoid leaking data
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLoanApplicationId() { return loanApplicationId; }
    public void setLoanApplicationId(Long loanApplicationId) { this.loanApplicationId = loanApplicationId; }

    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public List<VerificationResultDTO> getVerificationResults() { return verificationResults; }
    public void setVerificationResults(List<VerificationResultDTO> verificationResults) { this.verificationResults = verificationResults; }
}
