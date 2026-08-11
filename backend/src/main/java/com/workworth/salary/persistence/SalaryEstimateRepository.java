package com.workworth.salary.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryEstimateRepository extends JpaRepository<SalaryEstimate, Long> {

    Optional<SalaryEstimate> findBySalaryProfileIdAndFiscalYearAndRuleSetVersion(
            Long salaryProfileId, int fiscalYear, String ruleSetVersion);
}
