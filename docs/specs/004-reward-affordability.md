# SPEC 004: Reward Affordability

**Status:** Draft
**Owner:** WorkWorth
**Related tasks:** [TASKS.md](../../TASKS.md)
**Related documentation:** [SPEC process](README.md), [Project foundation](000-project-foundation.md), [Salary net estimation](001-salary-net-estimation.md), [Work schedule and workday](002-work-schedule-and-workday.md), [Earnings and earning periods](003-earnings-and-earning-periods.md), [Business rules](../business-rules.md), [Architecture decisions](../../DECISIONS.md)

## Objective

Allow the user to record personal rewards and understand which pending rewards, or valid combinations of pending rewards, their effectively registered net earnings can currently afford.

Rewards make WorkWorth's earnings tangible. They do not represent a bank balance, savings, spending transactions, assets, or any money external to WorkWorth.

## Context

SPEC 001 determines the valid net-income source and monthly rate. SPEC 002 determines economically computable workday time. SPEC 003 materializes effective net earnings, resolves historical corrections, and exposes the `TODAY`, `WEEK`, `MONTH`, and `ALL_TIME` contexts.

This SPEC consumes those effective Earnings results. It does not calculate salary, economic time, earnings, corrections, period aggregates, or currency conversion. The Spring Boot backend remains the source of truth for reward evaluation; Angular only presents the resulting public DTOs and initiates approved user actions.

`ALL_TIME` means effective net income registered by WorkWorth since use began. It is not a bank balance, savings, assets, money remaining after expenses, or a statement that the user literally has that amount available to spend.

## Scope

### In scope

- Personal reward CRUD.
- A positive integer `quantity`, defaulting to `1`.
- A total positive price for the entire reward, not a unit price.
- Currency codes limited to `EUR` and `USD` through the application-wide currency configuration.
- Pending and acquired reward lifecycle states.
- Manual marking of a pending reward as acquired.
- Separate pending and acquired reward lists.
- A minimal persisted `lastReachedContext` marker to detect that a reward has newly become achievable in a more immediate context.
- On-demand evaluation of pending rewards against `TODAY`, `WEEK`, `MONTH`, and `ALL_TIME` effective Earnings contexts.
- Determination of the smallest/recentest context that can afford a reward, using `TODAY → WEEK → MONTH → ALL_TIME` priority.
- Valid dynamic combinations of distinct pending rewards.
- A later request for another valid combination when alternatives exist.
- Backend, PostgreSQL/Flyway, `ProblemDetail`, DTO, unit, and PostgreSQL/Testcontainers integration coverage.
- A later UI that presents backend results without local economic calculations.

### Out of scope

- Reward-change history.
- Undoing acquisition.
- Evaluation history or persisted evaluation results.
- Currency conversion, exchange rates, or currencies beyond `EUR` and `USD`.
- External reward sources, images, categories, search, filters, recommendations, or AI.
- Dashboard motivational composition, including a principal reward card (SPEC 005).
- Goals (SPEC 006), statistics and charts (SPEC 007), and advanced gamification.

## Functional requirements

- [ ] A reward belongs to the user and is independent from all other rewards.
- [ ] A reward has a name, a positive integer quantity, a strictly positive total price, and an ISO currency code.
- [ ] A new reward defaults to quantity `1` and pending status.
- [ ] The persisted price is the total price for the complete reward and is never multiplied by quantity.
- [ ] The user can edit the name, quantity, and total price of a pending reward.
- [ ] The user can delete a reward.
- [ ] The user can manually mark a pending reward as acquired.
- [ ] An acquired reward appears in the acquired list, is absent from the pending list, and never participates in later reward evaluations or combinations.
- [ ] The MVP does not support undoing acquisition or recording a reward-change history.
- [ ] The backend can evaluate a pending reward against an explicitly requested Earnings context.
- [ ] The backend can determine the first affordable context for a pending reward in the priority order `TODAY → WEEK → MONTH → ALL_TIME`.
- [ ] The backend stores the last relevant affordable context only as the minimal marker needed to detect a newly reached or improved context; it does not persist an evaluation history.
- [ ] A valid reward evaluation uses only the effective Earnings amount supplied for the selected context.
- [ ] A reward evaluation uses the domain outcomes `AFFORDABLE` and `SHORTFALL` only when that Earnings context is economically evaluable.
- [ ] A non-evaluable Earnings context produces an explicit evaluation failure/result and never invents an affordability state, surplus, shortfall, or monetary amount.
- [ ] The backend can select a valid dynamic combination of distinct pending rewards whose total price does not exceed the effective available amount for a context.
- [ ] A combination is dynamic evaluation output only and is never persisted as a new reward.
- [ ] A reward can appear at most once in one combination.
- [ ] The frontend does not calculate affordability, context priority, combinations, surplus, shortfall, exchange rates, or monetary rounding.

