package com.finserve.dto;

import com.finserve.model.LoanStatus;
import jakarta.validation.constraints.NotNull;

public class LoanStatusUpdateRequest {
    @NotNull
    private LoanStatus status;

    public LoanStatusUpdateRequest() {}

    public LoanStatusUpdateRequest(LoanStatus status) {
        this.status = status;
    }

    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }
}