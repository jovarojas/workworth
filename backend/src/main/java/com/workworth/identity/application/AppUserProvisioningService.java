package com.workworth.identity.application;

import com.workworth.identity.persistence.AppUser;
import com.workworth.identity.persistence.AppUserRepository;

import java.time.Clock;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserProvisioningService {

    private final AppUserRepository users;
    private final Clock clock;
    private final String defaultTimeZone;

    public AppUserProvisioningService(AppUserRepository users, Clock clock,
                                      @Value("${workworth.identity.default-time-zone:Europe/Madrid}") String defaultTimeZone) {
        this.users = users;
        this.clock = clock;
        this.defaultTimeZone = defaultTimeZone;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppUser provision(String subject, String email) {
        return users.saveAndFlush(new AppUser(UUID.randomUUID(), subject, email, defaultTimeZone, clock.instant()));
    }
}