## Business rules

### Reward model and lifecycle

| Field | Category | Rule |
|---|---|---|
| `id` | Persisted | Stable technical identifier. |
| `name` | User-entered | Required reward name; no product-specific maximum length is defined beyond technical storage/transport limits. |
| `quantity` | User-entered | Positive integer; defaults to `1`. |
| `price` | User-entered | Strictly positive total price for the complete reward, at monetary scale suitable for its currency. |
| `currencyCode` | User-entered/configured | `EUR` or `USD`; it must equal WorkWorth's configured application currency. |
| `status` | Persisted | `PENDING` or `ACQUIRED`. |
| `lastReachedContext` | Persisted marker | Nullable last relevant affordable context; it is not an evaluation record or history. |
| `createdAt`, `updatedAt` | Historical technical data | Audit timestamps for the resource record, not a reward-change history. |

`PENDING` rewards are eligible for individual evaluation and combinations. `ACQUIRED` rewards are retained in the acquired list, are excluded from all later evaluation, and cannot be restored to pending in the MVP.

`lastReachedContext` records the last relevant affordable context under the fixed priority order. On a dynamic evaluation, a reward is newly reached when it has no stored reached context and now has one, or has improved to a more immediate context (for example, from `MONTH` to `WEEK`). The marker is then updated to the new relevant context. It is deliberately the only persisted transition aid: it does not retain amounts, results, timestamps, historical evaluations, or combinations.

The user may edit only a pending reward. Acquired rewards are retained as historical personal records and are not edited by this MVP flow.

For presentation, a quantity of `1` may be omitted naturally (for example, “Libro”); a higher quantity may be shown as “2 hamburguesas”. This is display-only and never changes the persisted total price.

### Application currency

The WorkWorth application currency is the common currency for Earnings, rewards, prices, and affordability evaluation. The MVP supports only `EUR` and `USD`.

No currency conversion exists. If a reward currency differs from the effective Earnings currency because of inconsistent data or incomplete application configuration, the backend rejects evaluation explicitly. It must not convert, compare unlike amounts, or choose a fallback currency.

The application-wide currency configuration is a prerequisite for this SPEC. It is not implemented by creating a separate Rewards-only currency setting.

### Earnings contexts and priority

Rewards may be evaluated against any explicit context:

| Context | Meaning supplied by SPEC 003 |
|---|---|
| `TODAY` | Effective earnings for the current local date. |
| `WEEK` | Effective earnings for the current ISO Monday–Sunday week. |
| `MONTH` | Effective earnings for the current calendar month. |
| `ALL_TIME` | All effective net earnings registered by WorkWorth. |

For automatic relevance, the backend evaluates contexts in this fixed order:

```text
TODAY → WEEK → MONTH → ALL_TIME
```

The first context whose effective available amount is at least the reward price is the reward's relevant affordable context. This deliberately favors the smallest/recentest context in which the reward is achievable.

For example, a reward costing 120 EUR with `TODAY=30`, `WEEK=85`, `MONTH=120`, and an affordable `ALL_TIME` result is relevant in `MONTH`, not `ALL_TIME`.

