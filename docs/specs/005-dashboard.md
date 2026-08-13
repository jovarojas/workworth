# SPEC 005: Motivational Dashboard

**Status:** Draft
**Owner:** WorkWorth
**Related tasks:** [TASKS.md](../../TASKS.md)
**Related documentation:** [SPEC process](README.md), [Reward affordability](004-reward-affordability.md), [Earnings and earning periods](003-earnings-and-earning-periods.md), [Business rules](../business-rules.md), [Architecture decisions](../../DECISIONS.md)

## Objective

Present the user's current effective WorkWorth earnings together with a concise, current motivational view of pending personal rewards. The Dashboard helps make registered net earnings tangible; it does not represent a bank balance, savings, spending capacity, assets, or money external to WorkWorth.

## Context

SPEC 003 supplies effective net Earnings and the `TODAY`, `WEEK`, `MONTH`, and `ALL_TIME` contexts. SPEC 004 owns personal rewards, their affordability, context priority, combinations, global-currency compatibility, and the persisted `lastReachedContext` marker.

The existing Dashboard already presents current earnings, the current workday, and the four Earnings summaries. This SPEC adds a read-only motivational composition for that Dashboard. It consumes current backend results and must not make Angular select reward contexts, calculate affordability, sum reward prices, apply corrections, or infer money.

## Scope

### In scope

- Preserve the existing current-earnings, workday, and period-summary Dashboard content.
- A read-only motivational result for pending rewards, resolved by the backend.
- One primary pending reward representing either current affordability or current progress.
- One optional valid combination of two or more pending rewards.
- Empty, unavailable, loading, and independent-error Dashboard states.
- Responsive, accessible Angular presentation that keeps existing Dashboard sections visible when motivation cannot be loaded.
- Navigation from a motivational result to the existing Rewards area.

### Out of scope

- Persisting, acknowledging, or displaying a durable “newly reached” event.
- Changing `lastReachedContext`, reward lifecycle, reward evaluation, or combination rules from SPEC 004.
- Goals, goal progress, statistics, charts, AI, external recommendations, gamification, images, categories, search, filters, or a reward catalog.
- Currency conversion, exchange rates, currencies beyond `EUR` and `USD`, or a Dashboard-local currency setting.
- Calculating earnings, salary rates, economic time, corrections, affordability, shortfall, surplus, or combinations in Angular.

## Functional requirements

- [ ] The Dashboard continues to show its current Earnings and Workday sections independently of motivational data.
- [ ] The backend provides one read-only Dashboard motivation result based only on current pending rewards and current effective Earnings.
- [ ] The Dashboard presents a primary reward when the backend supplies one.
- [ ] The backend selects an affordable primary reward by the most immediate relevant context in the fixed order `TODAY → WEEK → MONTH → ALL_TIME`.
- [ ] When no pending reward is affordable, the backend supplies a progress reward using the most immediate evaluable context in the same order.
- [ ] A valid optional combination contains at least two distinct pending rewards and is supplied only by the backend.
- [ ] A motivation result with no pending rewards presents an actionable empty state without treating it as an error.
- [ ] A non-evaluable Earnings situation presents a clear unavailable state without an invented amount or affordability outcome.
- [ ] A motivational loading or error state never hides otherwise valid Earnings or Workday data.
- [ ] Angular displays only amounts, currency codes, contexts, outcomes, and combinations received from the backend.

## Business rules

### Dashboard motivation states

The Dashboard motivation response has exactly one top-level presentation state:

| State | Meaning | Primary reward | Combination |
|---|---|---|---|
| `EMPTY` | There are no pending rewards. | `null` | `null` |
| `AVAILABLE` | A pending reward is currently affordable. | Affordable reward | Optional current valid combination |
| `PROGRESS` | At least one context is evaluable, but no pending reward is affordable. | Shortfall reward | `null` |
| `UNAVAILABLE` | Pending rewards exist but none has an evaluable Earnings context. | `null` | `null` |

These are Dashboard presentation states, not new Reward domain states. SPEC 004 keeps `AFFORDABLE` and `SHORTFALL` as the only affordability outcomes.

