package com.workworth.goals.persistence;

import com.workworth.goals.domain.GoalStatus;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findAllByUserIdAndStatusOrderByIdAsc(UUID userId, GoalStatus status);

    List<Goal> findAllByUserIdAndStatusInOrderByClosedAtDescIdDesc(UUID userId, Collection<GoalStatus> statuses);

    Optional<Goal> findByIdAndUserId(Long id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select goal from Goal goal where goal.id = :id and goal.user.id = :userId")
    Optional<Goal> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") UUID userId);
}