If a reward was previously relevant in `MONTH` and becomes relevant in `WEEK`, `WEEK` becomes its relevant context and the transition may be highlighted as a newly reached improvement. The user-facing copy must remain natural and achievement-oriented rather than presenting four independent financial comparisons.

### Individual affordability evaluation

For an economically evaluable context whose currency equals the reward currency:

```text
availableAmount = effective Earnings amount for the context
rewardPrice = persisted total reward price

if availableAmount >= rewardPrice:
    outcome = AFFORDABLE
    surplus = availableAmount - rewardPrice
    shortfall = null

if availableAmount < rewardPrice:
    outcome = SHORTFALL
    shortfall = rewardPrice - availableAmount
    surplus = null
```

Equality is `AFFORDABLE` with a surplus of zero. The backend performs all monetary calculation and rounding under the shared money rules; Angular must only display public values received from the API.

If Earnings cannot produce a valid amount for the context, the backend must return an explicit non-evaluable error/result. It must not introduce a third affordability outcome, and it must not produce `AFFORDABLE`, `SHORTFALL`, `surplus`, or `shortfall` by guessing a value.

The UI should use progress-oriented language rather than expose `SHORTFALL` as its main message. When an amount is available, the preferred presentation is equivalent to:

> Te faltan 35 € para conseguir tus auriculares (120 €).

It must not estimate time remaining.

### Dynamic combinations

A combination is a dynamic selection of two or more distinct pending rewards. It is valid when:

- every selected reward is `PENDING`;
- each selected reward appears at most once;
- all selected rewards have the configured evaluation currency;
- the sum of their persisted total prices does not exceed the effective available amount for the selected context.

Names, textual similarity, categories, and quantity values do not determine combinability. Quantity is already part of the independently persisted reward description, while price is already the total price.

The backend may return any valid combination; it is not required to maximize the total spent amount or solve an optimal subset-selection problem. An individually affordable reward may be presented instead of a combination. For example, with 100 EUR available, presenting a 90 EUR book is valid even when 10 EUR remains.

When alternatives exist, a user action such as “Ver otra combinación” requests another valid combination. The selected combination is not persisted.

### Presentation priority

When multiple current reward results exist, the intended presentation priority is:

1. Newly reached pending rewards and, when associated with one of those transitions, a valid newly highlighted combination.
2. Valid combinations of pending rewards that are currently affordable.
3. An individually affordable pending reward that is relevant to the available amount.
4. Progress toward a pending reward that is not yet affordable.

The product should communicate achievements and attainable personal items naturally. It must not imply that WorkWorth knows the user's bank balance or that an evaluated reward was actually purchased.

`lastReachedContext` detects transitions for individual rewards only. Because combinations are deliberately temporary and have no persisted history, the MVP can highlight a combination as newly reached when it includes a reward with a detected transition, but it does not claim to detect every combination that may have become valid solely because the available amount changed.

### Dynamic evaluation and corrections

The following are calculated under demand and never persisted:

- `AFFORDABLE` / `SHORTFALL`;
- available amount;
- relevant affordable context;
- surplus / shortfall;
- selected combination.

`lastReachedContext` is the sole exception: it persists only the previous relevant-context marker required to identify a newly reached or improved context. It does not persist an evaluation, amount, remainder, combination, or timeline.

An evaluation must consume the current effective Earnings result. Therefore, an eligible historical workday correction in SPEC 003 is reflected on the next evaluation without recomputing salary, time, or corrections inside Rewards.

## Use cases

### Create a personal reward

**Given** the application has a configured supported currency
**When** the user records “hamburguesa”, quantity `2`, and total price `30 EUR`
**Then** WorkWorth creates one pending personal reward with quantity `2` and total price `30 EUR`, not `60 EUR`.

### Find the relevant affordable context

**Given** a pending 120 EUR reward and effective context amounts of Today 30 EUR, Week 85 EUR, Month 120 EUR, and an affordable All Time amount
**When** WorkWorth evaluates its automatic relevance
**Then** it reports `MONTH` as the first affordable context.

