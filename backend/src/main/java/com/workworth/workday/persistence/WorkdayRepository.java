package com.workworth.workday.persistence;

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

    // Every local_date already reconciled for this user within [from, throughExclusive), regardless
    // of status. Used to compute the exact set of business days still missing a Workday, instead of
    // trusting a single MAX(local_date) pointer that a later date (created for any reason -- the
    // scheduler, a cancellation, a partial success) can push past an unnoticed gap underneath it.
    @Query("select w.localDate from Workday w where w.user.id = :userId and w.localDate >= :from and w.localDate < :throughExclusive")
    List<LocalDate> findLocalDatesByUserIdAndLocalDateBetween(@Param("userId") UUID userId,
                                                               @Param("from") LocalDate from,
                                                               @Param("throughExclusive") LocalDate throughExclusive);
}
