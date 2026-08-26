package com.finserve.controller;

import com.finserve.dto.ApiResponse;
import com.finserve.dto.UnderwritingResultDTO;
import com.finserve.exception.BadRequestException;
import com.finserve.service.UnderwritingAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/underwriting")
public class UnderwritingController {

    private static final Logger log = LoggerFactory.getLogger(UnderwritingController.class);

    private final UnderwritingAgentService underwritingAgentService;

    public UnderwritingController(UnderwritingAgentService underwritingAgentService) {
        this.underwritingAgentService = underwritingAgentService;
    }

    /**
     * POST /api/underwriting/{applicationId}/analyze
     *
     * Runs the AI underwriting agent for a loan application.
     * Restricted to ADMIN role only.
     * The AI result is advisory — it does NOT change loan status.
     */
    @PostMapping("/{applicationId}/analyze")
    public ResponseEntity<ApiResponse<UnderwritingResultDTO>> analyze(
            @PathVariable Long applicationId,
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @RequestHeader(value = "X-User-Role", required = true) String userRole) {

        if (!"ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Only admins can perform underwriting analysis"));
        }

        log.info("Underwriting analysis requested for applicationId={} by userId={}", applicationId, userId);

        try {
            UnderwritingResultDTO result = underwritingAgentService.analyze(applicationId, userRole);
            return ResponseEntity.ok(ApiResponse.success("Underwriting analysis complete", result));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Underwriting failed for applicationId={}: {}", applicationId, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("AI underwriting service unavailable. Application is unchanged and available for manual review."));
        }
    }
}
