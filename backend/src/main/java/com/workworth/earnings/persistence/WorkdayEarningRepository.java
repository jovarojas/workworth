package com.workworth.earnings.persistence;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkdayEarningRepository extends JpaRepository<WorkdayEarning, Long> {

    Optional<WorkdayEarning> findByWorkdayId(Long id);

    Optional<WorkdayEarning> findByLocalDate(LocalDate localDate);

    Page<WorkdayEarning> findAllByOrderByLocalDateDesc(Pageable pageable);
}
