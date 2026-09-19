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

    List<Reward> findAllByUserIdAndStatusOrderByDisplayOrderAscIdAsc(UUID userId, RewardStatus status);

    List<Reward> findAllByUserIdOrderByDisplayOrderAscIdAsc(UUID userId);

    Optional<Reward> findByIdAndUserId(Long id, UUID userId);

    // Bulk lookup for reordering: fetches every requested id owned by this user in one query so
    // the service can validate the whole request (all ids exist and belong to the user) before
    // touching any row.
    List<Reward> findAllByIdInAndUserId(List<Long> ids, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reward from Reward reward where reward.id = :id and reward.user.id = :userId")
    Optional<Reward> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") UUID userId);

    @Query("select coalesce(max(reward.displayOrder), 0) from Reward reward where reward.user.id = :userId")
    long findMaxDisplayOrderByUserId(@Param("userId") UUID userId);
}
