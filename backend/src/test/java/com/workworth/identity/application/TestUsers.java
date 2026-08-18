package com.workworth.identity.application;

import com.workworth.identity.persistence.AppUser;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

public final class TestUsers {

    private TestUsers() {
    }

    public static AppUser user(String subject) {
        return new AppUser(UUID.nameUUIDFromBytes(subject.getBytes(StandardCharsets.UTF_8)), subject,
            subject.replace('|', '-') + "@test.invalid", "Europe/Madrid", Instant.EPOCH);
    }
}
