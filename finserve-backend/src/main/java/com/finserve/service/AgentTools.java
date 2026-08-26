package com.finserve.service;

import com.finserve.exception.BadRequestException;
import com.finserve.exception.ResourceNotFoundException;
import com.finserve.model.*;
import com.finserve.repository.DocumentRepository;
import com.finserve.repository.LoanRepository;
import com.finserve.repository.VerificationResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlled, read-only tool set for the underwriting agent.
 *
 * Key constraints:
 * - All methods are READ ONLY. No entity writes happen here.
 * - Each method returns simple data maps (no JPA entities exposed to the LLM layer).
 * - Every method that touches user data enforces that the caller is ADMIN.
 * - The agent CANNOT call these methods directly — it receives pre-collected context.
 */
@Service
public class AgentTools implements AgentToolsService {

    private static final Logger log = LoggerFactory.getLogger(AgentTools.class);

    private final LoanRepository loanRepository;
    private final DocumentRepository documentRepository;
    private final VerificationResultRepository verificationResultRepository;

    @Value("${policy.min.credit.score:650}")
    private int minCreditScore;

    @Value("${policy.max.dti.ratio:0.45}")
    private double maxDtiRatio;

    @Value("${policy.min.income:50000}")
    private double minIncome;

    @Value("${policy.max.loan.to.income.multiplier:60}")
    private int maxLoanToIncomeMultiplier;

    public AgentTools(LoanRepository loanRepository,
                      DocumentRepository documentRepository,
                      VerificationResultRepository verificationResultRepository) {
        this.loanRepository = loanRepository;
        this.documentRepository = documentRepository;
        this.verificationResultRepository = verificationResultRepository;
    }

