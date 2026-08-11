# SPEC 003: Earnings and Earning Periods

**Status:** Verified
**Related documentation:** [SPEC process](README.md), [Project foundation](000-project-foundation.md), [Salary net estimation](001-salary-net-estimation.md), [Work schedule and workday](002-work-schedule-and-workday.md), [Business rules](../business-rules.md), [Project requirements](../../PROJECT.md), [Architecture decisions](../../DECISIONS.md)

## Objective

Define how WorkWorth transforms economically computable workday time from SPEC 002 and the applicable monthly net rate from SPEC 001 into registered net earnings, then aggregates those earnings for the Dashboard periods.

This specification follows the principle:

> Salary determines what one hour of work is worth. The workday determines when the user is working. Earnings record the economic value produced by economically computable time.

## Context

SPEC 001 is responsible for the valid salary source, salary-source precedence, monthly net rate, and salary-basis snapshot. SPEC 002 is responsible for the standard calendar, workdays, meal breaks, partial absences, cancellations, and economically computable time. This SPEC combines their outputs without duplicating either module's rules.

WorkWorth communicates what the user's work has enabled them to achieve. All earnings therefore use net income only. `ALL_TIME` means net income effectively registered by WorkWorth since use began; it is not a bank balance, savings, wealth, or money available after expenses.

Rewards, goals, advanced statistics, Dashboard visual implementation, Angular, authentication, multi-user behavior, and the Spanish fiscal estimator are out of scope.

## Functional requirements

- [ ] A workday earning is calculated only from its economically computable time and its applicable monthly net rate.
- [ ] A calculation uses `economicTime × monthlyNetRate`.
- [ ] A calculation never uses annual gross income, gross salary, a bank balance, available money, an inactive estimate, or a fixed annual rate.
- [ ] A valid real monthly net income has the source precedence defined by SPEC 001.
- [ ] An earning based on an estimate retains the estimate identity, fiscal year, and fiscal rule-set version.
- [ ] An active workday may expose a calculated-on-demand economic projection; that projection is not historical and creates no correction record.
- [ ] A base historical earning is materialized when its workday closes, using the definitive economically computable time and salary basis snapshot applicable to that workday's local month.
- [ ] A materialized base earning is immutable.
- [ ] Later eligible workday changes create immutable, ordered correction records; they never delete or overwrite the base earning or a prior correction.
- [ ] The effective earning for a workday is its base earning when no correction exists, or the latest valid correction otherwise.
- [ ] `TODAY`, `WEEK`, `MONTH`, and `ALL_TIME` totals use effective earnings.
- [ ] A workday with no valid monthly rate has earning status `UNAVAILABLE` and no invented monetary amount.
- [ ] Public money amounts are rounded once to two decimal places after internal calculation or aggregation.

## Business rules

### Earning formula and source

For a workday with an available salary rate:

```text
earningRaw = economicTime × monthlyNetRate
```

`economicTime` is supplied by SPEC 002. It already incorporates the applicable standard window, daily maximum, meal breaks, partial absences, and cancellation state. SPEC 003 does not repeat or reinterpret those rules.

`monthlyNetRate` is supplied by SPEC 001 for the calendar month of the workday's local date. It is based on the active monthly net-income source and that month's standard economic hours. SPEC 003 does not derive a rate from gross salary or from a different month.

Time beyond the configured economic maximum never produces additional earnings. A cancelled workday, or a workday with zero economically computable time, has an effective earning of zero when a rate is available.

### Salary basis snapshot

Every available base earning retains an immutable salary-basis snapshot containing at least:

| Snapshot data | Meaning |
|---|---|
| `salaryProfileId` | Salary profile used for the workday month. |
| `source` | `NET_MONTHLY_REAL` or `ESTIMATED_NET`. |
| `netMonthlyAmount` | Applicable monthly net income. |
| `netAnnualAmount` | Applicable annual net income. |
| `payPeriods` | Number of equal pay periods; `12` in the MVP. |
| `currencyCode` | ISO 4217 currency code. |
| `referenceMonth` | Calendar month of the workday's local date. |
| `standardEconomicHoursInMonth` | Standard monthly hours supplied by SPEC 002. |
| `monthlyNetRate` | Internal hourly rate used to calculate the earning. |
| `salaryEstimateId` | Estimate identity when source is `ESTIMATED_NET`. |
| `fiscalYear` and `fiscalRuleSetVersion` | Estimation metadata when applicable. |

