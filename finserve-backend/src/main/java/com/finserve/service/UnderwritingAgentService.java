package com.finserve.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finserve.dto.UnderwritingResultDTO;
import com.finserve.exception.BadRequestException;
import com.finserve.model.*;
import com.finserve.repository.UnderwritingResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Orchestrates the AI underwriting pipeline.
 *
 * Responsibilities:
 * 1. Collect structured context via AgentTools (all read-only, admin-enforced).
 * 2. Build a safe prompt (no PII — only financial metrics and policy rules).
 * 3. Call the LLM via OpenAiClient.
 * 4. Validate and parse the structured JSON response.
 * 5. Apply confidence threshold rule in Java (not delegated to the AI).
 * 6. Persist the UnderwritingResult for audit.
 * 7. Return the DTO to the controller.
 *
 * CRITICAL INVARIANT:
 * - This service NEVER modifies LoanApplication.status.
 * - The AI recommendation is advisory only.
 * - If the LLM fails, the loan application is left unchanged.
 */
@Service
public class UnderwritingAgentService {

    private static final Logger log = LoggerFactory.getLogger(UnderwritingAgentService.class);

    private static final String SYSTEM_PROMPT = """
            You are FinServe's AI underwriting assistant. Your job is to analyze loan applications \
            and return a structured risk assessment recommendation.
            
            You will receive:
            - Loan application financial metrics
            - Applicant financial profile
            - Loan history summary
            - Debt-to-income ratio calculation
            - Document verification results
            - Eligibility rules and loan policy
            
            IMPORTANT RULES:
            - You are an ASSISTANT, not the decision maker. A human admin makes the final decision.
            - Base your recommendation ONLY on the data provided. Do not fabricate information.
            - Do NOT recommend approval for applications with DTI above the policy maximum.
            - Flag applications with document mismatches for human review.
            - Be conservative: when uncertain, recommend REVIEW rather than APPROVE or REJECT.
            
            You MUST respond with ONLY a valid JSON object in this exact format:
            {
              "applicationId": <number>,
              "recommendation": "APPROVE" | "REVIEW" | "REJECT",
              "riskLevel": "LOW" | "MEDIUM" | "HIGH",
              "confidence": <0.0 to 1.0>,
              "reasons": ["reason1", "reason2", ...],
              "verificationIssues": ["issue1", ...],
              "policyReferences": [
                { "document": "Doc Name", "section": "Section Name", "relevance": 0.95 }
              ],
              "requiresHumanReview": true | false
            }
            
            IMPORTANT for policyReferences:
            - Only include references if you used them to make your decision.
            - Do NOT fabricate references. Use only the exact Document and Section names provided in the RETRIEVED POLICY CONTEXT.
            """;

    private final AgentToolsService agentTools;
    private final OpenAiClientService openAiClient;
    private final UnderwritingResultRepository underwritingResultRepository;
    private final SimpleVectorStoreService vectorStore;
    private final OpenAiEmbeddingClientService embeddingClient;
    private final com.finserve.repository.AuditEventRepository auditEventRepository;
    private final com.finserve.repository.LoanRepository loanRepository;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${ai.underwriting.confidence.threshold:0.70}")
    private double confidenceThreshold;

    public UnderwritingAgentService(AgentToolsService agentTools,
                                    OpenAiClientService openAiClient,
                                    UnderwritingResultRepository underwritingResultRepository,
                                    SimpleVectorStoreService vectorStore,
                                    OpenAiEmbeddingClientService embeddingClient,
                                    com.finserve.repository.AuditEventRepository auditEventRepository,
                                    com.finserve.repository.LoanRepository loanRepository) {
        this.agentTools = agentTools;
        this.openAiClient = openAiClient;
        this.underwritingResultRepository = underwritingResultRepository;
        this.vectorStore = vectorStore;
        this.embeddingClient = embeddingClient;
        this.auditEventRepository = auditEventRepository;
        this.loanRepository = loanRepository;
    }