### Present progress for an unaffordable reward

**Given** a pending 120 EUR reward and an evaluable context with 85 EUR available
**When** WorkWorth evaluates the reward for that context
**Then** the result is `SHORTFALL` with a 35 EUR shortfall that the UI can present as progress-oriented copy.

### Select a valid reward combination

**Given** pending rewards of 30 EUR hamburguesas, 60 EUR funkos, and 120 EUR headphones, with 90 EUR available in an evaluable context
**When** WorkWorth requests a combination
**Then** it may return the two distinct pending rewards costing 30 EUR and 60 EUR, without persisting a combined reward.

### Mark a reward as acquired

**Given** a pending reward
**When** the user marks it as acquired
**Then** it becomes `ACQUIRED`, appears in the acquired list, disappears from pending results, and is excluded from subsequent evaluation and combinations.

### Reject a non-evaluable currency or Earnings context

**Given** a reward whose currency does not equal the configured/effective Earnings currency, or an Earnings context without a valid amount
**When** WorkWorth receives an evaluation request
**Then** it explicitly rejects the evaluation without conversion or invented affordability data.

## Acceptance criteria

- [ ] A pending reward persists a name, positive integer quantity, positive total price, supported currency, and `PENDING` status.
- [ ] New rewards default quantity to `1`; their stored price is never multiplied by quantity.
- [ ] Pending rewards can be edited and deleted.
- [ ] Marking a pending reward as acquired makes it `ACQUIRED`, excludes it from pending lists/evaluations/combinations, and retains it in acquired results.
- [ ] There is no MVP operation to restore an acquired reward or retrieve a reward-change history.
- [ ] Explicit evaluation of each Earnings context consumes only the effective Earnings amount for that context.
- [ ] Automatic relevance selects the first affordable context in `TODAY → WEEK → MONTH → ALL_TIME` order.
- [ ] An equal available amount and price is `AFFORDABLE` with surplus zero.
- [ ] A lower available amount is `SHORTFALL` with the exact public shortfall; a higher amount is `AFFORDABLE` with the exact public surplus.
- [ ] An unavailable Earnings amount or mismatched currency cannot create an affordability outcome or substitute monetary value.
- [ ] A valid combination contains at least two distinct pending rewards, never exceeds the available amount, and is not persisted.
- [ ] Another-combination requests return a valid alternative when one exists, without locally calculating a combination in Angular.
- [ ] A current evaluation reflects the latest effective Earnings values after eligible historical corrections.
- [ ] API contracts use DTOs and `ProblemDetail`; Angular contains no reward business logic or monetary calculations.
- [ ] No Dashboard motivational composition, goals, statistics, AI, external integrations, currency conversion, advanced catalog behavior, or additional currencies is implemented by this SPEC.

## Technical considerations

- The future backend module is `rewards` in the Spring Boot modular monolith and preserves Controller → Service → Repository boundaries.
- A reward entity is personal by domain ownership. The single-user MVP need not introduce authentication, but the persistence/API design must avoid treating a reward as globally shared catalog data.
- Monetary storage and calculation use `BigDecimal`, ISO currency codes, and the established public rounding rules. Floating-point types are prohibited.
- Reward evaluation consumes an Earnings application abstraction or query service. It must not query salary rates, workday intervals, correction records, or JPA entities belonging to those modules directly.
- Reward persistence, constraints, and later schema evolution use Flyway and PostgreSQL. Integration tests use PostgreSQL Testcontainers.
- A global application-currency mechanism limited to `EUR` and `USD` is a prerequisite and must be defined outside a Rewards-only configuration.
- `EarningPeriodResponse` exposes `AVAILABLE` / `UNAVAILABLE` availability per context. `AVAILABLE` may carry a valid zero amount; `UNAVAILABLE` carries no amount or currency. Rewards must consume that contract independently for each context and must not infer a value for an unavailable one.
- Proposed API contracts must distinguish persisted reward resources from dynamic evaluation DTOs. Dynamic outcomes and combinations are never persisted.
- Failed evaluation of unavailable Earnings/currency mismatch uses stable `ProblemDetail` semantics; exact HTTP status and domain error-code names require the technical proposal.
- The later Angular UI may choose Spanish presentation templates from backend-provided structured results, but cannot derive money, context priority, shortfall, surplus, or combinations itself.

