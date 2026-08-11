# AGENTS.md

# WorkWorth

## Project Overview

WorkWorth is a full-stack application that helps users understand the value of their work by translating earned money into real-life rewards and experiences.

Examples:

- "Today you earned enough for a dinner for two."
- "You can now afford a flight to Venice."
- "You are 15€ away from a weekend Airbnb."

The application is composed of:

- Angular frontend
- Spring Boot backend
- PostgreSQL database

The backend contains all business logic.
The frontend is responsible only for presentation.

---

# Architecture

The project follows a layered architecture.

Controller
↓

Service
↓

Repository
↓

Database

Controllers must never access repositories directly.

Business logic belongs exclusively inside Services.

---

# Technologies

Frontend

- Angular
- TypeScript
- SCSS
- Angular Material
- RxJS
- Signals

Backend

- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Maven
- Lombok

---

# Backend Guidelines

Use constructor injection.

Never use field injection.

Entities represent database tables.

DTOs are mandatory for API communication.

Never expose entities directly.

Validation must use Jakarta Validation annotations.

Use ResponseEntity for every endpoint.

Controllers should remain thin.

Services contain business logic.

Repositories only access the database.

---

# Frontend Guidelines

Angular contains no business logic.

Calculations are performed by the backend.

Components should be small and focused.

Reusable components belong in shared.

Business features belong in features.

Use standalone components.

Prefer Signals for local state.

Use Observables for HTTP communication.

---

# Naming Conventions

Classes

PascalCase

Example

SalaryService

Variables

camelCase

Example

dailyIncome

Constants

UPPER_CASE

Example

DEFAULT_WORK_HOURS

Packages

lowercase

---

# API

Every endpoint starts with

/api/v1/

Use REST conventions.

Examples

GET /api/v1/workdays

POST /api/v1/workdays

PUT /api/v1/workdays/{id}

DELETE /api/v1/workdays/{id}

---

# Error Handling

Create custom exceptions.

Use a global exception handler.

Never return stack traces.

Return meaningful error messages.

---

# Database

Primary keys use Long.

IDs are generated automatically.

Prefer LocalDate for dates.

Prefer LocalTime or LocalDateTime where appropriate.

Avoid unnecessary eager loading.

---

# Code Style

Write clean and readable code.

Prefer readability over cleverness.

Keep methods short.

Avoid duplicated code.

Extract reusable logic.

Use meaningful names.

---

# Comments

Do not comment obvious code.

Comment business decisions only.

Public methods should be self-explanatory.

---

# Testing

Business logic should be testable.

Services should be independent from controllers.

---

# AI Integration

Angular must never communicate directly with AI providers.

Only Spring Boot can access AI APIs.

The AI is responsible only for generating natural language messages.

Business decisions are made before calling the AI.

---

# Future Features

The architecture should allow future support for:

- Multiple users
- Authentication
- JWT
- Mobile application
- External APIs
- AI providers
- Notifications
- Multiple currencies

---

# General Rule

When generating code, always prioritize:

1. Simplicity
2. Readability
3. Maintainability
4. Scalability

Avoid overengineering.

Write production-ready code whenever possible.

---

# Git Commit Convention

Use Conventional Commits.

Examples:

feat: add workday endpoint

fix: correct salary calculation

refactor: simplify reward selection logic

docs: update README

test: add salary service unit tests

style: format code

chore: update dependencies

---

# Design Principles

Follow SOLID principles whenever appropriate.

Prefer composition over inheritance.

Keep classes focused on a single responsibility.

Do not create abstractions until they are needed.

Avoid premature optimization.

Favor readability over clever implementations.

Follow the KISS principle (Keep It Simple, Stupid).

Avoid code duplication (DRY).

---

# Source of Truth

The Spring Boot backend is the single source of truth.

Never duplicate business logic in Angular.

The backend is responsible for:

- Salary calculations
- Workday calculations
- Statistics
- Reward selection
- Goal progress
- AI prompts
- External API communication

Angular is responsible only for:

- Rendering data
- User interaction
- Navigation
- Form validation
- UI state

If the same logic could exist in both frontend and backend, always implement it only in the backend.

---

# AI Coding Instructions

When generating code:

- Prefer clean architecture over quick solutions.
- Generate complete implementations instead of placeholders whenever possible.
- Keep methods small and easy to understand.
- Do not introduce unnecessary libraries or frameworks.
- Follow existing project conventions before creating new ones.
- Reuse existing services and utilities whenever appropriate.
- Explain important architectural decisions in comments only when they are not obvious.

When modifying existing code:

- Preserve the current architecture.
- Do not break public APIs without a clear reason.
- Avoid unrelated refactoring.
- Keep changes as small and focused as possible.

---

# Performance Guidelines

Prefer database queries over loading unnecessary data into memory.

Avoid N+1 query problems.

Paginate large collections.

Do not expose unnecessary fields in API responses.

Cache only when there is a demonstrated need.

---

# Security Guidelines

Never hardcode secrets, API keys or passwords.

Read sensitive configuration from environment variables.

Validate all user input.

Never trust data coming from the frontend.

Sanitize external API responses when necessary.

---

# Project Philosophy

WorkWorth should feel like a polished commercial application, not a university exercise.

Every feature should provide real value to the user.

Favor long-term maintainability over short-term speed.

Whenever multiple implementations are possible, choose the one that is easier to maintain and extend.

The codebase should always be understandable by a new developer joining the project.

# Agent Behavior

Before implementing a feature:

1. Analyze the existing architecture.
2. Reuse existing classes whenever possible.
3. Avoid creating duplicate functionality.
4. Ask whether a simpler solution already exists.

When implementing new code:

- Think like a senior software engineer.
- Prefer maintainable solutions.
- Avoid unnecessary complexity.
- Follow Java and Angular best practices.
- Keep consistency across the entire project.

If requirements are ambiguous:

Do not invent business rules.

Instead, clearly identify what is missing and request clarification.

# Development Workflow

Before implementing any feature:

1. Read the relevant documentation.
2. Search for existing implementations.
3. Reuse existing code whenever possible.
4. Keep changes focused on a single task.

After implementing a feature:

1. Review your own code.
2. Remove duplicated logic.
3. Verify consistency with the project architecture.
4. Explain the implementation before considering the task complete.

---

# Spec-Driven Development

WorkWorth follows the process defined in [docs/specs/README.md](docs/specs/README.md):

SPEC → technical proposal → explicit approval → implementation → tests and compilation → verification against the SPEC.

Before every implementation task, provide the proposed scope, affected files, relevant architectural or business decisions, and request explicit approval. Do not create or modify implementation files before that approval.

An approval covers the full approved task scope and reasonable related corrections, but not a new feature or a materially expanded scope.

Every important feature requires a SPEC based on [docs/specs/TEMPLATE.md](docs/specs/TEMPLATE.md). Requirements, acceptance criteria, and expected tests must be verified before marking the work complete.

# Additional Architectural Constraints

The backend is a modular monolith. Organize code by business capability while preserving Controller → Service → Repository within each module.

Use DTOs for every API contract. Do not expose JPA entities. Use Spring `ProblemDetail` for API errors unless an approved architectural decision records a justified exception.

Use `BigDecimal` for money, ISO currency codes, and explicit rounding. Inject `java.time.Clock` whenever current time affects business logic. Use Flyway for database migrations, Spring profiles and environment variables for configuration, automated service tests from the beginning, and Testcontainers PostgreSQL integration tests when persistence is introduced.
