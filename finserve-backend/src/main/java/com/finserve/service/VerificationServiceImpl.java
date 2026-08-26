package com.finserve.service;

import com.finserve.dto.ExtractedDocumentData;
import com.finserve.model.*;
import com.finserve.repository.VerificationResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Performs deterministic verification of extracted document data against
 * the declared values in the LoanApplication.
 *
 * All comparison logic is in Java — no AI system can directly alter loan status.
 */
@Service
public class VerificationServiceImpl implements VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationServiceImpl.class);

    private final VerificationResultRepository verificationResultRepository;

    @Value("${verification.income.tolerance.percent:5}")
    private double incomeTolerance;

    public VerificationServiceImpl(VerificationResultRepository verificationResultRepository) {
        this.verificationResultRepository = verificationResultRepository;
    }

    @Override
    public List<VerificationResult> verify(Document document, ExtractedDocumentData extracted) {
        List<VerificationResult> results = new ArrayList<>();
        LoanApplication loan = document.getLoanApplication();
        Map<String, String> fields = extracted.getExtractedFields();
        boolean anyMismatch = false;

        if (document.getDocumentType() == DocumentType.SALARY_SLIP) {
            // --- Income verification ---
            if (fields.containsKey("netIncome") && loan.getMonthlyIncome() != null) {
                String declaredStr = loan.getMonthlyIncome().toPlainString();
                String extractedStr = fields.get("netIncome");
                MatchStatus incomeStatus = checkIncomeMatch(loan.getMonthlyIncome(), extractedStr);
                results.add(buildResult(document, "Monthly Income", declaredStr, extractedStr, incomeStatus, 0.95));
                if (incomeStatus == MatchStatus.MISMATCH) anyMismatch = true;
            }

            // --- Name verification ---
            if (fields.containsKey("employeeName") && loan.getUser() != null) {
                String declared = loan.getUser().getName();
                String extracted2 = fields.get("employeeName");
                MatchStatus nameStatus = checkNameMatch(declared, extracted2);
                results.add(buildResult(document, "Employee Name", declared, extracted2, nameStatus, 0.90));
                if (nameStatus == MatchStatus.MISMATCH) anyMismatch = true;
            }

            // --- Employer info (just recorded, no hard rule) ---
            if (fields.containsKey("employer")) {
                results.add(buildResult(document, "Employer", "N/A (Declared)", fields.get("employer"), MatchStatus.NOT_FOUND, 0.80));
            }

        } else if (document.getDocumentType() == DocumentType.EMPLOYMENT_PROOF) {
            // --- Employment status ---
            if (fields.containsKey("employmentStatus")) {
                String status = fields.get("employmentStatus");
                MatchStatus empStatus = "Active".equalsIgnoreCase(status) ? MatchStatus.MATCH : MatchStatus.MISMATCH;
                results.add(buildResult(document, "Employment Status", "Active", status, empStatus, 0.90));
                if (empStatus == MatchStatus.MISMATCH) anyMismatch = true;
            }

            // --- Name verification ---
            if (fields.containsKey("employeeName") && loan.getUser() != null) {
                String declared = loan.getUser().getName();
                String extracted2 = fields.get("employeeName");
                MatchStatus nameStatus = checkNameMatch(declared, extracted2);
                results.add(buildResult(document, "Employee Name", declared, extracted2, nameStatus, 0.90));
                if (nameStatus == MatchStatus.MISMATCH) anyMismatch = true;
            }

            // --- Employer info ---
            if (fields.containsKey("employer")) {
                results.add(buildResult(document, "Employer", "N/A (Declared)", fields.get("employer"), MatchStatus.NOT_FOUND, 0.80));
            }
        }

        // Persist all results
        List<VerificationResult> saved = verificationResultRepository.saveAll(results);

        // Update document verification status
        if (anyMismatch) {
            document.setVerificationStatus(VerificationStatus.FLAGGED);
            log.info("Document {} flagged due to mismatches", document.getId());
        } else {
            document.setVerificationStatus(VerificationStatus.VERIFIED);
            log.info("Document {} verified successfully", document.getId());
        }

        return saved;
    }

    private VerificationResult buildResult(Document document, String field, String declared, String extracted, MatchStatus status, double confidence) {
        VerificationResult vr = new VerificationResult();
        vr.setDocument(document);
        vr.setField(field);
        vr.setDeclaredValue(declared);
        vr.setExtractedValue(extracted);
        vr.setMatchStatus(status);
        vr.setConfidence(confidence);
        return vr;
    }

    private MatchStatus checkIncomeMatch(BigDecimal declared, String extractedStr) {
        try {
            BigDecimal extracted = new BigDecimal(extractedStr);
            BigDecimal toleranceAmount = declared.multiply(BigDecimal.valueOf(incomeTolerance / 100.0));
            BigDecimal diff = declared.subtract(extracted).abs();
            return diff.compareTo(toleranceAmount) <= 0 ? MatchStatus.MATCH : MatchStatus.MISMATCH;
        } catch (NumberFormatException e) {
            log.warn("Could not parse extracted income value: {}", extractedStr);
            return MatchStatus.NOT_FOUND;
        }
    }

    private MatchStatus checkNameMatch(String declared, String extracted) {
        if (declared == null || extracted == null) return MatchStatus.NOT_FOUND;
        return declared.trim().equalsIgnoreCase(extracted.trim()) ? MatchStatus.MATCH : MatchStatus.MISMATCH;
    }
}
