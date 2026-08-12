package com.workworth.salary.application;

import com.workworth.common.money.MoneyRounding;
import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.preferences.application.ApplicationCurrencyService;
import com.workworth.salary.api.dto.CreateSalaryProfileRequest;
import com.workworth.salary.api.dto.SalaryProfileHistoryResponse;
import com.workworth.salary.api.dto.SalaryProfileResponse;
import com.workworth.salary.exception.SalaryProfileConflictException;
import com.workworth.salary.exception.SalaryProfileNotFoundException;
import com.workworth.salary.persistence.SalaryProfile;
import com.workworth.salary.persistence.SalaryProfileRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SalaryProfileService {

    private static final int MVP_PAY_PERIODS = 12;

    private final SalaryProfileRepository salaryProfileRepository;
    private final SalaryProfileMapper salaryProfileMapper;
    private final Clock clock;
    private final ApplicationCurrencyProvider applicationCurrency;
    private final ApplicationCurrencyService applicationCurrencyService;

    public SalaryProfileService(SalaryProfileRepository salaryProfileRepository,
                                SalaryProfileMapper salaryProfileMapper,
                                Clock clock,
                                ApplicationCurrencyProvider applicationCurrency,
                                ApplicationCurrencyService applicationCurrencyService) {
        this.salaryProfileRepository = salaryProfileRepository;
        this.salaryProfileMapper = salaryProfileMapper;
        this.clock = clock;
        this.applicationCurrency = applicationCurrency;
        this.applicationCurrencyService = applicationCurrencyService;
    }

    @Transactional
    public SalaryProfileResponse create(CreateSalaryProfileRequest request) {
        validateRequest(request);

        SalaryProfile profile = new SalaryProfile(
            request.effectiveFrom(),
            moneyOrNull(request.grossAnnual()),
            moneyOrNull(request.netMonthlyReal()),
            request.currencyCode(),
            request.payPeriods(),
            clock.instant());

        SalaryProfile savedProfile = salaryProfileRepository.save(profile);
        applicationCurrencyService.lockCurrencyAfterEconomicData();
        return salaryProfileMapper.toResponse(savedProfile);
    }

    public SalaryProfileResponse getCurrent(YearMonth month) {
        return salaryProfileMapper.toResponse(findEffectiveProfile(month));
    }

    public SalaryProfile findEffectiveProfile(YearMonth month) {
        return salaryProfileRepository.findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(month.atDay(1))
            .orElseThrow(() -> new SalaryProfileNotFoundException(
                "No salary profile is effective for " + month + "."));
    }

    public SalaryProfileHistoryResponse getHistory(int page, int size) {
        Page<SalaryProfile> profiles = salaryProfileRepository.findAllByOrderByEffectiveFromDesc(PageRequest.of(page, size));
        return new SalaryProfileHistoryResponse(
            profiles.map(salaryProfileMapper::toResponse).getContent(),
            profiles.getNumber(),
            profiles.getSize(),
            profiles.getTotalElements(),
            profiles.getTotalPages());
    }

    private void validateRequest(CreateSalaryProfileRequest request) {
        if (!applicationCurrency.currentCurrency().name().equals(request.currencyCode())) {
            throw new SalaryProfileConflictException("Salary profile currency must match the application currency.");
        }
        if (request.payPeriods() != MVP_PAY_PERIODS) {
            throw new SalaryProfileConflictException("The MVP supports exactly 12 equal pay periods.");
        }
        if (!request.effectiveFrom().equals(request.effectiveFrom().withDayOfMonth(1))) {
            throw new SalaryProfileConflictException("effectiveFrom must be the first day of a month.");
        }

        YearMonth currentMonth = YearMonth.now(clock);
        if (request.effectiveFrom().isBefore(currentMonth.atDay(1))) {
            throw new SalaryProfileConflictException("effectiveFrom cannot be before the current month in the MVP.");
        }
        if (salaryProfileRepository.existsByEffectiveFrom(request.effectiveFrom())) {
            throw new SalaryProfileConflictException("A salary profile already exists for this effective month.");
        }
    }

    private BigDecimal moneyOrNull(BigDecimal value) {
        return value == null ? null : MoneyRounding.money(value);
    }
}
