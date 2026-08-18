package com.workworth.preferences.application;

import com.workworth.preferences.domain.ApplicationCurrency;

public record ApplicationCurrencySettings(ApplicationCurrency currencyCode, boolean changeAllowed) {
}
