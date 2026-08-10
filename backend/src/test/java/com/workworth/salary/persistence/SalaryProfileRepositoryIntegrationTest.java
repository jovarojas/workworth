package com.workworth.salary.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SalaryProfileRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SalaryProfileRepository salaryProfileRepository;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void findsTheLatestEffectiveProfileForAMonth() {
        salaryProfileRepository.save(new SalaryProfile(
                LocalDate.of(2026, 8, 1), new BigDecimal("19000.00"), new BigDecimal("1250.00"),
                "EUR", 12, Instant.parse("2026-08-01T00:00:00Z")));
        salaryProfileRepository.save(new SalaryProfile(
                LocalDate.of(2026, 9, 1), new BigDecimal("20000.00"), new BigDecimal("1300.00"),
                "EUR", 12, Instant.parse("2026-09-01T00:00:00Z")));

        SalaryProfile profile = salaryProfileRepository
                .findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate.of(2026, 9, 1))
                .orElseThrow();

        assertThat(profile.getNetMonthlyReal()).isEqualByComparingTo("1300.00");
    }
}
