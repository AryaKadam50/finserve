package com.finserve.service;

import java.util.Map;

public interface AgentToolsService {
    Map<String, Object> getLoanApplication(Long applicationId, String callerRole);
    Map<String, Object> getApplicantFinancialProfile(Long applicationId, String callerRole);
    Map<String, Object> getApplicantLoanHistory(Long applicationId, String callerRole);
    Map<String, Object> calculateDebtToIncomeRatio(Long applicationId, String callerRole);
    Map<String, Object> getDocumentVerificationResults(Long applicationId, String callerRole);
    Map<String, Object> getEligibilityRules();
    Map<String, Object> getLoanPolicy(String loanType);
    Map<String, Object> requestHumanReview(Long applicationId, String reason, String callerRole);
}
