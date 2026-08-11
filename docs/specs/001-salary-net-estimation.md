# SPEC 001: Salary Net Estimation

**Status:** Verified
**Related documentation:** [SPEC process](README.md), [Project foundation](000-project-foundation.md), [Business rules](../business-rules.md), [Project requirements](../../PROJECT.md)

## Objective

Define how WorkWorth obtains a valid net-income source, gives priority to a user-declared real monthly net income, and derives the monthly economic rate used by future earnings calculations.

## Context

WorkWorth communicates what work has enabled the user to achieve. Earnings, rewards, goals, and all-time accumulated income therefore use net income only.

The initial user case is Spain, Comunidad Valenciana, indefinite contract, annual gross income of 19,000 EUR, 12 equal payments, single, no children, no disability, and no other income. The initial time zone is `Europe/Madrid`.

This SPEC defines salary source and rate rules. Workday timers, breaks, absences, earnings aggregation, reward evaluation, and UI behavior belong to later specifications.

## Functional requirements

- [ ] The user may provide a real monthly net income in EUR.
- [ ] The MVP supports 12 equal payments only.
- [ ] The real annual net income is derived as `real monthly net income × 12`.
- [ ] A real monthly net income is the absolute source of truth whenever it is present and valid for a month.
- [ ] A fiscal estimate is available only when every required estimation input is present and no real monthly net income is available.
- [ ] The gross annual income is used only as an estimator input and never directly calculates earnings, rewards, or accumulated income.
- [ ] A salary change becomes effective only on the first day of a calendar month.
- [ ] A change creates a new salary basis and preserves the previous basis and calculated-history snapshots.
- [ ] The monthly net hourly rate is calculated from the active monthly net income and the standard economic hours scheduled in that calendar month.
- [ ] Additional time beyond the standard economic workday never increases earnings.

## Business rules

### Salary data categories

| Category | Data | Meaning |
|---|---|---|
| User-entered | `grossAnnual` | Annual gross income; estimator input only. |
| User-entered | `netMonthlyReal` | User-declared real monthly net income. |
| User-entered | `payPeriods` | Always `12` in the MVP. |
| User-entered | `currencyCode` | ISO 4217 code; initial value `EUR`. |
| User-entered | `effectiveFrom` | First day of the month in which a salary basis becomes valid. |
| Derived | `netAnnualReal` | `netMonthlyReal × 12`. |
| Estimated | `estimatedNetAnnual` | Fiscal-estimation result for a defined exercise and rule-set version. |
| Estimated | `estimatedNetMonthly` | `estimatedNetAnnual ÷ 12`. |
| Derived | `hourlyNetRate` | Calendar-month economic rate. |
| Historical | `salaryBasisSnapshot` | Basis, rate, currency, and estimation version used by a historical earning. |

### Source precedence

```text
Valid real monthly net income
        ↓ otherwise
Valid fiscal estimate
        ↓ otherwise
No calculable earnings rate
```

The annual gross income is never an earnings source. It cannot be used directly to calculate earnings, rewards, goals, or `ALL_TIME` accumulated income.

### Real monthly net income

For the MVP, the user declares a monthly net amount for 12 equal payments:

```text
netAnnualReal = netMonthlyReal × 12
```

No variable extra payments, bonuses, commissions, benefits in kind, or special remuneration are modeled in the MVP. The model retains `payPeriods` for future extension, but validates the value `12` now.

### Monthly economic rate

For each calendar month, WorkWorth derives:

```text
hourlyNetRate = activeMonthlyNetIncome / standardEconomicHoursInMonth
```

`standardEconomicHoursInMonth` uses the standard WorkWorth calendar:

- Monday through Thursday: 8 economic hours.
- Friday: 7 economic hours.
- Saturday and Sunday: 0 economic hours.
- Public holidays are handled manually in the MVP and remain standard workdays until cancelled.

Completing every standard economic hour in a calendar month must produce exactly the active monthly net income for that month. Time beyond the standard economic workday never produces additional economic earnings.

### Fiscal estimate fallback

The fiscal estimator is a fallback only. Its results must always be labeled as an **estimate** and must not replace a real monthly net income.

The 2026 Spain estimator is conceptually defined as a versioned rule set that combines:

- The official 2026 AEAT work-income withholding algorithm.
- Applicable 2026 employee Social Security contributions.
- The user inputs required by those rules.

The future implementation must be verified against the official sources for the relevant exercise. This SPEC does not implement fiscal formulas, tables, rates, or code.

Every saved estimate retains its input values, output values, fiscal year, jurisdiction, and rule-set version. Later rule changes do not silently rewrite historical estimates or earnings.

### Salary changes and history

`effectiveFrom` must be the first day of a calendar month. Mid-month changes are out of scope for the MVP.

Changing salary creates a new effective salary basis. Existing earning records retain the `salaryBasisSnapshot` used for their calculation, including the selected source, monthly and annual net amounts, currency, monthly standard hours, hourly rate, and fiscal rule-set version when applicable.

The MVP does not recalculate historical earnings automatically. Any future retroactive correction requires an explicit and auditable feature outside this SPEC.

### Accumulated income

`ALL_TIME` means net income historically registered by WorkWorth since the user started using the application. It starts at zero and is not a bank balance, savings, or money available after expenses.

## Use cases

### Declare a real monthly net income

**Given** a user provides a valid real monthly net income in EUR  
**When** it becomes effective on the first day of a month  
**Then** WorkWorth derives the real annual net income and uses the real source for that month.

