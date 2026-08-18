package com.workworth.goals.application;

import com.workworth.goals.api.dto.CreateGoalRequest;
import com.workworth.goals.api.dto.UpdateGoalRequest;
import com.workworth.goals.domain.GoalStatus;
import com.workworth.goals.exception.GoalConflictException;
import com.workworth.goals.exception.GoalNotFoundException;
import com.workworth.goals.exception.GoalProgressUnavailableException;
import com.workworth.goals.persistence.Goal;
import com.workworth.goals.persistence.GoalRepository;
import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.preferences.application.ApplicationCurrencyService;
import com.workworth.identity.application.CurrentUserProvider;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GoalService {

    private final GoalRepository goals;
    private final GoalProgressService progress;
    private final ApplicationCurrencyProvider currency;
    private final ApplicationCurrencyService currencyService;
    private final Clock clock;
    private final CurrentUserProvider currentUser;

    public GoalService(GoalRepository goals, GoalProgressService progress, ApplicationCurrencyProvider currency,
                       ApplicationCurrencyService currencyService, Clock clock,
                       CurrentUserProvider currentUser) {
        this.goals = goals;
        this.progress = progress;
        this.currency = currency;
        this.currencyService = currencyService;
        this.clock = clock;
        this.currentUser = currentUser;
    }

    @Transactional
    public Goal create(CreateGoalRequest request) {
        Goal goal = new Goal(currentUser.currentUser(), request.title(), request.targetAmount(), currency.currentCurrency().name(), clock.instant());
        Goal saved = goals.save(goal);
        currencyService.lockCurrencyAfterEconomicData();
        return saved;
    }

    public List<Goal> active() {
        return goals.findAllByUserIdAndStatusOrderByIdAsc(currentUser.currentUser().getId(), GoalStatus.ACTIVE);
    }

    public List<Goal> history() {
        return goals.findAllByUserIdAndStatusInOrderByClosedAtDescIdDesc(currentUser.currentUser().getId(), List.of(GoalStatus.COMPLETED, GoalStatus.CANCELLED));
    }

    public Goal get(Long id) {
        return goals.findByIdAndUserId(id, currentUser.currentUser().getId()).orElseThrow(() -> new GoalNotFoundException("Goal not found."));
    }

    public GoalProgress progress(Goal goal) {
        return goal.getStatus() == GoalStatus.ACTIVE ? progress.resolve(goal) : null;
    }

    @Transactional
    public Goal update(Long id, UpdateGoalRequest request) {
        Goal goal = getForUpdate(id);
        requireActive(goal, "Only active goals can be edited.");
        goal.update(request.title(), request.targetAmount(), clock.instant());
        return goal;
    }

    @Transactional
    public Goal complete(Long id) {
        Goal goal = getForUpdate(id);
        requireActive(goal, "Only active goals can be completed.");
        GoalProgress currentProgress = progress.resolve(goal);
        if (!currentProgress.evaluable()) {
            throw new GoalProgressUnavailableException("Goal progress is unavailable because ALL_TIME earnings are unavailable.");
        }
        if (!Boolean.TRUE.equals(currentProgress.reached())) {
            throw new GoalConflictException("Only a reached active goal can be completed.");
        }
        goal.complete(clock.instant());
        return goal;
    }

    @Transactional
    public Goal cancel(Long id) {
        Goal goal = getForUpdate(id);
        requireActive(goal, "Only active goals can be cancelled.");
        goal.cancel(clock.instant());
        return goal;
    }

    private Goal getForUpdate(Long id) {
        return goals.findByIdAndUserIdForUpdate(id, currentUser.currentUser().getId()).orElseThrow(() -> new GoalNotFoundException("Goal not found."));
    }

    private void requireActive(Goal goal, String message) {
        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new GoalConflictException(message);
        }
    }
}
