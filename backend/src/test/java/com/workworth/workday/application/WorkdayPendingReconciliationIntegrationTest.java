package com.workworth.workday.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.workworth.WorkWorthApplication;
import com.workworth.earnings.application.EarningPeriodService;
import com.workworth.earnings.domain.EarningPeriod;
import com.workworth.earnings.domain.EarningStatus;
import com.workworth.earnings.domain.EarningUnavailableReason;
import com.workworth.earnings.persistence.EarningCorrectionRepository;
import com.workworth.earnings.persistence.WorkdayEarning;
import com.workworth.earnings.persistence.WorkdayEarningRepository;
import com.workworth.identity.persistence.AppUser;
import com.workworth.identity.persistence.AppUserRepository;
import com.workworth.preferences.application.ApplicationCurrencyProvider;
import com.workworth.salary.persistence.SalaryProfile;
import com.workworth.salary.persistence.SalaryProfileRepository;
import com.workworth.workday.persistence.Workday;
import com.workworth.workday.persistence.WorkdayRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Reproduces BUG 1: a user who does not open WorkWorth on a working day must still get that
 * day's automatic workday (and its earning) once she comes back, without duplicating anything
 * and without creating workdays for weekends. This includes a user who signs up and does not
 * open the app until several days later: her back-fill starts at {@code AppUser#createdAt}
 * (never earlier), not just at her last known workday, so those days are not lost either.
 *
 * Backfill is computed as "every laborable date in [createdAt, today) minus whatever already
 * has a Workday" -- a real set difference, not a single MAX(local_date) pointer. This matters
 * because a later Workday already existing (created by the scheduler, a retry, or anything
 * else) must never hide an earlier interior gap underneath it; see the *InteriorGap* tests.
 *
 * Materializing one date's Earning is its own reconciliation unit: a failure there (e.g. no
 * salary profile covering that date) must not roll back any other date's already-persisted
 * Workday or Earning in the same reconcileThroughToday() call; see
 * earningMaterializationFailureForSomeDatesDoesNotRollBackWorkdaysOrEarningsOfOtherDates.
 *
 * "Today" is pinned to a fixed Monday so the scenario (a two-business-day gap spanning one
 * weekend) is deterministic regardless of when the suite actually runs.
 */
