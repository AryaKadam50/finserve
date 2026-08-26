package com.finserve.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
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

    private BigDecimal monthlyExpenses;

    private BigDecimal existingMonthlyEmi;

    @Min(0)
    private Integer existingLoanCount;

    @NotBlank
    private String employmentType;

    @Min(0)
    private Integer yearsOfEmployment;

    private Integer creditScore;

    private String purpose;

    public LoanApplicationRequest() {}

    public LoanApplicationRequest(Long userId, BigDecimal amount, Integer tenure, BigDecimal monthlyIncome, BigDecimal monthlyExpenses, BigDecimal existingMonthlyEmi, Integer existingLoanCount, String employmentType, Integer yearsOfEmployment, Integer creditScore, String purpose) {
        this.userId = userId;
        this.amount = amount;
        this.tenure = tenure;
        this.monthlyIncome = monthlyIncome;
        this.monthlyExpenses = monthlyExpenses;
        this.existingMonthlyEmi = existingMonthlyEmi;
        this.existingLoanCount = existingLoanCount;
        this.employmentType = employmentType;
        this.yearsOfEmployment = yearsOfEmployment;
        this.creditScore = creditScore;
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

    public BigDecimal getMonthlyExpenses() { return monthlyExpenses; }
    public void setMonthlyExpenses(BigDecimal monthlyExpenses) { this.monthlyExpenses = monthlyExpenses; }

    public BigDecimal getExistingMonthlyEmi() { return existingMonthlyEmi; }
    public void setExistingMonthlyEmi(BigDecimal existingMonthlyEmi) { this.existingMonthlyEmi = existingMonthlyEmi; }

    public Integer getExistingLoanCount() { return existingLoanCount; }
    public void setExistingLoanCount(Integer existingLoanCount) { this.existingLoanCount = existingLoanCount; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public Integer getYearsOfEmployment() { return yearsOfEmployment; }
    public void setYearsOfEmployment(Integer yearsOfEmployment) { this.yearsOfEmployment = yearsOfEmployment; }

    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}