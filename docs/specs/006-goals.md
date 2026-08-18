# SPEC 006: Personal Goals

**Status:** Verified
**Owner:** WorkWorth
**Related documentation:** [SPEC process](README.md), [Project foundation](000-project-foundation.md), [Salary net estimation](001-salary-net-estimation.md), [Earnings and earning periods](003-earnings-and-earning-periods.md), [Reward affordability](004-reward-affordability.md), [Motivational dashboard](005-dashboard.md), [Development roadmap](../../TASKS.md)

## Objective

Allow the user to define a personal monetary objective and see its current progress against the effective net income historically registered by WorkWorth. A Goal makes an intended milestone visible; it does not represent a bank balance, savings, spending capacity, assets, or money external to WorkWorth.

## Context

SPEC 003 owns effective Earnings and the `ALL_TIME` aggregate. SPEC 004 owns independent personal Rewards and their affordability. SPEC 005 may present current Rewards on the Dashboard. This SPEC introduces independent personal Goals; it does not group Rewards, replace their lifecycle, or reuse their affordability rules.

Goals use only effective, registered net Earnings. Salary rates, Workday intervals, earning corrections, and `ALL_TIME` aggregation remain the responsibility of their existing modules. The Spring Boot backend is the only source of truth for Goal lifecycle and progress; Angular presents the resulting public DTOs and initiates approved user actions.

## Scope

### In scope

- Personal Goal CRUD for active Goals, except deletion as defined by this SPEC.
- A strictly positive monetary target in the current application currency.
- Dynamic progress for active Goals against current effective `ALL_TIME` Earnings.
- Manual completion after the backend determines that the target has been reached.
- Manual cancellation of an active Goal.
- A history list consisting of closed `COMPLETED` and `CANCELLED` Goals.
- Empty, unavailable, loading, error, active, and closed Goal presentation states.

### Out of scope

- Any relationship, grouping, assignment, or shared lifecycle between Goals and Rewards.
- Reward affordability, reward combinations, `lastReachedContext`, or Dashboard motivational composition.
- Persisted progress snapshots, progress events, change history, or historical earning evaluations.
- Automatic completion, automatic cancellation, reopening a closed Goal, or deletion of a Goal in the MVP.
- Statistics, charts, trend forecasts, time-to-goal estimates, gamification, AI, recommendations, external catalogues, categories, images, search, or advanced filters.
- Currency conversion, exchange rates, currencies beyond `EUR` and `USD`, or Goal-local currency configuration.
- Recalculation of Salary, Workday, Earnings, corrections, or `ALL_TIME` inside the Goals module or Angular.

## Functional requirements

- [x] A Goal belongs to the implicit single user of the MVP and is independent from every Reward.
- [x] A Goal has a required title and a strictly positive target amount.
- [x] A Goal currency comes exclusively from the global application currency and is not selected independently by the user.
- [x] An active Goal exposes dynamic progress derived only from the effective `ALL_TIME` Earnings result.
- [x] Reaching 100% progress never completes a Goal automatically.
- [x] The user may manually mark a reached active Goal as `COMPLETED`.
- [x] The user may cancel an active Goal, which closes it as `CANCELLED`.
- [x] Only an active Goal may be edited; title and target amount are editable.
- [x] Closed Goals form the Goal history and do not retain progress snapshots or change events.
- [x] If `ALL_TIME` is unavailable, an active Goal exposes an unavailable progress state without invented monetary values.
- [x] Angular renders backend-resolved progress and never calculates money, percentages, remaining amounts, or lifecycle eligibility.

## Goal model and lifecycle

### Goal resource

| Field | Category | Meaning |
|---|---|---|
| `id` | Technical identity | Stable generated identifier. |
| `title` | User-entered | Required Goal title. No product-specific maximum is defined beyond technical storage and transport limits. |
| `targetAmount` | User-entered | Strictly positive monetary target, represented with `BigDecimal` and the public monetary scale for the global currency. |
| `currencyCode` | Persisted currency | Snapshot of the global application currency at creation. It is not user-selectable and is not editable. |
| `status` | Lifecycle | `ACTIVE`, `COMPLETED`, or `CANCELLED`. |
| `createdAt`, `updatedAt` | Technical audit data | Resource timestamps; they are not a Goal change history. |
| `closedAt` | Lifecycle data | Timestamp at which an active Goal became `COMPLETED` or `CANCELLED`; absent while active. |

The current application currency is common to Earnings, Rewards, and Goals. The application may only use `EUR` or `USD`; no conversion is performed. Creating the first Goal is monetary data and must participate in the existing global-currency lock so the currency cannot subsequently change and mix monetary records.

### States and transitions

