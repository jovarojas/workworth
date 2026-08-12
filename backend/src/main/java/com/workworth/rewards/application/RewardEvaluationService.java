package com.workworth.rewards.application;

import com.workworth.common.money.MoneyRounding;
import com.workworth.earnings.application.EarningPeriodService;
import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.earnings.domain.EarningPeriodSummary;
import com.workworth.earnings.domain.EarningStatus;
import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.rewards.domain.RewardOutcome;
import com.workworth.rewards.exception.RewardCurrencyMismatchException;
import com.workworth.rewards.persistence.Reward;

import java.math.BigDecimal;
import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RewardEvaluationService {

    private final RewardService rewards;
    private final EarningPeriodService periods;
    private final ApplicationCurrencyProvider applicationCurrency;
    private final Clock clock;

    public RewardEvaluationService(RewardService rewards, EarningPeriodService periods,
                                   ApplicationCurrencyProvider applicationCurrency, Clock clock) {
        this.rewards = rewards;
        this.periods = periods;
        this.applicationCurrency = applicationCurrency;
        this.clock = clock;
    }

    public RewardEvaluation evaluate(Long rewardId, EarningPeriod context) {
        return evaluate(rewards.pending(rewardId), context);
    }

    @Transactional
    public RewardRelevance relevance(Long rewardId) {
        Reward reward = rewards.pending(rewardId);
        RewardEvaluation firstEvaluable = null;
        for (EarningPeriod context : EarningPeriod.values()) {
            RewardEvaluation evaluation = evaluate(reward, context);
            if (!evaluation.evaluable()) {
                continue;
            }
            if (firstEvaluable == null) {
                firstEvaluable = evaluation;
            }
            if (evaluation.outcome() == RewardOutcome.AFFORDABLE) {
                var previous = reward.getLastReachedContext();
                boolean newlyReached = previous == null || context.ordinal() < previous.ordinal();
                reward.updateLastReachedContext(context, clock.instant());
                return relevance(reward, evaluation, context, null, newlyReached, previous);
            }
        }
        if (firstEvaluable == null) {
            return new RewardRelevance(reward.getId(), false, null, null, null, null, reward.getPrice(),
                reward.getCurrencyCode(), null, null, false, reward.getLastReachedContext());
        }
        return relevance(reward, firstEvaluable, null, firstEvaluable.context(), false,
            reward.getLastReachedContext());
    }

    RewardEvaluation evaluate(Reward reward, EarningPeriod context) {
        EarningPeriodSummary summary = periods.summarize(context);
        if (summary.status() == EarningStatus.UNAVAILABLE) {
            return new RewardEvaluation(reward.getId(), context, false, null, null, reward.getPrice(),
                reward.getCurrencyCode(), null, null);
        }
        requireMatchingCurrency(reward, summary.currencyCode());
        BigDecimal available = summary.publicAmount();
        int comparison = available.compareTo(reward.getPrice());
        if (comparison >= 0) {
            return new RewardEvaluation(reward.getId(), context, true, RewardOutcome.AFFORDABLE, available,
                reward.getPrice(), reward.getCurrencyCode(), MoneyRounding.money(available.subtract(reward.getPrice())),
                null);
        }
        return new RewardEvaluation(reward.getId(), context, true, RewardOutcome.SHORTFALL, available,
            reward.getPrice(), reward.getCurrencyCode(), null,
            MoneyRounding.money(reward.getPrice().subtract(available)));
    }

    private RewardRelevance relevance(Reward reward, RewardEvaluation evaluation, EarningPeriod relevant,
                                      EarningPeriod progress, boolean newlyReached, EarningPeriod previous) {
        return new RewardRelevance(reward.getId(), true, relevant, progress, evaluation.outcome(),
            evaluation.availableAmount(), reward.getPrice(), reward.getCurrencyCode(), evaluation.surplus(),
            evaluation.shortfall(), newlyReached, previous);
    }

    void requireMatchingCurrency(Reward reward, String earningsCurrency) {
        if (!reward.getCurrencyCode().equals(applicationCurrency.currentCurrency().name())
            || !reward.getCurrencyCode().equals(earningsCurrency)) {
            throw new RewardCurrencyMismatchException(
                "Reward currency must match the configured application and earnings currency.");
        }
    }
}
