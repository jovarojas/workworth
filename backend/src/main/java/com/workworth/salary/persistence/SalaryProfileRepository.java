package com.workworth.salary.persistence;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryProfileRepository extends JpaRepository<SalaryProfile, Long> {

    boolean existsByEffectiveFrom(LocalDate effectiveFrom);

    Optional<SalaryProfile> findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate effectiveFrom);

    Page<SalaryProfile> findAllByOrderByEffectiveFromDesc(Pageable pageable);
}
