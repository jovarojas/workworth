package com.workworth.preferences.persistence;

import com.workworth.preferences.domain.ApplicationCurrency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

import lombok.Getter;

@Getter
@Entity
@Table(name = "application_settings")
public class ApplicationSettings {

    @Id
    private Short id;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_code", nullable = false, length = 3)
    private ApplicationCurrency currencyCode;

    @Column(name = "currency_locked_at")
    private Instant currencyLockedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApplicationSettings() {
    }

    public ApplicationSettings(ApplicationCurrency currencyCode, Instant createdAt) {
        this.id = 1;
        this.currencyCode = currencyCode;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void changeCurrency(ApplicationCurrency currencyCode, Instant updatedAt) {
        this.currencyCode = currencyCode;
        this.updatedAt = updatedAt;
    }

    public void lockCurrency(Instant lockedAt) {
        if (currencyLockedAt == null) {
            currencyLockedAt = lockedAt;
            updatedAt = lockedAt;
        }
    }
}