| Current state | Allowed operation | Result |
|---|---|---|
| `ACTIVE` | Create | A new active Goal. |
| `ACTIVE` | Edit title or target amount | Remains `ACTIVE`; current progress is recomputed dynamically from current `ALL_TIME`. |
| `ACTIVE` | Complete manually when current backend progress is reached | `COMPLETED`; `closedAt` is recorded. |
| `ACTIVE` | Cancel manually | `CANCELLED`; `closedAt` is recorded. |
| `COMPLETED` | No lifecycle or edit operation | Terminal historical Goal. |
| `CANCELLED` | No lifecycle or edit operation | Terminal historical Goal. |

Completion is a manual confirmation, not an automatic transition. The backend must reject completion when current Goal progress is unavailable or has not reached the target. Cancellation does not depend on Goal progress. Goals are never deleted in the MVP: cancellation preserves an explicit historical record instead of removing one.

## Progress

### Source and meaning

An active Goal's progress is derived only from the current effective `ALL_TIME` result provided by SPEC 003. It is an indicator of how WorkWorth-registered net earnings compare with this Goal's target; it is not a claim about money still held, saved, unspent, or otherwise available to the user.

For an evaluable `ALL_TIME` result whose currency equals the Goal currency, the backend resolves:

```text
progressAmount = effective ALL_TIME amount
remainingAmount = max(targetAmount - progressAmount, 0)
reached = progressAmount >= targetAmount
progressPercentage = min((progressAmount / targetAmount) × 100, 100)
```

All monetary subtraction, comparison, division, rounding, capping, and completion eligibility belong exclusively to the backend. Angular receives and presents the resolved values; it must not recreate these formulas.

The Goal response must make clear whether progress is evaluable. When `ALL_TIME` is `UNAVAILABLE`, it returns an unavailable progress result with no `progressAmount`, `remainingAmount`, percentage, or reached value. `UNAVAILABLE` is not zero progress and does not make a Goal completable.

Closed Goals are retained as history resources and do not participate in live progress evaluation. No progress snapshot is stored at completion or cancellation, so the MVP does not present a historical percentage or historical effective Earnings amount for a closed Goal.

## Relationship with Rewards

Goals and Rewards are fully independent in this MVP:

- A Goal does not contain Rewards.
- A Reward does not belong to a Goal.
- Completing or cancelling a Goal never acquires, edits, evaluates, combines, or otherwise changes a Reward.
- Acquiring, editing, or deleting a Reward never changes Goal progress or lifecycle.

The two features may both consume effective Earnings, but neither duplicates the other feature's business rules.

## Backend and Angular responsibilities

| Backend | Angular |
|---|---|
| Persist Goals, statuses, timestamps, target amounts, and currency snapshots | Render Goal DTOs, forms, lists, loading, empty, error, and closed-history states |
| Obtain the effective `ALL_TIME` Earnings aggregate through the Earnings application boundary | Format backend-provided monetary values only when present |
| Resolve availability, progress amount, remaining amount, percentage, reached status, and lifecycle eligibility | Initiate approved create, edit, complete, and cancel actions |
| Enforce global-currency compatibility and the currency lock | Keep active and closed views independently recoverable after UI errors |
| Enforce state transitions and `ProblemDetail` errors | Never calculate progress, compare amounts, convert currency, or complete a Goal automatically |

The Goals module must not access Salary, Workday, Earnings, or Rewards repositories directly to recreate existing rules. It consumes the public Earnings application abstraction for `ALL_TIME` effective data and the global application-currency abstraction for currency consistency.

## States, errors, and presentation

### Active and closed lists

- An empty active list is a valid state and invites the user to create a Goal.
- An empty closed list is a valid state and explains that completed or cancelled Goals will appear there.
- `COMPLETED` and `CANCELLED` Goals appear in history; `ACTIVE` Goals do not.
- An active Goal with unavailable `ALL_TIME` data remains visible with unavailable progress; it is not hidden, zeroed, or cancelled.

### Errors

Future API contracts use DTOs and standard `ProblemDetail` responses. At minimum, the backend distinguishes:

- Validation errors for a blank title or non-positive/invalid target amount.
- Resource-not-found errors for an unknown Goal.
- State-conflict errors for editing, completing, or cancelling a closed Goal.
- Progress-unavailable errors when a completion request cannot be evaluated from `ALL_TIME`.
- Currency-mismatch errors for inconsistent persisted data; no conversion or fallback is attempted.

An error in a Goal request must not hide already valid Goal lists or progress results. A Goal UI error has no effect on Earnings, Workday, Rewards, or Dashboard sections.

## Use cases

### Create an active Goal

**Given** the application currency is `EUR` and the user supplies a title and a positive target of 500 EUR
**When** the Goal is created
**Then** WorkWorth creates one `ACTIVE` Goal with currency `EUR`, without a Goal-local currency selection.

### Present dynamic progress

**Given** an active Goal with a target of 500 EUR and an evaluable effective `ALL_TIME` amount of 320 EUR
**When** its progress is requested
**Then** the backend returns the current progress and a remaining amount of 180 EUR, without asserting that 320 EUR is saved or spendable.

