package com.finserve.dto;

import com.finserve.model.LoanApplication;
import com.finserve.model.LoanStatus;
import com.finserve.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LoanApplicationResponseDTO {
    private Long id;
    private BigDecimal amount;
    private Integer tenure;
    private BigDecimal monthlyIncome;
    private String employmentType;
    private String purpose;
    private LoanStatus status;
    private LocalDateTime createdAt;
    private UserSummaryDTO user;

    public LoanApplicationResponseDTO() {}

    public LoanApplicationResponseDTO(Long id, BigDecimal amount, Integer tenure, BigDecimal monthlyIncome, String employmentType, String purpose, LoanStatus status, LocalDateTime createdAt, UserSummaryDTO user) {
        this.id = id;
        this.amount = amount;
        this.tenure = tenure;
        this.monthlyIncome = monthlyIncome;
        this.employmentType = employmentType;
        this.purpose = purpose;
        this.status = status;
        this.createdAt = createdAt;
        this.user = user;
    }

    public static LoanApplicationResponseDTO fromEntity(LoanApplication entity) {
        if (entity == null) return null;
        
        UserSummaryDTO userSummary = null;
        User userEntity = entity.getUser();
        if (userEntity != null) {
            userSummary = new UserSummaryDTO(userEntity.getId(), userEntity.getName());
        }
        
        return new LoanApplicationResponseDTO(
                entity.getId(),
                entity.getAmount(),
                entity.getTenure(),
                entity.getMonthlyIncome(),
                entity.getEmploymentType(),
                entity.getPurpose(),
                entity.getStatus(),
                entity.getCreatedAt(),
                userSummary
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public UserSummaryDTO getUser() { return user; }
    public void setUser(UserSummaryDTO user) { this.user = user; }
}
