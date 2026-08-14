package com.workworth.identity.application;

import com.workworth.identity.domain.AppUserStatus;
import com.workworth.identity.persistence.AppUser;
import com.workworth.identity.persistence.AppUserRepository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestCurrentUserProvider implements CurrentUserProvider {

    private static final String SUBJECT = "test|workworth";
    private final AppUserRepository users;

    public TestCurrentUserProvider(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public AppUser currentUser() {
        return users.findByIdentitySubjectAndStatus(SUBJECT, AppUserStatus.ACTIVE)
            .orElseGet(() -> users.save(new AppUser(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                SUBJECT, "test@workworth.invalid", "Europe/Madrid", Instant.EPOCH)));
    }
}
