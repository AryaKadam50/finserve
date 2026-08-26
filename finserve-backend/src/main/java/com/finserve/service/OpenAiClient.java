package com.finserve.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin wrapper around the OpenAI Chat Completions API.
 *
 * - API key is read from environment variable OPENAI_API_KEY.
 * - If the key is missing or blank, falls back to a deterministic mock response
 *   so development and tests work without spending API credits.
 * - Never logs the API key or raw prompt content containing personal data.
 */
@Service
public class OpenAiClient implements OpenAiClientService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${ai.openai.timeout.seconds:30}")
    private int timeoutSeconds;

    /**
     * Sends a prompt to OpenAI and returns the assistant's raw text reply.
     * Falls back to mock if OPENAI_API_KEY env var is not set.
     *
     * @param systemPrompt Instructions for the model role.
     * @param userPrompt   The structured underwriting context.
     * @return Raw JSON string from the model.
     */
    public String chat(String systemPrompt, String userPrompt) {
        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            log.info("OPENAI_API_KEY not set — using mock underwriting response");
            return buildMockResponse(userPrompt);
        }

        try {
            String requestBody = buildRequestBody(systemPrompt, userPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_CHAT_URL))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("OpenAI API returned HTTP {}: {}", response.statusCode(), response.body().substring(0, Math.min(200, response.body().length())));
                throw new RuntimeException("OpenAI API error: HTTP " + response.statusCode());
            }

            // Extract the assistant message content from the response
            JsonNode root = mapper.readTree(response.body());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI API call failed: {}", e.getMessage());
            throw new RuntimeException("LLM unavailable: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String systemPrompt, String userPrompt) throws Exception {
        // Build a JSON request body manually to avoid adding extra dependencies
        var requestNode = mapper.createObjectNode();
        requestNode.put("model", model);
        requestNode.put("temperature", 0.2); // Low temperature for consistent structured output
        requestNode.put("max_tokens", 800);

        var messages = mapper.createArrayNode();

        var systemMsg = mapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        var userMsg = mapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        requestNode.set("messages", messages);

        // Ask OpenAI to return JSON
        var responseFormat = mapper.createObjectNode();
        responseFormat.put("type", "json_object");
        requestNode.set("response_format", responseFormat);

        return mapper.writeValueAsString(requestNode);
    }

    /**
     * Returns a deterministic mock response based on simple rules applied to the prompt.
     * Used when OPENAI_API_KEY is not available.
     */
    private String buildMockResponse(String userPrompt) {
        // Parse simple signals from the prompt to produce a realistic mock
        boolean hasMismatch = userPrompt.contains("MISMATCH") || userPrompt.contains("FLAGGED");
        boolean dtiExceeds = userPrompt.contains("dtiExceedsLimit=true") || userPrompt.contains("\"dtiExceedsLimit\":true");
        boolean highIncome = userPrompt.contains("\"monthlyIncome\":") &&
                !userPrompt.contains("\"monthlyIncome\":0");

        String recommendation;
        String riskLevel;
        double confidence;
        String reasons;
        String issues;
        boolean humanReview;

        if (dtiExceeds && hasMismatch) {
            recommendation = "REJECT";
            riskLevel = "HIGH";
            confidence = 0.82;
            reasons = "[\"Debt-to-income ratio exceeds policy limit\",\"Document verification issues detected\",\"Combined risk factors indicate high default probability\"]";
            issues = "[\"Income MISMATCH found in salary slip\",\"DTI ratio exceeds configured threshold\"]";
            humanReview = false;
        } else if (hasMismatch || dtiExceeds) {
            recommendation = "REVIEW";
            riskLevel = "MEDIUM";
            confidence = 0.74;
            reasons = "[\"One or more risk factors require human judgment\",\"Document mismatch or elevated DTI ratio detected\"]";
            issues = hasMismatch ? "[\"Income MISMATCH found in uploaded document — manual review recommended\"]"
                                 : "[\"DTI ratio is elevated — verify applicant's total obligations\"]";
            humanReview = true;
        } else {
            recommendation = "APPROVE";
            riskLevel = "LOW";
            confidence = 0.88;
            reasons = "[\"Income meets policy minimum\",\"DTI ratio within acceptable limits\",\"Documents verified without issues\",\"Employment profile is stable\"]";
            issues = "[]";
            humanReview = false;
        }

        return String.format(
            "{\"applicationId\":0,\"recommendation\":\"%s\",\"riskLevel\":\"%s\",\"confidence\":%.2f,\"reasons\":%s,\"verificationIssues\":%s,\"requiresHumanReview\":%b}",
            recommendation, riskLevel, confidence, reasons, issues, humanReview
        );
    }
}
