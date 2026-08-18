package com.workworth.workday.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.workworth.WorkWorthApplication;
import com.workworth.identity.persistence.AppUser;
import com.workworth.identity.persistence.AppUserRepository;
import com.workworth.workday.persistence.WorkdayRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = WorkWorthApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class WorkdayReconciliationConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WorkdayService workdays;

    @Autowired
    private WorkdayRepository repository;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM workdays");
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void concurrentReconciliationsCreateAndReturnOnlyOneWorkday() throws Exception {
        AppUser user = users.save(new AppUser(UUID.randomUUID(), "test|workday-race-" + UUID.randomUUID(),
            "workday-race-" + UUID.randomUUID() + "@test.invalid", "Europe/Madrid", Instant.now()));
        LocalDate date = LocalDate.of(2027, 7, 6);
        CountDownLatch start = new CountDownLatch(1);

        Future<Long> first = executor.submit(() -> reconcileAfter(start, user, date));
        Future<Long> second = executor.submit(() -> reconcileAfter(start, user, date));
        start.countDown();

        Long firstId = first.get(5, TimeUnit.SECONDS);
        Long secondId = second.get(5, TimeUnit.SECONDS);

        assertThat(firstId).isEqualTo(secondId);
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), date)).isPresent();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM workdays WHERE user_id = ? AND local_date = ?", Integer.class, user.getId(), date
        )).isEqualTo(1);
    }

    private Long reconcileAfter(CountDownLatch start, AppUser user, LocalDate date) throws Exception {
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to reconcile the workday.");
        }
        return workdays.reconcile(user, date).getId();
    }
}
