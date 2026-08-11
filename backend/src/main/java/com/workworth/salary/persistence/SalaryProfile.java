package com.workworth.salary.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;

@Getter
@Entity
@Table(name = "salary_profiles")
public class SalaryProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "effective_from", nullable = false, unique = true)
    private LocalDate effectiveFrom;

    @Column(name = "gross_annual", precision = 19, scale = 2)
    private BigDecimal grossAnnual;

    @Column(name = "net_monthly_real", precision = 19, scale = 2)
    private BigDecimal netMonthlyReal;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "pay_periods", nullable = false)
    private int payPeriods;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SalaryProfile() {
    }

    public SalaryProfile(LocalDate effectiveFrom, BigDecimal grossAnnual, BigDecimal netMonthlyReal,
                         String currencyCode, int payPeriods, Instant createdAt) {
        this.effectiveFrom = effectiveFrom;
        this.grossAnnual = grossAnnual;
        this.netMonthlyReal = netMonthlyReal;
        this.currencyCode = currencyCode;
        this.payPeriods = payPeriods;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }
}
