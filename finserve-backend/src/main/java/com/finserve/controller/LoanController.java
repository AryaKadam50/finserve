package com.finserve.controller;

import com.finserve.dto.EligibilityRequest;
import com.finserve.dto.EligibilityResponse;
import com.finserve.dto.LoanApplicationRequest;
import com.finserve.dto.LoanStatusUpdateRequest;
import com.finserve.dto.LoanApplicationResponseDTO;
import com.finserve.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanApplicationResponseDTO> applyForLoan(@Valid @RequestBody LoanApplicationRequest request) {
        return new ResponseEntity<>(loanService.applyForLoan(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<LoanApplicationResponseDTO>> getAllLoans() {
        return ResponseEntity.ok(loanService.getAllLoans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanApplicationResponseDTO> getLoanById(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.getLoanById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<LoanApplicationResponseDTO> updateLoanStatus(@PathVariable Long id, @Valid @RequestBody LoanStatusUpdateRequest request) {
        return ResponseEntity.ok(loanService.updateLoanStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        loanService.deleteLoan(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/check-eligibility")
    public ResponseEntity<EligibilityResponse> checkEligibility(@Valid @RequestBody EligibilityRequest request) {
        return ResponseEntity.ok(loanService.checkEligibility(request));
    }
}