### Keep a reached Goal active until manual completion

**Given** an active Goal whose backend-resolved progress has reached 100%
**When** the Goal is read
**Then** it remains `ACTIVE` until the user explicitly marks it completed.

### Preserve a closed Goal in history

**Given** an active Goal is completed or cancelled
**When** the history is requested
**Then** it appears as a closed Goal with its status and closure timestamp, without a stored progress snapshot or edit operation.

### Keep an unavailable progress explicit

**Given** an active Goal and an `ALL_TIME` Earnings result that is `UNAVAILABLE`
**When** progress is requested
**Then** WorkWorth returns an unavailable progress state without an invented amount, percentage, remaining amount, or completion eligibility.

## Acceptance criteria

- [x] A Goal persists a required title, a strictly positive target amount, the global currency snapshot, an allowed lifecycle state, and technical timestamps.
- [x] A Goal has no user-selectable currency and no Goal-to-Reward relationship.
- [x] Active Goal progress uses only the current effective `ALL_TIME` Earnings result.
- [x] Progress is not presented as a bank balance, savings, or available money.
- [x] `ALL_TIME` unavailable produces an explicit unavailable progress result with no invented monetary values.
- [x] Reaching the target does not complete a Goal automatically.
- [x] The backend permits manual completion only for an active Goal whose current progress is reached.
- [x] The backend permits an active Goal to be cancelled and retains completed/cancelled Goals as history.
- [x] Closed Goals cannot be edited, reopened, deleted, or dynamically re-evaluated in the MVP.
- [x] Editing an active Goal changes only title and target amount; later reads resolve progress against current effective `ALL_TIME` without keeping the prior target.
- [x] No progress snapshots, change events, Reward mutations, currency conversion, statistics, charts, AI, or gamification are introduced.
- [x] Angular contains no Goal economic calculation, currency conversion, or automatic lifecycle decision.

## Technical considerations

- A future backend implementation belongs to a `goals` module in the modular monolith and preserves Controller → Service → Repository boundaries.
- Goals require a Flyway migration and PostgreSQL/Testcontainers integration coverage when persistence is introduced.
- Monetary fields use `BigDecimal`, ISO currency codes, and the established rounding conventions from SPEC 000 and SPEC 003.
- A future technical proposal must define exact DTOs, endpoint routes, `ProblemDetail` codes, indexes, and Angular lazy routes without changing these business rules.
- The global currency provider must be extended in the future so an existing Goal blocks a later application-currency change, consistently with other persisted monetary data.

## Edge cases

- A valid `ALL_TIME` amount of zero is evaluable progress, not `UNAVAILABLE`.
- An `ALL_TIME` amount equal to the target is reached with a remaining amount of zero.
- An `ALL_TIME` amount greater than the target remains reached; public percentage is capped at 100 and no surplus is inferred as available money.
- Editing an active target can make a previously reached active Goal no longer reached; it remains active until a later valid manual completion.
- A completed or cancelled Goal never returns to `ACTIVE` in the MVP.
- A goal created before any Salary profile or earning still uses the current global currency, but its progress remains unavailable until `ALL_TIME` is evaluable.
- Inconsistent stored currencies are rejected explicitly by the backend; they are never converted or compared as unlike amounts.

## Expected tests

| Requirement / rule | Test level | Expected test |
|---|---|---|
| Validation | Unit / integration | Blank title and non-positive or invalid target amount are rejected. |
| Currency | Unit / integration | Created Goal uses only the current global EUR/USD currency and participates in the lock. |
| Active lifecycle | Unit / integration | Create and edit an active Goal; completion and cancellation record terminal state and closure time. |
| Transition protection | Unit / integration | Closed Goals cannot be edited, completed, cancelled, reopened, or deleted. |
| Dynamic progress | Unit | Effective `ALL_TIME` values resolve progress, remaining amount, capped percentage, and reached status in the backend. |
| Availability | Unit / integration | `ALL_TIME` unavailable returns no invented progress values and rejects completion. |
| Manual completion | Unit / integration | A reached Goal remains active until completion is explicitly requested; an unreached Goal cannot be completed. |
| History | Integration | Completed and cancelled Goals appear in the closed list without progress snapshots. |
| Rewards independence | Unit / integration | Goal operations do not query or mutate Rewards. |
| Persistence | Integration | Flyway/PostgreSQL preserves Goal lifecycle, currency, timestamps, and monetary precision. |
| Angular boundary | Angular unit | UI renders backend progress and errors without computing money, progress, or lifecycle eligibility. |

## Traceability and verification

This SPEC is **Verified**. Goals persistence, lifecycle transitions, dynamic backend-resolved progress, global-currency enforcement, REST contracts, lazy Angular route, and presentation are implemented and covered by focused backend and Angular tests. Statistics consumes only closed Goal data through its own backend boundary.
