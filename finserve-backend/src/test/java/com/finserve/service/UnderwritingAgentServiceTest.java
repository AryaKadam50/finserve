package com.finserve.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finserve.dto.UnderwritingResultDTO;
import com.finserve.model.UnderwritingRecommendation;
import com.finserve.model.UnderwritingResult;
import com.finserve.repository.UnderwritingResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UnderwritingAgentServiceTest {

    @Mock private AgentToolsService agentTools;
    @Mock private OpenAiClientService openAiClient;
    @Mock private UnderwritingResultRepository underwritingResultRepository;
    @Mock private SimpleVectorStoreService vectorStore;
    @Mock private OpenAiEmbeddingClientService embeddingClient;
    @Mock private com.finserve.repository.AuditEventRepository auditEventRepository;
    @Mock private com.finserve.repository.LoanRepository loanRepository;

    @InjectMocks
    private UnderwritingAgentService underwritingAgentService;

    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underwritingAgentService, "confidenceThreshold", 0.70);
        ReflectionTestUtils.setField(underwritingAgentService, "model", "gpt-4o-mini");

        // Mock basic tool responses so the prompt builder doesn't fail
        lenient().when(agentTools.getLoanApplication(anyLong(), anyString())).thenReturn(new HashMap<>());
        lenient().when(agentTools.getApplicantFinancialProfile(anyLong(), anyString())).thenReturn(new HashMap<>());
        lenient().when(agentTools.getApplicantLoanHistory(anyLong(), anyString())).thenReturn(new HashMap<>());
        lenient().when(agentTools.calculateDebtToIncomeRatio(anyLong(), anyString())).thenReturn(new HashMap<>());
        lenient().when(agentTools.getEligibilityRules()).thenReturn(new HashMap<>());
        lenient().when(agentTools.getLoanPolicy(anyString())).thenReturn(new HashMap<>());

        // Mock RAG behavior
        lenient().when(embeddingClient.getEmbedding(anyString())).thenReturn(new float[1536]);
        lenient().when(vectorStore.search(any(), anyInt())).thenReturn(new java.util.ArrayList<>());

        lenient().when(underwritingResultRepository.save(any(UnderwritingResult.class)))
                .thenAnswer(inv -> {
                    UnderwritingResult res = inv.getArgument(0);
                    res.setId(999L);
                    res.setCreatedAt(java.time.LocalDateTime.now());
                    return res;
                });
        lenient().when(loanRepository.findById(anyLong())).thenReturn(java.util.Optional.of(new com.finserve.model.LoanApplication()));
    }

    @Test
    void analyze_ValidResponse_HighConfidence_NoDocIssues_Succeeds() {
        // Setup mock doc verification with no issues
        Map<String, Object> docs = new HashMap<>();
        docs.put("anyFlagged", false);
        docs.put("anyFailed", false);
        when(agentTools.getDocumentVerificationResults(1L, "ADMIN")).thenReturn(docs);

        // Setup mock LLM response
        String llmResponse = """
            {
              "applicationId": 1,
              "recommendation": "APPROVE",
              "riskLevel": "LOW",
              "confidence": 0.85,
              "reasons": ["Good income", "Low DTI"],
              "verificationIssues": [],
              "requiresHumanReview": false
            }
            """;
        when(openAiClient.chat(anyString(), anyString())).thenReturn(llmResponse);

        UnderwritingResultDTO result = underwritingAgentService.analyze(1L, "ADMIN");

        assertNotNull(result);
        assertEquals(UnderwritingRecommendation.APPROVE, result.getRecommendation());
        assertEquals(0.85, result.getConfidence());
        assertEquals(false, result.getRequiresHumanReview());
        assertEquals("gpt-4o-mini", result.getAiModel());
    }

    @Test
    void analyze_LowConfidence_ForcesHumanReview() {
        Map<String, Object> docs = new HashMap<>();
        docs.put("anyFlagged", false);
        docs.put("anyFailed", false);
        when(agentTools.getDocumentVerificationResults(1L, "ADMIN")).thenReturn(docs);

        // AI says APPROVE, but confidence is 0.65 (below 0.70 threshold)
        String llmResponse = """
            {
              "applicationId": 1,
              "recommendation": "APPROVE",
              "riskLevel": "MEDIUM",
              "confidence": 0.65,
              "reasons": ["Borderline income"],
              "verificationIssues": [],
              "requiresHumanReview": false
            }
            """;
        when(openAiClient.chat(anyString(), anyString())).thenReturn(llmResponse);

        UnderwritingResultDTO result = underwritingAgentService.analyze(1L, "ADMIN");

        // The service should override APPROVE to REVIEW and force human review
        assertEquals(UnderwritingRecommendation.REVIEW, result.getRecommendation());
        assertEquals(true, result.getRequiresHumanReview());
    }

    @Test
    void analyze_DocumentIssues_ForcesHumanReview() {
        // Setup mock doc verification with flagged documents
        Map<String, Object> docs = new HashMap<>();
        docs.put("anyFlagged", true);
        docs.put("anyFailed", false);
        when(agentTools.getDocumentVerificationResults(1L, "ADMIN")).thenReturn(docs);

        // AI says APPROVE with high confidence and doesn't set requiresHumanReview
        String llmResponse = """
            {
              "applicationId": 1,
              "recommendation": "APPROVE",
              "riskLevel": "LOW",
              "confidence": 0.90,
              "reasons": ["Looks good"],
              "verificationIssues": [],
              "requiresHumanReview": false
            }
            """;
        when(openAiClient.chat(anyString(), anyString())).thenReturn(llmResponse);

        UnderwritingResultDTO result = underwritingAgentService.analyze(1L, "ADMIN");

        // The service should force human review because of document issues
        assertEquals(true, result.getRequiresHumanReview());
        // Recommendation is untouched if it was already high confidence, just flagged for review.
    }

    @Test
    void analyze_MalformedJsonResponse_ThrowsException() {
        Map<String, Object> docs = new HashMap<>();
        docs.put("anyFlagged", false);
        docs.put("anyFailed", false);
        lenient().when(agentTools.getDocumentVerificationResults(1L, "ADMIN")).thenReturn(docs);

        // Missing riskLevel
        String llmResponse = """
            {
              "applicationId": 1,
              "recommendation": "APPROVE",
              "confidence": 0.90
            }
            """;
        when(openAiClient.chat(anyString(), anyString())).thenReturn(llmResponse);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            underwritingAgentService.analyze(1L, "ADMIN");
        });

        assertTrue(exception.getMessage().contains("invalid structured response"));
    }

    @Test
    void analyze_LlmCallFails_PropagatesException() {
        Map<String, Object> docs = new HashMap<>();
        docs.put("anyFlagged", false);
        docs.put("anyFailed", false);
        lenient().when(agentTools.getDocumentVerificationResults(1L, "ADMIN")).thenReturn(docs);

        when(openAiClient.chat(anyString(), anyString())).thenThrow(new RuntimeException("API Timeout"));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            underwritingAgentService.analyze(1L, "ADMIN");
        });

        assertTrue(exception.getMessage().contains("AI underwriting service unavailable"));
    }
}
