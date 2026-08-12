package com.workworth.rewards.persistence;

import com.workworth.rewards.domain.RewardStatus;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardRepository extends JpaRepository<Reward, Long> {

    List<Reward> findAllByStatusOrderByIdAsc(RewardStatus status);
}