### Estimate net income when no real value exists

**Given** no real monthly net income is effective and all required fiscal inputs are present  
**When** the user requests an estimate for a supported fiscal year  
**Then** WorkWorth stores and presents a versioned net-income estimate labeled as an estimate.

### Change a salary basis

**Given** earnings have already been registered using a prior salary basis  
**When** the user adds a new salary basis effective on the first day of a future month  
**Then** future calculations use the new basis and historical calculations retain the prior snapshot.

## Acceptance criteria

- [ ] Data entered by the user, derived values, estimates, and historical snapshots are distinguishable.
- [ ] A valid real monthly net income always takes priority over an available estimate.
- [ ] With 12 payments, real annual net income equals real monthly net income multiplied by 12.
- [ ] Gross annual income cannot directly produce an earnings, reward, goal, or accumulated-income value.
- [ ] An estimate is unavailable when any required fiscal input is missing.
- [ ] Every estimate exposes its fiscal year and rule-set version and is labeled as an estimate.
- [ ] Monthly hourly rate uses the active monthly net income and that month's standard economic hours.
- [ ] Completing all standard economic hours in a month yields exactly its active monthly net income.
- [ ] Additional work time cannot increase earnings beyond the standard economic workday.
- [ ] Salary changes only take effect on the first day of a month and do not silently alter history.
- [ ] Money uses `BigDecimal`, ISO currency codes, and explicit rounding rules.

## Technical considerations

- The future implementation belongs to the salary module of the backend modular monolith.
- API contracts will use DTOs and never expose persistence entities.
- `BigDecimal` is mandatory for all monetary calculations; `double` and `float` are prohibited.
- EUR input values have a maximum scale of two decimals. Intermediate rate calculations use scale 12. Non-terminating divisions and final public monetary amounts use `RoundingMode.HALF_UP`.
- Public and reward-comparison monetary values are rounded to two decimals. Internal calculations retain precision until an earning total is materialized, closed, or queried.
- The fiscal-rule implementation must be immutable by fiscal year and version, with source references and regression tests.
- Future persistence and integration tests require Flyway and PostgreSQL Testcontainers, as defined by SPEC 000.

## Edge cases

- A real monthly net income that is zero, negative, non-numeric, or has more than two decimal places is invalid.
- A currency different from the salary profile currency is invalid.
- A missing real net income and incomplete estimator input leaves the earnings rate unavailable.
- An unsupported fiscal year leaves the estimate unavailable; the user can provide a real monthly net income instead.
- A month with no standard economic hours cannot produce an hourly rate.
- A leap year and local `Europe/Madrid` calendar dates must be handled correctly when determining monthly standard hours.
- A manually un-cancelled public holiday remains a standard workday in the MVP.
- Replacing a real salary basis with an estimate, or the inverse, does not rewrite snapshots already used by historical earnings.

## Expected tests

| Requirement / rule | Test level | Expected test |
|---|---|---|
| Real source precedence | Unit | A valid real monthly net income wins over an available estimate. |
| Annual real-net derivation | Unit | Monthly real net income multiplied by 12 produces the annual real net income. |
| Monthly rate | Unit | Active monthly net income is divided by that month's standard economic hours. |
| Different calendar months | Unit | Months with different weekday distributions produce the expected rates. |
| Complete standard month | Unit | Standard economic hours total exactly the active monthly net income. |
| Standard-workday maximum | Unit | Additional time does not increase earnings. |
| Precision | Unit | `BigDecimal` scale and `HALF_UP` rounding are applied without floating-point conversion. |
| Validation | Unit | Invalid money, currency, pay-period count, and effective date are rejected. |
| Estimation availability | Unit | Missing fiscal input and unsupported rule sets prevent an estimate. |
| Fiscal-rule regression | Unit | Each fiscal version has verified reference scenarios. |
| Historical basis | Integration | A new month-effective basis preserves previous earning snapshots. |
| Persistence | Integration | PostgreSQL/Testcontainers preserves monetary values, currency, effective dates, and fiscal-rule versions. |

## Traceability and verification

Implementation evidence (2026-08-11):

- The `salary` module is implemented as a Spring Boot module with DTO API contracts, service-layer business logic, JPA persistence, Flyway migration, `ProblemDetail` errors, and unit tests.
- The implementation accepts a real monthly net-income source, derives annual real net income for 12 equal payments, preserves month-effective salary profiles, and leaves the fiscal-estimation source unavailable until an approved fiscal rule-set exists.
- `StandardEconomicHoursProvider` is implemented by the workday module in SPEC 002. `MonthlySalaryRateService` now consumes the configured monthly economic hours to expose the monthly rate.
- SPEC 003 verifies the salary → workday → earnings path: earnings retain the salary basis snapshot and use the monthly rate without falling back to gross salary.
- PostgreSQL 16 Testcontainers validates Flyway migrations V1–V7 from clean databases, including salary-profile persistence and the downstream earnings snapshot integration.
- The complete backend suite passed with 29 tests, 0 failures, 0 errors, and 0 skipped tests; `mvn package` and `git diff --check` passed.
- During this verification, the Flyway migration was corrected to align PostgreSQL types with JPA mappings: currency and country codes use `VARCHAR`, while `pay_periods` and `fiscal_year` use `INTEGER`.

This SPEC is **Verified**. The fiscal algorithm remains intentionally unimplemented and unavailable, as approved for the MVP; real monthly net income remains the verified source of truth. No functional salary rules are changed by this verification update.
