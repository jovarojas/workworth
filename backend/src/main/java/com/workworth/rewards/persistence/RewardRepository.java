package com.workworth.rewards.persistence;

import com.workworth.rewards.domain.RewardStatus;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardRepository extends JpaRepository<Reward, Long> {

    List<Reward> findAllByStatusOrderByIdAsc(RewardStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reward from Reward reward where reward.id = :id")
    Optional<Reward> findByIdForUpdate(@Param("id") Long id);
}
