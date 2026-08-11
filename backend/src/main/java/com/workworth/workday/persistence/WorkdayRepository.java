package com.workworth.workday.persistence;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
public interface WorkdayRepository extends JpaRepository<Workday, Long> { Optional<Workday> findByLocalDate(LocalDate localDate); @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select w from Workday w where w.localDate = :localDate") Optional<Workday> findLockedByLocalDate(LocalDate localDate); }
