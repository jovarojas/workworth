package com.workworth.workday.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.workworth.WorkWorthApplication;
import com.workworth.identity.persistence.AppUser;
import com.workworth.identity.persistence.AppUserRepository;
import com.workworth.workday.persistence.WorkdayRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * WorkdayLifecycleScheduler.reconcileToday() used to wrap the whole
 * {@code findAllByStatus(...).forEach(...)} in a single try/catch: one user's exception aborted
 * the Java Stream mid-iteration, silently skipping every ACTIVE user still to come in that tick
 * and leaving no trace at all. The try/catch now lives inside the loop, per user, with logging.
 */
@SpringBootTest(classes = {WorkWorthApplication.class, WorkdayLifecycleSchedulerIntegrationTest.FixedClockConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class WorkdayLifecycleSchedulerIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2028, 3, 13); // Monday

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WorkdayLifecycleScheduler scheduler;

    @Autowired
    private WorkdayRepository repository;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM workday_earnings");
        jdbcTemplate.update("DELETE FROM workdays");
    }

    @Test
    void oneUsersFailureDoesNotBlockReconciliationOfOtherActiveUsers() {
        // An invalid time zone makes ZoneId.of(...) throw a RuntimeException the moment
        // reconcileThroughToday() tries to compute "today" for this user -- a deterministic,
        // easily reproduced failure standing in for "anything can go wrong reconciling one user".
        AppUser broken = users.save(new AppUser(UUID.randomUUID(), "test|broken-" + UUID.randomUUID(),
            "broken@test.invalid", "Not/AZone", Instant.now()));
        AppUser healthy = users.save(new AppUser(UUID.randomUUID(), "test|healthy-" + UUID.randomUUID(),
            "healthy@test.invalid", "Europe/Madrid", Instant.now()));

        scheduler.reconcileToday();

        assertThat(repository.findByUserIdAndLocalDate(broken.getId(), TODAY)).isEmpty();
        assertThat(repository.findByUserIdAndLocalDate(healthy.getId(), TODAY)).isPresent();
    }

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(TODAY.atTime(12, 0).atZone(ZoneId.of("Europe/Madrid")).toInstant(), ZoneId.of("Europe/Madrid"));
        }
    }
}
