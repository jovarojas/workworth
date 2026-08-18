package com.workworth.salary.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "salary_estimates")
public class SalaryEstimate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salary_profile_id", nullable = false)
    private SalaryProfile salaryProfile;

    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "region_code", nullable = false, length = 10)
    private String regionCode;

    @Column(name = "rule_set_version", nullable = false, length = 40)
    private String ruleSetVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_snapshot", nullable = false, columnDefinition = "jsonb")
    private String inputSnapshot;

    @Column(name = "estimated_net_annual", nullable = false, precision = 19, scale = 2)
    private BigDecimal estimatedNetAnnual;

    @Column(name = "estimated_net_monthly", nullable = false, precision = 19, scale = 2)
    private BigDecimal estimatedNetMonthly;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SalaryEstimate() {
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
