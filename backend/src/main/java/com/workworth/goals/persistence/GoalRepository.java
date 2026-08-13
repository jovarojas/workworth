package com.workworth.goals.persistence;

import com.workworth.goals.domain.GoalStatus;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findAllByStatusOrderByIdAsc(GoalStatus status);

    List<Goal> findAllByStatusInOrderByClosedAtDescIdDesc(Collection<GoalStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select goal from Goal goal where goal.id = :id")
    Optional<Goal> findByIdForUpdate(@Param("id") Long id);
}
