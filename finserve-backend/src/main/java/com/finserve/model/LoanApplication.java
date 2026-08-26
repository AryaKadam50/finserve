package com.finserve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_applications", indexes = {
    @Index(name = "idx_loan_status", columnList = "status"),
    @Index(name = "idx_loan_user", columnList = "user_id"),
    @Index(name = "idx_loan_created_at", columnList = "created_at")
})
public class LoanApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private Integer tenure;

    @NotNull
    @Column(name = "monthly_income")
    private BigDecimal monthlyIncome;

    @Column(name = "monthly_expenses")
    private BigDecimal monthlyExpenses;

    @Column(name = "existing_monthly_emi")
    private BigDecimal existingMonthlyEmi;

    @Min(0)
    @Column(name = "existing_loan_count")
    private Integer existingLoanCount;

    @Column(name = "employment_type")
    private String employmentType;

    @Min(0)
    @Column(name = "years_of_employment")
    private Integer yearsOfEmployment;

    @Column(name = "credit_score")
    private Integer creditScore;

    private String purpose;

    @Enumerated(EnumType.STRING)
    private LoanStatus status = LoanStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties("loanApplications")
    private User user;

    public LoanApplication() {}

    public LoanApplication(Long id, BigDecimal amount, Integer tenure, BigDecimal monthlyIncome, BigDecimal monthlyExpenses, BigDecimal existingMonthlyEmi, Integer existingLoanCount, String employmentType, Integer yearsOfEmployment, Integer creditScore, String purpose, LoanStatus status, LocalDateTime createdAt, User user) {
        this.id = id;
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
        this.status = status;
        this.createdAt = createdAt;
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}