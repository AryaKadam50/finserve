package com.finserve.service;

import com.finserve.dto.DocumentDTO;
import com.finserve.dto.ExtractedDocumentData;
import com.finserve.dto.VerificationResultDTO;
import com.finserve.exception.BadRequestException;
import com.finserve.exception.ResourceNotFoundException;
import com.finserve.model.Document;
import com.finserve.model.DocumentType;
import com.finserve.model.LoanApplication;
import com.finserve.model.VerificationResult;
import com.finserve.model.VerificationStatus;
import com.finserve.repository.DocumentRepository;
import com.finserve.repository.LoanRepository;
import com.finserve.repository.VerificationResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final LoanRepository loanRepository;
    private final VerificationResultRepository verificationResultRepository;
    private final DocumentExtractionService documentExtractionService;
    private final VerificationService verificationService;
    private final com.finserve.repository.AuditEventRepository auditEventRepository;

    @Value("${upload.path:./uploads}")
    private String uploadDir;

    public DocumentService(DocumentRepository documentRepository,
                           LoanRepository loanRepository,
                           VerificationResultRepository verificationResultRepository,
                           DocumentExtractionService documentExtractionService,
                           VerificationService verificationService,
                           com.finserve.repository.AuditEventRepository auditEventRepository) {
        this.documentRepository = documentRepository;
        this.loanRepository = loanRepository;
        this.verificationResultRepository = verificationResultRepository;
        this.documentExtractionService = documentExtractionService;
        this.verificationService = verificationService;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public DocumentDTO uploadDocument(Long loanId, Long userId, String userRole, DocumentType type, MultipartFile file) {
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", "id", loanId));

        // Authorization Check
        if (!"ADMIN".equals(userRole) && !loan.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to upload documents for this application");
        }

        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        try {
            // Create directory if it doesn't exist
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Generate unique filename
            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName != null && originalFileName.contains(".")
                    ? originalFileName.substring(originalFileName.lastIndexOf("."))
                    : "";
            String uniqueFileName = UUID.randomUUID().toString() + extension;
            Path filePath = Paths.get(uploadDir, uniqueFileName);

            // Save file to local storage
            Files.write(filePath, file.getBytes());

            // Save initial document metadata
            Document document = new Document();
            document.setLoanApplication(loan);
            document.setDocumentType(type);
            document.setOriginalFileName(originalFileName);
            document.setStoragePath(filePath.toString());
            document.setVerificationStatus(VerificationStatus.PENDING);
            Document savedDocument = documentRepository.save(document);

            // --- Extraction & Verification Pipeline ---
            try {
                File storedFile = filePath.toFile();
                ExtractedDocumentData extracted = documentExtractionService.extractData(savedDocument, storedFile);
                if (extracted != null && !extracted.getExtractedFields().isEmpty()) {
                    verificationService.verify(savedDocument, extracted);
                    // Persist the updated verification status on the document
                    savedDocument = documentRepository.save(savedDocument);
                    log.info("Extraction and verification complete for document id={}", savedDocument.getId());
                } else {
                    savedDocument.setVerificationStatus(VerificationStatus.FAILED);
                    savedDocument = documentRepository.save(savedDocument);
                    log.warn("Extraction returned null/empty for document id={}", savedDocument.getId());
                }
            } catch (Exception e) {
                savedDocument.setVerificationStatus(VerificationStatus.FAILED);
                log.error("Extraction/verification pipeline failed for document id={}: {}", savedDocument.getId(), e.getMessage());
            }

            // Record audit event
            auditEventRepository.save(new com.finserve.model.AuditEvent(
                    loanId, 
                    "DOCUMENT_UPLOADED", 
                    "Uploaded " + type + " document: " + originalFileName, 
                    userId, 
                    userRole.equals("ADMIN") ? "Admin User" : loan.getUser().getName()
            ));

            return DocumentDTO.fromEntity(savedDocument);

        } catch (IOException e) {
            throw new RuntimeException("Could not store file", e);
        }
    }

    /**
     * Fetches documents for a loan.
     * Admins get full verification results; customers only see status.
     */
    public List<DocumentDTO> getDocumentsForLoan(Long loanId, Long userId, String userRole) {
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", "id", loanId));

        // Authorization Check
        if (!"ADMIN".equals(userRole) && !loan.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to view documents for this application");
        }

        boolean isAdmin = "ADMIN".equals(userRole);

        return documentRepository.findByLoanApplicationId(loanId).stream()
                .map(doc -> {
                    DocumentDTO dto = DocumentDTO.fromEntity(doc);
                    if (isAdmin) {
                        // Admins see full verification results
                        List<VerificationResultDTO> verResults = verificationResultRepository
                                .findByDocumentId(doc.getId())
                                .stream()
                                .map(VerificationResultDTO::fromEntity)
                                .collect(Collectors.toList());
                        dto.setVerificationResults(verResults);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
