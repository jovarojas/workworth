# SPEC 007: Statistics

**Status:** Verified
**Owner:** WorkWorth
**Related documentation:** [SPEC process](README.md), [Project foundation](000-project-foundation.md), [Earnings and earning periods](003-earnings-and-earning-periods.md), [Reward affordability](004-reward-affordability.md), [Motivational dashboard](005-dashboard.md), [Personal goals](006-goals.md), [Development roadmap](../../TASKS.md)

## Objective

Provide a historical and analytical view of the user's WorkWorth activity: effective economic hours, effective net earnings, average effective hourly earnings, and Goals completed over time.

Statistics answers “how has it gone?” across historical periods. It is distinct from the Dashboard, which presents the current workday, current Earnings contexts, and current reward motivation. Statistics does not represent a bank balance, savings, disposable money, affordability, or a forecast.

## Context

SPEC 003 owns materialized Earnings, effective corrections, effective economic time, and local-date aggregation. SPEC 006 owns Goal lifecycle and the terminal `COMPLETED` status with `closedAt`. SPEC 004 owns Rewards, affordability, and combinations; they are not Statistics inputs in this MVP.

The backend remains the sole source of truth. Angular requests already aggregated points and presents their values and availability; it never groups records, constructs periods, sums amounts or time, calculates averages, applies corrections, or derives Goal completion.

## Scope

### In scope

- Historical aggregated series with daily, weekly, monthly, or annual granularity.
- Exactly four MVP metrics per returned point: worked hours, average effective hourly earnings, total effective earnings, and completed Goal count.
- Independent availability for each metric.
- Effective Earnings and effective economic time after the latest valid correction.
- Goal completion count from terminal Goal state and `closedAt`.
- Explicit empty, inactive, unavailable, loading, and error presentation states.
- A bounded, backend-aggregated API that can query the complete recorded history at an appropriate granularity.

### Out of scope

- Rewards, affordability, relevance, combinations, or reward lifecycle metrics.
- Current Dashboard motivation, Dashboard composition, or real-time workday presentation.
- Goal progress history, Goal snapshots, Goal events, completion rates, or modifications to Goals.
- Predictions, forecasts, time-to-goal estimates, recommendations, AI, gamification, achievements, or rankings.
- Currency conversion, exchange rates, currencies beyond global `EUR`/`USD`, or a Statistics-local currency.
- Charts beyond rendering the backend-provided MVP series, raw-record pagination, data export, categories, advanced filters, mobile-native features, or new persistence snapshots.

## Functional requirements

- [x] Statistics returns only backend-aggregated points at one requested granularity: `DAY`, `WEEK`, `MONTH`, or `YEAR`.
- [x] Each point exposes effective worked hours, average effective hourly earnings, total effective earnings, and completed Goal count with independent availability where relevant.
- [x] Worked hours are derived from registered effective economic time, never from attendance/presence time.
- [x] Total earnings use only effective registered Earnings, including the latest valid correction from SPEC 003.
- [x] Average effective hourly earnings is computed by the backend from effective earnings and effective economic hours.
- [x] A Goal contributes exactly once when `status = COMPLETED` and its `closedAt` falls in the point's local-date range.
- [x] `ACTIVE` and `CANCELLED` Goals never contribute to completed Goal count.
- [x] An unavailable monetary source produces `UNAVAILABLE` for the affected monetary metric; the backend never returns a partial monetary total as complete.
- [x] A valid zero is `AVAILABLE`, not `UNAVAILABLE`.
- [x] Angular renders only returned points and values, without local monetary, temporal, average, correction, or Goal calculations.

## Business rules

### Granularity and local periods

All statistic periods use the configured WorkWorth time zone, currently `Europe/Madrid`, and local dates. A point has `startDate` and an exclusive `endDate`:

| Granularity | Point boundary |
|---|---|
| `DAY` | One local calendar day. |
| `WEEK` | ISO week, Monday through the following Monday. |
| `MONTH` | One local calendar month. |
| `YEAR` | One local calendar year. |

A query may request an explicit inclusive date interval using `from` and `to`; the backend maps it to all intersecting points of the requested granularity. When both dates are omitted, it covers all recorded Statistics history through the current local date. Supplying only one bound is invalid.

The backend returns at most **366 aggregated points**. If a requested granularity and interval would exceed this bound, it rejects the request with validation `ProblemDetail`; the caller must request a narrower interval or a coarser granularity. This keeps complete recorded history available without returning raw records or requiring raw-data pagination.

### Metric availability

Every monetary metric has one of two availability states:

| State | Meaning |
|---|---|
| `AVAILABLE` | The backend has a complete valid source for that metric. Its value may validly be zero. |
| `UNAVAILABLE` | The backend cannot resolve the metric completely. Monetary value and currency are `null`; no estimate or partial sum is returned as a complete total. |

