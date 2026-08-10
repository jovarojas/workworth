# DECISIONS.md

# Architectural Decisions

This document records important architectural and technical decisions made during the development of WorkWorth.

The objective is to explain **why** a decision was made, not only **what** was decided.

---

# ADR-001

## Date

2026-07-23

## Decision

The application follows a Backend-for-Frontend (BFF) architecture.

## Reason

The Spring Boot backend acts as the single source of truth.

Angular is responsible only for rendering data and handling user interaction.

This avoids duplicating business logic across multiple clients and makes it easier to support future mobile applications.

---

# ADR-002

## Date

2026-07-23

## Decision

Business logic will only exist in Spring Boot.

## Reason

Calculations such as salary, earnings, statistics, rewards and goal progress must remain centralized.

Future clients (Android, iOS or desktop) will reuse exactly the same logic.

---

# ADR-003

## Date

2026-07-23

## Decision

REST APIs will never expose JPA entities directly.

## Reason

Entities represent the persistence model.

DTOs represent the API contract.

Separating both layers prevents accidental exposure of internal data and allows the API to evolve independently.

---

# ADR-004

## Date

2026-07-23

## Decision

Constructor Injection will be used everywhere.

## Reason

Constructor injection creates immutable dependencies, improves testability and follows Spring best practices.

Field injection is not allowed.

---

# ADR-005

## Date

2026-07-23

## Decision

PostgreSQL has been selected as the relational database.

## Reason

PostgreSQL offers excellent performance, strong SQL support and is widely used in enterprise applications.

---

# ADR-006

## Date

2026-07-23

## Decision

Angular uses Standalone Components.

## Reason

Standalone components simplify the project structure and follow the current Angular recommendations.

---

# ADR-007

## Date

2026-07-23

## Decision

The frontend communicates only with Spring Boot.

## Reason

The frontend must never communicate directly with:

- AI providers
- External APIs
- Database

Spring Boot controls all external integrations.

---

# ADR-008

## Date

2026-07-23

## Decision

Artificial Intelligence is used only for text generation.

## Reason

Business decisions must remain deterministic.

The AI should never calculate salaries, choose rewards or make business decisions.

Its only responsibility is generating natural language.

---

# ADR-009

## Date

2026-07-23

## Decision

External APIs are optional dependencies.

## Reason

The application must continue working even if:

- Flight APIs fail
- Hotel APIs fail
- Concert APIs fail
- AI services are unavailable

The user experience should degrade gracefully without breaking core functionality.

---

# ADR-010

## Date

2026-07-23

## Decision

The project follows Feature-Based organization on the frontend.

## Reason

Features group all related files together, making the project easier to navigate and maintain than organizing by file type.

---

# ADR-011

## Date

2026-07-23

## Decision

The backend follows a layered architecture.

Controller

↓

Service

↓

Repository

## Reason

Each layer has a single responsibility.

Controllers handle HTTP.

Services implement business logic.

Repositories access persistence.

---

# ADR-012

## Date

2026-07-23

## Decision

API versioning starts at /api/v1.

## Reason

Versioning allows future API evolution without breaking existing clients.

---

# ADR-013

## Date

2026-07-23

## Decision

The project prioritizes maintainability over premature optimization.

## Reason

Readable and testable code provides greater long-term value than micro-optimizations.

Performance optimizations should only be introduced when supported by measurements.

---

# ADR-014

## Date

2026-07-23

## Decision

The MVP will support only a single user.

## Reason

Authentication is intentionally postponed.

The objective is validating the product idea before introducing user management, security and cloud infrastructure.

---

# ADR-015

## Date

2026-07-23

## Decision

The application is designed Mobile-Ready from day one.

## Reason

Although development starts as a web application, the architecture should allow future Android and iOS applications with minimal backend changes.

---

# Future Decisions

This document should be updated whenever an important architectural decision is made.

Examples:

- Introduce caching
- Add Redis
- Adopt Docker
- Change AI provider
- Introduce event-driven communication
- Migrate authentication provider
- Add WebSockets

---

# ADR-016

## Date

2026-08-10

## Decision

The backend will be a modular monolith organized by business capability, with Controller → Service → Repository layers inside each capability.

## Reason

This retains clear responsibilities while avoiding premature distributed-system complexity. Modules can later evolve independently without duplicating business logic.

---

# ADR-017

## Date

2026-08-10

## Decision

WorkWorth adopts Spec-Driven Development.

## Reason

Every important feature must have a SPEC before implementation. This makes business rules, acceptance criteria, expected tests, and approval scope explicit.

---

# ADR-018

## Date

2026-08-10

## Decision

Money uses `BigDecimal`, ISO currency codes, and explicit rounding rules. Time-dependent business logic receives `java.time.Clock`.

## Reason

Financial calculations require predictable precision, while injected time makes behavior reproducible and testable.

---

# ADR-019

## Date

2026-08-10

## Decision

Schema changes will use Flyway migrations. Configuration uses Spring profiles and environment variables. API errors use Spring `ProblemDetail` by default.

## Reason

These choices provide repeatable deployments, safe configuration, and a standard HTTP error contract.

---

# ADR-020

## Date

2026-08-10

## Decision

Business services require automated tests from their introduction. PostgreSQL integration tests will use Testcontainers when persistence is added.

## Reason

Core calculations need early regression protection and integration tests should exercise the production database behavior.

---

# ADR-021

## Date

2026-08-10

## Decision

WorkWorth evaluates net earnings in Today, Current Week, Current Month, and All Time contexts. All Time starts at zero and represents net income registered by WorkWorth, not a bank balance or savings.

## Reason

Explicit periods prevent ambiguity and allow rewards to communicate what work has enabled without claiming to know the user's finances outside the application.