Changing a salary profile, introducing a later real net income, or changing fiscal rules never automatically rewrites a historical earning snapshot. In particular, a real monthly net introduced after an earning was materialized from an estimate applies only to new earnings according to SPEC 001; it does not retroactively modify the historical earning.

### Materialization and active projection

An active workday can expose a provisional projection calculated under demand using current economic time, the current `Clock`, and the applicable salary snapshot. A projection is neither an historical earning nor a correction and must not be materialized per second or per minute.

When the workday closes, WorkWorth materializes one immutable base earning. It uses the definitive economic time from SPEC 002 and the salary rate/snapshot from SPEC 001. A closed workday with no valid salary rate materializes an `UNAVAILABLE` earning state without a monetary amount.

### Historical corrections

SPEC 002 permits a completed workday to be cancelled and permits later changes to eligible meal-break or partial-absence intervals. For a meal break, this is specifically an atomic replacement of the two boundaries of a closed break on a `COMPLETED` workday. When one of those changes affects a materialized earning, WorkWorth appends an immutable correction record.

Allowed MVP correction causes are:

- `WORKDAY_CANCELLED`
- `PARTIAL_ABSENCE_CHANGED`
- `MEAL_BREAK_CHANGED`

`MANUAL_CORRECTION` and any generic manual financial correction are outside the MVP.

Each correction retains at least:

| Correction data | Meaning |
|---|---|
| Base-earning reference | The immutable historical earning being corrected. |
| Previous-revision reference | The prior effective revision, if any. |
| Correction sequence | Monotonic version for the workday earning. |
| Previous economic time | Economic time before this correction. |
| New economic time | Economic time after this correction. |
| Previous amount | Internal amount before this correction, when available. |
| New amount | Internal amount after this correction, when available. |
| Cause | One approved correction cause. |
| Corrected at | Instant at which the correction was registered. |

The latest valid correction determines the effective earning. Historical totals and Dashboard period totals use that effective value while preserving every earlier value and correction in the audit trail.

### Monetary precision and rounding

- Money uses `BigDecimal` exclusively; `double` and `float` are prohibited.
- The internal monthly hourly rate uses scale 12, as established by SPEC 001.
- Internal earning amounts use scale 12.
- Economic time must preserve enough precision to avoid per-minute or per-segment rounding loss.
- A workday earning is not rounded for every minute, interval, or timer update.
- Public individual amounts and period totals use scale 2 and `RoundingMode.HALF_UP`.
- A period sum adds effective internal earning amounts first and rounds only its final public amount once.

This preserves precision and avoids accumulated rounding errors while remaining compatible with the SPEC 001 rule that completing all standard economic hours in a month yields exactly the applicable monthly net income before public presentation rounding.

### Earning availability

If no valid salary source or monthly rate exists for the workday month, WorkWorth does not create a substitute amount. It does not use annual gross income, a salary from another month, an inactive estimate, or any bank-account concept.

The workday and its economically computable time may still exist. Its earning has status `UNAVAILABLE` and exposes an explicit cause where applicable, such as no valid salary source or unavailable monthly rate.

### Earning periods

Period limits are calculated in the user-configured time zone, initially `Europe/Madrid`. A period includes workdays by their local workday date, not by creation time or correction time.

| Context | Inclusive start | Exclusive end | Meaning |
|---|---|---|---|
| `TODAY` | Start of the current local date | Start of the next local date | Effective earnings for today's local workday date. |
| `WEEK` | Start of current ISO-8601 Monday | Start of following Monday | Effective earnings for the seven local dates in the current ISO week. |
| `MONTH` | Start of current calendar month | Start of following calendar month | Effective earnings for the current calendar month. |
| `ALL_TIME` | Beginning of WorkWorth earning history | No historical upper bound | Every effective net earning registered by WorkWorth. |

`WEEK` uses ISO-8601: Monday is the first day, Sunday is the last day, and the first ISO week has at least four days in the week-based year. A week may cross calendar months or years and is never split for earning aggregation. `MONTH` is always the calendar month, even when it contains only part of an ISO week.

`ALL_TIME` starts at zero and sums all effective historical net earnings. It does not represent a balance, savings, assets, expenses, bank-account money, or spending power after expenses.

An eligible historical correction changes the effective earning for the corrected workday date. Consequently, it updates every period that includes that date, including `ALL_TIME`, rather than only the period in which the correction was made.

## Use cases

### Project an active workday earning

