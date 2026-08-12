package com.workworth.preferences.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateApplicationCurrencyRequest(
    @NotBlank @Pattern(regexp = "EUR|USD") String currencyCode) {
}