    /**
     * Tool 1: Get core loan application data.
     * Returns only financial metadata — no personal identifiers.
     */
    public Map<String, Object> getLoanApplication(Long applicationId, String callerRole) {
        enforceAdmin(callerRole);
        LoanApplication loan = findLoan(applicationId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applicationId", loan.getId());
        result.put("requestedAmount", loan.getAmount());
        result.put("tenureMonths", loan.getTenure());
        result.put("purpose", loan.getPurpose());
        result.put("status", loan.getStatus());
        result.put("createdAt", loan.getCreatedAt());
        return result;
    }

    /**
     * Tool 2: Get applicant financial profile (no PII — only financial numbers).
     */
    public Map<String, Object> getApplicantFinancialProfile(Long applicationId, String callerRole) {
        enforceAdmin(callerRole);
        LoanApplication loan = findLoan(applicationId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("monthlyIncome", loan.getMonthlyIncome());
        result.put("monthlyExpenses", loan.getMonthlyExpenses());
        result.put("existingMonthlyEmi", loan.getExistingMonthlyEmi() != null ? loan.getExistingMonthlyEmi() : BigDecimal.ZERO);
        result.put("existingLoanCount", loan.getExistingLoanCount() != null ? loan.getExistingLoanCount() : 0);
        result.put("employmentType", loan.getEmploymentType());
        result.put("yearsOfEmployment", loan.getYearsOfEmployment());
        result.put("creditScore", loan.getCreditScore());
        return result;
    }

    /**
     * Tool 3: Get applicant loan history (all previous applications for same user).
     * Returns only financial aggregates — no personal data.
     */
    public Map<String, Object> getApplicantLoanHistory(Long applicationId, String callerRole) {
        enforceAdmin(callerRole);
        LoanApplication loan = findLoan(applicationId);
        Long userId = loan.getUser().getId();

        List<LoanApplication> history = loanRepository.findByUserId(userId);
        long totalLoans = history.size();
        long approvedLoans = history.stream().filter(l -> l.getStatus() == LoanStatus.APPROVED).count();
        long rejectedLoans = history.stream().filter(l -> l.getStatus() == LoanStatus.REJECTED).count();
        BigDecimal totalApprovedAmount = history.stream()
                .filter(l -> l.getStatus() == LoanStatus.APPROVED)
                .map(LoanApplication::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalApplications", totalLoans);
        result.put("approvedLoans", approvedLoans);
        result.put("rejectedLoans", rejectedLoans);
        result.put("totalApprovedAmount", totalApprovedAmount);
        return result;
    }

    /**
     * Tool 4: Calculate Debt-to-Income ratio deterministically in Java.
     * DTI = (existingEMI + estimatedNewEMI) / monthlyIncome
     * New EMI estimated as: principal * monthlyRate / (1 - (1+r)^-n)
     */
    public Map<String, Object> calculateDebtToIncomeRatio(Long applicationId, String callerRole) {
        enforceAdmin(callerRole);
        LoanApplication loan = findLoan(applicationId);

        BigDecimal income = loan.getMonthlyIncome() != null ? loan.getMonthlyIncome() : BigDecimal.ZERO;
        BigDecimal existingEmi = loan.getExistingMonthlyEmi() != null ? loan.getExistingMonthlyEmi() : BigDecimal.ZERO;
        BigDecimal newEmi = estimateMonthlyEmi(loan.getAmount(), loan.getTenure());
        BigDecimal totalObligations = existingEmi.add(newEmi);

        double dti = income.compareTo(BigDecimal.ZERO) == 0 ? 1.0
                : totalObligations.divide(income, 4, RoundingMode.HALF_UP).doubleValue();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("existingMonthlyEmi", existingEmi);
        result.put("estimatedNewEmi", newEmi);
        result.put("totalMonthlyObligations", totalObligations);
        result.put("monthlyIncome", income);
        result.put("debtToIncomeRatio", Math.round(dti * 10000.0) / 10000.0);
        result.put("maxAllowedDti", maxDtiRatio);
        result.put("dtiExceedsLimit", dti > maxDtiRatio);
        return result;
    }

    /**
     * Tool 5: Get document verification results for this application.
     */
    public Map<String, Object> getDocumentVerificationResults(Long applicationId, String callerRole) {
        enforceAdmin(callerRole);
        findLoan(applicationId); // Validates the loan exists

        List<Document> docs = documentRepository.findByLoanApplicationId(applicationId);
        boolean anyFlagged = docs.stream().anyMatch(d -> d.getVerificationStatus() == VerificationStatus.FLAGGED);
        boolean anyFailed = docs.stream().anyMatch(d -> d.getVerificationStatus() == VerificationStatus.FAILED);
        boolean allVerified = !docs.isEmpty() && docs.stream().allMatch(d -> d.getVerificationStatus() == VerificationStatus.VERIFIED);

        List<Map<String, Object>> docSummaries = docs.stream().map(doc -> {
            List<VerificationResult> vrs = verificationResultRepository.findByDocumentId(doc.getId());
            List<Map<String, String>> vrSummaries = vrs.stream().map(vr -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("field", vr.getField());
                m.put("status", vr.getMatchStatus().name());
                m.put("declared", vr.getDeclaredValue());
                m.put("extracted", vr.getExtractedValue());
                return m;
            }).collect(Collectors.toList());

            Map<String, Object> docMap = new LinkedHashMap<>();
            docMap.put("documentType", doc.getDocumentType().name());
            docMap.put("verificationStatus", doc.getVerificationStatus().name());
            docMap.put("fieldResults", vrSummaries);
            return docMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalDocuments", docs.size());
        result.put("anyFlagged", anyFlagged);
        result.put("anyFailed", anyFailed);
        result.put("allVerified", allVerified);
        result.put("documents", docSummaries);
        return result;
    }

    /**
     * Tool 6: Get configurable eligibility rules (from application.properties).
     */
    public Map<String, Object> getEligibilityRules() {
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("minimumMonthlyIncome", minIncome);
        rules.put("minimumCreditScore", minCreditScore);
        rules.put("maximumDebtToIncomeRatio", maxDtiRatio);
        rules.put("maximumLoanToIncomeMultiplier", maxLoanToIncomeMultiplier);
        return rules;
    }

    /**
     * Tool 7: Get loan policy for the given loan type/purpose.
     * Returns static policy text for context injection into the prompt.
     */
    public Map<String, Object> getLoanPolicy(String loanType) {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("loanType", loanType != null ? loanType : "GENERAL");
        policy.put("policies", List.of(
            "Income must meet the minimum threshold defined in eligibility rules.",
            "Debt-to-income ratio must not exceed the configured maximum.",
            "Credit score (if provided) should meet the minimum threshold.",
            "Documents flagged with MISMATCH require heightened scrutiny.",
            "Self-employed applicants require at least 2 years of documented employment.",
            "Applicants with more than 3 existing loans should be escalated for human review.",
            "Loan amount must not exceed the configured income multiplier limit.",
            "First-time applicants with no loan history should be approved cautiously."
        ));
        return policy;
    }

    /**
     * Tool 8: Request human review — records the reason without changing loan status.
     * Returns a structured marker that the agent can reference.
     */
    public Map<String, Object> requestHumanReview(Long applicationId, String reason, String callerRole) {
        enforceAdmin(callerRole);
        findLoan(applicationId); // validates loan exists
        log.info("Human review requested for application {} — reason: {}", applicationId, reason);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applicationId", applicationId);
        result.put("humanReviewRequested", true);
        result.put("reason", reason);
        return result;
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private void enforceAdmin(String callerRole) {
        if (!"ADMIN".equals(callerRole)) {
            throw new BadRequestException("Only admins can perform underwriting analysis");
        }
    }

    private LoanApplication findLoan(Long applicationId) {
        return loanRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", "id", applicationId));
    }

    /**
     * Standard EMI formula: EMI = P * r * (1+r)^n / ((1+r)^n - 1)
     * Assumes 12% per annum interest rate for estimation purposes.
     */
    private BigDecimal estimateMonthlyEmi(BigDecimal principal, Integer tenureMonths) {
        if (principal == null || tenureMonths == null || tenureMonths == 0) return BigDecimal.ZERO;
        double p = principal.doubleValue();
        double r = 0.12 / 12; // 12% annual → monthly
        int n = tenureMonths;
        double emi = (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
        return BigDecimal.valueOf(Math.round(emi));
    }
}
