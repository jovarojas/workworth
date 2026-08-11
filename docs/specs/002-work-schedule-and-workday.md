# SPEC 002: Work Schedule and Workday

**Status:** Verified
**Related documentation:** [SPEC process](README.md), [Project foundation](000-project-foundation.md), [Salary net estimation](001-salary-net-estimation.md), [Business rules](../business-rules.md)

## Objective

Define WorkWorth's standard economic calendar, automatic workday lifecycle, and the historical recording of meal breaks and absences that determine when work time is economically computable.

This specification follows the principle:

> Salary determines what one hour of work is worth. The workday determines when the user is working.

## Context

WorkWorth uses the user-configured time zone, initially `Europe/Madrid`. SPEC 001 defines the active monthly net income and the monetary value of an economic hour. This SPEC defines only the standard calendar and economically computable time; it does not calculate monetary amounts.

The standard economic calendar has a normal period and a summer period. The summer schedule is a WorkWorth calendar configuration, not a representation of contractual hours, collective agreements, overtime, legal compensation, holidays, or vacation rules.

Earnings periods, monetary corrections, rewards, goals, statistics, and Dashboard behavior belong to later specifications, especially SPEC 003.

## Functional requirements

- [ ] During the normal period, Monday through Thursday has a scheduled window of 08:00–17:00 and a maximum of 8 economic hours per day.
- [ ] During the normal period, Friday has a scheduled window of 08:00–15:00 and a maximum of 7 economic hours per day.
- [ ] During July and August, Monday through Friday has a scheduled window of 08:00–15:00 and a maximum of 7 economic hours per day.
- [ ] Saturday and Sunday have no standard workday and no economic hours.
- [ ] The normal-period reference schedule totals 39 economic hours per week.
- [ ] The July/August reference schedule totals 35 economic hours per week.
- [ ] Standard workdays are created automatically and become active according to the local schedule.
- [ ] Standard workdays end automatically at their scheduled end time.
- [ ] The user can start and end a manual, recorded meal break during an active workday.
- [ ] An open meal break is closed automatically at the scheduled workday end time, and the closure is identified as automatic in the historical record.
- [ ] The user can record partial absences as intervals with a start, end, and optional reason.
- [ ] A user can record a full-day absence through the “Today I do not work” action.
- [ ] A manually managed public holiday is represented by cancelling that date's workday in the MVP.
- [ ] A cancelled workday remains in history with an appropriate state and has no economically computable time.
- [ ] Meal breaks and partial absences exclude their intervals from economically computable time.
- [ ] Overlapping meal-break and partial-absence intervals are rejected by validation; they are never implicitly merged or resolved.
- [ ] A cancellation or a partial absence recorded after a workday has started corrects that workday's economically computable time and preserves auditability for a future earnings correction.
- [ ] Time beyond the standard daily maximum never creates additional economically computable time.
- [ ] The workday module implements the `StandardEconomicHoursProvider` contract from SPEC 001.
- [ ] If automatic creation was missed, a standard workday can be deterministically reconciled when the user queries or operates on its local date.

## Business rules

### Standard economic calendar

| Local period | Weekday | Scheduled window | Maximum economic time |
|---|---|---:|---:|
| Normal period (January–June, September–December) | Monday–Thursday | 08:00–17:00 | 8 h |
| Normal period (January–June, September–December) | Friday | 08:00–15:00 | 7 h |
| Summer period (July–August) | Monday–Friday | 08:00–15:00 | 7 h |
| Any period | Saturday–Sunday | No workday | 0 h |

The normal-period reference schedule is 39 economic hours per week. Its Monday–Thursday attendance window has nine clock hours, with a flexible meal break that means the daily maximum remains eight economic hours. The summer-period reference schedule is 35 economic hours per week.

No contractual 40-hour schedule, overtime, manual clock-in/out adjustments, annual contractual-hour target, holiday calendar integration, or vacation rule is modeled by this SPEC.

### Economically computable time

Economically computable time is limited by the standard scheduled window and by the daily maximum for the date. It excludes all recorded meal-break and partial-absence intervals and is zero for a cancelled workday.

Time worked before the scheduled start, after the scheduled end, or after reaching the daily maximum does not create additional economically computable time. Small timing differences never automatically add or subtract economic time outside these rules.

This SPEC determines time only. It does not convert time to euros, maintain earning totals, calculate Today/Week/Month/`ALL_TIME` contexts, or evaluate rewards.

### Workday lifecycle

| State | Meaning |
|---|---|
| `SCHEDULED` | A standard local workday exists but its scheduled start has not arrived. |
| `ACTIVE` | The workday is within its scheduled window and has no open meal break. |
| `ON_MEAL_BREAK` | A meal break is open; economic time does not advance. |
| `COMPLETED` | The scheduled end has passed and the workday is no longer active. |
| `CANCELLED` | The full day was marked as not worked; it remains historical and has zero economic time. |

The lifecycle uses injected `Clock` and the user's configured time zone. A standard workday is identified uniquely by its local date. If system unavailability prevented its scheduled materialization, a query or operation for that date reconciles it deterministically and idempotently: the same local date can produce only one standard workday.

