package com.workworth.workday.persistence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PartialAbsenceRepository extends JpaRepository<PartialAbsence, Long> { List<PartialAbsence> findByWorkdayIdOrderByStartedAt(Long workdayId); }
