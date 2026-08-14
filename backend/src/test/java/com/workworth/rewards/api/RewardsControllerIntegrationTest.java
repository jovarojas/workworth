package com.workworth.rewards.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workworth.WorkWorthApplication;
import com.workworth.earnings.domain.EarningStatus;
import com.workworth.workday.application.WorkdayService;

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
class RewardsControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkdayService workdays;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void resetState() {
        ensureTestUser();
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
    void supportsRewardCrudAcquisitionAndTheGlobalCurrencyLock() throws Exception {
        var created = mockMvc.perform(post("/api/v1/rewards")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Hamburguesas\",\"quantity\":2,\"price\":30.00}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.quantity").value(2))
            .andExpect(jsonPath("$.price").value(30.00))
            .andExpect(jsonPath("$.currencyCode").value("EUR"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/rewards/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Dos hamburguesas\",\"quantity\":2,\"price\":30.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Dos hamburguesas"));
        mockMvc.perform(post("/api/v1/rewards/{id}/acquire", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACQUIRED"));
        mockMvc.perform(post("/api/v1/rewards/{id}/acquire", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACQUIRED"));
        mockMvc.perform(get("/api/v1/rewards?status=PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/v1/rewards?status=ACQUIRED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(get("/api/v1/rewards/{id}/evaluations/TODAY", id))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("REWARD_CONFLICT"));
        mockMvc.perform(put("/api/v1/application-settings/currency")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currencyCode\":\"USD\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("APPLICATION_CURRENCY_LOCKED"));
        mockMvc.perform(delete("/api/v1/rewards/{id}", id))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/rewards/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void validatesRewardInputWithTheExistingProblemDetailContract() throws Exception {
        mockMvc.perform(post("/api/v1/rewards")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"quantity\":0,\"price\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors.quantity").exists())
            .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void rejectsANullPriceWithoutCreatingAReward() throws Exception {
        mockMvc.perform(post("/api/v1/rewards")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Auriculares\",\"quantity\":1,\"price\":null}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors.price").exists());

        mockMvc.perform(get("/api/v1/rewards"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void evaluatesTheLatestEffectiveAmountAfterAnEarningCorrection() throws Exception {
        mockMvc.perform(post("/api/v1/salary-profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "effectiveFrom": "2026-08-01",
                      "grossAnnual": 19000.00,
                      "netMonthlyReal": 1250.00,
                      "currencyCode": "EUR",
                      "payPeriods": 12
                    }
                    """))
            .andExpect(status().isCreated());
        LocalDate completedDate = LocalDate.of(2026, 8, 10);
        workdays.reconcile(completedDate);
        workdays.cancel(completedDate, "test correction");

        var created = mockMvc.perform(post("/api/v1/rewards")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Libro\",\"price\":1.00}"))
            .andExpect(status().isCreated())
            .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/rewards/{id}/evaluations/ALL_TIME", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome").value("SHORTFALL"))
            .andExpect(jsonPath("$.availableAmount").value(0.00))
            .andExpect(jsonPath("$.shortfall").value(1.00));
    }

    @Test
    void returnsTheFirstRelevantCombinationOverHttpWithoutPersistingIt() throws Exception {
        insertEarning(EarningStatus.AVAILABLE, new BigDecimal("90.00"));
        createReward("Hamburguesas", 2, "30.00");
        createReward("Funkos", 2, "60.00");
        int rewardsBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rewards", Integer.class);

        mockMvc.perform(get("/api/v1/rewards/combinations/relevance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.evaluable").value(true))
            .andExpect(jsonPath("$.combination.context").value("TODAY"))
            .andExpect(jsonPath("$.combination.rewards.length()").value(2))
            .andExpect(jsonPath("$.combination.totalPrice").value(90.00));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rewards", Integer.class))
            .isEqualTo(rewardsBefore);
    }

    @Test
    void distinguishesEvaluableContextsWithoutACombinationOverHttp() throws Exception {
        createReward("Hamburguesas", 2, "30.00");
        createReward("Funkos", 2, "60.00");

        mockMvc.perform(get("/api/v1/rewards/combinations/relevance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.evaluable").value(true))
            .andExpect(jsonPath("$.combination").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void distinguishesAllUnavailableContextsOverHttp() throws Exception {
        insertEarning(EarningStatus.UNAVAILABLE, null);
        createReward("Hamburguesas", 2, "30.00");
        createReward("Funkos", 2, "60.00");

        mockMvc.perform(get("/api/v1/rewards/combinations/relevance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.evaluable").value(false))
            .andExpect(jsonPath("$.combination").value(org.hamcrest.Matchers.nullValue()));
    }

    private void createReward(String name, int quantity, String price) throws Exception {
        mockMvc.perform(post("/api/v1/rewards")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"quantity\":%d,\"price\":%s}".formatted(name, quantity, price)))
            .andExpect(status().isCreated());
    }

    private void insertEarning(EarningStatus status, BigDecimal amount) {
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
                raw_amount, materialized_at)
            VALUES (?, ?, ?, ?, 0, ?, ?)
            """, workdayId, date, date.withDayOfMonth(1).toString().substring(0, 7), status.name(), amount,
            timestamp);
    }
}