### Meal breaks

A meal break is manually started and ended by the user during a standard workday. It has no fixed start time, no minimum duration, and excludes time from economic computation while open.

If a meal break remains open at the scheduled end, WorkWorth closes it at that scheduled end and records that its end was automatic. This preserves historical traceability while ensuring that the completed workday has no open break.

### Partial absences

A partial absence has a start, end, and optional reason. Its interval excludes economically computable time. Reasons are historical context only and do not affect the economic calculation in the MVP.

Meal breaks and partial absences cannot overlap one another. An invalid overlapping interval is rejected rather than merged, shortened, or otherwise modified implicitly. Intervals must be valid and within the scheduled window of the relevant workday.

### Full-day absence and manual holidays

“Today I do not work” cancels the standard workday for its local date. The cancellation may be used for a full-day absence or a manually managed public holiday. The record is retained as `CANCELLED`; it is never destructively deleted.

Cancelling a workday after it was active or completed corrects its economically computable time to zero. Existing breaks and partial absences remain historical records. Recording or changing a partial absence after its interval has elapsed similarly corrects the affected economic time. These corrections must be traceable so that the future earnings module can reverse or adjust the corresponding monetary records without erasing history.

### Standard monthly economic hours

The workday module implements the `StandardEconomicHoursProvider` contract defined by SPEC 001. For a requested calendar month, it returns the sum of the standard maximum economic hours for every weekday in that month using the normal or summer calendar above.

The provider does not subtract cancelled workdays, manual holidays, meal breaks, or partial absences. It represents the configured standard monthly calendar used by SPEC 001 to derive the monthly hourly rate. Actual workday intervals determine economically computable time separately.

Therefore, in accordance with SPEC 001:

```text
hourlyNetRate = activeMonthlyNetIncome / standardEconomicHoursInMonth
```

SPEC 002 supplies `standardEconomicHoursInMonth`; it does not perform this monetary division.

## Use cases

### Automatically activate a normal-period workday

**Given** a non-cancelled Monday in October in `Europe/Madrid`<br>
**When** the scheduled time reaches 08:00<br>
**Then** the workday is active, has a scheduled end of 17:00, and cannot exceed 8 economic hours.

### Automatically activate a summer workday

**Given** a non-cancelled Wednesday in July in `Europe/Madrid`<br>
**When** the scheduled time reaches 08:00<br>
**Then** the workday is active, has a scheduled end of 15:00, and cannot exceed 7 economic hours.

### Record a flexible meal break

**Given** an active Tuesday workday<br>
**When** the user starts a meal break at 14:10 and ends it at 14:55<br>
**Then** that interval is retained in history and excluded from economically computable time.

### Automatically close an open meal break

**Given** a normal-period Thursday with a meal break still open at 17:00<br>
**When** the scheduled end is reached<br>
**Then** the meal break ends at 17:00, its automatic closure is recorded, and the workday becomes completed.

### Record a partial absence after it occurred

**Given** a workday already started<br>
**When** the user records an absence from 10:00 to 12:00 with an optional medical reason<br>
**Then** the interval is excluded from economic time and the corrected result is traceable for future earnings adjustment.

### Mark a day as not worked

**Given** an automatically created workday<br>
**When** the user selects “Today I do not work”<br>
**Then** the workday becomes `CANCELLED`, stays in history, and has zero economically computable time.

### Reconcile a missed automatic workday

**Given** system unavailability meant a standard Friday workday was not materialized<br>
**When** the user queries or operates on that local Friday<br>
**Then** exactly one deterministic standard workday is created or returned, without duplication.

## Acceptance criteria

- [ ] The normal and summer schedules are distinctly represented, including their respective 39-hour and 35-hour weekly references.
- [ ] Standard monthly hours count 8 hours for normal-period Monday–Thursday, 7 hours for normal-period Friday, and 7 hours for every summer-period weekday Monday–Friday.
- [ ] The implementation of `StandardEconomicHoursProvider` belongs to the workday module and is usable by the salary module without duplicating calendar rules.
- [ ] Standard workdays use the configured local time zone and transition automatically through their lifecycle.
- [ ] No normal-period Monday–Thursday workday exceeds 8 economic hours, and no Friday or summer weekday exceeds 7.
- [ ] Meal breaks and partial absences are persisted historical intervals that exclude their time from economic computation.
- [ ] A meal break open at scheduled end is automatically closed at that end and records the automatic closure.
- [ ] Overlapping meal breaks and partial absences are rejected with validation and are not changed implicitly.
- [ ] A full-day cancellation stays historical and sets economic time to zero.
- [ ] Late cancellations and absences provide a traceable correction of economically computable time.
- [ ] A missed automatic workday can be reconciled idempotently with one workday per local date.
- [ ] No functionality for monetary earnings, earnings periods, rewards, goals, statistics, or Dashboard is implemented or defined beyond the stated integration boundary.

## Technical considerations

