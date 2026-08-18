package com.workworth.preferences.api;

import com.workworth.preferences.api.dto.ApplicationCurrencyResponse;
import com.workworth.preferences.api.dto.UpdateApplicationCurrencyRequest;
import com.workworth.preferences.application.ApplicationCurrencyService;
import com.workworth.preferences.domain.ApplicationCurrency;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/application-settings/currency")
public class ApplicationCurrencyController {

    private final ApplicationCurrencyService currencyService;

    public ApplicationCurrencyController(ApplicationCurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @GetMapping
    public ResponseEntity<ApplicationCurrencyResponse> getCurrency() {
        return ResponseEntity.ok(ApplicationCurrencyResponse.from(currencyService.getSettings()));
    }

    @PutMapping
    public ResponseEntity<ApplicationCurrencyResponse> updateCurrency(
        @Valid @RequestBody UpdateApplicationCurrencyRequest request) {
        return ResponseEntity.ok(ApplicationCurrencyResponse.from(
            currencyService.updateCurrency(ApplicationCurrency.valueOf(request.currencyCode()))));
    }
}
