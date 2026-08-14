package com.workworth.identity.application;

import com.workworth.identity.domain.AppUserStatus;
import com.workworth.identity.persistence.AppUser;
import com.workworth.identity.persistence.AppUserRepository;

import java.time.Clock;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InitialAppUserProvisioner {

    private final AppUserRepository users;
    private final Clock clock;
    private final String subject;
    private final String email;

    public InitialAppUserProvisioner(AppUserRepository users, Clock clock,
                                     @Value("${workworth.identity.initial-subject:}") String subject,
                                     @Value("${workworth.identity.initial-email:}") String email) {
        this.users = users;
        this.clock = clock;
        this.subject = subject;
        this.email = email;
    }

    @Bean
    ApplicationRunner provisionInitialUser() {
        return arguments -> {
            if (!subject.isBlank() && !email.isBlank()
                && users.findByIdentitySubjectAndStatus(subject, AppUserStatus.ACTIVE).isEmpty()) {
                users.save(new AppUser(UUID.randomUUID(), subject, email, "Europe/Madrid", clock.instant()));
            }
        };
    }
}
