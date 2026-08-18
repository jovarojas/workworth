package com.workworth.workday.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MealBreakRepository extends JpaRepository<MealBreak, Long> {
    List<MealBreak> findByWorkdayIdOrderByStartedAt(Long workdayId);
}