Availability is evaluated independently per metric. A point can therefore expose available worked hours and completed Goal count while total earnings and average hourly earnings are unavailable.

### Worked hours

`workedHours` is the backend-resolved sum of effective economic seconds for materialized Earnings in the point, converted to public decimal hours by the backend. It does not use Workday attendance or clock-presence duration.

The backend aggregates effective economic seconds before converting to public hours. Public worked hours use scale 2 and `HALF_UP` rounding. A period with no materialized economic time returns `AVAILABLE` with `0.00` hours.

### Total effective earnings

`totalEarnings` is the sum of effective amounts of all materialized Earnings in the point. The latest valid correction supplies each corrected earning’s amount and economic time exactly as in SPEC 003. The sum retains the established internal precision and rounds its final public amount once using the existing monetary convention.

If any materialized earning in the point is `UNAVAILABLE`, `totalEarnings` is `UNAVAILABLE`; the backend does not present only its available subset as the total. A point with no materialized Earnings has `AVAILABLE` total earnings of `0.00` in the current global currency.

### Average effective hourly earnings

`averageHourlyEarnings` is resolved only by the backend:

```text
averageHourlyEarnings = total effective earnings / total effective economic hours
```

It is `UNAVAILABLE` when total earnings are unavailable or when the point has zero effective economic hours. It is never reconstructed by Angular and does not represent a current Salary profile rate.

### Completed Goals

`completedGoals` is the count of Goals whose persisted state is `COMPLETED` and whose `closedAt`, interpreted in the configured time zone, is in the point’s local-date range. `ACTIVE` and `CANCELLED` Goals count as zero. The metric does not inspect current Goal progress, targets, Earnings, Rewards, or historical snapshots.

With no matching completed Goals, it returns `AVAILABLE` with count `0`.

### Currency

All monetary output uses the existing global application currency and effective Earnings currency supplied by backend. The backend rejects inconsistent stored currency data rather than converting it. Angular displays a received currency only when a monetary metric is available; it uses no fallback currency or conversion.

## States and errors

- A successful response with zero points is a valid empty-history state, not an error.
- A returned point with zero activity remains a valid point: worked hours, total earnings, and completed Goals are zero where available; average hourly earnings is unavailable when hours are zero.
- `UNAVAILABLE` is a valid metric state and is distinct from an HTTP failure.
- A malformed granularity, incomplete range, reversed range, or range exceeding 366 points returns `400 VALIDATION_ERROR` with standard `ProblemDetail`.
- An internal service or connection failure is an API error and must not be represented as a zero or unavailable metric.
- Angular handles a Statistics request failure only in the Statistics section; it does not hide or alter Dashboard, Workday, Earnings, Goals, Rewards, Salary, or Preferences data.

## Use cases

### Show a monthly history

**Given** recorded WorkWorth Earnings and completed Goals across several months
**When** the user requests `MONTH` Statistics for that interval
**Then** the backend returns monthly points containing the four backend-resolved metrics and their independent availability.

### Preserve a valid zero

**Given** a day with no materialized Earnings and no completed Goals
**When** the backend returns its daily Statistics point
**Then** worked hours, total earnings, and completed Goals are `AVAILABLE` at zero, while average hourly earnings is `UNAVAILABLE` because no effective economic hours exist.

### Keep monetary incompleteness explicit

**Given** a monthly point with at least one `UNAVAILABLE` materialized earning
**When** Statistics is requested
**Then** its total earnings and average hourly earnings are `UNAVAILABLE`, without a partial monetary total; worked hours and Goal completion remain independently available when their sources are valid.

### Count completed Goals without reconstructing progress

**Given** one Goal is completed and one is cancelled in the same week
**When** weekly Statistics is requested
**Then** that week has `completedGoals = 1`, based only on the completed Goal’s `closedAt`.

## Acceptance criteria

- [x] Statistics is a historical analytical feature, distinct from the current-state Dashboard.
- [x] The API supports `DAY`, `WEEK`, `MONTH`, and `YEAR` backend-aggregated series with local-date semantics.
- [x] Every point supplies exactly the four approved MVP metrics and no Reward metric, forecast, or Goal progress history.
- [x] Worked hours use effective economic time, not attendance time.
- [x] Total earnings and average hourly earnings use effective Earnings and their latest valid corrections.
- [x] Average hourly earnings is backend-calculated and unavailable when effective hours are zero or monetary data is unavailable.
- [x] A completed Goal is counted exactly by `COMPLETED` plus `closedAt`; active and cancelled Goals are excluded.
- [x] Monetary metrics never present a partial available subset as a complete total when relevant data is unavailable.
- [x] Valid zeros are distinguishable from `UNAVAILABLE`.
- [x] The global currency is reused without conversion or local fallback.
- [x] No snapshots, events, raw-record pagination, Reward logic, or mutations to Goals/Earnings/Workday are introduced.
- [x] Angular presents backend-provided points and never aggregates records, computes metrics, constructs periods, or converts money.

