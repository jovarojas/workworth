# 💼 WorkWorth

> **Transform your work into real-world value.**

WorkWorth is a full-stack web application that helps users understand the value of their working time by converting earnings into meaningful real-life experiences, purchases and personal goals.

Instead of simply displaying how much money has been earned, WorkWorth answers questions like:

- ✈️ *"Today you earned enough for a flight to Venice."*
- 🍽️ *"Today's work pays for a dinner for two."*
- 🎮 *"You're only €23 away from buying a Nintendo Switch 2."*
- 🎵 *"You can already afford a ticket for your next concert."*

The goal is to make time and money feel tangible by showing what work has enabled the user to achieve. The product prioritizes net income registered in WorkWorth and the rewards or goals it can represent, not only today's earnings.

---

# ✨ Features

## MVP Scope

- Salary configuration
- Workday tracking
- Automatic earnings calculation
- Dashboard
- Personal goals
- Reward catalog
- Statistics
- Responsive interface
- Dark mode support

## Current Status

The repository is in Sprint 0. The Angular workspace is scaffolded; backend, database, and product features have not yet been implemented.

---

## Planned

- AI-generated motivational messages
- Live earnings counter
- Flight integration
- Hotel integration
- Concert integration
- Smart notifications
- Android APK
- iOS support
- Authentication
- Cloud deployment

---

# 🏗 Architecture

```text
Angular
    │
REST API
    │
Spring Boot
    │
PostgreSQL
```

The backend acts as the **single source of truth**.

Business logic is implemented exclusively in Spring Boot.

Angular is responsible only for presentation and user interaction.

---

# 🛠 Tech Stack

## Frontend

- Angular
- TypeScript
- Angular Material
- SCSS
- RxJS
- Signals

## Backend

- Java 17
- Spring Boot
- Spring Data JPA
- Maven
- PostgreSQL
- Lombok

## Future

- OpenAI / Gemini
- Docker
- JWT
- GitHub Actions

---

# 📁 Project Structure

```
WorkWorth/

├── backend/
├── frontend/
│   └── frontend/              # Angular workspace
├── database/
├── docs/

├── AGENTS.md
├── PROJECT.md
├── TASKS.md
├── DECISIONS.md

└── README.md
```

---

# 🚀 Getting Started

## Backend

```bash
cd backend
```

Run

```bash
mvn spring-boot:run
```

---

## Frontend

```bash
cd frontend/frontend
```

Install dependencies

```bash
npm install
```

Run

```bash
ng serve
```

Angular runs at

```
http://localhost:4200
```

Spring Boot runs at

```
http://localhost:8080
```

---

# 🗄 Database

PostgreSQL

Create a database named

```
workworth
```

Configure credentials in

```
application.properties
```

---

# 📚 Documentation

Project documentation is available inside the repository.

| File | Description |
|------|-------------|
| [AGENTS.md](AGENTS.md) | AI coding rules and conventions |
| [PROJECT.md](PROJECT.md) | Product vision and requirements |
| [TASKS.md](TASKS.md) | Development roadmap |
| [DECISIONS.md](DECISIONS.md) | Architectural decisions |
| [docs/specs](docs/specs/README.md) | Feature specifications and Spec-Driven Development process |
| [docs/business-rules.md](docs/business-rules.md) | Index of approved cross-cutting business rules |

---

# 📈 Roadmap

## Phase 1

- Project setup
- Salary calculator
- Dashboard
- Workday

## Phase 2

- Goals
- Statistics
- Rewards

## Phase 3

- AI integration
- External APIs

## Phase 4

- Authentication
- Android APK
- Deployment

---

# 🎯 Project Goals

The project has been designed to:

- Practice enterprise-level architecture
- Build a production-ready Spring Boot backend
- Develop a modern Angular frontend
- Learn clean architecture
- Create a real portfolio project

---

# 📷 Screenshots

Coming soon.

---

# 🤝 Contributing

This is currently a personal learning and portfolio project.

Suggestions and improvements are always welcome.

---

# 📄 License

This project is licensed under the MIT License.

---

# 👩‍💻 Author

Developed by **Jovanna Rojas**.
