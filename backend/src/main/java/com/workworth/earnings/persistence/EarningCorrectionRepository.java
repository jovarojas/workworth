package com.workworth.earnings.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EarningCorrectionRepository extends JpaRepository<EarningCorrection, Long> {

    boolean existsByWorkdayTimeCorrectionId(Long id);

    List<EarningCorrection> findByEarningIdOrderBySequenceDesc(Long id);

    List<EarningCorrection> findByEarningIdInOrderByEarningIdAscSequenceDesc(Collection<Long> earningIds);
}
