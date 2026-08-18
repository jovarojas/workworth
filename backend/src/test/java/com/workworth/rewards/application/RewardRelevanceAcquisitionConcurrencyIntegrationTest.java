package com.workworth.rewards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.workworth.WorkWorthApplication;
import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.rewards.api.dto.CreateRewardRequest;
import com.workworth.rewards.domain.RewardStatus;
import com.workworth.rewards.persistence.RewardRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = WorkWorthApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class RewardRelevanceAcquisitionConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RewardService rewards;

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

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
    void resetState() {
        ensureTestUser();
        jdbcTemplate.update("DELETE FROM rewards");
        jdbcTemplate.update("UPDATE application_settings "
            + "SET currency_code = 'EUR', currency_locked_at = NULL, updated_at = CURRENT_TIMESTAMP "
            + "WHERE user_id = '00000000-0000-0000-0000-000000000001'");
        executor = Executors.newFixedThreadPool(2);
    }

    private void ensureTestUser() {
        jdbcTemplate.update("INSERT INTO app_users (id, identity_subject, email, status, time_zone, created_at) "
            + "VALUES ('00000000-0000-0000-0000-000000000001', 'test|workworth', 'test@workworth.invalid', "
            + "'ACTIVE', 'Europe/Madrid', CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING");
    }

    @AfterEach
    void shutDownExecutor() {
        executor.shutdownNow();
    }

    @Test
    void serializesARelevanceMarkerWriteAndAcquisitionWithoutRestoringPending() throws Exception {
        var reward = rewards.create(new CreateRewardRequest("Auriculares", 1, new BigDecimal("120.00")));
        CountDownLatch relevanceHasLock = new CountDownLatch(1);
        CountDownLatch releaseRelevance = new CountDownLatch(1);
        CountDownLatch acquisitionStarted = new CountDownLatch(1);
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);

        Future<Void> relevance = executor.submit(() -> {
            transactions.executeWithoutResult(status -> {
                var pending = rewards.pendingForUpdate(reward.getId());
                relevanceHasLock.countDown();
                await(releaseRelevance);
                pending.updateLastReachedContext(EarningPeriod.WEEK, Instant.now());
            });
            return null;
        });

        assertThat(relevanceHasLock.await(5, TimeUnit.SECONDS)).isTrue();
        Future<?> acquisition = executor.submit(() -> {
            acquisitionStarted.countDown();
            return rewards.acquire(reward.getId());
        });
        assertThat(acquisitionStarted.await(5, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> acquisition.get(250, TimeUnit.MILLISECONDS))
            .isInstanceOf(TimeoutException.class);

        releaseRelevance.countDown();
        relevance.get(5, TimeUnit.SECONDS);
        acquisition.get(5, TimeUnit.SECONDS);

        var persisted = rewardRepository.findById(reward.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(RewardStatus.ACQUIRED);
        assertThat(persisted.getLastReachedContext()).isEqualTo(EarningPeriod.WEEK);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the concurrent transaction.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the concurrent transaction.", exception);
        }
    }
}
