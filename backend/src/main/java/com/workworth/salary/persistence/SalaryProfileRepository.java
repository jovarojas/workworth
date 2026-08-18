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
}
