package com.workworth.preferences.persistence;

import com.workworth.preferences.domain.ApplicationCurrency;
import com.workworth.identity.persistence.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

@Getter
@Entity
@Table(name = "application_settings")
public class ApplicationSettings {

    @Id
    private UUID userId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

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

    public ApplicationSettings(AppUser user, ApplicationCurrency currencyCode, Instant createdAt) {
        this.user = user;
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
