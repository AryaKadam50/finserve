package com.finserve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id")
    @JsonIgnoreProperties("documents")
    private LoanApplication loanApplication;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "document_type")
    private DocumentType documentType;

    @NotNull
    @Column(name = "original_file_name")
    private String originalFileName;

    @NotNull
    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status")
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    public Document() {}

    public Document(Long id, LoanApplication loanApplication, DocumentType documentType, String originalFileName, String storagePath, LocalDateTime uploadedAt, VerificationStatus verificationStatus) {
        this.id = id;
        this.loanApplication = loanApplication;
        this.documentType = documentType;
        this.originalFileName = originalFileName;
        this.storagePath = storagePath;
        this.uploadedAt = uploadedAt;
        this.verificationStatus = verificationStatus;
    }

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LoanApplication getLoanApplication() { return loanApplication; }
    public void setLoanApplication(LoanApplication loanApplication) { this.loanApplication = loanApplication; }

    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
}