### Primary reward selection

The backend evaluates pending rewards using current effective Earnings. It selects one primary reward without involving Angular:

1. Prefer an `AFFORDABLE` reward with the most immediate `relevantContext` under `TODAY → WEEK → MONTH → ALL_TIME`.
2. If several affordable rewards have the same relevant context, select the one with the lowest stable reward identifier.
3. If no reward is affordable, prefer a `SHORTFALL` reward with the most immediate `progressContext` under the same order.
4. If several progress rewards have the same progress context, select the one with the smallest backend-provided shortfall; ties use the lowest stable reward identifier.
5. If every pending reward is non-evaluable, return `UNAVAILABLE`.

The priority is temporal/contextual first. The shortfall comparison is only a deterministic tie-break among rewards that share the same already-selected progress context.

### Combination

The optional combination reuses SPEC 004 exactly: it consists only of distinct `PENDING` rewards, has at least two items, uses each stored total price once, and does not exceed the effective available amount for its selected context. It is dynamic and never persisted.

The backend may include a combination only with an `AVAILABLE` motivation. Its context, available amount, total price, currency, and items are authoritative. Angular does not request the four Earnings contexts to find a combination.

### Newly reached transitions

This version does not include a Dashboard event for a reward that was newly reached. `newlyReached` and `lastReachedContext` remain governed by SPEC 004 and are not included in the Dashboard motivation contract.

The Dashboard motivation endpoint is read-only: loading it must not update `lastReachedContext`, consume a transition, create an event, or persist any motivational result. A future requirement for durable transition highlighting requires a separate SPEC decision; it must not be inferred from current data.

### Currency and amounts

All motivation amounts use the effective currency received from backend. The Dashboard displays that currency without a local fallback or conversion. A reward/currency inconsistency remains a backend concern under SPEC 004 and must never be compensated in Angular.

### Unavailable Earnings and errors

`UNAVAILABLE` means no pending reward can be evaluated from current effective Earnings. It is not `SHORTFALL`, zero earnings, or a monetary estimate. A failed request is distinct from a valid `UNAVAILABLE` response and is shown only in the motivation section.

## Use cases

### Show an affordable reward

**Given** pending rewards and current effective Earnings that make a reward affordable first in `WEEK`
**When** the Dashboard loads motivation
**Then** the backend returns `AVAILABLE` with that reward and `WEEK` as its relevant context.

### Show progress when nothing is affordable

**Given** pending rewards with evaluable Earnings but no affordable reward
**When** the Dashboard loads motivation
**Then** it returns `PROGRESS` with one backend-selected reward, its progress context, and the public shortfall.

### Show no motivation before rewards exist

**Given** there are no pending rewards
**When** the Dashboard loads motivation
**Then** it returns `EMPTY` and the UI invites the user to add a reward.

### Keep Earnings visible after a motivation failure

**Given** the Earnings and Workday sections load successfully but the motivation request fails
**When** the Dashboard renders
**Then** the existing sections remain visible and only the motivation section shows its contextual error.

## Acceptance criteria

- [ ] Existing Dashboard Earnings and Workday behavior is retained.
- [ ] `GET /api/v1/dashboard/motivation` returns only current, backend-resolved motivation data.
- [ ] The response distinguishes `EMPTY`, `AVAILABLE`, `PROGRESS`, and `UNAVAILABLE`.
- [ ] An affordable primary reward uses the first applicable context according to `TODAY → WEEK → MONTH → ALL_TIME`.
- [ ] A progress primary reward uses the first evaluable context according to the same order.
- [ ] The response contains no persisted or durable newly-reached event.
- [ ] An optional combination contains at least two distinct pending rewards and is supplied only with `AVAILABLE`.
- [ ] `UNAVAILABLE` does not expose an invented amount, shortfall, surplus, or affordability outcome.
- [ ] The Dashboard formats only backend-provided amounts and currency codes and makes no economic decision.
- [ ] A motivation error or empty state does not remove valid Earnings or Workday content.
- [ ] The Dashboard remains usable on small screens and communicates loading/error states accessibly.

## Technical considerations

### Conceptual API contract

