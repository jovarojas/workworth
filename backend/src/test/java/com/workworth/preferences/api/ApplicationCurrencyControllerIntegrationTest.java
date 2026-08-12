package com.workworth.preferences.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workworth.WorkWorthApplication;
import com.workworth.earnings.persistence.WorkdayEarningRepository;
import com.workworth.workday.application.WorkdayService;

import java.math.BigDecimal;
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
class ApplicationCurrencyControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkdayService workdayService;

    @Autowired
    private WorkdayEarningRepository earnings;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("workworth.time-zone", () -> "Europe/Madrid");
    }

    @BeforeEach
    void resetEconomicData() {
        jdbcTemplate.update("DELETE FROM earning_corrections");
        jdbcTemplate.update("DELETE FROM workday_earnings");
        jdbcTemplate.update("DELETE FROM partial_absences");
        jdbcTemplate.update("DELETE FROM workday_time_corrections");
        jdbcTemplate.update("DELETE FROM meal_breaks");
        jdbcTemplate.update("DELETE FROM workdays");
        jdbcTemplate.update("DELETE FROM salary_profiles");
        jdbcTemplate.update("UPDATE application_settings "
            + "SET currency_code = 'EUR', currency_locked_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = 1");
    }

    @Test
    void usesEurByDefaultThenUsesTheSelectedCurrencyUntilEconomicDataLocksIt() throws Exception {
        mockMvc.perform(get("/api/v1/application-settings/currency"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currencyCode").value("EUR"))
            .andExpect(jsonPath("$.changeAllowed").value(true));

        mockMvc.perform(get("/api/v1/earnings/periods/TODAY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("AVAILABLE"))
            .andExpect(jsonPath("$.amount").value(0.00))
            .andExpect(jsonPath("$.currencyCode").value("EUR"));

        mockMvc.perform(put("/api/v1/application-settings/currency")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currencyCode\":\"USD\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currencyCode").value("USD"))
            .andExpect(jsonPath("$.changeAllowed").value(true));

        mockMvc.perform(put("/api/v1/application-settings/currency")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currencyCode\":\"EUR\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currencyCode").value("EUR"))
            .andExpect(jsonPath("$.changeAllowed").value(true));

        mockMvc.perform(get("/api/v1/earnings/periods/TODAY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("AVAILABLE"))
            .andExpect(jsonPath("$.amount").value(0.00))
            .andExpect(jsonPath("$.currencyCode").value("EUR"));

        mockMvc.perform(put("/api/v1/application-settings/currency")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currencyCode\":\"USD\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currencyCode").value("USD"));

        mockMvc.perform(get("/api/v1/earnings/periods/TODAY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("AVAILABLE"))
            .andExpect(jsonPath("$.amount").value(0.00))
            .andExpect(jsonPath("$.currencyCode").value("USD"));

        mockMvc.perform(post("/api/v1/salary-profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(salaryRequest("EUR")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SALARY_PROFILE_CONFLICT"));

        mockMvc.perform(post("/api/v1/salary-profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(salaryRequest("USD")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.currencyCode").value("USD"));

        mockMvc.perform(put("/api/v1/application-settings/currency")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currencyCode\":\"EUR\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("APPLICATION_CURRENCY_LOCKED"));
    }

    @Test
    void preservesMaterializedEarningCurrencyAndAmountWhenACurrencyChangeIsRejected() throws Exception {
        mockMvc.perform(put("/api/v1/application-settings/currency")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currencyCode\":\"USD\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/salary-profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(salaryRequest("USD")))
            .andExpect(status().isCreated());

        LocalDate completedDate = LocalDate.of(2026, 8, 10);
        workdayService.reconcile(completedDate);
        var before = earnings.findByLocalDate(completedDate).orElseThrow();
        String originalCurrency = before.getCurrencyCode();
        BigDecimal originalAmount = before.getRawAmount();

        mockMvc.perform(put("/api/v1/application-settings/currency")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currencyCode\":\"EUR\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("APPLICATION_CURRENCY_LOCKED"));

        var after = earnings.findByLocalDate(completedDate).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(after.getCurrencyCode()).isEqualTo(originalCurrency);
        org.assertj.core.api.Assertions.assertThat(after.getRawAmount()).isEqualByComparingTo(originalAmount);
    }

    @Test
    void rejectsCurrenciesOutsideTheMvpConfiguration() throws Exception {
        mockMvc.perform(put("/api/v1/application-settings/currency")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currencyCode\":\"GBP\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private String salaryRequest(String currencyCode) {
        return """
            {
              "effectiveFrom": "2026-08-01",
              "grossAnnual": 19000.00,
              "netMonthlyReal": 1250.00,
              "currencyCode": "%s",
              "payPeriods": 12
            }
            """.formatted(currencyCode);
    }
}
