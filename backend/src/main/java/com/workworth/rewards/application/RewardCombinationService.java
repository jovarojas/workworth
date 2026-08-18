package com.workworth.rewards.application;

import com.workworth.common.money.MoneyRounding;
import com.workworth.earnings.application.EarningPeriodService;
import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.earnings.domain.EarningPeriodSummary;
import com.workworth.earnings.domain.EarningStatus;
import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.identity.application.CurrentUserProvider;
import com.workworth.rewards.domain.RewardStatus;
import com.workworth.rewards.exception.RewardCurrencyMismatchException;
import com.workworth.rewards.persistence.Reward;
import com.workworth.rewards.persistence.RewardRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RewardCombinationService {

    private final RewardRepository rewards;
    private final EarningPeriodService periods;
    private final ApplicationCurrencyProvider applicationCurrency;
    private final CurrentUserProvider currentUser;

    public RewardCombinationService(RewardRepository rewards, EarningPeriodService periods,
                                    ApplicationCurrencyProvider applicationCurrency,
                                    CurrentUserProvider currentUser) {
        this.rewards = rewards;
        this.periods = periods;
        this.applicationCurrency = applicationCurrency;
        this.currentUser = currentUser;
    }

    public RewardCombination combination(EarningPeriod context, Set<Long> excludeRewardIds) {
        return combination(context, pendingRewards(excludeRewardIds));
    }

    public RewardCombinationRelevance relevantCombination() {
        List<Reward> pending = pendingRewards(Set.of());
        boolean hasEvaluableContext = false;
        for (EarningPeriod context : EarningPeriod.values()) {
            RewardCombination combination = combination(context, pending);
            if (!combination.evaluable()) {
                continue;
            }
            hasEvaluableContext = true;
            if (combination.rewards().size() >= 2) {
                return new RewardCombinationRelevance(true, combination);
            }
        }
        return new RewardCombinationRelevance(hasEvaluableContext, null);
    }

    private List<Reward> pendingRewards(Set<Long> excludeRewardIds) {
        return rewards.findAllByUserIdAndStatusOrderByIdAsc(currentUser.currentUser().getId(), RewardStatus.PENDING).stream()
            .filter(reward -> !excludeRewardIds.contains(reward.getId()))
            .toList();
    }

    private RewardCombination combination(EarningPeriod context, List<Reward> pending) {
        EarningPeriodSummary summary = periods.summarize(context);
        if (summary.status() == EarningStatus.UNAVAILABLE) {
            return new RewardCombination(context, false, null, null, applicationCurrency.currentCurrency().name(), List.of());
        }
        for (Reward reward : pending) {
            if (!reward.getCurrencyCode().equals(applicationCurrency.currentCurrency().name())
                || !reward.getCurrencyCode().equals(summary.currencyCode())) {
                throw new RewardCurrencyMismatchException(
                    "Reward currency must match the configured application and earnings currency.");
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        java.util.ArrayList<Reward> selected = new java.util.ArrayList<>();
        for (Reward reward : pending) {
            BigDecimal candidate = total.add(reward.getPrice());
            if (candidate.compareTo(summary.publicAmount()) <= 0) {
                selected.add(reward);
                total = candidate;
            }
        }
        if (selected.size() < 2) {
            return new RewardCombination(context, true, summary.publicAmount(), null, summary.currencyCode(), List.of());
        }
        return new RewardCombination(context, true, summary.publicAmount(), MoneyRounding.money(total),
            summary.currencyCode(), List.copyOf(selected));
    }
}
