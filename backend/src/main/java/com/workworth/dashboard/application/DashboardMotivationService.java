package com.workworth.dashboard.application;

import com.workworth.earnings.application.EarningPeriodService;
import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.rewards.application.RewardCombination;
import com.workworth.rewards.application.RewardCombinationService;
import com.workworth.rewards.application.RewardEvaluation;
import com.workworth.rewards.application.RewardEvaluationService;
import com.workworth.rewards.application.RewardService;
import com.workworth.rewards.domain.RewardOutcome;
import com.workworth.rewards.domain.RewardStatus;
import com.workworth.rewards.persistence.Reward;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardMotivationService {

    private final RewardService rewards;
    private final RewardEvaluationService evaluations;
    private final EarningPeriodService periods;
    private final RewardCombinationService combinations;

    public DashboardMotivationService(RewardService rewards, RewardEvaluationService evaluations,
                                      EarningPeriodService periods, RewardCombinationService combinations) {
        this.rewards = rewards;
        this.evaluations = evaluations;
        this.periods = periods;
        this.combinations = combinations;
    }

    public DashboardMotivation motivation() {
        List<Reward> pendingRewards = rewards.list(RewardStatus.PENDING);
        if (pendingRewards.isEmpty()) {
            return new DashboardMotivation(DashboardMotivationState.EMPTY, null, null);
        }

        List<RewardEvaluation> firstEvaluable = null;
        for (EarningPeriod context : EarningPeriod.values()) {
            var summary = periods.summarize(context);
            List<RewardEvaluation> evaluationsForContext = pendingRewards.stream()
                .map(reward -> evaluations.evaluate(reward, summary))
                .toList();

            var affordable = evaluationsForContext.stream()
                .filter(evaluation -> evaluation.evaluable() && evaluation.outcome() == RewardOutcome.AFFORDABLE)
                .findFirst();
            if (affordable.isPresent()) {
                RewardEvaluation evaluation = affordable.get();
                return new DashboardMotivation(DashboardMotivationState.AVAILABLE,
                    primary(pendingRewards, evaluation), relevantCombination());
            }
            if (firstEvaluable == null && evaluationsForContext.stream().anyMatch(RewardEvaluation::evaluable)) {
                firstEvaluable = evaluationsForContext;
            }
        }

        if (firstEvaluable == null) {
            return new DashboardMotivation(DashboardMotivationState.UNAVAILABLE, null, null);
        }

        RewardEvaluation progress = firstEvaluable.stream()
            .filter(RewardEvaluation::evaluable)
            .min(Comparator.comparing(RewardEvaluation::shortfall).thenComparing(RewardEvaluation::rewardId))
            .orElseThrow();
        return new DashboardMotivation(DashboardMotivationState.PROGRESS, primary(pendingRewards, progress), null);
    }

    private DashboardPrimaryReward primary(List<Reward> pendingRewards, RewardEvaluation evaluation) {
        Reward reward = pendingRewards.stream()
            .filter(candidate -> candidate.getId().equals(evaluation.rewardId()))
            .findFirst()
            .orElseThrow();
        return new DashboardPrimaryReward(reward, evaluation);
    }

    private RewardCombination relevantCombination() {
        return combinations.relevantCombination().combination();
    }
}
