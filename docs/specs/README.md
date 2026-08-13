# Specifications

This directory is the functional source of truth for WorkWorth features.

## Spec-Driven Development workflow

```text
SPEC (Draft) → technical proposal → explicit approval → implementation
→ tests and compilation → verification against the SPEC → Verified
```

No implementation may start until its SPEC and technical proposal are explicitly approved. An approval covers only the described scope.

## Statuses

| Status | Meaning |
|---|---|
| Draft | The feature is being defined and cannot be implemented. |
| Approved | The requirements are approved; an implementation proposal is still required. |
| Implemented | The approved implementation was completed. |
| Verified | Tests and acceptance criteria have been checked against the SPEC. |

## Required structure

Every SPEC must use [TEMPLATE.md](TEMPLATE.md) and contain the sections defined there. Requirements, acceptance criteria, and expected tests must be traceable to one another.

`TASKS.md` tracks delivery planning; it never replaces a SPEC's business rules.

## Initial specifications

| SPEC | Status | Scope |
|---|---|---|
| [000-project-foundation.md](000-project-foundation.md) | Approved | Technical and delivery foundations. |
| [001-salary-net-estimation.md](001-salary-net-estimation.md) | Verified | Net-income sources and fiscal estimation. |
| [002-work-schedule-and-workday.md](002-work-schedule-and-workday.md) | Verified | Schedule, automatic workdays, breaks, and absences. |
| [003-earnings-and-earning-periods.md](003-earnings-and-earning-periods.md) | Verified | Today, week, month, and all-time earnings. |
| [004-reward-affordability.md](004-reward-affordability.md) | Verified | Reward evaluation by earnings context. |
| [005-dashboard.md](005-dashboard.md) | Verified | Motivational dashboard hierarchy. |
| [006-goals.md](006-goals.md) | Verified | Personal goals and progress. |
| [007-statistics.md](007-statistics.md) | Verified | Historical statistics and aggregated charts. |