**Given** an active workday with an available monthly rate<br>
**When** the user requests the current workday earning<br>
**Then** WorkWorth returns a calculated-on-demand projection without materializing an earning or correction.

### Materialize a completed workday earning

**Given** a workday closes with definitive economically computable time and an available monthly rate<br>
**When** WorkWorth closes the workday<br>
**Then** it materializes one immutable base earning with the applicable salary basis snapshot.

### Materialize an unavailable earning

**Given** a workday closes without a valid salary rate for its month<br>
**When** WorkWorth attempts to materialize its earning<br>
**Then** it records status `UNAVAILABLE` with an explicit cause and no invented monetary amount.

### Correct a cancelled completed workday

**Given** a completed workday has a materialized earning<br>
**When** the user later cancels that workday<br>
**Then** WorkWorth appends a `WORKDAY_CANCELLED` correction whose new economic time and effective earning are zero.

### Correct a late partial absence

**Given** a completed workday has a materialized earning<br>
**When** a valid partial absence is recorded or changed later<br>
**Then** WorkWorth appends a `PARTIAL_ABSENCE_CHANGED` correction preserving previous and new values.

### Correct an amended historical meal break

**Given** SPEC 002 accepts a valid amendment to a closed meal break on a completed workday with a materialized earning<br>
**When** SPEC 002 persists `MEAL_BREAK_CHANGED` and publishes `WorkdayTimeCorrectionRegisteredEvent`<br>
**Then** earnings consumes that workday-only fact and appends one immutable `MEAL_BREAK_CHANGED` monetary correction using the base earning's original salary snapshot.

### Aggregate a week crossing a month

**Given** the current ISO week contains local dates from two calendar months<br>
**When** the user requests `WEEK` earnings<br>
**Then** WorkWorth sums the effective earnings of all seven local dates in that ISO week.

## Acceptance criteria

- [ ] Every available workday earning is based only on economic time from SPEC 002 and the monthly rate/snapshot from SPEC 001.
- [ ] Gross annual income, gross salary, bank balances, available money, and rates from other months cannot produce earnings.
- [ ] An estimated earning retains its estimate identity, fiscal year, and rule-set version.
- [ ] An active workday projection does not create historical earning or correction records.
- [ ] A closed workday materializes one immutable base earning when a valid rate exists.
- [ ] A closed workday without a valid rate has `UNAVAILABLE` status and no invented monetary amount.
- [ ] A correction never removes or overwrites a base earning or prior correction.
- [ ] Effective earnings use the latest valid correction, and period totals use effective earnings.
- [ ] Corrections retain previous/new amount and economic time, cause, correction instant, and references to the base/prior revision.
- [ ] Cancellation, partial-absence change, and meal-break change are the only MVP correction causes.
- [ ] A `MEAL_BREAK_CHANGED` time correction published by SPEC 002 creates exactly one immutable monetary revision using the base earning's frozen hourly-rate snapshot; earnings does not validate or calculate the meal-break interval itself.
- [ ] Normal and summer workday limits affect earnings only through the economic time supplied by SPEC 002.
- [ ] Internal amounts retain scale 12; public individual and aggregated amounts are rounded once to two decimals with `HALF_UP`.
- [ ] `TODAY`, `WEEK`, `MONTH`, and `ALL_TIME` have the stated local-date semantics.
- [ ] `WEEK` is ISO-8601 and can cross month and year boundaries without being split.
- [ ] `ALL_TIME` includes effective historical net earnings only and starts at zero.
- [ ] No rewards, goals, advanced statistics, Dashboard implementation, Angular, authentication, or fiscal-estimator implementation is introduced.

## Technical considerations

- The future implementation belongs to an `earnings` module in the Spring Boot modular monolith and preserves Controller → Service → Repository boundaries.
- Public API contracts use DTOs and never expose persistence entities.
- The module consumes salary and workday abstractions rather than duplicating their business rules.
- `Clock` and the configured zone determine active projections and period boundaries.
- A local workday date is the aggregation key; zone-aware instants retain auditability for materialization and correction events.
- Persistence must preserve immutable base earnings, immutable corrections, salary snapshots, internal monetary precision, and correction ordering.
- Flyway, PostgreSQL, Spring `ProblemDetail`, profiles, environment configuration, and PostgreSQL Testcontainers integration tests follow SPEC 000.
- The technical implementation proposal must define exact entities, constraints, API DTOs, error responses, query strategies, and concurrency behavior without expanding this functional scope.

