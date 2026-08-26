package com.finserve.service;

import com.finserve.dto.ExtractedDocumentData;
import com.finserve.model.*;
import com.finserve.repository.VerificationResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VerificationServiceTest {

    @Mock
    private VerificationResultRepository verificationResultRepository;

    @InjectMocks
    private VerificationServiceImpl verificationService;

    private Document salarySlipDoc;
    private LoanApplication loan;
    private User user;

    @BeforeEach
    void setUp() {
        // Set 5% tolerance via ReflectionTestUtils (since @Value won't inject in unit tests)
        ReflectionTestUtils.setField(verificationService, "incomeTolerance", 5.0);

        user = new User();
        user.setId(1L);
        user.setName("Arya Kadam");

        loan = new LoanApplication();
        loan.setId(100L);
        loan.setUser(user);
        loan.setMonthlyIncome(new BigDecimal("80000"));
        loan.setEmploymentType("Salaried");

        salarySlipDoc = new Document();
        salarySlipDoc.setId(10L);
        salarySlipDoc.setDocumentType(DocumentType.SALARY_SLIP);
        salarySlipDoc.setLoanApplication(loan);

        when(verificationResultRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ─── Income Tests ────────────────────────────────────────────────────────

    @Test
    void income_WithinTolerance_ShouldBeMatch() {
        // 79,000 is within 5% of 80,000 (threshold = 4,000)
        ExtractedDocumentData data = buildSalaryData("Arya Kadam", "79000");
        verificationService.verify(salarySlipDoc, data);

        ArgumentCaptor<List<VerificationResult>> captor = ArgumentCaptor.forClass(List.class);
        verify(verificationResultRepository).saveAll(captor.capture());

        VerificationResult incomeResult = findByField(captor.getValue(), "Monthly Income");
        assertEquals(MatchStatus.MATCH, incomeResult.getMatchStatus());
        assertEquals(VerificationStatus.VERIFIED, salarySlipDoc.getVerificationStatus());
    }

    @Test
    void income_AboveTolerance_ShouldBeMismatch() {
        // 74,000 is more than 5% below 80,000 (diff = 6,000 > threshold 4,000)
        ExtractedDocumentData data = buildSalaryData("Arya Kadam", "74000");
        verificationService.verify(salarySlipDoc, data);

        ArgumentCaptor<List<VerificationResult>> captor = ArgumentCaptor.forClass(List.class);
        verify(verificationResultRepository).saveAll(captor.capture());

        VerificationResult incomeResult = findByField(captor.getValue(), "Monthly Income");
        assertEquals(MatchStatus.MISMATCH, incomeResult.getMatchStatus());
        assertEquals(VerificationStatus.FLAGGED, salarySlipDoc.getVerificationStatus());
    }

    @Test
    void income_ExactMatch_ShouldBeMatch() {
        ExtractedDocumentData data = buildSalaryData("Arya Kadam", "80000");
        verificationService.verify(salarySlipDoc, data);

        ArgumentCaptor<List<VerificationResult>> captor = ArgumentCaptor.forClass(List.class);
        verify(verificationResultRepository).saveAll(captor.capture());

        VerificationResult incomeResult = findByField(captor.getValue(), "Monthly Income");
        assertEquals(MatchStatus.MATCH, incomeResult.getMatchStatus());
    }

    // ─── Name Tests ──────────────────────────────────────────────────────────

    @Test
    void name_ExactMatch_ShouldBeMatch() {
        ExtractedDocumentData data = buildSalaryData("Arya Kadam", "80000");
        verificationService.verify(salarySlipDoc, data);

        ArgumentCaptor<List<VerificationResult>> captor = ArgumentCaptor.forClass(List.class);
        verify(verificationResultRepository).saveAll(captor.capture());

        VerificationResult nameResult = findByField(captor.getValue(), "Employee Name");
        assertEquals(MatchStatus.MATCH, nameResult.getMatchStatus());
    }

    @Test
    void name_Mismatch_ShouldFlagDocument() {
        ExtractedDocumentData data = buildSalaryData("Wrong Name", "80000");
        verificationService.verify(salarySlipDoc, data);

        ArgumentCaptor<List<VerificationResult>> captor = ArgumentCaptor.forClass(List.class);
        verify(verificationResultRepository).saveAll(captor.capture());

        VerificationResult nameResult = findByField(captor.getValue(), "Employee Name");
        assertEquals(MatchStatus.MISMATCH, nameResult.getMatchStatus());
        assertEquals(VerificationStatus.FLAGGED, salarySlipDoc.getVerificationStatus());
    }

    @Test
    void name_CaseInsensitiveMatch_ShouldBeMatch() {
        ExtractedDocumentData data = buildSalaryData("arya kadam", "80000");
        verificationService.verify(salarySlipDoc, data);

        ArgumentCaptor<List<VerificationResult>> captor = ArgumentCaptor.forClass(List.class);
        verify(verificationResultRepository).saveAll(captor.capture());

        VerificationResult nameResult = findByField(captor.getValue(), "Employee Name");
        assertEquals(MatchStatus.MATCH, nameResult.getMatchStatus());
    }

    // ─── Helper Methods ──────────────────────────────────────────────────────

    private ExtractedDocumentData buildSalaryData(String name, String netIncome) {
        ExtractedDocumentData data = new ExtractedDocumentData();
        data.addField("employeeName", name);
        data.addField("netIncome", netIncome);
        data.addField("employer", "Test Corp");
        return data;
    }

    private VerificationResult findByField(List<VerificationResult> results, String field) {
        return results.stream()
                .filter(r -> field.equals(r.getField()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Field '" + field + "' not found in results"));
    }
}
