package com.finserve.service;

import com.finserve.dto.EligibilityRequest;
import com.finserve.dto.EligibilityResponse;
import com.finserve.dto.LoanApplicationRequest;
import com.finserve.dto.LoanStatusUpdateRequest;
import com.finserve.exception.ResourceNotFoundException;
import com.finserve.model.LoanApplication;
import com.finserve.model.LoanStatus;
import com.finserve.model.User;
import com.finserve.repository.LoanRepository;
import com.finserve.repository.UserRepository;
import com.finserve.dto.AdminDecisionRequest;
import com.finserve.model.AuditEvent;
import com.finserve.model.UnderwritingResult;
import com.finserve.exception.BadRequestException;
import com.finserve.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finserve.dto.LoanApplicationResponseDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;
    private final com.finserve.repository.UnderwritingResultRepository underwritingResultRepository;

    public LoanService(LoanRepository loanRepository, UserRepository userRepository,
                       AuditEventRepository auditEventRepository,
                       com.finserve.repository.UnderwritingResultRepository underwritingResultRepository) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.auditEventRepository = auditEventRepository;
        this.underwritingResultRepository = underwritingResultRepository;
    }

    /**
     * Applies for a new loan
     * @param request The loan application details
     * @return LoanApplication entity
     */
    @Transactional
    public LoanApplicationResponseDTO applyForLoan(LoanApplicationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        LoanApplication loan = new LoanApplication();
        loan.setUser(user);
        loan.setAmount(request.getAmount());
        loan.setTenure(request.getTenure());
        loan.setMonthlyIncome(request.getMonthlyIncome());
        
        // Map new fields
        loan.setMonthlyExpenses(request.getMonthlyExpenses());
        loan.setExistingMonthlyEmi(request.getExistingMonthlyEmi());
        loan.setExistingLoanCount(request.getExistingLoanCount());
        loan.setYearsOfEmployment(request.getYearsOfEmployment());
        loan.setCreditScore(request.getCreditScore());
        
        loan.setEmploymentType(request.getEmploymentType());
        loan.setPurpose(request.getPurpose());

        if (request.getMonthlyIncome().compareTo(new BigDecimal("50000")) >= 0) {
            loan.setStatus(LoanStatus.PENDING);
        } else {
            loan.setStatus(LoanStatus.UNDER_REVIEW);
        }

        LoanApplication saved = loanRepository.save(loan);
        
        auditEventRepository.save(new AuditEvent(
                saved.getId(), 
                "LOAN_SUBMITTED", 
                "Customer submitted loan application", 
                user.getId(), 
                user.getName()
        ));

        return LoanApplicationResponseDTO.fromEntity(saved);
    }

    /**
     * Retrieves all loans ordered by creation date
     * @return List of LoanApplication
     */
    public List<LoanApplicationResponseDTO> getAllLoans() {
        return loanRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(LoanApplicationResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a loan by ID
     * @param id The loan ID
     * @return LoanApplication entity
     */
    public LoanApplicationResponseDTO getLoanById(Long id) {
        LoanApplication loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", "id", id));
        return LoanApplicationResponseDTO.fromEntity(loan);
    }
    
    /**
     * Retrieves a loan entity by ID (internal use)
     */
    private LoanApplication getLoanEntityById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LoanApplication", "id", id));
    }

    /**
     * Retrieves loans for a specific user
     * @param userId The user ID
     * @return List of LoanApplication
     */
    public List<LoanApplicationResponseDTO> getUserLoans(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        return loanRepository.findByUserId(userId).stream()
                .map(LoanApplicationResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Updates the status of a loan via admin decision, enforcing AI override rules.
     * @param id The loan ID
     * @param request The AdminDecisionRequest
     * @param adminId Admin making the decision
     * @param adminName Admin's name
     * @return Updated LoanApplication entity
     */
    @Transactional
    public LoanApplicationResponseDTO updateLoanStatus(Long id, AdminDecisionRequest request, Long adminId, String adminName) {
        LoanApplication loan = getLoanEntityById(id);

        // Fetch latest AI result to check for overrides
        List<UnderwritingResult> aiResults = underwritingResultRepository.findByApplicationIdOrderByCreatedAtDesc(id);
        if (!aiResults.isEmpty()) {
            UnderwritingResult latestAi = aiResults.get(0);
            
            // Check if admin is contradicting AI
            boolean isOverride = false;
            if (latestAi.getRecommendation().name().equals("REJECT") && request.getStatus() == LoanStatus.APPROVED) {
                isOverride = true;
            } else if (latestAi.getRecommendation().name().equals("APPROVE") && request.getStatus() == LoanStatus.REJECTED) {
                isOverride = true;
            } else if (latestAi.getRequiresHumanReview() && request.getStatus() == LoanStatus.APPROVED) {
                isOverride = true; // Forcing approval on a flagged application is an override
            }

            if (isOverride && (request.getOverrideReason() == null || request.getOverrideReason().trim().isEmpty())) {
                throw new BadRequestException("An override reason is required when making a decision contrary to the AI recommendation.");
            }
        }

        loan.setStatus(request.getStatus());
        LoanApplication savedLoan = loanRepository.save(loan);

        // Record audit event
        String actionType = "ADMIN_DECISION_" + request.getStatus().name();
        String desc = "Admin set status to " + request.getStatus().name();
        if (request.getOverrideReason() != null && !request.getOverrideReason().trim().isEmpty()) {
            desc += ". Reason: " + request.getOverrideReason();
            actionType = "ADMIN_OVERRIDE_" + request.getStatus().name();
        }

        auditEventRepository.save(new AuditEvent(id, actionType, desc, adminId, adminName));

        return LoanApplicationResponseDTO.fromEntity(savedLoan);
    }

    public List<AuditEvent> getAuditEvents(Long applicationId) {
        return auditEventRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId);
    }

    /**
     * Deletes a loan by ID
     * @param id The loan ID
     */
    public void deleteLoan(Long id) {
        LoanApplication loan = getLoanEntityById(id);
        loanRepository.delete(loan);
    }

    /**
     * Checks eligibility for a loan based on income
     * @param request The eligibility request details
     * @return EligibilityResponse
     */
    public EligibilityResponse checkEligibility(EligibilityRequest request) {
        if (request.getMonthlyIncome().compareTo(new BigDecimal("50000")) >= 0) {
            return new EligibilityResponse(true, "Congratulations! You are eligible for the loan.");
        } else {
            return new EligibilityResponse(false, "Your application needs further review based on income criteria.");
        }
    }
}