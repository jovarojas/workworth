# Business Rules Index

This document indexes approved cross-cutting rules. The detailed and authoritative rules will live in their corresponding SPECs under [docs/specs](specs/README.md).

## Approved principles

- WorkWorth communicates what work has enabled the user to achieve, not only what was earned today.
- All earnings and reward calculations use net income; gross annual salary is only an input to a future estimator.
- A user-provided real monthly net income takes priority over an estimated net income.
- Earnings contexts are Today, Current Week, Current Month, and All Time.
- All Time starts at zero and means net income registered by WorkWorth, not a bank balance, savings, or money after expenses.
- Reward evaluation has the domain states `AFFORDABLE` and `SHORTFALL`; the result may include a surplus or shortfall amount.
- The initial user time zone is `Europe/Madrid` and remains editable.

## Planned detailed specifications

- `001-salary-net-estimation.md`
- `002-work-schedule-and-workday.md`
- `003-earnings-periods.md`
- `004-reward-affordability.md`
- `005-dashboard.md`
