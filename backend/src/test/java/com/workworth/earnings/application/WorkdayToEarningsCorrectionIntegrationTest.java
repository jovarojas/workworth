package com.workworth.earnings.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.workworth.WorkWorthApplication;
import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.earnings.domain.EarningCorrectionCause;
import com.workworth.earnings.persistence.EarningCorrectionRepository;
import com.workworth.earnings.persistence.WorkdayEarningRepository;
import com.workworth.salary.persistence.SalaryProfile;
import com.workworth.salary.persistence.SalaryProfileRepository;
import com.workworth.workday.application.WorkdayService;
import com.workworth.workday.domain.ScheduleVariant;
import com.workworth.workday.domain.WorkdayStatus;
import com.workworth.workday.persistence.Workday;
import com.workworth.workday.persistence.MealBreakRepository;
import com.workworth.workday.persistence.PartialAbsenceRepository;
import com.workworth.workday.persistence.WorkdayRepository;
import com.workworth.workday.persistence.WorkdayTimeCorrectionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(classes = WorkWorthApplication.class)
@Testcontainers
@AutoConfigureMockMvc
@Import(WorkdayToEarningsCorrectionIntegrationTest.FixedClockConfiguration.class)
class WorkdayToEarningsCorrectionIntegrationTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Madrid");
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 6);
    private static final LocalDate HISTORICAL_WORKDAY = LocalDate.of(2026, 7, 3);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private WorkdayService workdayService;
    @Autowired private EarningMaterializationService materializationService;
    @Autowired private EarningPeriodService periodService;
    @Autowired private WorkdayRepository workdays;
    @Autowired private WorkdayTimeCorrectionRepository workdayCorrections;
    @Autowired private WorkdayEarningRepository earnings;
    @Autowired private EarningCorrectionRepository earningCorrections;
    @Autowired private PartialAbsenceRepository absences;
    @Autowired private MealBreakRepository mealBreaks;
    @Autowired private SalaryProfileRepository salaryProfiles;
    @Autowired private MockMvc mvc;

    @BeforeEach
    void cleanDatabase() {
        earningCorrections.deleteAllInBatch();
        earnings.deleteAllInBatch();
        absences.deleteAllInBatch();
        workdayCorrections.deleteAllInBatch();
        mealBreaks.deleteAllInBatch();
        workdays.deleteAllInBatch();
        salaryProfiles.deleteAllInBatch();
        salaryProfiles.save(new SalaryProfile(
                LocalDate.of(2026, 7, 1), new BigDecimal("19000.00"), new BigDecimal("1300.00"),
                "EUR", 12, Instant.parse("2026-07-01T00:00:00Z")));
    }

    @Test
    void cancellingACompletedWorkdayCreatesAnEffectiveEarningRevisionInTheSameFlow() {
        var workday = workdayService.reconcile(TODAY);
        var base = materializationService.materialize(workday);

        workdayService.cancel(TODAY, "holiday");

        var timeCorrection = workdayCorrections.findAll().get(0);
        var monetaryCorrection = earningCorrections.findAll().get(0);
        var allTime = periodService.summarize(EarningPeriod.ALL_TIME);
        var today = periodService.summarize(EarningPeriod.TODAY);
        var week = periodService.summarize(EarningPeriod.WEEK);
        var month = periodService.summarize(EarningPeriod.MONTH);

        assertThat(base.getRawAmount()).isPositive();
        assertThat(timeCorrection.getWorkday().getId()).isEqualTo(workday.getId());
        assertThat(monetaryCorrection.getWorkdayTimeCorrectionId()).isEqualTo(timeCorrection.getId());
        assertThat(monetaryCorrection.getPreviousAmount()).isEqualByComparingTo(base.getRawAmount());
        assertThat(monetaryCorrection.getNewAmount()).isEqualByComparingTo(BigDecimal.ZERO.setScale(12));
        assertThat(monetaryCorrection.getNewEconomicSeconds()).isZero();
        assertThat(allTime.publicAmount()).isEqualByComparingTo("0.00");
        assertThat(today.publicAmount()).isEqualByComparingTo("0.00");
        assertThat(week.publicAmount()).isEqualByComparingTo("0.00");
        assertThat(month.publicAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void completingAWorkdayMaterializesItsBaseEarningExactlyOnce() {
        var workday = workdayService.reconcile(TODAY);

        assertThat(workday.getStatus().name()).isEqualTo("COMPLETED");
        assertThat(earnings.count()).isEqualTo(1);

        workdayService.reconcile(TODAY);

        assertThat(earnings.count()).isEqualTo(1);
    }

    @Test
    void earningsApiUsesDtosForQueriesAndStableProblemDetailsForInvalidRequests() throws Exception {
        workdayService.reconcile(TODAY);
        workdayService.cancel(TODAY, "holiday");

        mvc.perform(get("/api/v1/earnings/current/projection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
        mvc.perform(get("/api/v1/earnings/periods/TODAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("TODAY"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.amount").value(0.00));
        mvc.perform(get("/api/v1/earnings/periods/WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("WEEK"));
        mvc.perform(get("/api/v1/earnings/periods/MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("MONTH"));
        mvc.perform(get("/api/v1/earnings/periods/ALL_TIME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("ALL_TIME"));
        mvc.perform(get("/api/v1/earnings/workdays/{date}", TODAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localDate").value(TODAY.toString()))
                .andExpect(jsonPath("$.amount").value(0.00))
                .andExpect(jsonPath("$.economicSeconds").value(0));
        mvc.perform(get("/api/v1/earnings/history?page=0&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].localDate").value(TODAY.toString()))
                .andExpect(jsonPath("$.items[0].amount").value(0.00))
                .andExpect(jsonPath("$.items[0].economicSeconds").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));
        mvc.perform(get("/api/v1/earnings/workdays/{date}/corrections", TODAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cause").value("WORKDAY_CANCELLED"));
        mvc.perform(get("/api/v1/earnings/workdays/2026-07-01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mvc.perform(get("/api/v1/earnings/periods/not-a-context"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/v1/earnings/history?page=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/v1/earnings/history?size=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/v1/earnings/history?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/v1/earnings/history?page=invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/v1/earnings/history?size=invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void earningsPeriodsExposeUnavailableContextsWithoutInventingZeroAmounts() throws Exception {
        saveUnavailableEarning(TODAY);

        mvc.perform(get("/api/v1/earnings/periods/TODAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("TODAY"))
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.amount").value(nullValue()))
                .andExpect(jsonPath("$.currencyCode").value(nullValue()));
    }

    @Test
    void earningsHistoryIsPaginatedByLocalDateDescendingAndSupportsAnEmptyPage() throws Exception {
        saveUnavailableEarning(LocalDate.of(2026, 6, 30));
        for (LocalDate date : java.util.List.of(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 7, 3),
                LocalDate.of(2026, 7, 6))) {
            workdayService.reconcile(date);
        }

        mvc.perform(get("/api/v1/earnings/history?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].localDate").value("2026-07-06"))
                .andExpect(jsonPath("$.items[1].localDate").value("2026-07-03"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(false));
        mvc.perform(get("/api/v1/earnings/history?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].localDate").value("2026-07-02"))
                .andExpect(jsonPath("$.items[1].localDate").value("2026-07-01"))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(true));
        mvc.perform(get("/api/v1/earnings/history?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].localDate").value("2026-07-02"))
                .andExpect(jsonPath("$.items[1].localDate").value("2026-07-01"))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(true));
        mvc.perform(get("/api/v1/earnings/history?page=2&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].localDate").value("2026-06-30"))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(true));
        mvc.perform(get("/api/v1/earnings/history?page=3&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void unavailableEarningsKeepTheirReasonAndDoNotExposeAnAmount() throws Exception {
        LocalDate withoutSalaryProfile = LocalDate.of(2026, 6, 30);
        saveUnavailableEarning(withoutSalaryProfile);

        mvc.perform(get("/api/v1/earnings/workdays/{date}", withoutSalaryProfile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.unavailableReason").value("SALARY_PROFILE_NOT_FOUND"))
                .andExpect(jsonPath("$.amount").doesNotExist());
    }

    private void saveUnavailableEarning(LocalDate withoutSalaryProfile) {
        Workday workday = new Workday(withoutSalaryProfile, ZONE.getId(), ScheduleVariant.NORMAL,
                LocalTime.of(8, 0), LocalTime.of(17, 0), 28_800, Instant.parse("2026-06-30T16:00:00Z"));
        workday.changeStatus(WorkdayStatus.COMPLETED, Instant.parse("2026-06-30T16:00:00Z"));
        workday = workdays.save(workday);
        earnings.save(new com.workworth.earnings.persistence.WorkdayEarning(workday.getId(), withoutSalaryProfile,
                com.workworth.earnings.domain.EarningStatus.UNAVAILABLE,
                com.workworth.earnings.domain.EarningUnavailableReason.SALARY_PROFILE_NOT_FOUND, 28_800, null,
                null, null, null, null, 0, null, null, null, Instant.parse("2026-06-30T16:00:00Z")));
    }

    @Test
    void addingAnAbsenceToAHistoricalWorkdayUsesTheFrozenSalarySnapshotForItsRevision() {
        var workday = workdayService.reconcile(HISTORICAL_WORKDAY);
        var base = materializationService.materialize(workday);
        Instant absenceStart = HISTORICAL_WORKDAY.atTime(9, 0).atZone(ZONE).toInstant();
        Instant absenceEnd = HISTORICAL_WORKDAY.atTime(10, 0).atZone(ZONE).toInstant();

        workdayService.addAbsence(HISTORICAL_WORKDAY, absenceStart, absenceEnd, "appointment");

        var monetaryCorrection = earningCorrections.findAll().get(0);
        BigDecimal expected = base.getHourlyRate()
                .multiply(BigDecimal.valueOf(base.getEconomicSeconds() - 3600L))
                .divide(BigDecimal.valueOf(3600), 12, java.math.RoundingMode.HALF_UP);

        assertThat(monetaryCorrection.getCause()).isEqualTo(EarningCorrectionCause.PARTIAL_ABSENCE_CHANGED);
        assertThat(monetaryCorrection.getPreviousAmount()).isEqualByComparingTo(base.getRawAmount());
        assertThat(monetaryCorrection.getNewAmount()).isEqualByComparingTo(expected);
        assertThat(periodService.summarize(EarningPeriod.MONTH).internalAmount()).isEqualByComparingTo(expected);
    }

    @Test
    void amendingAHistoricalMealBreakThroughTheApiCreatesOneFrozenSnapshotRevision() throws Exception {
        var workday = workdayService.reconcile(TODAY);
        var mealBreak = mealBreaks.save(new com.workworth.workday.persistence.MealBreak(
                workday, TODAY.atTime(11, 0).atZone(ZONE).toInstant()));
        mealBreak.end(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), true);
        var base = materializationService.materialize(workday);

        String request = "{\"startedAt\":\"2026-07-06T08:00:00Z\",\"endedAt\":\"2026-07-06T10:00:00Z\"}";
        mvc.perform(put("/api/v1/workdays/{date}/meal-breaks/{id}", TODAY, mealBreak.getId())
                        .contentType("application/json").content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startedAt").value("2026-07-06T08:00:00Z"))
                .andExpect(jsonPath("$.endedAt").value("2026-07-06T10:00:00Z"))
                .andExpect(jsonPath("$.endedAutomatically").value(false));

        var timeCorrection = workdayCorrections.findAll().get(0);
        var monetaryCorrection = earningCorrections.findAll().get(0);
        BigDecimal expected = base.getHourlyRate().multiply(BigDecimal.valueOf(5))
                .setScale(12, java.math.RoundingMode.HALF_UP);

        assertThat(earnings.count()).isEqualTo(1);
        assertThat(timeCorrection.getCause().name()).isEqualTo("MEAL_BREAK_CHANGED");
        assertThat(timeCorrection.getMealBreak().getId()).isEqualTo(mealBreak.getId());
        assertThat(timeCorrection.getPreviousBreakEndedAutomatically()).isTrue();
        assertThat(monetaryCorrection.getCause()).isEqualTo(EarningCorrectionCause.MEAL_BREAK_CHANGED);
        assertThat(monetaryCorrection.getPreviousAmount()).isEqualByComparingTo(base.getRawAmount());
        assertThat(monetaryCorrection.getNewAmount()).isEqualByComparingTo(expected);
        assertThat(periodService.summarize(EarningPeriod.TODAY).internalAmount()).isEqualByComparingTo(expected);
        assertThat(periodService.summarize(EarningPeriod.WEEK).internalAmount()).isEqualByComparingTo(expected);
        assertThat(periodService.summarize(EarningPeriod.MONTH).internalAmount()).isEqualByComparingTo(expected);
        assertThat(periodService.summarize(EarningPeriod.ALL_TIME).internalAmount()).isEqualByComparingTo(expected);

        mvc.perform(put("/api/v1/workdays/{date}/meal-breaks/{id}", TODAY, mealBreak.getId())
                        .contentType("application/json").content(request))
                .andExpect(status().isOk());
        assertThat(workdayCorrections.count()).isEqualTo(1);
        assertThat(earningCorrections.count()).isEqualTo(1);

        workdayService.cancel(TODAY, "holiday");
        var revisions = earningCorrections.findByEarningIdOrderBySequenceDesc(base.getId());
        assertThat(revisions).hasSize(2);
        assertThat(revisions.get(0).getPreviousCorrection().getId()).isEqualTo(revisions.get(1).getId());

        mvc.perform(get("/api/v1/earnings/workdays/{date}/corrections", TODAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sequence").value(2))
                .andExpect(jsonPath("$[1].sequence").value(1));
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-07-06T16:00:00Z"), ZONE);
        }
    }
}
