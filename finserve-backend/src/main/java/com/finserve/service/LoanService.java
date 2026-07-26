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
import org.springframework.stereotype.Service;

import com.finserve.dto.LoanApplicationResponseDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;

    public LoanService(LoanRepository loanRepository, UserRepository userRepository) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
    }

    /**
     * Applies for a new loan
     * @param request The loan application details
     * @return LoanApplication entity
     */
    public LoanApplicationResponseDTO applyForLoan(LoanApplicationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        LoanApplication loan = new LoanApplication();
        loan.setUser(user);
        loan.setAmount(request.getAmount());
        loan.setTenure(request.getTenure());
        loan.setMonthlyIncome(request.getMonthlyIncome());
        loan.setEmploymentType(request.getEmploymentType());
        loan.setPurpose(request.getPurpose());

        if (request.getMonthlyIncome().compareTo(new BigDecimal("50000")) >= 0) {
            loan.setStatus(LoanStatus.PENDING);
        } else {
            loan.setStatus(LoanStatus.UNDER_REVIEW);
        }

        return LoanApplicationResponseDTO.fromEntity(loanRepository.save(loan));
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
     * Updates the status of a loan
     * @param id The loan ID
     * @param request The new status
     * @return Updated LoanApplication entity
     */
    public LoanApplicationResponseDTO updateLoanStatus(Long id, LoanStatusUpdateRequest request) {
        LoanApplication loan = getLoanEntityById(id);
        loan.setStatus(request.getStatus());
        return LoanApplicationResponseDTO.fromEntity(loanRepository.save(loan));
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