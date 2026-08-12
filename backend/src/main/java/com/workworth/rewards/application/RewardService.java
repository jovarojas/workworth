package com.workworth.rewards.application;

import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.preferences.application.ApplicationCurrencyService;
import com.workworth.rewards.api.dto.CreateRewardRequest;
import com.workworth.rewards.api.dto.UpdateRewardRequest;
import com.workworth.rewards.domain.RewardStatus;
import com.workworth.rewards.exception.RewardConflictException;
import com.workworth.rewards.exception.RewardNotFoundException;
import com.workworth.rewards.persistence.Reward;
import com.workworth.rewards.persistence.RewardRepository;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RewardService {

    private final RewardRepository rewards;
    private final ApplicationCurrencyProvider currency;
    private final ApplicationCurrencyService currencyService;
    private final Clock clock;

    public RewardService(RewardRepository rewards, ApplicationCurrencyProvider currency,
                         ApplicationCurrencyService currencyService, Clock clock) {
        this.rewards = rewards;
        this.currency = currency;
        this.currencyService = currencyService;
        this.clock = clock;
    }

    @Transactional
    public Reward create(CreateRewardRequest request) {
        Reward reward = new Reward(request.name(), request.quantity() == null ? 1 : request.quantity(), request.price(),
            currency.currentCurrency().name(), clock.instant());
        Reward saved = rewards.save(reward);
        currencyService.lockCurrencyAfterEconomicData();
        return saved;
    }

    public List<Reward> list(RewardStatus status) {
        return status == null ? rewards.findAll() : rewards.findAllByStatusOrderByIdAsc(status);
    }

    public Reward get(Long id) {
        return rewards.findById(id).orElseThrow(() -> new RewardNotFoundException("Reward not found."));
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
        Reward reward = get(id);
        reward.acquire(clock.instant());
        return reward;
    }

    Reward pending(Long id) {
        Reward reward = get(id);
        requirePending(reward, "Only pending rewards can be evaluated.");
        return reward;
    }

    private void requirePending(Reward reward, String message) {
        if (reward.getStatus() != RewardStatus.PENDING) {
            throw new RewardConflictException(message);
        }
    }
}
