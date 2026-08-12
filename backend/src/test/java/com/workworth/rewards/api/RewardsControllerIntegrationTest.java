package com.workworth.rewards.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workworth.WorkWorthApplication;
import com.workworth.workday.application.WorkdayService;

import java.time.LocalDate;
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
@AutoConfigureMockMvc
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
}