## Technical considerations

### Conceptual API contract

`GET /api/v1/statistics?granularity=MONTH&from=2026-01-01&to=2026-12-31`

```json
{
  "granularity": "MONTH",
  "from": "2026-01-01",
  "to": "2026-12-31",
  "points": [
    {
      "startDate": "2026-08-01",
      "endDate": "2026-09-01",
      "workedHours": {
        "status": "AVAILABLE",
        "value": 142.50
      },
      "averageHourlyEarnings": {
        "status": "AVAILABLE",
        "amount": 11.84,
        "currencyCode": "EUR"
      },
      "totalEarnings": {
        "status": "AVAILABLE",
        "amount": 1687.20,
        "currencyCode": "EUR"
      },
      "completedGoals": {
        "status": "AVAILABLE",
        "count": 1
      }
    }
  ]
}
```

For an unavailable monetary metric, `amount` and `currencyCode` are `null`. For unavailable worked hours, `value` is `null`; for unavailable Goal completion, `count` is `null`. The current MVP expects worked hours and completed Goals to remain available when their respective source records are valid, but the explicit shape preserves independent metric availability.

The implementation belongs to a backend `statistics` module and must consume public Earnings and Goals application boundaries. It must not query Salary, Workday, Earnings, Goals, or Rewards repositories to reproduce foreign rules. A technical proposal must define the exact query strategy, DTO names, indexes, range validation, and Error codes before implementation.

### Responsibilities

| Backend | Angular |
|---|---|
| Validate granularity and date range; produce bounded aggregated points | Request one selected granularity/range and render returned points |
| Resolve effective corrections, availability, monetary totals, hours, and averages | Render metric states and backend values without mathematical reconstruction |
| Count completed Goals from terminal lifecycle data | Choose presentation and chart components without grouping raw data |
| Apply currency consistency and standard `ProblemDetail` behavior | Keep loading, empty, unavailable, and request-error states distinct |

## Edge cases

- A correction changes the effective amount and effective economic time of its original workday date, therefore all Statistics points containing that local date reflect the corrected value.
- A zero-effective-time earning with a valid zero amount makes total earnings available but cannot by itself make average hourly earnings available.
- A week crossing a month or year follows ISO Monday-to-Monday boundaries.
- A date around a `Europe/Madrid` daylight-saving transition remains in its local-date bucket.
- A completed Goal is counted in the bucket containing `closedAt`, not its creation date or its current progress.
- A closed Goal remains counted historically even though its live progress is not evaluated.
- An empty result has no points; a requested point with no activity is represented only when the selected bounded range requires it. The backend does not synthesize unbounded inactive history before the earliest relevant record.
- An inconsistent historical currency is rejected explicitly; it is never converted or silently mixed with another currency.

## Expected tests

| Requirement / rule | Test level | Expected test |
|---|---|---|
| Granularity | Backend unit / integration | Daily, ISO weekly, monthly, and yearly points use configured-zone local boundaries. |
| Bounds | Backend integration | Complete history and explicit ranges obey the 366-point maximum without raw pagination. |
| Effective hours | Backend unit | Effective corrected economic seconds aggregate before backend hour conversion and rounding. |
| Effective total | Backend unit / integration | Latest correction amount is used; an unavailable earning makes the affected total unavailable. |
| Average | Backend unit | Effective total divided by effective hours is backend-resolved; zero hours is unavailable. |
| Goal completion | Backend unit / integration | Only `COMPLETED` Goals are counted by `closedAt`; cancelled and active Goals are excluded. |
| Metric independence | Backend unit | Unavailable monetary data does not erase independently available hours or Goal count. |
| Currency | Backend unit / integration | EUR/USD use the global currency with no conversion; inconsistency returns `ProblemDetail`. |
| Contract | Backend integration | JSON distinguishes available zero from unavailable null fields. |
| Angular boundary | Angular unit | UI renders point DTOs without local aggregation, averaging, period construction, or money conversion. |

## Traceability and verification

Implementation and verification are complete:

- `1fcba45 feat: add statistics backend` introduced the backend `statistics` module, the bounded HTTP contract, effective-Earnings aggregation, Goal counting, currency consistency, unit tests, and PostgreSQL/Testcontainers contract coverage.
- `e1f27e3 feat: integrate statistics API` introduced the typed Angular contract and HTTP service tests for all granularities, zero values, independent availability, EUR/USD, and `ProblemDetail` propagation.
- `9d12273 feat: add statistics UI` introduced the lazy `/statistics` route, navigation, responsive accessible table, backend-point charts, empty/loading/error states, and component tests.

The focused verification confirmed `UNAVAILABLE` remains distinct from zero, graphics render only backend-provided points, and Angular performs no Statistics calculations, period generation, currency conversion, or source-module queries.
