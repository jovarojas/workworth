# WorkWorth Frontend

This directory contains the Angular client for WorkWorth.

## Responsibility

Angular renders API data, handles user interaction, navigation, form validation, and local UI state. Spring Boot remains the only source of business logic, calculations, reward evaluation, and external integrations.

The client will use standalone components, lazy-loaded feature routes, `provideHttpClient`, environment-based API configuration, and Signals where they are appropriate for local UI state.

## Development

From this directory:

```bash
npm install
npm start
```

The development server runs at `http://localhost:4200`.

## Commands

```bash
npm run build
npm test
```

## Delivery process

Do not implement a feature until its SPEC and technical proposal have been explicitly approved. See the repository-level [SPEC process](../../docs/specs/README.md) and [AGENTS.md](../../AGENTS.md).
