package com.finserve.dto;

import com.finserve.model.LoanStatus;

public class AdminDecisionRequest {
    private LoanStatus status;
    private String overrideReason;

    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
}
