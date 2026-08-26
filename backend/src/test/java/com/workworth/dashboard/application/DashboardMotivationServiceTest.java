package com.workworth.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workworth.earnings.application.EarningPeriodService;
import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.earnings.domain.EarningPeriodSummary;
import com.workworth.earnings.domain.EarningStatus;
import com.workworth.identity.application.TestUsers;
import com.workworth.rewards.application.RewardCombination;
import com.workworth.rewards.application.RewardCombinationRelevance;
import com.workworth.rewards.application.RewardCombinationService;
import com.workworth.rewards.application.RewardEvaluation;
import com.workworth.rewards.application.RewardEvaluationService;
import com.workworth.rewards.application.RewardService;
import com.workworth.rewards.domain.RewardOutcome;
import com.workworth.rewards.domain.RewardStatus;
import com.workworth.rewards.persistence.Reward;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardMotivationServiceTest {

    private final RewardService rewards = mock(RewardService.class);
    private final RewardEvaluationService evaluations = mock(RewardEvaluationService.class);
    private final EarningPeriodService periods = mock(EarningPeriodService.class);
    private final RewardCombinationService combinations = mock(RewardCombinationService.class);
    private final Reward first = reward(1L, "Auriculares", "120.00");
    private final Reward second = reward(2L, "Libro", "90.00");
    private DashboardMotivationService service;

    @BeforeEach
    void setUp() {
        service = new DashboardMotivationService(rewards, evaluations, periods, combinations);
        when(rewards.list(RewardStatus.PENDING)).thenReturn(List.of(first, second));
        when(combinations.relevantCombination()).thenReturn(new RewardCombinationRelevance(true, null));
        when(evaluations.evaluate(any(Reward.class), any(EarningPeriodSummary.class))).thenAnswer(invocation -> {
            Reward reward = invocation.getArgument(0);
            EarningPeriodSummary summary = invocation.getArgument(1);
            if (summary.status() == EarningStatus.UNAVAILABLE) {
                return new RewardEvaluation(reward.getId(), summary.period(), false, null, null, reward.getPrice(),
                    reward.getCurrencyCode(), null, null);
            }
            int comparison = summary.publicAmount().compareTo(reward.getPrice());
            return new RewardEvaluation(reward.getId(), summary.period(), true,
                comparison >= 0 ? RewardOutcome.AFFORDABLE : RewardOutcome.SHORTFALL, summary.publicAmount(),
                reward.getPrice(), reward.getCurrencyCode(), comparison >= 0
                    ? summary.publicAmount().subtract(reward.getPrice()) : null,
                comparison < 0 ? reward.getPrice().subtract(summary.publicAmount()) : null);
        });
    }

    @Test
    void returnsEmptyWithoutEvaluatingWhenThereAreNoPendingRewards() {
        when(rewards.list(RewardStatus.PENDING)).thenReturn(List.of());

        var motivation = service.motivation();

        assertThat(motivation.state()).isEqualTo(DashboardMotivationState.EMPTY);
        assertThat(motivation.primaryReward()).isNull();
        assertThat(motivation.combination()).isNull();
        verify(evaluations, never()).evaluate(any(Reward.class), any(EarningPeriodSummary.class));
    }

    @Test
    void selectsTheFirstAffordableContextAndUsesTheLowestRewardIdAsTieBreak() {
        stubSummary(EarningPeriod.TODAY, "80.00");
        stubSummary(EarningPeriod.WEEK, "120.00");
        RewardCombination combination = new RewardCombination(EarningPeriod.WEEK, true, new BigDecimal("120.00"),
            new BigDecimal("90.00"), "EUR", List.of(reward(3L, "Hamburguesas", "30.00"),
                reward(4L, "Funkos", "60.00")));
        when(combinations.relevantCombination()).thenReturn(new RewardCombinationRelevance(true, combination));

        var motivation = service.motivation();

        assertThat(motivation.state()).isEqualTo(DashboardMotivationState.AVAILABLE);
        assertThat(motivation.primaryReward().reward()).isSameAs(first);
        assertThat(motivation.primaryReward().evaluation().context()).isEqualTo(EarningPeriod.WEEK);
        assertThat(motivation.combination()).isSameAs(combination);
        verify(periods, never()).summarize(EarningPeriod.MONTH);
    }

    @Test
    void selectsAllTimeOnlyAfterEarlierContextsAreNotAffordable() {
        stubSummary(EarningPeriod.TODAY, "20.00");
        stubSummary(EarningPeriod.WEEK, "40.00");
        stubSummary(EarningPeriod.MONTH, "80.00");
        stubSummary(EarningPeriod.ALL_TIME, "120.00");

        var motivation = service.motivation();

        assertThat(motivation.state()).isEqualTo(DashboardMotivationState.AVAILABLE);
        assertThat(motivation.primaryReward().reward()).isSameAs(first);
        assertThat(motivation.primaryReward().evaluation().context()).isEqualTo(EarningPeriod.ALL_TIME);
        assertThat(motivation.combination()).isNull();
    }

    @Test
    void selectsAnAffordableRewardInALaterContextAfterEarlierShortfalls() {
        stubUnavailable(EarningPeriod.TODAY);
        stubSummary(EarningPeriod.WEEK, "50.00");
        stubSummary(EarningPeriod.MONTH, "200.00");

        var motivation = service.motivation();

        assertThat(motivation.state()).isEqualTo(DashboardMotivationState.AVAILABLE);
        assertThat(motivation.primaryReward().evaluation().context()).isEqualTo(EarningPeriod.MONTH);
    }

    @Test
    void returnsProgressWhenNoContextCanAffordAnyReward() {
        for (EarningPeriod context : EarningPeriod.values()) {
            stubSummary(context, "20.00");
        }

        var motivation = service.motivation();

        assertThat(motivation.state()).isEqualTo(DashboardMotivationState.PROGRESS);
        assertThat(motivation.primaryReward().reward()).isSameAs(second);
        assertThat(motivation.primaryReward().evaluation().context()).isEqualTo(EarningPeriod.TODAY);
        assertThat(motivation.combination()).isNull();
        verify(combinations, never()).relevantCombination();
    }

    @Test
    void returnsUnavailableWhenEveryContextIsUnavailableWithoutChangingRewards() {
        for (EarningPeriod context : EarningPeriod.values()) {
            stubUnavailable(context);
        }

        var motivation = service.motivation();
        var repeated = service.motivation();

        assertThat(motivation.state()).isEqualTo(DashboardMotivationState.UNAVAILABLE);
        assertThat(repeated.state()).isEqualTo(DashboardMotivationState.UNAVAILABLE);
        assertThat(first.getLastReachedContext()).isNull();
        assertThat(second.getLastReachedContext()).isNull();
        verify(combinations, never()).relevantCombination();
    }

    private void stubSummary(EarningPeriod context, String amount) {
        BigDecimal value = new BigDecimal(amount);
        when(periods.summarize(context)).thenReturn(new EarningPeriodSummary(context, EarningStatus.AVAILABLE,
            LocalDate.of(2026, 8, 12), LocalDate.of(2026, 8, 13), value, value, "EUR"));
    }

    private void stubUnavailable(EarningPeriod context) {
        when(periods.summarize(context)).thenReturn(new EarningPeriodSummary(context, EarningStatus.UNAVAILABLE,
            LocalDate.of(2026, 8, 12), LocalDate.of(2026, 8, 13), null, null, null));
    }

    @Test
    void selectsTheMostExpensiveAffordableRewardInsteadOfStayingOnTheFirstOneReached() {
        // Regression test for BUG 2: "cafe" has the lower id (created first, cheaper) and
        // "escapada" has the higher id (created later, pricier). Both are affordable at
        // ALL_TIME once enough money has been earned. The old id-based tie-break stayed on
        // "cafe" forever; the primary reward must advance to "escapada" instead.
        Reward cafe = reward(10L, "Cafe", "3.00");
        Reward escapada = reward(11L, "Escapada", "40.00");
        when(rewards.list(RewardStatus.PENDING)).thenReturn(List.of(cafe, escapada));
        stubSummary(EarningPeriod.TODAY, "0.00");
        stubSummary(EarningPeriod.WEEK, "0.00");
        stubSummary(EarningPeriod.MONTH, "0.00");
        stubSummary(EarningPeriod.ALL_TIME, "45.00");

        var motivation = service.motivation();

        assertThat(motivation.state()).isEqualTo(DashboardMotivationState.AVAILABLE);
        assertThat(motivation.primaryReward().reward()).isSameAs(escapada);
    }

    @Test
    void advancesThroughEachRewardAsAvailableMoneyGrowsPastItsPrice() {
        Reward cafe = reward(20L, "Cafe", "3.00");
        Reward cine = reward(21L, "Cine", "8.00");
        Reward cena = reward(22L, "Cena", "15.00");
        Reward escapada = reward(23L, "Escapada", "40.00");
        when(rewards.list(RewardStatus.PENDING)).thenReturn(List.of(cafe, cine, cena, escapada));
        stubSummary(EarningPeriod.TODAY, "0.00");
        stubSummary(EarningPeriod.WEEK, "0.00");
        stubSummary(EarningPeriod.MONTH, "0.00");

        stubSummary(EarningPeriod.ALL_TIME, "0.00");
        var atZero = service.motivation();
        assertThat(atZero.state()).isEqualTo(DashboardMotivationState.PROGRESS);
        assertThat(atZero.primaryReward().reward()).isSameAs(cafe);

        stubSummary(EarningPeriod.ALL_TIME, "3.00");
        assertThat(service.motivation().primaryReward().reward()).isSameAs(cafe);

        stubSummary(EarningPeriod.ALL_TIME, "10.00");
        assertThat(service.motivation().primaryReward().reward()).isSameAs(cine);

        stubSummary(EarningPeriod.ALL_TIME, "20.00");
        assertThat(service.motivation().primaryReward().reward()).isSameAs(cena);

        // All rewards are now affordable: the most expensive one is shown as the final goal.
        stubSummary(EarningPeriod.ALL_TIME, "100.00");
        assertThat(service.motivation().primaryReward().reward()).isSameAs(escapada);
    }

    @Test
    void tiesOnPriceKeepTheLowestRewardIdAsTheFinalTieBreak() {
        Reward lowerId = reward(29L, "A", "20.00");
        Reward higherId = reward(30L, "B", "20.00");
        when(rewards.list(RewardStatus.PENDING)).thenReturn(List.of(lowerId, higherId));
        stubSummary(EarningPeriod.TODAY, "0.00");
        stubSummary(EarningPeriod.WEEK, "0.00");
        stubSummary(EarningPeriod.MONTH, "0.00");
        stubSummary(EarningPeriod.ALL_TIME, "20.00");

        var motivation = service.motivation();

        assertThat(motivation.primaryReward().reward()).isSameAs(lowerId);
    }

    private Reward reward(Long id, String name, String price) {
        Reward reward = new Reward(TestUsers.user("test|dashboard-motivation"), name, 1,
            new BigDecimal(price), "EUR", Instant.EPOCH);
        org.springframework.test.util.ReflectionTestUtils.setField(reward, "id", id);
        return reward;
    }
}
