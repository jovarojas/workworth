# PROJECT.md

# WorkWorth

## Vision

WorkWorth is a personal finance and motivation application that transforms the user's working time into meaningful real-world rewards.

Instead of only showing money earned, the application translates earnings into experiences, purchases and personal goals.

The objective is to make the value of time tangible.

Examples:

- Today you earned enough for dinner at your favorite restaurant.
- Today you can afford a flight to Venice.
- You are only 18€ away from your next concert.
- You have already earned enough for one week of Netflix.

The application should feel motivating, modern and rewarding.

---

# Target Users

Initially:

Single user.

Future:

Multiple users with authentication.

---

# Core Features

## User Profile

Store personal information.

- Name
- Salary
- Working schedule
- Tax percentage
- Currency
- Country
- Preferred categories

---

## Workday

The user can:

Start work.

Pause work.

Resume work.

Finish work.

The system calculates:

Worked hours.

Gross earnings.

Net earnings.

Accumulated earnings.

---

## Dashboard

Main screen.

The primary message is: **"This is what my work has enabled me to achieve."**

Visual priority:

1. A principal reward the user can afford.
2. Net income accumulated in WorkWorth and progress achieved.
3. Today, current week, current month, and all-time net-income summary.
4. Current goal progress.
5. Active workday and timer.

Display:

Today's earnings.

Monthly earnings.

Current work session.

Current goal progress.

Suggested reward.

Motivational AI message.

---

## Rewards

Rewards represent things the user can currently afford.

Examples:

Restaurants

Flights

Hotels

Concerts

Technology

Games

Experiences

Subscriptions

The reward catalog should be expandable.

---

## Personal Goals

Users can create their own goals.

Examples:

Nintendo Switch 2

Vacation in Japan

New monitor

Laptop

Camera

The application tracks progress automatically.

---

## Statistics

Daily earnings.

Weekly earnings.

Monthly earnings.

Yearly earnings.

Worked hours.

Average daily earnings.

Average hourly earnings.

Goal completion.

---

## AI

The AI is not responsible for calculations.

Its only responsibility is generating natural language.

Examples:

Today your effort is worth a dinner for two.

Only 14€ left until your next trip.

Today's work paid for your favorite concert ticket.

---

## External APIs

Future versions may integrate:

Flights

Hotels

Concerts

Weather

Currency exchange

Maps

The application must continue working even if external APIs fail.

---

# MVP

Version 1 should include only:

User profile

Salary calculation

Workday tracking

Dashboard

Reward catalog

Statistics

No authentication.

No AI.

No external APIs.

No mobile synchronization.

---

# Future Roadmap

Version 2

Authentication.

JWT.

Cloud deployment.

AI generated messages.

Version 3

External APIs.

Dynamic rewards.

Notifications.

Version 4

Android application.

iOS application.

Wear OS support.

Apple Watch support.

---

# Technical Requirements

Business logic belongs only to the backend.

Frontend must remain lightweight.

All monetary calculations use net income. Gross annual salary is only an input to a future fiscal estimator; a user-provided real net income takes priority over any estimate.

All-time accumulated income starts at zero and means net earnings registered by WorkWorth. It is not a bank balance, saving balance, or disposable amount after expenses.

Functional features are specified before implementation in [docs/specs](docs/specs/README.md).

Every API should return DTOs.

Database should be normalized.

All features should be modular.

---

# Quality Goals

Readable code.

Small classes.

Small methods.

Low coupling.

High cohesion.

Scalable architecture.

Easy testing.

Production-ready quality.

---

# UI Principles

Modern.

Minimalist.

Clean.

Motivational.

Fast.

Dark mode support.

Responsive.

Material Design.

---

# Project Structure

Frontend

Angular

Backend

Spring Boot

Database

PostgreSQL

Communication

REST API

Authentication

JWT (future)

Deployment

Docker (future)

---

# Success Criteria

A successful version of WorkWorth should allow a user to:

Configure their salary.

Track a workday.

Know how much they earned.

Understand what they can afford.

Stay motivated through meaningful rewards.

Use the application without unnecessary complexity.
