package com.workworth.earnings.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EarningProjection(LocalDate localDate, EarningStatus status, long economicSeconds,
                                BigDecimal rawAmount, String currencyCode,
                                EarningUnavailableReason unavailableReason) { }
