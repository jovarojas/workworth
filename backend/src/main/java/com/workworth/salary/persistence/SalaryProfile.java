package com.workworth.salary.persistence;

import com.workworth.identity.persistence.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "effective_from", nullable = false)
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

    public SalaryProfile(AppUser user, LocalDate effectiveFrom, BigDecimal grossAnnual, BigDecimal netMonthlyReal,
                         String currencyCode, int payPeriods, Instant createdAt) {
        this.user = user;
        this.effectiveFrom = effectiveFrom;
        this.grossAnnual = grossAnnual;
        this.netMonthlyReal = netMonthlyReal;
        this.currencyCode = currencyCode;
        this.payPeriods = payPeriods;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    // Only used to edit an already-scheduled, not-yet-effective salary change (see
    // SalaryProfileService#updateUpcoming): a profile that is already effective, or already in
    // the past, is never mutated in place -- SPEC 001 always models a salary change as a new
    // profile so historical snapshots stay untouched.
    public void updateNetMonthlyReal(BigDecimal netMonthlyReal, Instant updatedAt) {
        this.netMonthlyReal = netMonthlyReal;
        this.updatedAt = updatedAt;
    }

}
