package com.workworth.workday.persistence;

import com.workworth.workday.domain.WorkdayStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface WorkdayRepository extends JpaRepository<Workday, Long>, WorkdayRepositoryCustom {
    Optional<Workday> findByUserIdAndLocalDate(UUID userId, LocalDate localDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Workday w where w.user.id = :userId and w.localDate = :localDate")
    Optional<Workday> findLockedByUserIdAndLocalDate(UUID userId, LocalDate localDate);

    // Every Workday already reconciled for this user within [from, throughExclusive), together with
    // its status. Used to compute both (a) the exact set of business days still missing a Workday
    // entirely, instead of trusting a single MAX(local_date) pointer that a later date (created for
    // any reason -- the scheduler, a cancellation, a partial success) can push past an unnoticed gap
    // underneath it, and (b) which already-existing Workdays are still stuck in a non-terminal
    // status (SCHEDULED/ACTIVE/ON_MEAL_BREAK) whose scheduledEnd may already be behind "today" --
    // e.g. a day opened once and never revisited after its schedule ended -- so it can be refreshed
    // to COMPLETED and its Earning materialized instead of being silently skipped forever.
    @Query("select w.localDate as localDate, w.status as status from Workday w "
        + "where w.user.id = :userId and w.localDate >= :from and w.localDate < :throughExclusive")
    List<WorkdayDateStatus> findDateStatusesByUserIdAndLocalDateBetween(@Param("userId") UUID userId,
                                                                         @Param("from") LocalDate from,
                                                                         @Param("throughExclusive") LocalDate throughExclusive);

    interface WorkdayDateStatus {
        LocalDate getLocalDate();
        WorkdayStatus getStatus();
    }
}
