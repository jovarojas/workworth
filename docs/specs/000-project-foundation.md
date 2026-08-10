# SPEC 000: Project Foundation

**Status:** Approved  
**Related documentation:** [SPEC process](README.md), [Architecture decisions](../../DECISIONS.md), [Development roadmap](../../TASKS.md)

## Objective

Establish the architectural, delivery, and quality foundations required before WorkWorth product features are implemented.

## Context

WorkWorth is an Angular and Spring Boot application. Spring Boot is the single source of truth for business logic, while Angular renders API data and manages UI state. This SPEC defines technical foundations only; functional behavior belongs to later feature SPECs.

## Functional requirements

- [ ] WorkWorth follows the Spec-Driven Development process defined in [README.md](README.md).
- [ ] Backend capabilities are organized as modules, each preserving Controller → Service → Repository responsibilities.
- [ ] API contracts use DTOs; JPA entities are never exposed directly.
- [ ] API errors use Spring `ProblemDetail` unless a future approved ADR records a justified alternative.
- [ ] Monetary values use `BigDecimal`, ISO currency codes, and explicit rounding rules.
- [ ] Time-dependent backend logic receives `java.time.Clock` as a dependency.
- [ ] Database schema changes use Flyway migrations; Hibernate does not own schema generation.
- [ ] Spring configuration uses profiles and environment variables for sensitive or environment-specific values.
- [ ] Business services have automated tests from their introduction.
- [ ] PostgreSQL integration tests use Testcontainers when persistence is introduced.
- [ ] Angular uses standalone components, lazy feature routes, `provideHttpClient`, environment API configuration, and Signals for appropriate local UI state.

## Business rules

- Spring Boot remains the sole source of business decisions and calculations.
- No implementation begins without an approved SPEC and separately approved technical proposal.
- A task is complete only after relevant tests, compilation, and verification against its SPEC.

## Use cases

### Prepare a future feature

**Given** a feature has been identified  
**When** its requirements are drafted  
**Then** it receives a SPEC before a technical implementation proposal is presented.

### Implement an approved feature

**Given** a SPEC and implementation proposal are explicitly approved  
**When** implementation is performed  
**Then** the result is tested and verified against the SPEC before being reported complete.

## Acceptance criteria

- [ ] The SDD process and reusable template are available under `docs/specs/`.
- [ ] This SPEC is marked `Approved`.
- [ ] Project documentation consistently describes the approved architectural constraints.
- [ ] No product-specific salary, workday, reward, goal, dashboard, or statistics behavior is defined by this SPEC.

## Technical considerations

- The backend is a modular monolith, not a collection of independently deployed services.
- Modules may share cross-cutting infrastructure but must not bypass their Controller → Service → Repository boundaries.
- API versioning starts at `/api/v1/`.
- Future implementation proposals must name the exact files affected and request explicit approval.

## Edge cases

- Environment credentials and secrets must not be committed.
- A future exception to `ProblemDetail`, default monetary rounding, or the standard module boundary requires an ADR and approval.
- Introducing persistence requires its own migration and integration-test proposal.

## Expected tests

| Requirement / rule | Test level | Expected test |
|---|---|---|
| Business services are testable | Unit | Service behavior is tested without a controller. |
| API contract and errors | Integration | DTO responses and `ProblemDetail` errors are verified when endpoints exist. |
| Database migrations | Integration | Flyway migrations run against PostgreSQL Testcontainers when persistence exists. |
| Angular client foundation | Build / unit | Lazy routes and HTTP configuration compile when introduced. |

## Traceability and verification

This specification is approved as a documentation and governance foundation only. It does not authorize backend, frontend, database, configuration, or Git implementation work.
