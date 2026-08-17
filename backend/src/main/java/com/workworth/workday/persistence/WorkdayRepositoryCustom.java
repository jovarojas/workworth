package com.workworth.workday.persistence;

import java.time.LocalDate;
import java.util.UUID;

public interface WorkdayRepositoryCustom {

    void lockUserDate(UUID userId, LocalDate localDate);
}
