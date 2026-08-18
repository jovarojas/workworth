package com.workworth.preferences.application;

import com.workworth.preferences.domain.ApplicationCurrency;

public interface ApplicationCurrencyProvider {

    ApplicationCurrency currentCurrency();
}
