package com.workworth.rewards.application;

import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.preferences.application.ApplicationCurrencyService;
import com.workworth.identity.application.CurrentUserProvider;
import com.workworth.rewards.api.dto.CreateRewardRequest;
import com.workworth.rewards.api.dto.UpdateRewardRequest;
import com.workworth.rewards.domain.RewardStatus;
import com.workworth.rewards.exception.RewardConflictException;
import com.workworth.rewards.exception.RewardNotFoundException;
import com.workworth.rewards.persistence.Reward;
import com.workworth.rewards.persistence.RewardRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RewardService {

    private final RewardRepository rewards;
    private final ApplicationCurrencyProvider currency;
    private final ApplicationCurrencyService currencyService;
    private final Clock clock;
    private final CurrentUserProvider currentUser;

    public RewardService(RewardRepository rewards, ApplicationCurrencyProvider currency,
                         ApplicationCurrencyService currencyService, Clock clock,
                         CurrentUserProvider currentUser) {
        this.rewards = rewards;
        this.currency = currency;
        this.currencyService = currencyService;
        this.clock = clock;
        this.currentUser = currentUser;
    }

    @Transactional
    public Reward create(CreateRewardRequest request) {
        var user = currentUser.currentUser();
        Instant now = clock.instant();
        Reward reward = new Reward(user, request.name(), request.quantity() == null ? 1 : request.quantity(), request.price(),
            currency.currentCurrency().name(), now);
        // New rewards join the end of the user's own priority order (current max + 1), so they
        // never jump ahead of rewards the user already placed above others on purpose.
        reward.reorder(rewards.findMaxDisplayOrderByUserId(user.getId()) + 1, now);
        Reward saved = rewards.save(reward);
        currencyService.lockCurrencyAfterEconomicData();
        return saved;
    }

    public List<Reward> list(RewardStatus status) {
        return status == null ? rewards.findAllByUserIdOrderByDisplayOrderAscIdAsc(currentUser.currentUser().getId())
            : rewards.findAllByUserIdAndStatusOrderByDisplayOrderAscIdAsc(currentUser.currentUser().getId(), status);
    }

    // Applies a new manual priority order to exactly the given rewards, leaving every other
    // reward's order untouched. orderedIds is the caller's desired reading order (index 0 = shown
    // first); every id must exist and belong to the current user, and duplicates are rejected so a
    // malformed request never silently drops or repeats a reward.
    @Transactional
    public List<Reward> reorder(List<Long> orderedIds) {
        if (orderedIds.isEmpty()) {
            throw new RewardConflictException("orderedIds must not be empty.");
        }
        if (new HashSet<>(orderedIds).size() != orderedIds.size()) {
            throw new RewardConflictException("orderedIds must not contain duplicates.");
        }

        UUID userId = currentUser.currentUser().getId();
        Map<Long, Reward> byId = rewards.findAllByIdInAndUserId(orderedIds, userId).stream()
            .collect(Collectors.toMap(Reward::getId, Function.identity()));

        Instant now = clock.instant();
        List<Reward> reordered = new java.util.ArrayList<>();
        for (int index = 0; index < orderedIds.size(); index++) {
            Reward reward = byId.get(orderedIds.get(index));
            if (reward == null) {
                throw new RewardNotFoundException("Reward not found.");
            }
            reward.reorder(index, now);
            reordered.add(reward);
        }
        return reordered;
    }

    public Reward get(Long id) {
        return rewards.findByIdAndUserId(id, currentUser.currentUser().getId()).orElseThrow(() -> new RewardNotFoundException("Reward not found."));
    }

    @Transactional
    public Reward update(Long id, UpdateRewardRequest request) {
        Reward reward = get(id);
        requirePending(reward, "Only pending rewards can be edited.");
        reward.update(request.name(), request.quantity(), request.price(), clock.instant());
        return reward;
    }

    @Transactional
    public void delete(Long id) {
        rewards.delete(get(id));
    }

    @Transactional
    public Reward acquire(Long id) {
        Reward reward = getForUpdate(id);
        reward.acquire(clock.instant());
        return reward;
    }

    Reward pending(Long id) {
        Reward reward = get(id);
        requirePending(reward, "Only pending rewards can be evaluated.");
        return reward;
    }

    Reward pendingForUpdate(Long id) {
        Reward reward = getForUpdate(id);
        requirePending(reward, "Only pending rewards can be evaluated.");
        return reward;
    }

    private Reward getForUpdate(Long id) {
        return rewards.findByIdAndUserIdForUpdate(id, currentUser.currentUser().getId()).orElseThrow(() -> new RewardNotFoundException("Reward not found."));
    }

    private void requirePending(Reward reward, String message) {
        if (reward.getStatus() != RewardStatus.PENDING) {
            throw new RewardConflictException(message);
        }
    }
}
