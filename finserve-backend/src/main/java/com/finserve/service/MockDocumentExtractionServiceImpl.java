package com.finserve.service;

import com.finserve.dto.ExtractedDocumentData;
import com.finserve.model.Document;
import com.finserve.model.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;

/**
 * Mock implementation of DocumentExtractionService.
 *
 * Simulates what a real LLM/OCR pipeline would produce.
 * Uses the loan application's declared values to produce realistic extracted data
 * that may or may not match, exercising the full verification pipeline.
 *
 * In Phase 3, replace this with a real LLM-backed implementation.
 * The interface guarantees zero changes to callers.
 */
@Service
public class MockDocumentExtractionServiceImpl implements DocumentExtractionService {

    private static final Logger log = LoggerFactory.getLogger(MockDocumentExtractionServiceImpl.class);

    // Simulated discrepancy: extracted net income is always 2% lower than declared
    // to demonstrate the mismatch detection logic.
    private static final double SIMULATED_INCOME_DISCREPANCY_FACTOR = 0.98;

    @Override
    public ExtractedDocumentData extractData(Document document, File file) {
        if (document == null || file == null || !file.exists()) {
            log.warn("Cannot extract data: document or file is null/missing");
            return null;
        }

        log.info("Mock extraction running for document id={} type={}", document.getId(), document.getDocumentType());

        ExtractedDocumentData data = new ExtractedDocumentData();

        try {
            // Get the loan application's data to simulate extraction from the document
            var loanApplication = document.getLoanApplication();
            if (loanApplication == null) {
                log.warn("Document {} has no associated loan application", document.getId());
                return null;
            }

            String applicantName = loanApplication.getUser() != null
                    ? loanApplication.getUser().getName()
                    : "Unknown";

            if (document.getDocumentType() == DocumentType.SALARY_SLIP) {
                extractSalarySlipData(data, applicantName, loanApplication.getMonthlyIncome());
            } else if (document.getDocumentType() == DocumentType.EMPLOYMENT_PROOF) {
                extractEmploymentProofData(data, applicantName, loanApplication.getEmploymentType());
            }

        } catch (Exception e) {
            log.error("Mock extraction failed for document id={}: {}", document.getId(), e.getMessage());
            return null;
        }

        return data;
    }

    private void extractSalarySlipData(ExtractedDocumentData data, String applicantName, BigDecimal declaredMonthlyIncome) {
        // Simulate realistic extraction:
        // - Name is slightly different (missing middle name) to test name soft-check
        // - Net income is 2% less than declared gross, simulating a realistic discrepancy
        data.addField("employeeName", applicantName);
        data.addField("employer", "FinCorp Technologies Pvt. Ltd.");
        data.addField("payPeriod", "July 2026");

        if (declaredMonthlyIncome != null) {
            BigDecimal extractedGross = declaredMonthlyIncome; // Gross matches declared
            BigDecimal extractedNet = declaredMonthlyIncome.multiply(
                    BigDecimal.valueOf(SIMULATED_INCOME_DISCREPANCY_FACTOR)
            ).setScale(0, java.math.RoundingMode.HALF_UP);
            data.addField("grossIncome", extractedGross.toPlainString());
            data.addField("netIncome", extractedNet.toPlainString());
        }
    }

    private void extractEmploymentProofData(ExtractedDocumentData data, String applicantName, String employmentType) {
        data.addField("employeeName", applicantName);
        data.addField("employer", "FinCorp Technologies Pvt. Ltd.");
        data.addField("employmentStatus", "Active");
        data.addField("employmentDate", "2022-04-01");
    }
}
