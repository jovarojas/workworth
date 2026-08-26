package com.workworth.workday.persistence;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface WorkdayRepository extends JpaRepository<Workday, Long>, WorkdayRepositoryCustom {
    Optional<Workday> findByUserIdAndLocalDate(UUID userId, LocalDate localDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Workday w where w.user.id = :userId and w.localDate = :localDate")
    Optional<Workday> findLockedByUserIdAndLocalDate(UUID userId, LocalDate localDate);

    @Query("select max(w.localDate) from Workday w where w.user.id = :userId")
    Optional<LocalDate> findLatestLocalDate(UUID userId);
}
