package com.workworth.preferences.api.dto;

import com.workworth.preferences.application.ApplicationCurrencySettings;

public record ApplicationCurrencyResponse(String currencyCode, boolean changeAllowed) {

    public static ApplicationCurrencyResponse from(ApplicationCurrencySettings settings) {
        return new ApplicationCurrencyResponse(settings.currencyCode().name(), settings.changeAllowed());
    }
}
