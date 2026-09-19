package com.workworth.salary.persistence;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryProfileRepository extends JpaRepository<SalaryProfile, Long> {

    boolean existsByUserIdAndEffectiveFrom(UUID userId, LocalDate effectiveFrom);

    Optional<SalaryProfile> findTopByUserIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(UUID userId, LocalDate effectiveFrom);

    Page<SalaryProfile> findAllByUserIdOrderByEffectiveFromDesc(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);

    // The next salary basis that has not become effective yet -- i.e. a change already scheduled
    // for a future month. Ordered ascending so the earliest upcoming one wins if, for whatever
    // reason, more than one future profile exists.
    Optional<SalaryProfile> findFirstByUserIdAndEffectiveFromGreaterThanOrderByEffectiveFromAsc(
        UUID userId, LocalDate effectiveFrom);
}
