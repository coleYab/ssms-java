# School System Management System (SSMS)

[![Java CI with Maven and PostgreSQL](https://github.com/your-org/ssms/actions/workflows/test.yml/badge.svg)](https://github.com/your-org/ssms/actions/workflows/test.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)

A modern, production-grade REST API backend for managing secondary school institutions. Built with **Spring Boot**, **PostgreSQL**, **Spring Security (Argon2id + RS256 JWT)**, and designed according to strict **Spring MVC layered architecture**.

---

## 📖 Table of Contents

- [Features](#-features)
- [Architecture & Tech Stack](#-architecture--tech-stack)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Running PostgreSQL with Docker](#running-postgresql-with-docker)
  - [Configuration](#configuration)
  - [Building and Running the Application](#building-and-running-the-application)
- [Default Super Administrator Credentials](#-default-super-administrator-credentials)
- [API Documentation (Swagger / OpenAPI)](#-api-documentation-swagger--openapi)
- [API Modules & Key Endpoints](#-api-modules--key-endpoints)
- [Response & Error Formats](#-response--error-formats)
- [Testing & Quality Verification](#-testing--quality-verification)
- [CI/CD Workflow](#-cicd-workflow)

---

## 🚀 Features

- **Authentication & RBAC:**
  - Stateless Bearer JWT authentication (RSA RS256) with public JWKS key rotation endpoint (`/.well-known/jwks.json`).
  - Argon2id password hashing powered by Bouncy Castle.
  - Multi-tier role-based access control (`SUPER_ADMIN`, `ADMIN`, `TEACHER`, `STUDENT`).
  - Concurrent session management with single-session and global revocation.
  - Sensitive action protection with 10-minute step-up authentication.
  - Forced password reset gates.
- **Academic Calendar & Grade Levels:**
  - Multi-year lifecycles (`PLANNED`, `ACTIVE`, `LOCKED`) with immutable archival guarantees.
  - Term planning with automatic date validation and overlap prevention.
  - Configurable sequence-ordered grade levels.
- **Classes & Student Enrollments:**
  - Class rosters, capacity constraints, homeroom teacher assignments, and classroom locations.
  - Student enrollment, historical status tracking, and class transfers.
- **Student & Guardian Management:**
  - Student records with auto-generated identification numbers (`STU-YYYY-NNNN`).
  - Relationship-mapped guardian directory and emergency contact records.
  - Multipart CSV bulk import and template downloads.
- **Teachers, Workload & Timetable:**
  - Teacher profiles with auto-generated employee numbers (`TCH-YYYY-NNNN`).
  - Qualification checks, subject allowances, and weekly period constraints.
  - Course offering management and schedule slot collision detection (teacher and room clash prevention).
- **Attendance Management:**
  - Daily & period-based attendance sessions.
  - Bulk student status marking (`PRESENT`, `ABSENT`, `LATE`, `EXCUSED`, `HALFDAY`).
  - Timetable-driven session generation.
  - Absence excuse submission and approval workflows.
  - Aggregated daily, monthly, and low-attendance percentage reporting.
- **System Administration & Auditability:**
  - Immutable audit trail tracking user IDs, actions, IP addresses, user agents, and mutation diffs.
  - Configurable system settings (school hours, lockout windows, password rules).
  - Role permission matrices and operational dashboards.
  - Health (`/api/v1/health`) and readiness (`/api/v1/health/ready`) probes.

---

## 🏗 Architecture & Tech Stack

- **Language:** Java 21+
- **Framework:** Spring Boot 4.1.1 (Spring 7.x)
- **Architecture Pattern:** Clean Spring MVC Layered Architecture (`Controller` -> `Service` -> `Repository` -> `Entity`)
- **Persistence:** Spring Data JPA + Hibernate ORM (UUID primary keys, UTC timestamp auditing, soft-deletes)
- **Database:** PostgreSQL 17
- **Security:** Spring Security + JJWT 0.12.6 + Bouncy Castle (Argon2id)
- **API Spec & Docs:** SpringDoc OpenAPI 3.1 (`/api/v1/openapi.json` & `/swagger-ui.html`)
- **Architecture Testing:** ArchUnit 1.4.0 (verifies strict layering and boundary separation)

---

## 🛠 Getting Started

### Prerequisites

- **Java JDK 21+**
- **Docker & Docker Compose** (for running PostgreSQL)
- **Maven Wrapper** (included in repository)

### Running PostgreSQL with Docker

Start the PostgreSQL 17 container using Docker Compose:

```bash
docker compose up -d
```

This starts a container listening on port `5432` with database `ssms_db`.

### Configuration

The application is pre-configured in `src/main/resources/application.yaml`. You can customize environment variables as needed:

| Variable | Description | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC Connection URL | `jdbc:postgresql://localhost:5432/ssms_db` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `postgres` |
| `SERVER_PORT` | HTTP port | `8080` |

### Building and Running the Application

Run the application directly with the Maven wrapper:

```bash
./mvnw spring-boot:run
```

Or build an executable JAR:

```bash
./mvnw clean package
java -jar target/ssms-0.0.1-SNAPSHOT.jar
```

---

## 🔑 Default Super Administrator Credentials

When the database is initialized for the first time, a default `SUPER_ADMIN` account is automatically seeded:

- **Email:** `admin@school.example.com`
- **Password:** `Admin123456!`
- **Role:** `SUPER_ADMIN`

> [!IMPORTANT]
> Change this password immediately after first login in production environments using `POST /api/v1/auth/change-password`.

---

## 📚 API Documentation (Swagger / OpenAPI)

Once the application is running:

- **Interactive Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI 3.1 JSON Contract:** [http://localhost:8080/api/v1/openapi.json](http://localhost:8080/api/v1/openapi.json)

### Authorizing in Swagger UI
1. Call `POST /api/v1/auth/login` with your credentials to obtain an `accessToken`.
2. Click the green **Authorize** button at the top-right of Swagger UI.
3. Paste the token into the `bearerAuth` field (e.g. `eyJhbGciOi...`).

---

## 📦 API Modules & Key Endpoints

All API endpoints are prefixed with `/api/v1`:

| Area | Base Path | Description |
|---|---|---|
| **Auth** | `/api/v1/auth`, `/.well-known/jwks.json` | Login, token refresh, sessions, password lifecycle, and JWKS |
| **Admins** | `/api/v1/admins` | Admin management, invitations, and profile updates |
| **Users** | `/api/v1/users` | User accounts, account locking/unlocking, status changes |
| **Academic** | `/api/v1/academic-years`, `/api/v1/terms` | Academic years, active year lookup, terms scheduling |
| **Grades** | `/api/v1/grade-levels` | Sequential grade levels |
| **Classes** | `/api/v1/classes` | Classes, rosters, homeroom teachers, and student transfers |
| **Students** | `/api/v1/students` | Student profiles, guardian mapping, CSV import/export |
| **Teachers** | `/api/v1/teachers` | Teacher staff, qualification catalog, workload tracking |
| **Courses** | `/api/v1/courses` | Master course catalog and subject settings |
| **Offerings** | `/api/v1/course-offerings`, `/api/v1/timetable` | Course offerings, periods, clash-free timetables |
| **Attendance** | `/api/v1/attendance` | Attendance sessions, records, excuses, and summaries |
| **System** | `/api/v1/audit-logs`, `/api/v1/system`, `/api/v1/dashboard`, `/api/v1/health` | Audit log search, settings, metrics, and health probes |

---

## 📐 Response & Error Formats

### Standard Success Envelope
All JSON 2xx responses are wrapped in a standard envelope:
```json
{
  "success": true,
  "data": { ... },
  "meta": {
    "requestId": "6f1c2b0e-8a47-4a3e-9d3e-5b7b1d2c9a10",
    "timestamp": "2026-10-08T07:30:00Z",
    "pagination": {
      "page": 1,
      "limit": 20,
      "totalItems": 342,
      "totalPages": 18,
      "hasNext": true,
      "hasPrev": false
    }
  },
  "links": {
    "self": "/api/v1/students?page=1&limit=20",
    "next": "/api/v1/students?page=2&limit=20",
    "prev": null,
    "first": "/api/v1/students?page=1&limit=20",
    "last": "/api/v1/students?page=18&limit=20"
  }
}
```
*(Pagination and links are omitted for single-entity or mutation responses).*

### Standard Error Envelope
All error responses follow the error specification:
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Validation failed for one or more fields.",
    "status": 400,
    "details": [
      {
        "field": "email",
        "code": "VALIDATION_INVALID_EMAIL",
        "message": "Must be a well-formed email address.",
        "location": "body"
      }
    ],
    "requestId": "a825cb66-cfdb-46c7-b409-abcd0566fe57",
    "timestamp": "2026-10-08T15:20:32Z",
    "path": "/api/v1/users",
    "method": "POST",
    "documentationUrl": "https://docs.school.example.com/errors/VALIDATION_FAILED"
  }
}
```

---

## 🧪 Testing & Quality Verification

Run the entire automated test suite:

```bash
./mvnw clean test
```

This executes:
1. **ArchUnit Architecture Tests:** Verifies strict MVC layer boundaries (Controllers $\rightarrow$ Services $\rightarrow$ Repositories; no direct repository calls from controllers).
2. **Spring Context & Integration Tests:** Validates full application wiring, database migrations, and bootstrapper beans.

---

## 🤖 CI/CD Workflow

The repository includes a GitHub Actions workflow located at `.github/workflows/test.yml`:
- Runs automatically on pushes and pull requests to `main`, `master`, and `develop`.
- Spins up a healthy PostgreSQL 17 test container.
- Builds and executes `./mvnw -B test` under Java 21.
- Uploads Surefire test reports as downloadable artifacts on failure or completion.
