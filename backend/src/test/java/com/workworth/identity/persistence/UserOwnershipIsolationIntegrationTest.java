package com.workworth.identity.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.workworth.earnings.domain.EarningStatus;
import com.workworth.earnings.persistence.WorkdayEarning;
import com.workworth.earnings.persistence.WorkdayEarningRepository;
import com.workworth.goals.domain.GoalStatus;
import com.workworth.goals.persistence.Goal;
import com.workworth.goals.persistence.GoalRepository;
import com.workworth.preferences.domain.ApplicationCurrency;
import com.workworth.preferences.persistence.ApplicationSettings;
import com.workworth.preferences.persistence.ApplicationSettingsRepository;
import com.workworth.rewards.domain.RewardStatus;
import com.workworth.rewards.persistence.Reward;
import com.workworth.rewards.persistence.RewardRepository;
import com.workworth.salary.persistence.SalaryProfile;
import com.workworth.salary.persistence.SalaryProfileRepository;
import com.workworth.workday.domain.ScheduleVariant;
import com.workworth.workday.persistence.Workday;
import com.workworth.workday.persistence.WorkdayRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
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
class UserOwnershipIsolationIntegrationTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 14);
    private static final LocalDate SALARY_EFFECTIVE_FROM = LocalDate.of(2026, 8, 1);
    private static final Instant NOW = Instant.parse("2026-08-14T10:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private AppUserRepository users;
    @Autowired private ApplicationSettingsRepository settings;
    @Autowired private SalaryProfileRepository salaries;
    @Autowired private WorkdayRepository workdays;
    @Autowired private WorkdayEarningRepository earnings;
    @Autowired private RewardRepository rewards;
    @Autowired private GoalRepository goals;

    @Test
    void keepsAllOwnedRootsAndDerivedEarningsIsolatedBetweenUsers() {
        AppUser first = users.save(user("auth0|first", "first@example.test", "Europe/Madrid"));
        AppUser second = users.save(user("auth0|second", "second@example.test", "America/New_York"));
        settings.save(new ApplicationSettings(first, ApplicationCurrency.EUR, NOW));
        settings.save(new ApplicationSettings(second, ApplicationCurrency.USD, NOW));
        salaries.save(new SalaryProfile(first, SALARY_EFFECTIVE_FROM, null, new BigDecimal("1200.00"), "EUR", 12, NOW));
        salaries.save(new SalaryProfile(second, SALARY_EFFECTIVE_FROM, null, new BigDecimal("1500.00"), "USD", 12, NOW));
        Workday firstDay = workdays.save(workday(first));
        Workday secondDay = workdays.save(workday(second));
        earnings.save(earning(firstDay, "EUR"));
        earnings.save(earning(secondDay, "USD"));
        Reward firstReward = rewards.save(new Reward(first, "Libro", 1, new BigDecimal("20.00"), "EUR", NOW));
        rewards.save(new Reward(second, "Curso", 1, new BigDecimal("30.00"), "USD", NOW));
        Goal firstGoal = goals.save(new Goal(first, "Viaje", new BigDecimal("100.00"), "EUR", NOW));
        goals.save(new Goal(second, "Mudanza", new BigDecimal("200.00"), "USD", NOW));

        assertThat(workdays.findByUserIdAndLocalDate(first.getId(), DATE)).contains(firstDay);
        assertThat(workdays.findByUserIdAndLocalDate(second.getId(), DATE)).contains(secondDay);
        assertThat(salaries.findAllByUserIdOrderByEffectiveFromDesc(first.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
            .getContent()).hasSize(1).allMatch(profile -> profile.getUser().getId().equals(first.getId()));
        assertThat(rewards.findAllByUserIdAndStatusOrderByIdAsc(first.getId(), RewardStatus.PENDING))
            .extracting(Reward::getId).containsExactly(firstReward.getId());
        assertThat(goals.findAllByUserIdAndStatusOrderByIdAsc(first.getId(), GoalStatus.ACTIVE))
            .extracting(Goal::getId).containsExactly(firstGoal.getId());
        assertThat(earnings.findAllByWorkdayOwnerId(first.getId())).hasSize(1)
            .allMatch(earning -> earning.getWorkdayId().equals(firstDay.getId()));
        assertThat(settings.findById(first.getId())).hasValueSatisfying(value ->
            assertThat(value.getCurrencyCode()).isEqualTo(ApplicationCurrency.EUR));
        assertThat(settings.findById(second.getId())).hasValueSatisfying(value ->
            assertThat(value.getCurrencyCode()).isEqualTo(ApplicationCurrency.USD));
    }

    private AppUser user(String subject, String email, String zone) {
        return new AppUser(UUID.randomUUID(), subject, email, zone, NOW);
    }

    private Workday workday(AppUser user) {
        return new Workday(user, DATE, user.getTimeZone(), ScheduleVariant.NORMAL,
            LocalTime.of(9, 0), LocalTime.of(17, 0), 28_800, NOW);
    }

    private WorkdayEarning earning(Workday workday, String currencyCode) {
        return new WorkdayEarning(workday.getId(), DATE, EarningStatus.AVAILABLE, 3_600,
            new BigDecimal("10.00"), null, null, null, null, 0, currencyCode, null, null, NOW);
    }
}
