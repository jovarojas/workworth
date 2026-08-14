package com.workworth.goals.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workworth.WorkWorthApplication;
import com.workworth.earnings.domain.EarningStatus;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = WorkWorthApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
class GoalsControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        ensureTestUser();
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
            + "SET currency_code = 'EUR', currency_locked_at = NULL, updated_at = CURRENT_TIMESTAMP "
            + "WHERE user_id = '00000000-0000-0000-0000-000000000001'");
    }

    private void ensureTestUser() {
        jdbcTemplate.update("INSERT INTO app_users (id, identity_subject, email, status, time_zone, created_at) "
            + "VALUES ('00000000-0000-0000-0000-000000000001', 'test|workworth', 'test@workworth.invalid', "
            + "'ACTIVE', 'Europe/Madrid', CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING");
    }

    @Test
    void createsActiveGoalsWithDynamicEffectiveAllTimeProgressAndTheGlobalCurrency() throws Exception {
        insertEarning(EarningStatus.AVAILABLE, new BigDecimal("25.00"), "EUR");

        var result = mockMvc.perform(post("/api/v1/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Viaje\",\"targetAmount\":100.00}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.currencyCode").value("EUR"))
            .andExpect(jsonPath("$.progress.evaluable").value(true))
            .andExpect(jsonPath("$.progress.progressAmount").value(25.00))
            .andExpect(jsonPath("$.progress.remainingAmount").value(75.00))
            .andExpect(jsonPath("$.progress.progressPercentage").value(25.00))
            .andExpect(jsonPath("$.progress.reached").value(false))
            .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/goals"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(put("/api/v1/application-settings/currency")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currencyCode\":\"USD\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("APPLICATION_CURRENCY_LOCKED"));
    }

    @Test
    void resolvesZeroProgressAsAvailableAndRejectsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Curso\",\"targetAmount\":50.00}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.progress.evaluable").value(true))
            .andExpect(jsonPath("$.progress.progressAmount").value(0.00))
            .andExpect(jsonPath("$.progress.reached").value(false));

        mockMvc.perform(post("/api/v1/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\",\"targetAmount\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors.title").exists())
            .andExpect(jsonPath("$.fieldErrors.targetAmount").exists());
    }

    @Test
    void supportsEditingCompletionCancellationAndClosedHistoryWithoutProgressSnapshots() throws Exception {
        insertEarning(EarningStatus.AVAILABLE, new BigDecimal("100.00"), "EUR");
        long completedId = createGoal("Viaje", "100.00");
        long cancelledId = createGoal("Curso", "200.00");

        mockMvc.perform(put("/api/v1/goals/{id}", completedId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Viaje largo\",\"targetAmount\":100.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Viaje largo"));
        mockMvc.perform(post("/api/v1/goals/{id}/complete", completedId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.closedAt").exists())
            .andExpect(jsonPath("$.progress").value(org.hamcrest.Matchers.nullValue()));
        mockMvc.perform(post("/api/v1/goals/{id}/cancel", cancelledId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(get("/api/v1/goals"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/v1/goals/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].progress").value(org.hamcrest.Matchers.nullValue()));
        mockMvc.perform(put("/api/v1/goals/{id}", completedId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"No\",\"targetAmount\":100.00}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("GOAL_CONFLICT"));
    }

    @Test
    void rejectsCompletionBeforeTheTargetAndWhenAllTimeIsUnavailable() throws Exception {
        insertEarning(EarningStatus.AVAILABLE, new BigDecimal("50.00"), "EUR");
        long unreachedId = createGoal("Viaje", "100.00");

        mockMvc.perform(post("/api/v1/goals/{id}/complete", unreachedId))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("GOAL_CONFLICT"));

        resetState();
        insertEarning(EarningStatus.UNAVAILABLE, null, null);
        long unavailableId = createGoal("Curso", "100.00");
        mockMvc.perform(get("/api/v1/goals/{id}", unavailableId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.progress.evaluable").value(false))
            .andExpect(jsonPath("$.progress.progressAmount").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.progress.reached").value(org.hamcrest.Matchers.nullValue()));
        mockMvc.perform(post("/api/v1/goals/{id}/complete", unavailableId))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("GOAL_PROGRESS_UNAVAILABLE"));
    }

    @Test
    void createsGoalsInTheCurrentUsdApplicationCurrencyWithoutConversion() throws Exception {
        mockMvc.perform(put("/api/v1/application-settings/currency")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currencyCode\":\"USD\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Book\",\"targetAmount\":20.00}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.targetAmount").value(20.00))
            .andExpect(jsonPath("$.currencyCode").value("USD"));
    }

    private long createGoal(String title, String amount) throws Exception {
        var result = mockMvc.perform(post("/api/v1/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"%s\",\"targetAmount\":%s}".formatted(title, amount)))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void insertEarning(EarningStatus status, BigDecimal amount, String currencyCode) {
        LocalDate date = LocalDate.now(ZoneId.of("Europe/Madrid"));
        Timestamp timestamp = Timestamp.from(Instant.now());
        Long workdayId = jdbcTemplate.queryForObject("""
            INSERT INTO workdays (user_id, local_date, time_zone, schedule_variant, scheduled_start, scheduled_end,
                maximum_economic_seconds, status, created_at, updated_at)
            VALUES ('00000000-0000-0000-0000-000000000001', ?, 'Europe/Madrid', 'STANDARD', '09:00', '17:00', 28800, 'COMPLETED', ?, ?)
            RETURNING id
            """, Long.class, date, timestamp, timestamp);
        jdbcTemplate.update("""
            INSERT INTO workday_earnings (workday_id, local_date, reference_month, status, economic_seconds,
                raw_amount, currency_code, materialized_at)
            VALUES (?, ?, ?, ?, 0, ?, ?, ?)
            """, workdayId, date, date.withDayOfMonth(1).toString().substring(0, 7), status.name(), amount,
            currencyCode, timestamp);
    }
}
