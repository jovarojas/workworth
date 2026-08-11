# TASKS.md

# WorkWorth Development Roadmap

---

# Project Status

Current Phase

Sprint 0 - Project Setup

Current Version

0.1.0

## Delivery Rule

No implementation task may start until its SPEC is approved and its technical proposal has received explicit approval. See [docs/specs](docs/specs/README.md).

---

# Epic 1 - Project Foundation

## Sprint 0

### Documentation

- [ ] Create and maintain approved feature specifications
- [ ] Keep architecture and business-rule documentation aligned with approved SPECs

### Backend

- [ ] Configure Spring Boot project
- [ ] Configure PostgreSQL connection
- [ ] Configure application.properties
- [ ] Create package structure
- [ ] Configure CORS
- [ ] Create GlobalExceptionHandler
- [ ] Create ApiResponse model
- [ ] Configure validation

### Frontend

- [ ] Configure Angular project
- [ ] Install Angular Material
- [ ] Configure routing
- [ ] Create folder structure
- [ ] Create environment configuration
- [ ] Create HTTP interceptor
- [ ] Configure theme

### Communication

- [ ] Verify Angular ↔ Spring Boot communication
- [ ] Create health endpoint
- [ ] Display backend response in Angular

---

# Epic 2 - User Profile

## Sprint 1

### Backend

- [ ] Create User entity
- [ ] Create UserDTO
- [ ] Create UserRepository
- [ ] Create UserService
- [ ] Create UserController

### Frontend

- [ ] Create Profile page
- [ ] Create Profile form
- [ ] Save user profile

Features

- [ ] Name
- [ ] Salary
- [ ] Tax percentage
- [ ] Working hours
- [ ] Currency
- [ ] Country

---

# Epic 3 - Salary Calculator

## Sprint 2

### Backend

- [ ] Create SalaryService
- [ ] Calculate hourly salary
- [ ] Calculate minute salary
- [ ] Calculate second salary
- [ ] Calculate daily salary
- [ ] Calculate monthly salary
- [ ] Calculate yearly salary

### Frontend

- [ ] Salary summary card
- [ ] Salary breakdown

---

# Epic 4 - Workday

## Sprint 3

Backend

- [ ] Create WorkDay entity
- [ ] Repository
- [ ] DTO
- [ ] Mapper
- [ ] Service
- [ ] Controller

Features

- [ ] Start workday
- [ ] Pause workday
- [ ] Resume workday
- [ ] Finish workday
- [ ] Calculate worked time
- [ ] Calculate earnings

Frontend

- [ ] Workday page
- [ ] Live timer
- [ ] Earnings counter

---

# Epic 5 - Dashboard

## Sprint 4

- [ ] Today's earnings
- [ ] Monthly earnings
- [ ] Current work session
- [ ] Reward card
- [ ] Goal progress
- [ ] Recent activity

---

# Epic 6 - Reward Catalog

## Sprint 5

Backend

- [ ] Reward entity
- [ ] Reward CRUD
- [ ] Categories
- [ ] Search rewards

Frontend

- [ ] Reward list
- [ ] Reward details
- [ ] Reward filters

Categories

- [ ] Flights
- [ ] Hotels
- [ ] Restaurants
- [ ] Concerts
- [ ] Technology
- [ ] Games
- [ ] Streaming
- [ ] Experiences

---

# Epic 7 - Personal Goals

## Sprint 6

Backend

- [ ] Goal entity
- [ ] Goal service

Frontend

- [ ] Goal page
- [ ] Progress bar
- [ ] Goal history

Features

- [ ] Create goal
- [ ] Edit goal
- [ ] Delete goal
- [ ] Automatic progress

---

# Epic 8 - Statistics

## Sprint 7

Backend

- [ ] Statistics service

Frontend

- [ ] Daily chart
- [ ] Weekly chart
- [ ] Monthly chart
- [ ] Yearly chart

Metrics

- [ ] Hours worked
- [ ] Average salary
- [ ] Total earnings
- [ ] Goal completion

---

# Epic 9 - AI Integration

## Sprint 8

Backend

- [ ] AI provider interface
- [ ] OpenAI implementation
- [ ] Prompt builder
- [ ] AI service

Features

- [ ] Daily motivational message
- [ ] Reward description
- [ ] Goal encouragement

Frontend

- [ ] AI message card

---

# Epic 10 - External APIs

## Sprint 9

Possible integrations

- [ ] Flights
- [ ] Hotels
- [ ] Concerts
- [ ] Currency exchange

---

# Epic 11 - Authentication

## Sprint 10

Backend

- [ ] Spring Security
- [ ] JWT
- [ ] Login
- [ ] Register

Frontend

- [ ] Login page
- [ ] Register page
- [ ] Auth guard

---

# Epic 12 - Mobile

## Sprint 11

- [ ] Install Capacitor
- [ ] Android APK
- [ ] Notifications
- [ ] Dark mode improvements

---

# Epic 13 - Deployment

## Sprint 12

- [ ] Docker
- [ ] Docker Compose
- [ ] Production profile
- [ ] CI/CD
- [ ] Deploy backend
- [ ] Deploy frontend

---

# Nice to Have

- [ ] Multi-language support
- [ ] Multiple currencies
- [ ] Widgets
- [ ] Smart notifications
- [ ] Wear OS
- [ ] Apple Watch
- [ ] Offline mode

---

# Completed

Nothing yet 🚀