    /**
     * Runs the full underwriting agent pipeline for a loan application.
     *
     * @param applicationId  The loan application to analyze.
     * @param callerRole     Must be ADMIN — enforced by AgentTools.
     * @return UnderwritingResultDTO (recommendation + audit info).
     */
    @org.springframework.transaction.annotation.Transactional
    public UnderwritingResultDTO analyze(Long applicationId, String callerRole) {
        log.info("Starting underwriting analysis for applicationId={}", applicationId);

        // ── Step 1: Collect structured context via agent tools ──────────────
        Map<String, Object> loanData        = agentTools.getLoanApplication(applicationId, callerRole);
        Map<String, Object> financialProfile = agentTools.getApplicantFinancialProfile(applicationId, callerRole);
        Map<String, Object> loanHistory     = agentTools.getApplicantLoanHistory(applicationId, callerRole);
        Map<String, Object> dtiData         = agentTools.calculateDebtToIncomeRatio(applicationId, callerRole);
        Map<String, Object> docVerification = agentTools.getDocumentVerificationResults(applicationId, callerRole);
        Map<String, Object> eligibility     = agentTools.getEligibilityRules();

        // ── RAG Step: Formulate Query and Retrieve Policy Context ────────────
        String query = String.format("Applicant DTI %s, Credit Score %s, Employment %s, Income %s", 
                dtiData.get("debtToIncomeRatio"), 
                financialProfile.get("creditScore"), 
                financialProfile.get("employmentType"), 
                financialProfile.get("monthlyIncome"));
                
        float[] queryVector = embeddingClient.getEmbedding(query);
        java.util.List<SimpleVectorStore.SearchResult> retrievedPolicies = vectorStore.search(queryVector, 3);
        
        StringBuilder policyContextBuilder = new StringBuilder();
        for (SimpleVectorStore.SearchResult res : retrievedPolicies) {
            policyContextBuilder.append(String.format("Document: %s | Section: %s | Relevance: %.2f\n%s\n\n",
                    res.getChunk().getDocumentName(), res.getChunk().getSectionName(), res.getScore(), res.getChunk().getContent()));
        }
        String retrievedPolicyContext = policyContextBuilder.toString();

        // ── Step 2: Build the user prompt from collected context ─────────────
        String userPrompt = buildPrompt(applicationId, loanData, financialProfile, loanHistory,
                dtiData, docVerification, eligibility, retrievedPolicyContext);

        // ── Step 3: Call the LLM ─────────────────────────────────────────────
        String rawResponse;
        try {
            rawResponse = openAiClient.chat(SYSTEM_PROMPT, userPrompt);
        } catch (Exception e) {
            log.error("LLM call failed for applicationId={}: {}", applicationId, e.getMessage());
            
            // Graceful Degradation
            LoanApplication loan = loanRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
            loan.setStatus(LoanStatus.PENDING_HUMAN_REVIEW);
            loanRepository.save(loan);
            
            auditEventRepository.save(new com.finserve.model.AuditEvent(
                    applicationId, 
                    "AI_FAILURE", 
                    "AI underwriting service unavailable. Application flagged for manual review.", 
                    null, 
                    "System"
            ));
            
            throw new RuntimeException("AI underwriting service unavailable. The application is unchanged and available for manual review.", e);
        }

        // ── Step 4: Parse & validate structured response ──────────────────────
        UnderwritingResult result = parseAndValidate(rawResponse, applicationId);

        // ── Step 5: Apply confidence threshold rule in Java ───────────────────
        if (result.getConfidence() < confidenceThreshold) {
            log.info("Confidence {:.2f} below threshold {} — forcing human review for applicationId={}",
                    result.getConfidence(), confidenceThreshold, applicationId);
            result.setRequiresHumanReview(true);
            if (result.getRecommendation() != UnderwritingRecommendation.REJECT) {
                result.setRecommendation(UnderwritingRecommendation.REVIEW);
            }
        }

        // Force human review if documents have issues
        boolean docsHaveIssues = Boolean.TRUE.equals(docVerification.get("anyFlagged"))
                || Boolean.TRUE.equals(docVerification.get("anyFailed"));
        if (docsHaveIssues && !Boolean.TRUE.equals(result.getRequiresHumanReview())) {
            log.info("Document issues detected — forcing human review for applicationId={}", applicationId);
            result.setRequiresHumanReview(true);
        }

        // ── Step 6: Persist for audit and update Loan Status ───────────────────
        result.setAiModel(model);
        UnderwritingResult saved = underwritingResultRepository.save(result);
        log.info("Underwriting result persisted id={} for applicationId={} recommendation={}",
                saved.getId(), applicationId, saved.getRecommendation());

        // Update Loan Status and record Audit Event
        LoanApplication loan = loanRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        
        LoanStatus newStatus = result.getRequiresHumanReview() ? LoanStatus.PENDING_HUMAN_REVIEW : LoanStatus.AI_RECOMMENDED;
        loan.setStatus(newStatus);
        loanRepository.save(loan);

        String desc = String.format("AI Analysis complete. Recommendation: %s (Risk: %s, Confidence: %.0f%%). %s",
                result.getRecommendation(), result.getRiskLevel(), result.getConfidence() * 100,
                result.getRequiresHumanReview() ? "Flagged for human review." : "Ready for admin decision.");
                
        auditEventRepository.save(new com.finserve.model.AuditEvent(
                applicationId, 
                "AI_UNDERWRITING_COMPLETED", 
                desc, 
                null, 
                "AI Agent"
        ));

        return UnderwritingResultDTO.fromEntity(saved);
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private String buildPrompt(Long appId, Map<String, Object> loan, Map<String, Object> financial,
                                Map<String, Object> history, Map<String, Object> dti,
                                Map<String, Object> docs, Map<String, Object> eligibility,
                                String policyContext) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("APPLICATION ID: ").append(appId).append("\n\n");
            sb.append("=== LOAN DETAILS ===\n").append(mapper.writeValueAsString(loan)).append("\n\n");
            sb.append("=== FINANCIAL PROFILE ===\n").append(mapper.writeValueAsString(financial)).append("\n\n");
            sb.append("=== LOAN HISTORY ===\n").append(mapper.writeValueAsString(history)).append("\n\n");
            sb.append("=== DEBT-TO-INCOME ANALYSIS ===\n").append(mapper.writeValueAsString(dti)).append("\n\n");
            sb.append("=== DOCUMENT VERIFICATION ===\n").append(mapper.writeValueAsString(docs)).append("\n\n");
            sb.append("=== ELIGIBILITY RULES ===\n").append(mapper.writeValueAsString(eligibility)).append("\n\n");
            sb.append("=== RETRIEVED POLICY CONTEXT ===\n").append(policyContext).append("\n\n");
            sb.append("Provide your underwriting recommendation as a JSON object.");
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build underwriting prompt", e);
        }
    }

    private UnderwritingResult parseAndValidate(String rawResponse, Long applicationId) {
        try {
            JsonNode root = mapper.readTree(rawResponse);

            String recommendationStr = root.path("recommendation").asText();
            String riskLevelStr = root.path("riskLevel").asText();
            double confidence = root.path("confidence").asDouble(-1);

            // Validate required fields
            if (recommendationStr.isBlank() || riskLevelStr.isBlank() || confidence < 0) {
                throw new IllegalArgumentException("AI response missing required fields: recommendation, riskLevel, or confidence");
            }

            UnderwritingRecommendation recommendation;
            try {
                recommendation = UnderwritingRecommendation.valueOf(recommendationStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid recommendation value: " + recommendationStr);
            }

            RiskLevel riskLevel;
            try {
                riskLevel = RiskLevel.valueOf(riskLevelStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid riskLevel value: " + riskLevelStr);
            }

            // Clamp confidence to [0, 1]
            confidence = Math.max(0.0, Math.min(1.0, confidence));

            String reasonsJson = mapper.writeValueAsString(root.path("reasons"));
            String issuesJson = mapper.writeValueAsString(root.path("verificationIssues"));
            String policyRefJson = mapper.writeValueAsString(root.path("policyReferences"));
            boolean humanReview = root.path("requiresHumanReview").asBoolean(false);

            UnderwritingResult result = new UnderwritingResult();
            result.setApplicationId(applicationId);
            result.setRecommendation(recommendation);
            result.setRiskLevel(riskLevel);
            result.setConfidence(confidence);
            result.setReasons(reasonsJson);
            result.setVerificationIssues(issuesJson);
            result.setPolicyReferences(policyRefJson);
            result.setRequiresHumanReview(humanReview);

            return result;

        } catch (IllegalArgumentException e) {
            log.error("AI response validation failed: {}", e.getMessage());
            throw new BadRequestException("AI returned invalid structured response: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            throw new RuntimeException("Malformed AI response — could not parse JSON: " + e.getMessage(), e);
        }
    }
}
