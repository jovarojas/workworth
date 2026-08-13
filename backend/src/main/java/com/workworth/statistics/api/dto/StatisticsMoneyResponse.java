package com.workworth.statistics.api.dto;

import com.workworth.statistics.application.StatisticsMoney;

import java.math.BigDecimal;

public record StatisticsMoneyResponse(String status, BigDecimal amount, String currencyCode) {

    public static StatisticsMoneyResponse from(StatisticsMoney money) {
        return new StatisticsMoneyResponse(money.status().name(), money.amount(), money.currencyCode());
    }
}