@SpringBootTest(classes = {WorkWorthApplication.class, WorkdayPendingReconciliationIntegrationTest.FixedClockConfiguration.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class WorkdayPendingReconciliationIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2028, 3, 13); // Monday
    private static final LocalDate LAST_KNOWN = LocalDate.of(2028, 3, 8); // Wednesday, previous week
    private static final LocalDate MISSED_THURSDAY = LocalDate.of(2028, 3, 9);
    private static final LocalDate MISSED_FRIDAY = LocalDate.of(2028, 3, 10);
    private static final LocalDate WEEKEND_SATURDAY = LocalDate.of(2028, 3, 11);
    private static final LocalDate WEEKEND_SUNDAY = LocalDate.of(2028, 3, 12);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WorkdayService workdays;

    @Autowired
    private WorkdayRepository repository;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private WorkdayEarningRepository earningsRepository;

    @Autowired
    private EarningCorrectionRepository earningCorrections;

    @Autowired
    private SalaryProfileRepository salaryProfiles;

    @Autowired
    private ApplicationCurrencyProvider applicationCurrency;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM workday_earnings");
        jdbcTemplate.update("DELETE FROM workdays");
        jdbcTemplate.update("DELETE FROM salary_profiles");
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private AppUser newUser(String label, Instant createdAt) {
        return users.save(new AppUser(UUID.randomUUID(), "test|" + label + "-" + UUID.randomUUID(),
            label + "-" + UUID.randomUUID() + "@test.invalid", "Europe/Madrid", createdAt));
    }

    // Convenience for tests where the exact sign-up instant does not drive the assertions
    // (e.g. a Workday already exists, so the backfill anchors on it instead of on createdAt).
    // Pinned to TODAY so it can never accidentally read as "created several days ago".
    private AppUser newUser(String label) {
        return newUser(label, TODAY.atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant());
    }

    private int workdayCount(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM workdays WHERE user_id = ?", Integer.class, userId);
        return count == null ? 0 : count;
    }

    private int earningCount(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM workday_earnings we JOIN workdays w ON w.id = we.workday_id WHERE w.user_id = ?",
            Integer.class, userId);
        return count == null ? 0 : count;
    }

    // Most tests give every user a salary profile so their earnings materialize as AVAILABLE and
    // the assertions can check real amounts; earningMaterializationFailureForSomeDatesDoesNotRollBackWorkdaysOrEarningsOfOtherDates
    // is the one that deliberately leaves part of the range uncovered.
    // effective_from must be the first day of a month (DB check constraint); February covers LAST_KNOWN
    // (March 2028) and everything backfilled after it.
    private void givenASalaryProfile(AppUser user) {
        salaryProfiles.save(new SalaryProfile(user, LocalDate.of(2028, 2, 1), null,
            new BigDecimal("1400.00"), "EUR", 12, Instant.now()));
    }

    @Test
    void newUserCreatedTodayWithoutAnyWorkdayGetsOnlyTodayReconciled() {
        // createdAt == TODAY, so there is no prior day to back-fill from account creation either.
        AppUser user = newUser("new-user");

        Workday today = workdays.reconcileThroughToday(user);

        assertThat(today.getLocalDate()).isEqualTo(TODAY);
        assertThat(workdayCount(user.getId())).isEqualTo(1);
    }

    @Test
    void userCreatedSeveralDaysAgoWhoNeverOpenedTheAppBackfillsFromAccountCreationNotJustFromToday() {
        // Signs up on LAST_KNOWN (a Wednesday) but never opens WorkWorth until TODAY: she has no
        // Workday at all yet, so the old "no prior workday -> nothing to back-fill" behavior would
        // have silently lost LAST_KNOWN, MISSED_THURSDAY and MISSED_FRIDAY. The anchor must now be
        // her sign-up date, not just her last known workday. This also carries the fix through to
        // money: each recovered workday must materialize its Earning, and the accumulated total
        // read through the same EarningPeriodService the Dashboard uses must include them, proving
        // the full chain "days without opening the app -> Workday backfill -> Earnings -> money".
        ZoneId zone = ZoneId.of("Europe/Madrid");
        AppUser user = newUser("late-first-open", LAST_KNOWN.atStartOfDay(zone).toInstant());
        givenASalaryProfile(user);
        // Real EarningPeriodService (the class the Dashboard/API reads from), scoped to this test
        // user instead of the fixed "current user" TestCurrentUserProvider resolves everywhere else.
        EarningPeriodService periodsForUser = new EarningPeriodService(
            earningsRepository, earningCorrections, clock, applicationCurrency, () -> user);

        Workday today = workdays.reconcileThroughToday(user);

        assertThat(today.getLocalDate()).isEqualTo(TODAY);
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), LAST_KNOWN)).isPresent();
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), MISSED_THURSDAY)).isPresent();
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), MISSED_FRIDAY)).isPresent();
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), WEEKEND_SATURDAY)).isEmpty();
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), WEEKEND_SUNDAY)).isEmpty();
        // LAST_KNOWN (creation day itself) + Thursday + Friday + Today = 4 workdays; the weekend
        // never gets one, and nothing earlier than createdAt (LAST_KNOWN) is ever created.
        assertThat(workdayCount(user.getId())).isEqualTo(4);
        LocalDate earliest = jdbcTemplate.queryForObject(
            "SELECT MIN(local_date) FROM workdays WHERE user_id = ?", LocalDate.class, user.getId());
        assertThat(earliest).isEqualTo(LAST_KNOWN);

        // LAST_KNOWN, Thursday, and Friday are already in the past relative to "today" and complete
        // immediately on creation, so each materializes exactly one AVAILABLE earning; today is
        // still within its scheduled window (the fixed clock sits at midday) and has none yet.
        List<WorkdayEarning> earningsForUser = earningsRepository.findAllByWorkdayOwnerId(user.getId());
        assertThat(earningsForUser).hasSize(3);
        assertThat(earningsForUser).allMatch(e -> e.getStatus() == EarningStatus.AVAILABLE);
        assertThat(earningsForUser).extracting(WorkdayEarning::getLocalDate)
            .containsExactlyInAnyOrder(LAST_KNOWN, MISSED_THURSDAY, MISSED_FRIDAY);
        assertThat(earningsForUser).allMatch(e -> e.getRawAmount().signum() > 0);

        // The accumulated ALL_TIME total, read through the real Dashboard-facing service, reflects
        // every recovered workday, not just ones created through a normal same-day open.
        BigDecimal expectedTotal = earningsForUser.stream().map(WorkdayEarning::getRawAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        var allTime = periodsForUser.summarize(EarningPeriod.ALL_TIME);
        assertThat(allTime.status()).isEqualTo(EarningStatus.AVAILABLE);
        assertThat(allTime.publicAmount()).isEqualByComparingTo(expectedTotal);
        assertThat(allTime.publicAmount()).isPositive();

        // Reconciling again from the same state must not create or duplicate anything, in Workdays
        // or in the Earnings/money they produced.
        Workday repeated = workdays.reconcileThroughToday(user);

        assertThat(repeated.getId()).isEqualTo(today.getId());
        assertThat(workdayCount(user.getId())).isEqualTo(4);
        assertThat(earningCount(user.getId())).isEqualTo(3);
        assertThat(periodsForUser.summarize(EarningPeriod.ALL_TIME).publicAmount()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    void backfillsMissedWeekdaysSinceTheLastKnownWorkdaySkippingTheWeekendAndDoesNotDuplicateOnRepeat() {
        AppUser user = newUser("gap-user", LAST_KNOWN.atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant());
        givenASalaryProfile(user);
        workdays.reconcile(user, LAST_KNOWN);

        Workday today = workdays.reconcileThroughToday(user);

        assertThat(today.getLocalDate()).isEqualTo(TODAY);
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), MISSED_THURSDAY)).isPresent();
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), MISSED_FRIDAY)).isPresent();
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), WEEKEND_SATURDAY)).isEmpty();
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), WEEKEND_SUNDAY)).isEmpty();
        // LAST_KNOWN + Thursday + Friday + Today = 4 workdays; the weekend never gets one.
        assertThat(workdayCount(user.getId())).isEqualTo(4);
        // LAST_KNOWN, Thursday, and Friday are already in the past relative to "today" and
        // complete immediately on creation, so each materializes exactly one earning; today is
        // still within its scheduled window (the fixed clock sits at midday) and has none yet.
        int earningsAfterFirstPass = earningCount(user.getId());
        assertThat(earningsAfterFirstPass).isEqualTo(3);

        // Reconciling again from the same state must not create or duplicate anything.
        Workday repeated = workdays.reconcileThroughToday(user);

        assertThat(repeated.getId()).isEqualTo(today.getId());
        assertThat(workdayCount(user.getId())).isEqualTo(4);
        assertThat(earningCount(user.getId())).isEqualTo(earningsAfterFirstPass);
    }

    @Test
    void isolatesBackfillBetweenUsers() {
        AppUser userA = newUser("isolated-a", LAST_KNOWN.atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant());
        AppUser userB = newUser("isolated-b", MISSED_FRIDAY.atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant());
        givenASalaryProfile(userA);
        givenASalaryProfile(userB);
        workdays.reconcile(userA, LAST_KNOWN);
        // userB has a shorter gap, so MISSED_THURSDAY is unique to userA -- that is what tells the
        // two users' backfills apart, not the calendar dates themselves: both still legitimately
        // end up with a workday on the same shared dates (MISSED_FRIDAY and TODAY), same as any two
        // real coworkers on the same schedule.
        workdays.reconcile(userB, MISSED_FRIDAY);

        workdays.reconcileThroughToday(userA);
        workdays.reconcileThroughToday(userB);

        assertThat(workdayCount(userA.getId())).isEqualTo(4);
        assertThat(workdayCount(userB.getId())).isEqualTo(2);
        assertThat(repository.findByUserIdAndLocalDate(userB.getId(), MISSED_THURSDAY)).isEmpty();
        assertThat(repository.findByUserIdAndLocalDate(userB.getId(), MISSED_FRIDAY)).isPresent();
        // No workday row is shared or mis-attributed between the two users: every row in the table
        // belongs to exactly one of them (each user's own count above already excludes the other's).
        Integer totalWorkdays = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM workdays", Integer.class);
        assertThat(totalWorkdays).isEqualTo(workdayCount(userA.getId()) + workdayCount(userB.getId()));
    }

    @Test
    void concurrentBackfillsOfTheSameGapCreateNoDuplicateWorkdaysOrEarnings() throws Exception {
        AppUser user = newUser("concurrent-gap", LAST_KNOWN.atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant());
        givenASalaryProfile(user);
        workdays.reconcile(user, LAST_KNOWN);
        CountDownLatch start = new CountDownLatch(1);

        Future<Workday> first = executor.submit(() -> reconcileAfter(start, user));
        Future<Workday> second = executor.submit(() -> reconcileAfter(start, user));
        start.countDown();

        Workday firstResult = first.get(10, TimeUnit.SECONDS);
        Workday secondResult = second.get(10, TimeUnit.SECONDS);

        assertThat(firstResult.getId()).isEqualTo(secondResult.getId());
        assertThat(workdayCount(user.getId())).isEqualTo(4);
        Integer duplicateDates = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM (SELECT local_date FROM workdays WHERE user_id = ? "
                + "GROUP BY local_date HAVING COUNT(*) > 1) d",
            Integer.class, user.getId());
        assertThat(duplicateDates).isZero();
        Integer duplicateEarnings = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM (SELECT workday_id FROM workday_earnings "
                + "GROUP BY workday_id HAVING COUNT(*) > 1) d",
            Integer.class);
        assertThat(duplicateEarnings).isZero();
    }

    // Direct regression test for the reported incident: miércoles (LAST_KNOWN) and viernes
    // (MISSED_FRIDAY) already have a Workday, jueves (MISSED_THURSDAY) does not. The old
    // MAX(local_date) anchor would see viernes as "last known" and never look back at jueves.
    @Test
    void fillsAnInteriorGapEvenWhenALaterWorkdayAlreadyExists() {
        AppUser user = newUser("interior-gap", LAST_KNOWN.atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant());
        givenASalaryProfile(user);
        workdays.reconcile(user, LAST_KNOWN);
        workdays.reconcile(user, MISSED_FRIDAY);
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), MISSED_THURSDAY)).isEmpty();

        Workday today = workdays.reconcileThroughToday(user);

        assertThat(today.getLocalDate()).isEqualTo(TODAY);
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), MISSED_THURSDAY)).isPresent();
        assertThat(workdayCount(user.getId())).isEqualTo(4);
        assertThat(earningCount(user.getId())).isEqualTo(3);

        // Repeating from this state must not duplicate anything either.
        Workday repeated = workdays.reconcileThroughToday(user);
        assertThat(repeated.getId()).isEqualTo(today.getId());
        assertThat(workdayCount(user.getId())).isEqualTo(4);
        assertThat(earningCount(user.getId())).isEqualTo(3);
    }

    // lunes✓ martes✗ miércoles✓ jueves✗ viernes✓ -- several non-contiguous interior gaps behind
    // an already-existing later Workday, all recovered in a single pass.
    @Test
    void fillsMultipleNonContiguousInteriorGaps() {
        LocalDate monday = LocalDate.of(2028, 3, 6);
        LocalDate tuesday = LocalDate.of(2028, 3, 7);
        ZoneId zone = ZoneId.of("Europe/Madrid");
        AppUser user = newUser("multi-gap", monday.atStartOfDay(zone).toInstant());
        givenASalaryProfile(user);
        workdays.reconcile(user, monday);
        workdays.reconcile(user, LAST_KNOWN);
        workdays.reconcile(user, MISSED_FRIDAY);
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), tuesday)).isEmpty();
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), MISSED_THURSDAY)).isEmpty();

        Workday today = workdays.reconcileThroughToday(user);

        assertThat(today.getLocalDate()).isEqualTo(TODAY);
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), tuesday)).isPresent();
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), MISSED_THURSDAY)).isPresent();
        // monday, tuesday, LAST_KNOWN, MISSED_THURSDAY, MISSED_FRIDAY, today = 6 workdays.
        assertThat(workdayCount(user.getId())).isEqualTo(6);
    }

    // A workday's Earning is its own reconciliation unit: signing up in February but only getting
    // a salary profile from March 1st onward (a realistic history -- SPEC 001 profiles apply
    // forward from their effective_from) must not let February's SalaryProfileNotFoundException
    // roll back March's already-reconciled Workdays and Earnings within the same call.
    @Test
    void earningMaterializationFailureForSomeDatesDoesNotRollBackWorkdaysOrEarningsOfOtherDates() {
        LocalDate createdOn = LocalDate.of(2028, 2, 28); // Monday, no salary profile covers it yet
        LocalDate secondUncovered = LocalDate.of(2028, 2, 29); // Tuesday
        LocalDate firstCovered = LocalDate.of(2028, 3, 1); // Wednesday, salary profile starts here
        ZoneId zone = ZoneId.of("Europe/Madrid");
        AppUser user = newUser("earnings-failure-isolation", createdOn.atStartOfDay(zone).toInstant());
        salaryProfiles.save(new SalaryProfile(user, firstCovered, null,
            new BigDecimal("1400.00"), "EUR", 12, Instant.now()));

        Workday today = workdays.reconcileThroughToday(user);

        assertThat(today.getLocalDate()).isEqualTo(TODAY);
        // Every business day since sign-up has its Workday, whether or not its month had salary
        // coverage -- this is exactly what used to disappear, along with every later date in the
        // same call, before Earnings materialization was isolated per date.
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), createdOn)).isPresent();
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), secondUncovered)).isPresent();
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), firstCovered)).isPresent();
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), LAST_KNOWN)).isPresent();
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), MISSED_THURSDAY)).isPresent();
        assertThat(repository.findByUserIdAndLocalDate(user.getId(), MISSED_FRIDAY)).isPresent();

        List<WorkdayEarning> earningsForUser = earningsRepository.findAllByWorkdayOwnerId(user.getId());
        var uncovered = earningsForUser.stream().filter(e -> e.getLocalDate().isBefore(firstCovered)).toList();
        var covered = earningsForUser.stream().filter(e -> !e.getLocalDate().isBefore(firstCovered)).toList();
        assertThat(uncovered).extracting(WorkdayEarning::getLocalDate)
            .containsExactlyInAnyOrder(createdOn, secondUncovered);
        assertThat(uncovered).allMatch(e -> e.getStatus() == EarningStatus.UNAVAILABLE);
        assertThat(uncovered).allMatch(e -> e.getUnavailableReason() == EarningUnavailableReason.SALARY_PROFILE_NOT_FOUND);
        assertThat(covered).isNotEmpty();
        assertThat(covered).allMatch(e -> e.getStatus() == EarningStatus.AVAILABLE);
    }

    private Workday reconcileAfter(CountDownLatch start, AppUser user) throws Exception {
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to reconcile pending workdays.");
        }
        return workdays.reconcileThroughToday(user);
    }

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(TODAY.atTime(12, 0).atZone(ZoneId.of("Europe/Madrid")).toInstant(), ZoneId.of("Europe/Madrid"));
        }
    }
}
