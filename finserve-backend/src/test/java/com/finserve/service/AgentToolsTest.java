package com.finserve.service;

import com.finserve.exception.BadRequestException;
import com.finserve.model.*;
import com.finserve.repository.DocumentRepository;
import com.finserve.repository.LoanRepository;
import com.finserve.repository.VerificationResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentToolsTest {

    @Mock private LoanRepository loanRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private VerificationResultRepository verificationResultRepository;

    @InjectMocks
    private AgentTools agentTools;

    private LoanApplication loan;
    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(agentTools, "minCreditScore", 650);
        ReflectionTestUtils.setField(agentTools, "maxDtiRatio", 0.45);
        ReflectionTestUtils.setField(agentTools, "minIncome", 50000.0);
        ReflectionTestUtils.setField(agentTools, "maxLoanToIncomeMultiplier", 60);

        user = new User();
        user.setId(1L);
        user.setName("Test User");

        loan = new LoanApplication();
        loan.setId(1L);
        loan.setUser(user);
        loan.setAmount(new BigDecimal("500000"));
        loan.setTenure(24);
        loan.setMonthlyIncome(new BigDecimal("80000"));
        loan.setMonthlyExpenses(new BigDecimal("20000"));
        loan.setExistingMonthlyEmi(new BigDecimal("5000"));
        loan.setExistingLoanCount(1);
        loan.setEmploymentType("Salaried");
        loan.setYearsOfEmployment(3);
        loan.setCreditScore(720);
        loan.setPurpose("Home Renovation");
    }

    // ─── Authorization Tests ─────────────────────────────────────────────────

    @Test
    void nonAdmin_getLoanApplication_ThrowsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            agentTools.getLoanApplication(1L, "USER"));
    }

    @Test
    void nonAdmin_getFinancialProfile_ThrowsBadRequest() {
        assertThrows(BadRequestException.class, () ->
            agentTools.getApplicantFinancialProfile(1L, "USER"));
    }

    // ─── DTI Calculation Tests ────────────────────────────────────────────────

    @Test
    void dtiCalculation_WithinLimit_IsNotExceeded() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        // EMI for 500,000 at 12% p.a. over 24 months ≈ 23,537
        // existingEMI = 5,000 → total ≈ 28,537
        // DTI = 28,537 / 80,000 ≈ 0.357 < 0.45
        Map<String, Object> result = agentTools.calculateDebtToIncomeRatio(1L, "ADMIN");

        assertNotNull(result);
        assertNotNull(result.get("debtToIncomeRatio"));
        double dti = (Double) result.get("debtToIncomeRatio");
        assertEquals(false, result.get("dtiExceedsLimit"), "DTI should not exceed limit for this loan");
        assertTrue(dti > 0 && dti < 0.45, "DTI should be between 0 and the limit");
    }

    @Test
    void dtiCalculation_ExceedingLimit_IsFlagged() {
        // Large loan with small income → high DTI
        loan.setAmount(new BigDecimal("3000000")); // 30 lakh
        loan.setMonthlyIncome(new BigDecimal("30000")); // small income
        loan.setExistingMonthlyEmi(new BigDecimal("10000"));

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        Map<String, Object> result = agentTools.calculateDebtToIncomeRatio(1L, "ADMIN");

        assertEquals(true, result.get("dtiExceedsLimit"), "DTI should exceed limit for this scenario");
    }

    // ─── Tool Data Tests ──────────────────────────────────────────────────────

    @Test
    void getFinancialProfile_ReturnsExpectedFields() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        Map<String, Object> profile = agentTools.getApplicantFinancialProfile(1L, "ADMIN");

        assertNotNull(profile.get("monthlyIncome"));
        assertNotNull(profile.get("creditScore"));
        assertNotNull(profile.get("employmentType"));
        assertNotNull(profile.get("yearsOfEmployment"));
        // Must NOT contain personal identifiers
        assertFalse(profile.containsKey("name"), "Financial profile must not contain name");
        assertFalse(profile.containsKey("email"), "Financial profile must not contain email");
    }

    @Test
    void getLoanHistory_ReturnsAggregates() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.findByUserId(1L)).thenReturn(List.of(loan));

        Map<String, Object> history = agentTools.getApplicantLoanHistory(1L, "ADMIN");

        assertNotNull(history.get("totalApplications"));
        assertNotNull(history.get("approvedLoans"));
        assertNotNull(history.get("rejectedLoans"));
        assertEquals(1L, history.get("totalApplications"));
    }

    @Test
    void getEligibilityRules_ReturnsAllConfiguredRules() {
        Map<String, Object> rules = agentTools.getEligibilityRules();

        assertEquals(50000.0, rules.get("minimumMonthlyIncome"));
        assertEquals(650, rules.get("minimumCreditScore"));
        assertEquals(0.45, rules.get("maximumDebtToIncomeRatio"));
        assertEquals(60, rules.get("maximumLoanToIncomeMultiplier"));
    }

    @Test
    void getDocumentVerificationResults_NoDocuments_ReturnsEmptySummary() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(documentRepository.findByLoanApplicationId(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = agentTools.getDocumentVerificationResults(1L, "ADMIN");

        assertEquals(0, result.get("totalDocuments"));
        assertEquals(false, result.get("anyFlagged"));
        assertEquals(false, result.get("allVerified"));
    }

    @Test
    void requestHumanReview_Admin_Succeeds() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        Map<String, Object> result = agentTools.requestHumanReview(1L, "Income data inconsistent", "ADMIN");

        assertEquals(1L, result.get("applicationId"));
        assertEquals(true, result.get("humanReviewRequested"));
        assertEquals("Income data inconsistent", result.get("reason"));
    }
}
