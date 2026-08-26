package com.finserve.service;

import com.finserve.dto.ExtractedDocumentData;
import com.finserve.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the MockDocumentExtractionServiceImpl in isolation.
 * Validates extraction success, field presence, and failure handling.
 */
public class MockDocumentExtractionServiceImplTest {

    private MockDocumentExtractionServiceImpl extractionService;
    private LoanApplication loan;
    private User user;

    @BeforeEach
    void setUp() {
        extractionService = new MockDocumentExtractionServiceImpl();

        user = new User();
        user.setId(1L);
        user.setName("Arya Kadam");

        loan = new LoanApplication();
        loan.setId(100L);
        loan.setUser(user);
        loan.setMonthlyIncome(new BigDecimal("80000"));
        loan.setEmploymentType("Salaried");
    }

    @Test
    void salarySlip_Extraction_ReturnsExpectedFields() throws Exception {
        Document doc = buildDoc(DocumentType.SALARY_SLIP);
        File tempFile = File.createTempFile("test-salary", ".pdf");
        tempFile.deleteOnExit();

        ExtractedDocumentData result = extractionService.extractData(doc, tempFile);

        assertNotNull(result);
        assertTrue(result.getExtractedFields().containsKey("employeeName"));
        assertTrue(result.getExtractedFields().containsKey("netIncome"));
        assertTrue(result.getExtractedFields().containsKey("grossIncome"));
        assertTrue(result.getExtractedFields().containsKey("employer"));
        assertTrue(result.getExtractedFields().containsKey("payPeriod"));

        // Verify that extracted net income is ~2% less than declared (the simulated discrepancy)
        BigDecimal declared = new BigDecimal("80000");
        BigDecimal extracted = new BigDecimal(result.getExtractedFields().get("netIncome"));
        assertTrue(extracted.compareTo(declared) < 0, "Extracted net income should be less than declared gross");
    }

    @Test
    void employmentProof_Extraction_ReturnsExpectedFields() throws Exception {
        Document doc = buildDoc(DocumentType.EMPLOYMENT_PROOF);
        File tempFile = File.createTempFile("test-employment", ".pdf");
        tempFile.deleteOnExit();

        ExtractedDocumentData result = extractionService.extractData(doc, tempFile);

        assertNotNull(result);
        assertTrue(result.getExtractedFields().containsKey("employeeName"));
        assertTrue(result.getExtractedFields().containsKey("employer"));
        assertTrue(result.getExtractedFields().containsKey("employmentStatus"));
        assertTrue(result.getExtractedFields().containsKey("employmentDate"));
    }

    @Test
    void extraction_NullFile_ReturnsNull() {
        Document doc = buildDoc(DocumentType.SALARY_SLIP);
        ExtractedDocumentData result = extractionService.extractData(doc, null);
        assertNull(result);
    }

    @Test
    void extraction_NonExistentFile_ReturnsNull() {
        Document doc = buildDoc(DocumentType.SALARY_SLIP);
        File nonExistent = new File("/nonexistent/path/file.pdf");
        ExtractedDocumentData result = extractionService.extractData(doc, nonExistent);
        assertNull(result);
    }

    @Test
    void extraction_NullDocument_ReturnsNull() throws Exception {
        File tempFile = File.createTempFile("test", ".pdf");
        tempFile.deleteOnExit();
        ExtractedDocumentData result = extractionService.extractData(null, tempFile);
        assertNull(result);
    }

    private Document buildDoc(DocumentType type) {
        Document doc = new Document();
        doc.setId(10L);
        doc.setDocumentType(type);
        doc.setLoanApplication(loan);
        return doc;
    }
}
