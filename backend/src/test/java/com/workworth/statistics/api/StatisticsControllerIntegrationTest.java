package com.workworth.statistics.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workworth.WorkWorthApplication;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;

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
class StatisticsControllerIntegrationTest {

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
        jdbcTemplate.update("DELETE FROM goals");
        jdbcTemplate.update("DELETE FROM earning_corrections");
        jdbcTemplate.update("DELETE FROM workday_earnings");
        jdbcTemplate.update("DELETE FROM partial_absences");
        jdbcTemplate.update("DELETE FROM workday_time_corrections");
        jdbcTemplate.update("DELETE FROM meal_breaks");
        jdbcTemplate.update("DELETE FROM workdays");
        jdbcTemplate.update("DELETE FROM salary_profiles");
        jdbcTemplate.update("DELETE FROM rewards");
        jdbcTemplate.update("UPDATE application_settings "
            + "SET currency_code = 'EUR', currency_locked_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = 1");
    }

    @Test
    void returnsTypedDailyStatisticsWithEffectiveEarningsAndCompletedGoals() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 10);
        insertEarning(date, "AVAILABLE", 7_200, new BigDecimal("25.00"), "EUR");
        insertGoal("Viaje", "COMPLETED", Instant.parse("2026-08-10T10:00:00Z"));
        insertGoal("Curso", "CANCELLED", Instant.parse("2026-08-10T11:00:00Z"));

        mockMvc.perform(get("/api/v1/statistics?granularity=DAY&from=2026-08-10&to=2026-08-10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.granularity").value("DAY"))
            .andExpect(jsonPath("$.points.length()").value(1))
            .andExpect(jsonPath("$.points[0].workedHours.status").value("AVAILABLE"))
            .andExpect(jsonPath("$.points[0].workedHours.value").value(2.00))
            .andExpect(jsonPath("$.points[0].totalEarnings.amount").value(25.00))
            .andExpect(jsonPath("$.points[0].totalEarnings.currencyCode").value("EUR"))
            .andExpect(jsonPath("$.points[0].averageHourlyEarnings.amount").value(12.50))
            .andExpect(jsonPath("$.points[0].completedGoals.count").value(1));
    }

    @Test
    void distinguishesAValidZeroFromUnavailableMonetaryStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/statistics?granularity=DAY&from=2026-08-10&to=2026-08-10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.points[0].workedHours.value").value(0.00))
            .andExpect(jsonPath("$.points[0].totalEarnings.status").value("AVAILABLE"))
            .andExpect(jsonPath("$.points[0].totalEarnings.amount").value(0.00))
            .andExpect(jsonPath("$.points[0].averageHourlyEarnings.status").value("UNAVAILABLE"))
            .andExpect(jsonPath("$.points[0].averageHourlyEarnings.amount").value(org.hamcrest.Matchers.nullValue()));

        insertEarning(LocalDate.of(2026, 8, 11), "UNAVAILABLE", 3_600, null, null);
        mockMvc.perform(get("/api/v1/statistics?granularity=DAY&from=2026-08-11&to=2026-08-11"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.points[0].workedHours.value").value(1.00))
            .andExpect(jsonPath("$.points[0].totalEarnings.status").value("UNAVAILABLE"))
            .andExpect(jsonPath("$.points[0].totalEarnings.amount").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void rejectsIncompleteAndOversizedRangesWithValidationProblemDetails() throws Exception {
        mockMvc.perform(get("/api/v1/statistics?granularity=DAY&from=2026-08-10"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/statistics?granularity=DAY&from=2025-01-01&to=2026-01-02"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsAnInconsistentHistoricalCurrencyWithoutConversion() throws Exception {
        insertEarning(LocalDate.of(2026, 8, 10), "AVAILABLE", 3_600, new BigDecimal("20.00"), "USD");

        mockMvc.perform(get("/api/v1/statistics?granularity=DAY&from=2026-08-10&to=2026-08-10"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("STATISTICS_CURRENCY_MISMATCH"));
    }

    private void insertGoal(String title, String status, Instant closedAt) {
        Timestamp timestamp = Timestamp.from(closedAt);
        jdbcTemplate.update("""
            INSERT INTO goals (title, target_amount, currency_code, status, created_at, updated_at, closed_at)
            VALUES (?, 100.00, 'EUR', ?, ?, ?, ?)
            """, title, status, timestamp, timestamp, timestamp);
    }

    private void insertEarning(LocalDate date, String status, long economicSeconds, BigDecimal amount, String currencyCode) {
        Timestamp timestamp = Timestamp.from(Instant.parse("2026-08-13T10:00:00Z"));
        Long workdayId = jdbcTemplate.queryForObject("""
            INSERT INTO workdays (local_date, time_zone, schedule_variant, scheduled_start, scheduled_end,
                maximum_economic_seconds, status, created_at, updated_at)
            VALUES (?, 'Europe/Madrid', 'STANDARD', '09:00', '17:00', 28800, 'COMPLETED', ?, ?)
            RETURNING id
            """, Long.class, date, timestamp, timestamp);
        jdbcTemplate.update("""
            INSERT INTO workday_earnings (workday_id, local_date, reference_month, status, economic_seconds,
                raw_amount, currency_code, materialized_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, workdayId, date, date.withDayOfMonth(1).toString().substring(0, 7), status, economicSeconds,
            amount, currencyCode, timestamp);
    }
}
