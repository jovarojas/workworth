package com.workworth.identity.persistence;

import com.workworth.identity.domain.AppUserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

@Getter
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    private UUID id;

    @Column(name = "identity_subject", nullable = false, unique = true, length = 255)
    private String identitySubject;

    @Column(nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AppUserStatus status;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    protected AppUser() {
    }

    public AppUser(UUID id, String identitySubject, String email, String timeZone, Instant createdAt) {
        this.id = id;
        this.identitySubject = identitySubject;
        this.email = email;
        this.timeZone = timeZone;
        this.status = AppUserStatus.ACTIVE;
        this.createdAt = createdAt;
    }
}
