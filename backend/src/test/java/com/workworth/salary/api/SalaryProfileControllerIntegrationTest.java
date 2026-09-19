package com.workworth.salary.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workworth.WorkWorthApplication;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = WorkWorthApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
class SalaryProfileControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("workworth.time-zone", () -> "Europe/Madrid");
    }

    @Test
    void createsARealMonthlyNetSalaryProfile() throws Exception {
        // Computed relative to the real system clock (this class does not override it) instead
        // of a hard-coded month, which would eventually fall before "the current month" and start
        // failing the effectiveFrom validation for reasons unrelated to what this test checks.
        LocalDate currentMonth = YearMonth.now().atDay(1);
        String request = """
                {
                  "effectiveFrom": "%s",
                  "grossAnnual": 19000.00,
                  "netMonthlyReal": 1250.00,
                  "currencyCode": "EUR",
                  "payPeriods": 12
                }
                """.formatted(currentMonth);

        mockMvc.perform(post("/api/v1/salary-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.netAnnualReal").value(15000.00))
                .andExpect(jsonPath("$.activeIncomeSource").value("NET_MONTHLY_REAL"));
    }

    // Computed relative to the real system clock (this class does not override it, matching
    // createsARealMonthlyNetSalaryProfile above) so the test stays correct regardless of when it
    // actually runs, instead of hard-coding a month that eventually becomes "the past".
    @Test
    void exposesASalaryChangeScheduledForAFutureMonthAsUpcoming() throws Exception {
        LocalDate nextMonth = YearMonth.now().plusMonths(1).atDay(1);
        String request = """
                {
                  "effectiveFrom": "%s",
                  "netMonthlyReal": 1500.00,
                  "currencyCode": "EUR",
                  "payPeriods": 12
                }
                """.formatted(nextMonth);

        mockMvc.perform(post("/api/v1/salary-profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/salary-profiles/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salaryProfile.effectiveFrom").value(nextMonth.toString()))
                .andExpect(jsonPath("$.salaryProfile.netMonthlyReal").value(1500.00));
    }
}
