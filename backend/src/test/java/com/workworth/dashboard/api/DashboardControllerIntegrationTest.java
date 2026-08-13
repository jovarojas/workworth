package com.workworth.dashboard.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workworth.WorkWorthApplication;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = WorkWorthApplication.class)
@AutoConfigureMockMvc
@Testcontainers
class DashboardControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void resetState() {
        jdbcTemplate.update("DELETE FROM earning_corrections");
        jdbcTemplate.update("DELETE FROM workday_earnings");
        jdbcTemplate.update("DELETE FROM rewards");
        jdbcTemplate.update("DELETE FROM workdays");
        jdbcTemplate.update("UPDATE application_settings "
            + "SET currency_code = 'EUR', currency_locked_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = 1");
    }

    @Test
    void returnsCurrentMotivationWithoutChangingTheRewardMarkerAcrossRepeatedReads() throws Exception {
        long rewardId = insertReward("Auriculares", new BigDecimal("120.00"), "MONTH");
        insertAvailableEarning(new BigDecimal("120.00"));

        mockMvc.perform(get("/api/v1/dashboard/motivation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("AVAILABLE"))
            .andExpect(jsonPath("$.primaryReward.reward.id").value(rewardId))
            .andExpect(jsonPath("$.primaryReward.relevantContext").value("TODAY"))
            .andExpect(jsonPath("$.primaryReward.reward.lastReachedContext").doesNotExist())
            .andExpect(jsonPath("$.combination").value(org.hamcrest.Matchers.nullValue()));
        mockMvc.perform(get("/api/v1/dashboard/motivation"))
            .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT last_reached_context FROM rewards WHERE id = ?", String.class,
            rewardId)).isEqualTo("MONTH");
    }

    private long insertReward(String name, BigDecimal price, String lastReachedContext) {
        Timestamp timestamp = Timestamp.from(Instant.now());
        return jdbcTemplate.queryForObject("""
            INSERT INTO rewards (name, quantity, price, currency_code, status, last_reached_context, created_at, updated_at)
            VALUES (?, 1, ?, 'EUR', 'PENDING', ?, ?, ?)
            RETURNING id
            """, Long.class, name, price, lastReachedContext, timestamp, timestamp);
    }

    private void insertAvailableEarning(BigDecimal amount) {
        LocalDate date = LocalDate.now(ZoneId.of("Europe/Madrid"));
        Timestamp timestamp = Timestamp.from(Instant.now());
        Long workdayId = jdbcTemplate.queryForObject("""
            INSERT INTO workdays (local_date, time_zone, schedule_variant, scheduled_start, scheduled_end,
                maximum_economic_seconds, status, created_at, updated_at)
            VALUES (?, 'Europe/Madrid', 'STANDARD', '09:00', '17:00', 28800, 'COMPLETED', ?, ?)
            ON CONFLICT (local_date) DO UPDATE SET updated_at = EXCLUDED.updated_at
            RETURNING id
            """, Long.class, date, timestamp, timestamp);
        jdbcTemplate.update("""
            INSERT INTO workday_earnings (workday_id, local_date, reference_month, status, economic_seconds,
                raw_amount, materialized_at)
            VALUES (?, ?, ?, 'AVAILABLE', 0, ?, ?)
            """, workdayId, date, date.withDayOfMonth(1).toString().substring(0, 7), amount, timestamp);
    }
}
