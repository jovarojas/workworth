package com.workworth.goals.application;

import com.workworth.common.money.MoneyRounding;
import com.workworth.earnings.application.EarningPeriodService;
import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.earnings.domain.EarningPeriodSummary;
import com.workworth.earnings.domain.EarningStatus;
import com.workworth.goals.exception.GoalCurrencyMismatchException;
import com.workworth.goals.persistence.Goal;
import com.workworth.preferences.application.ApplicationCurrencyProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GoalProgressService {

    private final EarningPeriodService earningPeriods;
    private final ApplicationCurrencyProvider currency;

    public GoalProgressService(EarningPeriodService earningPeriods, ApplicationCurrencyProvider currency) {
        this.earningPeriods = earningPeriods;
        this.currency = currency;
    }

    public GoalProgress resolve(Goal goal) {
        EarningPeriodSummary summary = earningPeriods.summarize(EarningPeriod.ALL_TIME);
        if (summary.status() == EarningStatus.UNAVAILABLE) {
            return GoalProgress.unavailable();
        }
        requireMatchingCurrency(goal, summary);

        BigDecimal progressAmount = MoneyRounding.money(summary.publicAmount());
        BigDecimal remainingAmount = MoneyRounding.money(goal.getTargetAmount().subtract(progressAmount).max(BigDecimal.ZERO));
        boolean reached = progressAmount.compareTo(goal.getTargetAmount()) >= 0;
        BigDecimal progressPercentage = progressAmount.multiply(BigDecimal.valueOf(100))
            .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
            .min(BigDecimal.valueOf(100));
        return new GoalProgress(true, progressAmount, remainingAmount, progressPercentage, reached);
    }

    private void requireMatchingCurrency(Goal goal, EarningPeriodSummary summary) {
        String expectedCurrency = currency.currentCurrency().name();
        if (!goal.getCurrencyCode().equals(expectedCurrency) || !goal.getCurrencyCode().equals(summary.currencyCode())) {
            throw new GoalCurrencyMismatchException("Goal currency does not match the effective ALL_TIME earnings currency.");
        }
    }
}