## Edge cases

- A zero, negative, fractional, or absent quantity is invalid.
- A zero, negative, non-numeric, or over-precision total price is invalid according to established currency validation rules.
- Quantity `2` and price `30 EUR` means the complete reward costs 30 EUR.
- A pending reward with no affordable context may still produce an explicit-context `SHORTFALL` when that context is evaluable.
- An acquired reward is neither individually evaluated nor eligible for a combination.
- A context with no valid Earnings amount cannot return an affordability state or monetary remainder.
- A reward currency mismatch is rejected; no EUR/USD conversion is attempted.
- A combination cannot include the same reward twice, even if its quantity is greater than one.
- A reward price equal to available amount is affordable and may appear as an individual reward or part of a valid combination.
- A valid combination may leave unused available amount; optimal use of all money is not required.
- If only one individually affordable pending reward exists, a multi-reward combination is unavailable rather than duplicating that reward.
- Later effective Earnings corrections change future evaluations but do not create historical reward-evaluation records.

## Expected tests

| Requirement / rule | Test level | Expected test |
|---|---|---|
| Reward validation | Unit | Reject non-positive price and non-positive/non-integer quantity; default quantity is one. |
| Total-price semantics | Unit | Quantity does not multiply stored price or evaluation price. |
| Pending lifecycle | Unit / Integration | Create, edit, delete, and acquire a pending reward. |
| Acquired exclusion | Unit / Integration | Acquired rewards appear in acquired results and are absent from pending evaluation/combination queries. |
| Explicit contexts | Unit | Each evaluation consumes the corresponding effective Earnings context only. |
| Relevant context | Unit | Today, Week, Month, and All Time priority selects the first affordable context. |
| Newly reached context | Unit | A null marker or a transition from `MONTH` to `WEEK` updates `lastReachedContext` without persisting an evaluation history. |
| Affordability | Unit | Equal, lower, and higher available amounts produce the approved outcome and remainder. |
| Unavailable Earnings | Unit / Integration | No valid Earnings amount produces explicit non-evaluable behavior without invented money. |
| Currency mismatch | Unit / Integration | Unlike currencies reject evaluation without conversion. |
| Combination validity | Unit | A combination has distinct pending rewards, at least two items, and total price no greater than available amount. |
| Alternative combination | Unit | A further request yields a different valid combination when an alternative exists. |
| Non-optimal selection | Unit | A valid non-maximal individual reward/combination is accepted. |
| Earnings correction integration | Integration | A later effective Earnings correction changes the next dynamic reward evaluation without persisting an evaluation. |
| Persistence | Integration | PostgreSQL/Testcontainers preserves reward data, status, currency, and constraints. |
| API errors | Integration | Validation, resource-not-found, acquired-state, unavailable-Earnings, and currency-mismatch failures return stable `ProblemDetail` responses. |
| UI boundary | UI | Angular displays backend evaluation results and does not calculate price, affordability, context priority, or combinations. |

## Dependencies and technical choices for the implementation proposal

No functional ambiguity remains in this Draft. The implementation proposal must still define, without changing these business rules:

1. The request shape used to exclude a temporarily shown combination when the user asks for another one. It must not persist a combination or evaluation history.
2. The separate global-currency configuration flow that supplies the shared `EUR`/`USD` application currency. Rewards must depend on it rather than create a parallel setting.
3. Stable endpoint paths, HTTP statuses, and `ProblemDetail` codes for validation, acquired-state, unavailable-Earnings, and currency-mismatch failures.

## Traceability and verification

This SPEC is Draft. No Rewards backend, migration, API, Angular UI, or evaluation behavior is authorized until this specification and its technical proposal receive explicit approval.
