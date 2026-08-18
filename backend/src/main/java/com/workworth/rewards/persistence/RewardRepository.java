package com.workworth.rewards.persistence;

import com.workworth.rewards.domain.RewardStatus;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardRepository extends JpaRepository<Reward, Long> {

    List<Reward> findAllByUserIdAndStatusOrderByIdAsc(UUID userId, RewardStatus status);

    List<Reward> findAllByUserIdOrderByIdAsc(UUID userId);

    Optional<Reward> findByIdAndUserId(Long id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reward from Reward reward where reward.id = :id and reward.user.id = :userId")
    Optional<Reward> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") UUID userId);
}
