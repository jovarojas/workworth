package com.workworth.earnings.persistence;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkdayEarningRepository extends JpaRepository<WorkdayEarning, Long> {

    Optional<WorkdayEarning> findByWorkdayId(Long id);

    @Query("select earning from WorkdayEarning earning where earning.localDate = :localDate and earning.workdayId in (select workday.id from Workday workday where workday.user.id = :userId)")
    Optional<WorkdayEarning> findByLocalDateAndWorkdayOwnerId(@Param("localDate") LocalDate localDate,
                                                               @Param("userId") UUID userId);

    @Query("select earning from WorkdayEarning earning where earning.workdayId in (select workday.id from Workday workday where workday.user.id = :userId) order by earning.localDate desc")
    Page<WorkdayEarning> findAllByWorkdayOwnerIdOrderByLocalDateDesc(@Param("userId") UUID userId, Pageable pageable);

    @Query("select earning from WorkdayEarning earning where earning.workdayId in (select workday.id from Workday workday where workday.user.id = :userId)")
    java.util.List<WorkdayEarning> findAllByWorkdayOwnerId(@Param("userId") UUID userId);

    @Query("select count(earning) > 0 from WorkdayEarning earning where earning.workdayId in (select workday.id from Workday workday where workday.user.id = :userId)")
    boolean existsByWorkdayOwnerId(@Param("userId") UUID userId);
}