## Edge cases

- A cancelled workday has zero effective earning without deleting its historical base earning or corrections.
- A workday with zero economic time has zero effective earning when a valid rate is available.
- Meal breaks and partial absences affect earnings only through corrected economic time from SPEC 002.
- A late absence or meal-break change creates a correction rather than rewriting history.
- A workday around a `Europe/Madrid` daylight-saving transition retains its local date and uses SPEC 002 economic-time output.
- A salary change between months uses each workday month's own salary basis snapshot.
- A weekly period may include different salary rates when it crosses a month boundary.
- Months with different standard hours, including July and August, use the monthly rate from SPEC 001.
- A week crossing a calendar year remains one ISO week for aggregation.
- An unavailable fiscal estimate or incomplete salary profile results in `UNAVAILABLE`, not a gross-salary fallback.
- Very small amounts and fractional time retain internal precision until public output is rounded.
- `ALL_TIME` with no effective earnings returns zero in the applicable currency context.
- A later real net income does not automatically correct an already materialized estimated earning.

## Expected tests

| Requirement / rule | Test level | Expected test |
|---|---|---|
| Earning calculation | Unit | Economic time multiplied by monthly rate retains scale 12 without floating-point conversion. |
| Salary-source precedence | Unit | A real-net salary snapshot is used when SPEC 001 selects it; gross salary cannot produce an earning. |
| Estimated snapshot | Unit | Estimated earnings retain estimate ID, fiscal year, and rule-set version. |
| Active projection | Unit | An active projection is calculated without materializing an earning or revision. |
| Base materialization | Unit | A closed workday produces one immutable base earning with salary snapshot. |
| Unavailable rate | Unit | No rate yields `UNAVAILABLE` and no substitute monetary amount. |
| Workday inputs | Unit | Pause, absence, cancellation, normal, and summer schedules affect earnings only through supplied economic time. |
| Daily limits | Unit | Extra time cannot increase an earning beyond SPEC 002 economic time. |
| Corrections | Unit | A correction preserves old/new time and amount and points to the base/prior revision. |
| Correction causes | Unit | Only the three approved MVP correction causes are accepted. |
| Meal-break correction integration | Integration | A valid historical meal-break amendment enters through SPEC 002, produces one `MEAL_BREAK_CHANGED` time correction, and produces one earnings revision from the original salary snapshot. |
| Effective earning | Unit | The latest valid correction is used instead of base or older correction. |
| Precision | Unit | Period totals sum internal values first and round once with `HALF_UP`. |
| Today | Unit | Local-day boundaries and effective-earning inclusion are correct. |
| Week | Unit | ISO Monday–Sunday bounds work across month and year boundaries. |
| Month | Unit | Calendar-month boundaries are correct through month and year changes. |
| All time | Unit | All effective historical net earnings are summed and an empty history returns zero. |
| Salary changes | Unit | Historical salary snapshots remain unchanged after future salary changes or later real-net entry. |
| Daylight saving | Unit | `Europe/Madrid` transitions preserve local-date period semantics. |
| Persistence | Integration | PostgreSQL/Testcontainers preserves base earnings, corrections, snapshots, scale, ordering, and effective aggregation. |
| API contract | Integration | DTO responses and `ProblemDetail` errors represent unavailable rates and invalid correction requests. |

## Traceability and verification

Verification evidence (2026-08-11):

- A completed workday materializes exactly one immutable base earning through a workday-owned completion event. Active projections remain ephemeral and do not persist earnings or corrections.
- Available earnings preserve the salary snapshot and calculate internal amounts at scale 12. `UNAVAILABLE` earnings preserve a stable unavailable reason and never fall back to gross salary or another month’s rate.
- Immutable, ordered corrections retain previous/new amounts and economic seconds, a predecessor reference, and the original snapshot hourly rate. `WORKDAY_CANCELLED`, `PARTIAL_ABSENCE_CHANGED`, and `MEAL_BREAK_CHANGED` are exercised through the real workday event flow.
- PostgreSQL 16 Testcontainers validates Flyway migrations V1–V7 from clean databases, persistence of the snapshots and corrections, and the REST API’s DTO and `ProblemDetail` contracts.
- The complete backend suite passed with 29 tests, 0 failures, 0 errors, and 0 skipped tests. `mvn package` and `git diff --check` passed.

This SPEC is **Verified**. No Angular, rewards, goals, statistics, Dashboard, authentication, or fiscal-estimator functionality was introduced.