`GET /api/v1/dashboard/motivation`

```json
{
  "state": "AVAILABLE",
  "primaryReward": {
    "reward": {
      "id": 12,
      "name": "Auriculares",
      "quantity": 1,
      "price": 120.00,
      "currencyCode": "EUR",
      "status": "PENDING"
    },
    "evaluable": true,
    "relevantContext": "MONTH",
    "progressContext": null,
    "outcome": "AFFORDABLE",
    "availableAmount": 120.00,
    "surplus": 0.00,
    "shortfall": null
  },
  "combination": null
}
```

For `EMPTY` and `UNAVAILABLE`, `primaryReward` and `combination` are `null`. For `PROGRESS`, `primaryReward.outcome` is `SHORTFALL`, `progressContext` and `shortfall` are non-null, while `relevantContext` and `surplus` are null. `combination` reuses the public shape and semantics of SPEC 004’s relevant combination result.

The final DTO names and exact nesting are an implementation concern, but the endpoint must remain read-only and preserve these semantics. It may reuse application services from Earnings and Rewards; it must not access Workday, Salary, or Earnings repositories directly to recreate calculations.

### Responsibilities

| Backend | Angular |
|---|---|
| Evaluate current pending rewards and resolve context priority | Render returned state and structured values |
| Select the primary reward using the specified deterministic order | Choose Spanish copy templates without changing meaning |
| Resolve earnings availability, outcomes, shortfall, surplus, and currency consistency | Format received monetary values only when present |
| Select an optional valid combination | Handle independent loading, empty, unavailable, and error states |
| Preserve SPEC 004 lifecycle and evaluation boundaries | Link users to existing Rewards management |

## Edge cases

- A pending reward that is not affordable in any evaluable context produces `PROGRESS`, not a negative user-facing assertion or local calculation.
- A valid zero Earnings amount is evaluable and may produce `PROGRESS`; it is not `UNAVAILABLE`.
- A pending reward with all contexts unavailable contributes to `UNAVAILABLE` only when no other pending reward is evaluable.
- Acquired rewards are never candidates for primary motivation or combinations.
- A single individually affordable reward can produce `AVAILABLE`, but cannot produce a combination by duplicating itself.
- A valid combination may leave available money unused; it does not optimize spending.
- An empty combination is not an error and must not be displayed as a reward combination.
- A read of Dashboard motivation never changes `lastReachedContext` or creates a reward evaluation history.
- A future currency mismatch is returned as the established backend `ProblemDetail`; the Dashboard does not compare or convert currencies.

## Expected tests

| Requirement / rule | Test level | Expected test |
|---|---|---|
| Empty motivation | Backend unit / integration | No pending rewards returns `EMPTY` with null reward and combination. |
| Affordable priority | Backend unit | `TODAY`, `WEEK`, `MONTH`, and `ALL_TIME` select the first applicable affordable context. |
| Affordable tie-break | Backend unit | Same-context affordable rewards select the lowest reward id. |
| Progress priority | Backend unit | No affordable reward selects the first evaluable context; same-context ties use the smallest shortfall. |
| Unavailable motivation | Backend unit / integration | All pending rewards non-evaluable returns `UNAVAILABLE` without monetary outcome fields. |
| Combination | Backend unit | Only an `AVAILABLE` result may include a valid two-or-more-item pending combination. |
| Read-only boundary | Backend integration | Motivation reads do not update `lastReachedContext` or persist an evaluation. |
| Contract | Backend integration | JSON carries the declared state-specific nullability and `ProblemDetail` behavior. |
| Partial UI failure | Angular UI | Motivation failure leaves projection, period summaries, and Workday visible. |
| UI boundary | Angular UI | Dashboard renders backend contexts and amounts without affordability or combination calculation. |
| Responsive/accessibility | Angular UI | State changes are announced and the motivation card remains usable on mobile. |

## Traceability and verification

Before implementation, this Draft requires an approved technical proposal for the Dashboard backend read contract and the Dashboard Angular integration. Verification must demonstrate every acceptance criterion, independent loading/error behavior, and that no reward marker or economic rule is duplicated in Angular.
