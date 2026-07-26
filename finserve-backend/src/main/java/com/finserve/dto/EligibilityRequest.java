package com.finserve.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class EligibilityRequest {
    @NotNull
    private BigDecimal monthlyIncome;

    @NotNull
    private BigDecimal requestedAmount;

    @NotNull
    private Integer tenure;

    public EligibilityRequest() {}

    public EligibilityRequest(BigDecimal monthlyIncome, BigDecimal requestedAmount, Integer tenure) {
        this.monthlyIncome = monthlyIncome;
        this.requestedAmount = requestedAmount;
        this.tenure = tenure;
    }

    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }

    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }

    public Integer getTenure() { return tenure; }
    public void setTenure(Integer tenure) { this.tenure = tenure; }
}