- The backend module is `workday` within the Spring Boot modular monolith and preserves Controller → Service → Repository boundaries.
- Public APIs use DTOs; persistence entities are never exposed.
- The eventual implementation uses Flyway, PostgreSQL, `ProblemDetail`, profiles, and environment configuration according to SPEC 000.
- Time-dependent business logic receives `java.time.Clock`; business dates are interpreted with the configured user time zone.
- A local date identifies a workday. Historical intervals retain zone-aware timestamps and the zone used for the workday so later profile-zone changes do not reinterpret past history.
- The technical proposal will define the persistence model, API, reconciliation mechanism, corrections contract, and exact validation errors. It must not introduce monetary calculations that belong to SPEC 003.
- PostgreSQL/Testcontainers integration tests are required when the environment supports Docker, as established by SPEC 000.

## Edge cases

- July or August dates use the summer schedule even when the same weekday uses a different normal-period schedule.
- A month with different weekday distributions, including February in leap years, returns its correct standard monthly economic hours.
- `Europe/Madrid` daylight-saving transitions do not change the configured local schedule or its economic-hour maximum.
- Saturday and Sunday neither receive a standard workday nor standard monthly economic hours.
- Attempting to add a break or absence to a cancelled workday, outside its scheduled window, with an end before or equal to its start, or overlapping another excluded interval is invalid.
- A cancellation after a workday was active or completed retains the previous record and produces zero economic time going forward.
- A manually uncancelled public holiday remains a normal standard workday in the MVP.
- Reconciliation after system unavailability cannot create duplicate workdays for the same local date.
- A future user-zone change does not alter the historical zone or intervals associated with existing workdays.

## Expected tests

| Requirement / rule | Test level | Expected test |
|---|---|---|
| Normal monthly calendar | Unit | The provider returns 8 h for normal Monday–Thursday dates and 7 h for normal Fridays. |
| Summer monthly calendar | Unit | The provider returns 7 h for each Monday–Friday date in July and August. |
| Monthly variation | Unit | Different month layouts and leap-year February return the expected total standard hours. |
| Automatic lifecycle | Unit | A fixed `Clock` yields the correct scheduled, active, break, completed, and cancelled behavior. |
| Daily maximum | Unit | Normal Monday–Thursday never exceeds 8 h; normal Fridays and summer weekdays never exceed 7 h. |
| Meal break | Unit | A recorded break excludes its interval from economic time. |
| Automatic break closure | Unit | An open break closes at scheduled end and is marked automatic. |
| Partial absence | Unit | A valid interval excludes time and retains its optional reason. |
| Interval validation | Unit | Overlapping, invalid, out-of-window, and cancelled-day intervals are rejected. |
| Cancellation | Unit | Cancellation keeps the workday record and reduces economic time to zero. |
| Late correction | Unit | A late absence or cancellation exposes a traceable corrected economic-time result. |
| Reconciliation | Unit / Integration | Repeated reconciliation of one local date creates or returns exactly one workday. |
| Persistence | Integration | PostgreSQL/Testcontainers preserves workdays, breaks, absences, states, timestamps, and constraints. |
| API contract | Integration | DTO responses, validation, and `ProblemDetail` errors are returned correctly. |
| Salary integration | Integration | The salary module consumes the workday `StandardEconomicHoursProvider` without calendar-rule duplication. |

## Traceability and verification

Implementation evidence (2026-08-11):

- The `workday` module implements the normal and July/August economic calendars, workday persistence, Flyway migration `V2__create_workday_tracking.sql`, lifecycle reconciliation, meal breaks, partial absences, cancellation, and immutable economic-time correction records.
- `StandardEconomicHoursService` implements the salary module's `StandardEconomicHoursProvider` contract without calculating money.
- The implementation uses injected `Clock`, `Europe/Madrid` configuration, zone-aware instants, idempotent local-date reconciliation, and automatic closure of open meal breaks at scheduled end.
- PostgreSQL 16 Testcontainers validates Flyway V1/V2 from a clean database and the workday persistence model. The full backend suite passed with 11 tests, 0 failures, 0 errors, and 0 skipped tests; `mvn package` also passed.

Verification evidence (2026-08-11):

- `WorkdayServiceTest` verifies idempotent reconciliation, scheduled/active/completed/cancelled lifecycle paths, automatic meal-break closure, absence modification, and rejection of meal-break/meal-break, absence/absence, and meal-break/absence overlaps.
- `WorkdayScheduleTest` and `StandardEconomicHoursServiceTest` verify normal, July/August, weekend, leap-year, and varying monthly-calendar behavior without duplicating salary rules.
- `WorkdayRepositoryIntegrationTest` verifies PostgreSQL/Testcontainers persistence and `WorkdayControllerIntegrationTest` verifies an HTTP `ProblemDetail` response; the complete backend suite applies Flyway V1/V2 from clean PostgreSQL containers.
- The full backend suite passed with 15 tests, 0 failures, 0 errors, and 0 skipped tests. `mvn package` and `git diff --check` passed.

This SPEC is **Verified**. No earnings, monetary aggregation, rewards, goals, statistics, Dashboard, Angular, or SPEC 003 functionality was added.
