package com.finserve.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class LoanApplicationRequest {
    @NotNull
    private Long userId;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private Integer tenure;

    @NotNull
    private BigDecimal monthlyIncome;

    @NotBlank
    private String employmentType;

    private String purpose;

    public LoanApplicationRequest() {}

    public LoanApplicationRequest(Long userId, BigDecimal amount, Integer tenure, BigDecimal monthlyIncome, String employmentType, String purpose) {
        this.userId = userId;
        this.amount = amount;
        this.tenure = tenure;
        this.monthlyIncome = monthlyIncome;
        this.employmentType = employmentType;
        this.purpose = purpose;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getTenure() { return tenure; }
    public void setTenure(Integer tenure) { this.tenure = tenure; }

    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}