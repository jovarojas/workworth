package com.workworth.earnings.persistence;

import com.workworth.earnings.domain.EarningStatus;
import com.workworth.earnings.domain.EarningUnavailableReason;
import com.workworth.salary.domain.IncomeSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.Getter;

@Getter
@Entity
@Table(name = "workday_earnings")
public class WorkdayEarning {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "workday_id", nullable = false, unique = true) private Long workdayId;
    @Column(name = "local_date", nullable = false) private LocalDate localDate;
    @Column(name = "reference_month", nullable = false, length = 7) private String referenceMonth;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EarningStatus status;
    @Enumerated(EnumType.STRING) @Column(name = "unavailable_reason", length = 64)
    private EarningUnavailableReason unavailableReason;
    @Column(name = "economic_seconds", nullable = false) private long economicSeconds;
    @Column(name = "raw_amount", precision = 24, scale = 12) private BigDecimal rawAmount;
    @Column(name = "salary_profile_id") private Long salaryProfileId;
    @Enumerated(EnumType.STRING) @Column(name = "income_source") private IncomeSource incomeSource;
    @Column(name = "monthly_net", precision = 19, scale = 2) private BigDecimal monthlyNet;
    @Column(name = "annual_net", precision = 19, scale = 2) private BigDecimal annualNet;
    @Column(name = "pay_periods") private Integer payPeriods;
    @Column(name = "currency_code", length = 3) private String currencyCode;
    @Column(name = "standard_hours", precision = 24, scale = 12) private BigDecimal standardHours;
    @Column(name = "hourly_rate", precision = 24, scale = 12) private BigDecimal hourlyRate;
    @Column(name = "materialized_at", nullable = false) private Instant materializedAt;

    protected WorkdayEarning() { }

    public WorkdayEarning(Long workdayId, LocalDate date, EarningStatus status, long seconds,
                          BigDecimal amount, Long profile, IncomeSource source, BigDecimal monthly,
                          BigDecimal annual, int periods, String currency, BigDecimal hours,
                          BigDecimal rate, Instant at) {
        this(workdayId, date, status, null, seconds, amount, profile, source, monthly, annual,
                periods, currency, hours, rate, at);
    }

    public WorkdayEarning(Long workdayId, LocalDate date, EarningStatus status,
                          EarningUnavailableReason unavailableReason, long seconds, BigDecimal amount,
                          Long profile, IncomeSource source, BigDecimal monthly, BigDecimal annual,
                          int periods, String currency, BigDecimal hours, BigDecimal rate, Instant at) {
        this.workdayId = workdayId;
        this.localDate = date;
        this.referenceMonth = YearMonth.from(date).toString();
        this.status = status;
        this.unavailableReason = unavailableReason;
        this.economicSeconds = seconds;
        this.rawAmount = amount;
        this.salaryProfileId = profile;
        this.incomeSource = source;
        this.monthlyNet = monthly;
        this.annualNet = annual;
        this.payPeriods = periods;
        this.currencyCode = currency;
        this.standardHours = hours;
        this.hourlyRate = rate;
        this.materializedAt = at;
    }
}
